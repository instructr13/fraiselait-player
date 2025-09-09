import dev.wycey.mido.fraiselait.ConnectionStatus;
import dev.wycey.mido.fraiselait.builtins.orchestrators.MultiDeviceOrchestrator;

CurrentFrame frame = new CurrentFrame(this);

ComponentsRegistry components = new ComponentsRegistry(this);

MultiDeviceOrchestrator orchestrator = new MultiDeviceOrchestrator(460800);

OscillatorState oscillatorState = new OscillatorState();
FraiselaitOutput deviceOutput = null;
OscillatorSession oscillatorSession = new OscillatorSession(oscillatorState);

void setup() {
  size(1300, 900);

  windowTitle("Fraiselait Player");

  orchestrator.start();

  components.registerResource(new AnimationManager(frame));
  components.registerResource(new ButtonRenderer());
  components.registerResource(new KnobRenderer());

  components.register(new NotesKeyboard(oscillatorSession, 260, height - 260));

  components.register(new Header("O  S  C", new Point(40, 20), 780, 120));

  components.register(new GroupHeader("Waveform", new Point(40, 80), 250));
  components.register(new WaveformSelector(new Point(120, 130), oscillatorState));

  components.register(new GroupHeader("Envelope", new Point(40, 360), 250));
  components.register(new EnvelopeComponents(new Point(90, 405), oscillatorState));

  components.register(new GroupHeader("Output", new Point(490, 80), 140));
  components.register(new OutputComponents(new Point(538, 112), oscillatorState));

  components.register(new GroupHeader("Constants", new Point(490, 250), 80));
  components.register(new ConstantsArea(new Point(538, 272)));

  components.register(new Header("D  E  V  I  C  E  S", new Point(850, 20), width - 40 - 850, 180));
  components.register(new DevicesComponents(new Point(850, 80), 400, orchestrator));

  components.register(new Header("A  U  T  O  P  L  A  Y", new Point(850, 280), width - 40 - 850, 190));

  components.setup();
}

void draw() {
  frame.update();

  background(ColorScheme.Background);
  stroke(ColorScheme.Foreground);

  components.draw();

  for (var device : orchestrator.getDevices()) {
    if (device.getStatus() != ConnectionStatus.CONNECTED) continue;

    if (deviceOutput == null || (!deviceOutput.isUsable() && !deviceOutput.getDevice().equals(device))) {
      deviceOutput = new FraiselaitOutput(device);

      oscillatorSession.attachOutput(deviceOutput);

      oscillatorSession.start();
    }
  }
}

void keyPressed() {
  components.onKeyPressed();
}

void keyReleased() {
  components.onKeyReleased();
}

void mouseMoved() {
  components.onMouseMoved();
}

void mousePressed() {
  components.onMousePressed();
}

void mouseDragged() {
  components.onMouseDragged();
}

void mouseReleased() {
  components.onMouseReleased();
}

void mouseClicked() {
  components.onMouseClicked();
}

void dispose() {
  oscillatorSession.stop();

  orchestrator.stop();

  components.dispose();

  super.dispose();
}

