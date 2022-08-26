package model;


/**
 * A point representing a location in {@code (x,y, z)} coordinate space,
 * specified in int precision.
 */
public class Point3DInt {
    public final int x;
    public final int y;
    public final int z;

    public Point3DInt() {
        this(0, 0, 0);
    }

    public Point3DInt(Point3DInt p) {
        this(p.x, p.y, p.z);
    }

    public Point3DInt(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof Point3DInt) {
            final Point3DInt pt = (Point3DInt) o;
            return x == pt.x && y == pt.y && z == pt.z;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * x + y) + z;
    }

    public String toString() {
        return getClass().getName() + "[x=" + x + ",y=" + y + ",z=" + z + "]";
    }

}
