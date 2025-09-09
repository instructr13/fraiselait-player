import dev.wycey.mido.fraiselait.builtins.WaveformType;
import processing.core.PApplet;
import processing.core.PImage;

import java.util.List;

class WaveformSelectionButton extends Button {
  private static final int SIZE = 100;
  private static final int BORDER_LENGTH = 10;

  private final ColorAnimator bgColorAnimator;
  private final ColorAnimator borderColorAnimator;

  private boolean isSelected;

  public WaveformSelectionButton(Point position, PApplet a, boolean isSelected, AnimationManager anim) {
    super(new Transform(position, new Size(SIZE, SIZE)));

    this.isSelected = isSelected;
    this.bgColorAnimator = new ColorAnimator(
        a,
        anim,
        isSelected ? ColorScheme.BackgroundActive : ColorScheme.Background
    );
    this.borderColorAnimator  = new ColorAnimator(
        a,
        anim,
        isSelected ? ColorScheme.Foreground : ColorScheme.Surface1
    );
  }

  public void setSelected(boolean selected) {
    this.isSelected = selected;
  }

  @Override
  public void draw(PApplet a) {
    if (isSelected) {
      bgColorAnimator.setTargetColor(ColorScheme.BackgroundActive);
    } else if (isHovered()) {
      bgColorAnimator.setTargetColor(ColorScheme.Surface1);
    } else {
      bgColorAnimator.setTargetColor(ColorScheme.Background);
    }

    borderColorAnimator.setTargetColor(isSelected ? ColorScheme.Foreground : ColorScheme.Surface1);

    final var bgColor = bgColorAnimator.getCurrentColor();
    final var borderColor = borderColorAnimator.getCurrentColor();

    a.noStroke();
    a.fill(bgColor);
    a.rect(transform.position.x, transform.position.y, transform.size.width, transform.size.height);

    if (!isSelected) return;

    a.stroke(borderColor);
    a.strokeWeight(2);

    // top-left corner
    a.line(
        transform.position.x,
        transform.position.y + BORDER_LENGTH,
        transform.position.x,
        transform.position.y
    );
    a.line(
        transform.position.x,
        transform.position.y,
        transform.position.x + BORDER_LENGTH,
        transform.position.y
    );

    // top-right corner
    a.line(
        transform.position.x + transform.size.width - BORDER_LENGTH,
        transform.position.y,
        transform.position.x + transform.size.width,
        transform.position.y
    );
    a.line(
        transform.position.x + transform.size.width,
        transform.position.y,
        transform.position.x + transform.size.width,
        transform.position.y + BORDER_LENGTH
    );

    // bottom-left corner
    a.line(
        transform.position.x,
        transform.position.y + transform.size.height - BORDER_LENGTH,
        transform.position.x,
        transform.position.y + transform.size.height
    );
    a.line(
        transform.position.x,
        transform.position.y + transform.size.height,
        transform.position.x + BORDER_LENGTH,
        transform.position.y + transform.size.height
    );

    // bottom-right corner
    a.line(
        transform.position.x + transform.size.width - BORDER_LENGTH,
        transform.position.y + transform.size.height,
        transform.position.x + transform.size.width,
        transform.position.y + transform.size.height
    );
    a.line(
        transform.position.x + transform.size.width,
        transform.position.y + transform.size.height,
        transform.position.x + transform.size.width,
        transform.position.y + transform.size.height - BORDER_LENGTH
    );
  }
}

public class WaveformSelector implements Component {
  private static final int IMAGE_SIZE = 75;
  private static final int IMAGE_MARGIN = 40;
  private static final WaveformType[] waveformTypes = {
      WaveformType.SQUARE,
      WaveformType.SQUARE_25,
      WaveformType.SQUARE_12,
      WaveformType.TRIANGLE,
      WaveformType.SAW,
      WaveformType.NOISE
  };

  private static List<PImage> waveformImages = null;

  public static void loadWaveformImages(PApplet a) {
    if (waveformImages == null) {
      waveformImages = List.of(
        a.loadImage("waveforms/Square.png"),
        a.loadImage("waveforms/Square25.png"),
        a.loadImage("waveforms/Square12.png"),
        a.loadImage("waveforms/Triangle.png"),
        a.loadImage("waveforms/Saw.png"),
        a.loadImage("waveforms/Noise.png")
      );
    }
  }

  private final Point position;
  private final OscillatorState state;

  private AnimationManager anim;
  private ButtonRenderer buttons;

  private WaveformSelectionButton selectedButton = null;

  public WaveformSelector(Point position, OscillatorState state) {
    this.position = position;
    this.state = state;
  }

  @Override
  public void setup(PApplet a, ComponentsRegistry r) {
    loadWaveformImages(a);

    anim = r.getResource(AnimationManager.class);
    buttons = r.getResource(ButtonRenderer.class);

    for (int i = 0; i < waveformTypes.length; i++) {
      int col = i % 3;
      int row = i / 3;

      int x = position.x + col * (IMAGE_SIZE + IMAGE_MARGIN) - 12;
      int y = position.y + row * (IMAGE_SIZE + IMAGE_MARGIN) - 12;

      final var isSelectedByDefault = state.getWaveformType() == waveformTypes[i];
      final var waveformType = waveformTypes[i];
      final var button = new WaveformSelectionButton(new Point(x, y), a, isSelectedByDefault, anim);

      if (isSelectedByDefault) {
        selectedButton = button;
      }

      buttons.addButton(button, b -> {
        state.setWaveformType(waveformType);

        if (selectedButton != null && selectedButton != button) {
          selectedButton.setSelected(false);
        }

        button.setSelected(true);

        selectedButton = button;
      });
    }
  }

  @Override
  public void draw(PApplet a, ComponentsRegistry r) {
    // place images to 3 columns
    for (int i = 0; i < waveformImages.size(); i++) {
      int col = i % 3;
      int row = i / 3;

      int x = position.x + col * (IMAGE_SIZE + IMAGE_MARGIN);
      int y = position.y + row * (IMAGE_SIZE + IMAGE_MARGIN);

      a.image(waveformImages.get(i), x, y, IMAGE_SIZE, IMAGE_SIZE);
    }

    // text
    a.fill(ColorScheme.ForegroundDim);
    DrawingUtil.smoothTextSize(a, 14);

    a.textAlign(PApplet.RIGHT, PApplet.CENTER);
    a.text(
        state.getWaveformType().getDisplayName(),
        position.x + 3 * (IMAGE_SIZE + IMAGE_MARGIN) - IMAGE_MARGIN,
        position.y - 25
    );
  }
}
