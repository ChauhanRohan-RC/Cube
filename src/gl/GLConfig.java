package gl;

import gl.animation.*;
import model.cube.CubeI;
import model.cubie.Move;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import processing.core.PApplet;
import solver.Solver;
import sound.MidiNotePlayer;
import util.Format;

import java.awt.*;

public class GLConfig {

    public static final float FRAME_RATE = 120;
    public static final boolean DEFAULT_WINDOW_IN_SCREEN_CENTER = true;

    public static final boolean DEFAULT_SOUND_ENABLED = true;
    public static final boolean DEFAULT_POLY_RHYTHM_ENABLED = MidiNotePlayer.DEFAULT_POLY_RHYTHM_ENABLED;

    public static final boolean CUBE_INVERT_X = false;
    public static final boolean CUBE_INVERT_Y = true;
    public static final boolean CUBE_INVERT_Z = false;
    public static final boolean CUBE_DRAW_AXES = false;
    public static final boolean CUBE_DRAW_ONLY_POSITIVE_AXES = true;      // only when drawing axes

    /* Sticker (weights as fractions of side length) */
    public static final boolean CUBIE_DRAW_INTERNAL_FACES = false;

    public static final float CUBIE_STICKER_STROKE_WEIGHT = 0.075f;
    public static final float CUBIE_STICKER_CORNERS_RADIUS_WEIGHT = 0.08f;
    public static final float CUBIE_STICKER_ELEVATION_WEIGHT = 0.0005f;

//    public static final float[] INITIAL_CAM_ROTATIONS = new float[] { 0.55448544f, 0.49118212f, -0.32216954f };
    public static final float[] INITIAL_CAM_ROTATIONS = new float[] { 0, 0, 0 };
//    public static final long CAMERA_QUARTER_ROTATION_DURATION_MS = 400;

    public static final float CUBE_ZOOM_MIN = 0.05f;
    public static final float CUBE_ZOOM_MAX = 5;

    public static float cubeZoomIncStep(float current) {
        return 0.01f;
    }

    public static float cubeZoomDecStep(float current) {
        return 0.01f;
    }

    /* Move */

    public static final boolean DEFAULT_ANIMATE_MOVES = true;
    public static final boolean DEFAULT_MOVE_USE_NORMALIZED_QUARTERS = false;
    public static final Interpolator DEFAULT_MOVE_INTERPOLATOR = Interpolator.ACCELERATE_DECELERATE;

    public static final int MOVE_QUARTER_DURATION_MS_MIN = 30;
    public static final int MOVE_QUARTER_DURATION_MS_MAX = 2500;
    public static final int MOVE_QUARTER_DURATION_MS_DEFAULT = 280;

    public static int constraintMoveQuarterDurationMs(int durationMs) {
        return PApplet.constrain(durationMs, MOVE_QUARTER_DURATION_MS_MIN, MOVE_QUARTER_DURATION_MS_MAX);
    }

    public static float moveQuarterDurationMsToPercent(int durationMs) {
        return PApplet.norm(constraintMoveQuarterDurationMs(durationMs), MOVE_QUARTER_DURATION_MS_MIN, MOVE_QUARTER_DURATION_MS_MAX) * 100;
    }

    public static int percentToMoveQuarterDurationMs(float percent) {
        return (int) PApplet.lerp(MOVE_QUARTER_DURATION_MS_MIN, MOVE_QUARTER_DURATION_MS_MAX, PApplet.constrain(percent / 100f, 0, 1));
    }

    public static final float MOVE_QUARTER_SPEED_PERCENT_CONTINUOUS_INCREMENT = 0.25f;        // percent

    public static float moveQuarterDurationMsIncPercent(float currentPercent) {
        return 5;
    }

    public static float moveQuarterDurationMsDecPercent(float currentPercent) {
        return 5;
    }

    public static float convertDurationAndSpeedPercent(float durationPercent) {
        return 100 - durationPercent;
    }

//    public static int moveQuarterDurationMsWithDeltaPercent(int durationMs, float deltaPercentage) {
//        return constraintMoveQuarterDurationMs((int) (durationMs + (Math.signum(deltaPercentage) * percentToMoveQuarterDurationMs(Math.abs(deltaPercentage)))));
//    }


    public static final boolean DEFAULT_APPLY_MOVE_NOW = true;      // single move
    public static final boolean DEFAULT_APPLY_SEQUENCE_NOW = false;
    public static final boolean DEFAULT_SCRAMBLE_NOW = false;
    public static final boolean DEFAULT_UNDO_MOVE_NOW = true;
    public static final boolean DEFAULT_ROTATE_NOW = true;



    /* Levitation */

    public static final boolean LEVITATION_ENABLED = true;
    public static final float LEVITATION_HEIGHT_FRACTION = 0.0095f;
    public static final long LEVITATION_DURATION_MS = 1400;
    public static final Interpolator LEVITATION_INTERPOLATOR = Interpolator.ACCELERATE_DECELERATE;

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



    /* Styles */

