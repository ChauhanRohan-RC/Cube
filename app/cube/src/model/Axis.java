package model;

import math.Point3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import util.U;

import java.awt.geom.Point2D;


/**
 * Orthogonal Axis<br>
 *
 * Enum ordinals are such that += 3 gives opposite axis
 * */
public enum Axis {

    Y(0, 1, 0, 'U'),
    X(1, 0, 0, 'R'),
    Z(0, 0, 1, 'F'),
    Y_N(0, -1, 0, 'D'),
    X_N(-1, 0, 0, 'L'),
    Z_N(0, 0, -1, 'B')

    ;


    /* Unit Vector */
    public final int unitX;
    public final int unitY;
    public final int unitZ;

    public final int absUnitX;
    public final int absUnitY;
    public final int absUnitZ;

    /**
     * Face char
     *
     * U -> Up
     * R -> Right
     * F -> front
     * D -> Down
     * L -> Left
     * B -> Back
     * */
    public final char faceChar;

    @Nullable
    private Axis mInverted;

    Axis(int unitX, int unitY, int unitZ, char faceChar) {
        this.unitX = unitX;
        this.unitY = unitY;
        this.unitZ = unitZ;
        this.faceChar = faceChar;

        absUnitX = Math.abs(unitX);
        absUnitY = Math.abs(unitY);
        absUnitZ = Math.abs(unitZ);
    }

    public boolean isX() {
        return absUnitX == 1;
    }

    public boolean isY() {
        return absUnitY == 1;
    }

    public boolean isZ() {
        return absUnitZ == 1;
    }

    public boolean isNegative() {
        return unitX == -1 || unitY == -1 || unitZ == -1;
    }

    public boolean isPositive() {
        return !isNegative();
    }

    public boolean orthogonal(@NotNull Axis o) {
        return absUnitX != o.absUnitX || absUnitY != o.absUnitY || absUnitZ != o.absUnitZ;
    }

    public boolean parallel(@NotNull Axis o) {
        return absUnitX == o.absUnitX && absUnitY == o.absUnitY && absUnitZ == o.absUnitZ;
    }


    @NotNull
    public Axis invert() {
        if (mInverted == null) {
            mInverted = invert(this);
        }

        return mInverted;
    }


    /**
     * Rotates this axis around a given axis
     *
     * @param around axis to rotate around
     * @param quarters multiples of 90 deg rotations (+ve -> anticlockwise, -ve -> clockwise)
     *
     * @return this axis rotated around given axis
     * */
    @NotNull
    public Axis rotate(@NotNull Axis around, int quarters) {
        if (parallel(around))
            return this;

        if (Math.abs(quarters) >= 4)
            quarters %= 4;

        if (quarters == 0)
            return this;

        if (Math.abs(quarters) == 2)
            return invert();

        final Point3D rotated = rotate(unitX, unitY, unitZ, around, quarters * U.HALF_PI);
        final int x = Math.round((float) rotated.x);
        if (x == 1)
            return X;
        if (x == -1)
            return X_N;

        final int y = Math.round((float) rotated.y);
        if (y == 1)
            return Y;
        if (y == -1)
            return Y_N;

        final int z = Math.round((float) rotated.z);
        if (z == 1)
            return Z;
        if (z == -1)
            return Z_N;
        throw new AssertionError("Should not happen!!");
    }


    /**
     * @see #rotate(double, double, double, Axis, float)
     * */
    @NotNull
    public Point3D rotate(double x, double y, double z, float rad) {
        return rotate(x, y, z, this, rad);
    }

    /**
     * @see #rotate(Point3D, Axis, float)
     * */
    @NotNull
    public Point3D rotate(@NotNull Point3D point, float rad) {
        return rotate(point, this, rad);
    }



    @NotNull
    private static Axis invert(@NotNull Axis axis) {
        return switch (axis) {
            case X -> X_N;
            case Y -> Y_N;
            case Z -> Z_N;
            case X_N -> X;
            case Y_N -> Y;
            case Z_N -> Z;
        };
    }



    /**
     * Transforms a rotational angle according to an axis
     *
     * <pre>
     *     <b>DO NOT</b> pass it to any of rotate methods. pass angle directly
     * </pre>
     *
     * @param around axis around which to rotate
     * @param rad angle of rotation (in radians, +ve -> anticlockwise, -ve -> clockwise)
     * */
    public static float queryRotation(@NotNull Axis around, float rad) {
        return around.isNegative()? -rad: rad;
    }

    /**
     * @see #queryRotation(Axis, float)
     * */
    public static float queryRotation(@NotNull Axis around, int quarters) {
        return queryRotation(around, quarters * U.HALF_PI);
    }

    /**
     * Rotates a point around an axis
     *
     * @param x point x coordinate
     * @param y point y coordinate
     * @param z point z coordinate
     * @param around axis around which to rotate
     * @param rad angle in radians (+ve -> anticlockwise, -ve -> clockwise)
     *
     * @return given point rotated around the axis by rad angle
     *
     * @see #queryRotation(Axis, float)
     * */
    @NotNull
    public static Point3D rotate(double x, double y, double z, @NotNull Axis around, float rad) {
        rad = queryRotation(around, rad);

        switch (around) {
            case X, X_N -> {        // YZ rotation
                final Point2D r = U.rotate(y, z, rad);
                return new Point3D(x, r.getX(), r.getY());
            } case Y, Y_N -> {        // ZX rotation
                final Point2D r = U.rotate(z, x, rad);
                return new Point3D(r.getY(), y, r.getX());
            } default -> {        // XY rotation
                final Point2D r = U.rotate(x, y, rad);
                return new Point3D(r.getX(), r.getY(), z);
            }
        }
    }

    /**
     * @see #rotate(double, double, double, Axis, float)
     * */
    @NotNull
    public static Point3D rotate(@NotNull Point3D point, @NotNull Axis around, float rad) {
        return rotate(point.x, point.y, point.z, around, rad);
    }




    private final static Axis[] sAllValues = values();
    private final static Axis[] sPositives = { X, Y, Z };
    private final static Axis[] sNegatives = { X_N, Y_N, Z_N };

    @NotNull
    @Unmodifiable
    public static Axis[] sharedValues() {
//        if (sAllValues == null) {
//            sAllValues = values();
//        }

        return sAllValues;
    }

    public static int size() {
        return sharedValues().length;
    }

    @NotNull
    @Unmodifiable
    public static Axis[] sharedPositives() {
//        if (sPositives == null) {
//            sPositives = new Axis[] { X, Y, Z };
//        }

        return sPositives;
    }

    public static int positivesSize() {
        return sPositives.length;
    }


    @NotNull
    @Unmodifiable
    public static Axis[] sharedNegatives() {
//        if (sNegatives == null) {
//            sNegatives = new Axis[] { X_N, Y_N, Z_N };
//        }

        return sNegatives;
    }

    public static int negativesSize() {
        return sNegatives.length;
    }
}
