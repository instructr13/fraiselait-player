import dev.wycey.mido.fraiselait.builtins.WaveformType;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class OscillatorState {
  private final AtomicReference<WaveformType> waveformType = new AtomicReference<>(WaveformType.SQUARE);
  private final AtomicReference<Double> startVol = new AtomicReference<>(1.0);
  private final AtomicReference<Double> endVol = new AtomicReference<>(1.0);
  private final AtomicReference<Double> gamma = new AtomicReference<>(1.0);
  private final AtomicInteger duration = new AtomicInteger(2000);
  private final AtomicInteger quality = new AtomicInteger(250);

  private final AtomicReference<Double> vol = new AtomicReference<>(1.0);

  private final AtomicReference<Double> durationProgress = new AtomicReference<>(null);
  private final AtomicReference<Double> currentActualVol = new AtomicReference<>(0.0);

  public WaveformType getWaveformType() {
    return waveformType.get();
  }

  public void setWaveformType(WaveformType waveformType) {
    this.waveformType.set(waveformType);
  }

  public double getStartVol() {
    return startVol.get();
  }

  public void setStartVol(double startVol) {
    this.startVol.set(startVol);
  }

  public double getEndVol() {
    return endVol.get();
  }

  public void setEndVol(double endVol) {
    this.endVol.set(endVol);
  }

  public double getGamma() {
    return gamma.get();
  }

  public void setGamma(double gamma) {
    this.gamma.set(gamma);
  }

  public int getDuration() {
    return duration.get();
  }

  public void setDuration(int duration) {
    this.duration.set(duration);
  }

  public int getQuality() {
    return quality.get();
  }

  public void setQuality(int quality) {
    this.quality.set(quality);
  }

  public double getVol() {
    return vol.get();
  }

  public void setVol(double vol) {
    this.vol.set(vol);
  }

  public Double getDurationProgress() {
    return durationProgress.get();
  }

  public void setDurationProgress(Double durationProgress) {
    this.durationProgress.set(durationProgress);
  }

  public void resetDurationProgress() {
    this.durationProgress.set(null);
  }

  public boolean hasDurationProgress() {
    return this.durationProgress.get() != null;
  }

  public double getCurrentActualVol() {
    return currentActualVol.get();
  }

  public void setCurrentActualVol(double currentActualVol) {
    this.currentActualVol.set(currentActualVol);
  }
}
