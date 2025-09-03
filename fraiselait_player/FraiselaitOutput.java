import dev.wycey.mido.fraiselait.SerialDevice;
import dev.wycey.mido.fraiselait.commands.CommandBuilder;

import java.time.Duration;

/**
 * Fraiselait で音を鳴らすためのクラス。
 */
public class FraiselaitOutput implements PlaybackOutput {
  private final SerialDevice device;

  public FraiselaitOutput(SerialDevice device) {
    this.device = device;
  }

  public SerialDevice getDevice() {
    return device;
  }

  @Override
  public void tone(double frequency) {
    device.send(new CommandBuilder().tone((int) Math.round(frequency)).build(), false);
  }

  @Override
  public void tone(double frequency, Duration duration) {
    device.send(new CommandBuilder().tone((int) Math.round(frequency), duration.toMillis()).build(), false);
  }

  @Override
  public void noTone() {
    device.send(new CommandBuilder().noTone().build(), false);
  }
}
