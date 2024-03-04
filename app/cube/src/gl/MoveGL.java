package gl;

import gl.animation.Animator;
import gl.animation.FloatAnimator;
import model.Axis;
import model.cubie.Move;
import org.jetbrains.annotations.NotNull;
import util.live.Listeners;

import java.util.Objects;


public class MoveGL extends FloatAnimator {

    public interface Listener {

        void onMoveStarted(@NotNull MoveGL moveGL, boolean resumed);

        void onMovePaused(@NotNull MoveGL moveGL);

        void onMoveFinished(@NotNull MoveGL moveGL, @NotNull Animator.Finish how);
    }


    @NotNull
    private final Move move;
    private final int quarters;

    private boolean mSaveInStack = true;        // should save in stack by default
    @NotNull
    private final Listeners<Listener> mListeners = new Listeners<>();

    public MoveGL(@NotNull Move move, boolean useNormalizedQuarters) {
        super(0f, Axis.queryRotation(move.axis, useNormalizedQuarters? move.normalizedQuarters: move.quarters));
        this.move = move;
        quarters = useNormalizedQuarters? move.normalizedQuarters: move.quarters;

        setQuarterDurationMs(GLConfig.MOVE_QUARTER_DURATION_MS_DEFAULT);        // set default duration
        setDefaultInterpolator(GLConfig.DEFAULT_MOVE_ANIMATION_INTERPOLATOR);             // set default interpolator
    }

    public MoveGL(@NotNull Move move) {
        this(move, GLConfig.DEFAULT_MOVE_USE_NORMALIZED_QUARTERS);
    }

    @NotNull
    public Move getMove() {
        return move;
    }

    public int getQuarters() {
        return quarters;
    }

    public long getQuarterDurationMs() {
        return Math.round((double) getDurationMs() / Math.abs(quarters));
    }

    public void setQuarterDurationMs(long quarterDurationMs) {
        setDurationMs(quarterDurationMs * Math.abs(quarters));
    }

    public boolean shouldSaveInStack() {
        return mSaveInStack;
    }

    public void setSaveInStack(boolean saveInStack) {
        mSaveInStack = saveInStack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        MoveGL moveGL = (MoveGL) o;
        return quarters == moveGL.quarters && Objects.equals(move, moveGL.move);
    }

    @Override
    public int hashCode() {
        return Objects.hash(move, quarters);
    }

    /* Callbacks */

    public final void addListener(@NotNull Listener listener) {
        mListeners.addListener(listener);
    }

    public final boolean removeListener(@NotNull Listener listener) {
        return mListeners.removeListener(listener);
    }

    public final void ensureListener(@NotNull Listener listener) {
        mListeners.ensureListener(listener);
    }

    protected void onStarted(boolean resumed) {
        mListeners.forEachListener(l -> l.onMoveStarted(MoveGL.this, resumed));
    }

    protected void onPaused() {
        mListeners.forEachListener(l -> l.onMovePaused(MoveGL.this));
    }

    protected void onFinished(@NotNull Animator.Finish how) {
        mListeners.forEachListener(l -> l.onMoveFinished(MoveGL.this, how));
    }

    @Override
    protected void onReset() {

    }
}
