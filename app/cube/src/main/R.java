package main;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.Config;
import util.Format;

import java.awt.*;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.StringJoiner;

public class R {

    public static final boolean FROZEN = false;         // TODO: set true before packaging

    public static final String APP_TITLE = "Rubik's Cube Simulation and Solver";


    // Dir structure
    public static final Path DIR_MAIN = (FROZEN? Path.of("app") : Path.of("")).toAbsolutePath();
    public static final Path DIR_RES = DIR_MAIN.resolve("res");
    public static final Path DIR_IMAGE = DIR_RES.resolve("image");
    public static final Path DIR_FONT = DIR_RES.resolve("font");

    // Resources
    public static final Path APP_ICON = DIR_IMAGE.resolve("icon.png");

//    @Nullable
//    public static final Path IMAGE_BG = DIR_IMAGE.resolve("deep_space_2.jpg");

    @Nullable
    public static final Path IMAGE_BG = null;

    public static final Path FONT_PD_SANS_REGULAR = DIR_FONT.resolve("product_sans_regular.ttf");
    public static final Path FONT_PD_SANS_MEDIUM = DIR_FONT.resolve("product_sans_medium.ttf");


    // Configurations

    public static final Path FILE_CONFIG = DIR_MAIN.resolve("configuration.ini");
    public static final Config CONFIG = Config.obtain(FILE_CONFIG);       // Since configs are lazily loaded, this does not have any cost

    /* Config: [WINDOW] */
    public static final String CONFIG_KEY_FULLSCREEN = "fullscreen";
    public static final String CONFIG_KEY_INITIAL_FULLSCREEN_EXPANDED = "fullscreen_expanded";
    public static final String CONFIG_KEY_WIN_WIDTH_PIXELS = "win_width";   // pixels has precedence over ratio
    public static final String CONFIG_KEY_WIN_HEIGHT_PIXELS = "win_height"; // pixels has precedence over ratio
    public static final String CONFIG_KEY_WIN_WIDTH_RATIO = "win_width_ratio";
    public static final String CONFIG_KEY_WIN_HEIGHT_RATIO = "win_height_ratio";

    /* Config: [CUBE] */
    public static final String CONFIG_KEY_CUBE_SIZE = "cube_size";
    public static final String CONFIG_KEY_LOCK_CUBE_WHILE_SOLVING = "lock_cube_while_solving";
    public static final String CONFIG_KEY_DRAW_CUBE_AXES = "draw_cube_axes";

    /* Config: [SOUND] */
    public static final String CONFIG_KEY_SOUND = "sound";
    public static final String CONFIG_KEY_POLY_RHYTHM = "poly_rhythm";

    /* Config: [SIMULATION ENV] */
    public static final String CONFIG_KEY_ANIMATION_SPEED = "anim_speed";
    public static final String CONFIG_KEY_ANIMATION_INTERPOLATOR = "anim_interpolator";

    /* Config: [CAMERA] */
    public static final String CONFIG_KEY_CUBE_DRAW_SCALE_PERCENT = "cube_draw_scale";
    public static final String CONFIG_KEY_FREE_CAMERA = "free_camera";

    @NotNull
    public static Dimension getConfigWindowSize(@NotNull Config config, @NotNull Dimension screenSize, @NotNull Dimension defaultValue) {
        return Config.getConfigWindowSize(config, CONFIG_KEY_WIN_WIDTH_PIXELS, CONFIG_KEY_WIN_HEIGHT_PIXELS, CONFIG_KEY_WIN_WIDTH_RATIO, CONFIG_KEY_WIN_HEIGHT_RATIO, screenSize, defaultValue);
    }


    /* Shell ...................................................................................*/

    private static final String SHELL_ROOT_NS = "cube";       // Name Space

    @NotNull
    public static String shellPath(@Nullable String child) {
        return (Format.isEmpty(child)? SHELL_ROOT_NS: SHELL_ROOT_NS + "\\" + child) + "> ";
    }


