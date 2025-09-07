import dev.wycey.mido.fraiselait.builtins.WaveformType;

import java.time.Duration;

/**
 * 再生用のインターフェース。
 */
public interface PlaybackOutput {
  void changeWaveform(WaveformType type);

  void tone(double frequency, double volume);

  void tone(double frequency, double volume, Duration duration);

  void noTone();
}
