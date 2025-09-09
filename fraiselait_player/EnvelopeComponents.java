import processing.core.PApplet;

class EnvelopeVolumeKnob extends Knob {
  private final boolean isStart;

  public EnvelopeVolumeKnob(Point position, int height, boolean isStart, OscillatorState state) {
    super(new Transform(position, new Size(20, height)), 0, 1, isStart ? state.getStartVol() : state.getEndVol(), true);

    this.isStart = isStart;
  }

  @Override
  public void draw(PApplet a) {
    a.noStroke();
    a.fill(ColorScheme.Surface2);

    a.rect(transform.position.x, transform.position.y, transform.size.width, transform.size.height);

    a.fill(ColorScheme.Accent);

    final var knobY = (float) (transform.position.y + (1 - currentValue) * (transform.size.height - 4));
    a.rect(transform.position.x, knobY, transform.size.width, 4);

    a.stroke(ColorScheme.ForegroundDim);

    if (isStart) {
      a.line(
          transform.position.x + transform.size.width, transform.position.y,
          transform.position.x + transform.size.width,
          transform.position.y + transform.size.height
      );
    } else {
      a.line(
          transform.position.x, transform.position.y,
          transform.position.x,
          transform.position.y + transform.size.height
      );
    }

    a.fill(ColorScheme.ForegroundMuted);
    DrawingUtil.smoothTextSize(a, 14);
    a.textAlign(PApplet.CENTER, PApplet.CENTER);

    DrawingUtil.textWithLetterSpacing(
        a,
        isStart ? "START" : "END",
        transform.position.x + transform.size.width / 2f, transform.position.y - 15,
        0.6f
    );
  }
}

class EnvelopeGraph extends Knob {
  private static final int VERTICAL_GRIDS = 5;
  private static final int HORIZONTAL_GRIDS = 3;

  private final OscillatorState state;

  public EnvelopeGraph(Point position, Size size, OscillatorState state) {
    super(new Transform(position, size), 0.1, 10, 1, true);

    this.state = state;
  }

  @Override
  public void draw(PApplet a) {
    // Grid
    for (int i = 1; i <= VERTICAL_GRIDS; i++) {
      final var y = PApplet.map(i, 0, VERTICAL_GRIDS + 1, transform.position.y, transform.position.y + transform.size.height);

      DrawingUtil.linearGradientLine(
          a,
          transform.position.x, y,
          transform.position.x + transform.size.width, y,
          ColorScheme.Surface1,
          ColorScheme.Background,
          0.9f
      );
    }

    for (int i = 0; i <= HORIZONTAL_GRIDS; i++) {
      final var x = PApplet.map(i, 0, HORIZONTAL_GRIDS + 1, transform.position.x, transform.position.x + transform.size.width);

      DrawingUtil.linearGradientLine(
          a,
          x, transform.position.y,
          x, transform.position.y + transform.size.height,
          ColorScheme.Background,
          ColorScheme.Surface1,
          0.25f
      );
    }

    // Graph
    // transform x = t, y = startVol * (1 - (t/d)^gamma) + endVol * (t/d)^gamma

    final var samples = 100;
    final var xs = new float[samples];
    final var ys = new float[samples];

    final var startVol = state.getStartVol();
    final var endVol = state.getEndVol();
    final var d = transform.size.width;
    final var gamma = (float) currentValue;

    for (int i = 0; i < samples; i++) {
      final var t = PApplet.map(i, 0, samples - 1, 0, transform.size.width);
      final var vol = (float) (startVol * (1 - Math.pow(t / d, gamma)) + endVol * Math.pow(t / d, gamma));
      final var y = PApplet.map(vol, 0, 1, transform.position.y + transform.size.height, transform.position.y);

      xs[i] = transform.position.x + t;
      ys[i] = y;
    }

    a.noFill();
    a.stroke(ColorScheme.Accent);
    a.strokeWeight(2);
    a.beginShape();

    for (int i = 0; i < samples; i++) {
      a.vertex(xs[i], ys[i]);
    }

    a.endShape();

    a.strokeWeight(1);
    a.noStroke();
    a.fill(ColorScheme.Accent, 48);
    a.beginShape();
    a.vertex(transform.position.x, transform.position.y + transform.size.height);

    for (int i = 0; i < samples; i++) {
      a.vertex(xs[i], ys[i]);
    }

    a.vertex(transform.position.x + transform.size.width, transform.position.y + transform.size.height);
    a.endShape(PApplet.CLOSE);

    // Axis
    a.stroke(ColorScheme.ForegroundSub);

    // X axis
    a.line(
        transform.position.x, transform.position.y + transform.size.height,
        transform.position.x + transform.size.width, transform.position.y + transform.size.height
    );

    // Y axis
    a.line(
        transform.position.x, transform.position.y,
        transform.position.x, transform.position.y + transform.size.height
    );

    // T line
    if (state.hasDurationProgress()) {
      final var t = PApplet.map(state.getDurationProgress().floatValue(), 0, 1, 0, transform.size.width);

      a.stroke(ColorScheme.ForegroundDim);
      a.line(
          transform.position.x + t, transform.position.y,
          transform.position.x + t, transform.position.y + transform.size.height
      );
    }

    // Caption Text
    a.fill(ColorScheme.ForegroundMuted);
    DrawingUtil.smoothTextSize(a, 14);
    a.textAlign(PApplet.CENTER, PApplet.CENTER);
    DrawingUtil.textWithLetterSpacing(
        a,
        "CURVE",
        transform.position.x + transform.size.width / 2f, transform.position.y - 20,
        0.6f
    );

    final var duration = state.getDuration();

    // Duration Text
    a.fill(ColorScheme.Foreground);
    DrawingUtil.smoothTextSize(a, 14);
    a.textAlign(PApplet.CENTER, PApplet.CENTER);
    a.text(
        String.format("%d ms", duration),
        transform.position.x + transform.size.width / 2f,
        transform.position.y + transform.size.height + 15
    );
  }
}

