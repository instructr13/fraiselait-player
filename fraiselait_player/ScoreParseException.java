public class ScoreParseException extends RuntimeException {
  private final int line;
  private final int position;

  public ScoreParseException(String message, int line, int position) {
    super(line == 0 && position == 0 ? message : "%s at line %d, position %d".formatted(message, line, position));
    this.line = line;
    this.position = position;
  }

  public int getLine() {
    return line;
  }

  public int getPosition() {
    return position;
  }
}
