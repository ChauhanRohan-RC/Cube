package model.cubie;

import math.Point3DInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.function.Consumer;

public class GeneralCubie extends Cubie {

    @Nullable
    @Unmodifiable
    private final CubieFace[] faces;

    protected GeneralCubie(int n, @NotNull Point3DInt center, @Nullable CubieFace... faces) {
        super(n, center);

        if (faces != null && faces.length > 6)
            throw new IllegalArgumentException("A Cubie can have and at most 6 faces, given " + faces.length + " faces");
        this.faces = faces;
    }

    @Override
    public @NotNull Cubie copy() {
        CubieFace[] copy = null;
        if (faces != null) {
            copy = new CubieFace[faces.length];
            for (int i=0; i < faces.length; i++) {
                copy[i] = faces[i].copy();
            }
        }

        return new GeneralCubie(n, center(), copy);
    }

    @Override
    public final int noOfFaces() {
        return faces != null? faces.length: 0;
    }

    @Override
    @NotNull
    public final CubieFace getFace(int index) {
//        if (faces == null || index < 0 || index >= faces.length)
//            throw new IndexOutOfBoundsException("index: " + index + ", noOfFaces: " + noOfFaces());

        return faces[index];
    }

    @Override
    public final void forEachFace(@NotNull Consumer<CubieFace> action) {
        if (noOfFaces() == 0)
            return;

        for (CubieFace face: faces) {
            action.accept(face);
        }
    }

    @Override
    public int facesHash() {
        return Arrays.hashCode(faces);
    }
}
