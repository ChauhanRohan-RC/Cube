package main;

import gl.CubeGL;
import gl.GLConfig;
import gl.MoveGL;
import gl.animation.Animator;
import gl.animation.FloatAnimator;
import gl.animation.interpolator.Interpolator;
import model.Axis;
import model.Point3DF;
import model.cube.Cube;
import model.cube.CubeI;
import model.cubie.Move;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.event.KeyEvent;
import processing.opengl.PJOGL;
import solver.Solver;
import sound.MidiNotePlayer;
import util.Config;
import util.Format;
import util.U;
import util.async.Async;
import util.async.CancellationProvider;
import util.async.Canceller;

import java.awt.*;
import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

public class CubePUi3D extends PApplet implements CubeGL.Listener {

    private static final boolean DEFAULT_FULLSCREEN = false;
    private static final Dimension DEFAULT_WINDOW_SIZE = U.scaleDimension(U.SCREEN_RESOLUTION_NATIVE, 0.9f, 0.86f);

    /**
     * Custom event with a {@link Runnable} task payload to be executed on the UI thread
     *
     * @see #enqueueTask(Runnable)
     * @see #handleKeyEvent(KeyEvent)
     * */
    private static final int ACTION_EXECUTE_RUNNABLE = 121230123;


    @NotNull
    public static Point3DF cubeOrigin(int width, int height, int n) {
        return new Point3DF(width / 2f, height / 2f, 0);
    }

    public static float cubeScale(float width, float height, int n) {
        float size = Math.min(width, height);
        return size / (n * CubeI.CUBIE_SIDE_LEN * 2.5f);     // todo factor
    }

    public static boolean shouldRollOverRotatingCube(@NotNull KeyEvent event) {
        return event.isControlDown();
    }

    @NotNull
    private static Move createMove(@NotNull Axis axis, boolean shift, boolean ctrl, boolean alt) {
        int[] layers = Move.LAYERS_0;
        final int q;

        if (alt) {
            q = Move.QUARTERS_2;
        } else {
            q = shift ? Move.QUARTERS_ANTICLOCKWISE : Move.QUARTERS_CLOCKWISE;
            if (ctrl) {
                layers = Move.LAYERS_0_1;
            }
        }

        return new Move(axis, q, layers);
    }

    @NotNull
    private static Move createMove(@NotNull Axis axis, @NotNull KeyEvent event) {
        return createMove(axis, event.isShiftDown(), event.isControlDown(), event.isAltDown());
    }


    /* Ui */
    private int _w, _h;
    private PImage bgImage;
    private PFont pdSans, pdSansMedium;

    /* HUD and Controls */
    private boolean mHudEnabled = GLConfig.DEFAULT_HUD_ENABLED;
    private boolean mShowKeyBindings = GLConfig.DEFAULT_SHOW_KEY_BINDINGS;
    @Nullable
    private KeyEvent mKeyEvent;

    /* Cube */
    @NotNull
    public final CubeGL cubeGL;
    @Nullable
    private FloatAnimator mLevitationAnimator;

    /* Solver */
    private volatile boolean mSolving;
    @Nullable
    private volatile Canceller mSolveCanceller;
    @Nullable
    private volatile Solver.Solution mCurSolution;
    private volatile boolean mLockCubeWhileSolving = GLConfig.DEFAULT_LOCK_CUBE_WHILE_SOLVING;

    /* Camera */
    private boolean mFreeCamera = GLConfig.DEFAULT_FREE_CAMERA;
    private PeasyCam mPeasyCam;
    private volatile float mCubeDrawScale = GLConfig.CUBE_DRAW_SCALE_DEFAULT;

    /* Window */
    private boolean mFullscreen = DEFAULT_FULLSCREEN;
    @NotNull
    private Dimension mInitialWindowSize = DEFAULT_WINDOW_SIZE;

    /* Sound */
    private boolean mSoundEnabled = GLConfig.DEFAULT_SOUND_ENABLED;
    private boolean mPolyRhythmEnabled = GLConfig.DEFAULT_POLY_RHYTHM_ENABLED;
    @Nullable
    private volatile MidiNotePlayer mSoundPlayer;

    public CubePUi3D(@Nullable Cube cube) {
        final Config config = R.CONFIG;

        // Cube
        if (cube == null) {
            cube = new Cube(config.getValueInt(R.CONFIG_KEY_CUBE_SIZE, CubeI.DEFAULT_N));
        }

        cubeGL = new CubeGL(cube);

        // Config
        applyConfig(config);

        // Listeners At Last
        cubeGL.ensureListener(this);
    }

    public CubePUi3D(int n) {
        this(n >= 2 ? new Cube(n) : null);
    }

    public CubePUi3D() {
        this(null);
    }


    public final void applyConfig(@NotNull Config config) {
        // Window
        mFullscreen = config.getValueBool(R.CONFIG_KEY_FULLSCREEN, DEFAULT_FULLSCREEN);
        mInitialWindowSize = R.getConfigWindowSize(config, U.SCREEN_RESOLUTION_NATIVE, DEFAULT_WINDOW_SIZE);

        // Cube
        mLockCubeWhileSolving = config.getValueBool(R.CONFIG_KEY_LOCK_CUBE_WHILE_SOLVING, GLConfig.DEFAULT_LOCK_CUBE_WHILE_SOLVING);

        // Camera
        final float cube_scale_percent = config.getValueFloat(R.CONFIG_KEY_CUBE_DRAW_SCALE_PERCENT, -1);
        if (cube_scale_percent >= 0 && cube_scale_percent <= 100) {
            setCubeDrawScalePercent(cube_scale_percent);
        }

        mFreeCamera = config.getValueBool(R.CONFIG_KEY_FREE_CAMERA, GLConfig.DEFAULT_FREE_CAMERA);

        // Sounds
        mSoundEnabled = config.getValueBool(R.CONFIG_KEY_SOUND, mSoundEnabled);
        mPolyRhythmEnabled = config.getValueBool(R.CONFIG_KEY_POLY_RHYTHM, mPolyRhythmEnabled);

        // Simulation Environment
        final float anim_speed = config.getValueFloat(R.CONFIG_KEY_ANIMATION_SPEED, -1f);
        if (anim_speed >= 0 && anim_speed <= 100)
            cubeGL.setMoveQuarterSpeedPercent(anim_speed);

        final String anim_interp = config.getValueString(R.CONFIG_KEY_ANIMATION_SPEED, "");
        if (Format.notEmpty(anim_interp)) {
            final InterpolatorInfo ii = InterpolatorInfo.fromKey(anim_interp);
            if (ii != null) {
                cubeGL.setMoveGlInterpolatorOverride(ii.interpolator);
            }
        }
    }


