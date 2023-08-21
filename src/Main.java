import gl.CubeGL;
import gl.GLConfig;
import gl.animation.*;
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
import util.CollectionUtil;
import util.Util;

import java.awt.*;
import java.util.List;
import java.util.Scanner;
import java.util.StringJoiner;

public class Main extends PApplet implements Cube.Listener {

    @NotNull
    public static Dimension windowSize(int displayW, int displayH) {
        return new Dimension(Math.round(displayW / 1.4f), Math.round(displayH / 1.3f));
    }

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
    private boolean mShowControls = GLConfig.DEFAULT_CONTROLS_SHOWN;
    @Nullable
    private KeyEvent mKeyEvent;

    /* Cube */
    @NotNull
    private CubeGL cubeGL;
    private float cubeZoom = 1;
    @Nullable
    private FloatAnimator mLevitationAnimator;

    /* SOlver */
    private volatile boolean mSolving;
    @Nullable
    private volatile Solver.Solution mCurSolution;

    /* Camera */
    private boolean mFreeCam;
    private PeasyCam mPeasyCam;

    public Main(@Nullable Cube cube) {
        if (cube == null) {
            cube = new Cube(CubeI.DEFAULT_N);
        }

        cubeGL = new CubeGL(cube);
        cube.addListener(this);
    }

    public Main(int n) {
        this(n >= 2 ? new Cube(n) : null);
    }

    public Main() {
        this(null);
    }



    /* Main Utility */

    private void invalidateSolution() {
        mCurSolution = null;
    }

    protected void onCubeChanged(@NotNull Cube prevCube, @NotNull Cube newCube) {
        invalidateSolution();
    }

    @NotNull
    public final CubeGL getCubeGL() {
        return cubeGL;
    }

    @NotNull
    public final Cube getCube() {
        return cubeGL.getCube();
    }

    public final int getN() {
        return getCube().n;
    }


    public final boolean cubeLocked() {
        return mSolving;
    }

    public final boolean setCube(@NotNull Cube cube) {
        if (cubeLocked())
            return false;

        final Cube prev = getCube();
        if (cube == prev) {
            return false;
        }

        cubeGL = new CubeGL(cube);
        onCubeChanged(prev, cube);
        return true;
    }

    public final boolean setN(int n) {
        if (cubeLocked())
            return false;
        if (n < 2 || n > CubeI.DEFAULT_MAX_N || n == getN())
            return false;

        return setCube(new Cube(n));
    }

    public final boolean resetCube() {
        if (cubeLocked())
            return false;

        final Cube c = getCube();
        if (c.cacheIsSolved())
            return false;

        setCube(new Cube(c.n));
        return true;
    }

    @NotNull
    public final String cubeRepr2D() {
        return getCube().representation2D();
    }

    public float getTextSize(float size) {
        return GLConfig.getTextSize(this, size);
    }

    @Nullable
    public String getStatusText() {
        return GLConfig.getStatusText(this, mSolving, mCurSolution, "S");
    }

    public String getSecStatusText() {
        return GLConfig.getSecStatusText(getMoveSpeedPercent());
    }



    /* Setup and Drawing */

    protected void onResized(int w, int h) {
        if (bgImage != null) {
            bgImage.resize(w, h);
        }

        considerReCreateCam();
        resetLevitationAnimator();
    }


    @Override
    public void settings() {
        final Dimension size = windowSize(displayWidth, displayHeight);
        size(size.width, size.height, P3D);

        _w = width;
        _h = height;

        PJOGL.setIcon(R.APP_ICON.toString());       // app icon
    }


    @Override
    public void setup() {
//        colorMode(ARGB);

        surface.setTitle(R.APP_NAME);
        surface.setResizable(true);

        resetLevitationAnimator();
        setFreeCamInternal(mFreeCam);

        if (R.IMAGE_BG != null) {
            bgImage = loadImage(R.IMAGE_BG.toString());
        }

        pdSans = createFont(R.FONT_PD_SANS_REGULAR.toString(), 20);
        pdSansMedium = createFont(R.FONT_PD_SANS_MEDIUM.toString(), 20);

        textFont(pdSans);       // Default
    }


