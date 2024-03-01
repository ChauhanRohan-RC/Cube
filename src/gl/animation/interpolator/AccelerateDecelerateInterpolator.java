package gl.animation.interpolator;

import org.jetbrains.annotations.Nullable;

/**
 * An interpolator where the rate of change starts and ends slowly but
 * accelerates through the middle.
 */
public class AccelerateDecelerateInterpolator implements Interpolator {

    @Nullable
    private static AccelerateDecelerateInterpolator sInstance;

    public static AccelerateDecelerateInterpolator getSingleton() {
        AccelerateDecelerateInterpolator ins = sInstance;
        if (ins == null) {
            synchronized (AccelerateDecelerateInterpolator.class) {
                ins = sInstance;
                if (ins == null) {
                    ins = new AccelerateDecelerateInterpolator();
                    sInstance = ins;
                }
            }
        }

        return ins;
    }


    private AccelerateDecelerateInterpolator() {
    }

    @Override
    public float getInterpolation(float input) {
        return (float)(Math.cos((input + 1) * Math.PI) / 2.0f) + 0.5f;
    }
}
