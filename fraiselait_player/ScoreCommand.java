public abstract class ScoreCommand {
  private ScoreCommand() {
  }

  public static class Header extends ScoreCommand {
    private final float bpm;
    private final int measure;
    private final float offset;
    private final double baseA4Frequency;

    public Header(float bpm, int measure, float offset, double baseA4Frequency) {
      this.bpm = bpm;
      this.measure = measure;
      this.offset = offset;
      this.baseA4Frequency = baseA4Frequency;
    }

    public static void parse(ScoreParserContext context) {
      final var bpmToken = context.expect(ScoreToken.NumberToken.class);
      context.expectComma();
      final var measureToken = context.expect(ScoreToken.NumberToken.class);
      context.expectComma();
      final var offsetToken = context.expect(ScoreToken.NumberToken.class);
      context.expectComma();
      final var baseA4FrequencyToken = context.expect(ScoreToken.NumberToken.class);
      context.expectNewlineOrEOF();

      final var header = new Header(
          bpmToken.getLiteral().floatValue(),
          measureToken.getLiteral().intValue(),
          offsetToken.getLiteral().floatValue(),
          baseA4FrequencyToken.getLiteral().doubleValue()
      );

      context.addHeader(header);
    }

    public float getBPM() {
      return bpm;
    }

    public int getMeasure() {
      return measure;
    }

    public float getOffset() {
      return offset;
    }

    public double getBaseA4Frequency() {
      return baseA4Frequency;
    }
  }

  public static class PlayNote extends ScoreCommand {
    private final Frequency frequency;
    private final float duration;
    private final float nextNoteDuration;

    public PlayNote(Frequency frequency, float duration, float nextNoteDuration) {
      this.frequency = frequency;
      this.duration = duration;
      this.nextNoteDuration = nextNoteDuration;
    }

    public static void parse(ScoreParserContext context) {
      final var token = context.expectNoteOrFrequency();
      final var frequency = token.getLiteral();
      context.expectComma();
      final var durationToken = context.expect(ScoreToken.NumberToken.class);
      context.expectComma();
      final var nextNoteDurationToken = context.expect(ScoreToken.NumberToken.class);
      context.expectNewlineOrEOF();

      final var playNoteCommand = new PlayNote(
          frequency, durationToken.getLiteral().floatValue(), nextNoteDurationToken.getLiteral().floatValue()
      );

      context.addCommand(playNoteCommand);
    }

    public Frequency getFrequency() {
      return frequency;
    }

    public float getDuration() {
      return duration;
    }

    public float getNextNoteDuration() {
      return nextNoteDuration;
    }
  }

  public static class Rest extends ScoreCommand {
    private final float duration;

    public Rest(float duration) {
      this.duration = duration;
    }

    public static void parse(ScoreParserContext context) {
      context.expect(ScoreToken.RestToken.class);
      context.expectComma();
      final var durationToken = context.expect(ScoreToken.NumberToken.class);
      context.expectNewlineOrEOF();

      final var restCommand = new Rest(
          durationToken.getLiteral().floatValue()
      );

      context.addCommand(restCommand);
    }

    public float getDuration() {
      return duration;
    }
  }

  public static class ChangeBPM extends ScoreCommand {
    private final float bpm;

    public ChangeBPM(float bpm) {
      this.bpm = bpm;
    }

    public static void parse(ScoreParserContext context) {
      context.expectKeyword(ScoreKeywords.BPM.getLiteral());
      context.expectComma();
      final var bpmToken = context.expect(ScoreToken.NumberToken.class);
      context.expectNewlineOrEOF();

      final var changeBPMCommand = new ChangeBPM(
          bpmToken.getLiteral().floatValue()
      );

      context.addCommand(changeBPMCommand);
    }

    public float getBPM() {
      return bpm;
    }
  }

  public static class ChangeMeasure extends ScoreCommand {
    private final int measure;

    public ChangeMeasure(int measure) {
      this.measure = measure;
    }

    public static void parse(ScoreParserContext context) {
      context.expectKeyword(ScoreKeywords.MEASURE.getLiteral());
      context.expectComma();
      final var measureToken = context.expect(ScoreToken.NumberToken.class);
      context.expectNewlineOrEOF();

      final var changeMeasureCommand = new ChangeMeasure(
          measureToken.getLiteral().intValue()
      );

      context.addCommand(changeMeasureCommand);
    }

    public int getMeasure() {
      return measure;
    }
  }

  public static class Pitch extends ScoreCommand {
    private final Frequency beforeFrequency;
    private final Frequency afterFrequency;
    private final float duration;
    private final float nextNoteDuration;
    private final int quality;
    private final TimingFunctions function;

    public Pitch(Frequency beforeFrequency, Frequency afterFrequency, float duration, float nextNoteDuration, int quality,
                 TimingFunctions function) {
      this.beforeFrequency = beforeFrequency;
      this.afterFrequency = afterFrequency;
      this.duration = duration;
      this.nextNoteDuration = nextNoteDuration;
      this.quality = quality;
      this.function = function;
    }

    // TODO: allow raw frequency

    public static void parse(ScoreParserContext context) {
      context.expectKeyword(ScoreKeywords.PITCH.getLiteral());
      context.expectComma();
      final var beforeFrequencyToken = context.expectNoteOrFrequency();
      context.expectComma();
      final var afterFrequencyToken = context.expectNoteOrFrequency();
      context.expectComma();
      final var durationToken = context.expect(ScoreToken.NumberToken.class);
      context.expectComma();
      final var nextNoteDurationToken = context.expect(ScoreToken.NumberToken.class);
      context.expectComma();
      final var qualityToken = context.expect(ScoreToken.NumberToken.class);

      final TimingFunctions function;

      if (context.hasComma()) {
        context.expectComma();

        final var functionNameToken = context.expect(ScoreToken.IdentifierToken.class);

        if (!TimingFunctions.getNames().contains(functionNameToken.getLexeme().toLowerCase())) {
          throw new ScoreParseException(
              "Invalid timing function name: " + functionNameToken.getLexeme(),
              functionNameToken.getLineNumber(), functionNameToken.getPosition());
        }

        function = TimingFunctions.fromName(functionNameToken.getLexeme().toLowerCase());
      } else {
        function = TimingFunctions.LINEAR;
      }

      context.expectNewlineOrEOF();

      context.addCommand(new Pitch(
          beforeFrequencyToken.getLiteral(),
          afterFrequencyToken.getLiteral(),
          durationToken.getLiteral().floatValue(),
          nextNoteDurationToken.getLiteral().floatValue(),
          qualityToken.getLiteral().intValue(),
          function
      ));
    }

    public Frequency getBeforeFrequency() {
      return beforeFrequency;
    }

    public Frequency getAfterFrequency() {
      return afterFrequency;
    }

    public float getDuration() {
      return duration;
    }

    public float getNextNoteDuration() {
      return nextNoteDuration;
    }

    public int getQuality() {
      return quality;
    }

    public TimingFunctions getFunction() {
      return function;
    }
  }

  public static class Vibrato extends ScoreCommand {
    private final Frequency frequency1;
    private final Frequency frequency2;
    private final float duration;
    private final float nextNoteDuration;
    private final int count;

    public Vibrato(Frequency frequency1, Frequency frequency2, float duration, float nextNoteDuration, int count) {
      this.frequency1 = frequency1;
      this.frequency2 = frequency2;
      this.duration = duration;
      this.nextNoteDuration = nextNoteDuration;
      this.count = count;
    }

    public static void parse(ScoreParserContext context) {
      context.expectKeyword(ScoreKeywords.VIB.getLiteral());
      context.expectComma();
      final var frequency1Token = context.expectNoteOrFrequency();
      context.expectComma();
      final var frequency2Token = context.expectNoteOrFrequency();
      context.expectComma();
      final var durationToken = context.expect(ScoreToken.NumberToken.class);
      context.expectComma();
      final var nextNoteDurationToken = context.expect(ScoreToken.NumberToken.class);
      context.expectComma();
      final var countToken = context.expect(ScoreToken.NumberToken.class);
      context.expectNewlineOrEOF();

      context.addCommand(new Vibrato(
          frequency1Token.getLiteral(),
          frequency2Token.getLiteral(),
          durationToken.getLiteral().floatValue(),
          nextNoteDurationToken.getLiteral().floatValue(),
          countToken.getLiteral().intValue()
      ));
    }

    public Frequency getFrequency1() {
      return frequency1;
    }

    public Frequency getFrequency2() {
      return frequency2;
    }

    public float getDuration() {
      return duration;
    }

    public float getNextNoteDuration() {
      return nextNoteDuration;
    }

    public int getCount() {
      return count;
    }
  }

  public static class Replay extends ScoreCommand {
    private static final Replay INSTANCE = new Replay();

    private Replay() {
    }

    public static Replay getInstance() {
      return INSTANCE;
    }

    public static void parse(ScoreParserContext context) {
      context.expectKeyword(ScoreKeywords.REPLAY.getLiteral());
      context.expectNewlineOrEOF();

      context.addCommand(INSTANCE);
    }
  }

  public static class Stop extends ScoreCommand {
    private static final Stop INSTANCE = new Stop();

    private Stop() {
    }

    public static Stop getInstance() {
      return INSTANCE;
    }

    public static void parse(ScoreParserContext context) {
      context.expectKeyword(ScoreKeywords.STOP.getLiteral());
      context.expectNewlineOrEOF();

      context.addCommand(INSTANCE);
    }
  }
}
