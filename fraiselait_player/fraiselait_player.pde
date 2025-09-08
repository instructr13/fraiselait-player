import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import dev.wycey.mido.fraiselait.builtins.*;
import dev.wycey.mido.fraiselait.builtins.commands.*;
import dev.wycey.mido.fraiselait.builtins.orchestrators.MultiDeviceOrchestrator;

// From Catppuccin Latte
class ColorScheme {
  static final color Rosewater = #dc8a78;
  static final color Flamingo = #dd7878;
  static final color Pink = #ea76cb;
  static final color Mauve = #8839ef;
  static final color Red = #d20f39;
  static final color Maroon = #e64553;
  static final color Peach = #fe640b;
  static final color Yellow = #df8e1d;
  static final color Green = #40a02b;
  static final color Teal = #179299;
  static final color Sky = #04a5e5;
  static final color Sapphire = #209fb5;
  static final color Blue = #1e66f5;
  static final color Lavender = #7287fd;
  static final color Text = #4c4f69;
  static final color Subtext1 = #5c5f77;
  static final color Subtext0 = #6c6f85;
  static final color Overlay2 = #7c7f93;
  static final color Overlay1 = #8c8fa1;
  static final color Overlay0 = #9ca0b0;
  static final color Surface2 = #acb0be;
  static final color Surface1 = #bcc0cc;
  static final color Surface0 = #ccd0da;
  static final color Base = #eff1f5;
  static final color Mantle = #e6e9ef;
  static final color Crust = #dce0e8;
}

Color pColorToAWT(color col) {
  int r = (col >> 16) & 0xff;
  int g = (col >> 8) & 0xff;
  int b = col & 0xff;

  return new Color(r, g, b);
}

static final color BG = ColorScheme.Base;
static final float RADII_DEFAULT = 4;
static final float STROKE_DEFAULT = 3;
static final float TEXT_DEFAULT = 20;
static final float TEXT_SMALL = 14;

static class Point {
  int x;
  int y;

  Point(int x, int y) {
    this.x = x;
    this.y = y;
  }
}

static class Constraints {
  Point offset;
  Point size;

  Constraints(int ox, int oy, int sx, int sy) {
    offset = new Point(ox, oy);
    size = new Point(sx, sy);
  }
}

static class Radii {
  float topLeft;
  float topRight;
  float bottomRight;
  float bottomLeft;

  Radii(float topLeft, float topRight, float bottomRight, float bottomLeft) {
    this.topLeft = topLeft;
    this.topRight = topRight;
    this.bottomRight = bottomRight;
    this.bottomLeft = bottomLeft;
  }

  static final Radii NONE = Radii.all(0);

  static Radii all(float radii) {
    return new Radii(radii, radii, radii, radii);
  }
}

static class Renderer {
  private final PApplet p;

  public Renderer(PApplet p) {
    this.p = p;
  }

  public void box(Constraints constraints) {
    final var offset = constraints.offset;
    final var size = constraints.size;
  
    p.rect(offset.x, offset.y, size.x, size.y, RADII_DEFAULT);
  }
  
  
  public void box(Constraints constraints, color bgColor) {
    final var offset = constraints.offset;
    final var size = constraints.size;
  
    final var oldFill = p.g.fillColor;
  
    p.fill(bgColor);
  
    p.rect(offset.x, offset.y, size.x, size.y, RADII_DEFAULT);
  
    p.fill(oldFill);
  }
  
  public void box(Constraints constraints, color bgColor, Radii radii) {
    final var offset = constraints.offset;
    final var size = constraints.size;
  
    final var oldFill = p.g.fillColor;
  
    p.fill(bgColor);
  
    p.rect(offset.x, offset.y, size.x, size.y, radii.topLeft, radii.topRight, radii.bottomRight, radii.bottomLeft);
  
    p.fill(oldFill);
  }
  
  Point drawText(String content, Point offset, color textColor) {
    final var oldFill = p.g.fillColor;
  
    p.fill(textColor);
  
    p.text(content, offset.x, offset.y);
  
    p.fill(oldFill);
  
    return new Point((int) p.textWidth(content), (int) (p.textAscent() + p.textDescent()));
  }
  
  Point drawText(String content, Point offset) {
    return drawText(content, offset, ColorScheme.Text);
  }
}

final Renderer r = new Renderer(this);

static final int OCTAVES = 4;
static final int BASE_OCTAVE = 3;

CurrentFrame frame = new CurrentFrame(this);
AnimationManager anim = new AnimationManager(frame);

PlayerSession session;

Path scoreFile;
Score[] scores = new Score[1];

Metronome metronome = new Metronome();

Note currentHoveredNote;
Note lastNote;
Note prevLastNote;
java.util.List<Note> currentNotes = new ArrayList<>();

