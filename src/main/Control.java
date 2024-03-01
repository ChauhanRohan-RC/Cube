package main;

import gl.GLConfig;
import model.Axis;
import model.cube.CubeI;
import model.cubie.Move;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import peasy.PeasyCam;
import processing.core.PApplet;
import processing.event.Event;
import processing.event.KeyEvent;
import util.Format;
import util.U;

import java.awt.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public enum Control {

    CUBE_SIZE("Cube",
            "Cube Dimension.",
            ui -> (ui.isCubeLocked() ? "LOCKED | " : "") + (ui.getN() + "x" + ui.getN()),
            "[Ctr | Shf]-N",
            "N -> Increase Size  |  Shift-N -> Decrease Size  |  Ctrl-[Shift]-N -> Force change size",
            (ui, ev) -> {
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_N) {
                    return ui.cubeGL.stepN(!ev.isShiftDown(), ev.isControlDown());
                }

                return false;
            }, false),

    RESET("Reset",
            "Resets the Cube.",
            ui -> "",
            "[Ctr | Shf]-Q",
            "Q -> Reset Cube State  |  Shift-Q -> Reset Camera  |  Ctrl-Q -> Reset Simulation | Ctrl-Shift-Q -> Reset Everything",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_Q) {
                    if (mod == 0) {                     // reset cube state only
                        ui.cubeGL.resetCube(false);
                    } else if (mod == Event.SHIFT) {     // reset camera only
                        ui.resetCamera(true, true);
                    } else if (mod == Event.CTRL) {    // reset simulation only
                        ui.resetSimulation();
                    } else if (mod == (Event.CTRL | Event.SHIFT)) {
                        ui.cubeGL.resetCube(true);
                        ui.resetSimulation();
                        ui.resetCamera(true, true);
                    } else {
                        return false;
                    }

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

    SAVE_FRAME("Save Frame",
            "Save Current graphics frame in a png file.",
            ui -> "",
            "Ctrl-S",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_S && mod == Event.CTRL /* Ctrl only */) {
                    ui.snapshot();
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

    ANIMATION_SPEED("Anim Speed",
            "Move Animation Speed, in percentage.",
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

    ANIMATION_INTERPOLATOR("Interpolator",
            "Change Move Animation Interpolator.",
            ui -> ui.getCurrentMoveGlInterpolatorInfo(InterpolatorInfo.DEFAULT).displayName,
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


    MOVES("Moves",
            "Cube moves",
            ui -> "",
            "[Ctr]-[Shf]-[U R F D L B]",
            "[U | R | F | D | L | B] keys -> Clockwise Move\nShift-[U | R | F | D | L | B] -> Anticlockwise Move\nCtrl-[U | R | F | D | L | B] -> 2-Slice Move\nCtrl-Shift-[U | R | F | D | L | B] -> 180° Move",
            (ui, ev) -> {
                final Move move = createMove(ev.getKeyCode(), ev.getModifiers());
                return move != null && ui.cubeGL.applyMove(move);
            },
            false),

    FINISH_ALL_MOVES("Finish/Cancel Moves",
            "Finish or Cancel all running and pending moves",
            ui -> {
                final int count = ui.cubeGL.runningMovesCount() + ui.cubeGL.pendingMovesCount();
                return count > 0 ? String.valueOf(count) : "";
            },
            "[Shf]-X",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_X && (mod == 0 || mod == Event.SHIFT)) {
                    ui.cubeGL.finishAllMoves(mod == Event.SHIFT);
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
            }, false),

    SOLVE_OR_APPLY("Solve",
            "Solve or apply solution.",
            ui -> "",
            "Enter",
            "Enter or Return key",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER && mod == 0) {
                    ui.solve();
                    return true;
                }

                return false;
            }, false),



    /* Camera Controls */

    FREE_CAMERA("Camera",
            "Toggle camera mode between FREE and LOCKED.",
            ui -> ui.isFreeCameraEnabled() ? "Free" : "Locked",
            "V",
            "",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_V && mod == 0) {
                    ui.toggleFreeCamera();
                    return true;
                }

                return false;
            }, false),

    CUBE_DRAW_SCALE("Scale",
            "Cube draw scale, in both multiples and percentage.",
            ui -> String.format("%sx (%s%%)",
                    Format.nf001(ui.getCubeDrawScale()),
                    Format.nf001(ui.getCubeDrawScalePercentage())),
            "[Shf]-Z",
            "Z -> Increase Scale  |  Shift-Z -> Decrease Scale",
            (ui, ev) -> {
                final int mod = ev.getModifiers();
                if (ev.getKeyCode() == java.awt.event.KeyEvent.VK_Z && (mod == 0 || mod == Event.SHIFT) /* Only shift allowed */) {
                    ui.stepCubeDrawScale(false, mod == 0);
                    return true;
                }

                return false;
            }, false),

    CAMERA_ROTATE_X("Pitch-X",
            "Controls the Camera PITCH (rotation about X-Axis).",
            ui -> {
                final PeasyCam cam = ui.getCamera();
                return cam != null ? Format.nf001(U.normalizeDegrees(PApplet.degrees(cam.getRotations()[0]))) + "°" : "N/A";
            },
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
            }, false, false, GLConfig.COLOR_X_AXIS),

    CAMERA_ROTATE_Y("Yaw-Y",
            "Controls the Camera YAW (rotation about Y-Axis).",
            ui -> {
                final PeasyCam cam = ui.getCamera();
                return cam != null? Format.nf001(U.normalizeDegrees(PApplet.degrees(cam.getRotations()[1]))) + "°": "N/A";
            },
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
            }, false, false, GLConfig.COLOR_Y_AXIS),

    CAMERA_ROTATE_Z("Roll-Z",
            "Controls the Camera ROLL (rotation about Z-Axis).",
            ui -> {
                final PeasyCam cam = ui.getCamera();
                return cam != null? Format.nf001(U.normalizeDegrees(PApplet.degrees(cam.getRotations()[2]))) + "°": "N/A";
            },
            "Shf-Up/Down",
            "Shift-[LEFT | RIGHT] arrow keys",
            (ui, ev) -> {
//                final int mod = ev.getModifiers();
                final int kc = ev.getKeyCode();
                if (ui.isCameraSupported() && (kc == java.awt.event.KeyEvent.VK_LEFT || kc == java.awt.event.KeyEvent.VK_RIGHT) && ev.isShiftDown() /* With Shift */) {
                    ui.rotateCameraZByUnit(kc == java.awt.event.KeyEvent.VK_LEFT, !ev.isControlDown());
                    return true;
                }

                return false;
            }, false, false, GLConfig.COLOR_Z_AXIS),

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

