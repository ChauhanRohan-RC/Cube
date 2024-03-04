package gl.animation;

import org.jetbrains.annotations.NotNull;

public class FloatAnimator extends Animator<Float> {

    public FloatAnimator(float startVal, float endVal) {
        super(startVal, endVal);
    }

    @NotNull
    public FloatAnimator reverse() {
        final FloatAnimator anim = new FloatAnimator(mEndValue, mStartVal);
        anim.setDurationMs(getDurationMs());
        anim.setInterpolator(getInterpolator());
        anim.setDefaultInterpolator(getDefaultInterpolator());
        return anim;
    }

    @Override
    protected void doUpdate(float elapsedFraction) {
        updateCurValue(mStartVal + ((mEndValue - mStartVal) * getInterpolator().getInterpolation(elapsedFraction)));
    }
}
