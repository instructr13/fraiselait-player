public class Transform {
  public final Point position;
  public final Size size;

  public Transform(Point position, Size size) {
    this.position = position;
    this.size = size;
  }

  public boolean contains(Point point) {
    return point.x >= position.x && point.x <= position.x + size.width &&
           point.y >= position.y && point.y <= position.y + size.height;
  }
}
