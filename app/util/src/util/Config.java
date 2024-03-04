package util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A utility class for .ini configuration file. <br>
 * Config objects are lazily initialized, but can be initialized manually using {@link #preload()} <br>
 * <br>
 * Use {@link #obtain(Path)} method for instantiating this class
 * */
public class Config {

    public static final String DEFAULT_KEY_VALUE_SEPARATOR = "=";
    public static final String DEFAULT_COMMENT_TOKEN = "#";

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


    @Nullable
    private static Map<Path, Config> sPool;
    private static final Object sPoolLock = new Object();

    @NotNull
    public static Config obtain(@NotNull Path configFilePath) {
        Map<Path, Config> pool = sPool;
        if (pool == null) {
            synchronized (sPoolLock) {
                pool = sPool;
                if (pool == null) {
                    pool = new HashMap<>();
                    sPool = pool;
                }
            }
        }

        Config config = pool.get(configFilePath);
        if (config == null) {
            synchronized (sPoolLock) {
                config = pool.get(configFilePath);
                if (config == null) {
                    config = new Config(configFilePath);
                    pool.put(configFilePath, config);
                }
            }
        }

        return config;
    }




    /**
     * @return the configuration map, which can be empty if the file does not exist or the file is empty
     *
     * @throws IOException if an i/o exception occurs while attempting to read the file
     * */
    @NotNull
    public static Map<String, String> loadConfig(@NotNull Path config_file, @NotNull String keyValSeparator) throws IOException {
        final Map<String, String> map = new HashMap<>();

        if (!Files.isRegularFile(config_file)) {
            return map;     // return modifiable empty map
        }

        try (Stream<String> stream = Files.lines(config_file, DEFAULT_CHARSET)) {
            stream.forEach(line -> {
                if (isEmpty(line))
                    return;

                line = Format.removeAllWhiteSpaces(line.trim());
                if (line.isEmpty())
                    return;

                final int commentIndex = line.indexOf(DEFAULT_COMMENT_TOKEN);
                if (commentIndex != -1) {
                    line = line.substring(0, commentIndex);
                    if (line.isEmpty())
                        return;
                }

                final int sepIndex = line.indexOf(keyValSeparator);
                if (sepIndex == -1)
                    return;

                final String key = line.substring(0, sepIndex), value = line.substring(sepIndex + 1);
                if (notEmpty(key) && notEmpty(value)) {
                    map.put(key, value);
                }
            });
        }

        return map;
    }

    /**
     * @return the configuration map, which can be empty if the file does not exist or the file is empty, or {@code null} if an I/O error occurs
     * */
    @Nullable
    public static Map<String, String> loadConfigNoThrow(@NotNull Path config_file, @NotNull String keyValSeparator, @Nullable Consumer<Exception> exceptionHandler) {
        try {
            return loadConfig(config_file, keyValSeparator);
        } catch (Exception exc) {
            exc.printStackTrace();

            if (exceptionHandler != null) {
                exceptionHandler.accept(exc);
            }
        }

        return null;
    }


    public static void saveConfig(@NotNull Path config_file, Map<String, String> map, @NotNull String keyValSeparator) throws IOException {
        List<String> lines = new ArrayList<>();

        if (!(map == null || map.isEmpty())) {
            for (Map.Entry<String, String> e: map.entrySet()) {
                lines.add(e.getKey() + keyValSeparator + e.getValue());
            }
        }

        Files.write(config_file, lines, DEFAULT_CHARSET);
    }

    public static boolean saveConfigNoThrow(@NotNull Path config_file, Map<String, String> map, @NotNull String keyValSeparator, @Nullable Consumer<Exception> exceptionHandler) {
        try {
            saveConfig(config_file, map, keyValSeparator);
            return true;
        } catch (Exception exc) {
            exc.printStackTrace();

            if (exceptionHandler != null) {
                exceptionHandler.accept(exc);
            }

            return false;
        }
    }


    public static int getConfigWindowWidth(@NotNull Config config, @NotNull String key_win_width_pixels, @NotNull String key_win_width_ratio, int screenWidth, int defaultValue) {
        int w = config.getValueInt(key_win_width_pixels, -1);
        if (w <= 0) {
            final float ratio = config.getValueFloat(key_win_width_ratio, -1);
            if (ratio > 0) {
                w = Math.round(screenWidth * ratio);
            }
        }

        if (w <= 0) {
            w = defaultValue;
        }

        return w;
    }

    public static int getConfigWindowHeight(@NotNull Config config, @NotNull String key_win_height_pixels, @NotNull String key_win_height_ratio, int screenHeight, int defaultValue) {
        int h = config.getValueInt(key_win_height_pixels, -1);
        if (h <= 0) {
            final float ratio = config.getValueFloat(key_win_height_ratio, -1);
            if (ratio > 0) {
                h = Math.round(screenHeight * ratio);
            }
        }

        if (h <= 0) {
            h = defaultValue;
        }

        return h;
    }

    @NotNull
    public static Dimension getConfigWindowSize(@NotNull Config config, @NotNull String key_win_width_pixels, @NotNull String key_win_height_pixels, @NotNull String key_win_width_ratio, @NotNull String key_win_height_ratio, @NotNull Dimension screenSize, @NotNull Dimension defaultValue) {
        return new Dimension(
                getConfigWindowWidth(config, key_win_width_pixels, key_win_width_ratio, screenSize.width, defaultValue.width),
                getConfigWindowHeight(config, key_win_height_pixels, key_win_height_ratio, screenSize.height, defaultValue.height)
        );
    }





    static boolean isEmpty(@Nullable CharSequence seq) {
        return seq == null || seq.isEmpty();
    }

    static boolean notEmpty(@Nullable CharSequence seq) {
        return !isEmpty(seq);
    }





    @NotNull
    private final Path configFile;
    @NotNull
    private String keyValSeparator;

    @NotNull
    private final Object mConfigLock = new Object();
    @Nullable
    private volatile Map<String, String> mConfigMap;

    private Config(@NotNull Path configFile, @NotNull String keyValSeparator) {
        this.configFile = configFile;
        this.keyValSeparator = keyValSeparator;
    }

    private Config(@NotNull Path configFile) {
        this(configFile, DEFAULT_KEY_VALUE_SEPARATOR);
    }

    @NotNull
    public String getKeyValSeparator() {
        return keyValSeparator;
    }

    public Config setKeyValSeparator(@NotNull String keyValSeparator) {
        this.keyValSeparator = keyValSeparator;
        return this;
    }

    @NotNull
    private Map<String, String> getConfigMapInternal() {
        Map<String, String> config = mConfigMap;
        if (config == null) {
            synchronized (mConfigLock) {
                config = mConfigMap;
                if (config == null) {
                    config = loadConfigNoThrow(configFile, keyValSeparator, exc -> System.err.println("Error in loading configurations from file <" + configFile + "> : " + exc));;
                    if (config == null) {       // error occurred
                        config = new HashMap<>();
                    }

                    mConfigMap = config;
                }
            }
        }

        return config;
    }

    @NotNull
    @UnmodifiableView
    public Map<String, String> getConfigMapUnmodifiable() {
        return Collections.unmodifiableMap(getConfigMapInternal());
    }

    public Config preload() {
        getConfigMapInternal(); // just to initialize
        return this;
    }

    /* Config Accessors ................................................ */

    public boolean containsKey(@NotNull String key) {
        return getConfigMapInternal().containsKey(key);
    }

    @Nullable
    public String getValueRaw(@NotNull String key) {
        return getConfigMapInternal().get(key);
    }

    public String getValueString(@NotNull String key, String defaultValue) {
        final String val = getConfigMapInternal().get(key);
        return isEmpty(val)? defaultValue: val;
    }


    /**
     * @return the value with desired type, or {@code null} if no key is not defined
     *
     * @throws ClassCastException if the value could not be cast to the desired type by the caster function
     * */
    @Nullable
    public <T> T getValueOrThrow(@NotNull String key, @NotNull Function<String, ? extends T> caster) {
        final String val = getConfigMapInternal().get(key);
        if (isEmpty(val))
            return null;

        try {
            return caster.apply(val);
        } catch (ClassCastException | NumberFormatException exc) {
            throw new ClassCastException("Failed to cast configuration value to desired type. Key: " + key + ", Error: " + exc.getMessage());
        }
    }

    public <T> T getValue(@NotNull String key, T defValue, @NotNull Function<String, ? extends T> caster) {
        final String val = getConfigMapInternal().get(key);
        if (notEmpty(val)) {
            try {
                return caster.apply(val);
            } catch (ClassCastException | NumberFormatException ignored) {
            }
        }

        return defValue;
    }

    public int getValueInt(@NotNull String key, int defValue) {
        final String val = getConfigMapInternal().get(key);
        if (notEmpty(val)) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException ignored) {
            }
        }

        return defValue;
    }

    public float getValueFloat(@NotNull String key, float defValue) {
        final String val = getConfigMapInternal().get(key);
        if (notEmpty(val)) {
            try {
                return Float.parseFloat(val);
            } catch (NumberFormatException ignored) {
            }
        }

        return defValue;
    }

    public long getValueLong(@NotNull String key, long defValue) {
        final String val = getConfigMapInternal().get(key);
        if (notEmpty(val)) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException ignored) {
            }
        }

        return defValue;
    }

    public double getValueDouble(@NotNull String key, double defValue) {
        final String val = getConfigMapInternal().get(key);
        if (notEmpty(val)) {
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException ignored) {
            }
        }

        return defValue;
    }

    public boolean getValueBool(@NotNull String key, boolean defValue) {
        return getValueInt(key, defValue? 1: 0) > 0;
    }


    /* Config Modifiers ................................................ */

    @Nullable
    public String putValue(@NotNull String key, @NotNull String value) {
        return getConfigMapInternal().put(key, value);
    }


    public boolean saveToFile(@Nullable Consumer<Exception> exceptionHandler) {
        return saveConfigNoThrow(configFile, getConfigMapInternal(), keyValSeparator, exceptionHandler);
    }

}
