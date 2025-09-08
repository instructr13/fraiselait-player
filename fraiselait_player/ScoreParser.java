import java.util.List;

public class ScoreParser {
  private final ScoreParserContext context;

  public ScoreParser(List<ScoreToken<?>> tokens) {
    this.context = new ScoreParserContext(tokens);
  }

  public List<ScoreCommand> parse() {
    while (!(context.peek() instanceof ScoreToken.EOFToken)) {
      parseCommand();
    }

    context.validate();

    return context.getCommands();
  }

  public ScoreCommand.Header getHeader() {
    return context.getHeader();
  }

  public List<Oscillator> getOscillators() {
    return context.getOscillators();
  }

  private void parseCommand() {
    final var token = context.peek();

    if (token instanceof ScoreToken.NewlineToken) {
      context.consume();

      return;
    }

    // If the init command is not processed yet, parse it first
    if (!context.hasHeader()) {
      ScoreCommand.Header.parse(context);

      return;
    }

    if (token instanceof ScoreToken.KeywordToken) {
      final var keyword = ((ScoreToken.KeywordToken) token).getLiteral();

      switch (keyword) {
        case DEFINE:
          throw new ScoreParseException("BUG: DEFINE keyword should be pre-processed in ScoreLexer", token.getLineNumber(), token.getPosition());

        case STOP:
          ScoreCommand.Stop.parse(context);

          break;

        case REPLAY:
          ScoreCommand.Replay.parse(context);

          break;

        case BPM:
          ScoreCommand.ChangeBPM.parse(context);

          break;

        case MEASURE:
          ScoreCommand.ChangeMeasure.parse(context);

          break;

        case VOL:
          ScoreCommand.ChangeVolume.parse(context);

          break;

        case PITCH:
          ScoreCommand.Pitch.parse(context);

          break;

        case VIB:
          ScoreCommand.Vibrato.parse(context);

          break;

        case USE:
          ScoreCommand.Use.parse(context);

          break;

        case OSC:
          ScoreOscillatorParser.parse(context);

          break;

        default:
          throw new ScoreParseException("Unexpected keyword: " + keyword, token.getLineNumber(), token.getPosition());
      }

      return;
    }

    if (token instanceof ScoreToken.RestToken) {
      ScoreCommand.Rest.parse(context);

      return;
    }

    if (token instanceof ScoreToken.NoteToken || token instanceof ScoreToken.NumberToken) {
      ScoreCommand.PlayNote.parse(context);

      return;
    }

    throw new ScoreParseException("Unexpected token: " + token.getClass().getSimpleName(), token.getLineNumber(), token.getPosition());
  }
}
