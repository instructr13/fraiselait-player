import dev.wycey.mido.fraiselait.builtins.FraiselaitDevice;
import dev.wycey.mido.fraiselait.builtins.WaveformType;
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
  public void changeWaveform(WaveformType type) {
    device.sendCommand(new CommandBuilder().changeWaveform(type).build());
  }

  @Override
  public void tone(double frequency, double volume) {
    device.sendCommand(new CommandBuilder().tone((float) frequency, (float) volume).build());
  }

  @Override
  public void tone(double frequency, double volume, Duration duration) {
    device.sendCommand(new CommandBuilder().tone((float) frequency, (float) volume, duration.toMillis()).build());
  }

  @Override
  public void noTone() {
    device.sendCommand(new CommandBuilder().noTone().build());
  }
}
