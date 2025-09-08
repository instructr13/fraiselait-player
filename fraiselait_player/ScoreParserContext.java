import dev.wycey.mido.fraiselait.builtins.WaveformType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScoreParserContext {
  private static final Oscillator DEFAULT_OSCILLATOR = Oscillator.createConstantVolumeOscillator("default", WaveformType.SQUARE, 1);

  private final List<ScoreToken<?>> tokens;
  private final Map<String, String> definitions = new HashMap<>();
  private final List<ScoreCommand> commands = new ArrayList<>();

  private int position;

  private ScoreCommand.Header header;

  private boolean isFinished = false;

  // Oscillator registry
  private final Map<String, Oscillator> oscillatorsByName = new HashMap<>();
  private final List<Oscillator> oscillators = new ArrayList<>();

  public ScoreParserContext(List<ScoreToken<?>> tokens) {
    this.tokens = tokens;
    this.position = 0;

    // Add default oscillator
    addOscillator(DEFAULT_OSCILLATOR);
  }

  public ScoreToken<?> peek() {
    if (position >= tokens.size())
      return tokens.get(tokens.size() - 1); // EOF

    return tokens.get(position);
  }

  public ScoreToken<?> consume() {
    ScoreToken<?> token = peek();

    position++;

    return token;
  }

  public <T extends ScoreToken<?>> T expect(Class<T> tokenClass) {
    ScoreToken<?> token = peek();

    if (!tokenClass.isInstance(token)) {
      throw new ScoreParseException(
          "Expected %s but got %s".formatted(
              tokenClass.getSimpleName(), token.getClass().getSimpleName()
          ), token.getLineNumber(), token.getPosition());
    }

    position++;

    return tokenClass.cast(token);
  }

  public ScoreToken.KeywordToken expectKeyword(String keyword) {
    ScoreToken.KeywordToken token = expect(ScoreToken.KeywordToken.class);

    if (!token.getLexeme().equalsIgnoreCase(keyword)) {
      throw new ScoreParseException(
          "Expected keyword %s but got %s".formatted(
              keyword, token.getLexeme()
          ), token.getLineNumber(), token.getPosition());
    }

    return token;
  }

  public ScoreToken.IdentifierToken expectIdentifier(String name) {
    ScoreToken.IdentifierToken token = expect(ScoreToken.IdentifierToken.class);

    if (!token.getLexeme().equalsIgnoreCase(name)) {
      throw new ScoreParseException(
          "Expected identifier %s but got %s".formatted(
              name, token.getLexeme()
          ), token.getLineNumber(), token.getPosition());
    }

    return token;
  }

  public ScoreToken.IdentifierToken expectAnyIdentifier() {
    return expect(ScoreToken.IdentifierToken.class);
  }

  public ScoreToken.NumberToken expectNumber() {
    return expect(ScoreToken.NumberToken.class);
  }

  public ScoreToken.CommaToken expectComma() {
    return expect(ScoreToken.CommaToken.class);
  }

  public void expectEndOfCommand() {
    ScoreToken<?> token = peek();

    if (!(token instanceof ScoreToken.SemicolonToken) &&
        !(token instanceof ScoreToken.NewlineToken) && !(token instanceof ScoreToken.EOFToken)) {
      throw new ScoreParseException(
          "Expected semicolon, newline, or EOF but got %s".formatted(
              token.getClass().getSimpleName()
          ), token.getLineNumber(), token.getPosition());
    }

    position++;
  }

  public void expectNewLineOrEOF() {
    ScoreToken<?> token = peek();

    if (!(token instanceof ScoreToken.NewlineToken) && !(token instanceof ScoreToken.EOFToken)) {
      throw new ScoreParseException(
          "Expected newline or EOF but got %s".formatted(
              token.getClass().getSimpleName()
          ), token.getLineNumber(), token.getPosition());
    }

    position++;
  }

  public ScoreToken.FrequencyToken expectNoteOrFrequency() {
    ScoreToken<?> token = peek();

    if (!(token instanceof ScoreToken.NoteToken) && !(token instanceof ScoreToken.NumberToken)) {
      throw new ScoreParseException(
          "Expected note or frequency but got %s".formatted(
              token.getClass().getSimpleName()
          ), token.getLineNumber(), token.getPosition());
    }

    position++;

    if (token instanceof ScoreToken.NoteToken) {
      return new ScoreToken.FrequencyToken((ScoreToken.NoteToken) token);
    } else {
      return new ScoreToken.FrequencyToken((ScoreToken.NumberToken) token);
    }
  }

  public boolean hasComma() {
    return peek() instanceof ScoreToken.CommaToken;
  }

  public void addHeader(ScoreCommand.Header header) {
    if (this.header != null) {
      throw new IllegalStateException("Header is already set");
    }

    this.header = header;
  }

  public void addCommand(ScoreCommand command) {
    if (command instanceof ScoreCommand.Header) {
      throw new IllegalStateException("Do not add header with addCommand method");
    } else if (header == null) {
      throw new ScoreParseException("Header is required before other commands", 0, 0);
    }

    if (isFinished) {
      throw new ScoreParseException("Command is not allowed after STOP or REPLAY command", 0, 0);
    }

    if (command instanceof ScoreCommand.Stop || command instanceof ScoreCommand.Replay) {
      isFinished = true;
    }

    commands.add(command);
  }

  public void addDefinition(String name, String value) {
    definitions.put(name, value);
  }

  public String getDefinition(String name) {
    return definitions.get(name);
  }

  public boolean hasDefinition(String name) {
    return definitions.containsKey(name);
  }

  public boolean hasHeader() {
    return header != null;
  }

  public ScoreCommand.Header getHeader() {
    return header;
  }

  public List<ScoreCommand> getCommands() {
    return commands;
  }

  public void addOscillator(Oscillator osc) {
    final var name = osc.getName();

    if (oscillatorsByName.containsKey(name)) {
      throw new ScoreParseException("Oscillator already defined: " + name, 0, 0);
    }

    oscillatorsByName.put(name, osc);
    oscillators.add(osc);
  }

  public boolean hasOscillator(String name) {
    return oscillatorsByName.containsKey(name);
  }

  public Oscillator getOscillator(String name) {
    return oscillatorsByName.get(name);
  }

  public List<Oscillator> getOscillators() {
    return oscillators;
  }

  public void validate() {
    if (header == null)
      throw new ScoreParseException("Header is required", 0, 0);

    if (!isFinished)
      throw new ScoreParseException("STOP or REPLAY command is required", 0, 0);
  }
}
