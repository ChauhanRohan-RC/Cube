package gl.animation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public abstract class Animator<T> {

    public enum Finish {
        NORMAL,
        FORCE,
        CANCEL
    }

    @NotNull
    public static final Interpolator DEFAULT_INTERPOLATOR = Interpolator.LINEAR;
    public static final long DEFAULT_DURATION_MS = 300;


    protected final T mStartVal, mEndValue;
    private T mCurVal;
    private long mDurationMs = DEFAULT_DURATION_MS;

    private boolean mAnimating;
    private boolean mFinished;

    @Nullable
    private Long mStartMills;
    @Nullable
    private Long mPausedMs;

    @NotNull
    private Interpolator mDefaultInterpolator = DEFAULT_INTERPOLATOR;
    @Nullable
    private Interpolator mInterpolator;

    @Nullable
    private Object mTag;

    protected Animator(@NotNull T startVal, @NotNull T endValue) {
        mStartVal = startVal;
        mEndValue = endValue;

        mCurVal = mStartVal;
    }


    @NotNull
    public final T getStartValue() {
        return mStartVal;
    }

    @NotNull
    public final T getEndValue() {
        return mEndValue;
    }

    @NotNull
    public final T getCurrentVal() {
        return mCurVal;
    }

    public final long getDurationMs() {
        return mDurationMs;
    }

    public final void setDurationMs(long durationMs) {
        mDurationMs = durationMs;
    }


    @NotNull
    public final Interpolator getDefaultInterpolator() {
        return mDefaultInterpolator;
    }

    protected final void setDefaultInterpolator(@Nullable Interpolator defaultInterpolator) {
        if (defaultInterpolator == null) {
            defaultInterpolator = DEFAULT_INTERPOLATOR;
        }

        mDefaultInterpolator = defaultInterpolator;
    }

    @NotNull
    public final Interpolator getInterpolator() {
        return mInterpolator != null? mInterpolator: mDefaultInterpolator;
    }

    public final void setInterpolator(@Nullable Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    @Nullable
    public Object getTag() {
        return mTag;
    }

    public void setTag(Object tag) {
        mTag = tag;
    }




    public final void reset() {
        mAnimating = false;
        mFinished = false;
        mCurVal = mStartVal;
        mStartMills = null;
        mPausedMs = null;
        onReset();
    }

    public final void start() {
        if (mFinished || mAnimating)
            return;

        boolean resumed = false;
        if (mStartMills == null) {
            mStartMills = System.currentTimeMillis();       // first start
            mCurVal = mStartVal;
        } else if (mPausedMs != null) {
            mStartMills += (System.currentTimeMillis() - mPausedMs);
            resumed = true;
        }

        mPausedMs = null;
        mAnimating = true;
        onStarted(resumed);
    }

    public boolean isRunning() {
        return mAnimating;
    }

    public void pause() {
        if (!mAnimating)
            return;

        mPausedMs = System.currentTimeMillis();
        mAnimating = false;
        onPaused();
    }

    public boolean isPaused() {
        return mPausedMs != null && !(mAnimating || mFinished);
    }

    public boolean isFinished() {
        return mFinished;
    }



    protected void updateCurValue(T value) {
        mCurVal = value;
    }

    protected abstract void doUpdate(float elapsedFraction);

    public final void update() {
        if (mFinished || !mAnimating || mStartMills == null)
            return;

        final long now = System.currentTimeMillis();
        float fraction = (float) (now - mStartMills) / mDurationMs;
        if (fraction < 0) {
            // bad state
            return;
        }

        if (fraction > 1) {
            doFinishInternal(Finish.NORMAL);
        } else {
            doUpdate(fraction);
        }
    }

    @NotNull
    public final T updateAndGetCurrentValue() {
        update();
        return mCurVal;
    }

    public final void finish(boolean cancel) {
        if (mFinished)
            return;

        doFinishInternal(cancel? Finish.CANCEL: Finish.FORCE);
    }

    protected void doFinishInternal(@NotNull Finish how) {
        mAnimating = false;
        mPausedMs = null;
        mCurVal = mEndValue;
        mFinished = true;
        onFinished(how);
    }



    /* Callbacks */

    protected void onStarted(boolean resumed) {
    }

    protected void onPaused() {
    }

    protected void onReset() {
    }

    protected void onFinished(@NotNull Finish how) {
    }


}
