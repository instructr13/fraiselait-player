import processing.core.PApplet;

public interface Component {
  default void setup(PApplet a, ComponentsRegistry r) {}

  default void draw(PApplet a, ComponentsRegistry r) {}

  default void keyPressed(PApplet a, ComponentsRegistry r) {}

  default void keyReleased(PApplet a, ComponentsRegistry r) {}

  default void mouseMoved(PApplet a, ComponentsRegistry r) {}

  default void mousePressed(PApplet a, ComponentsRegistry r) {}

  default void mouseDragged(PApplet a, ComponentsRegistry r) {}

  default void mouseReleased(PApplet a, ComponentsRegistry r) {}

  default void mouseClicked(PApplet a, ComponentsRegistry r) {}

  default void dispose() {}
}
