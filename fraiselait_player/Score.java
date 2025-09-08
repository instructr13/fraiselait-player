import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 再生用の譜面データを表すクラス。ファクトリメソッドを通じて作成される。
 */
public class Score {
  private final float offset;
  private final float startingBPM;
  private final int startingMeasure;
  private final double baseA4Frequency;
  private final List<ScoreCommand> commands;

  private final List<Oscillator> oscillators;

  private Score(float startingBPM, int startingMeasure, float offset, double baseA4Frequency, List<ScoreCommand> commands, List<Oscillator> oscillators) {
    this.startingBPM = startingBPM;
    this.startingMeasure = startingMeasure;
    this.offset = offset;
    this.baseA4Frequency = baseA4Frequency;
    this.commands = commands;
    this.oscillators = oscillators;
  }

  /**
   * ファイルパスから譜面データを読み込む。
   *
   * @param filePath 読み込むファイルのパス
   * @param prefix   譜面データの先頭を示す文字列
   */
  public static Score loadFromPath(Path filePath, String prefix) throws IOException {
    if (!Files.isReadable(filePath)) throw new IllegalArgumentException();

    final var content = Files
        .readAllLines(filePath)
        .stream()
        .filter(s -> !s.isEmpty()) // Skip empty lines
        .dropWhile(s -> !s.equals(prefix))
        .takeWhile(s -> !s.equals("end"))
        .skip(1) // skip first prefix line
        .collect(Collectors.joining("\n"));

    if (content.isEmpty()) throw new IllegalArgumentException();

    final var lexer = new ScoreLexer(content);
    final var tokens = lexer.tokenize();

    final var parser = new ScoreParser(tokens);
    final var commands = parser.parse();

    final var header = parser.getHeader();

    final var bpm = header.getBPM();
    final var measure = header.getMeasure();
    final var offset = header.getOffset();
    final var baseA4Frequency = header.getBaseA4Frequency();

    final var oscillators = parser.getOscillators();

    return new Score(bpm, measure, offset, baseA4Frequency, commands, oscillators);
  }

  /**
   * ファイルパスから譜面データを読み込む。
   *
   * @param filePath 読み込むファイルのパス
   */
  public static Score loadFromPath(Path filePath) throws IOException {
    return loadFromPath(filePath, "sound");
  }

  public float getStartingBPM() {
    return startingBPM;
  }

  public int getStartingMeasure() {
    return startingMeasure;
  }

  public float getOffset() {
    return offset;
  }

  public double getBaseA4Frequency() {
    return baseA4Frequency;
  }

  public List<ScoreCommand> getCommands() {
    return new ArrayList<>(commands);
  }

  public List<Oscillator> getOscillators() {
    return oscillators;
  }

  public Oscillator getStartingOscillator() {
    return oscillators.stream().filter(o -> o.getName().equals("default")).findFirst().orElseThrow();
  }
}
