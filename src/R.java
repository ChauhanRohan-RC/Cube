import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.Config;
import util.Format;

import java.awt.*;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

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

    public static final String CONFIG_KEY_FULLSCREEN = "fullscreen";
    public static final String CONFIG_KEY_WIN_WIDTH_PIXELS = "win_width";   // pixels has precedence over ratio
    public static final String CONFIG_KEY_WIN_HEIGHT_PIXELS = "win_height"; // pixels has precedence over ratio
    public static final String CONFIG_KEY_WIN_WIDTH_RATIO = "win_width_ratio";
    public static final String CONFIG_KEY_WIN_HEIGHT_RATIO = "win_height_ratio";
    public static final String CONFIG_KEY_SOUND = "sound";
    public static final String CONFIG_KEY_POLY_RHYTHM = "poly_rhythm";
    public static final String CONFIG_KEY_ANIMATION_SPEED = "anim_speed";

    @NotNull
    public static Dimension getConfigWindowSize(@NotNull Config config, @NotNull Dimension screenSize, @NotNull Dimension defaultValue) {
        return Config.getConfigWindowSize(config, CONFIG_KEY_WIN_WIDTH_PIXELS, CONFIG_KEY_WIN_HEIGHT_PIXELS, CONFIG_KEY_WIN_WIDTH_RATIO, CONFIG_KEY_WIN_HEIGHT_RATIO, screenSize, defaultValue);
    }


    // Shell

    private static final String SHELL_ROOT_NS = "cube:RC";       // Name Space

    @NotNull
    public static String shellPath(@Nullable String child) {
        return (Format.isEmpty(child)? SHELL_ROOT_NS: SHELL_ROOT_NS + "\\" + child) + "> ";
    }

    public static final String SHELL_ROOT = shellPath(null);
    public static final String SHELL_DIMENSION = shellPath("dim");
    public static final String SHELL_SCRAMBLE = shellPath("scramble");
    public static final String SHELL_SOLVER = shellPath("solve");
    public static final String SHELL_MOVE = shellPath("move");
    public static final String SHELL_WINDOW = shellPath("win");


    public static final String DES_SHELL_COMMANDS =
            """
            # COMMANDS .................................
            
             -> <move_seq> : Directly enter moves separated by space to apply
                Example: U R' FF B2 L'[0,1]
                
             -> n <dimension> : Sets the cube dimension
             -> scramble <num_moves> : Scramble with the given number of moves
             -> undo: Undo the last move
             -> finish [c]: finish / cancel animating and pending moves
                Options:
                1. c -> Cancel pending moves
                
             -> solve : Solve / Apply solution (Only for 3*3 cube)
             -> reset [cube | zoom] : Reset the specified scope
                Scopes
                1. cube : reset cube state
                2. zoom : reset cube zoom
                
             -> speed <+ | - | percent>: Set move animation speed
                Wildcards
                1. + -> increase animation speed
                2. - -> decrease animation speed
                
             -> interpolator <key>: Set move animation interpolator
                Keys
                1. default -> the default interpolator
                2. linear -> Linear interpolator
                3. bounce -> Bounce interpolator
                4. acc -> Accelerate interpolator
                5. dec -> Decelerate interpolator
                6. acd -> Accelerate-Decelerate interpolator
                7. anticipate -> Anticipate interpolator
                8. overshoot -> Overshoot interpolator
                
             -> exit / quit: quit
            """;

    public static final String DES_SHELL_MOVES =
            """
            # MOVES (Clockwise) ..............................
            
              U: Up
              R: Right
              F: Front
              D: Down
              L: Left
              B: Back

             -> Moves are case insensitive
             -> Add prime (') for anticlockwise move.
                 ex R [clockwise] -> R' [anti-clockwise]
             -> type move twice for a half turn (180 deg)
                 ex R [single turn] -> RR [double turn]
             -> To turn middle slice, add it's index in range [0, n-1] from the side of move
                 ex 1. To turn 2nd slice from right -> R1
                    2. To turn 4th slice from UP anticlockwise -> U'3
             -> To turn multiple slices in a single move, type their indices in brackets separated by comma
                 ex Turn 1st and 3rd slices from right in a 5*5 cube -> R[0,2]
            """;

    public static final String SHELL_INSTRUCTIONS =
            "\n.......... "+ R.APP_TITLE +" ..........\n" +
            "This is a 3D generic NxNxN Rubik's cube simulator and solver program. It supports any N-dimensional cube, with both graphical and Command-Line controls\n\n"
            + DES_SHELL_MOVES + "\n"
            + DES_SHELL_COMMANDS;


    // Instructions

    public static final String DES_CONTROLS_MOVES =
            """
            U....Up
            R....Right
            F....Front
            D...Down
            L....Left
            B...Back

            SHIFT-Move....Inverse Move
            ALT-Move.......Twice Move
            CTRL-Move....2 Slice Move
            """;

    public static final String DES_CONTROLS_SOLVE =
            """
            S..................Solve
            SPACE......Scramble
            A..................Animate Moves
            X..................Finish Moves
            SHIFT X....Cancel Moves
            SHIFT Q....Reset Cube
            N / M..........Change Cube
            """;

    public static final String DES_CONTROLS_CUBE_CAMERA =
            """
            Up.....................Rotate Up
            Down.........Rotate Down
            Left.................Rotate Left
            Right............Rotate Right
            CTRL-Left...........Roll Left
            CTRL-Right......Roll Right

            +/-...............Zoom In/Out
            SHIFT +/-...............Speed
            Q.................................Reset
            C...........Toggle Controls
            """;

    // Readme

    public static boolean createReadme(@NotNull String instructions) {
        try (PrintWriter w = new PrintWriter("readme.txt", StandardCharsets.UTF_8)) {
            w.print(instructions);
            w.flush();
            return true;
        } catch (Throwable exc) {
            exc.printStackTrace();
        }

        return false;
    }

    public static boolean createShellInstructionsReadme() {
        return R.createReadme(SHELL_INSTRUCTIONS);
    }




}
