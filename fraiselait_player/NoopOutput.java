import dev.wycey.mido.fraiselait.builtins.WaveformType;

import java.time.Duration;

/**
 * 音を鳴らすためのクラス。 (なにもしない)
 */
public class NoopOutput implements PlaybackOutput {
  @Override
  public void changeWaveform(WaveformType type) {}

  @Override
  public void tone(double frequency, double volume) {}

  @Override
  public void tone(double frequency, double volume, Duration duration) {}

  @Override
  public void noTone() {}
}
