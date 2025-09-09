import processing.core.PApplet;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public abstract class Knob {
  private static final double DEFAULT_SENSITIVITY = 0.5f;

  protected Transform transform;
  protected final double minValue;
  protected final double maxValue;
  protected double currentValue;
  private final boolean vertical;
  private final UnaryOperator<Double> convertSensitivity;
  private Consumer<Knob> onChange;

  public Knob(Transform transform, double minValue, double maxValue, double initialValue, boolean vertical, UnaryOperator<Double> convertSensitivity) {
    this.transform = transform;
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.currentValue = initialValue;
    this.vertical = vertical;
    this.convertSensitivity = convertSensitivity;
  }

  public Knob(Transform transform, double minValue, double maxValue, double initialValue, boolean vertical) {
    this(transform, minValue, maxValue, initialValue, vertical, v -> DEFAULT_SENSITIVITY);
  }

  public Transform getTransform() {
    return transform;
  }

  public double getMinValue() {
    return minValue;
  }

  public double getMaxValue() {
    return maxValue;
  }

  public double getCurrentValue() {
    return currentValue;
  }

  public void setCurrentValue(double currentValue) {
    if (this.currentValue == currentValue) {
      return;
    }

    this.currentValue = currentValue;

    if (this.currentValue < minValue) {
      this.currentValue = minValue;
    } else if (this.currentValue > maxValue) {
      this.currentValue = maxValue;
    }

    if (onChange != null) {
      onChange.accept(this);
    }
  }

  public void setOnChange(Consumer<Knob> onChange) {
    this.onChange = onChange;
  }

  public boolean check(Point point) {
    return transform.contains(point);
  }

  public void drag(Point delta) {
    // set value by mouse movement like VST plugin's knobs
    final double deltaValue;
    final var sensitivity = convertSensitivity.apply(currentValue);

    if (vertical) {
      deltaValue = -delta.y * sensitivity * (maxValue - minValue) / transform.size.height;
    } else {
      deltaValue = delta.x * sensitivity * (maxValue - minValue) / transform.size.width;
    }

    setCurrentValue(currentValue + deltaValue);
  }

  public abstract void draw(PApplet a);
}
