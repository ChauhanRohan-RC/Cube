package main;

import gl.GLConfig;
import model.Axis;
import model.cube.CubeI;
import model.cubie.Move;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import processing.event.Event;
import processing.event.KeyEvent;
import solver.Solver;
import util.Format;
import util.U;

import java.awt.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public enum Control {

    ESCAPE("Cancel",
            "Cancel pending moves and stop solving, or exit.",
            ui -> "",
            "Escape",
            "",
            (ui, ev) -> {
//                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == Control.ESCAPE_KEY_CODE_SUBSTITUTE) {
                    return ui.onEscape(ev, true, true);
                }

                return false;
            }, false),

    MOVES("Moves",
            "Cube moves.",
            ui -> String.valueOf(ui.cubeGL.getCube().getAllAppliedMovesCount()),        // can be moves in stack
            "[Ctr]-[Shf]-[U R F D L B]",
            "[U | R | F | D | L | B] keys -> Clockwise Move\nwith Shift -> Anticlockwise Move\nwith Ctrl -> 2-Slice Move\nwith Ctrl-Shift -> 180° Move",
            (ui, ev) -> {
                final Move move = createMove(ev.getKeyCode(), ev.getModifiers());
                return move != null && ui.cubeGL.applyMove(move);
            },
            false, true, null),

    CUBE_SIZE("Cube",
            "Change cube Dimension.",
            ui -> (ui.isCubeLocked() ? "LOCKED | " : "") + (ui.getN() + "x" + ui.getN()),
            "[Ctr | Shf]-N",
            "N -> Increase Size  |  Shift-N -> Decrease Size  |  Ctrl-[Shift]-N -> Force change size",
            (ui, ev) -> {
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_N) {
                    return ui.cubeGL.stepN(!ev.isShiftDown(), ev.isControlDown());
                }

                return false;
            }, false),

    SCRAMBLE("Scramble",
            "Scramble with default number of moves (default: " + CubeI.DEFAULT_SCRAMBLE_MOVES + ").",
            ui -> "",
            "Space",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE && mod == 0) {
                    return ui.cubeGL.scramble(CubeI.DEFAULT_SCRAMBLE_MOVES);
                }

                return false;
            }, false, true, null),

    SOLVE_OR_APPLY("Solve",
            "Solve or apply solution.",
            ui -> "",
            "Enter",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER && mod == 0) {
                    ui.solve(true);
                    return true;
                }

                final Solver.Solution solution = ui.getCurrentSolution();
                if (solution != null && ev.getKeyCode() == java.awt.event.KeyEvent.VK_C && mod == Event.CTRL) {
                    Format.copyToClipboardNoThrow(solution.getSequence());
                    return true;
                }

                return false;
            }, false, true, null),

    RESET("Reset",
            "Resets the Cube.",
            ui -> "",
            "[Ctr | Shf]-Q",
            "Q -> Reset Cube State\nShift-Q -> Reset Camera\nCtrl-Q -> Reset Simulation\nCtrl-Shift-Q -> Reset Everything",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_Q) {
                    if (mod == 0) {                     // reset cube state only
                        ui.cubeGL.resetCube(false);
                    } else if (mod == Event.SHIFT) {     // reset camera only
                        ui.resetCamera(true);
                    } else if (mod == Event.CTRL) {    // reset simulation only
                        ui.resetSimulation();
                    } else if (mod == (Event.CTRL | Event.SHIFT)) {
                        ui.cubeGL.resetCube(true);
                        ui.resetSimulation();
                        ui.resetCamera(true);
                    } else {
                        return false;
                    }

                    return true;
                }

                return false;
            }, false),

    UNDO_LAST_MOVE("Undo Last",
            "Undo last move",
            ui -> {
                final Move move = ui.cubeGL.getCube().peekLastMove();
                return move != null ? move.toString() : "";
            },

            "Ctr-Z",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_Z && mod == Event.CTRL /* Ctrl only */) {
                    return ui.cubeGL.undoRunningOrLastCommittedMove();
                }

                return false;
            }, false),

    FINISH_ALL_MOVES("Finish Moves",
            "Finish all running and pending moves",
            ui -> {
                final int count = ui.cubeGL.runningMovesCount() + ui.cubeGL.pendingMovesCount();
                return count > 0 ? String.valueOf(count) : "";
            },
            "X",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_X && (mod == 0)) {
                    ui.cubeGL.finishAllMoves(false);
                    return true;
                }

                return false;
            }, false),

    CANCEL_ALL_MOVES("Cancel Moves",
            "Cancel all running and pending moves",
            ui -> {
                final int count = ui.cubeGL.runningMovesCount() + ui.cubeGL.pendingMovesCount();
                return count > 0 ? String.valueOf(count) : "";
            },
            "Shf-X",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_X && (mod == Event.SHIFT)) {
                    ui.cubeGL.finishAllMoves(true);
                    return true;
                }

                return false;
            }, false),

    MOVE_ANIMATIONS("Move Anim",
            "Toggle Move Animations.",
            ui -> ui.cubeGL.isMoveAnimationEnabled() ? "ON" : "OFF",
            "A",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_A && mod == 0) {
                    ui.cubeGL.toggleMoveAnimationEnabled();
                    return true;
                }

                return false;
            }, false),

    ANIMATION_SPEED("Speed",
            "Move animation speed (in percent) and animation time (in ms).",
            ui -> String.format("%s%% (%d ms)",
                    Format.nf001(ui.cubeGL.getMoveQuarterSpeedPercent()),
                    ui.cubeGL.getMoveQuarterDurationMs()
            ),
            "[Shf]-/",
            "/ -> Increase Speed  |  Shift-/ -> Decrease Speed",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_SLASH && (mod == 0 || mod == Event.SHIFT) /* Only shift allowed */) {
                    ui.cubeGL.stepMoveQuarterSpeed(true, mod == 0);
                    return true;
                }

                return false;
            }, true),

    ANIMATION_INTERPOLATOR("Interp",
            "Change Move Animation Interpolator.",
            ui -> ui.getCurrentMoveGlInterpolatorInfo(InterpolatorInfo.DEFAULT).getDisplayNamePreferShort(),
            "I",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_I && mod == 0) {
                    ui.setNextMoveGlInterpolator();
                    return true;
                }

                return false;
            }, false),

    SOUND("Sound",
            "Toggle Sounds.",
            ui -> ui.isSoundEnabled() ? "ON" : "OFF",
            "S",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_S && mod == 0) {
                    ui.toggleSoundEnabled();
                    return true;
                }

                return false;
            }, false),

    POLY_RHYTHM("Poly Rhythm",
            "Toggle Poly Rhythm (play multiple notes at once).",
            ui -> ui.isPolyRhythmEnabled() ? "ON" : "OFF",
            "Shf-S",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_S && mod == Event.SHIFT /* Shift only */) {
                    ui.togglePolyRhythmEnabled();
                    return true;
                }

                return false;
            }, false),

    HUD_ENABLED("HUD",
            "Show/Hide HUD.",
            ui -> ui.isHudEnabled() ? "ON" : "OFF",
            "H",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_H && mod == 0) {
                    ui.toggleHudEnabled();
                    return true;
                }

                return false;
            }, false, true, null),

    SHOW_KEY_BINDINGS("Controls",
            "Show/Hide Control Key Bindings.",
            ui -> ui.areKeyBindingsShown() ? "ON" : "OFF",
            "C",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_C && mod == 0) {
                    ui.toggleShowKeyBindings();
                    return true;
                }

                return false;
            }, false, true, null),


    /* Ui and Camera Controls ................................................... */

    // Only when fullscreen
    EXPAND_FULLSCREEN("Window",
            "Sets the fullscreen mode to Expanded or Windowed.",
            ui -> ui.isFullscreenExpanded()? "EXP": "WIN",
            "W",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                final int kc = ev.getKeyCode();
                if (ui.isFullscreen() && kc == java.awt.event.KeyEvent.VK_W && mod == 0) {
                    ui.toggleFullscreenExpanded(true, true);
                    return true;
                }

                return false;
            }, false),

    DRAW_CUBE_AXES("Axes",
            "Show / Hide cube axes.",
            ui -> ui.isDrawCubeAxesEnabled() ? "ON" : "OFF",
            "Shf-A",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_A && mod == Event.SHIFT) {
                    ui.toggleDrawCubeAxes();
                    return true;
                }

                return false;
            }, false),


    SAVE_FRAME("Save Frame",
            "Save Current graphics frame in a png file.",
            ui -> "",
            "Ctr-S",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_S && mod == Event.CTRL /* Ctrl only */) {
                    ui.snapshot();
                    return true;
                }

                return false;
            }, false),

    CUBE_DRAW_SCALE("Zoom",
            "Cube Zoom, in both multiples and percentage.",
            ui -> String.format("%sx (%s%%)",
                    Format.nf001(ui.getCubeDrawScale()),
                    Format.nf001(ui.getCubeDrawScalePercentage())),
            "[Shf]-Z",
            "Z -> Zoom-In  |  Shift-Z -> Zoom-Out",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_Z && (mod == 0 || mod == Event.SHIFT) /* Only shift allowed */) {
                    ui.stepCubeDrawScale(false, mod == 0);
                    return true;
                }

                return false;
            }, false),

    FREE_CAMERA("Camera",
            "Toggle camera mode between FREE and LOCKED.",
            ui -> ui.isFreeCameraEnabled() ? "FREE" : "LOCKED",
            "V",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_V && mod == 0) {
                    ui.toggleFreeCamera(true);
                    return true;
                }

                return false;
            }, false),

    CAMERA_ROTATE_X("Pitch-X",
            "Controls the Camera PITCH (rotation about X-Axis).",
            ui -> Format.nf001(U.normalizeDegrees((float) Math.toDegrees(ui.getCamera().getRotations()[0]))) + "°",
            "Up/Down",
            "[UP | DOWN] arrow keys",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                final int kc = ev.getKeyCode();

                if (ui.isCameraSupported() && (kc == java.awt.event.KeyEvent.VK_UP || kc == java.awt.event.KeyEvent.VK_DOWN) && (mod == 0 || mod == Event.CTRL)) {
                    ui.rotateCameraXByUnit(kc == java.awt.event.KeyEvent.VK_UP, mod == 0);
                    return true;
                }

                return false;
            }, false, false, GLConfig.COLOR_AXIS_X),

    CAMERA_ROTATE_Y("Yaw-Y",
            "Controls the Camera YAW (rotation about Y-Axis).",
            ui -> Format.nf001(U.normalizeDegrees((float) Math.toDegrees(ui.getCamera().getRotations()[1]))) + "°",
            "Left/Right",
            "[LEFT | RIGHT] arrow keys",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                final int kc = ev.getKeyCode();
                if (ui.isCameraSupported() && (kc == java.awt.event.KeyEvent.VK_LEFT || kc == java.awt.event.KeyEvent.VK_RIGHT) && (mod == 0 || mod == Event.CTRL)) {
                    ui.rotateCameraYByUnit(kc == java.awt.event.KeyEvent.VK_LEFT, mod == 0);
                    return true;
                }

                return false;
            }, false, false, GLConfig.COLOR_AXIS_Y),

    CAMERA_ROTATE_Z("Roll-Z",
            "Controls the Camera ROLL (rotation about Z-Axis).",
            ui -> Format.nf001(U.normalizeDegrees((float) Math.toDegrees(ui.getCamera().getRotations()[2]))) + "°",
            "Shf-Left/Right",
            "Shift-[LEFT | RIGHT] arrow keys",
            (ui, ev) -> {
//                final int mod = ev.getModifiers();
                final int kc = ev.getKeyCode();
                if (ui.isCameraSupported() && (kc == java.awt.event.KeyEvent.VK_LEFT || kc == java.awt.event.KeyEvent.VK_RIGHT) && ev.isShiftDown() /* With Shift */) {
                    ui.rotateCameraZByUnit(kc == java.awt.event.KeyEvent.VK_LEFT, !ev.isControlDown());
                    return true;
                }

                return false;
            }, false, false, GLConfig.COLOR_AXIS_Z),

    ;

    @NotNull
    public final String label;
    @NotNull
    public final String description;
    @NotNull
    private final Function<CubePUi3D, String> valueProvider;
    @NotNull
    public final String keyBindingLabel;
    @NotNull
    public final String keyBindingDescription;
    @NotNull
    private final BiFunction<CubePUi3D, KeyEvent, Boolean> keyEventHandler;

    /**
     * Whether this control is continuous or discrete event
     */
    public final boolean continuousKeyEvent;

    public final boolean alwaysShowKeyBinding;

    @Nullable
    public final Color labelColorOverride;

    Control(@NotNull String label,
            @NotNull String description,
            @NotNull Function<CubePUi3D, String> valueProvider,
            @NotNull String keyBindingLabel,
            @NotNull String keyBindingDescription,
            @NotNull BiFunction<CubePUi3D, KeyEvent, Boolean> keyEventHandler,
            boolean continuousKeyEvent,
            boolean alwaysShowKeyBinding,
            @Nullable Color labelColorOverride) {

        this.label = label;
        this.description = description;
        this.valueProvider = valueProvider;
        this.keyBindingLabel = keyBindingLabel;
        this.keyBindingDescription = keyBindingDescription;
        this.keyEventHandler = keyEventHandler;
        this.continuousKeyEvent = continuousKeyEvent;
        this.alwaysShowKeyBinding = alwaysShowKeyBinding;
        this.labelColorOverride = labelColorOverride;
    }

    Control(@NotNull String label,
            @NotNull String description,
            @NotNull Function<CubePUi3D, String> valueProvider,
            @NotNull String keyBindingLabel,
            @NotNull String keyBindingDescription,
            @NotNull BiFunction<CubePUi3D, KeyEvent, Boolean> keyEventHandler,
            boolean continuousKeyEvent) {
        this(label, description, valueProvider, keyBindingLabel, keyBindingDescription, keyEventHandler, continuousKeyEvent, false, null);
    }


    /**
     * @return current formatted value
     */
    @NotNull
    public String getFormattedValue(@NotNull CubePUi3D baseUi) {
        return valueProvider.apply(baseUi);
    }

    /**
     * @return true oif handled, false otherwise
     */
    public boolean handleKeyEvent(@NotNull CubePUi3D baseUi, @NotNull KeyEvent event) {
        return keyEventHandler.apply(baseUi, event);
    }


    /* ............................................................................ */

    /**
     * A substitute for {@link java.awt.event.KeyEvent#VK_ESCAPE} to be able to handle this event manually
     * */
    public static final int ESCAPE_KEY_CODE_SUBSTITUTE = Integer.MAX_VALUE;

    /**
     * A substitute for {@link processing.core.PConstants#ESC} to be able to handle this event manually
     * */
    public static final char ESCAPE_KEY_SUBSTITUTE = Character.MAX_VALUE;

    @NotNull
    public static KeyEvent changeKeyCode(@NotNull KeyEvent src, int newKeyCode, char newKey) {
        return new KeyEvent(
                src.getNative(),
                src.getMillis(),
                src.getAction(),
                src.getModifiers(),
                newKey,
                newKeyCode,
                src.isAutoRepeat()
        );
    }


    public static final Control[] CONTROLS_TOP_RIGHT1 = {
            CUBE_SIZE,
            RESET
    };

    public static final Control[] CONTROLS_TOP_RIGHT2 = {
            HUD_ENABLED,
            SHOW_KEY_BINDINGS
    };

    // Must be in reverse order
    public static final Control[] CONTROLS_BOTTOM_RIGHT = {
            SAVE_FRAME,
            POLY_RHYTHM,
            SOUND
    };

    // Must be in reverse order
    public static final Control[] CONTROLS_BOTTOM_LEFT1 = {
            MOVES
    };

    // Must be in reverse order
    public static final Control[] CONTROLS_BOTTOM_LEFT2 = {
            UNDO_LAST_MOVE
    };

    // Must be in reverse order
    public static final Control[] CONTROLS_BOTTOM_LEFT3 = {
            CANCEL_ALL_MOVES,
            FINISH_ALL_MOVES,
            MOVE_ANIMATIONS,
            SCRAMBLE,
            SOLVE_OR_APPLY
    };


    // Controls to show even when HUD is disabled
    public static final Control[] CONTROLS_HUD_DISABLED = {
            HUD_ENABLED
    };

    public static final Control[] CONTROLS_STATUS_LEFT = {
    };

    public static final Control[] CONTROLS_STATUS_RIGHT = {
            CUBE_DRAW_SCALE,
            ANIMATION_SPEED,
            ANIMATION_INTERPOLATOR
    };

    public static final Control[] CONTROLS_CAMERA1 = {
            CAMERA_ROTATE_X,
            CAMERA_ROTATE_Y,
            CAMERA_ROTATE_Z
    };

    public static final Control[] CONTROLS_CAMERA2 = {
            FREE_CAMERA,
            DRAW_CUBE_AXES
    };

    public static final Control[] CONTROLS_FULLSCREEN_WINDOW = {
            EXPAND_FULLSCREEN
    };


    @Nullable
    private static volatile Control[] sValuesShared;
    private static final Object sValuesLock = new Object();

    @Nullable
    private static volatile String sControlsDescription;

    public static Control[] getValuesShared() {
        Control[] values = sValuesShared;
        if (values == null) {
            synchronized (sValuesLock) {
                values = sValuesShared;
                if (values == null) {
                    values = values();
                    sValuesShared = values;
                }
            }
        }

        return values;
    }


    @NotNull
    private static String createControlsDescription() {
        final Control[] controls = getValuesShared();

        final StringBuilder sj = new StringBuilder();
        boolean firstControl = true;

        for (Control c : controls) {
            if (firstControl) {
                firstControl = false;
            } else {
                sj.append("\n\n");      // Delimiter
            }

            sj.append("-> ").append(c.keyBindingLabel).append(" : ").append(c.label)
                    .append("  [").append(c.continuousKeyEvent ? "Continuous" : "Discrete").append(']');


            // Descriptions
            if (Format.notEmpty(c.description)) {
                for (String line: c.description.split("\n")) {
                    if (Format.isEmpty(line))
                        continue;

                    sj.append("\n\t").append(line);
                }
            }

            // Key bindings Description
            if (Format.notEmpty(c.keyBindingDescription)) {
                boolean firstKeyBind = true;
                for (String line: c.keyBindingDescription.split("\n")) {
                    if (Format.isEmpty(line))
                        continue;

                    if (firstKeyBind) {
                        sj.append("\n\t<Keys> : ");
                        firstKeyBind = false;
                    } else {
                        sj.append("\n\t         ");
                    }

                    sj.append(line);
                }
            }
        }

        return sj.toString();
    }

    public static String getControlsDescription() {
        String des = sControlsDescription;

        if (des == null) {
            synchronized (Control.class) {
                des = sControlsDescription;
                if (des == null) {
                    des = createControlsDescription();
                    sControlsDescription = des;
                }
            }
        }

        return des;
    }


    /* Move Control */

    @Nullable
    public static Axis getMoveAxis(int keyCode) {
        return switch (keyCode) {
            case java.awt.event.KeyEvent.VK_U -> Axis.Y;
            case java.awt.event.KeyEvent.VK_R -> Axis.X;
            case java.awt.event.KeyEvent.VK_F -> Axis.Z;
            case java.awt.event.KeyEvent.VK_D -> Axis.Y_N;
            case java.awt.event.KeyEvent.VK_L -> Axis.X_N;
            case java.awt.event.KeyEvent.VK_B -> Axis.Z_N;
            default -> null;
        };
    }

    @Nullable
    public static Move createMove(int keyCode, int modifiers) {
        final Axis axis = getMoveAxis(keyCode);
        if (axis == null)
            return null;

        int[] relLayers = Move.LAYERS_0;
        final int quarters;

        switch (modifiers) {
            /* No modifiers -> Clockwise Move */
            case 0 -> quarters = Move.QUARTERS_CLOCKWISE;

            /* only shift -> Anticlockwise Move */
            case Event.SHIFT -> quarters = Move.QUARTERS_ANTICLOCKWISE;

            /* only ctrl -> 2-Slice Move */
            case Event.CTRL -> {
                quarters = Move.QUARTERS_CLOCKWISE;
                relLayers = Move.LAYERS_0_1;
            }

            /* ctrl-shift -> 180° move */
            case (Event.CTRL | Event.SHIFT) -> quarters = Move.QUARTERS_2;

            /* Unknown Modifiers */
            default -> {
                return null;
            }
        }

        return new Move(axis, quarters, relLayers);
    }

}
