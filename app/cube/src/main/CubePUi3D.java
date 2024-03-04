package main;

import camera.CameraState;
import camera.PCamera;
import gl.CubeGL;
import gl.GLConfig;
import gl.MoveGL;
import gl.animation.Animator;
import gl.animation.FloatAnimator;
import gl.animation.interpolator.Interpolator;
import math.geometry.Rotation;
import math.geometry.Vector3D;
import model.Axis;
import math.Point3DF;
import model.cube.Cube;
import model.cube.CubeI;
import model.cubie.Move;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class CubePUi3D extends PApplet implements CubeGL.Listener {


    // Fullscreen does not expand to entire screen with P2D and P3D renderers. See hack in setup() which solves this problem
    private static final boolean DEFAULT_FULLSCREEN = true;

    // Whether the fullscreen should be fully expanded or same as initial window size
    private static final boolean DEFAULT_INITIAL_FULLSCREEN_EXPANDED = false;       // true causes issues, just leave it to false

    @NotNull
    public static Dimension getNativeScreenResolution() {
        return U.SCREEN_RESOLUTION_NATIVE;
    }

    private static final Dimension DEFAULT_INITIAL_WINDOW_SIZE = U.scaleDimension(getNativeScreenResolution(), 0.8f);


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
    private boolean mDrawCubeAxes = GLConfig.DEFAULT_CUBE_DRAW_AXES;

    /* Solver */
    private volatile boolean mSolving;
    @Nullable
    private volatile Canceller mSolveCanceller;
    @Nullable
    private volatile Solver.Solution mCurSolution;
    private volatile boolean mLockCubeWhileSolving = GLConfig.DEFAULT_LOCK_CUBE_WHILE_SOLVING;

    /* Camera */
    private volatile boolean mFreeCamera = GLConfig.DEFAULT_FREE_CAMERA;
    private volatile float mCubeDrawScale = GLConfig.CUBE_DRAW_SCALE_DEFAULT;
    private PCamera mCamera;

    /* Window */
    private boolean mFullscreen = DEFAULT_FULLSCREEN;
    private boolean mInitialFullscreenExpanded = DEFAULT_INITIAL_FULLSCREEN_EXPANDED;
    @NotNull
    private Dimension mInitialWindowSize = DEFAULT_INITIAL_WINDOW_SIZE;

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
        this(n >= 2? new Cube(n): null);
    }

    public CubePUi3D() {
        this(null);
    }


    public final void applyConfig(@NotNull Config config) {
        // Window
        mFullscreen = config.getValueBool(R.CONFIG_KEY_FULLSCREEN, DEFAULT_FULLSCREEN);
        mInitialFullscreenExpanded = config.getValueBool(R.CONFIG_KEY_INITIAL_FULLSCREEN_EXPANDED, DEFAULT_INITIAL_FULLSCREEN_EXPANDED);
        mInitialWindowSize = R.getConfigWindowSize(config, getNativeScreenResolution(), DEFAULT_INITIAL_WINDOW_SIZE);

        // Cube
        mDrawCubeAxes = config.getValueBool(R.CONFIG_KEY_DRAW_CUBE_AXES, GLConfig.DEFAULT_CUBE_DRAW_AXES);
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


    /* Defining methods */

    public boolean isRendered3D() {
        return true;
    }

    public boolean isFullscreen() {
        return mFullscreen;
    }

    public boolean isInitialFullscreenExpanded() {
        return mInitialFullscreenExpanded;
    }

    public boolean shouldDrawAxesInHUD() {
        return true;
    }

    public boolean supportsSurfaceLocationSetter() {
        return true;
    }

    public boolean supportsSurfaceSizeSetter() {
        return true;
    }

    public @NotNull Dimension getInitialWindowedSize() {
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

    @NotNull
    public final String cubeRepresentation2D() {
        return getCube().representation2D();
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
        if (isFullscreen()) {
            fullScreen(P3D);
        } else {
            final Dimension size = getInitialWindowedSize();
            size(size.width, size.height, P3D);
        }

        // app icon
        PJOGL.setIcon(R.APP_ICON.toString());
    }


    @Override
    public void setup() {
        U.println("-> UI Thread: " + Thread.currentThread().getName());
        final List<Runnable> tasks = new LinkedList<>();

        _w = width;
        _h = height;
//        colorMode(ARGB);

        // Surface
        surface.setTitle(R.APP_TITLE);
        surface.setResizable(true);

        // Camera and Levitation Animator
        resetLevitationAnimator();
        syncFreeCameraEnabled(mFreeCamera, false, false);
        tasks.add(() -> resetCamera(false, false));

        // Fullscreen does not expand to entire screen with P2D and P3D renderers. THis hack solves the problem
        if (isFullscreen()) {       // (P2D.equals(sketchRenderer()) || P3D.equals(sketchRenderer()))
            // A Hack to force window to fullscreen with P2D and P3D. only works after a certain delay, so post an event
            tasks.add(() -> setFullscreenExpanded(isInitialFullscreenExpanded(), false));
        }

        if (GLConfig.DEFAULT_WINDOW_IN_SCREEN_CENTER) {
            setSurfaceLocationCenter(false);
        }

        frameRate(GLConfig.FRAME_RATE);

        // Background Image
        if (R.IMAGE_BG != null) {
            bgImage = loadImage(R.IMAGE_BG.toString());
        }

        // Fonts
        pdSans = createFont(R.FONT_PD_SANS_REGULAR.toString(), 20);
        pdSansMedium = createFont(R.FONT_PD_SANS_MEDIUM.toString(), 20);
        textFont(pdSans);       // Default

        // Finally, enqueue tasks
        enqueueTasks(tasks);
    }

    public float getTextSize(float size) {
        return GLConfig.getTextSize(width, height, size);
    }

    protected void onResized(int w, int h) {
        if (bgImage != null) {
            bgImage.resize(w, h);
        }

        updateCameraOnWindowResize();
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
            continuousKeyPressed(mKeyEvent);
        }
    }



    @Override
    public void draw() {
        preDraw();

        /* Background */
        if (bgImage != null) {
            bgImage.resize(width, height);
            background(bgImage);
        } else {
            background(GLConfig.BG_DARK.getRGB());
        }


        /* Cube */
        pushMatrix();

        final int n = getN();
        final Point3DF o = cubeOrigin(width, height, n);
        final float curLevitation = getLevitation(true);        // update and get current value

        translate(o.x, o.y + curLevitation, o.z);

        final float scale = cubeScale(width, height, n) * getCubeDrawScale();
        scale(scale * (GLConfig.CUBE_INVERT_X ? -1 : 1), scale * (GLConfig.CUBE_INVERT_Y ? -1 : 1), scale * (GLConfig.CUBE_INVERT_Z ? -1 : 1));

        if (isDrawCubeAxesEnabled()) {
            drawMainAxes(n, scale, GLConfig.DEFAULT_CUBE_DRAW_AXES_ONLY_POSITIVE);
        }

        cubeGL.draw(this);
        popMatrix();

        /* HUD */
        final PCamera camera = getCamera();
        camera.beginHUD();
        drawHUD();
        camera.endHUD();

        postDraw();
    }


    protected void drawHUD() {
        /* ................................... HUD .............................. */

        pushStyle();

        final boolean hudEnabled = isHudEnabled();
        final boolean showKeyBindings = areKeyBindingsShown();
        final Predicate<Control> keyBindingsFilter = c -> showKeyBindings || c.alwaysShowKeyBinding;

        final float padx = 20;
        final float pady = 20;
        final float vgap_main = pady / 3;
        final float vgap_status = pady / 2;
        final float lenAxis = 50;

        /* TOP-LEFT SIDE: Axes and Camera ................................................................... */
        final PCamera cam = getCamera();
        final boolean drawAxes = shouldDrawAxesInHUD();
        float y_1 = pady;

        if (drawAxes) {
            y_1 = drawHUDAxes(cam.getRotationsF(), padx, y_1, padx/1.5f, pady/1.5f, lenAxis);
            y_1 += (pady * 2);      // vgap
        }


//        final float cameraModeTextSize = getTextSize(GLConfig.CAMERA_MODE_TEXT_SIZE);
//        final float cubeLockedTextSize = getTextSize(GLConfig.CUBE_STATE_TEXT_SIZE);

        // Camera
//        if (GLConfig.SHOW_CAMERA_MODE) {
//            pushStyle();
//            fill(GLConfig.FG_CAMERA_MODE.getRGB());
//            textFont(pdSansMedium, cameraModeTextSize);
//            textAlign(LEFT, TOP);
//            text(GLConfig.getCameraModeText(mFreeCamera, "V"), h_offset, v_offset);
//            popStyle();
//        }

        // Cube State
//        if (GLConfig.SHOW_CUBE_STATE) {
//            final String stateText = getCubeStateText();
//            if (Format.notEmpty(stateText)) {
//                pushStyle();
//                fill(GLConfig.FG_CUBE_STATE.getRGB());
//                textFont(pdSans, cubeLockedTextSize);
//                textAlign(RIGHT, TOP);
//                text(stateText, width - h_offset, v_offset);
//                popStyle();
//            }
//        }

        // Controls
//        if (areKeyBindingsShown()) {
//            pushStyle();
//            final float titleTextSize = getTextSize(GLConfig.CONTROLS_DES_TITLE_TEXT_SIZE);
//            final float titleY = cameraModeTextSize + v_offset * 4;
//
//            fill(GLConfig.FG_CONTROLS_DES_TITLE.getRGB());
//            textFont(pdSansMedium, titleTextSize);
//            textAlign(LEFT, TOP);
//            text("Move Controls", h_offset * 3, titleY);
//            textAlign(RIGHT, TOP);
//            text("View Controls", width - h_offset * 3, titleY);
//
//            fill(GLConfig.FG_CONTROLS_DES.getRGB());
//            textFont(pdSans, getTextSize(GLConfig.CONTROLS_DES_TEXT_SIZE));
//
//            final float desY = titleY + titleTextSize + v_offset * 1.5f;
//            textAlign(LEFT, TOP);
//            text(R.DES_CONTROLS_MOVES + "\n\n" + R.DES_CONTROLS_SOLVE, h_offset, desY);
//
//            textAlign(RIGHT, TOP);
//            text(R.DES_CONTROLS_CUBE_CAMERA, width - h_offset, desY);
//            popStyle();
//        }


//        final float statusTextSize = getTextSize(GLConfig.STATUS_TEXT_SIZE);
//
//        // Status bar
//        if (GLConfig.SHOW_STATUS) {
//            final String statusText = getStatusText();
//            if (Format.notEmpty(statusText)) {
//                pushStyle();
//                fill(GLConfig.FG_STATUS_BAR.getRGB());
//                textSize(statusTextSize);
//                textAlign(LEFT, BOTTOM);
//                text(statusText, h_offset, height - v_offset);
//                popStyle();
//            }
//        }
//
//        // Last Move
//        if (GLConfig.SHOW_LAST_MOVE) {
//            final Move lastMove = cubeGL.getCube().peekLastMove();
//            if (lastMove != null) {
//                pushStyle();
//                fill(GLConfig.FG_LAST_MOVE.getRGB());
//                textSize(getTextSize(GLConfig.LAST_MOVE_TEXT_SIZE));
//                textAlign(LEFT, BOTTOM);
//                text(GLConfig.getLastMoveText(lastMove, "Ctrl-Z"), h_offset, height - statusTextSize - v_offset * 2);
//                popStyle();
//            }
//        }
//
//        // Secondary Status
//        if (GLConfig.SHOW_SEC_STATUS) {
//            final String secStatus = getSecStatusText();
//            if (secStatus != null) {
//                pushStyle();
//                fill(GLConfig.FG_SEC_STATUS.getRGB());
//                textSize(getTextSize(GLConfig.SEC_STATUS_TEXT_SIZE));
//                textAlign(RIGHT, BOTTOM);
//                text(secStatus, width - h_offset, height - statusTextSize - v_offset * 2);
//                popStyle();
//            }
//        }
//
//        // Cur Move (MIDDLE)
//        if (GLConfig.SHOW_CUR_MOVE) {
//            final Move curMove = cubeGL.getYoungestRunningMove();
//            if (curMove != null) {
//                pushStyle();
//                fill(GLConfig.FG_CUR_MOVE.getRGB());
//                textSize(getTextSize(GLConfig.CUR_MOVE_TEXT_SIZE));
//                textAlign(CENTER, BOTTOM);
//                text(curMove.toString(), width / 2f, height - statusTextSize - v_offset * 2);
//                popStyle();
//            }
//        }
    }


    protected void postDraw() {
    }




    private void drawMainXAxis(float positiveLen, float negativeLen, float arrowHalfLen, boolean drawPositiveArrow, boolean drawNegativeArrow) {
        line(-negativeLen, 0, 0, positiveLen, 0, 0);

        // Positive Arrow
        if (drawPositiveArrow) {
            triangle(positiveLen, -arrowHalfLen, positiveLen, arrowHalfLen, positiveLen + (arrowHalfLen * 1.732f), 0);
        }

        // Negative Arrow
        if (drawNegativeArrow) {
            triangle(-negativeLen, -arrowHalfLen, -negativeLen, arrowHalfLen, -negativeLen - (arrowHalfLen * 1.732f), 0);
        }
    }

    private void drawMainAxes(int n, float scale, boolean drawOnlyPositive) {
        pushStyle();
        strokeWeight(4 / scale);

        final float positiveLen = CubeI.CUBIE_SIDE_LEN * n;
        final float negativeLen = drawOnlyPositive? 0 : positiveLen;
        final float arrowHalfSideLen = 7 / scale;     // half side len of triangle

        // X-axis
        stroke(GLConfig.COLOR_AXIS_X_CUBE.getRGB());
        fill(GLConfig.COLOR_AXIS_X.getRGB());
        drawMainXAxis(positiveLen, negativeLen, arrowHalfSideLen, true, !drawOnlyPositive);

        // Y-axis
        stroke(GLConfig.COLOR_AXIS_Y_CUBE.getRGB());
        fill(GLConfig.COLOR_AXIS_Y.getRGB());

        pushMatrix();
        rotateZ(HALF_PI);
        drawMainXAxis(positiveLen, negativeLen, arrowHalfSideLen, true, !drawOnlyPositive);
        popMatrix();

        // Z-axis
        stroke(GLConfig.COLOR_AXIS_Z_CUBE.getRGB());
        fill(GLConfig.COLOR_AXIS_Z.getRGB());

        pushMatrix();
        rotateY(-HALF_PI);
        drawMainXAxis(positiveLen, negativeLen, arrowHalfSideLen, true, !drawOnlyPositive);
        popMatrix();

        popStyle();
    }

    private float drawHUDAxes(float @Nullable[] rotations, float topX, float topY, float ipadx, float ipady, float lenAxis) {
        pushMatrix();

        translate(topX + ipadx + lenAxis, topY + ipady + lenAxis, 0);       // go to center

        // bg
        pushStyle();
        rectMode(CENTER);
        noStroke();
        fill(GLConfig.BG_MEDIUM.getRGB());
        rect(0, 0, (ipadx + lenAxis) * 2, (ipady + lenAxis) * 2, max(ipadx, ipady));
        popStyle();

        // Axes
        if (rotations != null) {
            rotateX(rotations[0]);
            rotateY(rotations[1]);
            rotateZ(rotations[2]);
        }

        pushStyle();
        strokeWeight(2);

        // X-Axis
        stroke(GLConfig.COLOR_AXIS_X.getRGB());
        line(0, 0, 0, (GLConfig.CUBE_INVERT_X? -1: 1) * lenAxis, 0, 0);
//        text('X', lenAxis + 20, 0, 0);

        // Y-Axis
        stroke(GLConfig.COLOR_AXIS_Y.getRGB());
        line(0, 0, 0, 0, (GLConfig.CUBE_INVERT_Y? -1: 1) * lenAxis, 0);
//        text('Y', 0, lenAxis + 20, 0);

        // Z-Axis
        stroke(GLConfig.COLOR_AXIS_Z.getRGB());
        line(0, 0, 0, 0, 0, (GLConfig.CUBE_INVERT_Z? -1: 1) * lenAxis);
//        text('Z',0, 0, lenAxis + 20);

        popStyle();


        popMatrix();

        return topY + (lenAxis * 2);
    }

    /* Window ........................................................... */

    public final boolean setSurfaceLocation(int x, int y, boolean verbose) {
        if (supportsSurfaceLocationSetter()) {
            surface.setLocation(x, y);
            return true;
        }

        if (verbose) {
            U.printerrln(R.SHELL_WINDOW + String.format("Current %s renderer does not support changing window position on screen!!\n\tRenderer: %s\n\tFullscreen: %b", isRendered3D()? "3D": "2D", sketchRenderer(), isFullscreen()));
        }

        return false;
    }

    public final boolean setSurfaceSize(int w, int h, boolean verbose) {
        if (supportsSurfaceSizeSetter()) {
            surface.setSize(w, h);
            return true;
        }

        if (verbose) {
            U.printerrln(R.SHELL_WINDOW + String.format("Current %s renderer does not support changing window size!!\n\tRenderer: %s\n\tFullscreen: %b", isRendered3D()? "3D": "2D", sketchRenderer(), isFullscreen()));
        }

        return false;
    }

    public final boolean setSurfaceLocationCenter(boolean verbose) {
        final Dimension screen_res = getNativeScreenResolution();
        return setSurfaceLocation((screen_res.width - width) / 2, (screen_res.height - height) / 2, verbose);
    }

    public final void setSurfaceToInitialWindowedSize(boolean centerOnScreen, boolean verbose) {
        final Dimension def = getInitialWindowedSize();
        setSurfaceSize(def.width, def.height, verbose);

        if (centerOnScreen) {
            setSurfaceLocationCenter(verbose);
        }
    }

    // A Hack to force window to fullscreen with P2D and P3D.
    private void setSurfaceToNativeFullscreenSize() {
        final Dimension screen_res = getNativeScreenResolution();
        surface.setSize(screen_res.width, screen_res.height);
        surface.setLocation(0, 0);
    }

    /**
     * @return true if this window is in fullscreen mode and is expanded on the entire screen native resolution
     *
     * @see #isFullscreen()
     * @see #getNativeScreenResolution()
     * */
    public final boolean isFullscreenExpanded() {
        final Dimension screen_res;
        return isFullscreen() && (screen_res = getNativeScreenResolution()).width == width && screen_res.height == height;
    }

    /**
     * Expand or window a fullscreen PGraphics surface<br>
     * <br>
     * Expand: Fill the entire screen native resolution<br>
     * Window: Set the surface size to initial windowed size, as given by {@link #getInitialWindowedSize()} and center on screen
     * */
    public final void setFullscreenExpanded(boolean expanded, boolean verbose) {
        if (!isFullscreen() || (isFullscreenExpanded() == expanded))
            return;

        if (expanded) {
            setSurfaceToNativeFullscreenSize();
        } else {
            setSurfaceToInitialWindowedSize(true, verbose);
        }
    }

    public final void toggleFullscreenExpanded(boolean verbose) {
        if (!isFullscreen())
            return;

        setFullscreenExpanded(!isFullscreenExpanded(), verbose);
    }

    public final void resetSurfaceSizeAndPos(boolean verbose) {
        if (isFullscreen()) {
            setFullscreenExpanded(isInitialFullscreenExpanded(), verbose);
        } else {
            setSurfaceToInitialWindowedSize(true, verbose);
        }
    }

    public final void snapshot() {
        final Cube cube = cubeGL.getCube();

        String file_name = String.format("cube-%dx%d_moves-%d_distance-%d_timestamp-%d.png", cube.n, cube.n, cube.getAllAppliedMovesCount(), cube.cacheHeuristic(), System.currentTimeMillis());
//        file_name = Format.replaceAllWhiteSpaces(file_name.toLowerCase(), "_");

        saveFrame(file_name);
        U.println(R.SHELL_ROOT + "Frame saved to file: " + file_name);
    }


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

    /**
     * @return true if cancelled an ongoing solve, false if not currently solving
     * */
    public final boolean cancelSolve(boolean verbose) {
        final boolean solving = isSolving();
        final Canceller c = mSolveCanceller;
        if (c != null) {
            c.cancel(true);
            mSolveCanceller = null;     // gc, and no active canceller
        }

        final boolean cancelled = solving && c != null;
        if (verbose && cancelled) {
            U.println(R.SHELL_SOLVER + "Solving algorithm cancelled!");
        }

        return cancelled;
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

        cancelSolve(false);
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
//        if (createCopy) {
//            cube = new Cube(cube);      // Just copy to be safe
//        }

        final int n = cube.n();
        U.println(R.SHELL_SOLVER + String.format("Solving %dx%dx%d state: %s", n, n, n, cube.representation2D()));

        return Solver.solve(cube, createCopy, c);
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

        U.printerrln(R.SHELL_SOLVER + "Solver Failed: " + msg);
    }

    private void onSolveSuccess(@NotNull Solver.Solution solution) {
        mCurSolution = solution;

        if (solution.isEmpty()) {
            U.println(R.SHELL_SOLVER + "Cube is already solved!");
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
            sb.append("\nPress [").append(Control.SOLVE_OR_APPLY.keyBindingLabel).append("] or type command <solve> to apply the solution...");
            sb.append("\n...................................\n");
            U.println(R.SHELL_SOLVER + sb.toString());
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
        // If this is an ESC key down event, mask it to be able to handle it myself
        if (event.getAction() == KeyEvent.PRESS && (event.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE)) {
            event = Control.changeKeyCode(event, Control.ESCAPE_KEY_CODE_SUBSTITUTE, Control.ESCAPE_KEY_SUBSTITUTE);
        }

        super.handleKeyEvent(event);

        // Handle Custom Events
        if (event.getAction() == ACTION_EXECUTE_RUNNABLE && (event.getNative() instanceof Runnable task)) {
            task.run();
        }
    }


    @Override
    public void keyPressed(KeyEvent event) {
        super.keyPressed(event);
        mKeyEvent = event;

        if (event == null)
            return;

        // Handle Discrete Controls
        for (Control control: Control.getValuesShared()) {
            if (control.continuousKeyEvent)
                continue;

            if (control.handleKeyEvent(this, event))
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        super.keyReleased(event);
        if (mKeyEvent != null && mKeyEvent.getKeyCode() == event.getKeyCode()) {
            mKeyEvent = null;
        }
    }

    public void continuousKeyPressed(@Nullable KeyEvent event) {
        if (event == null)
            return;

        // Handle Continuous Controls
        for (Control control: Control.getValuesShared()) {
            if (!control.continuousKeyEvent)
                continue;

            if (control.handleKeyEvent(this, event))
                break;
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        super.mouseClicked(event);

        if (event == null)
            return;

        // Double mouse click  (left, right or middle)
        if (event.getCount() == 2) {
            resetCamera(true, true);
//            if (getCamera().insideViewport(event.getX(), event.getY())) {
//                resetCamera(true, true);
//            }
        }
    }


    /* Levitation */

    private void resetLevitationAnimator() {
        FloatAnimator lev_anim = mLevitationAnimator;
        if (lev_anim != null) {
            lev_anim.finish(true);
            mLevitationAnimator = null;     // gc
        }

        lev_anim = GLConfig.createLevitationAnimator(height);
        mLevitationAnimator = lev_anim;
        if (lev_anim != null) {
            lev_anim.start();
        }
    }

    private float getLevitation(boolean update) {
        final float lev;
        FloatAnimator lev_anim = mLevitationAnimator;

        if (lev_anim != null) {
            if (update) {
                lev_anim.update();
            }

            lev = lev_anim.getCurrentVal();
            if (lev_anim.isFinished()) {
                // Reverse the levitation animator
                lev_anim = lev_anim.reverse();
                mLevitationAnimator = lev_anim;
                lev_anim.start();
            }
        } else {
            lev = 0;
        }

        return lev;
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


    /* Cube Axes and Draw scale  ........................................................ */

    protected void onDrawCubeAxesChanged(boolean drawCubeAxes) {

    }

    public boolean isDrawCubeAxesEnabled() {
        return mDrawCubeAxes;
    }

    public final void setDrawCubeAxes(boolean drawCubeAxes) {
        if (mDrawCubeAxes == drawCubeAxes)
            return;

        mDrawCubeAxes = drawCubeAxes;
        onDrawCubeAxesChanged(drawCubeAxes);
    }

    public final void toggleDrawCubeAxes() {
        setDrawCubeAxes(!isDrawCubeAxesEnabled());
    }



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

    @NotNull
    private static CameraState createInitialCameraState(int width, int height, int n, @Nullable Rotation startRotation) {
        final Vector3D lookAt = cubeOrigin(width, height, n).toVector3D();
        final Rotation rotation = startRotation != null? startRotation: GLConfig.INITIAL_CAMERA_ROTATIONS;

        return new CameraState(
                rotation,
                lookAt,
                abs((float) (lookAt.getY() / tan(radians(26))))
        );
    }

    @NotNull
    private CameraState createInitialCameraState(@Nullable Rotation startRotation) {
        return createInitialCameraState(width, height, getN(), startRotation);
    }

    @NotNull
    private PCamera createNewCamera(@Nullable Rotation startRotation) {
        PCamera _camera = new PCamera(this, createInitialCameraState(startRotation));
        _camera.setResetOnDoubleClick(false);       // will handle myself
        _camera.setDefaultResetAnimationMills(GLConfig.CAMERA_RESET_ANIMATION_MILLS);
        return _camera;
    }

    @NotNull
    public final PCamera ensureCamera() {
        if (mCamera == null) {
            mCamera = createNewCamera(null);
        }

        return mCamera;
    }

    @NotNull
    public final PCamera getCamera() {
        return ensureCamera();
    }

    public final boolean isCameraSupported() {
        return true /* getCamera() != null */;
    }

    private void updateCameraInitialState() {
        getCamera().setStartState(createInitialCameraState(null));
    }

    private void updateCameraOnWindowResize() {
        final PCamera cam = getCamera();
        final CameraState state = cam.getState();

        // Update viewport and initial state
        cam.setViewport(0, 0, width, height);
        updateCameraInitialState();

        // Update only LookAt from current state
        cam.setState(state.withCenter(cam.getStartLookAt()), 0);
    }


    protected void onFreeCameraEnabledChanged(boolean freeCamera) {

    }

    public boolean isFreeCameraEnabled() {
        return mFreeCamera;
    }

    private void syncFreeCameraEnabled(boolean freeCamera, boolean animate, boolean notify) {
        mFreeCamera = freeCamera;

        final PCamera cam = getCamera();
        if (freeCamera) {
            cam.setLeftDragHandler(cam.getDefaultFreeRotationHandler());           // attach mouse drag handler
        } else {
            cam.setLeftDragHandler(null);       // detach mouse drag handler
            cam.reset(animate);     // can be a flag: reset_before_locking
        }

        if (notify) {
            onFreeCameraEnabledChanged(freeCamera);
        }
    }

    public void serFreeCameraEnabled(boolean freeCamera, boolean animate) {
        if (mFreeCamera == freeCamera)
            return;

        syncFreeCameraEnabled(freeCamera, animate, true);
    }

    public void toggleFreeCamera(boolean animate) {
        serFreeCameraEnabled(!isFreeCameraEnabled(), animate);
    }


    public final void resetCamera(boolean resetDrawScale, boolean animate) {
        if (resetDrawScale) {
            resetCubeDrawScale();
        }

        getCamera().reset(animate);
    }


    private static long rotationAnimMills(boolean animate) {
        return animate? GLConfig.CAMERA_ROTATIONS_ANIMATION_MILLS: 0;
    }

    public final void rotateCameraXByUnit(boolean up, boolean animate) {
        getCamera().rotateXBy((up? -1: 1) * HALF_PI, rotationAnimMills(animate));
    }

    public final void rotateCameraYByUnit(boolean left, boolean animate) {
        getCamera().rotateYBy((left? 1: -1) * HALF_PI, rotationAnimMills(animate));
    }

    public final void rotateCameraZByUnit(boolean left, boolean animate) {
        getCamera().rotateZBy((left? 1: -1) * HALF_PI, rotationAnimMills(animate));
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


    public final void resetSimulation() {
        cubeGL.resetMoveQuarterDurationMs();
        cubeGL.resetMoveGlInterpolatorOverride();
    }



    /* CubeGL Listeners */

    @Override
    public void onCubeChanged(@NotNull CubeGL cubeGL, @NotNull Cube old, @NotNull Cube _new) {
        cancelSolve(true);
        invalidateSolution();
        updateCameraInitialState();
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
        cancelSolve(true);  // Cancel solving
        invalidateSolution();
    }

    @Override
    public void onCubeLockChanged(@NotNull Cube cube, boolean locked) {

    }


    public static void init(String[] args) {
        //        R.createShellInstructionsReadme();
    }

    public static void main(String[] args) {
        init(args);

        final CubePUi3D app = new CubePUi3D();
        PApplet.runSketch(concat(new String[]{app.getClass().getName()}, args), app);

//        U.println(R.SHELL_INSTRUCTIONS);
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
//                final Runnable usagePrinter = () -> U.println("Usage: n <dimension>\nDimension should be in range [2, " + CubeI.DEFAULT_MAX_N + "]");
//
//                if (rest.isEmpty()) {
//                    usagePrinter.run();
//                    continue;
//                }
//
//                try {
//                    final int n = Integer.parseInt(rest);
//                    if (n < 2 || n > CubeI.DEFAULT_MAX_N) {
//                        u.printerrln(R.SHELL_DIMENSION + "Cube dimension must be >= 2 and <= " + CubeI.DEFAULT_MAX_N);
//                        usagePrinter.run();
//                    } else {
//                        app.enqueueTask(() -> app.cubeGL.setN(n, true /* use cli flag */));
//                    }
//                } catch (NumberFormatException ignored) {
//                    u.printerrln(R.SHELL_DIMENSION + "Cube dimension must be an integer, command: n [dim]");
//                    usagePrinter.run();
//                }
//            } else if (in.startsWith("scramble")) {
//                final String rest = in.substring(8).strip();
//                final Runnable usagePrinter = () -> U.println("Usage: scramble <num_moves>");
//                int n = CubeI.DEFAULT_SCRAMBLE_MOVES;
//
//                if (rest.isEmpty()) {
//                    U.println(R.SHELL_SCRAMBLE + "Using default scramble moves: " + n);
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
//                final Runnable usagePrinter = () -> U.println("Usage: speed <option>\nAvailable options\n\t+ : Increase speed\n\t- : Decrease Speed\n\t<percent> : set speed percentage in range [0, 100]");
//
//                if (rest.isEmpty()) {
//                    U.println("Current Speed: " + Format.nf001(app.getMoveSpeedPercent()));
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
//                            u.printerrln(R.SHELL_MOVE + "Move speed should be in range [0, 100], given: " + Format.nf001(in_per));
//                            usagePrinter.run();
//                            continue;
//                        }
//
//                        newPer = app.setMoveSpeedPercent(in_per);
//                    } catch (NumberFormatException ignored) {
//                        u.printerrln(R.SHELL_MOVE + "Move speed should be a number in range [0, 100], given: " + rest);
//                        usagePrinter.run();
//                        continue;
//                    }
//                }
//
//                U.println(R.SHELL_MOVE + "Move speed set to " + Format.nf001(newPer) + "%");
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
//                final Runnable usagePrinter = () -> U.println("Usage: interpolator <interpolator_key>\nAvailable Interpolators (key -> name)\n" + InterpolatorInfo.getDisplayInfo());
//                if (key.isEmpty()) {
//                    usagePrinter.run();
//                    continue;
//                }
//
//                final InterpolatorInfo info = InterpolatorInfo.fromKey(key);
//                if (info == null) {
//                    u.printerrln(R.SHELL_MOVE + "Invalid interpolator key <" + key + ">");
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
//                    u.printerrln(R.SHELL_MOVE + e.getMessage());
//                }
//            }
//        } while (running);
//
//        app.exit();
    }


}