    public static final String SHELL_ROOT = shellPath(null);
    public static final String SHELL_WINDOW = shellPath("win");
    public static final String SHELL_CUBE_SIZE = shellPath("size");
    public static final String SHELL_RESET = shellPath("reset");
    public static final String SHELL_ANIM_SPEED = shellPath("speed");
    public static final String SHELL_SCRAMBLE = shellPath("scramble");
    public static final String SHELL_SOLVER = shellPath("solve");
    public static final String SHELL_SCALE = shellPath("scale");
    public static final String SHELL_MOVE = shellPath("move");

    public static final String SHELL_CAMERA = shellPath("cam");
    public static final String SHELL_ROTATION_X = shellPath("pitch");
    public static final String SHELL_ROTATION_Y = shellPath("yaw");
    public static final String SHELL_ROTATION_Z = shellPath("roll");


    /* Usage Description ........................................................ */

    public static final String DESCRIPTION_GENERAL =
            "=======================  "+ R.APP_TITLE +"  =======================\n" +
            "This is a 3D NxNxN Rubik's cube simulation and solver program. It supports any N-dimensional cube, with both Graphical and Command-Line control interfaces";

    public static final String DESCRIPTION_GENERAL_WITH_HELP = DESCRIPTION_GENERAL + "\n\n\tType <help> for usage information.\n";

    public static final String DESCRIPTION_MOVES =
            """
             ## MOVES NOTATION ----------------------------------------------
            
              U : Up
              R : Right
              F : Front
              D : Down
              L : Left
              B : Back

             -> Moves are case insensitive
             -> Add prime (') for anticlockwise move.
                 ex R [clockwise] -> R' [anti-clockwise]
             -> type move twice for a half turn (180 deg)
                 ex R [single turn] -> RR [double turn]
             -> To turn middle slice, add it's index in range [0, n-1] from the side of move
                 ex 1. To turn 2nd slice from right -> R1
                    2. To turn 4th slice from UP anticlockwise -> U'3
             -> To turn multiple slices in a single move, type their indices in brackets separated by comma
                 ex Turn 1st and 3rd slices from right in a 5*5 cube -> R[0,2]  \s""";


    @Nullable
    private static String sDesControls;

    public static String getUiControlsDescription() {
        if (sDesControls == null) {
            sDesControls = "## UI CONTROLS ----------------------------------------------------\n\n" + Control.getControlsDescription();
        }

        return sDesControls;
    }


