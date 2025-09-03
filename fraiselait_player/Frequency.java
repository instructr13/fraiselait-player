public interface Frequency {
  double get(double a4Freq);

  default double get() {
    return get(Notes.A0_FREQ * 16);
  }

  @Override
  String toString();

  final class NoteFrequency implements Frequency {
    private final Note note;

    public NoteFrequency(Note note) {
      this.note = note;
    }

    public Note getNote() {
      return note;
    }

    @Override
    public double get(double a4Freq) {
      return Notes.hertz(note.index, note.octave, a4Freq);
    }

    @Override
    public String toString() {
      return note.toString();
    }
  }

  final class RawFrequency implements Frequency {
    private final double frequency;

    public RawFrequency(double frequency) {
      this.frequency = frequency;
    }

    @Override
    public double get(double a4Freq) {
      return frequency;
    }

    @Override
    public String toString() {
      return frequency + "Hz";
    }
  }
}