void drawNotes(int octave, Point offset, Point size, boolean[] pressedNotes, boolean disable) {
  final var noteSize = (int) Math.round(size.x / 7f);
  final var blackNoteWidth = (int) Math.round(size.x / 7f * 0.6f);
  final var blackNoteHeight = (int) Math.round(size.y * 0.6f);

  noStroke();

  // White Notes
  for (int i = 0; i < 7; i++) {
    final var whiteNoteColor = pressedNotes[Notes.whiteNoteToScalesPos(i)]  || disable
      ? ColorScheme.Crust
      : ColorScheme.Mantle;

    r.box(new Constraints(offset.x + i * noteSize, offset.y, noteSize, size.y), whiteNoteColor, Radii.NONE);
  }

  stroke(ColorScheme.Overlay1);
  strokeWeight(1);

  // White Notes lines
  for (int i = 0; i < 7; i++) {
    final var lx = offset.x + (i + 1) * noteSize;
    final var ly = offset.y;

    line(lx, ly, lx, ly + size.y);
  }

  // Black Notes
  for (int i = 0; i < 7; i++) {
    // Skip E and B
    if (i == 2 || i == 6) continue;

    final var blackNoteColor = pressedNotes[Notes.rightBlackNoteToScalesPos(i)] || disable
      ? ColorScheme.Text
      : ColorScheme.Subtext0;

    r.box(
      new Constraints((int) (offset.x + (i + 1) * noteSize - blackNoteWidth / 2), offset.y, blackNoteWidth, blackNoteHeight),
      blackNoteColor,
      new Radii(0, 0, RADII_DEFAULT, RADII_DEFAULT)
    );
  }
}

static final Point metronomeBeatStartOffset = new Point(460, 145);
static final int metronomeBeatWidth = 460;

void drawMetronomeBeatProgress() {
  if (!metronome.isPlaying()) {
    stroke(ColorScheme.Blue);
    strokeWeight(1);

    r.box(new Constraints(820, 53, 100, 30), BG, Radii.NONE);

    rectMode(CENTER);

    final var beats = metronome.getBeats();
    final var beatGap = (int) Math.round(metronomeBeatWidth / (float) beats);

    for (int i = 0; i < beats; i++) {
      final var x = i == 0
        ? metronomeBeatStartOffset.x + beatGap / 2
        : metronomeBeatStartOffset.x + i * beatGap + beatGap / 2;

      r.box(
        new Constraints(x, metronomeBeatStartOffset.y, 20, 20),
        BG,
        Radii.NONE
      );
    }

    rectMode(CORNER);
    strokeWeight(STROKE_DEFAULT);

    return;
  }

  final var beats = metronome.getBeats();
  final var beatProgress = metronome.getBeatProgress();
  final var barProgress = metronome.getBarProgress();
  final var beatPos = metronome.getBeat();

  final var barColorAlphaProgress = TimingFunctions.EASE_IN_OUT.apply(1 - barProgress);
  final var barColorAlpha = (int) Math.floor(barColorAlphaProgress * 255);
  final var barColor = (ColorScheme.Blue & 0xffffff) | (barColorAlpha << 24);

  final var beatColorAlphaProgress = TimingFunctions.EASE_OUT.apply(1 - beatProgress);
  final var beatColorAlpha = (int) Math.floor(beatColorAlphaProgress * 255);
  final var beatColor = (ColorScheme.Blue & 0xffffff) | (beatColorAlpha << 24);

  noStroke();
  r.box(new Constraints(820, 53, (int) Math.round(barProgress * 100), 15),  barColor, Radii.NONE); 
  r.box(new Constraints(820, 68, (int) Math.round(beatProgress * 100), 15), beatColor, Radii.NONE); 

  stroke(ColorScheme.Blue);
  rectMode(CENTER);
  strokeWeight(1);

  final var beatGap = (int) Math.round(metronomeBeatWidth / (float) beats);

  for (int i = 0; i < beats; i++) {
    final var x = i == 0
      ? metronomeBeatStartOffset.x + beatGap / 2
      : metronomeBeatStartOffset.x + i * beatGap + beatGap / 2;

    r.box(
      new Constraints(x, metronomeBeatStartOffset.y, 20, 20),
      i == beatPos ? ColorScheme.Blue : BG,
      Radii.NONE
    );
  }

  rectMode(CORNER);
  strokeWeight(STROKE_DEFAULT);
}

class MultiTrackPlayer {
  private final java.util.List<Score> scores;
  private final PlayerSession session;

  private final int visibleLines = 7;
  private final int lineHeight = 40;
  private final int trackWidth = 380;
  private final int trackPadding = 20;
  private final int topMargin = 20;
  private final int progressBarHeight = 8;

  private InnerApplet inner;
  private Renderer r;
  private float[] autoplayBPM;
  private int[] autoplayMeasure;
  private double[] autoplayProgress;
  private boolean opened = false;

