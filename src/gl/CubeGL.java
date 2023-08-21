package gl;

import gl.animation.Animator;
import gl.animation.Interpolator;
import model.cube.Cube;
import model.cube.CubeI;
import model.cubie.Cubie;
import model.cubie.Move;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import processing.core.PApplet;
import util.CollectionUtil;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;


public class CubeGL implements Drawable, MoveGL.Listener {

    /**
     * Class to cache filter of moves, so that they can be accessed on fly in draw
     * */
    private static class CubieFilterWrapper {

        @NotNull
        private final Predicate<Cubie> filter;

        private CubieFilterWrapper(@NotNull Predicate<Cubie> filter) {
            this.filter = filter;
        }
    }

    @NotNull
    private static Predicate<Cubie> cubieFilter(@NotNull MoveGL move, int n) {
        final Object tag = move.getTag();
        if (tag instanceof CubieFilterWrapper) {
            return ((CubieFilterWrapper) tag).filter;
        }

        final CubieFilterWrapper w = new CubieFilterWrapper(move.getMove().cubieFilter(n));
        move.setTag(w);
        return w.filter;
    }


    @NotNull
    private final Cube cube;
    @NotNull
    private final CubieGL[] cubiesGL;

    private boolean mAnimateMoves = GLConfig.DEFAULT_ANIMATE_MOVES;
    @NotNull
    private final LinkedList<MoveGL> mCurMoves = new LinkedList<>();        // like a queue
    @NotNull
    private final LinkedList<MoveGL> mPendingMoves = new LinkedList<>();            // like a queue

    @Nullable
    private Consumer<MoveGL> mMoveGlConfigurer;

    @Nullable
    private Interpolator mMoveGlInterpolatorOverride;

    private int mMoveQuarterDurationMs = GLConfig.MOVE_QUARTER_DURATION_MS_DEFAULT;

    public CubeGL(@NotNull Cube cube) {
        this.cube = cube;

        cubiesGL = new CubieGL[cube.noOfCubies()];
        for (int i=0; i < cubiesGL.length; i++) {
            cubiesGL[i] = new CubieGL(cube.getCubie(i));
        }
    }

    @NotNull
    public final Cube getCube() {
        return cube;
    }

    public boolean areMovesAnimating() {
        return mAnimateMoves;
    }

    public void setAnimateMoves(boolean animateMoves) {
        mAnimateMoves = animateMoves;
    }

    public void toggleAnimateMoves() {
        setAnimateMoves(!areMovesAnimating());
    }


    public int runningMovesCount() {
        return mCurMoves.size();
    }

    public boolean hasRunningMoves() {
        return runningMovesCount() > 0;
    }

    @Nullable
    public Move getRecentRunningMove() {
        final MoveGL last = mCurMoves.peekLast();
        return last != null? last.getMove(): null;
    }

    public int pendingMovesCount() {
        return mPendingMoves.size();
    }

    public boolean hasPendingMoves() {
        return pendingMovesCount() > 0;
    }

    public void finishRunningMoves(boolean cancel) {
        for (MoveGL m: CollectionUtil.linkedListCopy(mCurMoves)) {
            m.finish(cancel);
        }

        mCurMoves.clear();
    }

