package util.misc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.async.CancellationProvider;
import util.models.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtil {

    public static final String TAG = "misc.FileUtil";

    public static @Nullable String getParent(@NotNull String path) {
        final int index = path.lastIndexOf(File.separatorChar);
        return (index < 1) ? null: path.substring(0, index);
    }

    public static @NotNull String getFullName(@NotNull String path) {
        final int index = path.lastIndexOf(File.separatorChar);
        return (index == -1) ? path: path.substring(index + 1);
    }

    /**
     * Splits LoadMeta and FullName from path
     * Note : LoadMeta can be <code>null</code>.
     *
     * example  1. "/storage/emulated/0/log_file.txt" -> ( "/storage/emulated/0", "log_file.txt" )
     *          2. "/storage/emulated/0/log_dir" -> ( "/storage/emulated/0", "log_file" )
     * */
    public static @NotNull Pair<String, String> split(@NotNull String path) {
        final int index = path.lastIndexOf(File.separatorChar);
        return (index < 1) ? new Pair<>(null, (index == -1) ? path: path.substring(1)): new Pair<>(path.substring(0, index), path.substring(index + 1));
    }


    public static @NotNull String getExtensionFromName(@NotNull String fullName, boolean includeDot) {
        int index = fullName.lastIndexOf(".");
        return (index < 1) ? "": includeDot ? fullName.substring(index): fullName.substring(index + 1);
    }

    public static @NotNull String getName(@NotNull String fullName) {
        int index = fullName.lastIndexOf(".");
        return (index < 1) ? fullName: fullName.substring(0, index);
    }

    /**
     * Splits Full name to Name and Extension
     *
     * example  1. "log_file.txt" -> ( "log_file", ".txt" ) (if includeDot else "txt"
     *          2. "log_dir" -> ( "log_dir", "" )
     * */
    public static @NotNull Pair<String, String> splitNameExtension(@NotNull String fullName, boolean includeDot) {
        final int index = fullName.lastIndexOf(".");
        return (index < 1) ? new Pair<>(fullName, ""): new Pair<>(fullName.substring(0, index), includeDot? fullName.substring(index): fullName.substring(index + 1));
    }

    public static @NotNull String getExtensionFromPath(@NotNull String path, boolean includeDot) {
        return getExtensionFromName(getFullName(path), includeDot);
    }

    /**
     * Splits Extension from path
     *
     * example  1. "/storage/emulated/0/log_file.txt" -> ( "/storage/emulated/0/log_file", ".txt" ) (if includeDot else "txt".
     *          2. "/storage/emulated/0/log_dir" -> ( "/storage/emulated/0/log_dir", "" )
     * */
    public static @NotNull Pair<String, String> splitExtensionFromPath(@NotNull String path, boolean includeDot) {
        final int index = path.lastIndexOf(File.separatorChar);

        final String parent, fullName;
        if (index == -1) {
            parent = "";
            fullName = path;
        } else if (index == 0) {
            parent = File.separator;                     // including separator
            fullName = path.substring(1);
        } else {
            parent = path.substring(0, index + 1);      // including separator
            fullName = path.substring(index + 1);
        }

        final Pair<String, String> nameExt = splitNameExtension(fullName, includeDot);
        return new Pair<>(parent + nameExt.first, nameExt.second);
    }

    /**
     * Finds a non-existing file for a given file, by adding numbered suffix before extension
     *
     * <pre>
     *     for example
     *     let input file -> "/storage/emulated/0/Android/rc (201).txt"
     *
     *     if input does not exists, it will return (0, inputFile),
     *     else returns (202, new File("/storage/emulated/0/Android/rc (202).txt"))
     * </pre>
     *
     * @param path  path to get alternate non-existing file
     * @return pair containing (non-existing suffix no, suffixed file), or (0, inputFile) if inputFile does not exists
     * */
    @NotNull
    public static Pair<Integer, Path> getNonExistingSuffixAndFile(@NotNull Path path) {
        if (!Files.exists(path))
            return new Pair<>(0, path);

        final Pair<String, String> pathExt = splitExtensionFromPath(path.toString(), true);
        Path temp;
        int num = 1, index, last;

//            if (pathExt.first.charAt(last = pathExt.first.length() - 1) == ')' && (pathExt.first.charAt(index = last - 2) == '(' || pathExt.first.charAt(index = last - 3) == '(')) {

        if (pathExt.first.charAt(last = pathExt.first.length() - 1) == ')' && (index = pathExt.first.lastIndexOf('(')) != -1) {
            try {
                num = Integer.parseInt(pathExt.first.substring(index + 1, last));
                if (pathExt.first.charAt(index - 1) == ' ') index--;
                pathExt.first = pathExt.first.substring(0, index);
            } catch (NumberFormatException ignored) { }
        }

        while (Files.exists(temp = Path.of(pathExt.first + " (" + num + ")" + pathExt.second))) {
            num++;
        }

        return new Pair<>(num, temp);
    }

    /**
     * @see #getNonExistingSuffixAndFile(Path)
     *
     * {@inheritDoc}
     * */
    public static @NotNull Path getNonExistingFile(@NotNull Path file) {
        return getNonExistingSuffixAndFile(file).second;
    }

    public static boolean ensureDir(@NotNull Path dir) {
        if (!Files.isDirectory(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (Throwable t) {
                return false;
            }
        }

        return true;
    }

    public static boolean ensureFileParentDir(@NotNull Path file) {
        final Path dir = file.getParent();
        return dir == null || ensureDir(dir);
    }

    @NotNull
    public static String parseExtension(@NotNull String ext, boolean withDot) {
        if (Format.notEmpty(ext)) {
            if (withDot) {
                if (!(ext.charAt(0) == '.')) {
                    ext = '.' + ext;
                }
            } else {
                if (ext.charAt(0) == '.') {
                    ext = ext.substring(1);
                }
            }
        }

        return ext;
    }

    public static boolean hasExtension(@NotNull String path, @NotNull String extension) {
        return path.endsWith(parseExtension(extension, true));
    }

    @NotNull
    public static String ensureExtension(@NotNull String path, @NotNull String extension) {
        if (hasExtension(path, extension)) {
            return path;
        }

        return path + parseExtension(extension, true);
    }

    @NotNull
    public static String changeExtension(@NotNull String path, @NotNull String extension) {
        if (hasExtension(path, extension)) {
            return path;
        }

        return splitExtensionFromPath(path, true).first + parseExtension(extension, true);
    }


    @NotNull
    public static Path ensureExtension(@NotNull Path path, @NotNull String extension) {
        if (hasExtension(path.toString(), extension)) {
            return path;
        }

        return Path.of(path + parseExtension(extension, true));
    }

    @NotNull
    public static File ensureExtension(@NotNull File file, @NotNull String extension) {
        final String path = file.getAbsolutePath();
        if (hasExtension(path, extension)) {
            return file;
        }

        return new File(path + parseExtension(extension, true));
    }



    @NotNull
    public static List<Path> listDir(@NotNull Path dir, @Nullable Predicate<? super Path> filter) {
        if (Files.isDirectory(dir)) {
            try (final Stream<Path> stream = (filter != null? Files.list(dir).filter(filter): Files.list(dir))) {
                return stream.collect(Collectors.toList());
            } catch (Throwable ignored) { }
        }

        return Collections.emptyList();
    }

    @NotNull
    public static List<Path> listRegularFiles(@NotNull Path dir) {
        return listDir(dir, Files::isRegularFile);
    }


    public record PathInfo(@NotNull Path path, BasicFileAttributes attrs) {
    }

    @NotNull
    public static List<PathInfo> scanRegularFiles(@NotNull Path dir, @Nullable Predicate<PathInfo> fileFilter, @Nullable CancellationProvider c) throws IOException {
        final List<PathInfo> regularFiles = new LinkedList<>();

        final FileVisitor<Path> visitor = new FileVisitor<>() {

            private boolean isCancelled() {
                return c != null && c.isCancelled();
            }

            private FileVisitResult result() {
                return isCancelled()? FileVisitResult.TERMINATE: FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return result();
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                final PathInfo info = new PathInfo(file, attrs);
                if (fileFilter == null || fileFilter.test(info)) {
                    regularFiles.add(info);
                }

                return result();
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return result();
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return result();
            }
        };

        Files.walkFileTree(dir, visitor);
        return regularFiles;
    }

}
