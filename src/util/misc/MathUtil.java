package util.misc;

import org.jetbrains.annotations.NotNull;

public class MathUtil {

    public static final float PI = 3.141592653589793f;
    public static final float HALF_PI = PI / 2;
    public static final float THREE_HALF_PI = PI + HALF_PI;
    public static final float TWO_PI = PI * 2;
    public static final float MINUS_TWO_PI = -TWO_PI;


    public static final boolean DEFAULT_FAST_ENABLED = true;
    private static volatile boolean sFastEnabled = DEFAULT_FAST_ENABLED;

    protected static void onFastEnabledChanged() {

    }

    /**
     * @return whether preference is changed
     * */
    public static boolean setFastEnabled(boolean fastEnabled) {
        final boolean val = sFastEnabled;
        if (val == fastEnabled)
            return false;

        sFastEnabled = fastEnabled;
        onFastEnabledChanged();
        return true;
    }

    public static boolean isFastEnabled() {
        return sFastEnabled;
    }

    public static void initFast() {
        FastSinLookup.init();
    }



    public static double constraint(double min, double max, double value) {
        return Math.max(min, Math.min(max, value));
    }

    public static float constraint(float min, float max, float value) {
        return Math.max(min, Math.min(max, value));
    }

    public static long constraint(long min, long max, long value) {
        return Math.max(min, Math.min(max, value));
    }

    public static int constraint(int min, int max, int value) {
        return Math.max(min, Math.min(max, value));
    }

    public static double map(double val, double s0, double e0, double s1, double e1) {
        return s1 + (((val - s0) / (e0 - s0)) * (e1  - s1));
    }

    public static boolean isPowOf2(int v) {
        return v != 0 && (v & (v - 1)) == 0;
    }

    public static boolean isPowOf2(long v) {
        return v != 0 && (v & (v - 1)) == 0;
    }

    /**
     * @return a power of 2 <= given value
     * */
    public static int highestPowOf2(int v) {
        return Integer.highestOneBit(v);
    }

    /**
     * @return a power of 2 >= given value
     * */
    public static int lowestPowOf2(int v) {
        if (isPowOf2(v))
            return v;

        return Integer.highestOneBit(v) << 1;
    }

    /**
     * @return a power of 2 <= given value
     * */
    public static long highestPowOf2(long v) {
        return Long.highestOneBit(v);
    }

    /**
     * @return a power of 2 >= given value
     * */
    public static long lowestPowOf2(long v) {
        if (isPowOf2(v))
            return v;

        return Long.highestOneBit(v) << 1;
    }

    public static int signum(final double a) {
        return Double.compare(a, 0.0);
    }

    public static double norm(double start, double end, double value) {
        return ((value - start) / (end - start));
    }

    public static float normF(double start, double end, double value) {
        return (float) norm(start, end, value);
    }

    public static double norm(float start, float end, double value) {
        return (value - start) / (end - start);
    }

    public static float normF(float start, float end, double value) {
        return (float) norm(start, end, value);
    }

    public static double lerp(double start, double end, float t) {
        return start + ((end - start) * t);
    }

    public static float lerp(float start, float end, float t) {
        return start + ((end - start) * t);
    }

    public static double lerp(long start, long end, float t) {
        return (start + ((end - start) * t));
    }

    public static double @NotNull[] negate(double @NotNull[] data) {
        for (int i=0; i < data.length; i++) {
            data[i] = -data[i];
        }

        return data;
    }

    public static double @NotNull[] negateCopy(double @NotNull[] data) {
        final double[] newData = new double[data.length];

        for (int i=0; i < data.length; i++) {
            newData[i] = -data[i];
        }

        return newData;
    }

    public static double @NotNull[] scale(double @NotNull[] data, double scale) {
        for (int i=0; i < data.length; i++) {
            data[i] = scale * data[i];
        }

        return data;
    }




    /* ....................................... Trigonometry  ................................. */

    public static float sinexact(double rad) {
        return (float) Math.sin(rad);
    }

    public static float cosexact(double rad) {
        return (float) Math.cos(rad);
    }

    public static float sinfast(float rad) {
        return sFastEnabled? FastSinLookup.sin(rad): sinexact(rad);
    }

    public static float sinfast(double rad) {
        return sFastEnabled? FastSinLookup.sin(rad): sinexact(rad);
    }

    public static float cosfast(float rad) {
        return sFastEnabled? FastSinLookup.cos(rad): cosexact(rad);
    }

    public static float cosfast(double rad) {
        return sFastEnabled? FastSinLookup.cos(rad): cosexact(rad);
    }

    public static void sincosexact(float rad, float @NotNull[] dest) {
        dest[0] = sinexact(rad);
        dest[1] = cosexact(rad);
    }

    public static void sincosfast(float rad, float @NotNull[] dest) {
        if (sFastEnabled) {
            FastSinLookup.sincos(rad, dest);
        } else {
            sincosexact(rad, dest);
        }
    }



    /**
     * A lookup table for extremely fast sin and cos
     * */
    public static final class FastSinLookup {

        private static final int SIN_BITS;
        private static final int SIN_MASK;
        private static final int SIN_COUNT;
        private static final float radToIndex;

        private static final float[] sin;
        private static final float[] cos;

        static {
            SIN_BITS = 12;
            SIN_MASK = ~(-1 << SIN_BITS);
            SIN_COUNT = SIN_MASK + 1;

            radToIndex = SIN_COUNT / TWO_PI;
            sin = new float[SIN_COUNT];
            cos = new float[SIN_COUNT];

            for (int i = 0; i < SIN_COUNT; i++) {
                sin[i] = (float) Math.sin((i + 0.5f) / SIN_COUNT * TWO_PI);
                cos[i] = (float) Math.cos((i + 0.5f) / SIN_COUNT * TWO_PI);
            }

            // Four cardinal directions (credits: Nate)
            sin[0] = 0;
            sin[radIndexInternal(HALF_PI)] = 1;
            sin[radIndexInternal(PI)] = 0;
            sin[radIndexInternal(THREE_HALF_PI)] = -1;
            sin[radIndexInternal(TWO_PI)] = 0;

            cos[0] = 1;
            cos[radIndexInternal(HALF_PI)] = 0;
            cos[radIndexInternal(PI)] = -1;
            cos[radIndexInternal(THREE_HALF_PI)] = 0;
            cos[radIndexInternal(TWO_PI)] = 1;
        }


        private static void init() {
            // just call to load the class
        }

        private static int radIndexInternal(float rad) {
            return (int) (rad * radToIndex) & SIN_MASK;
        }

        public static float sin(float rad) {
            return sin[(int) (rad * radToIndex) & SIN_MASK];
        }

        public static float cos(float rad) {
            return cos[(int) (rad * radToIndex) & SIN_MASK];
        }

        public static float sin(double rad) {
            return sin[(int) (rad * radToIndex) & SIN_MASK];
        }

        public static float cos(double rad) {
            return cos[(int) (rad * radToIndex) & SIN_MASK];
        }

        public static void sincos(float rad, float @NotNull [] dest) {
            final int i = (int) (rad * radToIndex) & SIN_MASK;
            dest[0] = sin[i];
            dest[1] = cos[i];
        }
    }
}
