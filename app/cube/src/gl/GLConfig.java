package gl;

import gl.animation.*;
import gl.animation.interpolator.AccelerateDecelerateInterpolator;
import gl.animation.interpolator.Interpolator;
import math.geometry.Rotation;
import math.geometry.RotationOrder;
import model.cube.CubeI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sound.MidiNotePlayer;
import util.U;

import java.awt.*;

public class GLConfig {

    public static final float FRAME_RATE = 120;
    public static final boolean DEFAULT_WINDOW_IN_SCREEN_CENTER = true;

    public static final boolean DEFAULT_LOCK_CUBE_WHILE_SOLVING = true;

    public static final boolean CUBE_INVERT_X = false;
    public static final boolean CUBE_INVERT_Y = true;
    public static final boolean CUBE_INVERT_Z = false;
    public static final boolean DEFAULT_CUBE_DRAW_AXES = false;
    public static final boolean DEFAULT_CUBE_DRAW_AXES_ONLY_POSITIVE = false;      // only when drawing axes

    /* Sticker (weights as fractions of side length) */
    public static final boolean CUBIE_DRAW_INTERNAL_FACES = false;

    public static final boolean DEFAULT_HUD_ENABLED = true;
    public static final boolean DEFAULT_SHOW_KEY_BINDINGS = true;

    public static final float CUBIE_STICKER_STROKE_WEIGHT = 0.075f;
    public static final float CUBIE_STICKER_CORNERS_RADIUS_WEIGHT = 0.08f;
    public static final float CUBIE_STICKER_ELEVATION_WEIGHT = 0.0005f;

    /* Sound */
    public static final boolean DEFAULT_SOUND_ENABLED = true;
    public static final boolean DEFAULT_POLY_RHYTHM_ENABLED = MidiNotePlayer.DEFAULT_POLY_RHYTHM_ENABLED;
    public static final float MIDI_NOTE_MIN = 80;
    public static final float MIDI_NOTE_MAX = 100;
    public static final float MIDI_NOTE_STEP = 1;

    /* Camera and Scale */

    public static final boolean DEFAULT_FREE_CAMERA = true;

    @NotNull
    public static final Rotation LOCKED_CAMERA_ROTATIONS = new Rotation(
            RotationOrder.XYZ,
            0.5077301f /* radians(29.0907919) */,
            0.41399524f /* radians(23.72018) */,
            -0.27235895f /* radians(-15.6050183) */
    );

    @NotNull
    public static final Rotation INITIAL_CAMERA_ROTATIONS = LOCKED_CAMERA_ROTATIONS;

//    public static final long CAMERA_QUARTER_ROTATION_DURATION_MS = 400;
    public static final long CAMERA_RESET_ANIMATION_MILLS = 300;
    public static final long CAMERA_ROTATIONS_ANIMATION_MILLS = 250;        // set to 0 for no animation

    public static final float CUBE_DRAW_SCALE_MIN = 0.2f;
    public static final float CUBE_DRAW_SCALE_MAX = 4f;
    public static final float CUBE_DRAW_SCALE_DEFAULT = 1;

    public static final float CUBE_DRAW_SCALE_PERCENT__STEP_CONTINUOUS = 0.1f;    // 0.1%
    public static final float CUBE_DRAW_SCALE_PERCENT__STEP_DISCRETE = 5;     // 5%

    public static float cubeDrawScaleToPercent(float cubeDrawScale) {
        return U.norm(U.constrain(cubeDrawScale, GLConfig.CUBE_DRAW_SCALE_MIN, GLConfig.CUBE_DRAW_SCALE_MAX), GLConfig.CUBE_DRAW_SCALE_MIN, GLConfig.CUBE_DRAW_SCALE_MAX) * 100;
    }

    public static float percentToCubeDrawScale(float percent) {
        return U.lerp(GLConfig.CUBE_DRAW_SCALE_MIN, GLConfig.CUBE_DRAW_SCALE_MAX, U.constrain_0_1(percent / 100));
    }

    public static float stepCubeDrawScalePercent(float currentDrawScalePercent, boolean continuous, boolean increment) {
        return currentDrawScalePercent + ((increment? 1: -1) * (continuous? CUBE_DRAW_SCALE_PERCENT__STEP_CONTINUOUS : CUBE_DRAW_SCALE_PERCENT__STEP_DISCRETE));
    }

    /* Move */

