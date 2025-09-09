import processing.core.PApplet;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

public class CurrentFrame {
  private final PApplet parent;
  private final LocalDateTime startTime;
  private Instant lastFrameTime = null;

  public CurrentFrame(PApplet parent) {
    this.parent = parent;

    startTime = LocalDateTime.now().minus(Duration.ofMillis(parent.millis()));
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public Duration getLastFrameTime() {
    return Duration.ofMillis(lastFrameTime.toEpochMilli());
  }

  public void update() {
    final var now = parent.millis();

    lastFrameTime = Instant.ofEpochMilli(now);
  }
}
