//package bin;
//
//import gl.CubeGL;
//import gl.GLConfig;
//import gl.animation.FloatAnimator;
//import model.Axis;
//import model.Point3DF;
//import model.cube.Cube;
//import model.cube.CubeI;
//import model.cubie.Move;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import peasy.PeasyCam;
//import processing.core.PApplet;
//import processing.core.PFont;
//import processing.event.KeyEvent;
//import solver.Solver;
//import util.CollectionUtil;
//import util.Util;
//
//import java.util.InputMismatchException;
//import java.util.List;
//import java.util.Scanner;
//
//public class Main extends PApplet implements Cube.Listener {
//
//    private boolean mFreeCam;
//    private PeasyCam cam;
//    private int _w, _h;
//
//    @NotNull
//    private CubeGL cubeGL;
////    @NotNull
////    private String mCurMoveText = "";
//
//    @Nullable
//    private volatile Solver.Solution mCurSolution;
//    private volatile boolean mSolving;
//    private boolean mShowControls = GLConfig.DEFAULT_CONTROLS_SHOWN;
//
////    private float curLevitation = 0;
////    private int levitationSpeedMultiplier = 1;
//
//    private float cubeZoom = 1;
//    private FloatAnimator mLevitationAnimator;
//
//    public Main(@Nullable Cube cube) {
//        if (cube == null) {
//            cube = new Cube(CubeI.DEFAULT_N);
//        }
//
//        cubeGL = new CubeGL(cube);
//        cube.addListener(this);
//    }
//
//    public Main(int n) {
//        this(n >= 2? new Cube(n): null);
//    }
//
//    public Main() {
//        this(null);
//    }
//
//
//
//    /* Main Utility */
//
//    private void invalidateColution() {
//        mCurSolution = null;
//    }
//
//    protected void onCubeChanged(@NotNull Cube prevCube, @NotNull Cube newCube) {
//        invalidateColution();
//    }
//
//    @NotNull
//    public final CubeGL getCubeGL() {
//        return cubeGL;
//    }
//
//    @NotNull
//    public final Cube getCube() {
//        return cubeGL.getCube();
//    }
//
//    public final int getN() {
//        return getCube().n;
//    }
//
//    public final boolean setCube(@NotNull Cube cube) {
//        final Cube prev = getCube();
//        if (cube == prev) {
//            return false;
//        }
//
//        cubeGL = new CubeGL(cube);
//        onCubeChanged(prev, cube);
//        return true;
//    }
//
//    public final boolean setN(int n) {
//        if (n < 2 || n == getN())
//            return false;
//
//        return setCube(new Cube(n));
//    }
//
//    public final boolean resetCube() {
//        final Cube c = getCube();
//        if (c.cacheIsSolved())
//            return false;
//
//        setCube(new Cube(c.n));
//        return true;
//    }
//
//    @NotNull
//    public final String cubeRepr2D() {
//        return getCube().representation2D();
//    }
//
//    public final boolean cubeLocked() {
//        return mSolving;
//    }
//
//
//
//    @NotNull
//    public static Point3DF cubeOrigin(int width, int height, int n) {
//        return new Point3DF(width / 2f, height / 2f, 0);
//    }
//
//    private static float cubeScale(float width, float height, int n) {
//        float size = Math.min(width, height);
//
//        return size / (n * CubeI.CUBIE_SIDE_LEN * 2.5f);     // todo factor
//    }
//
//    public float getTextSize(float size) {
//        return GLConfig.getTextSize(this, size);
//    }
//
//    @Nullable
//    public String getStatusText() {
//        return GLConfig.getStatusText(this, mSolving, mCurSolution, "S");
//    }
//
//
//    /* Setup and Drawing */
//
//    private PFont pdSans, pdSansMedium;
//
//    public void pre() {
//        if (_w != width || _h != height) {
//            _w = width; _h = height;
//            onResized(width, height);
//        }
//    }
//
//    @Override
//    public void settings() {
//        size(round(displayWidth / 1.4f), round(displayHeight / 1.4f), P3D);
//        _w = width; _h = height;
//        registerMethod("pre", this);
//    }
//
//    @Override
//    public void setup() {
////        colorMode(ARGB);
//
//        resetLevitationAnimator();
//        setFreeCamInternal(mFreeCam);
//
//        pdSans = createFont(R.FONT_PD_SANS_REGULAR.toString(), 20);
//        pdSansMedium = createFont(R.FONT_PD_SANS_MEDIUM.toString(), 20);
//        textFont(pdSans);       // Default
//    }
//
//    public static final String DES_CONTROLS_MOVES = "U....Up\nR....Right\nF....Front\nD...Down\nL....Left\nB...Back\n\nSHIFT-Move....Inverse Move\nALT-Move.......Twice Move\nCTRL-Move....2 Slice Move";
//    public static final String DES_CONTROLS_CUBE_CAMERA = "Up.............Rotate Up\nDown.......Rotate Down\nLeft...........Rotate Left\nRight........Rotate Right\nCTRL-Left.......Roll Left\nCTRL-Right....Roll Right\n";
//    public static final String DES_CONTROLS_AUX = "S..................Solve\nSPACE.......Scramble\nSHIFT-Q....Reset Cube";
//    public static final String DES_CONTROLS_AUX2 = "A....Finish Moves\n+/-/Q....Zoom In/Out/Reset\nC....Toggle Controls";
//
//    public static final String DES_CONTROLS_DISPLAY = DES_CONTROLS_MOVES + "\n\n" + DES_CONTROLS_CUBE_CAMERA + "\n" + DES_CONTROLS_AUX;
//    public static final String DES_CONTROLS_ALL = DES_CONTROLS_MOVES + "\n\n" + DES_CONTROLS_CUBE_CAMERA + "\n" + DES_CONTROLS_AUX + "\n" + DES_CONTROLS_AUX2;
//
//    @Override
//    public void draw() {
//        background(GLConfig.BG.getRGB());       // todo image and lights, hud
//
//        // Cube
//        pushMatrix();
//
//        final Point3DF o = cubeOrigin(width, height, getN());
//        final float curLevitation = updateLevitation();
//
//        translate(o.x, o.y + curLevitation, o.z);
//        if (!mFreeCam) {
////            camera(o.x * sin(radians(32)), o.y * sin(radians(-72)), o.y / tan(radians(32)), 0, -curLevitation, 0, 0, 1,0);
//
//        camera(o.x * 0.53f, o.y * -0.95f, o.y * 1.6f, 0, -curLevitation, 0, 0, 1, 0);
//
//        }
//
////        spotLight(255, 255, 255, 0, 0, 0, 1, 0, 0, radians(60), 1);
////        spotLight(255, 255, 255, 0, 0, 0, -1, 0, 0, radians(60), 1);
////        spotLight(255, 255, 255, 0, 0, 0, 0, 1, 0, radians(60), 1);
////        spotLight(255, 255, 255, 0, 0, 0, 0, -1, 0, radians(60), 1);
////        spotLight(255, 255, 255, 0, 0, 0, 0, 0, 1, radians(60), 1);
////        spotLight(255, 255, 255, 0, 0, 0, 0, 0, -1, radians(60), 1);
//
//        if (GLConfig.CUBE_DRAW_AXISES) {
//            pushStyle();
//            stroke(GLConfig.COLOR_AXIS.getRGB());
//            strokeWeight(2);
//
//            final float plen = Math.max(width, height);
//            final float nlen = GLConfig.CUBE_DRAW_ONLY_POSITIVE_AXISES? 0: plen;
//
//            line(-nlen, 0, 0, plen, 0, 0);         // X
//            line(0, nlen, 0, 0, -plen, 0);       // Y (invert)
//            line(0, 0, -nlen, 0, 0, plen);        // Z
//            popStyle();
//        }
//
//        final float scale = cubeScale(width, height, getN()) * getCubeZoom();
//        scale(scale * (GLConfig.CUBE_INVERT_X? -1: 1), scale * (GLConfig.CUBE_INVERT_Y? -1: 1), scale * (GLConfig.CUBE_INVERT_Z? -1: 1));
//        cubeGL.draw(this);
//        popMatrix();
//
//
//        // HUD
//        if (cam != null) {
//            cam.beginHUD();
//        }
//
//        final float h_offset = width * 0.009f;
//        final float v_offset = height / 96f;
//
//        final float cameraModeTextSize = getTextSize(GLConfig.CAMERA_MODE_TEXT_SIZE);
//
//        // Camera
//        if (GLConfig.SHOW_CAMERA_MODE) {
//            pushStyle();
//            fill(GLConfig.FG_CAMERA_MODE.getRGB());
//            textFont(pdSansMedium, cameraModeTextSize);
//            textAlign(LEFT, TOP);
//            text(GLConfig.getCameraModeText(mFreeCam, "V"), h_offset, v_offset);
//            popStyle();
//        }
//
//        // Lock
//        if (GLConfig.SHOW_CUBE_LOCKED) {
//            final String lockText = GLConfig.getCubeLockedText(cubeLocked());
//            if (Util.notEmpty(lockText)) {
//                pushStyle();
//                fill(GLConfig.FG_CUBE_LOCKED.getRGB());
//                textFont(pdSansMedium, getTextSize(GLConfig.CUBE_LOCKED_TEXT_SIZE));
//                textAlign(RIGHT, TOP);
//                text(lockText, width - h_offset, v_offset);
//                popStyle();
//            }
//        }
//
//        // Controls
//        if (controlsShown()) {
//            pushStyle();
//            textAlign(LEFT, TOP);
//
//            final float titleTextSize = getTextSize(GLConfig.CONTROLS_DES_TITLE_TEXT_SIZE);
//            final float titleY = cameraModeTextSize + v_offset * 3;
//            fill(GLConfig.FG_CONTROLS_DES_TITLE.getRGB());
//            textFont(pdSansMedium, titleTextSize);
//            text("CONTROLS", h_offset, titleY);
//
//            fill(GLConfig.FG_CONTROLS_DES.getRGB());
//            textFont(pdSans, getTextSize(GLConfig.CONTROLS_DES_TEXT_SIZE));
//            text(DES_CONTROLS_DISPLAY, h_offset, titleY + titleTextSize + v_offset * 1.5f);
//            popStyle();
//        }
//
//
//        final float statusTextSize = getTextSize(GLConfig.STATUS_TEXT_SIZE);
//
//        // Status bar
//        if (GLConfig.SHOW_STATUS) {
//            final String statusText = getStatusText();
//            if (Util.notEmpty(statusText)) {
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
//                text(GLConfig.getLastMoveText(lastMove, "CTRL-Z"), h_offset, height - statusTextSize - v_offset * 2);
//                popStyle();
//            }
//        }
//
//        // Cur Move (MIDDLE)
//        if (GLConfig.SHOW_CUR_MOVE) {
//            final Move curMove = cubeGL.getRecentRunningMove();
//            if (curMove != null) {
//                pushStyle();
//                fill(GLConfig.FG_CUR_MOVE.getRGB());
//                textSize(getTextSize(GLConfig.CUR_MOVE_TEXT_SIZE));
//                textAlign(CENTER, BOTTOM);
//                text(curMove.toString(), width / 2f, height - statusTextSize - v_offset * 2);
//                popStyle();
//            }
//        }
//
//
//        // 1. Status bar (at bottom)
////        if (mSolving) {
////
////        }
////
////        final String text = mSolving? "Solving...": mCurMoveText.isEmpty()? mCurrentSolutionText != null && !mCurrentSolutionText.isEmpty()? "Solution: " + mCurrentSolutionText: "": "Move: " + mCurMoveText;
////        if (!text.isEmpty()) {
////            pushStyle();
////            fill(color(GLConfig.FG.getRGB()));
////            final float textSize = Math.min(width, height) / 22f;
////            final float textOffset = textSize * 1.2f;
////            textSize(textSize);
////            text(text, textOffset, height - textOffset);
////            popStyle();
////        }
//
//        if (cam != null) {
//            cam.endHUD();
//        }
//    }
//
//
//    protected void onResized(int w, int h) {
//        considerReCreateCam();
//        resetLevitationAnimator();
//    }
//
//
//
//    public static boolean shouldRollOverRotatingCube(@NotNull KeyEvent event) {
//        return event.isControlDown();
//    }
//
//
//    public void setCubeZoom(float zoom) {
//        if (mFreeCam)
//            return;
//        cubeZoom = constrain(zoom, GLConfig.CUBE_ZOOM_MIN, GLConfig.CUBE_ZOOM_MAX);
//    }
//
//    public float getCubeZoom() {
//        return mFreeCam? 1: cubeZoom;
//    }
//
//    public void incCubeZoom() {
//        setCubeZoom(cubeZoom + GLConfig.cubeZoomIncStep(cubeZoom));
//    }
//
//    public void decCubeZoom() {
//        setCubeZoom(cubeZoom - GLConfig.cubeZoomDecStep(cubeZoom));
//    }
//
//    public void resetCubeZoom() {
//        setCubeZoom(1);
//    }
//
//    public void setShowControls(boolean showControls) {
//        mShowControls = showControls;
//    }
//
//    public void toggleShowControls() {
//        setShowControls(!mShowControls);
//    }
//
//    public boolean controlsShown() {
//        return GLConfig.SHOW_CONTROLS_DES && mShowControls;
//    }
//
//    @NotNull
//    private static Move createMove(@NotNull Axis axis, boolean shift, boolean ctrl, boolean alt) {
//        int[] layers = Move.LAYERS_0;
//        final int q;
//
//        if (alt) {
//            q = Move.QUARTERS_2;
//        } else {
//            q = shift? Move.QUARTERS_ANTICLOCKWISE: Move.QUARTERS_CLOCKWISE;
//            if (ctrl) {
//                layers = Move.LAYERS_0_1;
//            }
//        }
//
//        return new Move(axis, q, layers);
//    }
//
//    @NotNull
//    private static Move createMove(@NotNull Axis axis, @NotNull KeyEvent event) {
//        return createMove(axis, event.isShiftDown(), event.isControlDown(), event.isAltDown());
//    }
//
//
//    /* Cube Moves */
//
//    public void applyMove(@NotNull Move move, boolean saveInStack, boolean now) {
//        if (cubeLocked())
//            return;
//        cubeGL.applyMove(move, saveInStack, now);
//    }
//
//    public void applyMove(@NotNull Move move, boolean saveInStack) {
//        if (cubeLocked())
//            return;
//        cubeGL.applyMove(move, saveInStack);
//    }
//
//    public void applyMove(@NotNull Move move) {
//        if (cubeLocked())
//            return;
//        cubeGL.applyMove(move);
//    }
//
//    public boolean undoLastMove(boolean now) {
//        if (cubeLocked())
//            return false;
//        return cubeGL.undoLastMove(now);
//    }
//
//    public boolean undoLastMove() {
//        if (cubeLocked())
//            return false;
//        return cubeGL.undoLastMove();
//    }
//
//    public void applySequence(@NotNull List<Move> sequence, boolean saveInStack, boolean now) {
//        if (cubeLocked())
//            return;
//        cubeGL.applySequence(sequence, saveInStack, now);
//    }
//
//    public void applySequence(@NotNull List<Move> sequence, boolean saveInStack) {
//        if (cubeLocked())
//            return;
//        cubeGL.applySequence(sequence, saveInStack);
//    }
//
//    public void applySequence(@NotNull List<Move> sequence) {
//        if (cubeLocked())
//            return;
//        cubeGL.applySequence(sequence);
//    }
//
//    public void scramble(int moves, boolean saveInStack, boolean now) {
//        if (cubeLocked())
//            return;
//        cubeGL.scramble(moves, saveInStack, now);
//    }
//
//    public void scramble(int moves, boolean saveInStack) {
//        if (cubeLocked())
//            return;
//        cubeGL.scramble(moves, saveInStack);
//    }
//
//    public void scramble(int moves) {
//        if (cubeLocked())
//            return;
//        cubeGL.scramble(moves);
//    }
//
//    public void scramble() {
//        if (cubeLocked())
//            return;
//        cubeGL.scramble();
//    }
//
//    public void rotateCubeX(int quarters) {
//        if (cubeLocked())
//            return;
//        cubeGL.rotateX(quarters, true);
//    }
//
//    public void rotateCubeY(int quarters) {
//        if (cubeLocked())
//            return;
//        cubeGL.rotateY(quarters, true);
//    }
//
//    public void rotateCubeZ(int quarters) {
//        if (cubeLocked())
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
//
//    public void finishAllMoves(boolean cancel) {
//        if (cubeLocked())
//            return;
//        cubeGL.finishAllMoves(cancel);
//    }
//
//    @Override
//    public void keyPressed(KeyEvent event) {
//        super.keyPressed(event);
//
////        if (mFreeCam)           // TODO
////            return;
//
//        final char key = event.getKey();
//        final int keyCode = event.getKeyCode();
//
////        println("Keyevent-> key: " + key + ", code: " + keyCode + ", ctrl: " + event.isControlDown() + ", alt: " + event.isAltDown());
//
//        switch (keyCode) {
//            case java.awt.event.KeyEvent.VK_UP -> rotateCubeUp();
//            case java.awt.event.KeyEvent.VK_DOWN -> rotateCubeDown();
//            case java.awt.event.KeyEvent.VK_LEFT -> {
//                if (shouldRollOverRotatingCube(event)) {
//                    rollCubeLeft();
//                } else {
//                    rotateCubeLeft();
//                }
//            } case java.awt.event.KeyEvent.VK_RIGHT -> {
//                if (shouldRollOverRotatingCube(event)) {
//                    rollCubeRight();
//                } else {
//                    rotateCubeRight();
//                }
//            }
//
//            case java.awt.event.KeyEvent.VK_U -> applyMove(createMove(Axis.Y, event));
//            case java.awt.event.KeyEvent.VK_R -> applyMove(createMove(Axis.X, event));
//            case java.awt.event.KeyEvent.VK_F -> applyMove(createMove(Axis.Z, event));
//            case java.awt.event.KeyEvent.VK_D -> applyMove(createMove(Axis.Y_N, event));
//            case java.awt.event.KeyEvent.VK_L -> applyMove(createMove(Axis.X_N, event));
//            case java.awt.event.KeyEvent.VK_B -> applyMove(createMove(Axis.Z_N, event));
//
//            case java.awt.event.KeyEvent.VK_Z -> {
//                if (event.isControlDown()) {
//                    undoLastMove();
//                }
//            }
//
//            case java.awt.event.KeyEvent.VK_SPACE -> onSpacePressed(event);
//            case java.awt.event.KeyEvent.VK_S -> solve();
//            case java.awt.event.KeyEvent.VK_V -> toggleFreeCam();
//            case java.awt.event.KeyEvent.VK_A -> finishAllMoves(event.isShiftDown());
//            case java.awt.event.KeyEvent.VK_C -> toggleShowControls();
//            case java.awt.event.KeyEvent.VK_Q -> {
//                if (event.isShiftDown()) {
//                    resetCube();
//                } else {
//                    resetCubeZoom();
//                }
//            }
//
//            case java.awt.event.KeyEvent.VK_DEAD_CEDILLA, java.awt.event.KeyEvent.VK_PLUS -> incCubeZoom();
//            case java.awt.event.KeyEvent.VK_DEAD_OGONEK, java.awt.event.KeyEvent.VK_MINUS -> decCubeZoom();
//        }
//
//
////        if (!updateMoveText(event)) {
////            final char key = event.getKey();
////
////            // Camera
////            switch (key) {
////                case 'x' -> rotateCamByQuerters(-1, 0, 0);
////                case 'X' -> rotateCamByQuerters(1, 0, 0);
////                case 'y' -> rotateCamByQuerters(0, -1, 0);
////                case 'Y' -> rotateCamByQuerters(0, 1, 0);
////                case 'z' -> rotateCamByQuerters(0, 0, -1);
////                case 'Z' -> rotateCamByQuerters(0, 0, 1);
////
////                case ' ' -> onSpacePressed(event);
////                case 's' -> solve();
////            }
////        }
//    }
//
//
//
//    private void onSpacePressed(@NotNull KeyEvent event) {
//       scramble();
//    }
//
//
//
//
//    /* Levitation */
//
//    private void resetLevitationAnimator() {
//        if (mLevitationAnimator != null) {
//            mLevitationAnimator.finish(true);
//            mLevitationAnimator = null;
//        }
//
//        mLevitationAnimator = GLConfig.createLevitationAnimator(height);
//        if (mLevitationAnimator != null) {
//            mLevitationAnimator.start();
//        }
//    }
//
//    private float updateLevitation() {
//        final float l;
//        if (mLevitationAnimator != null) {
//            l = mLevitationAnimator.updateAndGetCurrentValue();
//            if (mLevitationAnimator.isFinished()) {
//                mLevitationAnimator = mLevitationAnimator.reverse();
//                mLevitationAnimator.start();
//            }
//        } else {
//            l = 0;
//        }
//
//        return l;
//    }
//
//
////    protected float levitationHeight() {
////        return height / 110f;
////    }
////
////    protected float levitationSpeed(float levitationHeight) {
////        return levitationHeight / 55;
////    }
////
////    private void updateLevitation() {
////        final float lh = levitationHeight();
////        curLevitation += (levitationSpeed(lh) * levitationSpeedMultiplier);
////        if (curLevitation >= lh) {
////            levitationSpeedMultiplier = -1;     // reverse
////            curLevitation = lh;
////        } else if (curLevitation <= 0) {
////            levitationSpeedMultiplier = 1;
////            curLevitation = 0;
////        }
////    }
//
//
//
//    /* Camera */
//
//    @NotNull
//    private PeasyCam createCam(@Nullable float[] rotations) {
//        final Point3DF o = cubeOrigin(width, height, getN());
//        final PeasyCam cam = new PeasyCam(this, o.x, o.y, o.z, o.y / tan(radians(26)));
//
////        camera(o.x * sin(radians(30)), o.y * sin(radians(-72)), o.y / tan(radians(32)), 0, -curLevitation, 0, 0, 1,0);
//
//        if (rotations == null) {
//            rotations = GLConfig.INITIAL_CAM_ROTATIONS;
//        }
//
//        cam.setRotations(rotations[0], rotations[1], rotations[2]);
//        return cam;
//    }
//
//    @NotNull
//    private PeasyCam createCam() {
//        return createCam(null);
//    }
//
//    private void considerReCreateCam() {
//        if (!mFreeCam)
//            return;
//
//        float[] rotations = null;
//        if (cam != null) {
//            cam.setActive(false);
//            rotations = cam.getRotations();
//        }
//
//        cam = createCam(rotations);
//    }
//
//    private void setFreeCamInternal(boolean freeCam) {
//        mFreeCam = freeCam;
//
//        if (freeCam) {
//            considerReCreateCam();
//        } else if (cam != null) {
//            cam.setActive(false);
////            cam.reset();
////            cam = null;     // release
//        }
//    }
//
//    public void serFreeCam(boolean freeCam) {
//        if (mFreeCam == freeCam)
//            return;
//        setFreeCamInternal(freeCam);
//    }
//
//    public void toggleFreeCam() {
//        setFreeCamInternal(!mFreeCam);
//    }
//
//    public boolean isCamFree() {
//        return mFreeCam;
//    }
//
//
//    //
////    private void rotateCamTo(float radX, float radY, float radZ, long animationMs) {
//////        if (animationMs > 0) {
//////            final float[] center = cam.getLookAt();
//////            final CameraState state = new CameraState(new Rotation(RotationOrder.XYZ, radX, radY, radZ), new Vector3D(center[0], center[1], center[2]), cam.getDistance());
//////            cam.setState(state, animationMs);
//////        } else {
//////            cam.setRotations(radX, radY, radZ);
//////        }
////    }
////
////    private void rotateCamBy(float radX, float radY, float radZ, long animationMs) {
//////        final float[] r = cam.getRotations();
//////        final float rx = r[0] + radX, ry = r[1] + radY, rz = r[2] + radZ;
//////        rotateCamTo(rx, ry, rz, animationMs);
////    }
////
////    private void rotateCamByQuerters(int qx, int qy, int qz) {
//////        rotateCamBy(qx * Util.HALF_PI, qy * Util.HALF_PI, qz * Util.HALF_PI, GLConfig.CAMERA_QUARTER_ROTATION_DURATION_MS);
////    }
////
//
//
//
//    /* Moves, Shuffle and solve */
//
//
////    protected void onMoveTextApplied(@NotNull String moveText, @NotNull Move move) {
////        mCurMoveText = "";      // reset
////        mCurrentSolution = null;
////        mCurrentSolutionText = null;
////    }
////
////    protected void onMoveTextInvalid(@NotNull String moveText, @Nullable Move move, @Nullable Move.ParseException exc) {
////
////    }
////
////    private boolean applyMoveText(@NotNull String moveText) {
////        try {
////            final Move move = Move.parseMove(moveText);
//////            if (move.relLayerIndex < 0 || move.relLayerIndex >= getN()) {
//////                onMoveTextInvalid(moveText, move, null);
//////                return false;
//////            }
////
////            cubeGL.applyMove(move);
////            onMoveTextApplied(moveText, move);
////            return true;
////        } catch (Move.ParseException exc) {
////            Util.e(exc);
////            onMoveTextInvalid(moveText, null, exc);
////        }
////
////
////
////            float[] pos = cam.getPosition();
////            float[] look = cam.getLookAt();
////            float[] rotations = cam.getRotations();
////            rotations[1] = -rotations[1];       // invert y
////
////            pos[0] = pos[0] - look[0];
////            pos[1] = -(pos[1] - look[1]);       // invert y
////            pos[2] = pos[2] - look[2];
////            println("Camera Position: " + Arrays.toString(pos) + ", Rotations: " + Arrays.toString(rotations));
////
////            final float max = Math.max(Math.max(Math.abs(pos[0]), Math.abs(pos[1])), Math.abs(pos[2]));
////            final Axis x, y, z;
////            if (max == Math.abs(pos[0])) {      // Facing RIGHT or LEFT
////                final int q = roundToQuarters(rotations[0]);
////                if (pos[0] > 0) {       // RIGHT
////                    z = Axis.X;
////                } else {                // LEFT
////                    z = Axis.X_N;
////                }
////
////                switch (Axis.Y.rotate(Axis.X, q)) {
////                    case Z_N -> {
////                        y = Axis.Z;
////                        x = Axis.Y;
////                    } case Z -> {
////                        y = Axis.Z_N;
////                        x = Axis.Y_N;
////                    } case Y_N -> {
////                        x = Axis.Z;
////                        y = Axis.Y_N;
////                    } default -> {
////                        y = Axis.Y;
////                        x = Axis.Z_N;
////                    }
////                }
////            } else if (max == Math.abs(pos[1])) {       // Facing UP or DOWN
////                final int q = roundToQuarters(rotations[1]);
////                if (pos[1] > 0) {       // UP
////                    z = Axis.Y;
////                } else {                // DOWN
////                    z = Axis.Y_N;
////                }
////
////                switch (Axis.X.rotate(Axis.Y, q)) {
////                    case Z_N -> {
////                        y = Axis.X;
////                        x = Axis.Z;
////                    } case Z -> {
////                        y = Axis.X_N;
////                        x = Axis.Z_N;
////                    } case X_N -> {
////                        y = Axis.Z;
////                        x = Axis.X_N;
////                    } default -> {
////                        y = Axis.Z_N;
////                        x = Axis.X;
////                    }
////                }
////            } else {            // Facing FRONT or BACK
////                final int q = roundToQuarters(rotations[2]);
////                if (pos[2] > 0) {       // FRONT
////                    z = Axis.Z;
////                } else {                // BACK
////                    z = Axis.Z_N;
////                }
////
////                switch (Axis.X.rotate(Axis.Z, q)) {
////                    case Y_N -> {
////                        y = Axis.X_N;
////                        x = Axis.Y;
////                    } case Y -> {
////                        y = Axis.X;
////                        x = Axis.Y_N;
////                    } case X_N -> {
////                        y = Axis.Y_N;
////                        x = Axis.X_N;
////                    } default -> {
////                        y = Axis.Y;
////                        x = Axis.X;
////                    }
////                }
////            }
////
////            println("Axis -> X: " + x + ", Y: " + y + ", Z: " + z);
////
////            final Axis a = switch (move.axis) {
////                case X -> x;
////                case X_N -> x.invert();
////                case Y -> y;
////                case Y_N -> y.invert();
////                case Z -> z;
////                case Z_N -> z.invert();
////            };
////
////            if (a != move.axis) {
////                move = move.withAxis(a);
////            }
////
////
////
////            float[] rotations = cam.getRotations();
////            final Axis tz = Axis.X.rotate(Axis.X, roundToQuarters(rotations[0]))
////                    .rotate(Axis.Y, roundToQuarters(-rotations[1]))
////                    .rotate(Axis.Z, roundToQuarters(rotations[2]));
////
////            println("Transformed X = " + tz);
////
////            final Axis inAxis = move.axis.isY()? move.axis.invert(): move.axis;
////            Axis outAxis = inAxis.rotate(Axis.X, roundToQuarters(rotations[0]))
////                    .rotate(Axis.Y_N, roundToQuarters(rotations[1]))
////                    .rotate(Axis.Z, roundToQuarters(rotations[2]));
////
////            if (!inAxis.isY()) {
////                outAxis = outAxis.invert();
////            }
////
////
////            outAxis = outAxis.invert();
////
////
////            println("Axis -> Original: " + move.axis + ", Input: " + inAxis + ", Output: " + outAxis);
////            if (move.axis != outAxis) {
////                Move newMove = move.withAxis(outAxis);
////
////                if (move.axis.isPositive() != newMove.axis.isPositive()) {
////                    newMove = newMove.reverse();
////                }
////
////                move = newMove;
////            }
//
////        return false;
////    }
//
////    private boolean updateMoveText(@NotNull KeyEvent event) {
////        if (mSolving)
////            return false;
////
////        final String moveText = mCurMoveText;
////        final char key = event.getKey();
////        final int mvlen = moveText.length();
////
////        // Backspace
////        if (key == BACKSPACE) {
////            if (mvlen == 0)
////                return false;
////            mCurMoveText = moveText.substring(0, mvlen - 1);
////            return true;
////        }
////
////        // Enter
////        if (key == ENTER || key == RETURN) {
////            if (mvlen == 0)
////                return false;
////
////            final boolean applied = applyMoveText(moveText);
////            println("Move " + moveText + " " + (applied? "applied": "is invalid"));
////            mCurMoveText = "";
////            return applied;
////        }
////
////        boolean add = false;
////        if (mvlen == 0) {
////            add = Move.isValidCommandStart(key);
////        } else {
////            final boolean digit = Character.isDigit(key);
////            if (digit) {
////                final boolean valid = key != '0' || Character.isDigit(moveText.charAt(mvlen - 1));
////                if (valid) {
////                    StringBuilder num = new StringBuilder(mvlen);
////                    for (int i=mvlen - 1; i >= 0; i--) {
////                        final char c = moveText.charAt(i);
////                        if (Character.isDigit(c)) {
////                            num.append(c);
////                        } else {
////                            break;
////                        }
////                    }
////
////                    if (num.length() == 0) {
////                        add = true;
////                    } else {
////                        try {
////                            int layer = Integer.parseInt(num.append(key).toString());
////                            add = layer > 0 && layer < getN();
////                            println("Input Layer: " + layer + ", " + (add? "VALID": "OUT OF BOUNDS"));
////                        } catch (NumberFormatException ignored) {
////                            add = true;
////                        }
////                    }
////                }
////            } else if (Move.isValidCommandStart(moveText + key)) {
////                add = true;
////            }
////        }
////
////        if (add) {
////            mCurMoveText = moveText + key;
////            return true;
////        }
////
////        return false;
////    }
//
//
//
//    private void solve() {
//        if (mSolving)
//            return;
//
//        if (getN() != 3) {
//            Util.v("Solver only works for 3*3 cube");
//            return;
//        }
//
//        Solver.Solution curSolution = mCurSolution;
//        invalidateColution();
//        if (!(curSolution == null || curSolution.isEmpty())) {
//            applySequence(curSolution.moves);
//            return;
//        }
//
//        finishAllMoves(false);
//        thread("doSolveWorker");
//    }
//
//    public void doSolveWorker() {
//        if (mSolving)
//            return;
//
//        mSolving = true;
//        Util.v("Solving state: " + cubeRepr2D());
//
//        try {
//            final Solver.Solution solution = Solver.solve3(getCube());
//            mCurSolution = solution;
//            if (solution.isEmpty()) {
//                Util.v("Already solved!");
//            } else {
//                final Long msTaken = solution.getMsTaken();
//                Util.v("Solution: " + solution.getSequence() + (msTaken != null && msTaken > 0? ", time taken: " + msTaken + "ms": ""));
//            }
//        } catch (Exception exc) {
//            Util.e("Solver Failed: " + exc.getMessage());
//            invalidateColution();
//        }
//
//        mSolving = false;
//    }
//
//    @Override
//    public void onMoveApplied(@NotNull Move move, int cubiesAffected, boolean saved) {
//        invalidateColution();
//    }
//
//
//
//    public static void main(String[] args) {
//        final Main main = new Main();
//        PApplet.runSketch(concat(new String[] {main.getClass().getName()}, args), main);
//
//        println(".................. Cube RC .............\n");
//        println("Commands\n -> n: set cube dimension\n -> m: move sequence\n -> sc: scramble\n -> s: Solve (Only for 3*3 cube)\n -> exit\\quit: quit\n");
//
//        Scanner sc = new Scanner(System.in);
//        boolean running = true;
//
//        // TODO: options with startsWith instead with switch
//
//        do {
//            print("cube:RC> ");
//            final String command = sc.nextLine();
//            if (command.isEmpty())
//                continue;
//
//            switch (command) {
//                case "n" -> {
//                    print("cube:RC\\dim> Enter cube dimension (must be >= 2): ");
//                    try {
//                        final int n = sc.nextInt();
//                        if (n < 2) {
//                            Util.e("cube dimension must be > 2");
//                        } else {
//                            main.setN(n);
//                        }
//                    } catch (InputMismatchException ignored) {
//                        Util.e("An integer is required!");
//                    }
//                } case "m" -> {
//                    print("cube:RC\\move> Type move sequence: ");
//                    try {
//                        final String sequence = sc.nextLine();
//                        if (!(sequence == null || sequence.isEmpty())) {
//                            try {
//                                List<Move> moves = Move.parseSequence(sequence);
//                                if (CollectionUtil.notEmpty(moves)) {
//                                    main.applySequence(moves);
//                                }
//                            } catch (Move.ParseException e) {
//                                Util.e(e.getMessage());
//                            }
//                        }
//                    } catch (InputMismatchException ignored) {
//                        Util.e("Invalid moves sequence");
//                    }
//                } case "sc" -> {
//                    print("cube:RC\\scramble> Enter scramble moves: ");
//                    try {
//                        final int n = sc.nextInt();
//                        if (n < 1) {
//                            Util.e("scramble moves must be greater than 0, given: " + n);
//                        } else {
//                            main.scramble(n);
//                        }
//                    } catch (InputMismatchException ignored) {
//                        Util.e("An integer is required!");
//                    }
//                } case "s" -> {
//                    main.solve();
//                } case "exit", "quit" -> {
//                    running = false;
//                    main.exit();
//                } default -> {
//                    Util.e("Invalid command <" + command + ">");
//                }
//            }
//
//            sc = new Scanner(System.in);
//        } while (running);
//    }
//
//
//}
