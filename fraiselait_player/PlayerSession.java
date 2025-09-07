import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

/**
 * 音符の再生データ
 * ナノ秒単位で変換された再生オフセット・再生時間を保持する。
 */
class SoundData {
  final PlaybackData playbackData;
  final Double frequency;
  final Duration offset;
  final Duration duration;

  private SoundData(PlaybackData playbackData, Double frequency, Duration offset, Duration duration) {
    this.playbackData = playbackData;
    this.frequency = frequency;
    this.offset = offset;
    this.duration = duration;
  }

  static SoundData tone(PlaybackData playbackData, double frequency, Duration offset, Duration duration) {
    return new SoundData(playbackData, frequency, offset, duration);
  }

  static SoundData tone(PlaybackData playbackData, double frequency, Duration offset) {
    return new SoundData(playbackData, frequency, offset, null);
  }

  static SoundData noTone(PlaybackData playbackData, Duration offset) {
    return new SoundData(playbackData, null, offset, null);
  }
}

/**
 * パート毎の再生データ
 * 実際の演奏では、これと開始時刻 (ナノ秒) を基に行う
 */
class PlaybackPart {
  private static final double EPSILON = 1e-6;

  private final AtomicReference<PlaybackData> playbackData;
  private final List<SoundData> soundData;
  private final Duration totalDuration;
  private final boolean loop;

  private PlaybackOutput output;

  public PlaybackPart(Score score) {
    Objects.requireNonNull(score, "Score must not be null");

    var currentBPM = score.getStartingBPM();
    var currentMeasure = score.getStartingMeasure();

    playbackData = new AtomicReference<>(new PlaybackData(0, currentBPM, currentMeasure));

    var currentMeasureMillis = Notes.toDurationMillis(currentBPM, currentMeasure);
    var currentOffset = Duration.ofNanos(Math.round(score.getOffset() * 1_000_000));

    final var soundData = new ArrayList<SoundData>();
    final var commands = score.getCommands();
    var loop = false;

    for (int i = 0; i < commands.size(); i++) {
      final var command = commands.get(i);

      if (command instanceof ScoreCommand.Replay) {
        loop = true;

        continue;
      }

      if (command instanceof ScoreCommand.ChangeBPM) {
        currentBPM = ((ScoreCommand.ChangeBPM) command).getBPM();
        currentMeasureMillis = Notes.toDurationMillis(currentBPM, currentMeasure);

        continue;
      }

      if (command instanceof ScoreCommand.ChangeMeasure) {
        currentMeasure = ((ScoreCommand.ChangeMeasure) command).getMeasure();
        currentMeasureMillis = Notes.toDurationMillis(currentBPM, currentMeasure);

        continue;
      }

      final var playbackData = new PlaybackData(i, currentBPM, currentMeasure);

      if (command instanceof ScoreCommand.Rest) {
        soundData.add(SoundData.noTone(playbackData, currentOffset));

        currentOffset = currentOffset.plus(
            Duration.ofNanos(Math.round(((ScoreCommand.Rest) command).getDuration() * currentMeasureMillis * 1_000_000))
        );

        continue;
      }

      if (command instanceof ScoreCommand.Stop) {
        soundData.add(SoundData.noTone(playbackData, currentOffset));

        break;
      }

      if (command instanceof ScoreCommand.PlayNote) {
        final var playNote = (ScoreCommand.PlayNote) command;
        final var duration = playNote.getDuration();
        final var nextNoteDuration = playNote.getNextNoteDuration();

        final var frequency = playNote.getFrequency().get();

        if (Math.abs(nextNoteDuration - duration) > EPSILON)
          soundData.add(SoundData.tone(
              playbackData,
              frequency,
              currentOffset,
              Duration.ofNanos(Math.round(duration * currentMeasureMillis * 1_000_000))
          ));
        else
          soundData.add(SoundData.tone(playbackData, frequency, currentOffset));

        currentOffset = currentOffset.plus(
            Duration.ofNanos(Math.round(nextNoteDuration * currentMeasureMillis * 1_000_000))
        );

        continue;
      }

      if (command instanceof ScoreCommand.Pitch) {
        final var pitch = (ScoreCommand.Pitch) command;
        final var duration = pitch.getDuration();
        final var nextNoteDuration = pitch.getNextNoteDuration();

        final var beforeNoteFrequency = pitch.getBeforeFrequency().get();
        final var afterNoteFrequency = pitch.getAfterFrequency().get();

        final var quality = pitch.getQuality();
        final var function = pitch.getFunction();

        final List<Double> frequencies = new ArrayList<>();

        frequencies.add(beforeNoteFrequency);

        for (int j = 1; j < quality - 1; j++) {
          final var t = function.apply((double) j / quality);

          frequencies.add(
              beforeNoteFrequency * (1 - t) + afterNoteFrequency * t // linear interpolation
          );
        }

        frequencies.add(afterNoteFrequency);

        final var fragmentMeasure = currentMeasure * quality;
        final var fragmentMeasureMillis = Notes.toDurationMillis(currentBPM, fragmentMeasure);

        final var fragmentDuration = Duration.ofNanos(Math.round(duration * fragmentMeasureMillis * 1_000_000));

        for (final var freq : frequencies) {
          soundData.add(SoundData.tone(
              playbackData,
              freq,
              currentOffset,
              fragmentDuration
          ));

          currentOffset = currentOffset.plus(fragmentDuration);
        }

        if (Math.abs(nextNoteDuration - duration) > EPSILON) {
          soundData.add(SoundData.noTone(playbackData, currentOffset));

          // skip the remaining duration
          currentOffset = currentOffset.plus(
              Duration.ofNanos(Math.round((nextNoteDuration - duration) * currentMeasureMillis * 1_000_000))
          );
        }

        continue;
      }

      if (command instanceof ScoreCommand.Vibrato) {
        final var vibrato = (ScoreCommand.Vibrato) command;
        final var duration = vibrato.getDuration();
        final var nextNoteDuration = vibrato.getNextNoteDuration();

        final var note1Frequency = vibrato.getFrequency1().get();
        final var note2Frequency = vibrato.getFrequency2().get();

        final var count = vibrato.getCount();

        final var fragmentMeasure = currentMeasure * count;
        final var fragmentMeasureMillis = Notes.toDurationMillis(currentBPM, fragmentMeasure);

        final var frequencies = new ArrayList<Double>();

        for (int j = 0; j < count; j++) {
          if (j % 2 == 0) {
            frequencies.add(note1Frequency);
          } else {
            frequencies.add(note2Frequency);
          }
        }

        final var fragmentDuration = Duration.ofNanos(Math.round(duration * fragmentMeasureMillis * 1_000_000));

        for (final var freq : frequencies) {
          soundData.add(SoundData.tone(
              playbackData,
              freq,
              currentOffset,
              fragmentDuration
          ));

          currentOffset = currentOffset.plus(fragmentDuration);
        }

        if (Math.abs(nextNoteDuration - duration) > EPSILON) {
          soundData.add(SoundData.noTone(playbackData, currentOffset));

          // skip the remaining duration
          currentOffset = currentOffset.plus(
              Duration.ofNanos(Math.round((nextNoteDuration - duration) * currentMeasureMillis * 1_000_000))
          );
        }
      }

      continue;
    }

    this.soundData = soundData;
    this.totalDuration = currentOffset;
    this.loop = loop;
  }

