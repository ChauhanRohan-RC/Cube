package model.cubie;

import model.Axis;
import model.Point3D;
import model.Point3DInt;
import model.cube.CubeI;
import util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public abstract class Cubie {

    public interface Listener {
        void onCubieRotated(@NotNull Axis around, int quarters);
    }


    public final int n;     // dimension of cube (3*3 or 4*4 or any)

    // Coordinates
    private Point3DInt mCenter;

    @Nullable
    private Listener mListener;

    public Cubie(int n, @NotNull Point3DInt center) {
        this.n = n;
        this.mCenter = center;
    }

    public Cubie(int n, int centerX, int centerY, int centerZ) {
        this(n, new Point3DInt(centerX, centerY, centerZ));
    }

    @NotNull
    public Point3DInt center() {
        return mCenter;
    }

    @NotNull
    public final Point3DInt layers() {
        return new Point3DInt(CubeI.cubieCenterToLayer(n, mCenter.x), CubeI.cubieCenterToLayer(n, mCenter.y), CubeI.cubieCenterToLayer(n, mCenter.z));
    }

    @NotNull
    public abstract Cubie copy();

    public abstract int noOfFaces();     // [1, 6)

    @NotNull
    public abstract CubieFace getFace(int index);

    public void forEachFace(@NotNull Consumer<CubieFace> action) {
        for (int i=0; i < noOfFaces(); i++) {
            action.accept(getFace(i));
        }
    }

    public int calculateHeuristic() {
        int h = 0;
        for (int i=0; i < noOfFaces(); i++) {
            h += getFace(i).calculateHeuristic();
        }

        return h;
    }

    public boolean allFacesAtOriginalFaces() {           // when all faces are aligned
        for (int i=0; i < noOfFaces(); i++) {
            if (!getFace(i).isAtOriginalFace())
                return false;
        }

        return true;
    }


    @Nullable
    public Listener getListener() {
        return mListener;
    }

    public void setListener(@Nullable Listener listener) {
        mListener = listener;
    }

    protected void onRotated(@NotNull Axis around, int quarters, @NotNull Point3DInt prevCenter, @NotNull Point3DInt newCenter) {
        final Listener l = mListener;
        if (l != null) {
            l.onCubieRotated(around, quarters);
        }
    }

    public final void rotate(@NotNull Axis around, int quarters) {
        // rotate coordinates
        final Point3D coords = around.rotate(mCenter.x, mCenter.y, mCenter.z, quarters * Util.HALF_PI);

        final Point3DInt prev = mCenter;
        mCenter = new Point3DInt(Math.round((float) coords.x), Math.round((float) coords.y), Math.round((float) coords.z));

        // Rotate Faces
        forEachFace(face -> face.rotate(around, quarters));
        onRotated(around, quarters, prev, mCenter);
    }

    public final void applyMove(@NotNull Move move) {
        rotate(move.axis, move.normalizedQuarters);
    }


    public boolean equals(@NotNull Cubie cubie) {
        if (!(n == cubie.n && noOfFaces() == cubie.noOfFaces() && mCenter.equals(cubie.mCenter)))
            return false;

        for (int i=0; i < noOfFaces(); i++) {
            if (!getFace(i).equals(cubie.getFace(i)))
                return false;
        }

        return true;
    }

    @Override
    public final boolean equals(Object o) {
        return this == o || (o instanceof Cubie && equals((Cubie) o));
    }

    public int facesHash() {
        if (noOfFaces() == 0)
            return 0;

        int h = 1;
        for (int i=0; i < noOfFaces(); i++) {
            h = 31 * h + getFace(i).hashCode();
        }

        return h;
    }

    @Override
    public final int hashCode() {
        int hash = 31 * n + noOfFaces();
        hash = 31 * hash + mCenter.hashCode();
        hash = 31 * hash + facesHash();
        return hash;
    }
}
