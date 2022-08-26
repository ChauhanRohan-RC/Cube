package gl.animation;

import org.jetbrains.annotations.NotNull;

public interface Interpolator {

    /**
     * Maps a value representing the elapsed fraction of an animation to a value that represents
     * the interpolated fraction. This interpolated value is then multiplied by the change in
     * value of an animation to derive the animated value at the current elapsed animation time.
     *
     * @param input A value between 0 and 1.0 indicating our current point
     *        in the animation where 0 represents the start and 1.0 represents
     *        the end
     * @return The interpolation value. This value can be more than 1.0 for
     *         interpolators which overshoot their targets, or less than 0 for
     *         interpolators that undershoot their targets.
     */
    float getInterpolation(float input);



    /* Static */

    /**
     * An interpolator where the rate of change is constant
     */
    static float linear(float input) {
        return input;
    }

    @NotNull
    Interpolator LINEAR = Interpolator::linear;



    /**
     * An interpolator where the rate of change starts and ends slowly but
     * accelerates through the middle.
     */
    static float accelerateDecelerate(float input) {
        return (float)(Math.cos((input + 1) * Math.PI) / 2.0f) + 0.5f;
    }

    @NotNull
    Interpolator ACCELERATE_DECELERATE = Interpolator::accelerateDecelerate;



    /**
     * An interpolator where the change bounces at the end.
     */

    static float __bounce(float t) {
        return t * t * 8.0f;
    }

    static float bounce(float t) {
        // _b(t) = t * t * 8
        // bs(t) = _b(t) for t < 0.3535
        // bs(t) = _b(t - 0.54719) + 0.7 for t < 0.7408
        // bs(t) = _b(t - 0.8526) + 0.9 for t < 0.9644
        // bs(t) = _b(t - 1.0435) + 0.95 for t <= 1.0
        // b(t) = bs(t * 1.1226)
        t *= 1.1226f;
        if (t < 0.3535f) return __bounce(t);
        else if (t < 0.7408f) return __bounce(t - 0.54719f) + 0.7f;
        else if (t < 0.9644f) return __bounce(t - 0.8526f) + 0.9f;
        else return __bounce(t - 1.0435f) + 0.95f;
    }

    @NotNull
    Interpolator BOUNCE = Interpolator::bounce;

}
