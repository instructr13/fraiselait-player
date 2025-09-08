public class PlaybackData {
  private final int index;
  private final float bpm;
  private final int measure;
  private final float volume;

  public PlaybackData(int index, float bpm, int measure, float volume) {
    this.index = index;
    this.bpm = bpm;
    this.measure = measure;
    this.volume = volume;
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

  @Override
  public final boolean equals(Object obj) {
    if (!(obj instanceof PlaybackData)) return false;

    PlaybackData other = (PlaybackData) obj;
    return index == other.index && Float.compare(bpm, other.bpm) == 0 && measure == other.measure;
  }

  @Override
  public int hashCode() {
    int result = index;
    result = 31 * result + Float.hashCode(bpm);
    result = 31 * result + measure;
    result = 31 * result + Float.hashCode(volume);
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
