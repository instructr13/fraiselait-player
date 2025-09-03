import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScoreLexer {
  private final String source;
  private final List<ScoreToken<?>> tokens = new ArrayList<>();
  private final Map<String, String> definitions = new HashMap<>();
  private int position = 0;
  private int startPosition = 0;
  private int line = 1;
  private int startLinePosition = 0;
  private int linePosition = 0;

  public ScoreLexer(String source) {
    this.source = source;
  }

  public List<ScoreToken<?>> tokenize() {
    while (!isAtEnd()) {
      startPosition = position;
      startLinePosition = linePosition;

      scanToken();
    }

    tokens.add(new ScoreToken.EOFToken(line, linePosition));

    return tokens;
  }

  private void scanToken() {
    final var c = peek();

    if (Character.isWhitespace(c)) {
      if (ScoreToken.NewlineToken.isNewline(String.valueOf(c))) {
        addToken(new ScoreToken.NewlineToken(line));

        line++;
        linePosition = 0;
      }

      advance();

      return;
    }

    if (c == '#') {
      skipComment();

      return;
    }

    if (ScoreToken.CommaToken.isComma(String.valueOf(c))) {
      addToken(new ScoreToken.CommaToken(line, linePosition));

      advance();

      return;
    }

    if (c == '-') {
      advance();

      if (match('-') && match('-')) {
        addToken(new ScoreToken.RestToken(line, startLinePosition));
      } else {
        // TODO This may be false positive
        //if (!processNote(-1)) throw new ScoreParseException("Invalid note format", line, linePosition);
      }

      return;
    }

    if (Character.isDigit(c)) {
      number();

      return;
    }

    if (Character.isLetter(c)) {
      identifier();

      return;
    }

    throw new ScoreParseException("Unexpected character: " + c, line, linePosition);
  }

  private boolean processNote(int offset) {
    // Rewind and re-scan the note
    final var actualStartPosition = startPosition + offset; // -1 to re-scan the first '-'
    final var oldPosition = position;

    position = actualStartPosition;

    while (Character.isLetterOrDigit(peek()) || peek() == '-' || peek() == '#') {
      advance();
    }

    var text = source.substring(actualStartPosition, position);

    // Replace defined symbols
    text = resolveDefinition(text);

    if (!ScoreToken.NoteToken.isNote(text)) {
      position = oldPosition; // roll back

      return false;
    }

    addToken(new ScoreToken.NoteToken(text, line, startLinePosition));

    return true;

  }

  private boolean processNote() {
    return processNote(0);
  }

  private void identifier() {
    while (Character.isLetterOrDigit(peek()) || peek() == '-' || peek() == '_') {
      advance();
    }

    var text = source.substring(startPosition, position);

    // Replace defined symbols
    text = resolveDefinition(text);

    if (ScoreToken.KeywordToken.isKeyword(text)) {
      addToken(new ScoreToken.KeywordToken(text, line, startLinePosition));

      return;
    }

    if (processNote())
      return;

    addToken(new ScoreToken.IdentifierToken(text, line, linePosition));
  }

  private String resolveDefinition(String text) {
    if (definitions.containsKey(text)) {
      // Detect recursive definition
      if (isProcessingDefinition(text)) {
        throw new ScoreParseException("Recursive definition detected: " + text, line, startLinePosition);
      }

      return resolveDefinition(definitions.get(text));
    }

    return text;
  }

  private boolean isProcessingDefinition(String name) {
    final var value = definitions.get(name);

    if (value == null) return false;

    if (definitions.containsKey(value)) {
      return value.equals(name) || isProcessingDefinition(value);
    }

    return false;
  }

  private void number() {
    while (Character.isDigit(peek())) {
      advance();
    }

    // Process decimal point
    if (peek() == '.' && Character.isDigit(peekNext())) {
      // Consume the '.'
      do {
        advance();
      } while (Character.isDigit(peek()));
    }

    var text = source.substring(startPosition, position);

    // Replace defined symbols
    text = resolveDefinition(text);

    addToken(new ScoreToken.NumberToken(text, line, startLinePosition));
  }

  private boolean match(char expected) {
    if (isAtEnd()) return false;
    if (source.charAt(position) != expected) return false;

    position++;

    return true;
  }

  private char peek() {
    if (isAtEnd()) return '\0';

    return source.charAt(position);
  }

  private char peekNext() {
    if (position + 1 >= source.length()) return '\0';

    return source.charAt(position + 1);
  }

  private char advance() {
    linePosition++;

    return source.charAt(position++);
  }

  private void addToken(ScoreToken<?> token) {
    // Process DEFINE keyword
    if (token instanceof ScoreToken.KeywordToken && token.getLiteral() == ScoreKeywords.DEFINE) {
      processDefine();

      return;
    }

    tokens.add(token);
  }

  private void processDefine() {
    // Get two tokens after the DEFINE keyword
    final List<String> parameters = new ArrayList<>();

    while (parameters.isEmpty() && !isAtEnd()) { // wait for key
      final var c = peek();

      if (Character.isWhitespace(c)) {
        advance();

        continue;
      }

      if (c == '#') {
        skipComment();

        continue;
      }

      if (c == ',') {
        advance();

        continue;
      }

      if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
        final var startPosition = position;

        while (Character.isLetterOrDigit(peek()) || peek() == '-' || peek() == '_') {
          advance();
        }

        final var text = source.substring(startPosition, position);

        parameters.add(text);

        continue;
      }

      throw new ScoreParseException("Unexpected character: " + c, line, linePosition);
    }

    while (parameters.size() < 2 && !isAtEnd()) { // wait for value (allowing other than comma and comment, read until the eol)
      final var c = peek();

      if (Character.isWhitespace(c)) {
        advance();

        continue;
      }

      if (c == '#') {
        skipComment();

        continue;
      }

      if (c == ',') {
        advance();

        continue;
      }

      final var startPosition = position;

      while (!isAtEnd() && peek() != '\n') {
        advance();
      }

      final var text = source.substring(startPosition, position);

      parameters.add(text);
    }

    if (parameters.size() == 2) {
      final var name = parameters.get(0);
      final var value = parameters.get(1);

      if (definitions.containsKey(name)) {
        throw new ScoreParseException("Symbol redefinition: " + name, line, startLinePosition);
      }

      if (ScoreToken.KeywordToken.isKeyword(name)) {
        throw new ScoreParseException("Cannot define a keyword: " + name, line, startLinePosition);
      }

      if (ScoreToken.NoteToken.isNote(name)) {
        throw new ScoreParseException("Cannot define a note: " + name, line, startLinePosition);
      }

      definitions.put(name, value);
    }
  }

  private boolean isAtEnd() {
    return position >= source.length();
  }

  private void skipComment() {
    while (peek() != '\n' && !isAtEnd()) {
      advance();
    }
  }

  public Map<String, String> getDefinitions() {
    return new HashMap<>(definitions);
  }
}
