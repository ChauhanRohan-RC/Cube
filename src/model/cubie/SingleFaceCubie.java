package model.cubie;

import model.Point3DInt;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class SingleFaceCubie extends Cubie {

    @NotNull
    private final CubieFace mFace;

    public SingleFaceCubie(int n, @NotNull Point3DInt center, @NotNull CubieFace face) {
        super(n, center);
        mFace = face;
    }

    @Override
    @NotNull
    public Cubie copy() {
        return new SingleFaceCubie(n, center(), mFace.copy());
    }

    @Override
    public int noOfFaces() {
        return 1;
    }

    @Override
    public @NotNull CubieFace getFace(int index) {
        if (index == 0)
            return mFace;
        throw new IndexOutOfBoundsException("SingleFace Cubie only has 1 face, given index : " + index);
    }

    @Override
    public void forEachFace(@NotNull Consumer<CubieFace> action) {
        action.accept(mFace);
    }

    @Override
    public int calculateHeuristic() {
        return mFace.calculateHeuristic();
    }

    @Override
    public boolean allFacesAtOriginalFaces() {
        return mFace.isAtOriginalFace();
    }

    @Override
    public boolean equals(@NotNull Cubie cubie) {
        if (!(n == cubie.n && cubie instanceof SingleFaceCubie && center().equals(cubie.center())))
            return false;

        final SingleFaceCubie o = (SingleFaceCubie) cubie;
        return mFace.equals(o.mFace);
    }

    @Override
    public int facesHash() {
        return mFace.hashCode();
    }
}