    public void preDraw() {
        if (_w != width || _h != height) {
            _w = width;
            _h = height;
            onResized(width, height);
        }

        /* Handle Keys */
        if (keyPressed && mKeyEvent != null) {
            switch (mKeyEvent.getKeyCode()) {
                case java.awt.event.KeyEvent.VK_DEAD_CEDILLA, java.awt.event.KeyEvent.VK_PLUS -> {
                    if (mKeyEvent.isShiftDown()) {
                        incMoveSpeed(true);
                    } else {
                        incCubeZoom();
                    }
                }

                case java.awt.event.KeyEvent.VK_DEAD_OGONEK, java.awt.event.KeyEvent.VK_MINUS -> {
                    if (mKeyEvent.isShiftDown()) {
                        decMoveSpeed(true);
                    } else {
                        decCubeZoom();
                    }
                }

//                case java.awt.event.KeyEvent.VK_N -> incN();
//                case java.awt.event.KeyEvent.VK_M -> decN();
            }
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
        if (!mFreeCam) {
            camera(o.x * 0.53f /* sin(radians(32)) */, o.y * -0.95f /* sin(radians(-72)) */, o.y * 1.6f /* 1/tan(radians(32)) */, 0, -curLevitation, 0, 0, 1, 0);
        }

        if (GLConfig.CUBE_DRAW_AXISES) {
            pushStyle();
            stroke(GLConfig.COLOR_AXIS.getRGB());
            strokeWeight(2);

            final float plen = Math.max(width, height);
            final float nlen = GLConfig.CUBE_DRAW_ONLY_POSITIVE_AXISES ? 0 : plen;

            line(-nlen, 0, 0, plen, 0, 0);         // X
            line(0, nlen, 0, 0, -plen, 0);       // Y (invert)
            line(0, 0, -nlen, 0, 0, plen);        // Z
            popStyle();
        }

        final float scale = cubeScale(width, height, getN()) * getCubeZoom();
        scale(scale * (GLConfig.CUBE_INVERT_X ? -1 : 1), scale * (GLConfig.CUBE_INVERT_Y ? -1 : 1), scale * (GLConfig.CUBE_INVERT_Z ? -1 : 1));
        cubeGL.draw(this);
        popMatrix();

        /* HUD */
        if (mPeasyCam != null) {
            mPeasyCam.beginHUD();
        }

        final float h_offset = width * 0.009f;
        final float v_offset = height / 96f;

        final float cameraModeTextSize = getTextSize(GLConfig.CAMERA_MODE_TEXT_SIZE);
        final float cubeLockedTextSize = getTextSize(GLConfig.CUBE_LOCKED_TEXT_SIZE);

        // Camera
        if (GLConfig.SHOW_CAMERA_MODE) {
            pushStyle();
            fill(GLConfig.FG_CAMERA_MODE.getRGB());
            textFont(pdSansMedium, cameraModeTextSize);
            textAlign(LEFT, TOP);
            text(GLConfig.getCameraModeText(mFreeCam, "V"), h_offset, v_offset);
            popStyle();
        }

        // Lock
        if (GLConfig.SHOW_CUBE_LOCKED) {
            final String lockText = GLConfig.getCubeLockedText(cubeLocked());
            if (Util.notEmpty(lockText)) {
                pushStyle();
                fill(GLConfig.FG_CUBE_LOCKED.getRGB());
                textFont(pdSansMedium, cubeLockedTextSize);
                textAlign(RIGHT, TOP);
                text(lockText, width - h_offset, v_offset);
                popStyle();
            }
        }

        // Controls
        if (controlsShown()) {
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
            if (Util.notEmpty(statusText)) {
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
                text(GLConfig.getLastMoveText(lastMove, "CTRL-Z"), h_offset, height - statusTextSize - v_offset * 2);
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
            final Move curMove = cubeGL.getRecentRunningMove();
            if (curMove != null) {
                pushStyle();
                fill(GLConfig.FG_CUR_MOVE.getRGB());
                textSize(getTextSize(GLConfig.CUR_MOVE_TEXT_SIZE));
                textAlign(CENTER, BOTTOM);
                text(curMove.toString(), width / 2f, height - statusTextSize - v_offset * 2);
                popStyle();
            }
        }

        if (mPeasyCam != null) {
            mPeasyCam.endHUD();
        }

        postDraw();
    }


    protected void postDraw() {
    }


    public void setCubeZoom(float zoom) {
        if (mFreeCam)
            return;
        cubeZoom = constrain(zoom, GLConfig.CUBE_ZOOM_MIN, GLConfig.CUBE_ZOOM_MAX);
    }

    public float getCubeZoom() {
        return mFreeCam? 1 : cubeZoom;
    }

    public void incCubeZoom() {
        setCubeZoom(cubeZoom + GLConfig.cubeZoomIncStep(cubeZoom));
    }

    public void decCubeZoom() {
        setCubeZoom(cubeZoom - GLConfig.cubeZoomDecStep(cubeZoom));
    }

    public void resetCubeZoom() {
        setCubeZoom(1);
    }

    public float incMoveSpeed(boolean continuous) {
        return GLConfig.convertDurationAndSpeedPercent(cubeGL.decMoveQuarterDuration(continuous));
    }

    public float decMoveSpeed(boolean continuous) {
        return GLConfig.convertDurationAndSpeedPercent(cubeGL.incMoveQuarterDuration(continuous));
    }

    public float getMoveSpeedPercent() {
        return GLConfig.convertDurationAndSpeedPercent(cubeGL.getMoveQuarterDurationPercent());
    }

    public float setMoveSpeedPercent(float percent) {
        return GLConfig.convertDurationAndSpeedPercent(cubeGL.setMoveQuarterDurationPercent(GLConfig.convertDurationAndSpeedPercent(percent)));
    }

    public float changeMoveSpeedPercentBy(float percentDelta) {
        return GLConfig.convertDurationAndSpeedPercent(cubeGL.changeMoveQuarterDurationPercentBy(GLConfig.convertDurationAndSpeedPercent(percentDelta)));
    }

    public void setShowControls(boolean showControls) {
        mShowControls = showControls;
    }

    public void toggleShowControls() {
        setShowControls(!mShowControls);
    }

    public boolean controlsShown() {
        return GLConfig.SHOW_CONTROLS_DES && mShowControls;
    }


    public void setAnimateMoves(boolean animateMoves) {
        cubeGL.setAnimateMoves(animateMoves);
    }

    public void toggleAnimateMoves() {
        cubeGL.toggleAnimateMoves();
    }

    /* Cube Moves */

    @Override
    public void onMoveApplied(@NotNull Move move, int cubiesAffected, boolean saved) {
        invalidateSolution();
    }

    public void applyMove(@NotNull Move move, boolean saveInStack, boolean now) {
        if (cubeLocked())
            return;
        cubeGL.applyMove(move, saveInStack, now);
    }

    public void applyMove(@NotNull Move move, boolean saveInStack) {
        if (cubeLocked())
            return;
        cubeGL.applyMove(move, saveInStack);
    }

    public void applyMove(@NotNull Move move) {
        if (cubeLocked())
            return;
        cubeGL.applyMove(move);
    }

    public boolean undoLastMove(boolean now) {
        if (cubeLocked())
            return false;
        return cubeGL.undoLastMove(now);
    }

    public boolean undoLastMove() {
        if (cubeLocked())
            return false;
        return cubeGL.undoLastMove();
    }

    public void applySequence(@NotNull List<Move> sequence, boolean saveInStack, boolean now) {
        if (cubeLocked())
            return;
        cubeGL.applySequence(sequence, saveInStack, now);
    }

    public void applySequence(@NotNull List<Move> sequence, boolean saveInStack) {
        if (cubeLocked())
            return;
        cubeGL.applySequence(sequence, saveInStack);
    }

    public void applySequence(@NotNull List<Move> sequence) {
        if (cubeLocked())
            return;
        cubeGL.applySequence(sequence);
    }

    public void scramble(int moves, boolean saveInStack, boolean now) {
        if (cubeLocked())
            return;
        cubeGL.scramble(moves, saveInStack, now);
    }

    public void scramble(int moves, boolean saveInStack) {
        if (cubeLocked())
            return;
        cubeGL.scramble(moves, saveInStack);
    }

    public void scramble(int moves) {
        if (cubeLocked())
            return;
        cubeGL.scramble(moves);
    }

    public void scramble() {
        if (cubeLocked())
            return;
        cubeGL.scramble();
    }

    public void rotateCubeX(int quarters) {
        if (cubeLocked())
            return;
        cubeGL.rotateX(quarters, true);
    }

    public void rotateCubeY(int quarters) {
        if (cubeLocked())
            return;
        cubeGL.rotateY(quarters, true);
    }

    public void rotateCubeZ(int quarters) {
        if (cubeLocked())
            return;
        cubeGL.rotateZ(quarters, true);
    }

    public void rotateCubeUp() {
        rotateCubeX(-1);
    }

    public void rotateCubeDown() {
        rotateCubeX(1);
    }

    public void rotateCubeLeft() {
        rotateCubeY(-1);
    }

    public void rotateCubeRight() {
        rotateCubeY(1);
    }

    public void rollCubeLeft() {
        rotateCubeZ(1);
    }

    public void rollCubeRight() {
        rotateCubeZ(-1);
    }

    public void finishAllMoves(boolean cancel) {
        if (cubeLocked())
            return;
        cubeGL.finishAllMoves(cancel);
    }

    public boolean setMoveGlInterpolator(@Nullable Interpolator interpolator) {
        return cubeGL.setMoveGlInterpolatorOverride(interpolator);
    }


    /* Solve */

    public void doSolveWorker() {
        if (mSolving)
            return;

        mSolving = true;
        Util.v(R.SHELL_SOLVER, "Solving state: " + cubeRepr2D());

        try {
            final Solver.Solution solution = Solver.solve3(getCube());
            mCurSolution = solution;
            if (solution.isEmpty()) {
                Util.v(R.SHELL_SOLVER, "Already solved!");
            } else {
                final Long msTaken = solution.getMsTaken();
                Util.v(R.SHELL_SOLVER, "Solution: " + solution.getSequence() + (msTaken != null && msTaken > 0 ? ", time taken: " + msTaken + "ms" : ""));
            }
        } catch (Exception exc) {
            Util.e(R.SHELL_SOLVER, "Solver Failed: " + exc.getMessage());
            invalidateSolution();
        }

        mSolving = false;
    }

    public final void solve() {
        if (mSolving)
            return;

        if (getN() != 3) {
            Util.w(R.SHELL_SOLVER, "Solver only works for 3*3 cube");
            return;
        }

        final Solver.Solution curSolution = mCurSolution;
        invalidateSolution();
        if (!(curSolution == null || curSolution.isEmpty())) {
            applySequence(curSolution.moves);
            return;
        }

        finishAllMoves(false);
        thread("doSolveWorker");
    }


    /* Bindings */

    private void onSpacePressed(@NotNull KeyEvent event) {
        scramble();
    }

    private void incN() {
        setN(getN() + 1);
    }

    private void decN() {
        setN(getN() - 1);
    }


    @Override
    public void keyPressed(KeyEvent event) {
        super.keyPressed(event);
        mKeyEvent = event;

//        final char key = event.getKey();
        final int keyCode = event.getKeyCode();
//        println("KeyEvent-> key: " + key + ", code: " + keyCode + ", ctrl: " + event.isControlDown() + ", alt: " + event.isAltDown() + ", shift: " + event.isShiftDown());

        switch (keyCode) {
            /* Cube Rotations */
            case java.awt.event.KeyEvent.VK_UP -> rotateCubeUp();
            case java.awt.event.KeyEvent.VK_DOWN -> rotateCubeDown();
            case java.awt.event.KeyEvent.VK_LEFT -> {
                if (shouldRollOverRotatingCube(event)) {
                    rollCubeLeft();
                } else {
                    rotateCubeLeft();
                }
            }
            case java.awt.event.KeyEvent.VK_RIGHT -> {
                if (shouldRollOverRotatingCube(event)) {
                    rollCubeRight();
                } else {
                    rotateCubeRight();
                }
            }

            /* Cube Moves */
            case java.awt.event.KeyEvent.VK_U -> applyMove(createMove(Axis.Y, event));
            case java.awt.event.KeyEvent.VK_R -> applyMove(createMove(Axis.X, event));
            case java.awt.event.KeyEvent.VK_F -> applyMove(createMove(Axis.Z, event));
            case java.awt.event.KeyEvent.VK_D -> applyMove(createMove(Axis.Y_N, event));
            case java.awt.event.KeyEvent.VK_L -> applyMove(createMove(Axis.X_N, event));
            case java.awt.event.KeyEvent.VK_B -> applyMove(createMove(Axis.Z_N, event));

            case java.awt.event.KeyEvent.VK_A -> toggleAnimateMoves();
            case java.awt.event.KeyEvent.VK_X -> finishAllMoves(event.isShiftDown());
            case java.awt.event.KeyEvent.VK_Z -> {
                if (event.isControlDown()) {
                    undoLastMove();
                }
            }


            /* Scramble, Solve and Reset */
            case java.awt.event.KeyEvent.VK_SPACE -> onSpacePressed(event);
            case java.awt.event.KeyEvent.VK_S -> solve();
            case java.awt.event.KeyEvent.VK_Q -> {
                if (event.isShiftDown()) {
                    resetCube();
                } else {
                    resetCubeZoom();
                }
            }

            /* Others */
            case java.awt.event.KeyEvent.VK_V -> toggleFreeCam();
            case java.awt.event.KeyEvent.VK_C -> toggleShowControls();
            case java.awt.event.KeyEvent.VK_N -> incN();
            case java.awt.event.KeyEvent.VK_M -> decN();
//            case java.awt.event.KeyEvent.VK_DEAD_CEDILLA, java.awt.event.KeyEvent.VK_PLUS -> incCubeZoom();
//            case java.awt.event.KeyEvent.VK_DEAD_OGONEK, java.awt.event.KeyEvent.VK_MINUS -> decCubeZoom();
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        super.keyReleased(event);
        if (mKeyEvent != null && mKeyEvent.getKeyCode() == event.getKeyCode()) {
            mKeyEvent = null;
        }
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

    /* Camera */

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
        return createCam(null);
    }

    private void considerReCreateCam() {
        if (!mFreeCam)
            return;

        float[] rotations = null;
        if (mPeasyCam != null) {
            mPeasyCam.setActive(false);
            rotations = mPeasyCam.getRotations();
        }

        mPeasyCam = createCam(rotations);
    }

    private void setFreeCamInternal(boolean freeCam) {
        mFreeCam = freeCam;

        if (freeCam) {
            considerReCreateCam();
        } else if (mPeasyCam != null) {
            mPeasyCam.setActive(false);     // Do not reset or nullify
        }
    }

    public void serFreeCam(boolean freeCam) {
        if (mFreeCam == freeCam)
            return;
        setFreeCamInternal(freeCam);
    }

    public void toggleFreeCam() {
        setFreeCamInternal(!mFreeCam);
    }

    public boolean isCamFree() {
        return mFreeCam;
    }


    private enum InterpolatorInfo {
        DEFAULT("default", "Default", null),
        LINEAR("linear", "Linear", Interpolator.LINEAR),
        BOUNCE("bounce", "Bounce", Interpolator.BOUNCE),

        ACCELERATE("acc", "Accelerate", new AccelerateInterpolator()),
        DECELERATE("dec", "Decelerate", new DecelerateInterpolator()),
        ACCELERATE_DECELERATE("acd", "Accelerate Decelerate", Interpolator.ACCELERATE_DECELERATE),

        ANTICIPATE("anticipate", "Anticipate", new AnticipateInterpolator()),
        OVERSHOOT("overshoot", "Overshoot", new AnticipateOvershootInterpolator())
        ;

        @NotNull
        public final String key;
        @NotNull
        public final String displayName;
        @Nullable
        public final Interpolator interpolator;

        InterpolatorInfo(@NotNull String key, @NotNull String displayName, @Nullable Interpolator interpolator) {
            this.key = key;
            this.displayName = displayName;
            this.interpolator = interpolator;
        }

        public static String getDisplayInfo() {
            final StringJoiner sj = new StringJoiner("\n");

            for (InterpolatorInfo info: values()) {
                sj.add("\t" + info.key + " -> " + info.displayName);
            }

            return sj.toString();
        }

        @Nullable
        public static InterpolatorInfo fromKey(String key) {
            if (key == null || key.isEmpty())
                return null;

            for (InterpolatorInfo info: values()) {
                if (info.key.equals(key))
                    return info;
            }

            return null;
        }
    }


    public static void main(String[] args) {
//        R.createShellInstructionsReadme();

        final Main app = new Main();
        PApplet.runSketch(concat(new String[]{app.getClass().getName()}, args), app);

        println(R.SHELL_INSTRUCTIONS);
        Scanner sc;
        boolean running = true;

        do {
            sc = new Scanner(System.in);
            print(R.SHELL_ROOT);

            final String in = sc.nextLine();
            if (in.isEmpty())
                continue;

            if (in.startsWith("n")) {
                final String rest = in.substring(1).strip();
                final Runnable usagePrinter = () -> Util.v("", "Usage: n <dimension>\nDimension should be in range [2, " + CubeI.DEFAULT_MAX_N + "]");

                if (rest.isEmpty()) {
                    usagePrinter.run();
                    continue;
                }

                try {
                    final int n = Integer.parseInt(rest);
                    if (n < 2 || n > CubeI.DEFAULT_MAX_N) {
                        Util.e(R.SHELL_DIMENSION, "Cube dimension must be >= 2 and <= " + CubeI.DEFAULT_MAX_N);
                        usagePrinter.run();
                    } else {
                        app.setN(n);
                    }
                } catch (NumberFormatException ignored) {
                    Util.e(R.SHELL_DIMENSION, "Cube dimension must be an integer, command: n [dim]");
                    usagePrinter.run();
                }
            } else if (in.startsWith("scramble")) {
                final String rest = in.substring(8).strip();
                final Runnable usagePrinter = () -> Util.v("", "Usage: scramble <num_moves>");
                int n = CubeI.DEFAULT_SCRAMBLE_MOVES;

                if (rest.isEmpty()) {
                    Util.v(R.SHELL_SCRAMBLE, "Using default scramble moves: " + n);
                    usagePrinter.run();
                } else {
                    try {
                        final int n2 = Integer.parseInt(rest);
                        if (n2 <= 0) {
                            Util.w(R.SHELL_SCRAMBLE, "Scramble moves must be positive integer, falling back to default scramble moves: " + n);
                            usagePrinter.run();
                        } else {
                            n = n2;
                        }
                    } catch (NumberFormatException ignored) {
                        Util.w(R.SHELL_SCRAMBLE, "Scramble moves must be positive integer, falling back to default scramble moves: " + n);
                        usagePrinter.run();
                    }
                }

                app.scramble(n);
            } else if (in.startsWith("reset")) {
                if (in.length() > 5 && in.substring(5).endsWith("zoom")) {
                    app.resetCubeZoom();
                } else {
                    app.resetCube();
                }
            } else if (in.startsWith("finish")) {
                app.finishAllMoves(in.endsWith("c"));
            } else if (in.equals("solve")) {
                app.solve();
            } else if (in.equals("undo")) {
                app.undoLastMove();
            } else if (in.startsWith("speed")) {
                final String rest = in.substring(5).strip();
                final Runnable usagePrinter = () -> Util.v("", "Usage: speed <option>\nAvailable options\n\t+ : Increase speed\n\t- : Decrease Speed\n\t<percent> : set speed percentage in range [0, 100]");

                if (rest.isEmpty()) {
                    Util.v("", "Current Speed: " + GLConfig.formatPercentage(app.getMoveSpeedPercent()));
                    usagePrinter.run();
                    continue;
                }

                final float newPer;

                if (rest.equals("+")) {
                    newPer = app.incMoveSpeed(false);
                } else if (rest.equals("-")) {
                    newPer = app.decMoveSpeed(false);
                } else {
                    try {
                        final float in_per = Float.parseFloat(rest);
                        if (in_per < 0 || in_per > 100) {
                            Util.e(R.SHELL_MOVE, "Move speed should be in range [0, 100], given: " + GLConfig.formatPercentage(in_per));
                            usagePrinter.run();
                            continue;
                        }

                        newPer = app.setMoveSpeedPercent(in_per);
                    } catch (NumberFormatException ignored) {
                        Util.e(R.SHELL_MOVE, "Move speed should be a number in range [0, 100], given: " + rest);
                        usagePrinter.run();
                        continue;
                    }
                }

                Util.v(R.SHELL_MOVE, "Move speed set to " + GLConfig.formatPercentage(newPer) + "%");
            }

            else if (in.startsWith("intp") || in.startsWith("interp") || in.startsWith("interpolator")) {
                String key = "";
                // checks with spaces
                if (in.startsWith("intp ")) {
                    key = in.substring(5);
                } else if (in.startsWith("interp ")) {
                    key = in.substring(7);
                } else if (in.startsWith("interpolator "))  {
                    key = in.substring(13);
                }

                final Runnable usagePrinter = () -> Util.v("", "Usage: interpolator <interpolator_key>\nAvailable Interpolators (key -> name)\n" + InterpolatorInfo.getDisplayInfo());
                if (key.isEmpty()) {
                    usagePrinter.run();
                    continue;
                }

                final InterpolatorInfo info = InterpolatorInfo.fromKey(key);
                if (info == null) {
                    Util.e(R.SHELL_MOVE, "Invalid interpolator key <" + key + ">");
                    usagePrinter.run();
                    continue;
                }

                final boolean changed = app.setMoveGlInterpolator(info.interpolator);
                final String msg = changed? "Move animation interpolator changed to: " + info.displayName: "Move animation interpolator already set to: " + info.displayName;
                Util.w(R.SHELL_MOVE, msg);
            }

            else if (in.equals("quit") || in.equals("exit")) {
                running = false;
                app.exit();
            } else {
                try {
                    List<Move> moves = Move.parseSequence(in);
                    if (CollectionUtil.notEmpty(moves)) {
                        app.applySequence(moves);
                    }
                } catch (Move.ParseException e) {
                    Util.e(R.SHELL_MOVE, e.getMessage());
                }
            }
        } while (running);
    }


}
