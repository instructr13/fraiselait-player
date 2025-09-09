import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public class OscillatorSession {
  private volatile FraiselaitOutput output = null;

  private final OscillatorState state;
  private final AtomicReference<Note> currentNote = new AtomicReference<>(null);
  private final AtomicBoolean isRunning = new AtomicBoolean(false);
  private ExecutorService executorService;

  public OscillatorSession(OscillatorState state) {
    this.state = state;

    initializeExecutor();
  }

  private void initializeExecutor() {
    executorService = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r);

      t.setPriority(Thread.MAX_PRIORITY);
      t.setDaemon(true);
      t.setName("OscillatorSession-Thread");

      return t;
    });
  }

  public void attachOutput(FraiselaitOutput output) {
    this.output = output;
  }

  public void offerNote(Note note) {
    currentNote.set(note);
  }

  public void start() {
    if (isRunning.compareAndSet(false, true)) {
      if (executorService.isShutdown() || executorService.isTerminated()) {
        initializeExecutor();
      }

      executorService.submit(() -> {
        Note playedNote = null;
        LocalDateTime startTime = null;
        LocalDateTime lastUnitTime = null;
        Duration unitDuration = Duration.ZERO;

        // Main loop for audio processing
        while (isRunning.get()) {
          if (Thread.currentThread().isInterrupted()) {
            break;
          }

          if (output == null || !output.isUsable()) {
            LockSupport.parkNanos(1000);

            continue;
          }

          final var note = currentNote.get();

          if (note == null && playedNote != null) {
            state.setCurrentActualVol(0);
            state.resetDurationProgress();
            output.noTone();

            playedNote = null;
            startTime = null;
            lastUnitTime = null;
            unitDuration = Duration.ZERO;

            continue;
          }

          if (note == null) {
            LockSupport.parkNanos(1000);

            continue;
          }

          if (note.equals(playedNote)) {
            if (lastUnitTime.plus(unitDuration).isAfter(LocalDateTime.now())) {
              LockSupport.parkNanos(1000);

              continue;
            }

            final var elapsed = Duration.between(startTime, LocalDateTime.now()).toMillis();
            final var t = Math.min(1.0, elapsed / (double) state.getDuration());

            final var startVol = state.getStartVol();
            final var endVol = state.getEndVol();
            final var gamma = state.getGamma();

            final var vol = state.getVol();
            final var currentVol = startVol + (endVol - startVol) * Math.pow(t, gamma);

            state.setCurrentActualVol(currentVol);
            state.setDurationProgress(t);
            output.tone(note.toFreq(), currentVol * vol);

            lastUnitTime = LocalDateTime.now();

            continue;
          }

          playedNote = note;
          startTime = LocalDateTime.now();
          lastUnitTime = startTime;
          unitDuration = Duration.ofMillis(state.getDuration() / state.getQuality());

          output.changeWaveform(state.getWaveformType());

          final var vol = state.getVol();
          final var startVol = state.getStartVol();

          state.setCurrentActualVol(startVol);
          state.setDurationProgress(0.0);
          output.tone(note.toFreq(), startVol * vol);
        }
      });
    }
  }

  public void stop() {
    if (isRunning.compareAndSet(true, false)) {
      executorService.shutdownNow();

      try {
        if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
          System.err.println("Executor did not terminate in the specified time.");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