    public static final boolean DEFAULT_ANIMATE_MOVES = true;
    public static final boolean DEFAULT_MOVE_USE_NORMALIZED_QUARTERS = false;
    public static final Interpolator DEFAULT_MOVE_ANIMATION_INTERPOLATOR = AccelerateDecelerateInterpolator.getSingleton();

    public static final int MOVE_QUARTER_DURATION_MS_MIN = 30;
    public static final int MOVE_QUARTER_DURATION_MS_MAX = 2500;
    public static final int MOVE_QUARTER_DURATION_MS_DEFAULT = 280;

    private static final float MOVE_QUARTER_DURATION_PERCENT__STEP_CONTINUOUS = 0.1f;        // percent
    private static final float MOVE_QUARTER_DURATION_PERCENT__STEP_DISCRETE = 5f;        // percent

    public static int constraintMoveQuarterDurationMs(int durationMs) {
        return U.constrain(durationMs, MOVE_QUARTER_DURATION_MS_MIN, MOVE_QUARTER_DURATION_MS_MAX);
    }

    public static float moveQuarterDurationMsToPercent(int durationMs) {
        return U.norm(constraintMoveQuarterDurationMs(durationMs), MOVE_QUARTER_DURATION_MS_MIN, MOVE_QUARTER_DURATION_MS_MAX) * 100;
    }

    public static int percentToMoveQuarterDurationMs(float percent) {
        return (int) U.lerp(MOVE_QUARTER_DURATION_MS_MIN, MOVE_QUARTER_DURATION_MS_MAX, U.constrain_0_1(percent / 100f));
    }

    public static float convertDurationAndSpeedPercent(float durationPercent) {
        return 100 - durationPercent;
    }

    public static float stepMoveQuarterDurationMsPercent(float currentPercent, boolean continuous, boolean increment) {
        float new_per = currentPercent + ((increment? 1: -1) * (continuous? MOVE_QUARTER_DURATION_PERCENT__STEP_CONTINUOUS: MOVE_QUARTER_DURATION_PERCENT__STEP_DISCRETE));
        return U.constrain_0_100(new_per);
    }

    public static int stepMoveQuarterDurationMs(int currentMs, boolean continuous, boolean increment) {
        final float cur_per = moveQuarterDurationMsToPercent(currentMs);
        final float new_per = stepMoveQuarterDurationMsPercent(cur_per, continuous, increment);
        return percentToMoveQuarterDurationMs(new_per);
    }


//    public static int moveQuarterDurationMsWithDeltaPercent(int durationMs, float deltaPercentage) {
//        return constraintMoveQuarterDurationMs((int) (durationMs + (Math.signum(deltaPercentage) * percentToMoveQuarterDurationMs(Math.abs(deltaPercentage)))));
//    }


    public static final boolean DEFAULT_APPLY_MOVE_NOW = true;      // single move
    public static final boolean DEFAULT_APPLY_SEQUENCE_NOW = false;
    public static final boolean DEFAULT_SCRAMBLE_NOW = false;
    public static final boolean DEFAULT_UNDO_LAST_MOVE_NOW = true;
    public static final boolean DEFAULT_ROTATE_NOW = true;

    /* Levitation */

    public static final boolean LEVITATION_ENABLED = true;
    public static final float LEVITATION_HEIGHT_FRACTION = 0.0095f;
    public static final long LEVITATION_DURATION_MS = 1400;
    public static final Interpolator LEVITATION_INTERPOLATOR = AccelerateDecelerateInterpolator.getSingleton();

    @Nullable
    public static FloatAnimator createLevitationAnimator(float height) {
        if (!LEVITATION_ENABLED)
            return null;

        final float extent = height * LEVITATION_HEIGHT_FRACTION * 0.5f;

        final FloatAnimator anim = new FloatAnimator(-extent, extent);
        anim.setInterpolator(LEVITATION_INTERPOLATOR);
        anim.setDurationMs(LEVITATION_DURATION_MS);

        return anim;
    }



    /* Colors */

    public static final Color BG_DARK = new Color(0, 0, 0, 255);
    public static final Color BG_MEDIUM = new Color(28, 28, 28, 255);
    public static final Color BG_LIGHT = new Color(45, 45, 45, 255);
    public static final Color BG_OVERLAY = new Color(0, 0, 0, 160);

    public static final Color FG_DARK = new Color(255, 255, 255, 255);
    public static final Color FG_MEDIUM = new Color(230, 230, 230, 255);
    public static final Color FG_LIGHT = new Color(200, 200, 200, 255);
    public static final Color COLOR_ACCENT = new Color(107, 196, 255, 255);
    public static final Color COLOR_ACCENT_HIGHLIGHT = new Color(255, 221, 83, 255);
    public static final Color COLOR_ERROR = new Color(255, 40, 40, 255);

