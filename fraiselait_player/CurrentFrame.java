import processing.core.PApplet;

import java.time.Duration;
import java.time.LocalDateTime;

public class CurrentFrame {
  private final PApplet parent;
  private final LocalDateTime startTime;
  private Integer lastFrame = null;
  private Duration lastFrameTime = Duration.ZERO;

  public CurrentFrame(PApplet parent) {
    this.parent = parent;

    startTime = LocalDateTime.now().minus(Duration.ofMillis(parent.millis()));
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public Duration getLastFrameTime() {
    return lastFrameTime;
  }

  public void update() {
    final var now = parent.millis();

    if (lastFrame != null) {
      lastFrameTime = Duration.ofMillis(now - lastFrame);
    }

    lastFrame = now;
  }
}