    public boolean isFullscreen() {
        return mFullscreen;
    }

    public boolean supportsSurfaceLocationSetter() {
        return true;
    }

    public boolean supportsSurfaceSizeSetter() {
        return false;
    }

    public @NotNull Dimension getInitialSurfaceDimensions() {
        return mInitialWindowSize;
    }


    /* Main Utility */

    @NotNull
    public final Cube getCube() {
        return cubeGL.getCube();
    }

    public final int getN() {
        return cubeGL.getCube().n();
    }

    public final boolean isCubeLocked() {
        return cubeGL.getCube().isLocked();
    }

//    public final boolean isCubeLocked() {
//        return mSolving;
//    }

    public final void resetSimulation() {
        cubeGL.resetMoveQuarterDurationMs();
        cubeGL.resetMoveGlInterpolatorOverride();
    }

    @NotNull
    public final String cubeRepresentation2D() {
        return getCube().representation2D();
    }

    /* CubeGL Listeners */

    @Override
    public void onCubeChanged(@NotNull CubeGL cubeGL, @NotNull Cube old, @NotNull Cube _new) {
        cancelSolve();
        invalidateSolution();
    }

    @Override
    public void onMoveAnimationEnabledChanged(@NotNull CubeGL cubeGL, boolean moveAnimationEnabled) {

    }

    @Override
    public void onMoveGlConfigurerChanged(@NotNull CubeGL cubeGL, @Nullable Consumer<MoveGL> old, @Nullable Consumer<MoveGL> _new) {

    }

    @Override
    public void onMoveGlInterpolatorOverrideChanged(@NotNull CubeGL cubeGL, @Nullable Interpolator old, @Nullable Interpolator _new) {

    }

    @Override
    public void onMoveStarted(@NotNull MoveGL moveGL, boolean resumed) {

    }

    @Override
    public void onMovePaused(@NotNull MoveGL moveGL) {

    }

    @Override
    public void onMoveFinished(@NotNull MoveGL moveGL, Animator.@NotNull Finish how) {

    }

    @Override
    public void onMoveApplied(@NotNull Cube cube, @NotNull Move move, int cubiesAffected, boolean saved) {
        cancelSolve();  // Cancel solving
        invalidateSolution();
    }

    @Override
    public void onCubeLockChanged(@NotNull Cube cube, boolean locked) {

    }


    /* Sounds  ..................................... */

    @NotNull
    private MidiNotePlayer createSoundPlayer() {
        return new MidiNotePlayer(this)
                .setPolyRhythmEnabled(mPolyRhythmEnabled)
                .setAmplitude(0.5f)
                .setAttackTime(0.01f)
                .setSustainTime(0.1f)
                .setSustainLevel(0.5f)
                .setReleaseTime(0.25f);
    }

    @NotNull
    private MidiNotePlayer getSoundPlayer() {
        MidiNotePlayer player = mSoundPlayer;
        if (player == null) {
            synchronized (this) {
                player = mSoundPlayer;
                if (player == null) {
                    player = createSoundPlayer();
                    mSoundPlayer = player;
                }
            }
        }

        // Fast Configurations
        player.setPolyRhythmEnabled(mPolyRhythmEnabled);
        return player;
    }

    private void playMidiNote(float midiNote) {
        if (!isSoundEnabled())
            return;

        getSoundPlayer().play(midiNote);
    }


    protected void onSoundEnabledChanged(boolean soundEnabled) {

    }

    public final boolean isSoundEnabled() {
        return mSoundEnabled;
    }

    public final void setSoundEnabled(boolean soundEnabled) {
        if (mSoundEnabled == soundEnabled)
            return;

        mSoundEnabled = soundEnabled;
        onSoundEnabledChanged(soundEnabled);
    }

    public final void toggleSoundEnabled() {
        setSoundEnabled(!isSoundEnabled());
    }


    protected void onPolyRhythmEnabledChanged(boolean polyRhythmEnabled) {

    }

    public final boolean isPolyRhythmEnabled() {
        return mPolyRhythmEnabled;
    }

    public final void setPolyRhythmEnabled(boolean polyRhythmEnabled) {
        if (mPolyRhythmEnabled == polyRhythmEnabled)
            return;

        mPolyRhythmEnabled = polyRhythmEnabled;
        onPolyRhythmEnabledChanged(polyRhythmEnabled);
    }

    public final void togglePolyRhythmEnabled() {
        setPolyRhythmEnabled(!isPolyRhythmEnabled());
    }





    @Nullable
    public String getStatusText() {
        return GLConfig.getStatusText(width, height, frameRate, frameCount, mSolving, mCurSolution, "S");
    }

    @Nullable
    public String getSecStatusText() {
        return GLConfig.getSecStatusText(cubeGL.getMoveQuarterSpeedPercent());
    }

    @Nullable
    public String getCubeStateText() {
        return GLConfig.getCubeStateText(getCube().n(), isCubeLocked());
    }



    /* Setup and Drawing */

    @Override
    public void settings() {

        if (mFullscreen) {
            fullScreen(P3D);
        } else {
            size(mInitialWindowSize.width, mInitialWindowSize.height, P3D);
        }

        PJOGL.setIcon(R.APP_ICON.toString());       // app icon
    }


    @Override
    public void setup() {
        println("-> UI Thread: " + Thread.currentThread().getName());

        _w = width;
        _h = height;
//        colorMode(ARGB);

        // Surface
        surface.setTitle(R.APP_TITLE);
        surface.setResizable(true);
        if (GLConfig.DEFAULT_WINDOW_IN_SCREEN_CENTER) {
            setSurfaceLocationCenter(false);
        }

        frameRate(GLConfig.FRAME_RATE);

        // Leviation Animator and Camera
        resetLevitationAnimator();
        setFreeCameraInternal(mFreeCamera);

        // Background Image
        if (R.IMAGE_BG != null) {
            bgImage = loadImage(R.IMAGE_BG.toString());
        }

        // Fonts
        pdSans = createFont(R.FONT_PD_SANS_REGULAR.toString(), 20);
        pdSansMedium = createFont(R.FONT_PD_SANS_MEDIUM.toString(), 20);
        textFont(pdSans);       // Default
    }

