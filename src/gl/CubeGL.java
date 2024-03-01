package gl;

import gl.animation.Animator;
import gl.animation.interpolator.Interpolator;
import model.cube.Cube;
import model.cube.CubeI;
import model.cubie.Cubie;
import model.cubie.Move;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import processing.core.PApplet;
import util.U;
import util.live.Listeners;
import util.misc.CollectionUtil;

import java.util.*;
import java.util.function.Consumer;


public class CubeGL implements Drawable, Cube.Listener, MoveGL.Listener {

    public interface Listener extends Cube.Listener, MoveGL.Listener {

        void onCubeChanged(@NotNull CubeGL cubeGL, @NotNull Cube old, @NotNull Cube _new);

        void onMoveAnimationEnabledChanged(@NotNull CubeGL cubeGL, boolean moveAnimationEnabled);

        void onMoveGlConfigurerChanged(@NotNull CubeGL cubeGL, @Nullable Consumer<MoveGL> old, @Nullable Consumer<MoveGL> _new);

        void onMoveGlInterpolatorOverrideChanged(@NotNull CubeGL cubeGL, @Nullable Interpolator old, @Nullable Interpolator _new);
    }


    @NotNull
    private Cube mCube;
    @NotNull
    private final ArrayList<CubieGL> mCubiesGL = new ArrayList<>();

    /* GL Parameters (independent of underlying cube) */
    private volatile boolean mAnimateMoves = GLConfig.DEFAULT_ANIMATE_MOVES;
    @NotNull
    private final LinkedHashSet<MoveGL> mRunningMoves = new LinkedHashSet<>();        // A set of all the moves that are currently running simultaneously
    @NotNull
    private final LinkedList<MoveGL> mPendingMoves = new LinkedList<>();            // A queue of pending moves, to be applied once every running move is finished

    private int mMoveQuarterDurationMs = GLConfig.MOVE_QUARTER_DURATION_MS_DEFAULT;
    @Nullable
    private Consumer<MoveGL> mMoveGlConfigurer;
    @Nullable
    private Interpolator mMoveGlInterpolatorOverride;

    private final Listeners<Listener> mListeners = new Listeners<>();

    public CubeGL(@NotNull Cube cube) {
        setCube(cube, true, false);
    }

    @NotNull
    public final Cube getCube() {
        return mCube;
    }

    public final int getN() {
        return mCube.n;
    }

    private void updateCubiesGl() {
        mCubiesGL.clear();

        for (int i = 0; i < mCube.noOfCubies(); i++) {
            mCubiesGL.add(new CubieGL(mCube.getCubie(i)));
        }
    }

    private boolean setCube(@NotNull Cube cube, boolean force, boolean notify) {
        final Cube old = mCube;
        if (old == cube)
            return false;

        // detach
        if (old != null) {
            if (!force && old.isLocked())
                return false;

            old.removeListener(this);
        }

        cancelAllMoves();       // cancel all moves

        // Main update
        mCube = cube;
        updateCubiesGl();

        mCube.ensureListener(this);     // attach
        if (notify) {
            mListeners.forEachListener(l -> l.onCubeChanged(this, old, cube));
        }

        return true;
    }

    public boolean setCube(@NotNull Cube cube, boolean force) {
        return setCube(cube, force, true);
    }

    public final boolean setN(int n, boolean force) {
        if (!force && mCube.isLocked())
            return false;

        if (n < 2 || n > CubeI.DEFAULT_MAX_N || n == getN())
            return false;

        return setCube(new Cube(n), force);
    }

    public boolean stepN(boolean increment, boolean force) {
        return setN(getN() + (increment? 1: -1), force);
    }

    public final boolean resetCube(boolean force) {
        if (!force && mCube.isLocked())
            return false;

        final Cube c = getCube();
        if (c.cacheIsSolved())
            return false;

        return setCube(new Cube(c.n), force);
    }


    protected void onMoveAnimationEnabledChanged(boolean animationEnabled) {
        mListeners.forEachListener(l -> l.onMoveAnimationEnabledChanged(this, animationEnabled));
    }

    public boolean isMoveAnimationEnabled() {
        return mAnimateMoves;
    }

    public void setMoveAnimationEnabled(boolean moveAnimationEnabled) {
        final boolean prev = mAnimateMoves;
        if (prev == moveAnimationEnabled)
            return;

        mAnimateMoves = moveAnimationEnabled;
        onMoveAnimationEnabledChanged(moveAnimationEnabled);
    }

