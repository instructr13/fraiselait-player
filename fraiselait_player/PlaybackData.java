import java.util.Objects;

public class PlaybackData {
  private final int index;
  private final float bpm;
  private final int measure;
  private final float volume;
  private final Oscillator oscillator;

  public PlaybackData(int index, float bpm, int measure, float volume, Oscillator oscillator) {
    this.index = index;
    this.bpm = bpm;
    this.measure = measure;
    this.volume = volume;
    this.oscillator = oscillator;
  }

  public int getIndex() {
    return index;
  }

  public float getBPM() {
    return bpm;
  }

  public int getMeasure() {
    return measure;
  }

  public float getVolume() {
    return volume;
  }

  public Oscillator getOscillator() {
    return oscillator;
  }

  @Override
  public final boolean equals(Object o) {
    if (!(o instanceof PlaybackData)) return false;

    PlaybackData that = (PlaybackData) o;
    return index == that.index && Float.compare(bpm, that.bpm) == 0 && measure == that.measure && Float.compare(volume, that.volume) == 0 && Objects.equals(oscillator, that.oscillator);
  }

  @Override
  public int hashCode() {
    int result = index;
    result = 31 * result + Float.hashCode(bpm);
    result = 31 * result + measure;
    result = 31 * result + Float.hashCode(volume);
    result = 31 * result + Objects.hashCode(oscillator);
    return result;
  }

  @Override
  public String toString() {
    return "CurrentPlaybackData(" +
        "index=" + index +
        ", bpm=" + bpm +
        ", measure=" + measure +
        ", volume=" + volume +
        ')';
  }
}
