import processing.core.PApplet;

import java.util.function.Consumer;

public abstract class Button {
  protected Transform transform;
  private Consumer<Button> onClick;

  protected boolean hovered = false;

  public Button(Transform transform) {
    this.transform = transform;
  }

  public Transform getTransform() {
    return transform;
  }

  public void setOnClick(Consumer<Button> onClick) {
    this.onClick = onClick;
  }

  public boolean isHovered() {
    return hovered;
  }

  public void setHovered(boolean hovered) {
    this.hovered = hovered;
  }

  public void click() {
    if (onClick != null) {
      onClick.accept(this);
    }
  }

  public boolean check(Point point) {
    return transform.contains(point);
  }

  public abstract void draw(PApplet a);
}