  public MultiTrackPlayer(PlayerSession session) {
    this.scores = session.getScores();
    this.session = session;
    this.autoplayBPM = new float[scores.size()];
    this.autoplayMeasure = new int[scores.size()];
    this.autoplayProgress = new double[scores.size()];

    for (int i = 0; i < scores.size(); i++) {
      autoplayBPM[i] = scores.get(i).getStartingBPM();
      autoplayMeasure[i] = scores.get(i).getStartingMeasure();
      autoplayProgress[i] = 0.0;
    }
  }

  public void open() {
    if (opened) return;

    inner = new InnerApplet();
    r = new Renderer(inner);
    PApplet.runSketch(new String[]{inner.getClass().getName()}, inner);

    opened = true;
  }

  public void close() {
    if (!opened) return;

    inner.close();
    inner.dispose();
    inner = null;

    opened = false;
  }

  public void toggle() {
    if (opened) open();
    else close();
  }

  public boolean isOpened() { return opened; }

  private void drawTrack(int trackIndex) {
    final var score = scores.get(trackIndex);

    if (score == null) return;

    final var trackXOffset = trackIndex * (trackWidth + trackPadding) + 30;

    drawTrackHeader(trackIndex, trackXOffset);

    drawTrackContent(trackIndex, trackXOffset);

    drawProgressBar(trackIndex, trackXOffset);

    drawTrackInfo(trackIndex, trackXOffset);
  }

  private void drawTrackHeader(int trackIndex, int xOffset) {
    final var headerText = "Track " + (trackIndex + 1);

    r.drawText(headerText, new Point(xOffset, topMargin - 10));
  }

  private void drawTrackContent(int trackIndex, int xOffset) {
    final var score = scores.get(trackIndex);
    if (score == null || inner == null) return;

    final var cursor = session != null ? session.getCursorFor(trackIndex) : 0;
    final var commands = score.getCommands();
    final var around = Utils.aroundList(commands, cursor, visibleLines);
    final var localCursor = around.getLocalCursor();
    final var slice = around.get();
    final var lineOffset = new Point(xOffset, topMargin);
    final var lineSize = new Point(trackWidth, lineHeight);

    if (session != null && session.isPlaying()) {
      final var newBPM = session.getBPMFor(trackIndex);

      if (autoplayBPM[trackIndex] != newBPM)
        autoplayBPM[trackIndex] = newBPM;

      autoplayMeasure[trackIndex] = session.getMeasureFor(trackIndex);
    }

    inner.noStroke();

    for (int i = 0; i < slice.size(); i++) {
      final var command = slice.get(i);
      final var isCurrent = i == localCursor;
      final var bgColor = isCurrent ? ColorScheme.Crust : ColorScheme.Mantle;
      final var by = lineOffset.y + lineSize.y * i;

      r.box(new Constraints(lineOffset.x, by, lineSize.x, lineSize.y), bgColor, Radii.NONE);

      drawCommandText(command, lineOffset, lineSize, by, i, xOffset);
    }

    autoplayProgress[trackIndex] = (double) cursor / commands.size();
  }

