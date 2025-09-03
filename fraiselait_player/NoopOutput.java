import java.time.Duration;

/**
 * 音を鳴らすためのクラス。 (なにもしない)
 */
public class NoopOutput implements PlaybackOutput {
  @Override
  public void tone(double frequency) {}

  @Override
  public void tone(double frequency, Duration duration) {}

  @Override
  public void noTone() {}
}
