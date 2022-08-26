package model;

import java.util.Objects;

/**
 * A point representing a location in {@code (x,y, z)} coordinate space,
 * specified in float precision.
 */
public class Point3DF {
    public final float x;
    public final float y;
    public final float z;

    public Point3DF() {
        this(0, 0, 0);
    }

    public Point3DF(Point3DF p) {
        this(p.x, p.y, p.z);
    }

    public Point3DF(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof Point3DF) {
            final Point3DF pt = (Point3DF) o;
            return x == pt.x && y == pt.y && z == pt.z;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 31 * (31 *   Float.hashCode(x) + Float.hashCode(y)) + Float.hashCode(z);
    }

    public String toString() {
        return getClass().getName() + "[x=" + x + ",y=" + y + ",z=" + z + "]";
    }

}