import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

class Notes {
  static final double A0_FREQ = 27.5;
  static final int NOTES_IN_OCTAVE = 12;

  static final double[] scalesRatio = {
      Math.pow(2, (-9 / 12.0)), // C
      Math.pow(2, (-8 / 12.0)), // C#
      Math.pow(2, (-7 / 12.0)), // D
      Math.pow(2, (-6 / 12.0)), // D#
      Math.pow(2, (-5 / 12.0)), // E
      Math.pow(2, (-4 / 12.0)), // F
      Math.pow(2, (-3 / 12.0)), // F#
      Math.pow(2, (-2 / 12.0)), // G
      Math.pow(2, (-1 / 12.0)), // G#
      Math.pow(2, (0 / 12.0)), // A
      Math.pow(2, (1 / 12.0)), // A#
      Math.pow(2, (2 / 12.0)), // B
  };

  private static final Map<Character, Note> notesBindings = Map.ofEntries(
    Map.entry('z', Note.parse("C4")),
    Map.entry('s', Note.parse("C#4")),
    Map.entry('x', Note.parse("D4")),
    Map.entry('d', Note.parse("D#4")),
    Map.entry('c', Note.parse("E4")),
    Map.entry('v', Note.parse("F4")),
    Map.entry('g', Note.parse("F#4")),
    Map.entry('b', Note.parse("G4")),
    Map.entry('h', Note.parse("G#4")),
    Map.entry('n', Note.parse("A4")),
    Map.entry('j', Note.parse("A#4")),
    Map.entry('m', Note.parse("B4")),
    Map.entry(',', Note.parse("C5")),
    Map.entry('l', Note.parse("C#5")),
    Map.entry('.', Note.parse("D5")),
    Map.entry(';', Note.parse("D#5")),
    Map.entry('/', Note.parse("E5")),
    Map.entry('q', Note.parse("C5")),
    Map.entry('2', Note.parse("C#5")),
    Map.entry('w', Note.parse("D5")),
    Map.entry('3', Note.parse("D#5")),
    Map.entry('e', Note.parse("E5")),
    Map.entry('r', Note.parse("F5")),
    Map.entry('5', Note.parse("F#5")),
    Map.entry('t', Note.parse("G5")),
    Map.entry('6', Note.parse("G#5")),
    Map.entry('y', Note.parse("A5")),
    Map.entry('7', Note.parse("A#5")),
    Map.entry('u', Note.parse("B5")),
    Map.entry('i', Note.parse("C6")),
    Map.entry('9', Note.parse("C#6")),
    Map.entry('o', Note.parse("D6")),
    Map.entry('0', Note.parse("D#6")),
    Map.entry('p', Note.parse("E6")),
    Map.entry('[', Note.parse("F6")),
    Map.entry('=', Note.parse("F#6")),
    Map.entry(']', Note.parse("G6"))
  );

  public static Optional<Note> bindingOf(char binding) {
    if (!notesBindings.containsKey(binding)) return Optional.empty();

    return Optional.of(notesBindings.get(binding));
  }

  static double hertz(int pos, int octave, double a4freq) {
    if (pos >= scalesRatio.length || pos < 0) throw new IllegalArgumentException();

    final var baseFreq = a4freq * scalesRatio[pos];

    return baseFreq * Math.pow(2, octave - 4);
  }

  static double hertz(int pos, int octave) {
    return hertz(pos, octave, A0_FREQ * 16);
  }

  public static int whiteNoteToScalesPos(int pos) {
    if (pos > 7 || pos < 0) throw new IllegalArgumentException();
    // 0 -> 0
    // 1 -> 2
    // 2 -> 4
    // 3 -> 5
    // 4 -> 7
    // 5 -> 9
    // 6 -> 11
    return pos * 2 - (pos >= 3 ? 1 : 0);
  }

  public static int leftBlackNoteToScalesPos(int pos) {
    if (pos > 7 || pos < 0 || pos == 0 || pos == 3) throw new IllegalArgumentException();
    // 0 -> x
    // 1 -> 1
    // 2 -> 3
    // 3 -> x
    // 4 -> 6
    // 5 -> 8
    // 6 -> 10
    if (pos == 1) return pos;
    if (pos == 2) return pos + 1;
    if (pos == 4) return pos + 2;
    if (pos == 5) return pos + 3;
    return pos + 4;
  }

  public static int rightBlackNoteToScalesPos(int pos) {
    if (pos > 7 || pos < 0 || pos == 2 || pos == 6) throw new IllegalArgumentException();
    // 0 -> 1
    // 1 -> 3
    // 2 -> x
    // 3 -> 6
    // 4 -> 8
    // 5 -> 10
    // 6 -> x
    return pos * 2 + (pos < 3 ? 1 : 0);
  }

  static double toDurationMillis(float bpm, int measure) {
    // (10/bpm): 24分1個の長さ[s]
    // * 24で1小節の長さ[s]
    // measureは?分音符を表すので1小節を割ると欲しい音符の長さ[s]
    // * 1000で単位を[ms]に

    return (10 / (double) bpm) * 24 / (double) measure * 1000;
  }
}

public class Note {
  public static final Pattern NOTE_PATTERN = Pattern.compile("^([A-G][#\\-]?)([0-9]+)$");

  private static final String[] noteNames = {
      "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
  };

  public final int index;
  public final int octave;

  public Note(int index, int octave) {
    if (index >= Notes.scalesRatio.length || index < 0) throw new IllegalArgumentException();
    if (octave < 0) throw new IllegalArgumentException();

    this.index = index;
    this.octave = octave;
  }

  public static Note parse(String fullText) {
    final var matcher = NOTE_PATTERN.matcher(fullText);

    if (!matcher.matches()) throw new IllegalArgumentException();

    var noteName = matcher.group(1);

    if (noteName.endsWith("-"))
      noteName = noteName.substring(0, 1);

    final var index = Arrays.asList(noteNames).indexOf(noteName);
    final var octave = Integer.parseInt(matcher.group(2));

    return new Note(index, octave);
  }

  public double toFreq(double a4Freq) {
    return Notes.hertz(index, octave, a4Freq);
  }

  public double toFreq() {
    return Notes.hertz(index, octave, Notes.A0_FREQ * 16);
  }

  public static Note fromWhiteNote(int pos, int octave) {
    return new Note(Notes.whiteNoteToScalesPos(pos), octave);
  }

  public static Note fromLeftBlackNote(int pos, int octave) {
    return new Note(Notes.leftBlackNoteToScalesPos(pos), octave);
  }

  public static Note fromRightBlackNote(int pos, int octave) {
    return new Note(Notes.rightBlackNoteToScalesPos(pos), octave);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Note)) return false;

    Note other = (Note) obj;
    return index == other.index && octave == other.octave;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + index;
    result = 31 * result + octave;
    return result;
  }

  @Override
  public String toString() {
    return noteNames[index] + octave;
  }
}
