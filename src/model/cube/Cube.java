package model.cube;

import model.cubie.Cubie;
import model.cubie.Move;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.live.Listeners;
import util.misc.CollectionUtil;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Basic implementation of a cube
 */
public final class Cube implements CubeI {

    public interface Listener {
        void onMoveApplied(@NotNull Cube cube, @NotNull Move move, int cubiesAffected, boolean saved);

        void onCubeLockChanged(@NotNull Cube cube, boolean locked);
    }

    public final int n;       // dimension
    @NotNull
    private final Cubie[] cubies;
    private volatile boolean mLocked;

    //    private int mCachedHeuristic = -1;       // -ve for not set (0 for solved)
    @NotNull
    private final LinkedList<Move> mInternalMoves = new LinkedList<>();      // Parsed internal moves
    @Nullable
    private LinkedList<Move> mMovesStack;       // Applied moves that are explicitly saved
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
            throw new IllegalArgumentException("INVALID STATE: For a " + n + "*" + n + " cube, cubies array length must be equal to " + CubeI.totalCubies(n) + ", given: " + cubies.length);

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
        for (Cubie qb : cubies) {
            action.accept(qb);
        }
    }

    @Override
    public boolean isLocked() {
        return mLocked;
    }

    private void onLockChanged(boolean locked) {
        mListeners.forEachListener(l -> l.onCubeLockChanged(this, locked));
    }

    @Override
    public void setLocked(boolean locked) {
        final boolean l = mLocked;
        if (l == locked)
            return;

        mLocked = locked;
        onLockChanged(locked);
    }


    @NotNull
    @Override
    public int[] allLayersShared() {
        if (mAllLayersShared == null) {
            mAllLayersShared = CubeI.createAllLayers(n);
        }

        return mAllLayersShared;
    }

    @Nullable
    public Move peekLastMove() {
        return mMovesStack != null ? mMovesStack.peekLast() : null;
    }

    @Nullable
    public Move pollLastMove() {
        return mMovesStack != null ? mMovesStack.pollLast() : null;
    }

    @Nullable
    public Move peekFirstMove() {
        return mMovesStack != null ? mMovesStack.peekFirst() : null;
    }

    @Nullable
    public Move pollFirstMove() {
        return mMovesStack != null ? mMovesStack.pollFirst() : null;
    }

    public void forEachMove(@NotNull Consumer<Move> action) {
        final List<Move> stack = mMovesStack;
        if (stack == null || stack.isEmpty())
            return;

        for (Move m : CollectionUtil.linkedListCopy(stack)) {
            action.accept(m);
        }
    }


    private void addMoveInternal(@NotNull Move move) {
        Move last = mInternalMoves.peekLast();

        // parsing, can be better than this
        if (last != null && last.isReverse(move, n)) {
            mInternalMoves.removeLast();
        } else {
            mInternalMoves.addLast(move);
        }
    }

    public int getAllAppliedMovesCount() {
        return CollectionUtil.size(mInternalMoves);
    }

    @NotNull
    public List<Move> getAllAppliedMovesCopy(boolean randomAccess) {
        return randomAccess? CollectionUtil.arrayListCopy(mInternalMoves): CollectionUtil.linkedListCopy(mInternalMoves);
    }

    private void addMoveInStack(@NotNull Move move) {
        if (mMovesStack == null) {
            mMovesStack = new LinkedList<>();
        }

        mMovesStack.addLast(move);
    }

    public int getMovesInStackCount() {
        return CollectionUtil.size(mMovesStack);
    }

    @NotNull
    @Override
    public List<Move> getInternalSolution() {
        final Iterator<Move> itr = mInternalMoves.descendingIterator();
        final List<Move> sol = new LinkedList<>();

        while (itr.hasNext()) {
            sol.add(itr.next().reverse());
        }

        return sol;
    }


    public void addListener(@NotNull Listener listener) {
        mListeners.addListener(listener);
    }

    public boolean removeListener(@NotNull Listener listener) {
        return mListeners.removeListener(listener);
    }

    public void ensureListener(@NotNull Listener listener) {
        mListeners.ensureListener(listener);
    }

    private void onMoveApplied(@NotNull Move move, int cubiesAffected, boolean saved) {
//        if (cubiesAffected > 0) {
//            System.out.println("Move applied: " + move);
//        }

        mListeners.forEachListener(l -> l.onMoveApplied(Cube.this, move, cubiesAffected, saved));
    }

    /**
     * @return whether the given move has affected the cube
     * */
    public boolean applyMove(@NotNull Move move, boolean saveInStack) {
        if (isLocked())
            return false;

        final Move.CubieFilter filter = move.cubieFilter(n);

        int qbCounter = 0;
        for (Cubie qb : cubies) {
            if (filter.test(qb)) {
//                final int h = cacheHeuristic();
//                final int ch = qb.calculateHeuristic();
                qb.applyMove(move);
//                mCachedHeuristic = h - ch + qb.calculateHeuristic();
                qbCounter++;
            }
        }

        final boolean applied = qbCounter > 0;
        if (applied) {
            addMoveInternal(move);
        }

        final boolean save = applied && saveInStack;
        if (save) {
            addMoveInStack(move);
        }

        onMoveApplied(move, qbCounter, save);
        return applied;
    }

    public boolean applyMove(@NotNull Move move) {
        return applyMove(move, true);
    }

    public boolean undoLastMove() {
        if (isLocked())
            return false;

        final Move last = pollLastMove();
        if (last == null)
            return false;
        return applyMove(last.reverse(), false);
    }

    public boolean rotateX(int quarters, boolean saveInStack) {
        if (isLocked())
            return false;

        return applyMove(rotateXMove(quarters), saveInStack);
    }

    public boolean rotateY(int quarters, boolean saveInStack) {
        if (isLocked())
            return false;

        return applyMove(rotateYMove(quarters), saveInStack);
    }

    public boolean rotateZ(int quarters, boolean saveInStack) {
        if (isLocked())
            return false;

        return applyMove(rotateZMove(quarters), saveInStack);
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