    public float getTextSize(float size) {
        return GLConfig.getTextSize(width, height, size);
    }

    protected void onResized(int w, int h) {
        if (bgImage != null) {
            bgImage.resize(w, h);
        }

        considerReCreateCam();
        resetLevitationAnimator();
    }

    public void preDraw() {
        if (_w != width || _h != height) {
            _w = width;
            _h = height;
            onResized(width, height);
        }

        /* Handle Keys [Continuous] */
        if (keyPressed && mKeyEvent != null) {
            onContinuousKeyPressed(mKeyEvent);
        }
    }

    @Override
    public void draw() {
        preDraw();

        // todo bg image and lights
        if (bgImage != null) {
            bgImage.resize(width, height);
            background(bgImage);
        } else {
            background(GLConfig.BG.getRGB());
        }

        /* Cube */
        pushMatrix();
        final Point3DF o = cubeOrigin(width, height, getN());
        final float curLevitation = updateLevitation();

        translate(o.x, o.y + curLevitation, o.z);
        if (!mFreeCamera) {
            camera(o.x * 0.53f /* sin(radians(32)) */, o.y * -0.95f /* sin(radians(-72)) */, o.y * 1.6f /* 1/tan(radians(32)) */, 0, -curLevitation, 0, 0, 1, 0);
        }

        if (GLConfig.CUBE_DRAW_AXES) {
            pushStyle();
            stroke(GLConfig.COLOR_AXIS.getRGB());
            strokeWeight(2);

            final float plen = Math.max(width, height);
            final float nlen = GLConfig.CUBE_DRAW_ONLY_POSITIVE_AXES ? 0 : plen;

            line(-nlen, 0, 0, plen, 0, 0);         // X
            line(0, nlen, 0, 0, -plen, 0);       // Y (invert)
            line(0, 0, -nlen, 0, 0, plen);        // Z
            popStyle();
        }

        final float scale = cubeScale(width, height, getN()) * getCubeDrawScale();
        scale(scale * (GLConfig.CUBE_INVERT_X ? -1 : 1), scale * (GLConfig.CUBE_INVERT_Y ? -1 : 1), scale * (GLConfig.CUBE_INVERT_Z ? -1 : 1));
        cubeGL.draw(this);
        popMatrix();

        /* HUD */
        if (mPeasyCam != null) {
            mPeasyCam.beginHUD();
        }

        drawHUD();

        if (mPeasyCam != null) {
            mPeasyCam.endHUD();
        }

        postDraw();
    }


    protected void drawHUD() {
        final float h_offset = width * 0.009f;
        final float v_offset = height / 96f;

        final float cameraModeTextSize = getTextSize(GLConfig.CAMERA_MODE_TEXT_SIZE);
        final float cubeLockedTextSize = getTextSize(GLConfig.CUBE_STATE_TEXT_SIZE);

        // Camera
        if (GLConfig.SHOW_CAMERA_MODE) {
            pushStyle();
            fill(GLConfig.FG_CAMERA_MODE.getRGB());
            textFont(pdSansMedium, cameraModeTextSize);
            textAlign(LEFT, TOP);
            text(GLConfig.getCameraModeText(mFreeCamera, "V"), h_offset, v_offset);
            popStyle();
        }

        // Cube State
        if (GLConfig.SHOW_CUBE_STATE) {
            final String stateText = getCubeStateText();
            if (Format.notEmpty(stateText)) {
                pushStyle();
                fill(GLConfig.FG_CUBE_STATE.getRGB());
                textFont(pdSans, cubeLockedTextSize);
                textAlign(RIGHT, TOP);
                text(stateText, width - h_offset, v_offset);
                popStyle();
            }
        }

        // Controls
        if (areKeyBindingsShown()) {
            pushStyle();
            final float titleTextSize = getTextSize(GLConfig.CONTROLS_DES_TITLE_TEXT_SIZE);
            final float titleY = cameraModeTextSize + v_offset * 4;

            fill(GLConfig.FG_CONTROLS_DES_TITLE.getRGB());
            textFont(pdSansMedium, titleTextSize);
            textAlign(LEFT, TOP);
            text("Move Controls", h_offset * 3, titleY);
            textAlign(RIGHT, TOP);
            text("View Controls", width - h_offset * 3, titleY);

            fill(GLConfig.FG_CONTROLS_DES.getRGB());
            textFont(pdSans, getTextSize(GLConfig.CONTROLS_DES_TEXT_SIZE));

            final float desY = titleY + titleTextSize + v_offset * 1.5f;
            textAlign(LEFT, TOP);
            text(R.DES_CONTROLS_MOVES + "\n\n" + R.DES_CONTROLS_SOLVE, h_offset, desY);

            textAlign(RIGHT, TOP);
            text(R.DES_CONTROLS_CUBE_CAMERA, width - h_offset, desY);
            popStyle();
        }


        final float statusTextSize = getTextSize(GLConfig.STATUS_TEXT_SIZE);

        // Status bar
        if (GLConfig.SHOW_STATUS) {
            final String statusText = getStatusText();
            if (Format.notEmpty(statusText)) {
                pushStyle();
                fill(GLConfig.FG_STATUS_BAR.getRGB());
                textSize(statusTextSize);
                textAlign(LEFT, BOTTOM);
                text(statusText, h_offset, height - v_offset);
                popStyle();
            }
        }

        // Last Move
        if (GLConfig.SHOW_LAST_MOVE) {
            final Move lastMove = cubeGL.getCube().peekLastMove();
            if (lastMove != null) {
                pushStyle();
                fill(GLConfig.FG_LAST_MOVE.getRGB());
                textSize(getTextSize(GLConfig.LAST_MOVE_TEXT_SIZE));
                textAlign(LEFT, BOTTOM);
                text(GLConfig.getLastMoveText(lastMove, "Ctrl-Z"), h_offset, height - statusTextSize - v_offset * 2);
                popStyle();
            }
        }

        // Secondary Status
        if (GLConfig.SHOW_SEC_STATUS) {
            final String secStatus = getSecStatusText();
            if (secStatus != null) {
                pushStyle();
                fill(GLConfig.FG_SEC_STATUS.getRGB());
                textSize(getTextSize(GLConfig.SEC_STATUS_TEXT_SIZE));
                textAlign(RIGHT, BOTTOM);
                text(secStatus, width - h_offset, height - statusTextSize - v_offset * 2);
                popStyle();
            }
        }

        // Cur Move (MIDDLE)
        if (GLConfig.SHOW_CUR_MOVE) {
            final Move curMove = cubeGL.getYoungestRunningMove();
            if (curMove != null) {
                pushStyle();
                fill(GLConfig.FG_CUR_MOVE.getRGB());
                textSize(getTextSize(GLConfig.CUR_MOVE_TEXT_SIZE));
                textAlign(CENTER, BOTTOM);
                text(curMove.toString(), width / 2f, height - statusTextSize - v_offset * 2);
                popStyle();
            }
        }
    }