    public static final Color COLOR_UP = new Color(255, 219, 77, 255);
    public static final Color COLOR_RIGHT = new Color(116, 255, 77, 255);
    public static final Color COLOR_FRONT = new Color(255, 47, 22, 255);
    public static final Color COLOR_DOWN = new Color(255, 255, 255, 255);
    public static final Color COLOR_LEFT = new Color(77, 157, 255, 255);
    public static final Color COLOR_BACK = new Color(255, 126, 29, 255);
    public static final Color COLOR_INTERNAL = new Color(43, 43, 43, 255);


//    public static final Color COLOR_RIGHT = new Color(255, 126, 29, 255);
//    public static final Color COLOR_FRONT = new Color(116, 255, 77, 255);
//    public static final Color COLOR_LEFT = new Color(255, 47, 22, 255);
//    public static final Color COLOR_BACK = new Color(77, 157, 255, 255);

    public static final Color COLOR_AXIS_X = new Color(246, 92, 92, 255);
    public static final Color COLOR_AXIS_Y = new Color(88, 250, 88, 255);
    public static final Color COLOR_AXIS_Z = new Color(95, 95, 241, 255);

    public static final int ALPHA_AXIS_CUBE = 160;
    public static final Color COLOR_AXIS_X_CUBE = new Color(U.withAlpha(COLOR_AXIS_X.getRGB(), ALPHA_AXIS_CUBE));
    public static final Color COLOR_AXIS_Y_CUBE = new Color(U.withAlpha(COLOR_AXIS_Y.getRGB(), ALPHA_AXIS_CUBE));
    public static final Color COLOR_AXIS_Z_CUBE = new Color(U.withAlpha(COLOR_AXIS_Z.getRGB(), ALPHA_AXIS_CUBE));

    @NotNull
    public static Color mapFaceCodeColor(int faceCode) {
        final Color color;

        if (faceCode == CubeI.FACE_UP) {
            color = COLOR_UP;
        } else if (faceCode == CubeI.FACE_RIGHT) {
            color = COLOR_RIGHT;
        } else if (faceCode == CubeI.FACE_FRONT) {
            color = COLOR_FRONT;
        } else if (faceCode == CubeI.FACE_DOWN) {
            color = COLOR_DOWN;
        } else if (faceCode == CubeI.FACE_LEFT) {
            color = COLOR_LEFT;
        } else if (faceCode == CubeI.FACE_BACK) {
            color = COLOR_BACK;
        } else {
            color = COLOR_INTERNAL;
        }

        return color;
    }


    public static float getTextSize(float width, float height, float size) {
        return Math.min(width, height) * size;
    }

    public static final float TEXT_SIZE_HUGE3 = 0.066f;
    public static final float TEXT_SIZE_HUGE2 = 0.062f;
    public static final float TEXT_SIZE_HUGE1 = 0.058f;
    public static final float TEXT_SIZE_LARGE3 = 0.034f;
    public static final float TEXT_SIZE_LARGE2 = 0.028f;
    public static final float TEXT_SIZE_LARGE1 = 0.026f;
    public static final float TEXT_SIZE_NORMAL = 0.023f;
    public static final float TEXT_SIZE_SMALL1 = 0.019f;
    public static final float TEXT_SIZE_SMALL2 = 0.0175f;
    public static final float TEXT_SIZE_SMALL3 = 0.016f;
    public static final float TEXT_SIZE_TINY = 0.014f;

    public static final float TEXT_SIZE_CONTROL_KEY_BINDING_LABEL = TEXT_SIZE_SMALL3;
    public static final Color FG_CONTROL_KEY_BINDING_LABEL = new Color(204, 178, 60, 255);

    public static final float TEXT_SIZE__STATUS_CONTROLS = TEXT_SIZE_SMALL2;
    public static final Color FG__STATUS_CONTROLS = FG_DARK;

    public static final float TEXT_SIZE_MAIN_CONTROLS_LABEL = TEXT_SIZE_SMALL3;
    public static final float TEXT_SIZE_MAIN_CONTROLS_KEY_LABEL = TEXT_SIZE_SMALL2;
    public static final float TEXT_SIZE_MAIN_CONTROLS_VALUE = TEXT_SIZE_SMALL1;
    public static final Color FG_MAIN_CONTROLS_LABEL = FG_MEDIUM;
    public static final Color FG_MAIN_CONTROLS_KEY_LABEL = FG_CONTROL_KEY_BINDING_LABEL;
    public static final Color FG_MAIN_CONTROLS_VALUE = COLOR_ACCENT_HIGHLIGHT;