  public PlaybackPart(Score score, PlaybackOutput output) {
    this(score);

    Objects.requireNonNull(output, "Output must not be null");

    this.output = output;
  }

  public PlaybackData getPlaybackData() {
    return playbackData.get();
  }

  void setPlaybackData(PlaybackData value) {
    playbackData.set(value);
  }

  public PlaybackOutput getOutput() {
    return output;
  }

  public void setOutput(PlaybackOutput output) {
    this.output = output;
  }

  public boolean hasOutput() {
    return output != null;
  }

  public List<SoundData> getSoundData() {
    return soundData;
  }

  public Duration getTotalDuration() {
    return totalDuration;
  }

  public boolean isLoop() {
    return loop;
  }
}

class PlaybackState {
  public final AtomicBoolean isPlaying = new AtomicBoolean(false);
  private final AtomicReference<LocalDateTime> globalTimeOffset = new AtomicReference<>();

  public LocalDateTime getGlobalTimeOffset() {
    return globalTimeOffset.get();
  }

  public void setGlobalTimeOffset(LocalDateTime value) {
    globalTimeOffset.set(value);
  }

  public void updateAndGetGlobalTimeOffset(UnaryOperator<LocalDateTime> updater) {
    globalTimeOffset.updateAndGet(updater);
  }
}

/**
 * 音楽再生セッション
 * 複数パートを管理できるため、音楽一つにつき一つのインスタンスが必要。ただし、パート数はインスタンスにつき不変なため、再利用はできない。
 * try-with-resources で使用することを推奨する。
 */
public class PlayerSession implements AutoCloseable {
  private final List<Score> scores;
  private final PlaybackExecutor executor;