class EnvelopeDurationKnob extends Knob {
  // 10-1000ms
  private static final int FIRST_STAGE_COLOR = ColorScheme.ForegroundMuted;

  // 1000ms-10000ms
  private static final int SECOND_STAGE_COLOR = ColorScheme.Foreground;

  public EnvelopeDurationKnob(Point position, int height, OscillatorState state) {
    super(new Transform(position, new Size(24, height)), 10, 10000, state.getDuration(), true, v -> {
      if (v < 1000) return 0.05;

      return 0.5;
    });
  }

  @Override
  public void draw(PApplet a) {
    a.noStroke();

    // if currentValue < 1000, draw in normal background color
    if (currentValue < 1000) {
      a.fill(ColorScheme.Surface2);
    } else {
      a.fill(FIRST_STAGE_COLOR);
    }

    a.rect(transform.position.x, transform.position.y, transform.size.width, transform.size.height);

    // Side border
    if (currentValue < 1000) {
      a.stroke(ColorScheme.Surface1);

      a.line(
          transform.position.x, transform.position.y,
          transform.position.x, transform.position.y + transform.size.height - 1
      );

      a.line(
          transform.position.x + transform.size.width - 1, transform.position.y,
          transform.position.x + transform.size.width - 1, transform.position.y + transform.size.height - 1
      );

      a.noStroke();
    }

    // if currentValue < 1000, draw in first stage color
    if (currentValue < 1000) {
      a.fill(FIRST_STAGE_COLOR);
    } else {
      a.fill(SECOND_STAGE_COLOR);
    }

    final float knobY;

    if (currentValue < 1000) {
      knobY = (float) (transform.position.y + (1 - (currentValue - 10) / (1000 - 10)) * (transform.size.height - 4));
    } else {
      knobY = (float) (transform.position.y + (1 - (currentValue - 1000) / (10000 - 1000)) * (transform.size.height - 4));
    }

    // Draw filled progress bar
    a.rect(transform.position.x, knobY, transform.size.width, transform.size.height - (knobY - transform.position.y));

    a.fill(ColorScheme.ForegroundMuted);
    DrawingUtil.smoothTextSize(a, 14);
    a.textAlign(PApplet.CENTER, PApplet.CENTER);
    DrawingUtil.textWithLetterSpacing(
        a,
        "D",
        transform.position.x + transform.size.width / 2f, transform.position.y + transform.size.height + 15,
        0.6f
    );
  }
}