    public static final String DESCRIPTION_COMMANDS =
            """
            ## COMMANDS ----------------------------------------------------
            
            -> help [-moves | -controls | -commands | -all] : Print usage information
               Scopes
               1. -moves -> print moves notation and usage
               1. -controls -> print controls usage
               2. -commands -> print commands usage
               3. -all -> print entire usage information
                       
            -> <move_seq> : Directly enter moves separated by space to apply
               Example: U R' FF B2 L'[0,1]
                
            -> size <size> : Sets the cube size
               Alias: dim, cube
             
            -> solve : Solve the current state or Apply solution
            -> scramble <num_moves> : Scramble with the given number of moves
            
            -> undo : Undo the last move
            -> finish : finish animating and pending moves
            -> cancel : cancel solver and all pending moves
               Alias: stop
               
            -> reset [-f] [-state | -env | -cam | -win | -all] : Reset given scope(s)
               Scopes
               1. -state -> reset cube state
               2. -env -> reset simulation environment
               3. -cam -> reset camera (pitch, yaw and roll)
               4. -win -> reset window size and position
               5. -all -> reset everything  (Default)
               Options
               1. -f -> force reset without animations
            
            -> save : Save current frame to a png file
               Alias: snap, snapshot, saveframe
               
            -> axes : toggle cube axes
               Alias: toggle axes
               
            -> anim : toggle move animations
               Alias: toggle anim, animations, toggle animations
               
            -> speed [-p | -d] <value> : Set move animation speed or duration
               Modes
               1. -p -> animation speed, in percentage  (Default)
               2. -d -> animation duration, in milliseconds
               
            -> interpolator <next | key> : Sets the move animation interpolator
               Alias: intp, interp
               Wildcard: next -> cycle to next interpolator
               Keys
               1. default -> the default interpolator
               2. linear -> Linear interpolator
               3. bounce -> Bounce interpolator
               4. acc -> Accelerate interpolator
               5. dec -> Decelerate interpolator
               6. acd -> Accelerate-Decelerate interpolator
               7. anticipate -> Anticipate interpolator
               8. overshoot -> Overshoot interpolator
               
            -> sound : Toggle sounds
            -> poly-rhythm : Toggle Poly Rhythm mode. If enabled, it allows playing multiple notes at once
                        
            -> hud : Show / Hide HUD overlay
            -> keys : Show / Hide control key bindings
               Alias: toggle keys, controls, toggle controls
               
            -> win [-size | -pos] <x> <y> : Sets the window size or location on screen
               Options
               1. -size -> set window size.
               2. -pos -> set window location on screen
               Wildcards
               1. w : set to initial windowed size. To be used with -size option
               2. c : center window on screen. To be used with -pos option
               
            -> expand : Expand / Collapse Fullscreen window
               Alias: toggle expand, collapse, toggle collapse
               
            -> cam [-free | -locked | -toggle] : set or toggle camera modes
               Modes
               1. -free -> free camera mode, mouse controlled
               2. -locked -> locked camera, keyboard controlled
               3. -toggle -> toggle camera mode between FREE and LOCKED  (Default)
               Options
               1. -f -> force without animations
            
            -> zoom [-x | -p] <value> : sets the cube zoom
               Alias: scale
               Modes
               1. -x -> Multiples or times  (Default)
               2. -p -> percentage, in range [0, 100]
                                
            -> pitch [-by | -f] <+ | - | value_in_deg> : Sets the camera pitch (rotation about X-axis)
               Alias: rx, rotx, rotationx
               Wildcards: + or up, - or down
               Options
               1. -by -> change current pitch by the given value
               2. -f -> force without animations
               
            -> yaw [-by | -f] <+ | - | value_in_deg> : Sets the camera yaw (rotation about Y-axis)
               Alias: ry, roty, rotationy
               Wildcards: + or left, - or right
               Options
               1. -by -> change current yaw by the given value
               2. -f -> force without animations
                        
            -> roll [-by | -f] <+ | - | value_in_deg> : Sets the camera roll (rotation about Z-axis)
               Alias: rz, rotz, rotationz
               Wildcards: + or left, - or right
               Options
               1. -by -> change current roll by the given value
               2. -f -> force without animations
               
            -> exit / quit: quit  \s""";


    public static String getFullDescription(boolean withGeneral, boolean withUiControls) {
        final StringJoiner sj = new StringJoiner("\n\n\n");

        if (withGeneral) {
            sj.add(DESCRIPTION_GENERAL);
        }

        sj.add(DESCRIPTION_MOVES);

        if (withUiControls) {
            sj.add(getUiControlsDescription());
        }

        sj.add(DESCRIPTION_COMMANDS);

        return sj.toString();
    }

//    public static final String DES_CONTROLS_MOVES =
//            """
//            U....Up
//            R....Right
//            F....Front
//            D...Down
//            L....Left
//            B...Back
//
//            SHIFT-Move....Inverse Move
//            ALT-Move.......Twice Move
//            CTRL-Move....2 Slice Move
//            """;
//
//    public static final String DES_CONTROLS_SOLVE =
//            """
//            S..................Solve
//            SPACE......Scramble
//            A..................Animate Moves
//            X..................Finish Moves
//            SHIFT X....Cancel Moves
//            SHIFT Q....Reset Cube
//            N / M..........Change Cube
//            """;
//
//    public static final String DES_CONTROLS_CUBE_CAMERA =
//            """
//            Up.....................Rotate Up
//            Down.........Rotate Down
//            Left.................Rotate Left
//            Right............Rotate Right
//            CTRL-Left...........Roll Left
//            CTRL-Right......Roll Right
//
//            +/-...............Zoom In/Out
//            SHIFT +/-...............Speed
//            Q.................................Reset
//            C...........Toggle Controls
//            """;

    // Readme

    public static boolean createReadme(@NotNull String content) {
        try (PrintWriter w = new PrintWriter("readme.txt", StandardCharsets.UTF_8)) {
            w.print(content);
            w.flush();
            return true;
        } catch (Throwable exc) {
            System.out.println("Failed to create readme.txt: " + exc.getMessage());
        }

        return false;
    }

    public static boolean createFullDescriptionReadme(boolean withUiControls) {
        return R.createReadme(getFullDescription(true, withUiControls));
    }

}