  private void drawCommandText(ScoreCommand command, Point lineOffset, Point lineSize, int by, int lineIndex, int xOffset) {
    if (inner == null) return;

    inner.textAlign(LEFT, CENTER);

    final var textOffset = by + lineSize.y / 2;

    if (command instanceof ScoreCommand.PlayNote) {
      final var playNote = (ScoreCommand.PlayNote) command;

      r.drawText(playNote.getFrequency().toString(), new Point(lineOffset.x + 10, textOffset));
      inner.textAlign(RIGHT, CENTER);
      r.drawText(Float.toString(playNote.getDuration()), new Point(lineOffset.x + lineSize.x - 60, textOffset));
      r.drawText(Float.toString(playNote.getNextNoteDuration()), new Point(lineOffset.x + lineSize.x - 10, textOffset));
    } else if (command instanceof ScoreCommand.Stop) {
      r.drawText("Stop", new Point(lineOffset.x + 10, textOffset), ColorScheme.Flamingo);
    } else if (command instanceof ScoreCommand.Replay) {
      r.drawText("Replay", new Point(lineOffset.x + 10, textOffset), ColorScheme.Green);
    } else if (command instanceof ScoreCommand.ChangeBPM) {
      final var changeBPM = (ScoreCommand.ChangeBPM) command;

      r.drawText("BPM", new Point(lineOffset.x + 10, textOffset), ColorScheme.Blue);
      inner.textAlign(RIGHT, CENTER);
      r.drawText(Float.toString(changeBPM.getBPM()), new Point(lineOffset.x + lineSize.x - 10, textOffset));
    } else if (command instanceof ScoreCommand.ChangeMeasure) {
      final var changeMeasure = (ScoreCommand.ChangeMeasure) command;

      r.drawText("Measure", new Point(lineOffset.x + 10, textOffset), ColorScheme.Peach);
      inner.textAlign(RIGHT, CENTER);
      r.drawText(Integer.toString(changeMeasure.getMeasure()), new Point(lineOffset.x + lineSize.x - 10, textOffset));
    } else if (command instanceof ScoreCommand.ChangeVolume) {
      final var changeVolume = (ScoreCommand.ChangeVolume) command;

      r.drawText("Volume", new Point(lineOffset.x + 10, textOffset), ColorScheme.Lavender);
      inner.textAlign(RIGHT, CENTER);
      r.drawText(Float.toString(changeVolume.getVolume()), new Point(lineOffset.x + lineSize.x - 10, textOffset));
    } else if (command instanceof ScoreCommand.Rest) {
      final var rest = (ScoreCommand.Rest) command;

      r.drawText("Rest", new Point(lineOffset.x + 10, textOffset), ColorScheme.Subtext0);
      inner.textAlign(RIGHT, CENTER);
      r.drawText(Float.toString(rest.getDuration()), new Point(lineOffset.x + lineSize.x - 10, textOffset));
    } else if (command instanceof ScoreCommand.Pitch) {
      final var pitch = (ScoreCommand.Pitch) command;

      r.drawText("Pitch", new Point(lineOffset.x + 10, textOffset), ColorScheme.Red);
      r.drawText(pitch.getBeforeFrequency().toString(), new Point(lineOffset.x + 60, textOffset), ColorScheme.Subtext0);
      r.drawText(pitch.getAfterFrequency().toString(), new Point(lineOffset.x + 135, textOffset));
      inner.textAlign(RIGHT, CENTER);
      r.drawText(pitch.getFunction().getName(), new Point(lineOffset.x + lineSize.x - 120, textOffset));
      r.drawText(Integer.toString(pitch.getQuality()), new Point(lineOffset.x + lineSize.x - 90, textOffset));
      r.drawText(Float.toString(pitch.getDuration()), new Point(lineOffset.x + lineSize.x - 60, textOffset));
      r.drawText(Float.toString(pitch.getNextNoteDuration()), new Point(lineOffset.x + lineSize.x - 10, textOffset));
    } else if (command instanceof ScoreCommand.Vibrato) {
      final var vib = (ScoreCommand.Vibrato) command;

      r.drawText("Vib", new Point(lineOffset.x + 10, textOffset), ColorScheme.Sky);
      r.drawText(vib.getFrequency1().toString(), new Point(lineOffset.x + 60, textOffset), ColorScheme.Subtext0);
      r.drawText(vib.getFrequency2().toString(), new Point(lineOffset.x + 135, textOffset));
      inner.textAlign(RIGHT, CENTER);
      r.drawText(Integer.toString(vib.getCount()), new Point(lineOffset.x + lineSize.x - 90, textOffset));
      r.drawText(Float.toString(vib.getDuration()), new Point(lineOffset.x + lineSize.x - 60, textOffset));
      r.drawText(Float.toString(vib.getNextNoteDuration()), new Point(lineOffset.x + lineSize.x - 10, textOffset));
    }

    inner.textAlign(LEFT, CENTER);
  }

  private void drawProgressBar(int trackIndex, int xOffset) {
    final var barY = topMargin + lineHeight * visibleLines;

    r.box(new Constraints(xOffset, barY, trackWidth, progressBarHeight), ColorScheme.Surface0, Radii.NONE);
    r.box(new Constraints(xOffset, barY, (int) Math.round(trackWidth * autoplayProgress[trackIndex]), progressBarHeight), ColorScheme.Blue, Radii.NONE);
  }

  private void drawTrackInfo(int trackIndex, int xOffset) {
    final var score = scores.get(trackIndex);
    if (score == null) return;

    final var infoY = topMargin + lineHeight * visibleLines + progressBarHeight + 20;

    final var infoText = String.format("BPM: %s, Measure: %d (%.1f%%)\nOffset: %s, A4=%sHz",
        Float.toString(autoplayBPM[trackIndex]),
        autoplayMeasure[trackIndex],
        autoplayProgress[trackIndex] * 100,
        Float.toString(score.getOffset()),
        Double.toString(score.getBaseA4Frequency()));
        
    r.drawText(infoText, new Point(xOffset, infoY));
  }
    
  public Dimension getRequiredSize() {
    final var width = scores.size() * (trackWidth + trackPadding) + 30;
    final var height = topMargin + lineHeight * visibleLines + progressBarHeight + 60;

    return new Dimension(width, height);
  }
    
  public double getProgressForTrack(int trackIndex) {
    if (trackIndex >= 0 && trackIndex < autoplayProgress.length)
      return autoplayProgress[trackIndex];

    return 0.0;
  }

  public float getBPMForTrack(int trackIndex) {
    if (trackIndex >= 0 && trackIndex < autoplayBPM.length)
      return autoplayBPM[trackIndex];

    return 150.0f;
  }

  public boolean hasEnoughDevices() {
    return devices != null && devices.size() >= scores.size();
  }

  class InnerApplet extends PApplet {
    JButton autoplayButton;

    void settings() {
      final var size = getRequiredSize();

      size(size.width + topMargin, size.height + 30);
    }