  public PlayerSession(List<Score> scores, List<PlaybackOutput> outputs) {
    this.scores = scores;

    final var parts = scores.stream().map(PlaybackPart::new).toList();

    if (outputs != null) {
      if (scores.size() != outputs.size()) {
        throw new IllegalArgumentException("Number of parts and outputs must match");
      }

      for (int i = 0; i < parts.size(); i++) {
        parts.get(i).setOutput(outputs.get(i));
      }
    }

    executor = new PlaybackExecutor(parts);
  }

  public PlayerSession(List<Score> scores) {
    this(scores, null);
  }

  public List<Score> getScores() {
    return scores;
  }

  private PlaybackData getPlaybackDataFor(int partIndex) {
    return executor.getParts().get(partIndex).getPlaybackData();
  }

  public int getCursorFor(int partIndex) {
    return getPlaybackDataFor(partIndex).getIndex();
  }

  public float getBPMFor(int partIndex) {
    return getPlaybackDataFor(partIndex).getBPM();
  }

  public int getMeasureFor(int partIndex) {
    return getPlaybackDataFor(partIndex).getMeasure();
  }

  /**
   * 音楽再生を開始する
   *
   * @param outputs 各パートの音声出力
   */
  public CompletableFuture<Void> start(List<PlaybackOutput> outputs) {
    try {
      return executor.play(outputs);
    } catch (ExecutionException e) {
      throw new CompletionException(e.getCause());
    }
  }

  /**
   * 音楽再生を開始する
   */
  public CompletableFuture<Void> start() {
    return start(null);
  }

  /**
   * 音楽再生を少し早める
   * <p>
   * 使い方:
   * <ol>
   *   <li>
   *     フレームが落ちたときに同期する:
   *     <pre>
   *       void handleFrameDrop() {
   *         if (frameTime > expectedFrameTime) {
   *           Duration dropTime = Duration.ofNanos(frameTime - expectedFrameTime);
   *           player.skip(dropTime);
   *         }
   *       }
   *     </pre>
   *   </li>
   * </ol>
   * </p>
   *
   * @param duration 進める時間
   */
  public void skip(Duration duration) {
    executor.skip(duration);
  }

  /**
   * 再生開始した時刻を取得する
   * ループ再生時は一番最新の繰り返しが始まった時刻を取得する。
   */
  public LocalDateTime getStartTime() {
    return executor.getStartTime();
  }

  /**
   * 音楽再生中かどうかを取得する
   */
  public boolean isPlaying() {
    return executor.isPlaying();
  }

  /**
   * 現在の再生位置を取得する
   */
  public Duration getPosition() {
    return executor.getPosition();
  }

  /**
   * 再生を停止する
   * すでに停止している場合は何もしない
   */
  public void stop() {
    executor.stop();
  }

  /*
   * @throws Exception 演奏スレッドが1秒以上停止に応答しなかった場合は {@link TimeoutException} が発生する
   */
  @Override
  public void close() throws Exception {
    executor.close();
  }

  private static class PlaybackExecutor implements AutoCloseable {
    private final List<PlaybackPart> parts;
    private final Duration maxDuration;
    private final CyclicBarrier startBarrier;
    private final AtomicInteger threadId = new AtomicInteger(0);
    private final PlaybackState state = new PlaybackState();

    private ExecutorService executorService;
    private CompletableFuture<Void> playbackFuture = new CompletableFuture<>();

    public PlaybackExecutor(List<PlaybackPart> parts) {
      this.parts = parts;

      maxDuration = parts
          .stream()
          .map(PlaybackPart::getTotalDuration)
          .max(Duration::compareTo)
          .orElse(Duration.ZERO);

      startBarrier = new CyclicBarrier(parts.size());

      initializePlaybackResources();
    }

    private void initializePlaybackResources() {
      threadId.set(0);

      executorService = Executors.newFixedThreadPool(
          parts.size(),
          r -> {
            var thread = new Thread(r);

            thread.setPriority(Thread.MAX_PRIORITY);
            thread.setName("PlayerSession Thread " + threadId.getAndIncrement());

            return thread;
          }
      );

      playbackFuture = new CompletableFuture<>();
    }

    public List<PlaybackPart> getParts() {
      return parts;
    }

    private void accurateSleep(LocalDateTime targetTime) throws InterruptedException {
      if (LocalDateTime.now().isAfter(targetTime)) {
        return;
      }

      final var sleepTime = Duration.between(LocalDateTime.now(), targetTime).minus(Duration.ofMillis(16));

      if (!sleepTime.isNegative()) {
        Thread.sleep(sleepTime.toMillis());
      }

      while (LocalDateTime.now().isBefore(targetTime)) {
        Thread.onSpinWait();
      }
    }

