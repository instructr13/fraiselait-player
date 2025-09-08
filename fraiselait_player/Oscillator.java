import dev.wycey.mido.fraiselait.builtins.WaveformType;

import java.util.Objects;

public class Oscillator {
  private final String name;
  private final WaveformType waveform;
  private final double startVolume;
  private final double endVolume;
  private final double gamma;
  private final double duration;
  private final int quality;
  private final double durationPerSample;

  public Oscillator(String name, WaveformType waveform, double startVolume, double endVolume, double gamma, double duration, int quality) {
    this.name = name;
    this.waveform = waveform;
    this.startVolume = startVolume;
    this.endVolume = endVolume;
    this.gamma = gamma;
    this.duration = duration;
    this.quality = quality;
    this.durationPerSample = duration / quality;
  }

  public static Oscillator createConstantVolumeOscillator(String name, WaveformType waveform, double volume) {
    return new Oscillator(name, waveform, volume, volume, 1.0, 0.0, 1);
  }

  public String getName() {
    return name;
  }

  public WaveformType getWaveform() {
    return waveform;
  }

  public double getStartVolume() {
    return startVolume;
  }

  public double getEndVolume() {
    return endVolume;
  }

  public double getGamma() {
    return gamma;
  }

  public double getDuration() {
    return duration;
  }

  public int getQuality() {
    return quality;
  }

  public double getDurationPerSample() {
    return durationPerSample;
  }

  public boolean isSilent() {
    return startVolume == 0.0 && endVolume == 0.0;
  }

  public boolean isConstantVolume() {
    return startVolume == endVolume;
  }

  public double calculateVolume(double t) {
    if (t < 0 || t > 1) {
      throw new IllegalArgumentException("Time must be between 0 and 1");
    }

    if (isConstantVolume()) {
      return startVolume;
    }

    if (t <= 0.0) {
      return startVolume;
    }

    if (t >= 1.0) {
      return endVolume;
    }

    if (gamma == 1.0) {
      return startVolume + (endVolume - startVolume) * t;
    } else {
      return startVolume + (endVolume - startVolume) * Math.pow(t, gamma);
    }
  }

  @Override
  public final boolean equals(Object o) {
    if (!(o instanceof Oscillator)) return false;

    Oscillator that = (Oscillator) o;
    return Double.compare(startVolume, that.startVolume) == 0 && Double.compare(endVolume, that.endVolume) == 0 && Double.compare(gamma, that.gamma) == 0 && Double.compare(duration, that.duration) == 0 && quality == that.quality && Objects.equals(name, that.name) && waveform == that.waveform;
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(name);
    result = 31 * result + Objects.hashCode(waveform);
    result = 31 * result + Double.hashCode(startVolume);
    result = 31 * result + Double.hashCode(endVolume);
    result = 31 * result + Double.hashCode(gamma);
    result = 31 * result + Double.hashCode(duration);
    result = 31 * result + quality;
    return result;
  }

  @Override
  public String toString() {
    return "Oscillator(" +
        "name='" + name + '\'' +
        ", waveform=" + waveform +
        ", startVolume=" + startVolume +
        ", endVolume=" + endVolume +
        ", gamma=" + gamma +
        ", duration=" + duration +
        ", quality=" + quality +
        ')';
  }
}
