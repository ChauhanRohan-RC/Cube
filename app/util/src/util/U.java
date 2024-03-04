package util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Random;

public class U {

    public static final float HALF_PI = (float) (Math.PI / 2);
    public static final Random RANDOM = new Random();

    private static final boolean DEBUG = true;
    private static final String LOG_TAG_DEBUG = "DEBUG";
    private static final String LOG_TAG_ERR = "ERR";
    private static final String LOG_TAG_WARN = "WARNING";

    /**
     * Native screen resolution, ex. 1920x1080
     * */
    @NotNull
    public static final Dimension SCREEN_RESOLUTION_NATIVE;

    /**
     * Screen resolution with scaling. Normally, the scaling is 125% (1.25).<br>
     * Hence, for a native 1920x1080 screen, the apparent resolution will be 1920/1.25 , 1080/1.25 = 1536x864<br><br>
     * */
    @NotNull
    public static final Dimension SCREEN_RESOLUTION_SCALED;

    static {
        final DisplayMode displayMode = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();

        SCREEN_RESOLUTION_NATIVE = new Dimension(displayMode.getWidth(), displayMode.getHeight());
        SCREEN_RESOLUTION_SCALED = Toolkit.getDefaultToolkit().getScreenSize();
    }


    @NotNull
    public static Dimension scaleDimension(@NotNull Dimension o, float widthScale, float heightScale) {
        return new Dimension(Math.round(o.width * widthScale), Math.round(o.height * heightScale));
    }

    @NotNull
    public static Dimension scaleDimension(@NotNull Dimension o, float scale) {
        return scaleDimension(o, scale, scale);
    }


    /* Logging ............................ */

    public static void println(Object o) {
        System.out.println(o);
        System.out.flush();
    }

    public static void printerrln(Object o) {
        System.err.println(o);
        System.err.flush();
    }

    @NotNull
    public static String withTag(@Nullable String tag, Object o) {
        return (Format.isEmpty(tag)? "": tag + ": ") + o;
    }

    public static void v(@Nullable String tag, Object o) {
        println(withTag(tag, o));
    }

    public static void v(Object o) {
        println(o);
    }

    public static void d(@Nullable String tag, Object o) {
        if (DEBUG) {
            if (tag == null) {
                tag = LOG_TAG_DEBUG;
            }

            println(withTag(tag, o));
        }
    }

    public static void d(Object o) {
        d(null, o);
    }

    public static void e(@Nullable String tag, Object o) {
        if (tag == null) {
            tag = LOG_TAG_ERR;
        }

        printerrln(withTag(tag, o));
    }

    public static void e(Object o) {
        e(null, o);
    }

    public static void w(@Nullable String tag, Object o) {
        if (tag == null) {
            tag = LOG_TAG_WARN;
        }

        println(withTag(tag, o));
    }

    public static void w(Object o) {
        w(null, o);
    }




    @NotNull
    public static Point2D.Double rotate(double x, double y, double rad) {
        final double cos = Math.cos(rad);
        final double sin = Math.sin(rad);

        return new Point2D.Double((x * cos) - (y * sin), (x * sin) + (y * cos));
    }

    @NotNull
    public static Point2D.Double rotate(@NotNull Point2D point, double rad) {
        return rotate(point.getX(), point.getY(), rad);
    }

    @NotNull
    public static Point2D.Double rotate(double x, double y, double rad, double px, double py) {
        if (px == 0 && py == 0)
            return rotate(x, y, rad);

        final Point2D r = rotate(x - px, y - py, rad);
        return new Point2D.Double(r.getX() + px, r.getY() + py);
    }

    @NotNull
    public static Point2D.Double rotate(@NotNull Point2D point, double rad, @Nullable Point2D pivot) {
        if (pivot == null)
            return rotate(point, rad);
        return rotate(point.getX(), point.getY(), rad, pivot.getX(), pivot.getY());
    }





    public static <T extends Enum<T>> T cycleEnum(@NotNull Class<T> clazz, int curOrdinal) {
        final T[] values = clazz.getEnumConstants();

        if (values.length == 0)
            return null;

        int nextI = curOrdinal + 1;
        if (nextI >= values.length) {
            nextI = 0;
        }

        return values[nextI];
    }

    public static <T extends Enum<T>> T cycleEnum(@NotNull Class<T> clazz, T current) {
        return cycleEnum(clazz, current != null ? current.ordinal() : -1);
    }


    public static int alpha255(int argb) {
        return (argb >>> 24) & 0xff;
    }

    public static int red255(int argb) {
        return (argb >>> 16) & 0xff;
    }

    public static int green255(int argb) {
        return (argb >>> 8) & 0xff;
    }

    public static int blue255(int argb) {
        return argb & 0xff;
    }

    public static float alpha01(int argb) {
        return alpha255(argb) / 255.0f;
    }

    public static int rgb255(int argb) {
        return argb & 0xffffff;
    }

    public static int withAlpha(int argb, int alpha) {
        return ((alpha & 0xff) << 24) | rgb255(argb);
    }

    @NotNull
    public static String hex(int rgb) {
        return String.format("#%02x%02x%02x", red255(rgb), green255(rgb), blue255(rgb));
    }

    public static float luminance255(int r, int g, int b) {
        return (0.2126f * r) + (0.7152f * g) + (0.0722f * b);
    }

    public static float luminance255(int rgb) {
        return luminance255(red255(rgb), green255(rgb), blue255(rgb));
    }


    public static float sq(float n) {
        return n * n;
    }



    public static float lerp(float start, float stop, float amt) {
        return start + (stop - start) * amt;
    }

    public static float norm(float value, float start, float stop) {
        return (value - start) / (stop - start);
    }

    public static float map(float value, float start1, float stop1, float start2, float stop2) {
        float outgoing = start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1));
        String badness = null;
        if (outgoing != outgoing) {
            badness = "NaN (not a number)";
        } else if (outgoing == Float.NEGATIVE_INFINITY || outgoing == Float.POSITIVE_INFINITY) {
            badness = "infinity";
        }

        if (badness != null) {
            String msg = String.format("map(%s, %s, %s, %s, %s) called, which returns %s", Format.nf(value), Format.nf(start1), Format.nf(stop1), Format.nf(start2), Format.nf(stop2), badness);
            System.err.println(msg);
        }

        return outgoing;
    }

    public static int constrain(int amt, int low, int high) {
        return (amt < low)? low: Math.min(amt, high);
    }


    public static float constrain(float amt, float low, float high) {
        return (amt < low)? low: Math.min(amt, high);
    }

    public static float constrain_0_100(float amt) {
        return constrain(amt, 0, 100);
    }

    public static float constrain_0_1(float amt) {
        return constrain(amt, 0, 1);
    }


    @Nullable
    public static Runnable chainRunnables(@Nullable Collection<? extends Runnable> tasks) {
        if (tasks == null || tasks.isEmpty())
            return null;

        Runnable merged = null;
        for (Runnable task: tasks) {
            if (merged == null) {
                merged = task;
            } else {
                final Runnable old = merged;
                merged = () -> { old.run(); task.run(); };
            }
        }

        return merged;
    }


    public static float normalizeDegrees(float degrees) {
        degrees %= 360;

        if (degrees < 0) {
            degrees += 360;
        }

        return degrees;
    }

}
