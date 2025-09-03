import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum TimingFunctions implements Function<Double, Double> {
  LINEAR(t -> t, "linear"),
  EASE_IN(CubicBezier.EASE_IN, "ease-in"),
  EASE_OUT(CubicBezier.EASE_OUT, "ease-out"),
  EASE_IN_OUT(CubicBezier.EASE_IN_OUT, "ease-in-out");

  private final Function<Double, Double> function;
  private final String name;

  TimingFunctions(Function<Double, Double> function, String name) {
    this.function = function;
    this.name = name;
  }

  public static TimingFunctions fromName(String name) {
    for (var value : values()) {
      if (value.name.equals(name)) return value;
    }

    throw new IllegalArgumentException("Unknown timing function: " + name);
  }

  public static Set<String> getNames() {
    return Arrays.stream(values()).map(TimingFunctions::getName).collect(Collectors.toUnmodifiableSet());
  }

  public String getName() {
    return name;
  }

  @Override
  public Double apply(Double value) {
    return function.apply(value);
  }
}

class CubicBezier implements Function<Double, Double> {
  public static final CubicBezier EASE_IN = new CubicBezier(0.42, 0.0, 1.0, 1.0);
  public static final CubicBezier EASE_OUT = new CubicBezier(0.0, 0.0, 0.58, 1.0);
  public static final CubicBezier EASE_IN_OUT = new CubicBezier(0.42, 0.0, 0.58, 1.0);

  private static final double EPSILON = 1e-6;
  private final double x1, y1, x2, y2;

  public CubicBezier(double x1, double y1, double x2, double y2) {
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
  }

  private double bezierX(double t) {
    return 3 * x1 * t * Math.pow(1 - t, 2) + 3 * x2 * Math.pow(t, 2) * (1 - t) + Math.pow(t, 3);
  }

  private double bezierY(double t) {
    return 3 * y1 * t * Math.pow(1 - t, 2) + 3 * y2 * Math.pow(t, 2) * (1 - t) + Math.pow(t, 3);
  }

  private double bezierDerivativeX(double t) {
    return 3 * x1 * Math.pow(1 - t, 2) + 6 * x2 * t * (1 - t) + 3 * Math.pow(t, 2);
  }

  private double solveBezierX(double t) {
    var guessT = t;

    for (int i = 0; i < 10; i++) { // Newton-Raphson method
      final var x = bezierX(guessT) - t;
      final var dx = bezierDerivativeX(guessT);

      if (Math.abs(x) < EPSILON) return guessT;

      guessT -= x / dx;
    }

    return guessT; // Fallback
  }

  @Override
  public Double apply(Double value) {
    return bezierY(solveBezierX(value));
  }
}