    void setup() {
      windowTitle("Player Session for " + (scoreFile != null ? scoreFile.getFileName() : "a score"));
    }

    void toggleAutoplay() {
      if (!hasEnoughDevices()) return;
    
      if (!session.isPlaying()) {
        autoplayButton.setBackground(pColorToAWT(ColorScheme.Red));
        autoplayButton.setText("停止");

        var it = devices.iterator();
        final java.util.List<PlaybackOutput> outputs = new ArrayList<>();

        for (int i = 0; i < scores.size(); i++) {
          if (!it.hasNext()) throw new IllegalStateException("Devices count was changed");

          outputs.add(new FraiselaitOutput(it.next()));
        }

        session.start(outputs).thenRun(() -> {
          autoplayButton.setBackground(pColorToAWT(ColorScheme.Green));
          autoplayButton.setText("再生");
        });
    
        return;
      }
    
      autoplayButton.setBackground(pColorToAWT(ColorScheme.Green));
      autoplayButton.setText("再生");
    
      session.stop();
    }

    protected PSurface initSurface() {
      surface = (PSurface) super.initSurface();
    
      final var canvas = (Canvas) surface.getNative();
      final var pane = (JLayeredPane) canvas.getParent().getParent();

      autoplayButton = new JButton("再生");
      autoplayButton.setForeground(pColorToAWT(BG));
      autoplayButton.setBackground(pColorToAWT(ColorScheme.Green));
      autoplayButton.setBounds(width - (10 + 60), height - (10 + 30), 60, 30);
      autoplayButton.addActionListener((e) -> { toggleAutoplay(); });
      pane.add(autoplayButton);

      return surface;
    }

    public void close() {
      surface.setVisible(false);
    }

    void draw() {
      background(BG);
      stroke(STROKE_DEFAULT);
      textSize(TEXT_DEFAULT);

      if (scores == null) {
        close();

        return;
      }

      if (inner == null) return;
      if (scores.isEmpty()) return;

      for (int trackIndex = 0; trackIndex < scores.size(); trackIndex++)
        drawTrack(trackIndex);

      if (!hasEnoughDevices()) {
        textAlign(LEFT, CENTER);

        r.drawText("E: Not enough devices", new Point(30, height - 36), ColorScheme.Red);
      }
    }
  }
}

MultiTrackPlayer player;

// Autoplay

JButton autoplayFileButton;
JButton autoplayRefreshButton;
JSpinner autoplayPartsField;
JButton autoplayWindowToggleButton;

// Metronome

JSpinner bpmField;
JSpinner beatsField;
JButton metronomeButton;

// Notes

JComboBox waveformCombo;

// Fraiselait

JButton fraiselaitRefreshButton;

MultiDeviceOrchestrator orchestrator = new MultiDeviceOrchestrator(152000);

Set<FraiselaitDevice> devices;
FraiselaitDevice device;

void setup() {
  size(1200, 800);
  windowTitle("Fraiselait Player");

  surface.setVisible(true);
  orchestrator.start();
}

void loadScoreFile(JLayeredPane pane) {
  final var chooser = new JFileChooser();

  chooser.setFileFilter(new FileNameExtensionFilter("Raspberyth Score File (*.txt)", "txt"));

  final var ret = chooser.showOpenDialog(pane);

  if (ret == JFileChooser.APPROVE_OPTION) {
    scoreFile = chooser.getSelectedFile().toPath();

    try {
      for (int i = 0; i < scores.length; i++)
        scores[i] = null; // clear

      if (scores.length > 1) {
        for (int i = 0; i < scores.length; i++) {
          try {
            scores[i] = Score.loadFromPath(scoreFile, "sound" + i);
          } catch (Exception e) {
            throw new RuntimeException("Error at sound " + i, e);
          }
        }
      } else scores[0] = Score.loadFromPath(scoreFile);

      if (session != null)
        session.close();

      if (player != null)
        player.close();

      session = new PlayerSession(Arrays.asList(scores));

      player = new MultiTrackPlayer(session);

      autoplayWindowToggleButton.setText("開く");
      autoplayWindowToggleButton.setForeground(pColorToAWT(ColorScheme.Text));
      autoplayWindowToggleButton.setBackground(pColorToAWT(ColorScheme.Surface0));
    } catch (Exception err) {
      err.printStackTrace();
    }
  } else if (ret == JFileChooser.ERROR_OPTION) {
    println("Warning: JFileChooser was reported an error");
  }
}

void refreshScoreFile() {
  if (scoreFile == null) return;
  if (session != null && session.isPlaying()) return;

  try {
    for (int i = 0; i < scores.length; i++)
      scores[i] = null; // clear

    if (scores.length > 1) {
      for (int i = 0; i < scores.length; i++) {
        try {
          scores[i] = Score.loadFromPath(scoreFile, "sound" + i);
        } catch (Exception e) {
          throw new RuntimeException("Error at sound " + i, e);
        }
      }
    } else scores[0] = Score.loadFromPath(scoreFile);

    if (session != null)
      session.close();

    if (player != null)
      player.close();

    session = new PlayerSession(Arrays.asList(scores));

    player = new MultiTrackPlayer(session);

    player.open();

    autoplayWindowToggleButton.setText("閉じる");
    autoplayWindowToggleButton.setForeground(pColorToAWT(ColorScheme.Text));
    autoplayWindowToggleButton.setBackground(pColorToAWT(ColorScheme.Surface0));
  } catch (Exception err) {
    err.printStackTrace();
  }
}

