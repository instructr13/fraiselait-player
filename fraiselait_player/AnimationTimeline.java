import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class AnimationTimeline {
  final Consumer<Double> onProgress;
  private final Duration duration;
  private final List<AnimationKeyframe> keyframes;

  int finishedKeyframes = 0;
  Runnable onStarted;
  Runnable onFinished;

  public AnimationTimeline(Duration duration, Consumer<Double> onProgress, List<AnimationKeyframe> keyframes) {
    this.duration = duration;
    this.onProgress = onProgress;
    this.keyframes = keyframes;
  }

  static AnimationTimeline of(Duration duration, Consumer<Double> onProgress, AnimationKeyframe... keyframes) {
    return new AnimationTimeline(duration, onProgress, List.of(keyframes));
  }

  public void add(AnimationKeyframe keyframe) {
    keyframes.add(keyframe);

    keyframes.sort(Comparator.comparingDouble(AnimationKeyframe::getProgress));
  }

  public Duration getDuration() {
    return duration;
  }

  public List<AnimationKeyframe> getKeyframes() {
    return keyframes;
  }

  public void onStarted(Runnable callback) {
    onStarted = callback;
  }

  public void onFinished(Runnable callback) {
    onFinished = callback;
  }

  @Override
  public final boolean equals(Object o) {
    if (!(o instanceof AnimationTimeline)) return false;

    AnimationTimeline obj = (AnimationTimeline) o;
    return Objects.equals(duration, obj.duration) && Objects.equals(keyframes, obj.keyframes);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(duration);
    result = 31 * result + Objects.hashCode(keyframes);
    return result;
  }
}
