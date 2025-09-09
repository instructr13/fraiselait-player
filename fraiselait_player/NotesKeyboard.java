import processing.core.PApplet;

import java.util.ArrayList;
import java.util.List;

public class NotesKeyboard implements Component {
  public static final int OCTAVES = 6;
  public static final int BASE_OCTAVE = 2;
  private static final int BAR_HEIGHT = 40;

  private final OscillatorSession oscillatorSession;
  private final int height;
  private final int offsetY;

  private final List<Note> activeNotes = new ArrayList<>();

  private Note hoveredNote = null;
  private Note lastNote = null;

  public NotesKeyboard(OscillatorSession oscillatorSession, int height, int offsetY) {
    if (height <= BAR_HEIGHT) {
      throw new IllegalArgumentException("Height must be greater than " + BAR_HEIGHT);
    }

    this.oscillatorSession = oscillatorSession;
    this.height = height - BAR_HEIGHT;
    this.offsetY = offsetY + BAR_HEIGHT;
  }

  private void setNote(Note note) {
    if ((lastNote == null && note != null) || (lastNote != null && !lastNote.equals(note))) {
      oscillatorSession.offerNote(note);

      lastNote = note;
    }
  }

  private void drawBar(PApplet a) {
    a.noStroke();

    a.fill(ColorScheme.ZincSurface0);

    a.rect(0, offsetY - BAR_HEIGHT, a.width, BAR_HEIGHT);

    a.fill(ColorScheme.ForegroundMuted);
    a.textAlign(PApplet.LEFT, PApplet.CENTER);
    DrawingUtil.smoothTextSize(a, 14);

    DrawingUtil.textWithLetterSpacing(
        a,
        "FRAISELAIT PLAYER",
        40, offsetY - BAR_HEIGHT / 2.0f,
        0.6f
    );

    a.textAlign(PApplet.RIGHT, PApplet.CENTER);

    DrawingUtil.textWithLetterSpacing(
        a,
        "USE MOUSE OR KEYBOARD TO INTERACT",
        a.width - 40, offsetY - BAR_HEIGHT / 2.0f,
        0.6f
    );
  }

  private void drawNotes(PApplet a, int octave, int offsetX, int width, boolean[] pressedNotes) {
    final var noteSize = (int) Math.round(width / 7.0);
    final var blackNoteWidth = (int) Math.round(width / 7.0 * 0.6);
    final var blackNoteHeight = (int) Math.round(height * 0.6);

    // Draw white notes
    for (int i = 0; i < 7; i++) {
      final var note = Note.fromWhiteNote(i, octave);
      final var x = offsetX + i * noteSize;

      a.noStroke();

      if (pressedNotes[note.index]) {
        a.fill(ColorScheme.ZincSurface0);
      } else {
        a.fill(ColorScheme.ZincSurface1);
      }

      a.rect(x, offsetY, noteSize, height);

      a.stroke(ColorScheme.ZincBackground);
      a.strokeWeight(1);

      final var lineX = x + noteSize - 1;

      if (lineX < offsetX + width) {
        a.line(lineX, offsetY, lineX, offsetY + height);
      }
    }

    a.noStroke();

    // Draw black notes
    for (int i = 0; i < 7; i++) {
      if (i == 2 || i == 6) {
        // No black note on E and B
        continue;
      }

      final var noteIndex = Notes.rightBlackNoteToScalesPos(i);
      final var x = offsetX + (i + 1) * noteSize - (blackNoteWidth / 2);

      if (pressedNotes[noteIndex]) {
        a.fill(ColorScheme.ZincBackground);
      } else {
        a.fill(ColorScheme.ZincSurface0);
      }

      a.rect(x, offsetY, blackNoteWidth, blackNoteHeight);
    }
  }

