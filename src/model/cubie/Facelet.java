package model.cubie;

import model.Point3DInt;
import model.cube.CubeI;
import org.jetbrains.annotations.NotNull;


/**
 * Wraps a {@link CubieFace} with information about it's current layers
 * */
public class Facelet {

    @NotNull
    public final CubieFace face;
    @NotNull
    public final Point3DInt layers;

    public Facelet(@NotNull CubieFace face, @NotNull Point3DInt layers) {
        this.face = face;
        this.layers = layers;
    }

    // up
    public final int compareZAscXAsc(@NotNull Facelet o) {
        final int z = Integer.compare(layers.z, o.layers.z);
        if (z != 0)
            return z;

        return Integer.compare(layers.x, o.layers.x);
    }

    // down
    public final int compareZDescXAsc(@NotNull Facelet o) {
        final int z = Integer.compare(o.layers.z, layers.z);
        if (z != 0)
            return z;

        return Integer.compare(layers.x, o.layers.x);
    }

    // Front
    public final int compareYDescXAsc(@NotNull Facelet o) {
        final int y = Integer.compare(o.layers.y, layers.y);
        if (y != 0)
            return y;

        return Integer.compare(layers.x, o.layers.x);
    }

    // Back
    public final int compareYDescXDesc(@NotNull Facelet o) {
        final int y = Integer.compare(o.layers.y, layers.y);
        if (y != 0)
            return y;

        return Integer.compare(o.layers.x, layers.x);
    }

    // Left
    public final int compareYDescZAsc(@NotNull Facelet o) {
        final int y = Integer.compare(o.layers.y, layers.y);
        if (y != 0)
            return y;

        return Integer.compare(layers.z, o.layers.z);
    }

    // Right
    public final int compareYDescZDesc(@NotNull Facelet o) {
        final int y = Integer.compare(o.layers.y, layers.y);
        if (y != 0)
            return y;

        return Integer.compare(o.layers.z, layers.z);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof Facelet) {
            final Facelet other = (Facelet) o;
            return face.equals(other.face) && layers.equals(other.layers);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 31 * face.hashCode() + layers.hashCode();
    }
}