void togglePlayerWindow() {
  if (player == null) return;

  if (player.isOpened()) {
    try {
      session.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

    player.close();

    return;
  }
  
  session = new PlayerSession(Arrays.asList(scores));

  player = new MultiTrackPlayer(session);
}

void toggleMetronome() {
  if (session != null && session.isPlaying()) return;

  if (!metronome.isPlaying()) {
    metronome.start();

    metronomeButton.setText("停止");
  } else {
    metronome.stop();

    metronomeButton.setText("開始");
  }
}

void setWaveform(WaveformType type) {
  if (session != null && session.isPlaying()) return;
 
  orchestrator.sendAll(new CommandBuilder().changeWaveform(type).build());
}

protected PSurface initSurface() {
  surface = (PSurface) super.initSurface();

  final var canvas = (Canvas) surface.getNative();
  final var pane = (JLayeredPane) canvas.getParent().getParent();

  // Autoplay

  autoplayFileButton = new JButton("ファイルを選択");
  autoplayFileButton.setForeground(pColorToAWT(ColorScheme.Text));
  autoplayFileButton.setBackground(pColorToAWT(ColorScheme.Surface0));
  autoplayFileButton.setBounds(30, 84, 120, 30);
  autoplayFileButton.addActionListener((e) -> { loadScoreFile(pane); });
  pane.add(autoplayFileButton);

  autoplayRefreshButton = new JButton("更新");
  autoplayRefreshButton.setForeground(pColorToAWT(ColorScheme.Text));
  autoplayRefreshButton.setBackground(pColorToAWT(ColorScheme.Surface0));
  autoplayRefreshButton.setBounds(350, 54, 60, 30);
  autoplayRefreshButton.addActionListener((e) -> { refreshScoreFile(); });
  pane.add(autoplayRefreshButton);

  final var autoplayPartsModel = new SpinnerNumberModel(scores.length, 1, 10, 1);

  autoplayPartsField = new JSpinner(autoplayPartsModel);
  autoplayPartsField.setBounds(80, 140, 40, 30);
  autoplayPartsField.addChangeListener((e) -> {
    final var newScores = new Score[(int) autoplayPartsField.getValue()];

    for (int i = 0; i < scores.length && i < newScores.length; i++)
      newScores[i] = scores[i];

    scores = newScores;
  });

  pane.add(autoplayPartsField);

  autoplayWindowToggleButton = new JButton("開く");
  autoplayWindowToggleButton.setForeground(pColorToAWT(ColorScheme.Subtext0));
  autoplayWindowToggleButton.setBackground(pColorToAWT(ColorScheme.Surface1));
  autoplayWindowToggleButton.setBounds(140, 140, 80, 30);
  autoplayWindowToggleButton.addActionListener((e) -> {
    if (player == null) return;

    if (player.isOpened()) {
      player.close();

      autoplayWindowToggleButton.setText("開く");
    } else {
      player.open();

      autoplayWindowToggleButton.setText("閉じる");
    }
  });
  pane.add(autoplayWindowToggleButton);

  // Metronome

  final var bpmModel = new SpinnerNumberModel((double) metronome.getBPM(), 1, 3390, 1);

  bpmField = new JSpinner(bpmModel);
  bpmField.setBounds(500, 53, 80, 30);
  pane.add(bpmField);

  final var beatsModel = new SpinnerNumberModel(metronome.getBeats(), 2, 12, 1);

  beatsField = new JSpinner(beatsModel);
  beatsField.setBounds(660, 53, 40, 30);
  pane.add(beatsField);

  metronomeButton = new JButton("開始");
  metronomeButton.setForeground(pColorToAWT(ColorScheme.Blue));
  metronomeButton.setBackground(pColorToAWT(ColorScheme.Surface0));
  metronomeButton.setBounds(725, 53, 70, 30);
  metronomeButton.addActionListener((e) -> { toggleMetronome(); });
  pane.add(metronomeButton);

  // Notes

  waveformCombo = new JComboBox(
    new DefaultComboBoxModel(WaveformType.values())
  );
  waveformCombo.setForeground(pColorToAWT(ColorScheme.Text));
  waveformCombo.setBackground(pColorToAWT(ColorScheme.Surface0));
  waveformCombo.setBounds(1075, 173, 90, 30);
  waveformCombo.addActionListener((e) -> { setWaveform((WaveformType) waveformCombo.getSelectedItem()); });
  pane.add(waveformCombo);

  // Friaselait

  fraiselaitRefreshButton = new JButton("更新");
  fraiselaitRefreshButton.setForeground(pColorToAWT(ColorScheme.Text));
  fraiselaitRefreshButton.setBackground(pColorToAWT(ColorScheme.Surface0));
  fraiselaitRefreshButton.setBounds(470, 280, 60, 30);
  fraiselaitRefreshButton.addActionListener((e) -> { devices = orchestrator.getDevices(); });
  pane.add(fraiselaitRefreshButton);

  return surface;
}

void draw() {
  background(BG);
  fill(BG);
  strokeWeight(STROKE_DEFAULT);
  textAlign(LEFT, TOP);
  textSize(TEXT_DEFAULT);

  if (device != null) {
    if (lastNote != null && !lastNote.equals(prevLastNote)) {
      new FraiselaitOutput(device).tone(lastNote.toFreq(), 1.0);

      prevLastNote = lastNote;
    } else if (lastNote == null && prevLastNote != null) {
      new FraiselaitOutput(device).noTone();

      prevLastNote = null;
    }
  }

  // Updating UI state

  // Metronome

  if (session == null || !session.isPlaying())
    metronome.setBPM((double) bpmField.getValue());

  metronome.setBeats((int) beatsField.getValue());

  // Fraiselait

  if (devices == null) {
    var newDevices = orchestrator.getDevices();

    if (newDevices.size() != 0)
      devices = newDevices;
  }

  if (device != null && device.getStatus() != ConnectionStatus.CONNECTED)
    device = null;

  if (device == null && devices != null && devices.size() != 0) {
    for (final var d : devices) {
      if (d.getStatus() == ConnectionStatus.CONNECTED)
        device = d;
    }
  }

  // Drawing

  // Main Notes

  final var baseOffset = new Point(0, 500);
  final var baseSize = new Point((int) Math.round(width / (float) OCTAVES), 300);

  for (int i = 0; i < OCTAVES; i++) {
    final var capturedI = i;
    var pressedNotes = new boolean[12];

    currentNotes.stream().filter((note) -> note.octave == capturedI + BASE_OCTAVE).forEach((note) -> {
      pressedNotes[note.index] = true;
    });

    drawNotes(
      i + BASE_OCTAVE,
      new Point(baseOffset.x + baseSize.x * i, baseOffset.y),
      baseSize,
      pressedNotes,
      session != null && session.isPlaying()
    );
  }

  // Fix lines
  for (int i = 0; i < OCTAVES; i++) {
    final var lx = baseOffset.x + baseSize.x * (i + 1);

    line(lx, 500, lx, 800);
  }

  strokeWeight(1);
  line(0, 500, width, 500);

  strokeWeight(STROKE_DEFAULT);

  // Autoplay

  final var autoplayColor = session != null && session.isPlaying() ? ColorScheme.Green : ColorScheme.Overlay0;

  stroke(autoplayColor);
  r.box(new Constraints(20, 20, 400, 450));
  r.drawText("Autoplay", new Point(30, 30), autoplayColor);

  r.drawText("Score File", new Point(30, 60));

  if (scoreFile != null) {
    var fname = scoreFile.getFileName().toString();

    if (fname.length() > 22) fname = fname.substring(0, 22) + "...";

    r.drawText(fname, new Point(160, 92));
  }

  r.drawText("Parts", new Point(30, 147));

  // Metronome

  stroke(ColorScheme.Blue);
  r.box(new Constraints(440, 20, 500, 200));
  r.drawText("Metronome", new Point(450, 30), ColorScheme.Blue);

  r.drawText("BPM", new Point(450, 60));

  r.drawText("Beats", new Point(600, 60));

  drawMetronomeBeatProgress();

  // Notes

  stroke(ColorScheme.Red);
  r.box(new Constraints(960, 20, 220, 200));
  r.drawText("Notes", new Point(970, 30), ColorScheme.Red);

  r.drawText(currentNotes.toString(), new Point(980, 60));

  r.drawText("A0 Freq.: " + Double.toString(Notes.A0_FREQ) + "Hz", new Point(980, 84));
  r.drawText("Edit Note.java to\nchange A0 freq.", new Point(980, 114));

  r.drawText("Waveform", new Point(980, 180));

  // Fraiselait

  stroke(ColorScheme.Lavender);
  r.box(new Constraints(440, 240, 740, 230));
  r.drawText("Fraiselait", new Point(450, 250), ColorScheme.Lavender);

  if (device != null) {
    r.drawText("Using device: %s (%s)".formatted(device.getId(), device.getPort()), new Point(540, 287));
  }

  r.drawText("Available devices:", new Point(470, 320));

  var deviceListOffset = new Point(480, 350);
  var deviceListIndex = 0;

  if (devices != null) {
    for (final var d : devices) {
      r.drawText(
        "%s (%s)".formatted(d.getId(), d.getPort()),
        new Point(deviceListOffset.x, deviceListOffset.y + deviceListIndex * 24)
      );

      deviceListIndex++;
    }
  } else if (devices == null || devices.size() == 0) {
    r.drawText("- Empty -", deviceListOffset);
  }
}

void dispose() {
  if (session != null) {
    try {
      session.close();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      session = null;
    }
  }
}

void checkMainNotesHover() {
  final var baseOffset = 500;
  final var whiteNoteSize = (int) Math.round(width / (7f * OCTAVES));
  final var blackNoteWidth = (int) Math.round(width / (7f * OCTAVES) * 0.6f);
  final var blackNoteHeight = (int) Math.round(300 * 0.6f);

  if (mouseY > baseOffset + blackNoteHeight) {
    // Only White Notes
    final var rawPos = mouseX / whiteNoteSize;

    currentHoveredNote = Note.fromWhiteNote(rawPos % 7, rawPos / 7 + BASE_OCTAVE);

    return;
  }

  // White Notes & Black Notes

  // Check approx. white note pos
  final var rawWhitePos = mouseX / whiteNoteSize;
  final var rawWhiteOffset = mouseX % whiteNoteSize;

  final var rawWhitePosRem7 = rawWhitePos % 7;

  final var leftIsBlackNote = rawWhiteOffset < blackNoteWidth / 2;
  final var rightIsBlackNote = rawWhiteOffset > whiteNoteSize - (blackNoteWidth / 2);

  // White Notes
  if (
    // Left
    (
         !leftIsBlackNote
      && (rawWhitePosRem7 == 2 || rawWhitePosRem7 == 6)
    )
    // Right
    || (
         !rightIsBlackNote
      && (rawWhitePosRem7 == 3 || rawWhitePosRem7 == 0)
    )
    // Middle
    || !leftIsBlackNote && !rightIsBlackNote
  ) {
    currentHoveredNote = Note.fromWhiteNote(rawWhitePosRem7, rawWhitePos / 7 + BASE_OCTAVE);

    return;
  }

  // Black Notes

  if (leftIsBlackNote) {
    currentHoveredNote = Note.fromLeftBlackNote(rawWhitePosRem7, rawWhitePos / 7 + BASE_OCTAVE);

    return;
  }

  if (rightIsBlackNote) {
    currentHoveredNote = Note.fromRightBlackNote(rawWhitePosRem7, rawWhitePos / 7 + BASE_OCTAVE);

    return;
  }
}

void mouseMoved() {
  if (mouseY >= 500) {
    cursor(
      session == null || !session.isPlaying() ? HAND : ARROW
    );

    checkMainNotesHover();

    return;
  }

  currentHoveredNote = null;

  cursor(ARROW);
}

void keyPressed() {
  if (session == null || !session.isPlaying()) {
    final var maybeNote = Notes.bindingOf(key);
  
    maybeNote.ifPresent((note) -> {
      if (!currentNotes.contains(note)) {
        currentNotes.add(note);
  
        if (!note.equals(lastNote)) lastNote = note;
      }
    });
  }
}

void keyReleased() {
  if (session == null || !session.isPlaying()) {
    final var maybeNote = Notes.bindingOf(key);
  
    maybeNote.ifPresent((note) -> {
      currentNotes.remove(note);
  
      if (note.equals(lastNote))
        lastNote = currentNotes.size() == 0 ? null : currentNotes.get(currentNotes.size() - 1);
    });
  }
}

void mousePressed() {
  if (mouseButton != LEFT) return;

  // Main Notes Click
  if (session == null || !session.isPlaying()) {
    if (currentHoveredNote == null) return;
  
    if (!currentNotes.contains(currentHoveredNote)) {
      currentNotes.add(currentHoveredNote);
      lastNote = currentHoveredNote;
    }
  }
}

void mouseReleased() {
  if (mouseButton != LEFT) return;

  // Main Notes Release
  if (session == null || !session.isPlaying()) {
    if (currentHoveredNote == null) return;
  
    currentNotes.remove(currentHoveredNote);
  
    lastNote = currentNotes.size() == 0 ? null : currentNotes.get(currentNotes.size() - 1);
  }
}

void mouseDragged() {
  if (mouseButton != LEFT) return;
  if (mouseX < 0 || mouseX > width || mouseY < 0 || mouseY > height) return;


  if (session == null || !session.isPlaying()) {
    if (mouseY >= 500) {
      final var prevHoveredNote = currentHoveredNote;
  
      if (prevHoveredNote == null) return;
  
      checkMainNotesHover();
  
      if (currentHoveredNote == null) return;
  
      if (!prevHoveredNote.equals(currentHoveredNote)) {
        currentNotes.remove(prevHoveredNote);
        currentNotes.add(currentHoveredNote);
        lastNote = currentHoveredNote;
      }
  
      return;
    }
  
    if (currentNotes.contains(currentHoveredNote)) currentNotes.remove(currentHoveredNote);
  
    currentHoveredNote = null;
  }
}