    public void toggleMoveAnimationEnabled() {
        setMoveAnimationEnabled(!isMoveAnimationEnabled());
    }


    public int runningMovesCount() {
        return mRunningMoves.size();
    }

    public boolean hasRunningMoves() {
        return runningMovesCount() > 0;
    }

    /**
     * @return the move which is animating for the longest period of time (among all currently running moves) i.e. the oldest move, or {@code null} if no move is currently animating
     * */
    @Nullable
    public Move getOldestRunningMove() {
        try {
            return mRunningMoves.getFirst().getMove();
        } catch (NoSuchElementException ignored) {
            return null;
        }
    }

    /**
     * @return the move which is animating for the shortest period of time (among all currently running moves) i.e. the youngest move, or {@code null} if no move is currently animating
     * */
    @Nullable
    public Move getYoungestRunningMove() {
        try {
            return mRunningMoves.getLast().getMove();
        } catch (NoSuchElementException ignored) {
            return null;
        }
    }


    public int pendingMovesCount() {
        return mPendingMoves.size();
    }

    public boolean hasPendingMoves() {
        return pendingMovesCount() > 0;
    }

    public void finishRunningMoves(boolean cancel) {
        if (mRunningMoves.isEmpty())
            return;

        final Collection<MoveGL> runningMoves = CollectionUtil.linkedListCopy(mRunningMoves);
        mRunningMoves.clear();

        for (MoveGL m: CollectionUtil.linkedListCopy(runningMoves)) {
            m.finish(cancel);
        }
    }

    public void finishAllMoves(boolean cancel) {
        if (mRunningMoves.isEmpty() && mPendingMoves.isEmpty())
            return;

        final LinkedList<MoveGL> all = new LinkedList<>();
        CollectionUtil.addAll(mRunningMoves, all);
        CollectionUtil.addAll(mPendingMoves, all);
        mRunningMoves.clear();
        mPendingMoves.clear();

        for (MoveGL m: all) {
            m.removeListener(this);

            m.finish(cancel);
            // Since listener is detached, must be handled manually
            if (!cancel) {
                commitInternal(m);
            }
        }
    }


    public void cancelPendingMoves() {
        mPendingMoves.clear();
    }

    public void cancelAllMoves() {
        cancelPendingMoves();
        finishRunningMoves(true);
    }


    private void releaseFinishedMove(@NotNull MoveGL move) {
        move.removeListener(this);
        mRunningMoves.remove(move);
    }

    private boolean commitInternal(Move move, boolean saveInStack) {
        return mCube.applyMove(move, saveInStack);
    }

    private boolean commitInternal(@NotNull MoveGL move) {
        return commitInternal(move.getMove(), move.shouldSaveInStack());
    }

    private void startNextPendingMove() {
        final MoveGL next = mPendingMoves.pollFirst();
        if (next != null) {
            initAndStartMove(next, true);
        }
    }

    private void considerStartPendingMove() {
        if (mRunningMoves.isEmpty()) {      // when all running moves are finished
            startNextPendingMove();
        }
    }

    private void forEachPending(@NotNull Consumer<MoveGL> action) {
        mPendingMoves.forEach(action);
    }


    //    /**
//     * @param now whether to start this move right now, or wait for pending moves to finish
//     * */
//    private void applyMove(@NotNull MoveGL moveGL, boolean now) {
//        if (now || (mCurMoves.isEmpty() && mPendingMoves.isEmpty())) {
//            doStartMove(moveGL);
//        } else {
//            mPendingMoves.addLast(moveGL);
//        }
//    }
//
//    public void applyMove(@NotNull MoveGL moveGL) {
//        applyMove(moveGL, GLConfig.DEFAULT_APPLY_MOVE_NOW);
//    }

    @Nullable
    public Consumer<MoveGL> getMoveGlConfigurer() {
        return mMoveGlConfigurer;
    }

    protected void onMoveGlConfigurerChanged(@Nullable Consumer<MoveGL> old, @Nullable Consumer<MoveGL> _new) {
        mListeners.forEachListener(l -> l.onMoveGlConfigurerChanged(this, old, _new));
    }

