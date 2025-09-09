import processing.core.PApplet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentsRegistry {
  private final PApplet a;

  private final List<Component> components = new ArrayList<>();
  private final Map<Class<?>, Component> resources = new HashMap<>();

  public ComponentsRegistry(PApplet a) {
    this.a = a;
  }

  public void register(Component component) {
    components.add(component);
  }

  public <T> void registerResource(T resource) {
    resources.put(resource.getClass(), (Component) resource);
  }

  public <T> T getResource(Class<T> clazz) {
    return clazz.cast(resources.get(clazz));
  }

  public void setup() {
    resources.values().forEach(c -> {
      a.pushStyle();

      c.setup(a, this);

      a.popStyle();
    });

    components.forEach(c -> {
      a.pushStyle();

      c.setup(a, this);

      a.popStyle();
    });
  }

  public void draw() {
    resources.values().forEach(c -> {
      a.pushStyle();

      c.draw(a, this);

      a.popStyle();
    });

    components.forEach(c -> {
      a.pushStyle();

      c.draw(a, this);

      a.popStyle();
    });
  }

  public void onKeyPressed() {
    resources.values().forEach(c -> c.keyPressed(a, this));
    components.forEach(c -> c.keyPressed(a, this));
  }

  public void onKeyReleased() {
    resources.values().forEach(c -> c.keyReleased(a, this));
    components.forEach(c -> c.keyReleased(a, this));
  }

  public void onMouseMoved() {
    resources.values().forEach(c -> c.mouseMoved(a, this));
    components.forEach(c -> c.mouseMoved(a, this));
  }

  public void onMousePressed() {
    resources.values().forEach(c -> c.mousePressed(a, this));
    components.forEach(c -> c.mousePressed(a, this));
  }

  public void onMouseDragged() {
    resources.values().forEach(c -> c.mouseDragged(a, this));
    components.forEach(c -> c.mouseDragged(a, this));
  }

  public void onMouseReleased() {
    resources.values().forEach(c -> c.mouseReleased(a, this));
    components.forEach(c -> c.mouseReleased(a, this));
  }

  public void onMouseClicked() {
    resources.values().forEach(c -> c.mouseClicked(a, this));
    components.forEach(c -> c.mouseClicked(a, this));
  }

  public void dispose() {
    components.forEach(Component::dispose);
    resources.values().forEach(Component::dispose);
  }
}
