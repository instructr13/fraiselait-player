import processing.core.PApplet;

import java.time.Duration;

public class ColorAnimator {
  private final PApplet p;
  private final AnimationManager animationManager;

  private int currentColor;
  private int targetColor;

  public ColorAnimator(PApplet p, AnimationManager animationManager, int initialColor) {
    this.p = p;
    this.animationManager = animationManager;
    this.currentColor = initialColor;
    this.targetColor = initialColor;
  }

  public void setTargetColor(int newColor) {
    if (newColor == targetColor) return;

    final var startColor = currentColor;
    final var endColor = newColor;

    this.targetColor = newColor;

    final var timeline = AnimationTimeline.of(
        Duration.ofMillis(300),
        progress -> {
          currentColor = p.lerpColor(startColor, endColor, progress.floatValue());
        },
        new AnimationKeyframe(0.0),
        new AnimationKeyframe(1.0)
    );

    animationManager.play(timeline);
  }

  public int getCurrentColor() {
    return currentColor;
  }
}
