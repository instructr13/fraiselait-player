import processing.core.PApplet;

public class GroupHeader implements Component {
  private static final int BOX_WIDTH = 30;
  private static final int TEXT_LEFT_MARGIN = 15;

  public String title;
  public Point position;
  public int height;

  public GroupHeader(String title, Point position, int height) {
    this.title = title;
    this.position = position;
    this.height = height;
  }

  @Override
  public void draw(PApplet a, ComponentsRegistry r) {
    a.stroke(ColorScheme.Foreground);

    a.line(position.x, position.y, position.x, position.y + height);
    a.line(position.x, position.y, position.x + BOX_WIDTH, position.y);
    a.line(position.x, position.y + height, position.x + BOX_WIDTH, position.y + height);

    a.noStroke();
    a.fill(ColorScheme.Foreground);

    DrawingUtil.smoothTextSize(a, 24);
    a.textAlign(PApplet.LEFT, PApplet.CENTER);
    DrawingUtil.textWithLetterSpacing(a, title, position.x + BOX_WIDTH + TEXT_LEFT_MARGIN, position.y, 0.25f);
  }
}