  private void checkMainNotesHover(PApplet a) {
    final var whiteNoteSize = (int) Math.round(a.width / (OCTAVES * 7.0));
    final var blackNoteSize = (int) Math.round(a.width / (OCTAVES * 7.0) * 0.6);
    final var blackNoteHeight = (int) Math.round(height * 0.6);

    if (a.mouseY > offsetY + blackNoteHeight) {
      // Only white notes are hoverable in this area
      final var rawNoteIndex = a.mouseX / whiteNoteSize;

      hoveredNote = Note.fromWhiteNote(rawNoteIndex % 7, BASE_OCTAVE + (rawNoteIndex / 7));

      return;
    }

    // Check approximate white note index
    final var rawWhiteNoteIndex = a.mouseX / whiteNoteSize;
    final var rawWhiteNoteOffset = a.mouseX % whiteNoteSize;

    final var rawWhiteNoteIndexRem7 = rawWhiteNoteIndex % 7;

    final var leftIsBlackNote = rawWhiteNoteOffset < blackNoteSize / 2;
    final var rightIsBlackNote = rawWhiteNoteOffset > whiteNoteSize - (blackNoteSize / 2);

    if (
        // Left black note
        (
            !leftIsBlackNote
                && (rawWhiteNoteIndexRem7 == 2 || rawWhiteNoteIndexRem7 == 6)
        )
        // Right black note
        || (
            !rightIsBlackNote
                && (rawWhiteNoteIndexRem7 == 0 || rawWhiteNoteIndexRem7 == 3)
        )
        // Middle black note
        || (
            !leftIsBlackNote && !rightIsBlackNote
        )
    ) {
      hoveredNote = Note.fromWhiteNote(rawWhiteNoteIndexRem7, BASE_OCTAVE + (rawWhiteNoteIndex / 7));

      return;
    }

    // Black note

    if (leftIsBlackNote) {
      hoveredNote = Note.fromLeftBlackNote(rawWhiteNoteIndexRem7, BASE_OCTAVE + (rawWhiteNoteIndex / 7));

      return;
    }

    if (rightIsBlackNote) {
      hoveredNote = Note.fromRightBlackNote(rawWhiteNoteIndexRem7, BASE_OCTAVE + (rawWhiteNoteIndex / 7));

      return;
    }
  }

  @Override
  public void draw(PApplet a, ComponentsRegistry r) {
    final var baseWidth = (int) Math.round(a.width / (double) OCTAVES);

    for (int i = 0; i < OCTAVES; i++) {
      final var capturedI = i;
      var pressedNotes = new boolean[12];

      activeNotes.stream()
          .filter(note -> note.octave == BASE_OCTAVE + capturedI)
          .forEach(note -> pressedNotes[note.index] = true);

      drawNotes(
          a,
          BASE_OCTAVE + i,
          i * baseWidth,
          baseWidth,
          pressedNotes
      );
    }

    drawBar(a);
  }

  @Override
  public void keyPressed(PApplet a, ComponentsRegistry r) {
    final var maybeNote = Notes.bindingOf(a.key);

    maybeNote.ifPresent(note -> {
      if (!activeNotes.contains(note)) {
        activeNotes.add(note);

        if (!note.equals(lastNote)) {
          setNote(note);
        }
      }
    });
  }

  @Override
  public void keyReleased(PApplet a, ComponentsRegistry r) {
    final var maybeNote = Notes.bindingOf(a.key);

    maybeNote.ifPresent(note -> {
      activeNotes.remove(note);

      if (activeNotes.isEmpty()) {
        setNote(null);
      } else if (note.equals(lastNote)) {
        setNote(activeNotes.get(activeNotes.size() - 1));
      }
    });
  }

  @Override
  public void mouseMoved(PApplet a, ComponentsRegistry r) {
    if (a.mouseY < offsetY || a.mouseY > offsetY + height) {
      hoveredNote = null;

      return;
    }

    checkMainNotesHover(a);
  }

  @Override
  public void mousePressed(PApplet a, ComponentsRegistry r) {
    if (a.mouseButton != PApplet.LEFT || hoveredNote == null) {
      return;
    }

    if (!activeNotes.contains(hoveredNote)) {
      activeNotes.add(hoveredNote);
      setNote(hoveredNote);
    }
  }

  @Override
  public void mouseDragged(PApplet a, ComponentsRegistry r) {
    if (a.mouseButton != PApplet.LEFT || hoveredNote == null || a.mouseY < offsetY || a.mouseY > offsetY + height) {
      if (activeNotes.contains(hoveredNote)) {
        activeNotes.remove(hoveredNote);

        if (activeNotes.isEmpty()) {
          setNote(null);
        } else {
          setNote(activeNotes.get(activeNotes.size() - 1));
        }
      }

      hoveredNote = null;

      return;
    }

    final var prevHoveredNote = hoveredNote;

    checkMainNotesHover(a);

    if (hoveredNote == null) {
      return;
    }

    if (!prevHoveredNote.equals(hoveredNote)) {
      activeNotes.remove(prevHoveredNote);
      activeNotes.add(hoveredNote);

      setNote(hoveredNote);
    }
  }

  @Override
  public void mouseReleased(PApplet a, ComponentsRegistry r) {
    if (a.mouseButton != PApplet.LEFT || hoveredNote == null) {
      return;
    }

    activeNotes.remove(hoveredNote);

    if (activeNotes.isEmpty()) {
      setNote(null);
    } else {
      setNote(activeNotes.get(activeNotes.size() - 1));
    }
  }
}