    protected void postDraw() {
    }


    public final boolean setSurfaceLocation(int x, int y, boolean verbose) {
        if (supportsSurfaceLocationSetter()) {
            surface.setLocation(x, y);
            return true;
        }

        if (verbose) {
            U.printerrln(R.SHELL_WINDOW + String.format("Current 3D renderer does not support changing window position on screen!! Renderer: %s", sketchRenderer()));
        }

        return false;
    }

    public final boolean setSurfaceSize(int w, int h, boolean verbose) {
        if (supportsSurfaceSizeSetter()) {
            surface.setSize(w, h);
            return true;
        }

        if (verbose) {
            U.printerrln(R.SHELL_WINDOW + String.format("Current 3D renderer does not support changing window size!! Renderer: %s", sketchRenderer()));
        }

        return false;
    }

    public final boolean setSurfaceLocationCenter(boolean verbose) {
        return setSurfaceLocation((U.SCREEN_RESOLUTION_NATIVE.width - width) / 2, (U.SCREEN_RESOLUTION_NATIVE.height - height) / 2, verbose);
    }

    public final void resetSurfaceSize(boolean verbose) {
        final Dimension def = getInitialSurfaceDimensions();
        setSurfaceSize(def.width, def.height, verbose);
    }

    public final void snapshot() {
        final Cube cube = cubeGL.getCube();

        String file_name = String.format("cube-%dx%d_moves-%d_distance-%d_timestamp-%d.png", cube.n, cube.n, cube.getAllAppliedMovesCount(), cube.cacheHeuristic(), System.currentTimeMillis());

//        file_name = Format.replaceAllWhiteSpaces(file_name.toLowerCase(), "_");

        saveFrame(file_name);
        println(R.SHELL_ROOT + "Frame saved to file: " + file_name);
    }



//    /* Cube Moves */
//
//    public void applyMove(@NotNull Move move, boolean saveInStack, boolean now) {
//        if (isCubeLocked())
//            return;
//        cubeGL.applyMove(move, saveInStack, now);
//    }
//
//    public void applyMove(@NotNull Move move, boolean saveInStack) {
//        if (isCubeLocked())
//            return;
//        cubeGL.applyMove(move, saveInStack);
//    }
//
//    public void applyMove(@NotNull Move move) {
//        if (isCubeLocked())
//            return;
//        cubeGL.applyMove(move);
//    }
//
//    public boolean undoLastMove(boolean now) {
//        if (isCubeLocked())
//            return false;
//        return cubeGL.undoLastMove(now);
//    }
//
//    public boolean undoLastMove() {
//        if (isCubeLocked())
//            return false;
//        return cubeGL.undoLastMove();
//    }
//
//    public void applySequence(@NotNull List<Move> sequence, boolean saveInStack, boolean now) {
//        if (isCubeLocked())
//            return;
//        cubeGL.applySequence(sequence, saveInStack, now);
//    }
//
//    public void applySequence(@NotNull List<Move> sequence, boolean saveInStack) {
//        if (isCubeLocked())
//            return;
//        cubeGL.applySequence(sequence, saveInStack);
//    }
//
//    public void applySequence(@NotNull List<Move> sequence) {
//        if (isCubeLocked())
//            return;
//        cubeGL.applySequence(sequence);
//    }
//
//    public void scramble(int moves, boolean saveInStack, boolean now) {
//        if (isCubeLocked())
//            return;
//        cubeGL.scramble(moves, saveInStack, now);
//    }
//
//    public void scramble(int moves, boolean saveInStack) {
//        if (isCubeLocked())
//            return;
//        cubeGL.scramble(moves, saveInStack);
//    }
//
//    public void scramble(int moves) {
//        if (isCubeLocked())
//            return;
//        cubeGL.scramble(moves);
//    }
//
//    public void scramble() {
//        if (isCubeLocked())
//            return;
//        cubeGL.scramble();
//    }
//
//    public void rotateCubeX(int quarters) {
//        if (isCubeLocked())
//            return;
//        cubeGL.rotateX(quarters, true);
//    }
//
//    public void rotateCubeY(int quarters) {
//        if (isCubeLocked())
//            return;
//        cubeGL.rotateY(quarters, true);
//    }
//
//    public void rotateCubeZ(int quarters) {
//        if (isCubeLocked())
//            return;
//        cubeGL.rotateZ(quarters, true);
//    }
//
//    public void rotateCubeUp() {
//        rotateCubeX(-1);
//    }
//
//    public void rotateCubeDown() {
//        rotateCubeX(1);
//    }
//
//    public void rotateCubeLeft() {
//        rotateCubeY(-1);
//    }
//
//    public void rotateCubeRight() {
//        rotateCubeY(1);
//    }
//
//    public void rollCubeLeft() {
//        rotateCubeZ(1);
//    }
//
//    public void rollCubeRight() {
//        rotateCubeZ(-1);
//    }
//
//    public void finishAllMoves(boolean cancel) {
//        if (isCubeLocked())
//            return;
//        cubeGL.finishAllMoves(cancel);
//    }
//
//    public boolean setMoveGlInterpolator(@Nullable Interpolator interpolator) {
//        return cubeGL.setMoveGlInterpolatorOverride(interpolator);
//    }


    /* Solve */

    public boolean isLockCubeWhileSolvingEnabled() {
        return mLockCubeWhileSolving;
    }

    public void setLockCubeWhileSolving(boolean lockCubeWhileSolving) {
        mLockCubeWhileSolving = lockCubeWhileSolving;
    }