    public CubeGL setMoveGlConfigurer(@Nullable Consumer<MoveGL> moveGlConfigurer) {
        if (mMoveGlConfigurer != moveGlConfigurer) {
            final Consumer<MoveGL> old = mMoveGlConfigurer;
            mMoveGlConfigurer = moveGlConfigurer;
            onMoveGlConfigurerChanged(old, moveGlConfigurer);
        }

        return this;
    }

    @Nullable
    public Interpolator getMoveGlInterpolatorOverride() {
        return mMoveGlInterpolatorOverride;
    }

    protected void onMoveGlInterpolatorOverrideChanged(@Nullable Interpolator old, @Nullable Interpolator _new) {
        forEachPending(m -> m.setInterpolator(_new));

        mListeners.forEachListener(l -> l.onMoveGlInterpolatorOverrideChanged(this, old, _new));
    }

    public boolean setMoveGlInterpolatorOverride(@Nullable Interpolator moveGlInterpolatorOverride) {
        if (mMoveGlInterpolatorOverride == moveGlInterpolatorOverride) {
            return false;
        }

        final Interpolator old = mMoveGlInterpolatorOverride;
        mMoveGlInterpolatorOverride = moveGlInterpolatorOverride;
        onMoveGlInterpolatorOverrideChanged(old, moveGlInterpolatorOverride);
        return true;
    }

    public boolean resetMoveGlInterpolatorOverride() {
        return setMoveGlInterpolatorOverride(null);
    }


    public int getMoveQuarterDurationMs() {
        return mMoveQuarterDurationMs;
    }

    public float getMoveQuarterDurationPercent() {
        return GLConfig.moveQuarterDurationMsToPercent(mMoveQuarterDurationMs);
    }


    protected void onMoveQuarterDurationMsChanged(int oldMs, int newMs) {
        forEachPending(m -> m.setQuarterDurationMs(newMs));
    }

    /**
     * @return new duration ms
     * */
    public int setMoveQuarterDurationMs(int moveQuarterDurationMs) {
        final int oldMs = mMoveQuarterDurationMs;
        final int newMs = GLConfig.constraintMoveQuarterDurationMs(moveQuarterDurationMs);
        if (oldMs != newMs) {
            mMoveQuarterDurationMs = newMs;
            onMoveQuarterDurationMsChanged(oldMs, newMs);
        }

        return mMoveQuarterDurationMs;
    }

    public int resetMoveQuarterDurationMs() {
        return setMoveQuarterDurationMs(GLConfig.MOVE_QUARTER_DURATION_MS_DEFAULT);
    }

    /**
     * @return new duration percent
     * */
    public float setMoveQuarterDurationPercent(float percent) {
        return GLConfig.moveQuarterDurationMsToPercent(setMoveQuarterDurationMs(GLConfig.percentToMoveQuarterDurationMs(percent)));
    }

    /**
     * @return new duration percent
     * */
    public float changeMoveQuarterDurationPercentBy(float percentDelta) {
        return setMoveQuarterDurationPercent(U.constrain_0_100(getMoveQuarterDurationPercent() + percentDelta));
    }

    /**
     * @return new duration ms
     * */
    public int stepMoveQuarterDurationMs(boolean continuous, boolean increment) {
        return setMoveQuarterDurationMs(GLConfig.stepMoveQuarterDurationMs(getMoveQuarterDurationMs(), continuous, increment));
    }


    public float getMoveQuarterSpeedPercent() {
        return GLConfig.convertDurationAndSpeedPercent(getMoveQuarterDurationPercent());
    }

    /**
     * @return new speed percent
     * */
    public float setMoveQuarterSpeedPercent(float speedPercent) {
        return GLConfig.convertDurationAndSpeedPercent(setMoveQuarterDurationPercent(GLConfig.convertDurationAndSpeedPercent(speedPercent)));
    }

    public float changeMoveQuarterSpeedPercentBy(float percentDelta) {
        return setMoveQuarterSpeedPercent(U.constrain_0_100(getMoveQuarterSpeedPercent() + percentDelta));
    }

    public void stepMoveQuarterSpeed(boolean continuous, boolean increment) {
        stepMoveQuarterDurationMs(continuous, !increment /* speed is inverse of duration ms */);
    }


    @NotNull
    private MoveGL createMoveGl(@NotNull Move move, boolean saveInStack) {
        final MoveGL moveGl = new MoveGL(move);
        moveGl.setSaveInStack(saveInStack);
        moveGl.ensureListener(this);
        return moveGl;
    }

