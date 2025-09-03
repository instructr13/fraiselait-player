public class PlaybackData {
  final int index;
  final float bpm;
  final int measure;

  public PlaybackData(int index, float bpm, int measure) {
    this.index = index;
    this.bpm = bpm;
    this.measure = measure;
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
    return result;
  }

  @Override
  public String toString() {
    return "CurrentPlaybackData(" +
        "index=" + index +
        ", bpm=" + bpm +
        ", measure=" + measure +
        ')';
  }
}
