import java.util.List;

class ListCursor<T> {
  private final List<T> inner;
  private final int globalCursor;
  private final int localCursor;

  ListCursor(List<T> inner, int globalCursor, int localCursor) {
    this.inner = inner;
    this.globalCursor = globalCursor;
    this.localCursor = localCursor;
  }

  public List<T> get() { return inner; }

  public int getGlobalCursor() { return globalCursor; }

  public int getLocalCursor() { return localCursor; }
}

public class Utils {
  static <T> ListCursor<T> aroundList(List<T> list, int i, int amount) {
    final var halfAmount = (amount - 1) / 2;
    var start = Math.max(0, Math.min(i - halfAmount, list.size() - amount));
    final var end = Math.min(list.size(), start + amount);

    start = Math.max(0, end - amount);

    return new ListCursor(list.subList(start, end), i, i - start);
  }
}