    private void playOnce(PlaybackPart part) throws InterruptedException {
      final var startTime = state.getGlobalTimeOffset();
      final var output = part.getOutput();

      for (var it = part.getSoundData().iterator(); it.hasNext(); ) {
        final var soundData = it.next();

        if (Thread.interrupted()) throw new InterruptedException();

        final var targetTime = startTime.plus(soundData.offset);

        accurateSleep(targetTime);

        if (soundData.frequency != null) {
          if (soundData.duration != null) {
            output.tone(soundData.frequency, 1.0, soundData.duration);
          } else {
            output.tone(soundData.frequency, 1.0);
          }

          if (!it.hasNext() && part.isLoop())
            accurateSleep(startTime.plus(part.getTotalDuration())); // wait until the end of the loop
        } else {
          output.noTone();
        }

        part.setPlaybackData(soundData.playbackData);
      }
    }

    private void playPart(PlaybackPart part) {
      final var output = part.getOutput();

      try {
        do {
          if (startBarrier.await() == 0) {
            state.setGlobalTimeOffset(LocalDateTime.now());
          }

          playOnce(part);
        } while (part.isLoop() && state.isPlaying.get());
      } catch (InterruptedException | BrokenBarrierException e) {
        Thread.currentThread().interrupt();

        output.noTone();

        throw new CompletionException(e);
      } catch (Exception e) {
        output.noTone();

        throw new CompletionException(e);
      }
    }

    public CompletableFuture<Void> play(List<PlaybackOutput> outputs) throws ExecutionException {
      if (state.isPlaying.compareAndSet(false, true)) {
        if (outputs != null) {
          if (parts.size() != outputs.size()) {
            throw new IllegalArgumentException("Number of parts and outputs must match");
          }

          for (int i = 0; i < parts.size(); i++) {
            parts.get(i).setOutput(outputs.get(i));
          }
        }

        final var missingOutputs = parts.stream().filter(part -> !part.hasOutput()).count();

        if (missingOutputs > 0) {
          throw new IllegalArgumentException("All parts must have outputs (missing: " + missingOutputs + ")");
        }

        if (executorService.isShutdown()) {
          initializePlaybackResources();
        }

        CompletableFuture.allOf(parts
                .stream()
                .map(part -> CompletableFuture.runAsync(() -> playPart(part), executorService))
                .toArray(CompletableFuture[]::new))
            .thenRun(() -> {
              state.isPlaying.set(false);
              playbackFuture.complete(null);
            })
            .exceptionally(t -> {
              final var cause = t.getCause();

              playbackFuture.completeExceptionally(
                  cause instanceof RuntimeException ? cause : new RuntimeException("Failed to execute playback", cause)
              );

              return null;
            });
      }

      return playbackFuture;
    }

    public synchronized CompletableFuture<Void> play() throws ExecutionException {
      return play(null);
    }

    public void skip(Duration duration) {
      state.updateAndGetGlobalTimeOffset(time -> time.minus(duration));
    }

    public LocalDateTime getStartTime() {
      return state.getGlobalTimeOffset();
    }

    public boolean isPlaying() {
      return state.isPlaying.get();
    }

    public Duration getPosition() {
      if (!state.isPlaying.get())
        return Duration.ZERO;

      final var now = LocalDateTime.now();
      final var startTime = state.getGlobalTimeOffset();

      return Duration.between(startTime, now).compareTo(maxDuration) > 0
          ? maxDuration
          : Duration.between(startTime, now);
    }

    public synchronized void stop() {
      if (state.isPlaying.compareAndSet(true, false)) {
        for (final var part : parts) {
          part.getOutput().noTone();
        }

        executorService.shutdownNow();

        try {
          if (!executorService.awaitTermination(1, TimeUnit.SECONDS))
            throw new RuntimeException("Failed to stop playback within 1 second");
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();

          throw new CompletionException(e);
        }

        if (!playbackFuture.isDone())
          playbackFuture.completeExceptionally(new CancellationException("Playback stopped"));
      }
    }

    @Override
    public synchronized void close() throws Exception {
      executorService.shutdownNow();

      try {
        if (!executorService.awaitTermination(1, TimeUnit.SECONDS))
          throw new TimeoutException("Failed to stop playback within 1 second");
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();

        throw new CompletionException(e);
      }

      if (!playbackFuture.isDone())
        playbackFuture.completeExceptionally(new CancellationException("Playback stopped"));
    }
  }
}
