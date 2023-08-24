package util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import processing.core.PApplet;

import java.awt.geom.Point2D;
import java.util.Random;

public class Util {

    public static final float HALF_PI = PApplet.HALF_PI;
    public static final Random RANDOM = new Random();

    private static final boolean DEBUG = true;
    private static final String LOG_TAG_DEBUG = "DEBUG";
    private static final String LOG_TAG_ERR = "ERR";
    private static final String LOG_TAG_WARN = "WARNING";

//    public static final char ARROW_LEFT = '\u2190';
//    public static final char ARROW_UP = '\u2191';
//    public static final char ARROW_RIGHT = '\u2192';
//    public static final char ARROW_DOWN = '\u2193';


    private Util() {
    }

    public static boolean isEmpty(@Nullable CharSequence sequence) {
        return sequence == null || sequence.length() == 0;
    }

    public static boolean notEmpty(@Nullable CharSequence sequence) {
        return !isEmpty(sequence);
    }



    public static void println(Object o) {
        System.out.println(o);
        System.out.flush();
    }

    @NotNull
    public static String withTag(@Nullable String tag, Object o) {
        return (isEmpty(tag)? "": tag + ": ") + o;
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

        System.err.println(withTag(tag, o));
        System.err.flush();
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


}
