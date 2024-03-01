package gl.animation.interpolator;

import org.jetbrains.annotations.Nullable;

/**
 * An interpolator where the rate of change is constant
 */
public class LinearInterpolator implements Interpolator {

    @Nullable
    private static LinearInterpolator sInstance;

    public static LinearInterpolator getSingleton() {
        LinearInterpolator ins = sInstance;
        if (ins == null) {
            synchronized (LinearInterpolator.class) {
                ins = sInstance;
                if (ins == null) {
                    ins = new LinearInterpolator();
                    sInstance = ins;
                }
            }
        }

        return ins;
    }


    private LinearInterpolator() {
    }

    @Override
    public float getInterpolation(float input) {
        return input;
    }
}
