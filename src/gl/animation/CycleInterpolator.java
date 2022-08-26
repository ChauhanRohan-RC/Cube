package gl.animation;

/**
 * Repeats the animation for a specified number of cycles. The
 * rate of change follows a sinusoidal pattern.
 *
 */
public class CycleInterpolator implements Interpolator {

    private final float mCycles;

    public CycleInterpolator(float cycles) {
        mCycles = cycles;
    }

    public float getInterpolation(float input) {
        return (float) (Math.sin(2 * mCycles * Math.PI * input));
    }
}