class QualityKnob extends Knob {
  public QualityKnob(Point position, int height, OscillatorState state) {
    super(new Transform(position, new Size(24, height)), 10, 5000, state.getQuality(), true);
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
        transform.position.x, transform.position.y + transform.size.height - 1
    );

    a.line(
        transform.position.x + transform.size.width - 1, transform.position.y,
        transform.position.x + transform.size.width - 1, transform.position.y + transform.size.height - 1
    );

    a.noStroke();

    // Draw filled progress bar
    final var knobY = (float) (transform.position.y + (1 - (currentValue - minValue) / (maxValue - minValue)) * (transform.size.height - 4));

    a.fill(ColorScheme.Foreground);
    a.rect(transform.position.x, knobY, transform.size.width, transform.size.height - (knobY - transform.position.y));

    a.fill(ColorScheme.ForegroundMuted);
    DrawingUtil.smoothTextSize(a, 14);
    a.textAlign(PApplet.CENTER, PApplet.CENTER);
    DrawingUtil.textWithLetterSpacing(
        a,
        "Q",
        transform.position.x + transform.size.width / 2f, transform.position.y + transform.size.height + 15,
        0.6f
    );
  }
}

public class EnvelopeComponents implements Component {
  private static final String[] props = {
      "start_vol", "end_vol", "gamma", "duration", "quality"
  };

  private static final int PROPS_MARGIN_TOP = 12;

  private final Point position;
  private final OscillatorState state;

  private KnobRenderer knobs;

  public EnvelopeComponents(Point position, OscillatorState state) {
    this.position = position;
    this.state = state;
  }

  @Override
  public void setup(PApplet a, ComponentsRegistry r) {
    knobs = r.getResource(KnobRenderer.class);

    final var envelopeStart = new EnvelopeVolumeKnob(new Point(position.x, position.y), 190, true, state);
    final var envelopeGraph = new EnvelopeGraph(new Point(position.x + 40, position.y), new Size(400, 190), state);
    final var envelopeEnd = new EnvelopeVolumeKnob(new Point(position.x + 460, position.y), 190, false, state);
    final var envelopeDuration = new EnvelopeDurationKnob(new Point(position.x + 490, position.y), 190, state);
    final var qualityKnob = new QualityKnob(new Point(position.x + 525, position.y), 190, state);

    knobs.addKnob(envelopeStart, k -> state.setStartVol(k.getCurrentValue()));
    knobs.addKnob(envelopeGraph, k -> state.setGamma(k.getCurrentValue()));
    knobs.addKnob(envelopeEnd, k -> state.setEndVol(k.getCurrentValue()));
    knobs.addKnob(envelopeDuration, k -> state.setDuration((int) k.getCurrentValue()));
    knobs.addKnob(qualityKnob, k -> state.setQuality((int) k.getCurrentValue()));
  }

  @Override
  public void draw(PApplet a, ComponentsRegistry r) {
    // PROPS
    a.fill(ColorScheme.ForegroundMuted);
    DrawingUtil.smoothTextSize(a, 14);
    a.textAlign(PApplet.LEFT, PApplet.CENTER);
    DrawingUtil.textWithLetterSpacing(a, "PROPS", position.x + 570, position.y - 15, 0.6f);

    // Key
    a.fill(ColorScheme.ForegroundDim);

    for (int i = 0; i < props.length; i++) {
      DrawingUtil.textWithLetterSpacing(
          a,
          props[i],
          position.x + 570,
          position.y + PROPS_MARGIN_TOP + i * 30,
          0.2f
      );
    }

    // Value
    a.textAlign(PApplet.RIGHT, PApplet.CENTER);
    a.fill(ColorScheme.Foreground);

    for (int i = 0; i < props.length; i++) {
      String value;

      switch (i) {
        case 0:
          value = String.format("%.2f", state.getStartVol());

          break;

        case 1:
          value = String.format("%.2f", state.getEndVol());

          break;

        case 2:
          value = String.format("%.2f", state.getGamma());

          break;

        case 3:
          value = String.format("%d", state.getDuration());

          break;

        case 4:
          value = String.format("%d", state.getQuality());

          break;

        default:
          throw new IllegalStateException("Unexpected value: " + i);
      }

      DrawingUtil.textWithLetterSpacing(
          a,
          value,
          position.x + 740,
          position.y + PROPS_MARGIN_TOP + i * 30,
          0.2f
      );
    }
  }
}
