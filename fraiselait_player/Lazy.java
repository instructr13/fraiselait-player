import java.util.Objects;

class Lazy<T> {
  // Must be volatile to ensure thread safety
  private volatile LazyValue<T> wrapper;

  public Lazy() {
  }

  public Lazy(T value) throws IllegalAccessError {
    set(value);
  }

  public T get() {
    if (wrapper == null) {
      return null;
    }

    return wrapper.value;
  }

  public void set(T value) throws IllegalAccessError {
    if (wrapper != null) {
      throw new IllegalAccessError("Cannot set value of Lazy more than once");
    }

    synchronized (this) {
      if (wrapper != null) {
        throw new IllegalAccessError("Cannot set value of Lazy more than once");
      }

      wrapper = new LazyValue<>(value);
    }
  }

  public void trySet(T value) {
    if (wrapper == null) {
      synchronized (this) {
        if (wrapper == null) {
          wrapper = new LazyValue<>(value);
        }
      }
    }
  }

  public boolean isSet() {
    return wrapper != null;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    final var other = (Lazy<?>) obj;

    return Objects.equals(get(), other.get());
  }

  @Override
  public int hashCode() {
    return Objects.hash(get());
  }

  static final private class LazyValue<V> {
    private final V value;

    public LazyValue(V value) {
      this.value = value;
    }
  }
}
