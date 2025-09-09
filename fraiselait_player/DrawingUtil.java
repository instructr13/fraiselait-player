import processing.core.PApplet;
import processing.core.PFont;

import java.util.List;

public class DrawingUtil {
  private static final List<Integer> sizes = List.of(14, 18, 24, 28);
  private static List<PFont> fonts = null;

  public static void linearGradientLine(PApplet a, float x1, float y1, float x2, float y2, int c1, int c2, float colorStop) {
    final var steps = (int) PApplet.dist(x1, y1, x2, y2);

    a.pushStyle();

    a.beginShape(PApplet.LINES);

    for (int i = 0; i <= steps; i++) {
      float inter = PApplet.map(i, 0, steps, 0, 1);

      int c = a.lerpColor(c1, c2, PApplet.constrain(inter / colorStop, 0, 1));

      float x = PApplet.lerp(x1, x2, inter);
      float y = PApplet.lerp(y1, y2, inter);

      a.stroke(c);
      a.vertex(x, y);
    }

    a.endShape();

    a.popStyle();
  }

  public static void smoothTextSize(PApplet a, int size) {
    if (size <= 0 || !sizes.contains(size)) {
      throw new IllegalArgumentException("Size must be one of " + sizes);
    }

    if (fonts == null) {
      fonts = sizes.stream()
          .map(s -> a.loadFont("fonts/Sen-Regular-" + s + ".vlw"))
          .toList();
    }

    a.textFont(fonts.get(sizes.indexOf(size)));
    a.textSize(size);
  }

  public static float textWidthWithLetterSpacing(PApplet a, String text, float letterSpacingFactor) {
    if (text == null || text.isEmpty()) return 0;

    char[] chars = text.toCharArray();
    float totalWidth = 0;

    for (char c : chars) {
      totalWidth += a.textWidth(c) * (1 + letterSpacingFactor);
    }

    totalWidth -= a.textWidth(chars[chars.length - 1]) * letterSpacingFactor;

    return totalWidth;
  }

  public static void textWithLetterSpacing(PApplet a, String text, float x, float y, float letterSpacingFactor) {
    float totalWidth = textWidthWithLetterSpacing(a, text, letterSpacingFactor);

    if (totalWidth == 0) return;

    final var alignX = a.g.textAlign;
    float currentX = x;

    if (alignX == PApplet.CENTER) {
      currentX -= totalWidth / 2f;
    } else if (alignX == PApplet.RIGHT) {
      currentX -= totalWidth;
    }

    a.textAlign(PApplet.LEFT, a.g.textAlignY);

    for (char c : text.toCharArray()) {
      a.text(c, currentX, y);

      currentX += a.textWidth(c) * (1 + letterSpacingFactor);
    }

    a.textAlign(alignX, a.g.textAlignY);
  }
}
