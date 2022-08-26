package model.cubie;

import model.Point3DInt;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class EdgeCubie extends Cubie {

    @NotNull
    private final CubieFace mFaceOne;
    @NotNull
    private final CubieFace mFaceTwo;

    public EdgeCubie(int n, @NotNull Point3DInt center, @NotNull CubieFace faceOne, @NotNull CubieFace faceTwo) {
        super(n, center);
        mFaceOne = faceOne;
        mFaceTwo = faceTwo;
    }

    @Override
    @NotNull
    public Cubie copy() {
        return new EdgeCubie(n, center(), mFaceOne.copy(), mFaceTwo.copy());
    }

    @Override
    public int noOfFaces() {
        return 2;
    }

    @Override
    public @NotNull CubieFace getFace(int index) {
        return switch (index) {
            case 0 -> mFaceOne;
            case 1 -> mFaceTwo;
            default -> throw new IndexOutOfBoundsException("Edge Cubie only has 2 faces, given index : " + index);
        };
    }

    @Override
    public void forEachFace(@NotNull Consumer<CubieFace> action) {
        action.accept(mFaceOne);
        action.accept(mFaceTwo);
    }

    @Override
    public int calculateHeuristic() {
        return mFaceOne.calculateHeuristic() + mFaceTwo.calculateHeuristic();
    }

    @Override
    public boolean allFacesAtOriginalFaces() {
        return mFaceOne.isAtOriginalFace() && mFaceTwo.isAtOriginalFace();
    }

    @Override
    public boolean equals(@NotNull Cubie cubie) {
        if (!(n == cubie.n && cubie instanceof EdgeCubie && center().equals(cubie.center())))
            return false;

        final EdgeCubie o = (EdgeCubie) cubie;
        return mFaceOne.equals(o.mFaceOne) && mFaceTwo.equals(o.mFaceTwo);
    }

    @Override
    public int facesHash() {
        return 31 * mFaceOne.hashCode() + mFaceTwo.hashCode();
    }
}

