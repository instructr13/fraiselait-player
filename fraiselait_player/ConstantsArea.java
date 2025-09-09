import processing.core.PApplet;

public class ConstantsArea implements Component {
  private static final int HEIGHT = 50;

  private final Point position;

  public ConstantsArea(Point position) {
    this.position = position;
  }

  @Override
  public void draw(PApplet a, ComponentsRegistry r) {
    a.strokeWeight(2);
    a.stroke(ColorScheme.Surface1);

    a.line(position.x, position.y, position.x, position.y + HEIGHT);

    a.noStroke();
    a.fill(ColorScheme.Foreground);
    DrawingUtil.smoothTextSize(a, 28);

    a.textAlign(PApplet.LEFT, PApplet.CENTER);
    a.text(Double.toString(Notes.A0_FREQ), position.x + 15, position.y + 15);

    a.fill(ColorScheme.ForegroundMuted);
    DrawingUtil.smoothTextSize(a, 14);
    DrawingUtil.textWithLetterSpacing(a,
        "A0 FREQ.",
        position.x + 15,
        position.y + 40,
        0.6f
        );
  }
}
