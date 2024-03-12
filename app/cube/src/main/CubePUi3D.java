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

import org.tritonus.share.ArraySet;
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
import java.util.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class CubePUi3D extends PApplet implements CubeGL.Listener {

    private static final boolean CREATE_README = false;

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
        return size / (n * CubeI.CUBIE_SIDE_LEN * 2.5f);
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
    @Nullable
    private volatile Exception mCurSolveException;
    private volatile boolean mLockCubeWhileSolving = GLConfig.DEFAULT_LOCK_CUBE_WHILE_SOLVING;

    /* Camera */
    private volatile boolean mFreeCamera = GLConfig.DEFAULT_FREE_CAMERA;
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
    private float mCurMidiNote = GLConfig.MIDI_NOTE_MIN;
    private float mMidiNoteStep = GLConfig.MIDI_NOTE_STEP;

    public CubePUi3D(@Nullable Cube cube) {
        final Config config = R.CONFIG;

        // Cube
        if (cube == null) {
            cube = new Cube(config.getValueInt(R.CONFIG_KEY_CUBE_SIZE, CubeI.DEFAULT_N));
        }

        cubeGL = new CubeGL(cube);

        // Config
        applyConfig(config);

        // preload
        if (mSoundEnabled) {
            getSoundPlayer();
        }

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
        return mCurSolution == null && mCurSolveException == null && !isSolving();        // Interferes with solution status
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

//    @Nullable
//    public String getStatusText() {
//        return GLConfig.getStatusText(width, height, frameRate, frameCount, mSolving, mCurSolution, "S");
//    }
//
//    @Nullable
//    public String getSecStatusText() {
//        return GLConfig.getSecStatusText(cubeGL.getMoveQuarterSpeedPercent());
//    }
//
//    @Nullable
//    public String getCubeStateText() {
//        return GLConfig.getCubeStateText(getCube().n(), isCubeLocked());
//    }



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
//        U.println("-> UI Thread: " + Thread.currentThread().getName());
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
        tasks.add(() -> resetCamera(false));

        // Fullscreen does not expand to entire screen with P2D and P3D renderers. THis hack solves the problem
        if (isFullscreen()) {       // (P2D.equals(sketchRenderer()) || P3D.equals(sketchRenderer()))
            // A Hack to force window to fullscreen with P2D and P3D. only works after a certain delay, so post an event
            tasks.add(() -> setFullscreenExpanded(isInitialFullscreenExpanded(), false, false));
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

        final float scale = cubeScale(width, height, n);
        scale(scale * (GLConfig.CUBE_INVERT_X ? -1 : 1), scale * (GLConfig.CUBE_INVERT_Y ? -1 : 1), scale * (GLConfig.CUBE_INVERT_Z ? -1 : 1));

        if (isDrawCubeAxesEnabled()) {
            drawMainAxes(n, scale, GLConfig.DEFAULT_CUBE_DRAW_AXES_ONLY_POSITIVE);
        }

        cubeGL.draw(this);
        popMatrix();

        /* HUD */
        beginHUD();
        drawHUD();
        endHUD();

        postDraw();
    }

    protected void postDraw() {
    }


    /* ................................... HUD .............................. */

    private int mSolverDrawTrigger;

    protected void beginHUD() {
        getCamera().beginHUD();
    }

    protected void endHUD() {
        getCamera().endHUD();
    }

    protected void drawHUD() {
        pushStyle();

        final boolean hudEnabled = isHudEnabled();
        final boolean showKeyBindings = areKeyBindingsShown();
        final boolean drawAxes = shouldDrawAxesInHUD();
        final Predicate<Control> keyBindingsFilter = c -> showKeyBindings || c.alwaysShowKeyBinding;

        final float padx = 20;
        final float pady = 20;
        final float vgap_main = pady / 3;
        final float vgap_status = pady / 2;
        final float lenAxis = 50;

        /* TOP-LEFT SIDE: Axes and Camera ................................................................... */
        final PCamera cam = getCamera();

        float y_left_top = pady;
        float y_right_top = pady;
        float y_right_bottom = height - pady;
        float y_left_bottom = y_right_bottom;

        if (drawAxes) {
            y_left_top = drawHUDAxes(cam.getRotationsF(), padx, y_left_top, padx/1.5f, pady/1.5f, lenAxis);
            y_left_top += (pady * 2);      // vgap
        }

        if (hudEnabled) {
            y_left_top = drawMainControlsAlignLeft(Control.CONTROLS_CAMERA1, y_left_top, true, padx, 0, vgap_main, keyBindingsFilter);
            y_left_top += pady;   // vgap

            y_left_top = drawMainControlsAlignLeft(Control.CONTROLS_CAMERA2, y_left_top, true, padx, 0, vgap_main, keyBindingsFilter);
            y_left_top += (pady * 1.5f);   // vgap

            if (isFullscreen()) {
                y_left_top = drawMainControlsAlignLeft(Control.CONTROLS_FULLSCREEN_WINDOW, y_left_top, true, padx, 0, vgap_main, keyBindingsFilter);
                y_left_top += (pady * 1.5f);   // vgap
            }
        }

        /* TOP-RIGHT and BOTTOM: HUD and Status ................................................... */
        final float statusTopY;

        if (hudEnabled) {
            // TOP-RIGHT: Main Controls 1
            y_right_top = drawMainControlsAlignRight(Control.CONTROLS_TOP_RIGHT1, y_right_top, true, padx, 0, vgap_main, keyBindingsFilter);
            y_right_top += pady;   // vgap

            // TOP-RIGHT: Main Controls 2
            y_right_top = drawMainControlsAlignRight(Control.CONTROLS_TOP_RIGHT2, y_right_top, true, padx, 0, vgap_main, keyBindingsFilter);
            y_right_top += (pady * 1.5f);   // vgap

            // BOTTOM-LEFT and BOTTOM-RIGHT: status 1 and status 2
            final String status_delimiter = "     |     ";
            statusTopY = drawStatusControls(Control.CONTROLS_STATUS_LEFT, Control.CONTROLS_STATUS_RIGHT, status_delimiter, y_right_bottom, padx, pady, vgap_status, showKeyBindings);

            if (Control.CONTROLS_STATUS_LEFT.length > 0) {
                y_left_bottom = statusTopY;
                y_left_bottom -= (pady * 1.5f);    // vgap
            }

            if (Control.CONTROLS_STATUS_RIGHT.length > 0) {
                y_right_bottom = statusTopY;
                y_right_bottom -= (pady * 1.5f);    // vgap
            }

            // BOTTOM-RIGHT: Main Controls 3
            y_right_bottom = drawMainControlsAlignRight(Control.CONTROLS_BOTTOM_RIGHT, y_right_bottom, false, padx, 0, vgap_main, keyBindingsFilter);
            y_right_bottom -= (pady * 1.5f);    // vgap

            // BOTTOM-LEFT: Main Controls 1
            y_left_bottom = drawMainControlsAlignLeft(Control.CONTROLS_BOTTOM_LEFT1, y_left_bottom, false, padx, 0, vgap_main, keyBindingsFilter);
            y_left_bottom -= pady;    // vgap

            // BOTTOM-LEFT: Main Controls 2
            y_left_bottom = drawMainControlsAlignLeft(Control.CONTROLS_BOTTOM_LEFT2, y_left_bottom, false, padx, 0, vgap_main, keyBindingsFilter);
            y_left_bottom -= pady;    // vgap

            // BOTTOM-LEFT: Main Controls 3
            y_left_bottom = drawMainControlsAlignLeft(Control.CONTROLS_BOTTOM_LEFT3, y_left_bottom, false, padx, 0, vgap_main, keyBindingsFilter);
            y_left_bottom -= (pady * 1.5f);    // vgap
        } else {
            // TOP-RIGHT: Controls to show even when HUD is disabled
            y_right_top = drawMainControlsAlignRight(Control.CONTROLS_HUD_DISABLED, y_right_top, true, padx, 0, vgap_main, keyBindingsFilter);
            y_right_top += (pady * 1.5f);   // vgap

            statusTopY = height;
        }

        // Currently Running Move
        if (GLConfig.SHOW_CUR_MOVE) {
            drawCurrentMove(cubeGL.getYoungestRunningMove(), height - pady, false);
        }

        // Solver
        drawSolverPages(padx, pady);

        // End
        popStyle();
    }


    private void drawSolverPages(float padx, float pady) {
        // Solver
        final boolean solving = isSolving();
        final Solver.Solution solution = mCurSolution;
        final Exception solveExc = mCurSolveException;

        if (solving || solution != null || solveExc != null) {
            final int n = getN();

            fill(GLConfig.BG_OVERLAY.getRGB());
            rectMode(CORNER);
            rect(0, 0, width, height);

            if (solving) {
                final int mills = millis();
                if (mSolverDrawTrigger == 0)
                    mSolverDrawTrigger = mills;

                final long pauseDelta = mills - mSolverDrawTrigger;
                final boolean draw = (pauseDelta >= 0 && pauseDelta < 500 /* ms to draw */);
                if (draw) {
                    // Draw things which depends on Solve state
                    final String text_main = "SOLVING  " + n + "x" + n;
                    final String text_sub = "Press [" + Control.ESCAPE.keyBindingLabel + "] to cancel";

                    drawSolverPage(text_main, text_sub, null, null);
                } else if (pauseDelta > 0) {
                    mSolverDrawTrigger = mills + 400 /* ms to hide */;
                }
            } else if (solution != null) {
                if (solution.isEmpty()) {
                    final String text_main = "ALREADY  SOLVED";
                    final String text_sub = "Press any key to continue";

                    drawSolverPage(text_main, text_sub, null, null);
                } else {
                    // A healthy solution
                    drawSolverSolutionPage(solution, padx, pady);
                }
            } else {
                // Solver error
                final String text_main = "SOLVER  FAILED";
                final String text_sub = "Error: " + getSolverExceptionMessage(solveExc) + "\nPress any key to continue";

                drawSolverPage(text_main, text_sub, GLConfig.COLOR_ERROR, null);
            }
        } else {
            mSolverDrawTrigger = 0;
        }
    }


    private void drawSolverPage(@NotNull String text_main, @Nullable String text_sub, @Nullable Color color_main, @Nullable Color color_sub) {
        textSize(getTextSize(GLConfig.TEXT_SIZE_HUGE3));
        final float height_main = textAscent() + textDescent();

        // Main
        if (color_main == null)
            color_main = GLConfig.COLOR_ACCENT_HIGHLIGHT;
        fill(color_main.getRGB());
        textAlign(CENTER, CENTER);
        text(text_main, width / 2f, height / 2f);

        if (Format.isEmpty(text_sub))
            return;

        // Sub
        textSize(getTextSize(GLConfig.TEXT_SIZE_LARGE1));
        if (color_sub == null)
            color_sub = GLConfig.FG_DARK;
        fill(color_sub.getRGB());
        textAlign(CENTER, TOP);
        text(text_sub, width / 2f, (height / 2f) + height_main);
    }

    // set textSize() before calling this method
    @NotNull
    private String ellipMoveSequence(@NotNull ListIterator<Move> moves_itr, int totalMovesCount, float maxSequenceWidth) {
        maxSequenceWidth -= textWidth(totalMovesCount + " + moves");    // to compensate for suffix

        final StringBuilder seq = new StringBuilder();
        float seq_w = 0;

        while (moves_itr.hasNext()) {
            String v = (moves_itr.nextIndex() == 0? "": Move.SEQUENCE_DELIMITER) + moves_itr.next().toString();

            float w = textWidth(v);
            if (seq_w + w > maxSequenceWidth)
                break;

            seq.append(v);
            seq_w += w;
        }

        if (moves_itr.hasNext()) {
            seq.append(" + ")
                    .append(Math.abs(totalMovesCount - moves_itr.previousIndex() /* this move was excluded in the while loop*/))
                    .append(" moves");
        }

        return seq.toString();
    }

    private void drawSolverSolutionPage(@NotNull Solver.Solution solution, float padx, float pady) {
        // A healthy solution
        final String title = solution.n + "x" + solution.n + "  SOLVED";

        textSize(getTextSize(GLConfig.TEXT_SIZE_HUGE1));
        final float titleH = textAscent() + textDescent();
        final float titleW = textWidth(title);
        float y = pady * 3;

        // Title
        fill(GLConfig.COLOR_ACCENT_HIGHLIGHT.getRGB());
        textAlign(CENTER, TOP);
        text(title, width / 2f, y);

        // Title Lines
        stroke(GLConfig.COLOR_ACCENT_HIGHLIGHT.getRGB());
        strokeWeight(2);
        final float line_padx = padx * 2, line_y = y + (titleH / 2);
        line(line_padx, line_y, (width / 2f) - (titleW / 2f) - line_padx, line_y);
        line((width / 2f) + (titleW / 2f) + line_padx, line_y, width - line_padx, line_y);

        y += (titleH + (pady * 3));

        // Entries ..........................................................
        final String delimiter = "    ";
        final String time_taken_label = "Time Taken";
        final String seq_label = "Moves (" + solution.moveCount() + ")";

        final float ts_label = getTextSize(GLConfig.TEXT_SIZE_SMALL1);
        final float ts_value = getTextSize(GLConfig.TEXT_SIZE_NORMAL);
        textSize(ts_label);

        final float max_label_w = max(textWidth(time_taken_label), textWidth(seq_label));
        final float label_h = textAscent() + textDescent();
        final float vgap = pady;

        // Labels
        fill(GLConfig.COLOR_ACCENT_HIGHLIGHT.getRGB());
        textAlign(LEFT, TOP);
        text(time_taken_label, line_padx, y);
        text(seq_label, line_padx, y + label_h + vgap);

        // Values
        textSize(ts_value);
        final String time_taken_val = solution.getMsTaken() + "  ms";

        final float max_seq_w = width - (line_padx * 2) - max_label_w - textWidth(delimiter.repeat(2));
        final String seq_val = ellipMoveSequence(solution.movesUnmodifiable.listIterator(), solution.moveCount(), max_seq_w);

        fill(GLConfig.FG_DARK.getRGB());
        textAlign(LEFT, BOTTOM);
        y += label_h;
        text(delimiter + time_taken_val, line_padx + max_label_w, y);
        y += vgap + label_h;
        text(delimiter + seq_val, line_padx + max_label_w, y);

        // Info...........................
        final String[] info_labels = {
                Control.SOLVE_OR_APPLY.keyBindingLabel,
                Control.ESCAPE.keyBindingLabel,
                "Ctrl-C"
        };

        final String[] info_vals = {
                "Apply solution",
                "Cancel solution",
                "Copy to clipboard"
        };

        final float ts_info_label = getTextSize(GLConfig.TEXT_SIZE_SMALL1);
        final float ts_info_val = getTextSize(GLConfig.TEXT_SIZE_NORMAL);
        textSize(max(ts_info_label, ts_info_val));
        final float info_e_height = textAscent() + textDescent();

        textSize(ts_info_label);
        float max_info_label_w = 0;
        for (String l: info_labels) {
            max_info_label_w = max(max_info_label_w, textWidth(l));
        }

        final float info_y = height - pady;
        final float info_vgap = pady / 2;

        textAlign(LEFT, BOTTOM);
        fill(GLConfig.FG_MAIN_CONTROLS_KEY_LABEL.getRGB());
        for (int i=0; i < info_labels.length; i++) {
            text(info_labels[info_labels.length - i - 1], line_padx, info_y - (i * (info_e_height + info_vgap)));
        }

        textSize(ts_info_val);
        textAlign(LEFT, BOTTOM);
        fill(GLConfig.FG_DARK.getRGB());
        for (int i=0; i < info_vals.length; i++) {
            text(delimiter + info_vals[info_vals.length - i - 1], line_padx + max_info_label_w, info_y - (i * (info_e_height + info_vgap)));
        }
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



    private record ControlDrawInfo(@NotNull Control control, boolean showKeyBinding, String keyBindingLabel, String formattedValue) {
    }

    /**
     * @return if alignTop is true -> bottomY of the bounding box, else topY of the bounding box
     * */
    private float drawMainControlsAlignRight(@NotNull Control[] controls, float startY, boolean alignTop, float padx, float hgap, float vgap, @NotNull Predicate<Control> showKeyBindingsFilter) {
        final String label_value_delimiter = "      ";
        final String label_key_label_delimiter = "   ";

        textSize(getTextSize(max(GLConfig.TEXT_SIZE_MAIN_CONTROLS_LABEL, GLConfig.TEXT_SIZE_MAIN_CONTROLS_KEY_LABEL, GLConfig.TEXT_SIZE_MAIN_CONTROLS_VALUE)));
        final float entry_height = textAscent() + textDescent();
        float max_val_width = 0;

        final ArrayList<ControlDrawInfo> list = new ArrayList<>();

        for (Control c: controls) {
            final boolean showKey = showKeyBindingsFilter.test(c);
            final String val = c.getFormattedValue(this);
            if (!showKey && Format.isEmpty(val))
                continue;

            final String keyLabel = showKey? c.keyBindingLabel : "";
            max_val_width = max(max_val_width, textWidth(val));

            list.add(new ControlDrawInfo(c, showKey, keyLabel, val));
        }

        max_val_width += textWidth(label_value_delimiter);

        final float x1 = width - padx;
        final float x2 = x1 - max_val_width - hgap;

        for (int i=0; i < list.size(); i++) {
            final ControlDrawInfo info = list.get(i);
            final String label = info.control.label;
            final String keyLabel = info.keyBindingLabel;
            final String val = info.formattedValue;

            final float y1 = startY + (alignTop? 1: -1) * (i * (entry_height + vgap));

            // Value
            textSize(getTextSize(GLConfig.TEXT_SIZE_MAIN_CONTROLS_VALUE));
            fill(GLConfig.FG_MAIN_CONTROLS_VALUE.getRGB());
            textAlign(RIGHT, alignTop? TOP: BOTTOM);
            text(label_value_delimiter + val, x1, y1);

            // Key Label
            final float keyLabelW;
            if (info.showKeyBinding) {
                textSize(getTextSize(GLConfig.TEXT_SIZE_MAIN_CONTROLS_KEY_LABEL));
                fill(GLConfig.FG_MAIN_CONTROLS_KEY_LABEL.getRGB());
                textAlign(RIGHT, BOTTOM);
                text(label_key_label_delimiter + keyLabel, x2, y1 + (alignTop? entry_height: 0));
                keyLabelW = textWidth(label_key_label_delimiter + keyLabel);
            } else {
                keyLabelW = 0;
            }

            // Label
            textSize(getTextSize(GLConfig.TEXT_SIZE_MAIN_CONTROLS_LABEL));
            fill((info.control.labelColorOverride != null? info.control.labelColorOverride: GLConfig.FG_MAIN_CONTROLS_LABEL).getRGB());
            textAlign(RIGHT, BOTTOM);
            text(label, x2 - keyLabelW, y1 + (alignTop? entry_height: 0));
        }

        final float my_height = ((list.size() - 1) * (entry_height + vgap)) + entry_height;
        return startY + (alignTop? 1: -1) * my_height;
    }


    private float drawMainControlsAlignLeft(@NotNull Control[] controls, float startY, boolean alignTop, float padx, float hgap, float vgap, @NotNull Predicate<Control> showKeyBindingsFilter) {
        final String label_value_delimiter = "      ";
        final String label_key_label_delimiter = "   ";

        textSize(getTextSize(max(GLConfig.TEXT_SIZE_MAIN_CONTROLS_LABEL, GLConfig.TEXT_SIZE_MAIN_CONTROLS_KEY_LABEL, GLConfig.TEXT_SIZE_MAIN_CONTROLS_VALUE)));
        final float entry_height = textAscent() + textDescent();

        textSize(getTextSize(max(GLConfig.TEXT_SIZE_MAIN_CONTROLS_LABEL, GLConfig.TEXT_SIZE_MAIN_CONTROLS_KEY_LABEL)));

        final ArrayList<ControlDrawInfo> list = new ArrayList<>(controls.length + 2);

        float max_label_width = 0;
        for (Control c: controls) {
            boolean showKey = showKeyBindingsFilter.test(c);
            final String value = c.getFormattedValue(this);

            if (!showKey && Format.isEmpty(value))
                continue;

            final String keyLabel = showKey? c.keyBindingLabel : "";
            max_label_width = max(max_label_width, textWidth(c.label + label_key_label_delimiter + keyLabel));

            list.add(new ControlDrawInfo(c, showKey, keyLabel, value));
        }

        final float x2 = padx + max_label_width + hgap;

        for (int i=0; i < list.size(); i++) {
            final ControlDrawInfo info = list.get(i);
            final String label = info.control.label;
            final String keyLabel = info.keyBindingLabel;
            final String val = info.formattedValue;

            final float y1 = startY + (alignTop? 1: -1) * (i * (entry_height + vgap));

            // Label
            textSize(getTextSize(GLConfig.TEXT_SIZE_MAIN_CONTROLS_LABEL));
            fill((info.control.labelColorOverride != null? info.control.labelColorOverride: GLConfig.FG_MAIN_CONTROLS_LABEL).getRGB());
            textAlign(LEFT, BOTTOM);
            text(label, padx, y1 + (alignTop? entry_height: 0));
            final float labelW = textWidth(label);

            // Key Label
            if (info.showKeyBinding) {
                textSize(getTextSize(GLConfig.TEXT_SIZE_MAIN_CONTROLS_KEY_LABEL));
                fill(GLConfig.FG_MAIN_CONTROLS_KEY_LABEL.getRGB());
                textAlign(LEFT, BOTTOM);
                text(label_key_label_delimiter + keyLabel, padx + labelW, y1 + (alignTop? entry_height: 0));
            }

            // Value
            textSize(getTextSize(GLConfig.TEXT_SIZE_MAIN_CONTROLS_VALUE));
            fill(GLConfig.FG_MAIN_CONTROLS_VALUE.getRGB());
            textAlign(LEFT, alignTop? TOP: BOTTOM);
            text(label_value_delimiter + val, x2, y1);
        }

        final float my_height = ((list.size() - 1) * (entry_height + vgap)) + entry_height;
        return startY + (alignTop? 1: -1) * my_height;
    }

    /**
     * @return top y coordinate of the bounding rectangle
     * */
    private float drawStatusControls(@NotNull Control[] controls1, @NotNull Control[] controls2, @NotNull String delimiter, float bottomY, float padx, float pady, float vgap, boolean showKeyBindings) {
        final String[] values1 = new String[controls1.length];
        final String[] values2 = new String[controls2.length];

        for (int i=0; i < controls1.length; i++) {
            Control c = controls1[i];
            values1[i] = c.label + " : " + c.getFormattedValue(this);
        }

        for (int i=0; i < controls2.length; i++) {
            Control c = controls2[i];
            values2[i] = c.label + " : " + c.getFormattedValue(this);
        }

        final String status1 = join(values1, delimiter);
        final String status2 = join(values2, delimiter);

        textSize(getTextSize(GLConfig.TEXT_SIZE__STATUS_CONTROLS));

        // Status1
        fill(GLConfig.FG__STATUS_CONTROLS.getRGB());
        textAlign(LEFT, BOTTOM);
        text(status1, padx, bottomY);

        // Status 2
        textAlign(RIGHT, BOTTOM);
        text(status2, width - padx, bottomY);

        final float statusTextHeight = textAscent() + textDescent();
        float statusTopY = bottomY - statusTextHeight;        // Top Y to return

        if (showKeyBindings) {
//            textSize(getTextSize(GLConfig.TEXT_SIZE__STATUS_CONTROLS));         // redundant

            final float bottomYKeys = statusTopY - vgap;

            final float delW = textWidth(delimiter);
            final float[] center1Pos = new float[values1.length];
            final float[] center2Pos = new float[values2.length];

            float xs1 = padx;
            for (int i=0; i < values1.length; i++) {
                if (i != 0)
                    xs1 += delW;

                float tw = textWidth(values1[i]);
                center1Pos[i] = xs1 + (tw / 2);

                xs1 += tw;
            }

            final float status2W = textWidth(status2);
            float xs2 = width - padx - status2W;
            for (int i=0; i < values2.length; i++) {
                if (i != 0)
                    xs2 += delW;

                float tw = textWidth(values2[i]);
                center2Pos[i] = xs2 + (tw / 2);

                xs2 += tw;
            }

            textSize(getTextSize(GLConfig.TEXT_SIZE_CONTROL_KEY_BINDING_LABEL));
            fill(GLConfig.FG_CONTROL_KEY_BINDING_LABEL.getRGB());
            textAlign(CENTER, BOTTOM);

            for (int i=0; i < controls1.length; i++) {
                text(controls1[i].keyBindingLabel, center1Pos[i], bottomYKeys);
            }

            for (int i=0; i < controls2.length; i++) {
                text(controls2[i].keyBindingLabel, center2Pos[i], bottomYKeys);
            }

            // final top y
            statusTopY = bottomYKeys - (textAscent() + textDescent());
        }


        return statusTopY;
    }

    private void drawCurrentMove(@Nullable Move curMove, float y, boolean alignTop) {
        if (curMove == null)
            return;

        fill(GLConfig.FG_CUR_MOVE.getRGB());
        textSize(getTextSize(GLConfig.TEXT_SIZE_CUR_MOVE));
        textAlign(CENTER, alignTop? TOP: BOTTOM);
        text(curMove.toString(), width / 2f, y);
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
    public final void setFullscreenExpanded(boolean expanded, boolean verbose, boolean force) {
        if (!isFullscreen() || (!force && isFullscreenExpanded() == expanded)) {
            if (verbose && !isFullscreen()) {
                U.printerrln(R.SHELL_WINDOW + "Window is currently NOT in fullscreen mode. Relaunch in fullscreen mode to expand/collapse.");
            }

            return;
        }

        if (expanded) {
            setSurfaceToNativeFullscreenSize();
        } else {
            setSurfaceToInitialWindowedSize(true, verbose);
        }
    }

    public final void toggleFullscreenExpanded(boolean verbose, boolean force) {
        setFullscreenExpanded(!isFullscreenExpanded(), verbose, force);
    }

    public final void resetSurfaceSizeAndPos(boolean verbose, boolean force) {
        if (isFullscreen()) {
            setFullscreenExpanded(isInitialFullscreenExpanded(), verbose, force);
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

        playNextMidiNote();
    }


    /* Solve */

    public boolean isLockCubeWhileSolvingEnabled() {
        return mLockCubeWhileSolving;
    }

    public void setLockCubeWhileSolving(boolean lockCubeWhileSolving) {
        mLockCubeWhileSolving = lockCubeWhileSolving;
    }

    protected void onSolvingFlagChanged(boolean solving) {
        playNextMidiNote();
    }

    public final boolean isSolving() {
        return mSolving;
    }

    private void setSolvingFlag(boolean solving) {
        final boolean old = mSolving;
        mSolving = solving;

        // Lock cube accordingly
        cubeGL.getCube().setLocked(solving && isLockCubeWhileSolvingEnabled());

        if (old != solving) {
            onSolvingFlagChanged(solving);
        }
    }


    @Nullable
    public final Exception getCurSolveException() {
        return mCurSolveException;
    }

    @Nullable
    public final Exception invalidateSolveException() {
        final Exception cur_exc = mCurSolveException;
        mCurSolveException = null;
        return cur_exc;
    }

    @Nullable
    public final Solver.Solution getCurrentSolution() {
        return mCurSolution;
    }

    /**
     * @return Solution which is invalidated, or {@code null} if there was no solution
     * */
    @Nullable
    public final Solver.Solution invalidateSolution(boolean invalidateSolveException) {
        final Solver.Solution cur_sol = mCurSolution;
        mCurSolution = null;
        if (invalidateSolveException) {
            invalidateSolveException();
        }

        return cur_sol;
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

    public final void solve(final boolean verbose) {
        if (isSolving()) {
            if (verbose) {
                U.printerrln(R.SHELL_SOLVER + "Solution sequence is in progress...");
            }

            return;
        }


//        if (getN() != 3) {
//            Util.w(R.SHELL_SOLVER, "Solver only works for 3*3 cube");
//            return;
//        }

        // If Has a solution, apply it
        final Solver.Solution curSolution = invalidateSolution(true);
        if (!(curSolution == null || curSolution.isEmpty()) && curSolution.n == getN()) {
            if (verbose) {
                U.println(R.SHELL_SOLVER + "Applying solution sequence: " + curSolution.getSequence());
            }

            cubeGL.applySequence(curSolution.movesUnmodifiable);
            return;
        }

        // Finish all pending moves, and start solving
        final int finishedCount = cubeGL.finishAllMoves(false);
        if (finishedCount > 0 && verbose) {
            U.println(R.SHELL_SOLVER + String.format("%d pending move%s have been finished before solving the cube", finishedCount, finishedCount > 1? "s": ""));
        }

        setSolvingFlag(true);       // turn solving-flag ON

        cancelSolve(false);
        final Canceller canceller = Canceller.basic();
        mSolveCanceller = canceller;

        Async.execute(() -> {
            Solver.Solution sol = null;
            Exception exc = null;
            try {
                sol = doSolveWorker(cubeGL.getCube(), true, canceller, verbose);
            } catch (Exception exc_) {
                exc = exc_;
            }

            onSolveFinishedWorker(sol, exc, verbose);
        });
    }


    // Worker thread
    @NotNull
    private Solver.Solution doSolveWorker(@NotNull Cube cube, final boolean createCopy, @Nullable CancellationProvider c, final boolean verbose) throws Solver.SolveException, CancellationException {
//        if (createCopy) {
//            cube = new Cube(cube);      // Just copy to be safe
//        }

        if (verbose) {
            final int n = cube.n();
            U.println(R.SHELL_SOLVER + String.format("Solving %dx%dx%d state: %s", n, n, n, cube.representation2D()));
        }

        return Solver.solve(cube, createCopy, c);
    }


    // Worker thread
    private void onSolveFinishedWorker(@Nullable Solver.Solution solution, @Nullable Exception exc, final boolean verbose) {
        enqueueTask(() -> onSolveFinishedMain(solution, exc, verbose));      // just enqueue for now
    }

    // UI-thread
    private void onSolveFinishedMain(@Nullable Solver.Solution solution, @Nullable Exception exc, final boolean verbose) {
        if (solution != null) {
            onSolveSuccess(solution, verbose);
        } else if (exc instanceof CancellationException cancellationException) {
            onSolveCancelled(cancellationException, verbose);
        } else {
            onSolveError(exc, verbose);
        }

        setSolvingFlag(false);       // turn solving-flag OFF
    }

    private void onSolveCancelled(@Nullable CancellationException exc, final boolean verbose) {
        if (verbose) {
            U.printerrln(R.SHELL_SOLVER + "Solver Cancelled");
        }

        invalidateSolution(true);
    }

    @NotNull
    private static String getSolverExceptionMessage(@Nullable Exception exc) {
        final String msg;

        if (exc instanceof Solver.SolveException) {
            msg = exc.getMessage();
        } else if (exc != null) {
            msg = exc.toString();
        } else {
            msg = "Unknown Error";
        }

        return msg;
    }

    private void onSolveError(@Nullable Exception exc, final boolean verbose) {
        invalidateSolution(false);
        if (exc == null) {
            exc = Solver.SolveException.UNKNOWN;
        }

        mCurSolveException = exc;
        if (verbose) {
            U.printerrln(R.SHELL_SOLVER + "Solver Failed: " + getSolverExceptionMessage(exc));
        }
    }

    private void onSolveSuccess(@NotNull Solver.Solution solution, final boolean verbose) {
        mCurSolution = solution;
        mCurSolveException = null;

        if (verbose) {
            if (solution.isEmpty()) {
                U.println(R.SHELL_SOLVER + "Cube is already solved!");
            } else {
                StringBuilder sb = new StringBuilder()
                        .append("Cube Solved!")
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

        // Key traps
        final Solver.Solution solution = mCurSolution;
        final Exception solveExc = mCurSolveException;
        if (solveExc != null || (solution != null && solution.isEmpty())) {
            invalidateSolution(true);       // Invalidate empty solution or error on any key press
            return;
        }

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

    public boolean onEscape(@Nullable KeyEvent event, final boolean canExit, final boolean verbose) {
        final int cancelled_count = cubeGL.cancelAllMoves();
        final boolean sol_cancelled = cancelSolve(verbose);
        final boolean sol_invalidated = invalidateSolution(false) != null;
        final boolean err_invalidated = invalidateSolveException() != null;

        if (canExit && !(cancelled_count > 0 || sol_cancelled || sol_invalidated || err_invalidated)) {
            exit();
        }

        if (verbose) {
            if (cancelled_count > 0) {
                U.println("\n" + R.SHELL_MOVE + String.format("%d move%s have been cancelled", cancelled_count, cancelled_count > 1? "s": ""));
            }
        }

        return true;
    }


    @Override
    public void mouseClicked(MouseEvent event) {
        super.mouseClicked(event);

        if (event == null)
            return;

        // Double mouse click  (left, right or middle)
        if (event.getCount() == 2) {
            if (!getCamera().isInStartState()) {
                resetCamera(true);
                return;
            }

            toggleFullscreenExpanded(false, true);

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
        playNextMidiNote();
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
        playNextMidiNote();
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
                .setReleaseTime(0.2f);
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

    private float nextMidiNote() {
        final float old = mCurMidiNote;

        // Change in a cyclic way
        mCurMidiNote += mMidiNoteStep;
        if (mCurMidiNote >= GLConfig.MIDI_NOTE_MAX) {
            mCurMidiNote = GLConfig.MIDI_NOTE_MAX;
            mMidiNoteStep *= -1;
        } else if (mCurMidiNote <= GLConfig.MIDI_NOTE_MIN) {
            mCurMidiNote = GLConfig.MIDI_NOTE_MIN;
            mMidiNoteStep *= -1;
        }

        return old;
    }

    private void playNextMidiNote() {
        if (!isSoundEnabled())
            return;

        getSoundPlayer().play(nextMidiNote());
    }


    protected void onSoundEnabledChanged(boolean soundEnabled) {
        playNextMidiNote();
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
        playNextMidiNote();
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
        playNextMidiNote();
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
        final PCamera cam = getCamera();
        return (float) (cam.getStartDistance() / cam.getDistance());
    }

    /**
     * @return new cube draw scale, which might be different from the drawScale argument
     * */
    public float setCubeDrawScale(float drawScale) {
        final float cur_scale = getCubeDrawScale();
        final float new_scale = constrain(drawScale, GLConfig.CUBE_DRAW_SCALE_MIN, GLConfig.CUBE_DRAW_SCALE_MAX);

        if (cur_scale == new_scale)
            return cur_scale;

        final PCamera cam = getCamera();
        final double new_dis = cam.getStartDistance() / new_scale;

        cam.setDistance(new_dis, 300);
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
        final CameraState startState = createInitialCameraState(startRotation);

        final PCamera _camera = new PCamera(this, startState);
        _camera.setResetOnDoubleClick(false);       // will handle myself
        _camera.setDefaultResetAnimationMills(GLConfig.CAMERA_RESET_ANIMATION_MILLS);
        onCameraInitialStateChanged(_camera, GLConfig.CUBE_DRAW_SCALE_DEFAULT);
        return _camera;
    }

    private static void onCameraInitialStateChanged(@NotNull PCamera camera, float currentDrawScale) {
        camera.setMinimumDistance(camera.getStartDistance() / GLConfig.CUBE_DRAW_SCALE_MAX, 0);
        camera.setMaximumDistance(camera.getStartDistance() / GLConfig.CUBE_DRAW_SCALE_MIN, 0);

        camera.setDistance(camera.getStartDistance() / currentDrawScale, 0);
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
        final PCamera cam = getCamera();
        final float drawScale = getCubeDrawScale();

        cam.setStartState(createInitialCameraState(null));
        onCameraInitialStateChanged(cam, drawScale);
    }

    private void updateCameraOnWindowResize() {
        final PCamera cam = getCamera();

        // Update viewport and initial state
        cam.setViewport(0, 0, width, height);
        updateCameraInitialState();         // also updates distance according to current drawScale

        // Update only LookAt from current state
        cam.lookAt(cam.getStartLookAt(), 0);
    }


    protected void onFreeCameraEnabledChanged(boolean freeCamera) {
        playNextMidiNote();
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

    public void setFreeCameraEnabled(boolean freeCamera, boolean animate) {
        if (mFreeCamera == freeCamera)
            return;

        syncFreeCameraEnabled(freeCamera, animate, true);
    }

    public void toggleFreeCamera(boolean animate) {
        setFreeCameraEnabled(!isFreeCameraEnabled(), animate);
    }


    public final void resetCamera(boolean animate) {
        getCamera().reset(animate);
    }


    private static long rotationAnimMills(boolean animate) {
        return animate? GLConfig.CAMERA_ROTATIONS_ANIMATION_MILLS: 0;
    }



    public final void rotateCameraXTo(double angle, boolean animate) {
        getCamera().rotateXTo(angle, rotationAnimMills(animate));
    }

    public final void rotateCameraYTo(double angle, boolean animate) {
        getCamera().rotateYTo(angle, rotationAnimMills(animate));
    }

    public final void rotateCameraZTo(double angle, boolean animate) {
        getCamera().rotateZTo(angle, rotationAnimMills(animate));
    }

    public final void rotateCameraXBy(double angle, boolean animate) {
        getCamera().rotateXBy(angle, rotationAnimMills(animate));
    }

    public final void rotateCameraYBy(double angle, boolean animate) {
        getCamera().rotateYBy(angle, rotationAnimMills(animate));
    }

    public final void rotateCameraZBy(double angle, boolean animate) {
        getCamera().rotateZBy(angle, rotationAnimMills(animate));
    }

    public final void rotateCameraXByUnit(boolean up, boolean animate) {
        rotateCameraXBy((up? -1: 1) * HALF_PI, animate);
    }

    public final void rotateCameraYByUnit(boolean left, boolean animate) {
        rotateCameraYBy((left? 1: -1) * HALF_PI, animate);
    }

    public final void rotateCameraZByUnit(boolean left, boolean animate) {
        rotateCameraZBy((left? 1: -1) * HALF_PI, animate);
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
        invalidateSolution(true);
        updateCameraInitialState();

        playNextMidiNote();
    }

    @Override
    public void onMoveAnimationEnabledChanged(@NotNull CubeGL cubeGL, boolean moveAnimationEnabled) {
        playNextMidiNote();
    }

    @Override
    public void onMoveGlConfigurerChanged(@NotNull CubeGL cubeGL, @Nullable Consumer<MoveGL> old, @Nullable Consumer<MoveGL> _new) {
        playNextMidiNote();
    }

    @Override
    public void onMoveGlInterpolatorOverrideChanged(@NotNull CubeGL cubeGL, @Nullable Interpolator old, @Nullable Interpolator _new) {
        playNextMidiNote();
    }

    @Override
    public void onMoveStarted(@NotNull MoveGL moveGL, boolean resumed) {

    }

    @Override
    public void onMovePaused(@NotNull MoveGL moveGL) {

    }

    @Override
    public void onMoveFinished(@NotNull MoveGL moveGL, Animator.@NotNull Finish how) {
        if (how == Animator.Finish.NORMAL && isSoundEnabled()) {
            playNextMidiNote();
        }
    }

    @Override
    public void onMoveApplied(@NotNull Cube cube, @NotNull Move move, int cubiesAffected, boolean saved) {
        cancelSolve(true);  // Cancel solving
        invalidateSolution(true);
    }

    @Override
    public void onCubeLockChanged(@NotNull Cube cube, boolean locked) {

    }

    private void printerrlnCubeLocked(@Nullable String prefix) {
        U.printerrln(R.SHELL_ROOT + (Format.isEmpty(prefix)? "": prefix + ". ") + "CUBE LOCKED: " + cubeGL.getCube().isLocked());
    }

    public void printerrlnCameraUnsupported() {
        U.printerrln(R.SHELL_CAMERA + "Camera is not supported by the current Renderer: " + sketchRenderer());
    }

    protected void main_init(String[] args) {
        if (CREATE_README) {
            R.createFullDescriptionReadme(true);
        }
    }

    public void main_cli(String[] args) {
        main_init(args);

        U.println(R.DESCRIPTION_GENERAL_WITH_HELP);
//        U.println("-> Command Line Thread: " + Thread.currentThread().getName() + "\n");
        boolean running = true;
        Scanner sc;

        final ArrayList<String> main_cmds = new ArrayList<>();
        final Set<String> ops = new ArraySet<>();
        final ArrayList<Runnable> tasks = new ArrayList<>();

        while (running) {
            sc = new Scanner(System.in);
            print(R.SHELL_ROOT);

            main_cmds.clear();
            ops.clear();
            tasks.clear();

            final String cmd = sc.nextLine().trim().toLowerCase();
            if (cmd.isEmpty())
                continue;

            switch (cmd) {
                case "exit", "quit" -> running = false;
                case "cancel", "stop" -> tasks.add(() -> onEscape(null, false, true));
                case "finish" -> tasks.add(() -> {
                    final int finishCount = cubeGL.finishAllMoves(false);
                    if (finishCount > 0) {
                        U.println("\n" + R.SHELL_MOVE + String.format("%d move%s have been finished", finishCount, finishCount > 1? "s": ""));
                    }
                });
                case "undo", "undo last" -> tasks.add(cubeGL::undoRunningOrLastCommittedMove);
                case "solve" -> tasks.add(() -> this.solve(true));
                case "anim", "toggle anim", "animations", "toggle animations" -> tasks.add(cubeGL::toggleMoveAnimationEnabled);
                case "axes", "toggle axes" -> tasks.add(this::toggleDrawCubeAxes);
                case "sound", "toggle sound" -> tasks.add(this::toggleSoundEnabled);
                case "poly-rhythm", "toggle poly-rhythm" -> tasks.add(this::togglePolyRhythmEnabled);
                case "hud", "toggle hud" -> tasks.add(this::toggleHudEnabled);
                case "controls", "toggle controls", "keys", "toggle keys" -> tasks.add(this::toggleShowKeyBindings);
                case "expand", "toggle expand", "collapse", "toggle collapse" ->  tasks.add(() -> toggleFullscreenExpanded(true, true));
                case "save", "save frame", "saveframe", "snap", "snapshot" -> tasks.add(this::snapshot);
                default -> {
                    final String[] tokens = splitTokens(cmd);
                    for (String s: tokens) {
                        if (s.isEmpty())
                            continue;

                        if (s.length() > 1 && s.charAt(0) == '-' && !Character.isDigit(s.charAt(1))) {
                            ops.add(s);
                        } else {
                            main_cmds.add(s);
                        }
                    }
                    if (main_cmds.isEmpty()) {
                        continue;
                    }

                    final String main_cmd = main_cmds.get(0);       // main command
                    final boolean forceFlag = ops.contains("-f");
//                    final boolean resetFlag = ops.contains("-reset");

                    switch (main_cmd) {
                        case "help", "usage" -> {
                            final Runnable usage_pr = () -> U.println(R.SHELL_ROOT + "Usage: help [-moves | -controls | -commands | -all]");

                            if (ops.contains("-all")) {
                                U.println("\n" + R.getFullDescription(true, true));
                                continue;
                            }

                            boolean done = false;

                            if (ops.contains("-m") || ops.contains("-move") || ops.contains("-moves")) {
                                U.println("\n" + R.DESCRIPTION_MOVES); 
                                done = true;
                            }

                            if (ops.contains("-control") || ops.contains("-controls") || ops.contains("-key") || ops.contains("-keys") || ops.contains("-keybindings") || ops.contains("-key-bindings")) {
                                U.println("\n" + R.getUiControlsDescription());
                                done = true;
                            }

                            if (ops.contains("-cmd") || ops.contains("-command") || ops.contains("-commands")) {
                                U.println("\n" + R.DESCRIPTION_COMMANDS);
                                done = true;
                            }

                            if (!done) {
                                usage_pr.run();
                            }
                        }

                        case "win", "window" -> {
                            final Runnable usage_pr = () -> U.println(R.SHELL_WINDOW + "Sets the window size or screen location.\nUsage: win [-size | -pos] <x> <y>\nWildcards: w -> initial windowed size (to be used with -size)  |  c -> center window on screen (to be used with -pos)\nExample: win -size 200 400  |  win -pos 10 20  |  win -pos center  |  win -size w\n");
                            final int mode;

                            // Mode: 0 -> Size, 1 -> Position
                            if (ops.contains("-size")) {
                                mode = 0;
                            } else if (ops.contains("-pos") || ops.contains("-position") || ops.contains("-loc") || ops.contains("-location")) {
                                mode = 1;
                            } else {
                                usage_pr.run();
                                continue;
                            }

                            // Wildcards
                            final String wildcard = main_cmds.size() == 2? main_cmds.get(1): null;
                            boolean wildcardDone = false;

                            if (Format.notEmpty(wildcard)) {
                                if (mode == 0 && (wildcard.equals("w") || wildcard.equals("win") || wildcard.equals("window") || wildcard.equals("windowed"))) {
                                    tasks.add(() -> setSurfaceToInitialWindowedSize(false, true));
                                    wildcardDone = true;
                                } else if (mode == 1 && (wildcard.equals("c") || wildcard.equals("center"))) {
                                    tasks.add(() -> setSurfaceLocationCenter(true));
                                    wildcardDone = true;
                                }
                            }

                            // Main commands
                            if (!wildcardDone) {
                                if (main_cmds.size() < 3) {
                                    usage_pr.run();
                                    continue;
                                }

                                final String v1_str = main_cmds.get(1);
                                final String v2_str = main_cmds.get(2);

                                try {
                                    final int v1 = Integer.parseInt(v1_str);
                                    final int v2 = Integer.parseInt(v2_str);

                                    if (mode == 0) {
                                        tasks.add(() -> setSurfaceSize(v1, v2, true));
                                    } else {
                                        tasks.add(() -> setSurfaceLocation(v1, v2, true));
                                    }
                                } catch (NumberFormatException n_exc) {
                                    U.printerrln(R.SHELL_WINDOW + String.format("Invalid arguments supplied to window %s. %s must only be integers. GIven: %s, %s", mode == 0? "size": "position", mode == 0? "Width and Height": "Screen X and Y coordinates", v1_str, v2_str));
                                    usage_pr.run();
                                } catch (Exception exc) {
                                    U.printerrln(R.SHELL_WINDOW + "Failed to set window " + (mode == 0? "size": "position") + ".\nException: " + exc);
                                    usage_pr.run();
                                }
                            }
                        }

                        case "n", "size", "dim", "dimension", "cube", "cube-size" -> {
                            final Runnable usage_pr = () -> U.println(R.SHELL_CUBE_SIZE + "Sets the cube size, in range [2, " + CubeI.DEFAULT_MAX_N + "].\nUsage: n [-f] <cube size>\nExample: n 12\n");
                            final Runnable cur_val_pr = () -> U.println(R.SHELL_CUBE_SIZE + String.format("Current cube size: %dx%d  |  Locked: %b", getN(), getN(), cubeGL.getCube().isLocked()));

                            final String count_str = main_cmds.size() > 1 ? main_cmds.get(1) : "";
                            if (count_str.isEmpty()) {
                                cur_val_pr.run();
                                usage_pr.run();
                                continue;
                            }

                            try {
                                final int size = Integer.parseInt(count_str);
                                if (size < 2 || size > CubeI.DEFAULT_MAX_N) {
                                    throw new IllegalArgumentException("Cube size must be in range [2, " + CubeI.DEFAULT_MAX_N + "], given: " + size);
                                }
                                
                                tasks.add(() -> {
                                    final boolean done = cubeGL.setN(size, forceFlag);
                                    if (done) {
                                        U.println(R.SHELL_CUBE_SIZE + String.format("Cube Size set to %dx%d. Forced: %b", size, size, forceFlag));
                                    } else {
                                        printerrlnCubeLocked(String.format("Failed to set cube size to %dx%d", size, size));
                                    }
                                });
                            } catch (NumberFormatException ignored) {
                                U.printerrln(R.SHELL_CUBE_SIZE + "Cube size must be an integer. Given: " + count_str);
                                usage_pr.run();
                            } catch (IllegalArgumentException arg_exc) {
                                U.printerrln(R.SHELL_CUBE_SIZE + arg_exc.getMessage());
                                usage_pr.run();
                            }
                        }

                        case "reset" -> {
                            final Runnable usage_pr = () -> U.println(R.SHELL_RESET + "Usage: reset [-f] [-state | -env | -cam | -win | -all]\nExample: reset -env -state  |  Default: reset -f -state -cam\n");

                            boolean done = false;

                            if (ops.contains("-all")) {
                                tasks.add(() -> {
                                    cubeGL.resetCube(forceFlag);
                                    resetSimulation();
                                    resetCamera(!forceFlag);
                                    resetSurfaceSizeAndPos(false, true);
                                });

                                done = true;
                            } else {
                                if (ops.contains("-env")) {
                                    tasks.add(this::resetSimulation);
                                    done = true;
                                }

                                if (ops.contains("-cam") || ops.contains("-camera")) {
                                    tasks.add(() -> resetCamera(!forceFlag));
                                    done = true;
                                }

                                if (ops.contains("-win") || ops.contains("-window")) {
                                    tasks.add(() -> resetSurfaceSizeAndPos(true, true));
                                    done = true;
                                }

                                if (ops.contains("-state")) {
                                    tasks.add(() -> cubeGL.resetCube(forceFlag));
                                    done = true;
                                }
                            }

                            if (!done) {        // Default
                                tasks.add(() -> cubeGL.resetCube(forceFlag));
                                usage_pr.run();
                            }
                        }

                        case "scramble", "shuffle" -> {
                            final Runnable usage_pr = () -> U.println(R.SHELL_SCRAMBLE + "Scrambles the cube.\nUsage: scramble [num_moves]. Defaults to " + CubeI.DEFAULT_SCRAMBLE_MOVES + " moves\nExample: scramble 24\n");

                            int count = CubeI.DEFAULT_SCRAMBLE_MOVES;
                            final String count_str = main_cmds.size() > 1 ? main_cmds.get(1) : "";
                            boolean set = false;

                            if (count_str.isEmpty()) {
                                usage_pr.run();
                                set = true;
                            } else {
                                try {
                                    count = Integer.parseInt(count_str);
                                    if (count <= 0) {
                                        throw new IllegalArgumentException("Number of scramble moves must be > 0, given: " + count);
                                    }

                                    set = true;
                                } catch (NumberFormatException ignored) {
                                    U.printerrln(R.SHELL_SCRAMBLE + "Number of scramble moves must be an integer. Given: " + count_str);
                                    usage_pr.run();
                                } catch (IllegalArgumentException arg_exc) {
                                    U.printerrln(R.SHELL_SCRAMBLE + arg_exc.getMessage());
                                    usage_pr.run();
                                }
                            }

                            if (set) {
                                final int finalCount = count;
                                tasks.add(() -> {
                                    final boolean done = cubeGL.scramble(finalCount);
                                    if (done) {
                                        U.println("\n" + R.SHELL_SCRAMBLE + String.format("Scrambling %dx%d cube with %d move%s...", getN(), getN(), finalCount, finalCount > 1? "s": ""));
                                    } else {
                                        printerrlnCubeLocked("Failed to scramble the cube");
                                    }
                                });
                            }
                        }

                        case "speed", "animspeed", "anim-speed" -> {
                            final Runnable cur_val_pr = () -> U.println(R.SHELL_ANIM_SPEED + String.format("Current: %s%% (%d ms)  |  Default: %s%% (%d ms)", Format.nf001(cubeGL.getMoveQuarterSpeedPercent()), cubeGL.getMoveQuarterDurationMs(), Format.nf001(GLConfig.moveQuarterDurationMsToPercent(GLConfig.MOVE_QUARTER_DURATION_MS_DEFAULT)), GLConfig.MOVE_QUARTER_DURATION_MS_DEFAULT));
                            final Runnable usage_pr = () -> U.println(R.SHELL_ANIM_SPEED + String.format("Usage: speed [-d | -p] <value>. Modes: -d -> Duration (ms, in range [%d, %d]) | -p -> Percentage. Defaults to percentage (-p) values\nExample: speed 91 | speed -d 300\n", GLConfig.MOVE_QUARTER_DURATION_MS_MIN, GLConfig.MOVE_QUARTER_DURATION_MS_MAX));

                            final String val_str = main_cmds.size() > 1 ? main_cmds.get(1) : "";
                            if (val_str.isEmpty()) {
                                cur_val_pr.run();
                                usage_pr.run();
                                continue;
                            }

                            boolean done = false;
                            try {
                                final float val = Float.parseFloat(val_str);

                                if (ops.contains("-d") || ops.contains("-dur") || ops.contains("-duration")) {
                                    final int int_val = floor(val);

                                    if (int_val < GLConfig.MOVE_QUARTER_DURATION_MS_MIN || int_val > GLConfig.MOVE_QUARTER_DURATION_MS_MAX) {
                                        throw new IllegalArgumentException(String.format("Move animation duration must be in range [%d, %d] ms. Given: %d ms", GLConfig.MOVE_QUARTER_DURATION_MS_MIN, GLConfig.MOVE_QUARTER_DURATION_MS_MAX, int_val));
                                    }

                                    tasks.add(() -> cubeGL.setMoveQuarterDurationMs(int_val));
                                    done = true;
                                } else {
                                    if (val < 0 || val > 100) {
                                        throw new IllegalArgumentException(String.format("Move animation speed percentage must be in range [0, 100] %%. Given: %s%%", Format.nf001(val)));
                                    }

                                    tasks.add(() -> cubeGL.setMoveQuarterSpeedPercent(val));
                                    done = true;
                                }
                            } catch (NumberFormatException ignored) {
                                U.printerrln(R.SHELL_ANIM_SPEED + "Move animation Speed (or duration) must be an integer or a floating point number, given: " + val_str);
                                usage_pr.run();
                            } catch (IllegalArgumentException iae) {
                                U.printerrln(R.SHELL_ANIM_SPEED + iae.getMessage());
                                usage_pr.run();
                            }

                            if (done) {
                                tasks.add(() -> U.println("\n" + R.SHELL_ANIM_SPEED + "Move animation speed set to " + Control.ANIMATION_SPEED.getFormattedValue(this)));
                            }
                        }

                        case "scale", "zoom" -> {
                            final Runnable cur_val_pr = () -> U.println(R.SHELL_SCALE + String.format("Current: %sx (%s%%)  |  Default: %sx (%s%%)", Format.nf001(getCubeDrawScale()), Format.nf001(getCubeDrawScalePercentage()), Format.nf001(GLConfig.CUBE_DRAW_SCALE_DEFAULT), Format.nf001(GLConfig.cubeDrawScaleToPercent(GLConfig.CUBE_DRAW_SCALE_DEFAULT))));
                            final Runnable usage_pr = () -> U.println(R.SHELL_SCALE + String.format("Usage: scale [-x | -p] <value>. Modes:-p -> Percentage | -x -> Multiple (in range [%s, %s]). Defaults to multiple (-x) values\nExample: scale -x 2.5 | scale -p 50\n", Format.nf001(GLConfig.CUBE_DRAW_SCALE_MIN), Format.nf001(GLConfig.CUBE_DRAW_SCALE_MAX)));

                            final String val_str = main_cmds.size() > 1 ? main_cmds.get(1) : "";
                            if (val_str.isEmpty()) {
                                cur_val_pr.run();
                                usage_pr.run();
                                continue;
                            }

                            boolean done = false;
                            try {
                                final float val = Float.parseFloat(val_str);

                                if (ops.contains("-p") || ops.contains("-percent")) {
                                    if (val < 0 || val > 100) {
                                        throw new IllegalArgumentException("Cube draw scale percentage must be in range [0, 100] %. Given: " + val);
                                    }

                                    tasks.add(() -> setCubeDrawScalePercent(val));
                                    done = true;
                                } else {
                                    if (val < GLConfig.CUBE_DRAW_SCALE_MIN || val > GLConfig.CUBE_DRAW_SCALE_MAX) {
                                        throw new IllegalArgumentException(String.format("Cube draw scale must be in range [%s, %s] x. Given: %s", Format.nf001(GLConfig.CUBE_DRAW_SCALE_MIN), Format.nf001(GLConfig.CUBE_DRAW_SCALE_MAX), Format.nf001(val)));
                                    }

                                    tasks.add(() -> setCubeDrawScale(val));
                                    done = true;
                                }
                            } catch (NumberFormatException ignored) {
                                U.printerrln(R.SHELL_SCALE + "Cube draw scale must be an integer or a floating point number, given: " + val_str);
                                usage_pr.run();
                            } catch (IllegalArgumentException iae) {
                                U.printerrln(R.SHELL_SCALE + iae.getMessage());
                                usage_pr.run();
                            }

                            if (done) {
                                tasks.add(() -> U.println("\n" + R.SHELL_SCALE + "Cube draw scale set to " + Control.CUBE_DRAW_SCALE.getFormattedValue(this)));
                            }
                        }

                        case "intp", "interp", "interpolator" -> {
                            final Runnable cur_val_pr = () -> U.println(R.SHELL_MOVE + String.format("Current Interpolator: %s | Default: %s", getCurrentMoveGlInterpolatorInfo(InterpolatorInfo.DEFAULT).displayName, InterpolatorInfo.fromInterpolator(GLConfig.DEFAULT_MOVE_ANIMATION_INTERPOLATOR, InterpolatorInfo.DEFAULT).displayName));
                            final Runnable usage_pr = () -> U.println(R.SHELL_MOVE + "Sets the move animation interpolator.\nUsage: interpolator <next | key>.  Wildcard: next -> cycle to next interpolator\nAvailable Interpolators (key -> name)\n" + InterpolatorInfo.getDisplayInfo(false));

                            final String val_str = main_cmds.size() > 1 ? main_cmds.get(1) : "";
                            if (val_str.isEmpty()) {
                                cur_val_pr.run();
                                usage_pr.run();
                                continue;
                            }

                            boolean done = false;

                            if (val_str.equals("next") || val_str.equals("up") || val_str.equals("+")) {
                                tasks.add(this::setNextMoveGlInterpolator);
                                done = true;
                            } else {
                                final InterpolatorInfo info = InterpolatorInfo.fromKey(val_str);
                                if (info == null) {
                                    U.printerrln(R.SHELL_MOVE + "Invalid interpolator key <" + val_str + ">");
                                    usage_pr.run();
                                } else {
                                    tasks.add(() -> setMoveGlInterpolator(info.interpolator));
                                    done = true;
                                }
                            }

                            if (done) {
                                tasks.add(() -> U.println("\n" + R.SHELL_MOVE + "Move animation interpolator set to: " + Control.ANIMATION_INTERPOLATOR.getFormattedValue(this)));
                            }
                        }

                        case "cam", "camera" -> {
                            final Runnable usage_pr = () -> U.println(R.SHELL_CAMERA + "Sets the camera mode to FREE or LOCKED\nUsage: cam [-free | -locked | -toggle]. Defaults to -toggle");
                            final Runnable cur_val_pr = () -> U.println("\n" + R.SHELL_CAMERA + "Current Camera Mode: " + (isFreeCameraEnabled()? "FREE": "LOCKED"));

                            if (ops.contains("-free")) {
                                tasks.add(() -> setFreeCameraEnabled(true, !forceFlag));
                            } else if (ops.contains("-loc") || ops.contains("-locked")) {
                                tasks.add(() -> setFreeCameraEnabled(false, !forceFlag));
                            } else {
                                // Toggle
                                tasks.add(() -> toggleFreeCamera(!forceFlag));
                                if (!ops.contains("-toggle")) {
                                    usage_pr.run();
                                }
                            }

                            tasks.add(cur_val_pr);
                        }

                        case "rotationx", "rotx", "rx", "pitch" -> {
                            if (!isCameraSupported()) {
                                printerrlnCameraUnsupported();
                                continue;
                            }

                            final Runnable cur_val_pr = () -> U.println(R.SHELL_ROTATION_X + String.format("Pitch (rotation about X-axis). Current: %s", Control.CAMERA_ROTATE_X.getFormattedValue(this)));
                            final Runnable usage_pr = () -> U.println(R.SHELL_ROTATION_X + "Usage: pitch [-by | -f] <+ | - | value_in_deg>. Options: -by -> change rotation by  |  -f -> Force without animations\nExample: pitch 90  |  rx -by 10.5  |  pitch +\n");

                            final String val_str = main_cmds.size() > 1 ? main_cmds.get(1) : "";
                            if (val_str.isEmpty()) {
                                cur_val_pr.run();
                                usage_pr.run();
                                continue;
                            }

                            if (val_str.equals("+") || val_str.equals("up")) {
                                tasks.add(() -> rotateCameraXByUnit(true, !forceFlag));
                            } else if (val_str.equals("-") || val_str.equals("down")) {
                                tasks.add(() -> rotateCameraXByUnit(false, !forceFlag));
                            } else {
                                try {
                                    final float val = radians(Float.parseFloat(val_str));
                                    if (ops.contains("-by")) {
                                        tasks.add(() -> rotateCameraXBy(val, !forceFlag));
                                    } else {
                                        tasks.add(() -> rotateCameraXTo(val, !forceFlag));
                                    }
                                } catch (NumberFormatException exc) {
                                    U.printerrln(R.SHELL_ROTATION_X + "Pitch (X-Rotation) must be an integer or a floating point number, given: " + val_str);
                                    usage_pr.run();
                                }
                            }
                        }

                        case "rotationy", "roty", "ry", "yaw" -> {
                            if (!isCameraSupported()) {
                                printerrlnCameraUnsupported();
                                continue;
                            }

                            final Runnable cur_val_pr = () -> U.println(R.SHELL_ROTATION_Y + String.format("Yaw (rotation about Y-axis). Current: %s", Control.CAMERA_ROTATE_Y.getFormattedValue(this)));
                            final Runnable usage_pr = () -> U.println(R.SHELL_ROTATION_Y + "Usage: yaw [-by | -f] <+ | - | value_in_deg>. Options: -by -> change rotation by  |  -f -> Force without animations\nExample: yaw 90  |  ry -by 10.5  |  yaw -\n");

                            final String val_str = main_cmds.size() > 1 ? main_cmds.get(1) : "";
                            if (val_str.isEmpty()) {
                                cur_val_pr.run();
                                usage_pr.run();
                                continue;
                            }

                            if (val_str.equals("+") || val_str.equals("left")) {
                                tasks.add(() -> rotateCameraYByUnit(true, !forceFlag));
                            } else if (val_str.equals("-") || val_str.equals("right")) {
                                tasks.add(() -> rotateCameraYByUnit(false, !forceFlag));
                            } else {
                                try {
                                    final float val = radians(Float.parseFloat(val_str));
                                    if (ops.contains("-by")) {
                                        tasks.add(() -> rotateCameraYBy(val, !forceFlag));
                                    } else {
                                        tasks.add(() -> rotateCameraYTo(val, !forceFlag));
                                    }
                                } catch (NumberFormatException exc) {
                                    U.printerrln(R.SHELL_ROTATION_Y + "Yaw (Y-Rotation) must be an integer or a floating point number, given: " + val_str);
                                    usage_pr.run();
                                }
                            }
                        }

                        case "rotationz", "rotz", "rz", "roll" -> {
                            if (!isCameraSupported()) {
                                printerrlnCameraUnsupported();
                                continue;
                            }

                            final Runnable cur_val_pr = () -> U.println(R.SHELL_ROTATION_Z + String.format("Roll (rotation about Z-axis). Current: %s", Control.CAMERA_ROTATE_Z.getFormattedValue(this)));
                            final Runnable usage_pr = () -> U.println(R.SHELL_ROTATION_Z + "Usage: roll [-by | -f] <+ | - | value_in_deg>. Options: -by -> change rotation by  |  -f -> Force without animations\nExample: roll 90  |  rz -by 10.5  |  roll right\n");

                            final String val_str = main_cmds.size() > 1 ? main_cmds.get(1) : "";
                            if (val_str.isEmpty()) {
                                cur_val_pr.run();
                                usage_pr.run();
                                continue;
                            }

                            if (val_str.equals("+") || val_str.equals("left")) {
                                tasks.add(() -> rotateCameraZByUnit(true, !forceFlag));
                            } else if (val_str.equals("-") || val_str.equals("right")) {
                                tasks.add(() -> rotateCameraZByUnit(false, !forceFlag));
                            } else {
                                try {
                                    final float val = radians(Float.parseFloat(val_str));
                                    if (ops.contains("-by")) {
                                        tasks.add(() -> rotateCameraZBy(val, !forceFlag));
                                    } else {
                                        tasks.add(() -> rotateCameraZTo(val, !forceFlag));
                                    }
                                } catch (NumberFormatException exc) {
                                    U.printerrln(R.SHELL_ROTATION_Z + "Roll (Z-Rotation) must be an integer or a floating point number, given: " + val_str);
                                    usage_pr.run();
                                }
                            }
                        }

                        default -> {
                            // Try to parse moves from the full command
                            try {
                                List<Move> moves = Move.parseSequence(cmd);
                                if (moves != null && !moves.isEmpty()) {
                                    tasks.add(() -> {
                                        final boolean applied = cubeGL.applySequence(moves);
                                        if (applied) {
                                            U.println("\n" + R.SHELL_MOVE + "Applying move sequence: " + Move.sequence(moves));
                                        } else {
                                            printerrlnCubeLocked("Failed to apply the given move sequence");
                                        }
                                    });
                                }
                            } catch (Move.ParseException e) {
                                U.printerrln(R.SHELL_MOVE + e.getMessage());
                            } catch (Exception ignored) {
                                U.printerrln(R.SHELL_ROOT + "Unknown Command: " + cmd);      // Unknown command
                            }
                        }
                    }
                }
            }

            // Enqueue UI tasks of this command
            enqueueTasks(tasks);
        }

        exit();

    }

    public static void main(String[] args) {
        final CubePUi3D app = new CubePUi3D();
        PApplet.runSketch(concat(new String[]{app.getClass().getName()}, args), app);

        app.main_cli(args);
    }


}
