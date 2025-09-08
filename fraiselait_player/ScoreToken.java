import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

enum ScoreKeywords {
  // Pre-processed keywords
  DEFINE("DEFINE"),

  // Commands
  USE("USE"),

  STOP("STOP"),
  REPLAY("REPLAY"),
  BPM("BPM"),
  MEASURE("MEASURE"),
  VOL("VOL"),
  PITCH("PITCH"),
  VIB("VIB"),

  END("END"),

  // Oscillator
  OSC("OSC")

  ;

  public static final Set<String> LITERALS =
      Arrays.stream(ScoreKeywords.values()).map(ScoreKeywords::getLiteral).collect(Collectors.toUnmodifiableSet());

  private final String literal;

  ScoreKeywords(String value) {
    this.literal = value;
  }

  public static boolean isKeyword(String value) {
    return LITERALS.contains(value);
  }

  public String getLiteral() {
    return literal;
  }
}

public abstract class ScoreToken<T> {
  private final String lexeme;
  private final Lazy<T> literal;
  private int lineNumber;
  private int position;

  protected ScoreToken(String lexeme, T literal, int lineNumber, int position) {
    this.lexeme = lexeme;
    this.literal = new Lazy<>(literal);
    this.lineNumber = lineNumber;
    this.position = position;
  }

  protected ScoreToken(String lexeme, int lineNumber, int position) {
    this.lexeme = lexeme;
    this.literal = new Lazy<>();
    this.lineNumber = lineNumber;
    this.position = position;
  }

  public String getLexeme() {
    return lexeme;
  }

  public T getLiteral() {
    return literal.get();
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public int getPosition() {
    return position;
  }

  public ScoreToken<T> withPosition(int lineNumber, int position) {
    this.lineNumber = lineNumber;
    this.position = position;

    return this;
  }

  @Override
  public String toString() {
    return String.format("%s(%s at %d:%d)", getClass().getSimpleName(), lexeme, lineNumber, position);
  }

  public static class KeywordToken extends ScoreToken<ScoreKeywords> {
    public KeywordToken(String value, int lineNumber, int position) {
      super(value.toUpperCase(), lineNumber, position);

      if (!ScoreKeywords.LITERALS.contains(value.toUpperCase())) {
        throw new ScoreParseException("Invalid keyword: " + value, lineNumber, position);
      }

      super.literal.trySet(ScoreKeywords.valueOf(value.toUpperCase()));
    }

    public static boolean isKeyword(String value) {
      return ScoreKeywords.isKeyword(value.toUpperCase());
    }
  }

  public static class NoteToken extends ScoreToken<Note> {
    public NoteToken(String value, int lineNumber, int position) {
      super(value, lineNumber, position);

      if (!isNote(value)) {
        throw new ScoreParseException("Invalid note format: " + value, lineNumber, position);
      }

      super.literal.trySet(Note.parse(value));
    }

    public static boolean isNote(String value) {
      return Note.NOTE_PATTERN.matcher(value).matches();
    }
  }

  public static class NumberToken extends ScoreToken<Number> {
    public NumberToken(String value, int lineNumber, int position) {
      super(value, lineNumber, position);

      if (!isNumber(value)) {
        throw new ScoreParseException("Invalid number format: " + value, lineNumber, position);
      }

      // parse double to Number
      super.literal.trySet(Double.parseDouble(value));
    }

    public static boolean isNumber(String value) {
      try {
        Double.parseDouble(value);

        return true;
      } catch (NumberFormatException e) {
        return false;
      }
    }
  }

  public static class RestToken extends ScoreToken<Void> {
    private static final String REST_SYMBOL = "---";

    public RestToken(int lineNumber, int position) {
      super(REST_SYMBOL, null, lineNumber, position);
    }

    public static boolean isRest(String value) {
      return REST_SYMBOL.equals(value);
    }
  }

  public static class IdentifierToken extends ScoreToken<Void> {
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_\\-]*$");

    public IdentifierToken(String value, int lineNumber, int position) {
      super(value, null, lineNumber, position);

      if (!IDENTIFIER_PATTERN.matcher(value).matches()) {
        throw new ScoreParseException("Invalid identifier format: " + value, lineNumber, position);
      }
    }

    public static boolean isIdentifier(String value) {
      return IDENTIFIER_PATTERN.matcher(value).matches();
    }
  }

  public static class CommaToken extends ScoreToken<Void> {
    private static final String COMMA_SYMBOL = ",";

    public CommaToken(int lineNumber, int position) {
      super(COMMA_SYMBOL, null, lineNumber, position);
    }

    public static boolean isComma(String value) {
      return COMMA_SYMBOL.equals(value);
    }
  }

  public static class SemicolonToken extends ScoreToken<Void> {
    private static final String SEMICOLON_SYMBOL = ";";

    public SemicolonToken(int lineNumber, int position) {
      super(SEMICOLON_SYMBOL, null, lineNumber, position);
    }

    public static boolean isSemicolon(String value) {
      return SEMICOLON_SYMBOL.equals(value);
    }
  }

  public static class NewlineToken extends ScoreToken<Void> {
    private static final List<String> NEWLINE_SYMBOLS = List.of("\n");

    public NewlineToken(String value, int lineNumber) {
      super(value, null, lineNumber, 0);

      if (!isNewline(value)) {
        throw new ScoreParseException("Invalid newline symbol: " + value, lineNumber, 0);
      }
    }

    public static boolean isNewline(String value) {
      return NEWLINE_SYMBOLS.contains(value);
    }
  }

  public static class EOFToken extends ScoreToken<Void> {
    public EOFToken(int lineNumber, int position) {
      super("EOF", null, lineNumber, position);
    }
  }

  // Special Token: not created by lexer but by parser context

  public static class FrequencyToken extends ScoreToken<Frequency> {
    private FrequencyToken(String value, Frequency freq, int lineNumber, int position) {
      super(value, freq, lineNumber, position);
    }

    public FrequencyToken(NoteToken noteToken) {
      this(noteToken.getLexeme(), new Frequency.NoteFrequency(noteToken.getLiteral()), noteToken.getLineNumber(), noteToken.getPosition());
    }

    public FrequencyToken(NumberToken numberToken) {
      this(numberToken.getLexeme(), new Frequency.RawFrequency(numberToken.getLiteral().doubleValue()), numberToken.getLineNumber(), numberToken.getPosition());
    }
  }
}
