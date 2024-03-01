package gl.animation.interpolator;

import org.jetbrains.annotations.Nullable;

/**
 * An interpolator where the change bounces at the end.
 */
public class BounceInterpolator implements Interpolator {

    @Nullable
    private static BounceInterpolator sInstance;

    public static BounceInterpolator getSingleton() {
        BounceInterpolator ins = sInstance;
        if (ins == null) {
            synchronized (BounceInterpolator.class) {
                ins = sInstance;
                if (ins == null) {
                    ins = new BounceInterpolator();
                    sInstance = ins;
                }
            }
        }

        return ins;
    }


    private BounceInterpolator() {
    }


    private static float _bounce(float t) {
        return t * t * 8.0f;
    }

    @Override
    public float getInterpolation(float input) {
        // _b(t) = t * t * 8
        // bs(t) = _b(t) for t < 0.3535
        // bs(t) = _b(t - 0.54719) + 0.7 for t < 0.7408
        // bs(t) = _b(t - 0.8526) + 0.9 for t < 0.9644
        // bs(t) = _b(t - 1.0435) + 0.95 for t <= 1.0
        // b(t) = bs(t * 1.1226)

        input *= 1.1226f;
        if (input < 0.3535f)
            return _bounce(input);

        if (input < 0.7408f)
            return _bounce(input - 0.54719f) + 0.7f;

        if (input < 0.9644f)
            return _bounce(input - 0.8526f) + 0.9f;

        return _bounce(input - 1.0435f) + 0.95f;
    }
}
