import dev.wycey.mido.fraiselait.builtins.orchestrators.MultiDeviceOrchestrator;
import processing.core.PApplet;

import java.util.Objects;

public class DevicesComponents implements Component {
  private static final int GAP = 10;
  private static final int HEIGHT = 80;

  private final ColorAnimator[] bgColorAnimators = new ColorAnimator[4];
  private final ColorAnimator[] borderColorAnimators = new ColorAnimator[4];

  private final Point position;
  private final int width;
  private final MultiDeviceOrchestrator orchestrator;

  private AnimationManager anim;

  public DevicesComponents(Point position, int width, MultiDeviceOrchestrator orchestrator) {
    this.position = position;
    this.width = width;
    this.orchestrator = orchestrator;
  }

  @Override
  public void setup(PApplet a, ComponentsRegistry r) {
    anim = r.getResource(AnimationManager.class);

    for (int i = 0; i < 4; i++) {
      bgColorAnimators[i] = new ColorAnimator(a, anim, ColorScheme.Surface3);
      borderColorAnimators[i] = new ColorAnimator(a, anim, ColorScheme.Surface1);
    }
  }

  @Override
  public void draw(PApplet a, ComponentsRegistry r) {
    var devices = orchestrator.getDevices().stream().toList();

    // max 4, 2x2 grid, fill gray when no device
    int maxDevices = Math.min(devices.size(), 4);

    for (int i = 0; i < 4; i++) {
      int col = i % 2;
      int row = i / 2;

      int x = position.x + col * (width / 2 + GAP);
      int y = position.y + row * (HEIGHT + GAP);

      a.strokeWeight(2);

      final var bgColorAnimator = bgColorAnimators[i];
      final var borderColorAnimator = borderColorAnimators[i];

      bgColorAnimator.setTargetColor(i < maxDevices ? ColorScheme.GreenSurface2 : ColorScheme.Surface3);
      borderColorAnimator.setTargetColor(i < maxDevices ? ColorScheme.GreenBorder : ColorScheme.Surface1);

      a.fill(bgColorAnimator.getCurrentColor());
      a.stroke(borderColorAnimator.getCurrentColor());
      a.rect(x, y, width / 2f, HEIGHT);

      if (i < maxDevices) {
        a.noStroke();

        var device = devices.get(i);

        a.fill(ColorScheme.ForegroundMuted);
        DrawingUtil.smoothTextSize(a, 14);
        a.textAlign(PApplet.LEFT, PApplet.CENTER);

        a.text("port", x + 15, y + HEIGHT / 2f - 10);
        a.text("id", x + 15, y + HEIGHT / 2f + 10);

        a.fill(ColorScheme.Foreground);
        a.textAlign(PApplet.RIGHT, PApplet.CENTER);

        // ellipsis if long
        var devicePort = device.getPort();

        if (Objects.requireNonNull(devicePort).length() > 16) {
          devicePort = "..." + devicePort.substring(devicePort.length() - 13);
        }

        a.text(devicePort, x + width / 2f - 15, y + HEIGHT / 2f - 10);
        a.text("0x" + Objects.requireNonNull(device.getId()).toUpperCase(), x + width / 2f - 15, y + HEIGHT / 2f + 10);
      }
    }

    a.noStroke();
  }
}