    /**
     * Configures a newly created MoveGL object
     * */
    protected void initMoveGl(@NotNull MoveGL moveGL) {
        moveGL.reset();

        final Consumer<MoveGL> config = mMoveGlConfigurer;
        if (config != null) {
            config.accept(moveGL);
        }

        final Interpolator interp = mMoveGlInterpolatorOverride;
        if (interp != null) {
            moveGL.setInterpolator(interp);
        }

        moveGL.setQuarterDurationMs(mMoveQuarterDurationMs);
    }

    private void initAndStartMove(@NotNull MoveGL moveGL, boolean checkAnimate) {
        if (checkAnimate && !mAnimateMoves) {
            moveGL.reset();
            moveGL.finish(false);       // directly finish: commit internal
        } else {
            initMoveGl(moveGL);
            moveGL.start();
        }
    }

    /**
     * @return whether the move is enqueued, or affected the cube when directly applied
     * */
    public boolean applyMove(@NotNull Move move, boolean saveInStack, boolean now) {
        if (mCube.isLocked())
            return false;

        if (mAnimateMoves) {
            final MoveGL moveGl = createMoveGl(move, saveInStack);

            if (now || (mRunningMoves.isEmpty() && mPendingMoves.isEmpty())) {
                if (mRunningMoves.isEmpty()) {     // if no currently running moves, start this move directly
                    initAndStartMove(moveGl, false);
                } else {
                    mPendingMoves.addFirst(moveGl);     // make it the first pending move
                }
            } else {
                mPendingMoves.addLast(moveGl);
            }

            return true;        // enqueued
        }

        return commitInternal(move, saveInStack);
    }

    public boolean applyMove(@NotNull Move move, boolean saveInStack) {
        return applyMove(move, saveInStack, GLConfig.DEFAULT_APPLY_MOVE_NOW);
    }

    public boolean applyMove(@NotNull Move move) {
        return applyMove(move, true);
    }

    public boolean undoLastCommittedMove(boolean now) {
        if (mCube.isLocked())
            return false;

        final Move last = mCube.pollLastMove();
        if (last == null)
            return false;

        return applyMove(last.reverse(), false, now);
    }

    public boolean undoLastCommittedMove() {
        return undoLastCommittedMove(GLConfig.DEFAULT_UNDO_LAST_MOVE_NOW);
    }

    public boolean undoRunningOrLastCommittedMove(boolean now) {
        if (mCube.isLocked())
            return false;

        Move move = getYoungestRunningMove();
        if (move == null) {
            move = mCube.pollLastMove();
            if (move == null)
                return false;
        }

        return applyMove(move.reverse(), false, now);
    }

    public boolean undoRunningOrLastCommittedMove() {
        return undoRunningOrLastCommittedMove(GLConfig.DEFAULT_UNDO_LAST_MOVE_NOW);
    }

    public boolean rotateX(int quarters, boolean saveInStack, boolean now) {
        if (mCube.isLocked())
            return false;

        return applyMove(mCube.rotateXMove(quarters), saveInStack, now);
    }

    public boolean rotateX(int quarters, boolean saveInStack) {
        return rotateX(quarters, saveInStack, GLConfig.DEFAULT_ROTATE_NOW);
    }

    public boolean rotateY(int quarters, boolean saveInStack, boolean now) {
        if (mCube.isLocked())
            return false;

        return applyMove(mCube.rotateYMove(quarters), saveInStack, now);
    }

    public boolean rotateY(int quarters, boolean saveInStack) {
        return rotateY(quarters, saveInStack, GLConfig.DEFAULT_ROTATE_NOW);
    }

    public boolean rotateZ(int quarters, boolean saveInStack, boolean now) {
        if (mCube.isLocked())
            return false;

        return applyMove(mCube.rotateZMove(quarters), saveInStack, now);
    }

    public boolean rotateZ(int quarters, boolean saveInStack) {
        return rotateZ(quarters, saveInStack, GLConfig.DEFAULT_ROTATE_NOW);
    }


    public boolean applySequence(@NotNull List<Move> sequence, boolean saveInStack, boolean now) {
        if (mCube.isLocked())
            return false;

        for (Move m: sequence) {
            applyMove(m, saveInStack, now);
        }

        return true;
    }

