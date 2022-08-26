package model.cubie;

import model.Point3DInt;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class CornerCubie extends Cubie {

    @NotNull
    private final CubieFace mFaceOne;
    @NotNull
    private final CubieFace mFaceTwo;
    @NotNull
    private final CubieFace mFaceThree;

    public CornerCubie(int n, @NotNull Point3DInt center, @NotNull CubieFace faceOne, @NotNull CubieFace faceTwo, @NotNull CubieFace faceThree) {
        super(n, center);
        mFaceOne = faceOne;
        mFaceTwo = faceTwo;
        mFaceThree = faceThree;
    }

    @Override
    @NotNull
    public Cubie copy() {
        return new CornerCubie(n, center(), mFaceOne.copy(), mFaceTwo.copy(), mFaceThree.copy());
    }

    @Override
    public int noOfFaces() {
        return 3;
    }

    @Override
    public @NotNull CubieFace getFace(int index) {
        return switch (index) {
            case 0 -> mFaceOne;
            case 1 -> mFaceTwo;
            case 2 -> mFaceThree;
            default -> throw new IndexOutOfBoundsException("Corner Cubie only has 3 faces, given index : " + index);
        };
    }

    @Override
    public void forEachFace(@NotNull Consumer<CubieFace> action) {
        action.accept(mFaceOne);
        action.accept(mFaceTwo);
        action.accept(mFaceThree);
    }

    @Override
    public int calculateHeuristic() {
        return mFaceOne.calculateHeuristic() + mFaceTwo.calculateHeuristic() + mFaceThree.calculateHeuristic();
    }

    @Override
    public boolean allFacesAtOriginalFaces() {
        return mFaceOne.isAtOriginalFace() && mFaceTwo.isAtOriginalFace() && mFaceThree.isAtOriginalFace();
    }


    @Override
    public boolean equals(@NotNull Cubie cubie) {
        if (!(n == cubie.n && cubie instanceof CornerCubie && center().equals(cubie.center())))
            return false;

        final CornerCubie o = (CornerCubie) cubie;
        return mFaceOne.equals(o.mFaceOne) && mFaceTwo.equals(o.mFaceTwo) && mFaceThree.equals(o.mFaceThree);
    }

    @Override
    public int facesHash() {
        int h = 31 * mFaceOne.hashCode() + mFaceTwo.hashCode();
        return 31 * h + mFaceThree.hashCode();
    }
}
