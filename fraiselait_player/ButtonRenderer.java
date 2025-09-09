import processing.core.PApplet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ButtonRenderer implements Component {
  private final List<Button> buttons = new ArrayList<>();
  private Button hoveredButton = null;

  public void addButton(Button button, Consumer<Button> onClick) {
    buttons.add(button);

    button.setOnClick(onClick);
  }

  public void addButton(Button button) {
    addButton(button, b -> {});
  }

  @Override
  public void draw(PApplet a, ComponentsRegistry r) {
    for (var button : buttons) {
      button.draw(a);
    }
  }

  @Override
  public void mouseMoved(PApplet a, ComponentsRegistry r) {
    final var newHoveredButton = buttons.stream()
        .filter(button -> button.check(new Point(a.mouseX, a.mouseY)))
        .findFirst()
        .orElse(null);

    if (newHoveredButton != null) {
      a.cursor(PApplet.HAND);

      newHoveredButton.setHovered(true);
    } else if (hoveredButton != null) {
      a.cursor(PApplet.ARROW);

      hoveredButton.setHovered(false);
    }

    hoveredButton = newHoveredButton;
  }

  @Override
  public void mouseClicked(PApplet a, ComponentsRegistry r) {
    if (hoveredButton != null) {
      hoveredButton.click();
    }
  }
}