//    public static final Control[] CONTROLS_MAIN1 = {
//            CUBE_SIZE,
//            RESET,
//            DRAW_ONLY_BOBS
//    };
//
//    public static final Control[] CONTROLS_MAIN2 = {
//            SOUND,
//            POLY_RHYTHM
//    };
//
//    public static final Control[] CONTROLS_MAIN3 = {
//            HUD_ENABLED,
//            SHOW_KEY_BINDINGS
//    };
//
//    // Controls to show even when HUD is disabled
//    public static final Control[] CONTROLS_HUD_DISABLED = {
//            HUD_ENABLED
//    };
//
//
//    public static final Control[] CONTROLS_STATUS1 = {
//            SIMULATION_SPEED,
//            GRAVITY,
//            DRAG
//    };
//
//    public static final Control[] CONTROLS_STATUS2 = {
//            PENDULUM_MASS,
//            PENDULUM_START_ANGLE,
//            WAVE_PERIOD,
//            MIN_OSCILLATIONS_IN_WAVE_PERIOD,
//            OSCILLATION_STEP_PER_PENDULUM
//    };
//
//    public static final Control[] CONTROLS_CAMERA = {
//            CAMERA_ROTATE_X,
//            CAMERA_ROTATE_Y,
//            CAMERA_ROTATE_Z
//    };


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
            for (String line : c.description.split("\n")) {
                if (line == null || line.isEmpty())
                    continue;

                sj.append("\n\t").append(line);
            }

            boolean firstKeyBind = true;
            // Key bindings Description
            for (String line : c.keyBindingDescription.split("\n")) {
                if (line == null || line.isEmpty())
                    continue;

                if (firstKeyBind) {
                    sj.append("\n\t<Keys> : ");
                    firstKeyBind = false;
                } else {
                    sj.append("\n\t              ");
                }

                sj.append(line);
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
