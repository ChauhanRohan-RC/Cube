package model.cubie;

import model.Axis;
import model.cube.CubeI;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a sticker (or a single face of a cubie)
 * */
public class CubieFace {

    @NotNull
    public static CubieFace internal(@NotNull Axis normal) {
        return new CubieFace(CubeI.FACE_INTERNAL, normal);
    }


    /**
     * Original face (or color) of this sticker (never changes)
     * */
    private final int mFaceCode;

    /**
     * Current normal (or face) of this sticker
     * */
    @NotNull
    private Axis mCurrentNormal;

    public CubieFace(int faceCode, @NotNull Axis normal) {
        mFaceCode = faceCode;
        mCurrentNormal = normal;
    }

    @NotNull
    public CubieFace copy() {
        return new CubieFace(mFaceCode, mCurrentNormal);
    }

    @NotNull
    public CubieFace withNormal(@NotNull Axis normal) {
        return new CubieFace(mFaceCode, normal);
    }

    @NotNull
    public CubieFace withFaceCode(int faceCode) {
        return new CubieFace(faceCode, mCurrentNormal);
    }


    /**
     * @return Original color of sticker (never changes)
     * */
    public final int originalFaceCode() {
        return mFaceCode;
    }

    @NotNull
    public final Axis originalNormal() {
        return CubeI.axis(mFaceCode);
    }

    public final char originalFaceChar() {
        return originalNormal().faceChar;
    }


    /**
     * @return normal (or face) this sticker is currently at
     * */
    @NotNull
    public final Axis currentNormal() {
        return mCurrentNormal;
    }

    /**
     * @return face at which this sticker is currently at
     * */
    public final int currentFaceCode() {
        return CubeI.faceCode(mCurrentNormal);
    }

    public final int currentFaceChar() {
        return mCurrentNormal.faceChar;
    }


    /**
     * @return whether this sticker is currently at it's original face (in solved state)
     * */
    public final boolean isAtOriginalFace() {
        return mFaceCode == currentFaceCode();
    }

    /**
     * @return heuristic to reach original face
     * */
    public final int getMinMovesToOriginalFace() {
        return isAtOriginalFace()? 0: 1;            // 1 since twice clockwise is also a move
    }

    public final int calculateHeuristic() {
        return getMinMovesToOriginalFace();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof CubieFace) {
            final CubieFace cubieFace = (CubieFace) o;
            return mCurrentNormal == cubieFace.mCurrentNormal && mFaceCode == cubieFace.mFaceCode;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 31 * mFaceCode + mCurrentNormal.hashCode();
    }


    protected void onNormalChanged(@NotNull Axis old, @NotNull Axis _new) {

    }

    private void setNormal(@NotNull Axis normal) {
        if (mCurrentNormal == normal) {
            return;
        }

        final Axis old = mCurrentNormal;
        mCurrentNormal = normal;
        onNormalChanged(old, normal);
    }

    public void rotate(@NotNull Axis around, int quarters) {
        setNormal(mCurrentNormal.rotate(around, quarters));
    }

}