    public boolean applySequence(@NotNull List<Move> sequence, boolean saveInStack) {
        return applySequence(sequence, saveInStack, GLConfig.DEFAULT_APPLY_SEQUENCE_NOW);
    }

    public boolean applySequence(@NotNull List<Move> sequence) {
        return applySequence(sequence, true);
    }

    public boolean scramble(int moves, boolean saveInStack, boolean now) {
        if (mCube.isLocked())
            return false;

        return applySequence(mCube.createScrambleSequence(moves), saveInStack, now);
    }

    public boolean scramble(int moves, boolean saveInStack) {
        return scramble(moves, saveInStack, GLConfig.DEFAULT_SCRAMBLE_NOW);
    }

    public boolean scramble(int moves) {
        return scramble(moves, true);
    }

    public boolean scramble() {
        return scramble(CubeI.DEFAULT_SCRAMBLE_MOVES);
    }



    private boolean updateAndCheckInactive(@NotNull MoveGL move) {
        return move.isFinished() || move.updateAndGetCurrentValue() == 0 /* Main update call */ || move.isFinished() /* also check after update */;
    }

    private boolean updateAndCheckActive(@NotNull MoveGL move) {
        return !updateAndCheckInactive(move);
    }

    /* Draw */

    @Override
    public void draw(@NotNull PApplet p) {
        final Collection<MoveGL> moves = CollectionUtil.linkedListCopy(mRunningMoves);      // Must create a copy each time
        moves.removeIf(this::updateAndCheckInactive);       // update call results in onMOveStarted and onMOveFinished callbacks which alters the main array

        if (moves.isEmpty()) {
            for (CubieGL qbGl: mCubiesGL) {
                qbGl.draw(p);
            }
        } else {
            for (CubieGL qbGl: mCubiesGL) {
                final Cubie qb = qbGl.getCubie();
                float rx = 0, ry = 0, rz = 0;

                for (MoveGL movegl: moves) {
                    final Move move = movegl.getMove();
                    final Move.CubieFilter filter = move.cubieFilter(mCube.n);
                    if (filter.test(qb)) {
                        final float curVal = movegl.getCurrentVal();

                        if (move.axis.isX()) {
                            rx += curVal;
                        } else if (move.axis.isY()) {
                            ry += curVal;
                        } else {
                            rz += curVal;
                        }
                    }
                }

                if (rx == 0 && ry == 0 && rz == 0) {
                    qbGl.draw(p);
                } else {
                    p.pushMatrix();
                    p.rotateX(rx);
                    p.rotateY(ry);
                    p.rotateZ(rz);

                    qbGl.draw(p);
                    p.popMatrix();
                }
            }
        }
    }



    /* Listeners */

    public void addListener(@NotNull Listener listener) {
        mListeners.addListener(listener);
    }

    public boolean removeListener(@NotNull Listener listener) {
        return mListeners.removeListener(listener);
    }

    public void ensureListener(@NotNull Listener listener) {
        mListeners.ensureListener(listener);
    }


    /* Cube Listener */

    @Override
    public void onCubeLockChanged(@NotNull Cube cube, boolean locked) {
        mListeners.forEachListener(l -> l.onCubeLockChanged(cube, locked));
    }

    @Override
    public void onMoveApplied(@NotNull Cube cube, @NotNull Move move, int cubiesAffected, boolean saved) {
        mListeners.forEachListener(l -> l.onMoveApplied(cube, move, cubiesAffected, saved));
    }


    /* MoveGL Listener */

    @Override
    public void onMoveStarted(@NotNull MoveGL moveGL, boolean resumed) {
        mRunningMoves.add(moveGL);
        mListeners.forEachListener(l -> l.onMoveStarted(moveGL, resumed));
    }

    @Override
    public void onMovePaused(@NotNull MoveGL moveGL) {
        mListeners.forEachListener(l -> l.onMovePaused(moveGL));
    }

    @Override
    public void onMoveFinished(@NotNull MoveGL moveGL, @NotNull Animator.Finish how) {
        // Commit this move
        if (how != Animator.Finish.CANCEL) {
            commitInternal(moveGL);
        }

        releaseFinishedMove(moveGL);
        mListeners.forEachListener(l -> l.onMoveFinished(moveGL, how));
        considerStartPendingMove();
    }
}
