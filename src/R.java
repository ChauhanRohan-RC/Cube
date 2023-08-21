import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.Util;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class R {

    public static final boolean FROZEN = false;         // TODO: set true before packaging


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


    public static final String APP_NAME = "Rubik's Cube Solver-AI";

    // Shell

    private static final String SHELL_ROOT_NS = "cube:RC";       // Name Space

    @NotNull
    public static String shellPath(@Nullable String child) {
        return (Util.isEmpty(child)? SHELL_ROOT_NS: SHELL_ROOT_NS + "\\" + child) + ">";
    }

    public static final String SHELL_ROOT = shellPath(null);
    public static final String SHELL_DIMENSION = shellPath("dim");
    public static final String SHELL_SCRAMBLE = shellPath("scramble");
    public static final String SHELL_SOLVER = shellPath("solve");
    public static final String SHELL_MOVE = shellPath("move");


    public static final String DES_SHELL_COMMANDS =
            """
            # COMMANDS
             -> enter moves sequence (moves separated by space)
             -> n [dimension] -> set cube dimension
             -> scramble [moves]: scramble with given no of moves
             -> undo: undo last move
             -> finish [c]: finish / cancel animating and pending moves
             -> solve: Solve / Apply solution (Only for 3*3 cube)
             -> reset [what]: Reset [cube, zoom]
             -> speed [+ / - / percent]: Increase / Decrease / Set move animation speed
             -> interpolator [key]: Set move animation interpolator
             -> exit / quit: quit
            """;

    public static final String DES_SHELL_MOVES =
            """
            # MOVES (Clockwise)
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
            "\n.......... "+ R.APP_NAME +" ..........\n" +
            "This is a 3D generic N*N Rubik's cube simulator and solver program. It supports any N*N*N dimension cube, with both graphical and Command-Line controls\n\n"
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