    public final boolean isSolving() {
        return mSolving;
    }

    private void setSolvingFlag(boolean solving) {
        mSolving = solving;

        // Lock cube accordingly
        cubeGL.getCube().setLocked(solving && isLockCubeWhileSolvingEnabled());
    }


    private void invalidateSolution() {
        mCurSolution = null;
    }

    public final void cancelSolve() {
        final Canceller c = mSolveCanceller;
        if (c != null) {
            c.cancel(true);
        }
    }

    public final void solve() {
        if (isSolving())
            return;

//        if (getN() != 3) {
//            Util.w(R.SHELL_SOLVER, "Solver only works for 3*3 cube");
//            return;
//        }

        // If Has a solution, apply it
        final Solver.Solution curSolution = mCurSolution;
        invalidateSolution();
        if (!(curSolution == null || curSolution.isEmpty()) && curSolution.n == getN()) {
            cubeGL.applySequence(curSolution.moves);
            return;
        }

        // Finish all pending moves, and start solving
        cubeGL.finishAllMoves(false);

        setSolvingFlag(true);       // turn solving-flag ON

        cancelSolve();
        final Canceller canceller = Canceller.basic();
        mSolveCanceller = canceller;

        Async.execute(() -> {
            Solver.Solution sol = null;
            Exception exc = null;
            try {
                sol = doSolveWorker(cubeGL.getCube(), true, canceller);
            } catch (Exception exc_) {
                exc = exc_;
            }

            onSolveFinishedWorker(sol, exc);
        });
    }


    // Worker thread
    @NotNull
    private Solver.Solution doSolveWorker(@NotNull Cube cube, boolean createCopy, @Nullable CancellationProvider c) throws Solver.SolveException, CancellationException {
        if (createCopy) {
            cube = new Cube(cube);      // Just copy to be safe
        }

        final int n = cube.n();
        U.v(R.SHELL_SOLVER, String.format("Solving %dx%dx%d state: %s", n, n, n, cube.representation2D()));

        return Solver.solve(cube, false, c);
    }


    // Worker thread
    private void onSolveFinishedWorker(@Nullable Solver.Solution solution, @Nullable Exception exc) {
        enqueueTask(() -> onSolveFinishedMain(solution, exc));      // just enqueue for now
    }

    // UI-thread
    private void onSolveFinishedMain(@Nullable Solver.Solution solution, @Nullable Exception exc) {
        if (solution != null) {
            onSolveSuccess(solution);
        } else if (exc instanceof CancellationException cancellationException) {
            onSolveCancelled(cancellationException);
        } else {
            onSolveError(exc);
        }

        setSolvingFlag(false);       // turn solving-flag OFF
    }


    private void onSolveCancelled(@Nullable CancellationException exc) {
        invalidateSolution();
    }

    private void onSolveError(@Nullable Exception exc) {
        invalidateSolution();

        final String msg;

        if (exc instanceof Solver.SolveException) {
            msg = exc.getMessage();
        } else if (exc != null) {
            msg = exc.toString();
        } else {
            msg = "Unknown Error";
        }

        U.e(R.SHELL_SOLVER, "Solver Failed: " + msg);
    }

    private void onSolveSuccess(@NotNull Solver.Solution solution) {
        mCurSolution = solution;

        if (solution.isEmpty()) {
            U.v(R.SHELL_SOLVER, "Cube is already solved!");
        } else {
            StringBuilder sb = new StringBuilder()
                    .append(" Cube Solved!")
                    .append("\n............ Solution ............")
                    .append("\n\t -> Dimensions: ").append(solution.n).append('x').append(solution.n).append('x').append(solution.n)
                    .append("\n\t -> Moves: ").append(solution.moveCount());

            final Long msTaken = solution.getMsTaken();
            if (msTaken != null && msTaken > 0) {
                sb.append("\n\t -> Time Taken: ").append(msTaken).append(" ms");
            }

            sb.append("\n\t -> Sequence: ").append(solution.getSequence());
            sb.append("\nPress S or enter command <solve> to apply the solution...");
            sb.append("\n...................................\n");
            U.v(R.SHELL_SOLVER, sb.toString());
        }
    }



    /* Events and Bindings */


    /**
     * Enqueue a custom task to be executed on the UI thread
     * */
    public final void enqueueTask(@NotNull Runnable task) {
        postEvent(new KeyEvent(task, millis(), ACTION_EXECUTE_RUNNABLE, 0, (char) 0, 0, false));
    }

    public final void enqueueTasks(@Nullable Collection<? extends Runnable> tasks) {
        final Runnable chain = U.chainRunnables(tasks);     // Merge tasks to a single task

        if (chain != null) {
            enqueueTask(chain);
        }
    }

    @Override
    protected void handleKeyEvent(KeyEvent event) {
        super.handleKeyEvent(event);

        // Handle Custom Events
        if (event.getAction() == ACTION_EXECUTE_RUNNABLE && (event.getNative() instanceof Runnable task)) {
            task.run();
        }
    }

//    private void onSpacePressed(@NotNull KeyEvent event) {
//        cubeGL.scramble();
//    }

