package model.cube;

import model.cubie.Cubie;
import model.cubie.Move;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.Listeners;

import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Basic implementation of a cube
 * */
public final class Cube implements CubeI {

    public interface Listener {
        void onMoveApplied(@NotNull Move move, int cubiesAffected, boolean saved);
    }

    public final int n;       // dimension
    @NotNull
    private final Cubie[] cubies;
//    private int mCachedHeuristic = -1;       // -ve for not set (0 for solved)

    @Nullable
    private LinkedList<Move> mMoves;
    @Nullable
    private int[] mAllLayersShared;

    @NotNull
    private final Listeners<Listener> mListeners = new Listeners<>();

    public Cube(int n) {
        this.n = n;
        cubies = CubeI.createSolvedState(n);
//        mCachedHeuristic = 0;       // start as solved
    }

    private Cube(int n, @NotNull Cubie[] cubies) {
        if (cubies.length != CubeI.totalCubies(n))
            throw new IllegalArgumentException("INVALID STATE: For a " + n + "*" + n + " cube, cubies array length must be equal to " + CubeI.totalCubies(n) +", given: " + cubies.length);

        this.n = n;
        this.cubies = cubies;
    }

    public Cube(@NotNull CubeI cube) {
        this.n = cube.n();
        this.cubies = cube.copyState();
//        mCachedHeuristic = cube.cacheHeuristic();
    }

    @Override
    public int n() {
        return n;
    }

    @Override
    public final int noOfCubies() {
        return cubies.length;
    }

    @NotNull
    @Override
    public final Cubie getCubie(int index) {
        return cubies[index];
    }

    public final void forEachCubie(@NotNull Consumer<Cubie> action) {
        for (Cubie qb: cubies) {
            action.accept(qb);
        }
    }

    @NotNull
    @Override
    public int[] allLayersShared() {
        if (mAllLayersShared == null) {
            mAllLayersShared = CubeI.createAllLayers(n);
        }

        return mAllLayersShared;
    }


    private void addMove(@NotNull Move move) {
        if (mMoves == null) {
            mMoves = new LinkedList<>();
        }

        mMoves.addLast(move);
    }

    @Nullable
    public Move peekLastMove() {
        return mMoves != null? mMoves.peekLast(): null;
    }

    @Nullable
    public Move pollLastMove() {
        return mMoves != null? mMoves.pollLast(): null;
    }

    @Nullable
    public Move peekFirstMove() {
        return mMoves != null? mMoves.peekFirst(): null;
    }

    @Nullable
    public Move pollFirstMove() {
        return mMoves != null? mMoves.pollFirst(): null;
    }

    public void forEachMove(@NotNull Consumer<Move> action) {
        if (mMoves == null || mMoves.isEmpty())
            return;

        for (Move m: mMoves) {
            action.accept(m);
        }
    }


    public void addListener(@NotNull Listener listener) {
        mListeners.addListener(listener);
    }

    public boolean removeListener(@NotNull Listener listener) {
        return mListeners.removeListener(listener);
    }

    protected void onMoveApplied(@NotNull Move move, int cubiesAffected, boolean saved) {
//        if (cubiesAffected > 0) {
//            System.out.println("Move applied: " + move);
//        }

        mListeners.forEachListener(l -> l.onMoveApplied(move, cubiesAffected, saved));
    }

    public final void applyMove(@NotNull Move move, boolean saveInStack) {
        final Predicate<Cubie> filter = move.cubieFilter(n);

        int qbCounter = 0;
        for (Cubie qb: cubies) {
            if (filter.test(qb)) {
//                final int h = cacheHeuristic();
//                final int ch = qb.calculateHeuristic();
                qb.applyMove(move);
//                mCachedHeuristic = h - ch + qb.calculateHeuristic();
                qbCounter++;
            }
        }

        final boolean save = saveInStack && qbCounter > 0;
        if (save) {
            addMove(move);
        }

        onMoveApplied(move, qbCounter, save);
    }

    public final void applyMove(@NotNull Move move) {
        applyMove(move, true);
    }

    public final boolean undoLastMove() {
        final Move last = pollLastMove();
        if (last == null)
            return false;
        applyMove(last.reverse(), false);
        return true;
    }

    public void rotateX(int quarters, boolean saveInStack) {
        applyMove(rotateXMove(quarters), saveInStack);
    }

    public void rotateY(int quarters, boolean saveInStack) {
        applyMove(rotateYMove(quarters), saveInStack);
    }

    public void rotateZ(int quarters, boolean saveInStack) {
        applyMove(rotateZMove(quarters), saveInStack);
    }



//    @Override
//    public final int cacheHeuristic() {
//        if (mCachedHeuristic < 0) {
//            mCachedHeuristic = calculateHeuristic();
//        }
//
//        return mCachedHeuristic;
//    }
//
//    @Override
//    public final boolean cacheIsSolved() {
//        return cacheHeuristic() == 0;
//    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof CubeI && equals((CubeI) o));
    }

    @Override
    public int hashCode() {
        return hashImpl();
    }
}

