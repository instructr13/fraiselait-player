import dev.wycey.mido.fraiselait.builtins.FraiselaitDevice;
import dev.wycey.mido.fraiselait.builtins.commands.CommandBuilder;

import java.time.Duration;

/**
 * Fraiselait で音を鳴らすためのクラス。
 */
public class FraiselaitOutput implements PlaybackOutput {
  private final FraiselaitDevice device;

  public FraiselaitOutput(FraiselaitDevice device) {
    this.device = device;
  }

  public FraiselaitDevice getDevice() {
    return device;
  }

  @Override
  public void tone(double frequency) {
    device.sendCommand(new CommandBuilder().tone((int) Math.round(frequency)).build());
  }

  @Override
  public void tone(double frequency, Duration duration) {
    device.sendCommand(new CommandBuilder().tone((int) Math.round(frequency), duration.toMillis()).build());
  }

  @Override
  public void noTone() {
    device.sendCommand(new CommandBuilder().noTone().build());
  }
}