    @Override
    public void keyPressed(KeyEvent event) {
        super.keyPressed(event);
        mKeyEvent = event;

        // Handle Discrete Controls
        for (Control control: Control.getValuesShared()) {
            if (control.continuousKeyEvent)
                continue;

            if (control.handleKeyEvent(this, event))
                break;
        }

//        final char key = event.getKey();
//        final int keyCode = event.getKeyCode();
//        println("KeyEvent-> key: " + key + ", code: " + keyCode + ", ctrl: " + event.isControlDown() + ", alt: " + event.isAltDown() + ", shift: " + event.isShiftDown());

//        switch (keyCode) {
//            /* Cube Rotations */
//            case java.awt.event.KeyEvent.VK_UP -> rotateCubeUp();
//            case java.awt.event.KeyEvent.VK_DOWN -> rotateCubeDown();
//            case java.awt.event.KeyEvent.VK_LEFT -> {
//                if (shouldRollOverRotatingCube(event)) {
//                    rollCubeLeft();
//                } else {
//                    rotateCubeLeft();
//                }
//            }
//            case java.awt.event.KeyEvent.VK_RIGHT -> {
//                if (shouldRollOverRotatingCube(event)) {
//                    rollCubeRight();
//                } else {
//                    rotateCubeRight();
//                }
//            }
//
//            /* Cube Moves */
//            case java.awt.event.KeyEvent.VK_U -> applyMove(createMove(Axis.Y, event));
//            case java.awt.event.KeyEvent.VK_R -> applyMove(createMove(Axis.X, event));
//            case java.awt.event.KeyEvent.VK_F -> applyMove(createMove(Axis.Z, event));
//            case java.awt.event.KeyEvent.VK_D -> applyMove(createMove(Axis.Y_N, event));
//            case java.awt.event.KeyEvent.VK_L -> applyMove(createMove(Axis.X_N, event));
//            case java.awt.event.KeyEvent.VK_B -> applyMove(createMove(Axis.Z_N, event));
//
//            case java.awt.event.KeyEvent.VK_A -> toggleAnimateMoves();
//            case java.awt.event.KeyEvent.VK_X -> finishAllMoves(event.isShiftDown());
//            case java.awt.event.KeyEvent.VK_Z -> {
//                if (event.isControlDown()) {
//                    undoLastMove();
//                }
//            }
//
//
//            /* Scramble, Solve and Reset */
//            case java.awt.event.KeyEvent.VK_SPACE -> onSpacePressed(event);
//            case java.awt.event.KeyEvent.VK_S -> solve();
//            case java.awt.event.KeyEvent.VK_Q -> {
//                if (event.isShiftDown()) {
//                    resetCube();
//                } else {
//                    resetCubeDrawScale();
//                }
//            }
//
//            /* Others */
//            case java.awt.event.KeyEvent.VK_V -> toggleFreeCamera();
//            case java.awt.event.KeyEvent.VK_C -> toggleShowControls();
//            case java.awt.event.KeyEvent.VK_N -> incrementN();
//            case java.awt.event.KeyEvent.VK_M -> decrementN();
////            case java.awt.event.KeyEvent.VK_DEAD_CEDILLA, java.awt.event.KeyEvent.VK_PLUS -> incCubeZoom();
////            case java.awt.event.KeyEvent.VK_DEAD_OGONEK, java.awt.event.KeyEvent.VK_MINUS -> decCubeZoom();
//        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        super.keyReleased(event);
        if (mKeyEvent != null && mKeyEvent.getKeyCode() == event.getKeyCode()) {
            mKeyEvent = null;
        }
    }

    public void onContinuousKeyPressed(@Nullable KeyEvent event) {
        if (event == null)
            return;

        // Handle Continuous Controls
        for (Control control: Control.getValuesShared()) {
            if (!control.continuousKeyEvent)
                continue;

            if (control.handleKeyEvent(this, event))
                break;
        }


//        final int keyCode = event.getKeyCode();
//
//        switch (keyCode) {
//            case java.awt.event.KeyEvent.VK_DEAD_CEDILLA, java.awt.event.KeyEvent.VK_PLUS -> {
//                if (event.isShiftDown()) {
//                    incMoveSpeed(true);
//                } else {
//                    incCubeZoom();
//                }
//            }
//
//            case java.awt.event.KeyEvent.VK_DEAD_OGONEK, java.awt.event.KeyEvent.VK_MINUS -> {
//                if (event.isShiftDown()) {
//                    decMoveSpeed(true);
//                } else {
//                    decCubeZoom();
//                }
//            }
//        }
    }

    /* Levitation */

    private void resetLevitationAnimator() {
        if (mLevitationAnimator != null) {
            mLevitationAnimator.finish(true);
            mLevitationAnimator = null;
        }

        mLevitationAnimator = GLConfig.createLevitationAnimator(height);
        if (mLevitationAnimator != null) {
            mLevitationAnimator.start();
        }
    }

    private float updateLevitation() {
        final float l;
        if (mLevitationAnimator != null) {
            l = mLevitationAnimator.updateAndGetCurrentValue();
            if (mLevitationAnimator.isFinished()) {
                mLevitationAnimator = mLevitationAnimator.reverse();
                mLevitationAnimator.start();
            }
        } else {
            l = 0;
        }

        return l;
    }


    /* HUD and Controls  .......................................................... */

    protected void onHudEnabledChanged(boolean hudEnabled) {

    }

    public final boolean isHudEnabled() {
        return mHudEnabled;
    }

    public final void setHudEnabled(boolean hudEnabled) {
        if (mHudEnabled == hudEnabled)
            return;

        mHudEnabled = hudEnabled;
        onHudEnabledChanged(hudEnabled);
    }

    public final void toggleHudEnabled() {
        setHudEnabled(!isHudEnabled());
    }


    protected void onShowKeyBindingsChanged(boolean showKeyBindings) {

    }

    public final boolean areKeyBindingsShown() {
        return mShowKeyBindings;
    }

    public final void setShowKeyBindings(boolean showKeyBindings) {
        if (mShowKeyBindings == showKeyBindings)
            return;

        mShowKeyBindings = showKeyBindings;
        onShowKeyBindingsChanged(showKeyBindings);
    }

    public final void toggleShowKeyBindings() {
        setShowKeyBindings(!areKeyBindingsShown());
    }


    /* Cube Draw Scale ........................................................ */

    protected void onCubeDrawScaleChanged(float prevDrawScale, float newDrawScale) {

    }

    public final float getCubeDrawScale() {
        return mCubeDrawScale;
//        return mFreeCam? 1 : cubeScale;
    }

    /**
     * @return new cube draw scale, which might be different from the drawScale argument
     * */
    public float setCubeDrawScale(float drawScale) {
//        if (mFreeCam)
//            return;

        final float cur_scale = mCubeDrawScale;
        final float new_scale = constrain(drawScale, GLConfig.CUBE_DRAW_SCALE_MIN, GLConfig.CUBE_DRAW_SCALE_MAX);

        if (cur_scale == new_scale)
            return cur_scale;

        mCubeDrawScale = new_scale;
        onCubeDrawScaleChanged(cur_scale, new_scale);
        return new_scale;
    }

    public final float getCubeDrawScalePercentage() {
        return GLConfig.cubeDrawScaleToPercent(getCubeDrawScale());
    }

    public void setCubeDrawScalePercent(float drawScalePercent) {
        setCubeDrawScale(GLConfig.percentToCubeDrawScale(drawScalePercent));
    }