    public static final Color BG = new Color(0, 0, 0, 255);
    public static final Color FG_DARK = new Color(255, 255, 255, 255);
    public static final Color FG_MEDIUM = new Color(230, 230, 230, 255);
    public static final Color FG_LIGHT = new Color(200, 200, 200, 255);
    public static final Color ACCENT = new Color(107, 196, 255, 255);
    public static final Color ACCENT_HIGHLIGHT = new Color(255, 219, 77, 255);

    public static final boolean SHOW_CUR_MOVE = true;
    public static final float CUR_MOVE_TEXT_SIZE = 0.062f;
    public static final Color FG_CUR_MOVE = FG_DARK;

    // SPEED etc
    public static final boolean SHOW_SEC_STATUS = true;
    public static final float SEC_STATUS_TEXT_SIZE = 0.018f;
    public static final Color FG_SEC_STATUS = FG_DARK;

    public static final boolean SHOW_LAST_MOVE = true;
    public static final float LAST_MOVE_TEXT_SIZE = 0.02f;
    public static final Color FG_LAST_MOVE = FG_DARK;

    public static final boolean SHOW_STATUS = true;
    public static final float STATUS_TEXT_SIZE = 0.024f;
    public static final Color FG_STATUS_BAR = ACCENT;

    public static final boolean SHOW_CAMERA_MODE = true;
    public static final float CAMERA_MODE_TEXT_SIZE = 0.02f;
    public static final Color FG_CAMERA_MODE = ACCENT_HIGHLIGHT;

    public static final boolean SHOW_CUBE_STATE = true;
    public static final float CUBE_STATE_TEXT_SIZE = 0.024f;
    public static final Color FG_CUBE_STATE = ACCENT_HIGHLIGHT;

    public static final boolean SHOW_CONTROLS_DES = true;
    public static final boolean DEFAULT_CONTROLS_SHOWN = true;      // only when SHOW_CONTROLS_DES = true
    public static final float CONTROLS_DES_TITLE_TEXT_SIZE = 0.0195f;
    public static final Color FG_CONTROLS_DES_TITLE = ACCENT;
    public static final float CONTROLS_DES_TEXT_SIZE = 0.018f;
    public static final Color FG_CONTROLS_DES = FG_MEDIUM;

    public static final Color COLOR_UP = new Color(255, 219, 77, 255);
    public static final Color COLOR_RIGHT = new Color(255, 126, 29, 255);
    public static final Color COLOR_FRONT = new Color(116, 255, 77, 255);
    public static final Color COLOR_DOWN = new Color(255, 255, 255, 255);
    public static final Color COLOR_LEFT = new Color(255, 47, 22, 255);
    public static final Color COLOR_BACK = new Color(77, 157, 255, 255);
    public static final Color COLOR_INTERNAL = new Color(43, 43, 43, 255);

    public static final Color COLOR_AXIS = new Color(130, 130, 130, 179);


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

    public static float getTextSize(@NotNull PApplet app, float size) {
        return getTextSize(app.width, app.height, size);
    }



    /* Status */

    public static final String STATUS_SOLVING = "Solving";
    public static final String STATUS_ALREADY_SOLVED = "Solved";
    public static final String STATUS_SOLUTION_SEQ = "Solution";
    public static final String DOT = ".";

    @Nullable
    public static String getStatusText(@NotNull PApplet app, boolean solving, @Nullable Solver.Solution solution, @Nullable String runSolutionKeyBinding) {
        if (solving) {
            final float rate = app.frameRate / 5;       // 5 = (dots per sec)
            final float count = (app.frameCount % (rate * 4)) / rate;           // 4 = no of dots + 1
            return STATUS_SOLVING + DOT.repeat((int) count);
        }

        if (solution != null) {
            if (solution.isEmpty())
                return STATUS_ALREADY_SOLVED;

            final int maxCount = (int) (app.width / (getTextSize(app, STATUS_TEXT_SIZE) * 2)) - 10;
            final String seq;
            if (solution.moveCount() <= maxCount) {
                seq = solution.getSequence();
            } else {
                seq = solution.getHeadSequence(maxCount) + " + " + Math.abs(solution.moveCount() - maxCount) + " moves";
            }

            return concatKeyBinding(STATUS_SOLUTION_SEQ, seq, runSolutionKeyBinding);
        }

        return null;
    }

    @Nullable
    public static String getSecStatusText(float moveSpeedPercent) {
        return "Speed: " + Format.nf001(moveSpeedPercent) + "%";
    }

    @NotNull
    public static String concatKeyBinding(@NotNull String prefix, @Nullable String suffix, @Nullable String keyBinding) {
        return prefix + (Format.isEmpty(keyBinding)? "": " [" + keyBinding + "]") + " : " + (Format.isEmpty(suffix)? "N/A": suffix);
    }

    @NotNull
    public static String getLastMoveText(@NotNull Move lastMove, @Nullable String keyBinding) {
        return concatKeyBinding("Last Move", lastMove.toString(), keyBinding);
    }

    @NotNull
    public static String getCameraModeText(boolean freeCam, @Nullable String keyBinding) {
        return concatKeyBinding("CAMERA", freeCam? "FREE (Use Mouse)": "CUBE (Use Arrows)", keyBinding);
    }

    @Nullable
    public static String getCubeStateText(int n, boolean cubeLocked) {
        return (cubeLocked? "LOCKED | ": "") + n + "x" + n;
    }
}
