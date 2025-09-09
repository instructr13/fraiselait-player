public class Size {
  public int width;
  public int height;

  public Size(int width, int height) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Size dimensions must be positive");
    }

    this.width = width;
    this.height = height;
  }
}