    public void stepCubeDrawScale(boolean continuous, boolean increment) {
        setCubeDrawScalePercent(GLConfig.stepCubeDrawScalePercent(getCubeDrawScalePercentage(), continuous, increment));
    }

//    public float stepCubeScale(boolean continuous, boolean increment) {
//        return setCubeScale(GLConfig.stepCubeScale(cubeScale, continuous, increment));
//    }


    public void resetCubeDrawScale() {
        setCubeDrawScale(GLConfig.CUBE_DRAW_SCALE_DEFAULT);
    }


    /* Camera ........................................................ */

    public boolean isFreeCameraEnabled() {
        return mFreeCamera;
    }

    private void setFreeCameraInternal(boolean freeCamera) {
        mFreeCamera = freeCamera;

        if (freeCamera) {
            considerReCreateCam();
        } else if (mPeasyCam != null) {
            mPeasyCam.setActive(false);     // Do not reset or nullify
        }
    }

    public void serFreeCamera(boolean freeCamera) {
        if (mFreeCamera == freeCamera)
            return;
        setFreeCameraInternal(freeCamera);
    }

    public void toggleFreeCamera() {
        setFreeCameraInternal(!mFreeCamera);
    }


    @NotNull
    private PeasyCam createCam(@Nullable float[] rotations) {
        final Point3DF o = cubeOrigin(width, height, getN());
        final PeasyCam cam = new PeasyCam(this, o.x, o.y, o.z, o.y / tan(radians(26)));

        if (rotations == null) {
            rotations = GLConfig.INITIAL_CAM_ROTATIONS;
        }

        cam.setRotations(rotations[0], rotations[1], rotations[2]);
        return cam;
    }

    @NotNull
    private PeasyCam createCam() {
        return createCam(null);     // TODO: initial rotations
    }

    @Nullable
    public final PeasyCam getCamera() {
        return mPeasyCam;
    }

    public final boolean isCameraSupported() {
        return getCamera() != null;
    }

    private void considerReCreateCam() {
        if (!mFreeCamera)
            return;

        float[] rotations = null;
        if (mPeasyCam != null) {
            mPeasyCam.setActive(false);
            rotations = mPeasyCam.getRotations();
        }

        mPeasyCam = createCam(rotations);
    }


    public final boolean resetCamera(boolean resetDrawScale, boolean animate) {
        if (resetDrawScale) {
            resetCubeDrawScale();
        }

        final PeasyCam cam = getCamera();
        if (cam == null)
            return false;

        // TODO: reset to initial rotations
        cam.reset(Math.max(animate? GLConfig.CAMERA_EXPLICIT_RESET_ANIMATION_MILLS: 0, 1));
        return true;
    }

    public final boolean rotateCameraXTo(float rotationX, boolean animate) {
        final PeasyCam cam = getCamera();
        if (cam == null)
            return false;

        U.rotateXTo(cam, rotationX, animate? GLConfig.CAMERA_ROTATIONS_ANIMATION_MILLS: 0);
        return true;
    }

    public final boolean rotateCameraYTo(float rotationY, boolean animate) {
        final PeasyCam cam = getCamera();
        if (cam == null)
            return false;

        U.rotateYTo(cam, rotationY, animate? GLConfig.CAMERA_ROTATIONS_ANIMATION_MILLS: 0);
        return true;
    }

    public final boolean rotateCameraZTo(float rotationZ, boolean animate) {
        final PeasyCam cam = getCamera();
        if (cam == null)
            return false;

        U.rotateZTo(cam, rotationZ, animate? GLConfig.CAMERA_ROTATIONS_ANIMATION_MILLS: 0);
        return true;
    }

    public final boolean rotateCameraXBy(float rotationXBy, boolean animate) {
        final PeasyCam cam = getCamera();
        if (cam == null)
            return false;

        U.rotateXBy(cam, rotationXBy, animate? GLConfig.CAMERA_ROTATIONS_ANIMATION_MILLS: 0);
        return true;
    }

    public final boolean rotateCameraXByUnit(boolean up, boolean animate) {
        return rotateCameraXBy((up? 1: -1) * HALF_PI, animate);
    }


    public final boolean rotateCameraYBy(float rotationYBy, boolean animate) {
        final PeasyCam cam = getCamera();
        if (cam == null)
            return false;

        U.rotateYBy(cam, rotationYBy, animate? GLConfig.CAMERA_ROTATIONS_ANIMATION_MILLS: 0);
        return true;
    }

    public final boolean rotateCameraYByUnit(boolean left, boolean animate) {
        return rotateCameraYBy((left? 1: -1) * HALF_PI, animate);
    }

    public final boolean rotateCameraZBy(float rotationZBy, boolean animate) {
        final PeasyCam cam = getCamera();
        if (cam == null)
            return false;

        U.rotateZBy(cam, rotationZBy, animate? GLConfig.CAMERA_ROTATIONS_ANIMATION_MILLS: 0);
        return true;
    }

    public final boolean rotateCameraZByUnit(boolean left, boolean animate) {
        return rotateCameraZBy((left? 1: -1) * HALF_PI, animate);
    }

    /* Animation */

    public final InterpolatorInfo getCurrentMoveGlInterpolatorInfo(@Nullable InterpolatorInfo defaultValue) {
        Interpolator interp = cubeGL.getMoveGlInterpolatorOverride();
        if (interp == null) {
            interp = GLConfig.DEFAULT_MOVE_ANIMATION_INTERPOLATOR;
        }

        return InterpolatorInfo.fromInterpolator(interp, defaultValue);
    }

    public final void setMoveGlInterpolator(@Nullable Interpolator interpolator) {
        cubeGL.setMoveGlInterpolatorOverride(interpolator);
    }

    /**
     * Cycles available MoveGL animation interpolators
     *
     * @return new MoveGL animation interpolator set by this call
     * */
    @NotNull
    public final InterpolatorInfo setNextMoveGlInterpolator() {
        final InterpolatorInfo curIi = getCurrentMoveGlInterpolatorInfo(InterpolatorInfo.DEFAULT);

        InterpolatorInfo nextIi = U.cycleEnum(InterpolatorInfo.class, curIi);
        if (nextIi == InterpolatorInfo.DEFAULT) {       // do not want default
            nextIi = U.cycleEnum(InterpolatorInfo.class, nextIi);
        }

        setMoveGlInterpolator(nextIi.interpolator);
        return nextIi;
    }



