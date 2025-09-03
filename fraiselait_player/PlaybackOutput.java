import java.time.Duration;

/**
 * 再生用のインターフェース。
 */
public interface PlaybackOutput {
  void tone(double frequency);

  void tone(double frequency, Duration duration);

  void noTone();
}
