import java.time.Duration;
import java.time.LocalDateTime;

public class Metronome {
  private LocalDateTime startTime;
  private double bpm;
  private int beats;

  public Metronome(double bpm, int beats) {
    this.bpm = bpm;
    this.beats = beats;
  }

  public Metronome() {
    this(150.0, 4);
  }

  public double getBPM() {
    return bpm;
  }

  public void setBPM(double value) {
    bpm = value;
  }

  public int getBeats() {
    return beats;
  }

  public void setBeats(int value) {
    beats = value;
  }

  public int getBars() {
    return (int) Math.ceil((double) getElapsedDuration().toNanos() / getBarDuration().toNanos());
  }

  public Duration getBeatDuration() {
    return Duration.ofNanos((long) (60_000_000_000.0 / bpm));
  }

  public Duration getBarDuration() {
    return getBeatDuration().multipliedBy(beats);
  }

  public Duration getElapsedDuration() {
    return startTime != null ? Duration.between(startTime, LocalDateTime.now()) : Duration.ZERO;
  }

  public int getBeat() {
    return (int) Math.floor((double) getElapsedDuration().toNanos() / getBeatDuration().toNanos()) % beats;
  }

  public Duration getLastBeatDuration() {
    return getElapsedDuration().minus(getBeatDuration().multipliedBy(getBeat()));
  }

  public double getBeatProgress() {
    return (double) getElapsedDuration().toNanos() / getBeatDuration().toNanos() % 1;
  }

  public double getBarProgress() {
    return (double) getElapsedDuration().toNanos() / getBarDuration().toNanos() % 1;
  }

  public void start(LocalDateTime value) {
    startTime = value;
  }

  public void start() {
    start(LocalDateTime.now());
  }

  public void stop() {
    startTime = null;
  }

  public boolean isPlaying() {
    return startTime != null && startTime.isBefore(LocalDateTime.now());
  }
}
