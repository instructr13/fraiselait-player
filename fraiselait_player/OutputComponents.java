import processing.core.PApplet;

class VolumeSliderKnob extends Knob {
  public VolumeSliderKnob(Point position, int width, OscillatorState state) {
    super(new Transform(position, new Size(width, 24)), 0, 1, state.getVol(), false);
  }

  @Override
  public void draw(PApplet a) {
    a.noStroke();
    a.fill(ColorScheme.Surface2);

    a.rect(transform.position.x, transform.position.y, transform.size.width, transform.size.height);

    // Side border
    a.stroke(ColorScheme.Surface1);

    a.line(
        transform.position.x, transform.position.y,
        transform.position.x + transform.size.width, transform.position.y
    );

    a.line(
        transform.position.x, transform.position.y + transform.size.height,
        transform.position.x + transform.size.width, transform.position.y + transform.size.height
    );

    a.noStroke();

    // Draw filled progress bar
    final var knobX = (float) (transform.position.x + (currentValue - minValue) / (maxValue - minValue) * transform.size.width);

    a.fill(ColorScheme.Foreground, (float) (255 * (currentValue - minValue) / (maxValue - minValue)));
    a.rect(transform.position.x, transform.position.y, knobX - transform.position.x, transform.size.height);

    a.fill(ColorScheme.ForegroundMuted);
    DrawingUtil.smoothTextSize(a, 14);
    a.textAlign(PApplet.LEFT, PApplet.CENTER);
    DrawingUtil.textWithLetterSpacing(
        a,
        "VOLUME",
        transform.position.x, transform.position.y - transform.size.height / 2f - 6,
        0.5f
    );

    final var percentage = (int) Math.round(currentValue * 100);
    final var percentageText = percentage + "%";

    a.fill(ColorScheme.Foreground);
    a.textAlign(PApplet.RIGHT, PApplet.CENTER);
    DrawingUtil.textWithLetterSpacing(
        a,
        percentageText,
        transform.position.x + transform.size.width, transform.position.y - transform.size.height / 2f - 6,
        0.5f
    );
  }
}

public class OutputComponents implements Component {
  private final Point position;
  private final OscillatorState state;

  private KnobRenderer knobs;

  public OutputComponents(Point position, OscillatorState state) {
    this.position = position;
    this.state = state;
  }

  @Override
  public void setup(PApplet a, ComponentsRegistry r) {
    knobs = r.getResource(KnobRenderer.class);

    knobs.addKnob(new VolumeSliderKnob(new Point(position.x, position.y + 65), 260, state), v -> {
      state.setVol(v.currentValue);
    });
  }

  @Override
  public void draw(PApplet a, ComponentsRegistry r) {
    final var actualVol = state.getCurrentActualVol();

    a.noStroke();
    a.fill(ColorScheme.Foreground, (float) (255 * actualVol));

    a.rect(position.x, position.y, 260, 24);
  }
}
