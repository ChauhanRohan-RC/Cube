import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class R {

    public static final boolean FROZEN = false;         // TODO: set true before packaging

    public static final Path DIR_MAIN = (FROZEN? Path.of("app") : Path.of("")).toAbsolutePath();
    public static final Path DIR_RES = DIR_MAIN.resolve("res");
    public static final Path DIR_IMAGE = DIR_RES.resolve("image");
    public static final Path DIR_FONT = DIR_RES.resolve("font");

    public static final Path APP_ICON = DIR_IMAGE.resolve("icon.png");
//    @Nullable
//    public static final Path IMAGE_BG = DIR_IMAGE.resolve("deep_space_2.jpg");

    @Nullable
    public static final Path IMAGE_BG = null;

    public static final Path FONT_PD_SANS_REGULAR = DIR_FONT.resolve("product_sans_regular.ttf");
    public static final Path FONT_PD_SANS_MEDIUM = DIR_FONT.resolve("product_sans_medium.ttf");

    public static final String APP_NAME = "RC Cube";

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

}
