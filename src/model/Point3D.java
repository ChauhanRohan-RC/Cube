package model;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Point3D {

    public final double x;
    public final double y;
    public final double z;

    public Point3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof Point3D) {
            final Point3D other = (Point3D) o;
            return other.x == x && other.y == y && other.z == z;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * Double.hashCode(x) + Double.hashCode(y)) + Double.hashCode(z);
    }

    @Override
    public String toString() {
        return "Point3D(" + x + ", " + y + ", " + z + ')';
    }

    @NotNull
    public Point3D add(@NotNull Point3D o) {
        return new Point3D(x + o.x, y + o.y, z + o.z);
    }

    @NotNull
    public Point3D sub(@NotNull Point3D o) {
        return new Point3D(x - o.x, y - o.y, z - o.z);
    }

    @NotNull
    public Point3D scale(double scale) {
        return new Point3D(x * scale, y * scale, z * scale);
    }
}
