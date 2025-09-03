import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

class AnimationHandle {
  final int startingTimeMillis;

  public AnimationHandle(int startingTimeMillis) {
    this.startingTimeMillis = startingTimeMillis;
  }
}

public class AnimationManager {
  private final CurrentFrame currentFrame;
  private final Queue<AnimationTimeline> queue = new LinkedList<>();
  private final Map<AnimationTimeline, AnimationHandle> timelines = new HashMap<>();

  public AnimationManager(CurrentFrame currentFrame) {
    this.currentFrame = currentFrame;
  }

  public void play(AnimationTimeline timeline) {
    queue.add(timeline);
  }

  public void update() {
    final var lastFrameTime = currentFrame.getLastFrameTime();

    while (!queue.isEmpty()) {
      final var timeline = queue.poll();

      if (timeline.onStarted != null)
        timeline.onStarted.run();

      timelines.put(timeline, new AnimationHandle((int) lastFrameTime.toMillis()));
    }

    for (var it = timelines.entrySet().iterator(); it.hasNext(); ) {
      final var entry = it.next();
      final var timeline = entry.getKey();
      final var handle = entry.getValue();

      final var progress = (double) (currentFrame.getLastFrameTime().toMillis() - handle.startingTimeMillis) / timeline.getDuration().toMillis();

      for (int i = timeline.finishedKeyframes; i < timeline.getKeyframes().size(); i++) {
        final var keyframe = timeline.getKeyframes().get(i);

        if (keyframe.getProgress() <= progress) {
          timeline.finishedKeyframes++;
        } else {
          break;
        }
      }

      timeline.onProgress.accept(progress);

      if (timeline.finishedKeyframes == timeline.getKeyframes().size()) {
        if (timeline.onFinished != null)
          timeline.onFinished.run();

        it.remove();
      }
    }
  }
}
