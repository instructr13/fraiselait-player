import processing.core.PApplet;

public class Header implements Component {
  private final String text;
  private final Point position;
  private final int width;
  private final int additionalPadding;

  public Header(String text, Point position, int width, int additionalPadding) {
    this.text = text;
    this.position = position;
    this.width = width;
    this.additionalPadding = additionalPadding;
  }

  @Override
  public void draw(PApplet a, ComponentsRegistry r) {
    final var centerX = position.x + width / 2f;
    final var centerY = position.y + 20;

    a.stroke(ColorScheme.Foreground);

    a.line(position.x, centerY, position.x + width, centerY);

    a.noStroke();

    final var rectWidth = a.textWidth(text) + additionalPadding;

    a.fill(ColorScheme.Background);
    a.rectMode(PApplet.CENTER);
    a.rect(centerX, centerY, rectWidth, 40);

    a.fill(ColorScheme.Foreground);
    a.textAlign(PApplet.CENTER, PApplet.CENTER);

    DrawingUtil.smoothTextSize(a, 28);
    a.text(text, centerX, centerY);
  }
}