    public static final boolean SHOW_CUR_MOVE = true;
    public static final float TEXT_SIZE_CUR_MOVE = TEXT_SIZE_HUGE1;
    public static final Color FG_CUR_MOVE = COLOR_ACCENT;


//    public static final boolean SHOW_SEC_STATUS = true;
//    public static final float SEC_STATUS_TEXT_SIZE = 0.018f;
//    public static final Color FG_SEC_STATUS = FG_DARK;

//    public static final boolean SHOW_LAST_MOVE = true;
//    public static final float LAST_MOVE_TEXT_SIZE = 0.02f;
//    public static final Color FG_LAST_MOVE = FG_DARK;
//
//    public static final boolean SHOW_STATUS = true;
//    public static final float STATUS_TEXT_SIZE = 0.024f;
//    public static final Color FG_STATUS_BAR = ACCENT;
//
//    public static final boolean SHOW_CAMERA_MODE = true;
//    public static final float CAMERA_MODE_TEXT_SIZE = 0.02f;
//    public static final Color FG_CAMERA_MODE = ACCENT_HIGHLIGHT;
//
//    public static final boolean SHOW_CUBE_STATE = true;
//    public static final float CUBE_STATE_TEXT_SIZE = 0.024f;
//    public static final Color FG_CUBE_STATE = ACCENT_HIGHLIGHT;
//
//    public static final float CONTROLS_DES_TITLE_TEXT_SIZE = 0.0195f;
//    public static final Color FG_CONTROLS_DES_TITLE = ACCENT;
//    public static final float CONTROLS_DES_TEXT_SIZE = 0.018f;
//    public static final Color FG_CONTROLS_DES = FG_MEDIUM;


//    public static final String STATUS_SOLVING = "Solving";
//    public static final String STATUS_ALREADY_SOLVED = "Solved";
//    public static final String STATUS_SOLUTION_SEQ = "Solution";
//    public static final String DOT = ".";

//    @Nullable
//    public static String getStatusText(float width, float height, float frameRate, int frameCount, boolean solving, @Nullable Solver.Solution solution, @Nullable String runSolutionKeyBinding) {
//        if (solving) {
//            final float rate = frameRate / 5;       // 5 = (dots per sec)
//            final float count = (frameCount % (rate * 4)) / rate;           // 4 = no of dots + 1
//            return STATUS_SOLVING + DOT.repeat((int) count);
//        }
//
//        if (solution != null) {
//            if (solution.isEmpty())
//                return STATUS_ALREADY_SOLVED;
//
//            final int maxCount = (int) (width / (getTextSize(width, height, STATUS_TEXT_SIZE) * 2)) - 10;
//            final String seq;
//            if (solution.moveCount() <= maxCount) {
//                seq = solution.getSequence();
//            } else {
//                seq = solution.getHeadSequence(maxCount) + " + " + Math.abs(solution.moveCount() - maxCount) + " moves";
//            }
//
//            return concatKeyBinding(STATUS_SOLUTION_SEQ, seq, runSolutionKeyBinding);
//        }
//
//        return null;
//    }
//
//    @Nullable
//    public static String getSecStatusText(float moveSpeedPercent) {
//        return "Speed: " + Format.nf001(moveSpeedPercent) + "%";
//    }
//
//    @NotNull
//    public static String concatKeyBinding(@NotNull String prefix, @Nullable String suffix, @Nullable String keyBinding) {
//        return prefix + (Format.isEmpty(keyBinding)? "": " [" + keyBinding + "]") + " : " + (Format.isEmpty(suffix)? "N/A": suffix);
//    }
//
//    @NotNull
//    public static String getLastMoveText(@NotNull Move lastMove, @Nullable String keyBinding) {
//        return concatKeyBinding("Last Move", lastMove.toString(), keyBinding);
//    }
//
//    @NotNull
//    public static String getCameraModeText(boolean freeCam, @Nullable String keyBinding) {
//        return concatKeyBinding("CAMERA", freeCam? "FREE (Use Mouse)": "CUBE (Use Arrows)", keyBinding);
//    }
//
//    @Nullable
//    public static String getCubeStateText(int n, boolean cubeLocked) {
//        return (cubeLocked? "LOCKED | ": "") + n + "x" + n;
//    }
}
