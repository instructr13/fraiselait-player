import dev.wycey.mido.fraiselait.builtins.WaveformType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 音符の再生データ
 * ナノ秒単位で変換された再生オフセット・再生時間を保持する。
 */
class SoundData {
  final PlaybackData playbackData;
  final Double frequency;
  final Double actualVolume;
  final Duration offset;
  final Duration duration;
  final WaveformType waveformType;

  private SoundData(PlaybackData playbackData, Double frequency, Double actualVolume, Duration offset, Duration duration, WaveformType waveformType) {
    this.playbackData = playbackData;
    this.frequency = frequency;
    this.actualVolume = actualVolume;
    this.offset = offset;
    this.duration = duration;
    this.waveformType = waveformType;
  }

  static SoundData tone(PlaybackData playbackData, double frequency, double actualVolume, Duration offset, Duration duration) {
    return new SoundData(playbackData, frequency, actualVolume, offset, duration, null);
  }

  static SoundData tone(PlaybackData playbackData, double frequency, double actualVolume, Duration offset) {
    return new SoundData(playbackData, frequency, actualVolume, offset, null, null);
  }

  static SoundData noTone(PlaybackData playbackData, Duration offset) {
    return new SoundData(playbackData, null, null, offset, null, null);
  }

  static SoundData changeWaveform(PlaybackData playbackData, Duration offset, WaveformType waveformType) {
    return new SoundData(playbackData, null, null, offset, null, waveformType);
  }
}

/**
 * パート毎の再生データ
 * 実際の演奏では、これと開始時刻 (ナノ秒) を基に行う
 */
public class PlaybackPart {
  private static final double EPSILON = 1e-6;

  private final AtomicReference<PlaybackData> playbackData;
  private final AtomicReference<Float> actualVolume = new AtomicReference<>(1.0f);
  private final List<SoundData> soundData;
  private final Duration totalDuration;
  private final boolean loop;

  private PlaybackOutput output;

  PlaybackPart(Score score) {
    Objects.requireNonNull(score, "Score must not be null");

    var currentBPM = score.getStartingBPM();
    var currentMeasure = score.getStartingMeasure();
    var currentVolume = 1f;
    var currentOscillator = score.getStartingOscillator();
    var currentWaveform = currentOscillator.getWaveform();

    playbackData = new AtomicReference<>(new PlaybackData(0, currentBPM, currentMeasure, currentVolume, currentOscillator));

    var currentMeasureMillis = Notes.toDurationMillis(currentBPM, currentMeasure);
    var currentOffset = Duration.ofNanos(Math.round(score.getOffset() * 1_000_000));

    final var soundData = new ArrayList<SoundData>();
    final var commands = score.getCommands();
    var loop = false;

    // Set default oscillator waveform at the beginning
    soundData.add(SoundData.changeWaveform(playbackData.get(), currentOffset, currentWaveform));

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

      if (command instanceof ScoreCommand.ChangeVolume) {
        currentVolume = ((ScoreCommand.ChangeVolume) command).getVolume();

        continue;
      }

      final var playbackData = new PlaybackData(i, currentBPM, currentMeasure, currentVolume, currentOscillator);

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

        if (currentOscillator.isConstantVolume()) {
          final var actualVolume = currentVolume * currentOscillator.getStartVolume();

          if (Math.abs(nextNoteDuration - duration) > EPSILON)
            soundData.add(SoundData.tone(
                playbackData,
                frequency,
                actualVolume,
                currentOffset,
                Duration.ofNanos(Math.round(duration * currentMeasureMillis * 1_000_000))
            ));
          else
            soundData.add(SoundData.tone(playbackData, frequency, actualVolume, currentOffset));

          currentOffset = currentOffset.plus(
              Duration.ofNanos(Math.round(nextNoteDuration * currentMeasureMillis * 1_000_000))
          );
        } else {
          final var durationMillis = duration * currentMeasureMillis;
          final var oscillatorDurationMillis = currentOscillator.getDuration();
          final var durationMillisPerSample = currentOscillator.getDurationPerSample();

          var prevDurationMillis = 0.0;

          while (prevDurationMillis < durationMillis) {
            final var nextDurationMillis = Math.min(prevDurationMillis + durationMillisPerSample, durationMillis);
            final var fragmentDuration = Duration.ofNanos(Math.round((nextDurationMillis - prevDurationMillis) * 1_000_000));

            final var t = nextDurationMillis / oscillatorDurationMillis;
            final var actualVolume = currentVolume * currentOscillator.calculateVolume(t);

            soundData.add(SoundData.tone(
                playbackData,
                frequency,
                actualVolume,
                currentOffset
            ));

            currentOffset = currentOffset.plus(
                fragmentDuration
            );

            if (t >= 1.0) {
              break;
            }

            prevDurationMillis = nextDurationMillis;
          }

          // handle overshoot
          if (durationMillis > oscillatorDurationMillis) {
            final var overshootDuration = Duration.ofNanos(Math.round((durationMillis - oscillatorDurationMillis) * 1_000_000));

            soundData.add(SoundData.tone(
                playbackData,
                frequency,
                currentVolume * currentOscillator.getEndVolume(),
                currentOffset,
                overshootDuration
            ));

            currentOffset = currentOffset.plus(
                overshootDuration
            );
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

        final var unitT = duration * fragmentMeasureMillis / currentOscillator.getDuration();
        final var fragmentDuration = Duration.ofNanos(Math.round(duration * fragmentMeasureMillis * 1_000_000));

        for (int j = 0; j < frequencies.size(); j++) {
          final var frequency = frequencies.get(j);

          var t = j * unitT;

          if (t > 1) t = 1;

          final var actualVolume = currentVolume * currentOscillator.calculateVolume(t);

          soundData.add(SoundData.tone(
              playbackData,
              frequency,
              actualVolume,
              currentOffset
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

        final var unitT = duration * fragmentMeasureMillis / currentOscillator.getDuration();
        final var fragmentDuration = Duration.ofNanos(Math.round(duration * fragmentMeasureMillis * 1_000_000));

        for (int j = 0; j < frequencies.size(); j++) {
          final var frequency = frequencies.get(j);

          final var t = j * unitT;

          if (t > 1) t = 1;

          final var actualVolume = currentVolume * currentOscillator.calculateVolume(t);

          soundData.add(SoundData.tone(
              playbackData,
              frequency,
              actualVolume,
              currentOffset
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

      if (command instanceof ScoreCommand.Use) {
        currentOscillator = ((ScoreCommand.Use) command).getOscillator();

        if (currentWaveform != currentOscillator.getWaveform()) {
          currentWaveform = currentOscillator.getWaveform();

          soundData.add(SoundData.changeWaveform(playbackData, currentOffset, currentOscillator.getWaveform()));
        }

        continue;
      }
    }

    this.soundData = soundData;
    this.totalDuration = currentOffset;
    this.loop = loop;
  }

  PlaybackPart(Score score, PlaybackOutput output) {
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

  public float getActualVolume() {
    return actualVolume.get();
  }

  void setActualVolume(float volume) {
    actualVolume.set(volume);
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

  List<SoundData> getSoundData() {
    return soundData;
  }

  public Duration getTotalDuration() {
    return totalDuration;
  }

  public boolean isLoop() {
    return loop;
  }
}