    public static void init(String[] args) {
        //        R.createShellInstructionsReadme();
    }

    public static void main(String[] args) {
        init(args);

        final CubePUi3D app = new CubePUi3D();
        PApplet.runSketch(concat(new String[]{app.getClass().getName()}, args), app);

//        println(R.SHELL_INSTRUCTIONS);
//        Scanner sc;
//        boolean running = true;
//
//        do {
//            sc = new Scanner(System.in);
//            print(R.SHELL_ROOT);
//
//            final String in = sc.nextLine();
//            if (in.isEmpty())
//                continue;
//
//            if (in.startsWith("n")) {
//                final String rest = in.substring(1).strip();
//                final Runnable usagePrinter = () -> U.v("", "Usage: n <dimension>\nDimension should be in range [2, " + CubeI.DEFAULT_MAX_N + "]");
//
//                if (rest.isEmpty()) {
//                    usagePrinter.run();
//                    continue;
//                }
//
//                try {
//                    final int n = Integer.parseInt(rest);
//                    if (n < 2 || n > CubeI.DEFAULT_MAX_N) {
//                        U.e(R.SHELL_DIMENSION, "Cube dimension must be >= 2 and <= " + CubeI.DEFAULT_MAX_N);
//                        usagePrinter.run();
//                    } else {
//                        app.enqueueTask(() -> app.cubeGL.setN(n, true /* use cli flag */));
//                    }
//                } catch (NumberFormatException ignored) {
//                    U.e(R.SHELL_DIMENSION, "Cube dimension must be an integer, command: n [dim]");
//                    usagePrinter.run();
//                }
//            } else if (in.startsWith("scramble")) {
//                final String rest = in.substring(8).strip();
//                final Runnable usagePrinter = () -> U.v("", "Usage: scramble <num_moves>");
//                int n = CubeI.DEFAULT_SCRAMBLE_MOVES;
//
//                if (rest.isEmpty()) {
//                    U.v(R.SHELL_SCRAMBLE, "Using default scramble moves: " + n);
//                    usagePrinter.run();
//                } else {
//                    try {
//                        final int n2 = Integer.parseInt(rest);
//                        if (n2 <= 0) {
//                            U.w(R.SHELL_SCRAMBLE, "Scramble moves must be positive integer, falling back to default scramble moves: " + n);
//                            usagePrinter.run();
//                        } else {
//                            n = n2;
//                        }
//                    } catch (NumberFormatException ignored) {
//                        U.w(R.SHELL_SCRAMBLE, "Scramble moves must be positive integer, falling back to default scramble moves: " + n);
//                        usagePrinter.run();
//                    }
//                }
//
//                app.scramble(n);
//            } else if (in.startsWith("reset")) {
//                if (in.length() > 5 && in.substring(5).endsWith("zoom")) {
//                    app.resetCubeDrawScale();
//                } else {
//                    app.cubeGL.resetCube();
//                }
//            } else if (in.startsWith("finish")) {
//                app.cubeGL.finishAllMoves(in.endsWith("c"));
//            } else if (in.equals("solve")) {
//                app.solve();
//            } else if (in.equals("undo")) {
//                app.cubeGL.undoLastMove();
//            } else if (in.startsWith("speed")) {
//                final String rest = in.substring(5).strip();
//                final Runnable usagePrinter = () -> U.v("", "Usage: speed <option>\nAvailable options\n\t+ : Increase speed\n\t- : Decrease Speed\n\t<percent> : set speed percentage in range [0, 100]");
//
//                if (rest.isEmpty()) {
//                    U.v("", "Current Speed: " + Format.nf001(app.getMoveSpeedPercent()));
//                    usagePrinter.run();
//                    continue;
//                }
//
//                final float newPer;
//
//                if (rest.equals("+")) {
//                    newPer = app.incMoveSpeed(false);
//                } else if (rest.equals("-")) {
//                    newPer = app.decMoveSpeed(false);
//                } else {
//                    try {
//                        final float in_per = Float.parseFloat(rest);
//                        if (in_per < 0 || in_per > 100) {
//                            U.e(R.SHELL_MOVE, "Move speed should be in range [0, 100], given: " + Format.nf001(in_per));
//                            usagePrinter.run();
//                            continue;
//                        }
//
//                        newPer = app.setMoveSpeedPercent(in_per);
//                    } catch (NumberFormatException ignored) {
//                        U.e(R.SHELL_MOVE, "Move speed should be a number in range [0, 100], given: " + rest);
//                        usagePrinter.run();
//                        continue;
//                    }
//                }
//
//                U.v(R.SHELL_MOVE, "Move speed set to " + Format.nf001(newPer) + "%");
//            }
//
//            else if (in.startsWith("intp") || in.startsWith("interp") || in.startsWith("interpolator")) {
//                String key = "";
//                // checks with spaces
//                if (in.startsWith("intp ")) {
//                    key = in.substring(5);
//                } else if (in.startsWith("interp ")) {
//                    key = in.substring(7);
//                } else if (in.startsWith("interpolator "))  {
//                    key = in.substring(13);
//                }
//
//                final Runnable usagePrinter = () -> U.v("", "Usage: interpolator <interpolator_key>\nAvailable Interpolators (key -> name)\n" + InterpolatorInfo.getDisplayInfo());
//                if (key.isEmpty()) {
//                    usagePrinter.run();
//                    continue;
//                }
//
//                final InterpolatorInfo info = InterpolatorInfo.fromKey(key);
//                if (info == null) {
//                    U.e(R.SHELL_MOVE, "Invalid interpolator key <" + key + ">");
//                    usagePrinter.run();
//                    continue;
//                }
//
//                final boolean changed = app.setMoveGlInterpolator(info.interpolator);
//                final String msg = changed? "Move animation interpolator changed to: " + info.displayName: "Move animation interpolator already set to: " + info.displayName;
//                U.w(R.SHELL_MOVE, msg);
//            }
//
//            else if (in.equals("quit") || in.equals("exit")) {
//                running = false;
//            } else {
//                try {
//                    List<Move> moves = Move.parseSequence(in);
//                    if (CollectionUtil.notEmpty(moves)) {
//                        app.applySequence(moves);
//                    }
//                } catch (Move.ParseException e) {
//                    U.e(R.SHELL_MOVE, e.getMessage());
//                }
//            }
//        } while (running);
//
//        app.exit();
    }


}