    public void finishAllMoves(boolean cancel) {
        final LinkedList<MoveGL> all = new LinkedList<>();
        CollectionUtil.addAll(mCurMoves, all);
        CollectionUtil.addAll(mPendingMoves, all);
        mCurMoves.clear();
        mPendingMoves.clear();

        for (MoveGL m: all) {
            m.removeListener(this);

            m.finish(cancel);
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
        mCurMoves.remove(move);
    }

    private void doStartMove(@NotNull MoveGL moveGL) {
        moveGL.reset();
        moveGL.addListener(this);
        mCurMoves.addLast(moveGL);
        moveGL.start();
    }


    private void commitInternal(Move move, boolean saveInStack) {
        cube.applyMove(move, saveInStack);
    }

    private void commitInternal(@NotNull MoveGL move) {
        commitInternal(move.getMove(), move.shouldSaveInStack());
    }

    private void startNextPendingMove() {
        final MoveGL next = mPendingMoves.pollFirst();
        if (next != null) {
            doStartMove(next);
        }
    }

    private void considerStartPendingMove() {
        if (mCurMoves.isEmpty()) {
            startNextPendingMove();
        }
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


    public int getMoveQuarterDurationMs() {
        return mMoveQuarterDurationMs;
    }

    public float getMoveQuarterDurationPercent() {
        return GLConfig.moveQuarterDurationMsToPercent(mMoveQuarterDurationMs);
    }

    /**
     * @return new duration ms
     * */
    public int setMoveQuarterDurationMs(int moveQuarterDurationMs) {
        mMoveQuarterDurationMs = GLConfig.constraintMoveQuarterDurationMs(moveQuarterDurationMs);
        return mMoveQuarterDurationMs;
    }

    /**
     * @return new duration percent
     * */
    public float setMoveQuarterDurationPercent(float percent) {
        mMoveQuarterDurationMs = GLConfig.percentToMoveQuarterDurationMs(percent);
        return getMoveQuarterDurationPercent();
    }

    /**
     * @return new duration percent
     * */
    public float changeMoveQuarterDurationPercentBy(float percentDelta) {
        return setMoveQuarterDurationPercent(getMoveQuarterDurationPercent() + percentDelta);
    }

    /**
     * @return new duration percent
     * */
    public float incMoveQuarterDuration(boolean continuous) {
        return changeMoveQuarterDurationPercentBy(continuous? GLConfig.MOVE_QUARTER_SPEED_PERCENT_CONTINUOUS_INCREMENT: GLConfig.moveQuarterDurationMsIncPercent(getMoveQuarterDurationPercent()));
    }

    /**
     * @return new duration percent
     * */
    public float decMoveQuarterDuration(boolean continuous) {
        return changeMoveQuarterDurationPercentBy(-(continuous? GLConfig.MOVE_QUARTER_SPEED_PERCENT_CONTINUOUS_INCREMENT: GLConfig.moveQuarterDurationMsDecPercent(getMoveQuarterDurationPercent())));
    }



    /**
     * Configures a newly created MoveGL object
     * */
    protected void initMoveGl(@NotNull MoveGL moveGL) {
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

    public void applyMove(@NotNull Move move, boolean saveInStack, boolean now) {
        if (mAnimateMoves) {
            final MoveGL m = new MoveGL(move);
            initMoveGl(m);
            m.setSaveInStack(saveInStack);

            if (now || (mCurMoves.isEmpty() && mPendingMoves.isEmpty())) {
                doStartMove(m);
            } else {
                mPendingMoves.addLast(m);
            }
        } else {
            commitInternal(move, saveInStack);
        }
    }

    public void applyMove(@NotNull Move move, boolean saveInStack) {
        applyMove(move, saveInStack, GLConfig.DEFAULT_APPLY_MOVE_NOW);
    }

    public void applyMove(@NotNull Move move) {
        applyMove(move, true);
    }

    public boolean undoLastMove(boolean now) {
        final Move last = cube.pollLastMove();
        if (last == null)
            return false;

        applyMove(last.reverse(), false, now);
        return true;
    }

    public boolean undoLastMove() {
        return undoLastMove(GLConfig.DEFAULT_UNDO_MOVE_NOW);
    }

    public void rotateX(int quarters, boolean saveInStack, boolean now) {
        applyMove(cube.rotateXMove(quarters), saveInStack, now);
    }

    public void rotateX(int quarters, boolean saveInStack) {
        rotateX(quarters, saveInStack, GLConfig.DEFAULT_ROTATE_NOW);
    }

    public void rotateY(int quarters, boolean saveInStack, boolean now) {
        applyMove(cube.rotateYMove(quarters), saveInStack, now);
    }

    public void rotateY(int quarters, boolean saveInStack) {
        rotateY(quarters, saveInStack, GLConfig.DEFAULT_ROTATE_NOW);
    }

    public void rotateZ(int quarters, boolean saveInStack, boolean now) {
        applyMove(cube.rotateZMove(quarters), saveInStack, now);
    }

    public void rotateZ(int quarters, boolean saveInStack) {
        rotateZ(quarters, saveInStack, GLConfig.DEFAULT_ROTATE_NOW);
    }


    public void applySequence(@NotNull List<Move> sequence, boolean saveInStack, boolean now) {
        for (Move m: sequence) {
            applyMove(m, saveInStack, now);
        }
    }

    public void applySequence(@NotNull List<Move> sequence, boolean saveInStack) {
        applySequence(sequence, saveInStack, GLConfig.DEFAULT_APPLY_SEQUENCE_NOW);
    }

    public void applySequence(@NotNull List<Move> sequence) {
        applySequence(sequence, true);
    }

    public void scramble(int moves, boolean saveInStack, boolean now) {
        applySequence(cube.createScrambleSequence(moves), saveInStack, now);
    }

    public void scramble(int moves, boolean saveInStack) {
        scramble(moves, saveInStack, GLConfig.DEFAULT_SCRAMBLE_NOW);
    }

    public void scramble(int moves) {
        scramble(moves, true);
    }

    public void scramble() {
        scramble(CubeI.DEFAULT_SCRAMBLE_MOVES);
    }



    /* Draw */

    @Override
    public void draw(@NotNull PApplet p) {
        final List<MoveGL> moves = CollectionUtil.linkedListCopy(mCurMoves);

        final Iterator<MoveGL> itr = moves.iterator();
        while (itr.hasNext()) {
            MoveGL move = itr.next();
            if (move.isFinished() || move.updateAndGetCurrentValue() == 0 || move.isFinished()) {
                itr.remove();
            }
        }

        if (moves.isEmpty()) {
            for (CubieGL qb: cubiesGL) {
                qb.draw(p);
            }
        } else {
            for (CubieGL qbgl: cubiesGL) {
                final Cubie qb = qbgl.getCubie();
                float rx = 0, ry = 0, rz = 0;

                for (MoveGL movegl: moves) {
                    final Predicate<Cubie> filter = cubieFilter(movegl, cube.n);
                    if (filter.test(qb)) {
                        final Move move = movegl.getMove();
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
                    qbgl.draw(p);
                } else {
                    p.pushMatrix();
                    p.rotateX(rx);
                    p.rotateY(ry);
                    p.rotateZ(rz);

                    qbgl.draw(p);
                    p.popMatrix();
                }
            }
        }
    }



    /* Callbacks */

    @Override
    public void onMoveStarted(@NotNull MoveGL moveGL, boolean resumed) {
    }

    @Override
    public void onMovePaused(@NotNull MoveGL moveGL) {
        // no-op
    }

    @Override
    public void onMoveFinished(@NotNull MoveGL moveGL, @NotNull Animator.Finish how) {
        // Commit this move
        if (how != Animator.Finish.CANCEL) {
            commitInternal(moveGL);
        }

        releaseFinishedMove(moveGL);
        considerStartPendingMove();
    }


}
