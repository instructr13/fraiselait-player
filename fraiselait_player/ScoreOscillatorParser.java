import dev.wycey.mido.fraiselait.builtins.WaveformType;

public class ScoreOscillatorParser {
  private ScoreOscillatorParser() {}

  public static void parse(ScoreParserContext context) {
    // osc,<name> EOL
    context.expectKeyword(ScoreKeywords.OSC.getLiteral());
    context.expectComma();

    final var nameToken = context.expectAnyIdentifier();
    final var oscName = nameToken.getLexeme();

    context.expectEndOfCommand();

    WaveformType waveform = null;
    Double startVol = null;
    Double endVol = null;
    double gamma = 1.0; // default 1
    Double duration = null;
    Integer quality = null;

    // parse until end,osc
    while (true) {
      final var token = context.peek();

      if (token instanceof ScoreToken.NewlineToken) {
        context.consume();

        continue;
      }

      if (token instanceof ScoreToken.IdentifierToken) {
        final var id = token.getLexeme().toLowerCase();

        switch (id) {
          case "waveform": {
            context.expectIdentifier("waveform");
            context.expectComma();

            final var waveformNameToken = context.expectAnyIdentifier();
            final var waveformName = waveformNameToken.getLexeme();

            final var newWaveform = WaveformType.fromTokenName(waveformName.toLowerCase());

            if (newWaveform == null) {
              throw new ScoreParseException(
                  "Invalid waveform name: " + waveformName,
                  waveformNameToken.getLineNumber(), waveformNameToken.getPosition());
            }

            if (waveform != null) {
              throw new ScoreParseException("Duplicate waveform directive", waveformNameToken.getLineNumber(), waveformNameToken.getPosition());
            }

            waveform = newWaveform;

            context.expectEndOfCommand();

            break;
          }

          case "envelope": {
            context.expectIdentifier("envelope");
            context.expectComma();
            final var startVolToken = context.expectNumber();
            context.expectComma();
            final var endVolToken = context.expectNumber();
            context.expectComma();
            final var durationToken = context.expectNumber();

            double newGamma = 1.0;

            if (context.hasComma()) {
              context.expectComma();

              final var gammaToken = context.expectNumber();

              newGamma = gammaToken.getLiteral().doubleValue();
            }

            context.expectEndOfCommand();

            if (startVol != null) {
              throw new ScoreParseException("Duplicate envelope directive", startVolToken.getLineNumber(), startVolToken.getPosition());
            }

            startVol = startVolToken.getLiteral().doubleValue();
            endVol = endVolToken.getLiteral().doubleValue();
            duration = durationToken.getLiteral().doubleValue();
            gamma = newGamma;

            break;
          }

          case "quality": {
            context.expectIdentifier("quality");
            context.expectComma();
            final var qualityToken = context.expectNumber();
            context.expectEndOfCommand();

            if (quality != null) {
              throw new ScoreParseException("Duplicate quality directive", qualityToken.getLineNumber(), qualityToken.getPosition());
            }

            quality = qualityToken.getLiteral().intValue();

            break;
          }

          default:
            throw new ScoreParseException(
                "Directive not allowed in osc block: " + token.getLexeme(),
                token.getLineNumber(), token.getPosition());
        }

        continue;
      }

      if (token instanceof ScoreToken.KeywordToken) {
        final var keyword = ((ScoreToken.KeywordToken) token).getLiteral();

        if (keyword.getLiteral().equalsIgnoreCase(ScoreKeywords.END.getLiteral())) {
          context.consume();

          context.expectComma();
          context.expectKeyword(ScoreKeywords.OSC.getLiteral());
          context.expectEndOfCommand();

          // finalize
          if (waveform == null) {
            throw new ScoreParseException("Missing waveform directive in osc block: " + oscName, nameToken.getLineNumber(), nameToken.getPosition());
          }

          if (startVol == null) {
            throw new ScoreParseException("Missing envelope directive in osc block: " + oscName, nameToken.getLineNumber(), nameToken.getPosition());
          }

          if (quality == null) {
            throw new ScoreParseException("Missing quality directive in osc block: " + oscName, nameToken.getLineNumber(), nameToken.getPosition());
          }

          final var osc = new Oscillator(oscName, waveform, startVol, endVol, gamma, duration, quality);

          context.addOscillator(osc);

          return; // end block
        } else {
          throw new ScoreParseException(
              "Unexpected keyword inside osc block: " + keyword,
              token.getLineNumber(), token.getPosition());
        }
      }

      // Any other token is invalid inside osc block
      throw new ScoreParseException(
          "Unexpected token inside osc block: " + token.getClass().getSimpleName(),
          token.getLineNumber(), token.getPosition());
    }
  }
}
