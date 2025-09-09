import processing.core.PApplet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class KnobRenderer implements Component {
  private final List<Knob> knobs = new ArrayList<>();
  private Knob hoveredKnob = null;
  private Knob activeKnob = null;
  private Point startDragPoint = null;

  public void addKnob(Knob knob, Consumer<Knob> onChange) {
    knobs.add(knob);

    knob.setOnChange(onChange);
  }

  public void addKnob(Knob knob) {
    addKnob(knob, k -> {});
  }

  @Override
  public void draw(PApplet a, ComponentsRegistry r) {
    for (var knob : knobs) {
      knob.draw(a);
    }
  }

  @Override
  public void mouseMoved(PApplet a, ComponentsRegistry r) {
    final var newHoveredKnob = knobs.stream()
        .filter(knob -> knob.check(new Point(a.mouseX, a.mouseY)))
        .findFirst()
        .orElse(null);

    if (newHoveredKnob != null) {
      a.cursor(PApplet.HAND);
    } else if (hoveredKnob != null) {
      a.cursor(PApplet.ARROW);
    }

    hoveredKnob = newHoveredKnob;
  }

  @Override
  public void mousePressed(PApplet a, ComponentsRegistry r) {
    if (a.mouseButton != PApplet.LEFT) {
      return;
    }

    if (hoveredKnob != null) {
      activeKnob = hoveredKnob;
      startDragPoint = new Point(a.mouseX, a.mouseY);
    }
  }

  @Override
  public void mouseDragged(PApplet a, ComponentsRegistry r) {
    if (a.mouseButton != PApplet.LEFT) {
      return;
    }

    if (activeKnob != null) {
      activeKnob.drag(new Point(a.mouseX - startDragPoint.x, a.mouseY - startDragPoint.y));
      startDragPoint = new Point(a.mouseX, a.mouseY);

      a.noCursor();
    }
  }

  @Override
  public void mouseReleased(PApplet a, ComponentsRegistry r) {
    if (a.mouseButton != PApplet.LEFT) {
      return;
    }

    if (activeKnob != null) {
      activeKnob = null;
      startDragPoint = null;

      a.cursor();
    }
  }
}
