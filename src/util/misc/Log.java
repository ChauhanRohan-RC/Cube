package util.misc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.logging.*;

/**
 * Logging Utility
 *<br>
 * <pre>
 *     There are 2 modes of logging
 *     1. Console (uses {@link System#out stdout} and {@link System#err stderr})
 *     2. File logging (create log files on daily basis in {@link #setLogsDir(Path) logsDir})
 * </pre>
 *
 * SETUP<br>
 * 1. set logs dir wih {@link #setLogsDir(Path)}<br>
 * 2. call {@link #init()} to initialize with default configurations
 * */
public class Log {

    public static final String LOGGER_NAME = "complex";
    public static final String TAG = "LOG";

    public static final boolean DEFAULT_DEBUG = true;
    public static final boolean DEFAULT_LOG_TO_CONSOLE = true;
    public static final boolean DEFAULT_LOG_TO_FILE = true;


    /* Levels */

    private static final Level VERBOSE = new Level("Verbose", 600) {
    };

    private static final Level DEBUG = new Level("Debug", 700) {
    };

    private static final Level WARN = new Level("Warn", 800) {
    };

    private static final Level ERR = new Level("Err", 900) {
    };


    /* Formatters */

    public static final SimpleDateFormat FORMATTER_FILE_NAME_DAY = new SimpleDateFormat("MMM dd, yyyy");

    private static final DateTimeFormatter FOrMATTER_LOG_INSTANT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:n");


    /* Files */

    public static final Path DEFAULT_LOGS_DIR = Path.of("", "logs").toAbsolutePath();

    @Nullable
    private static Path sLogsDir;

    @NotNull
    public static synchronized Path getLogsDir() {
        Path dir = sLogsDir;
        if (dir == null) {
            dir = DEFAULT_LOGS_DIR;
        }

        return dir;
    }

    public static boolean ensureLogsDir() {
        final Path dir = getLogsDir();
        if (Files.isDirectory(dir)) {
            return true;
        }

        try {
            Files.createDirectories(dir);
            return true;
        } catch (Throwable t) {
            e(TAG, "Failed to create Logs Directory: " + dir, t);
        }

        return false;
    }

    protected static synchronized void onLogsDirChanged() {
    }

    public static synchronized void setLogsDir(@Nullable Path logsDir) {
        Path dir = sLogsDir;
        if (Objects.equals(dir, logsDir))
            return;

        sLogsDir = logsDir;
        onLogsDirChanged();
    }

    @NotNull
    public static synchronized Path createLogFilePath() {
        return getLogsDir().resolve("logs (" + FORMATTER_FILE_NAME_DAY.format(new Date()) + ").txt");
    }


    private static final Logger sLogger;

    private static volatile boolean sDebug;
    private static volatile boolean sLogToConsole;
    private static volatile boolean sLogToFile;

    @Nullable
    private static Action sDebugAction;
    @Nullable
    private static Action sLogToConsoleAction;
    @Nullable
    private static Action sLogToFileAction;
    @Nullable
    private static Action sResetAction;

    @Nullable
    private static volatile ConsoleHandler sConsoleHandler;
    @Nullable
    private static volatile FileHandler sFileHandler;

    private static volatile boolean sConsoleHandlerAdded;
    private static volatile boolean sFileHandlerAdded;

    static {
        sLogger = Logger.getLogger(LOGGER_NAME);
        sLogger.setUseParentHandlers(false);
        sLogger.setLevel(Level.ALL);        // ALL
    }


    public static void init() {
        resetDefaults();
    }

    /* Reset */

    public static void resetDefaults() {
        setDebug(DEFAULT_DEBUG);
        setLogToConsole(DEFAULT_LOG_TO_CONSOLE);
        setLogToFile(DEFAULT_LOG_TO_FILE);
    }

    @NotNull
    public static Action getResetAction() {
        Action action = sResetAction;
        if (action == null) {
            action = new ResetAction();
            sResetAction = action;
        }

        return action;
    }


    /* Debug */

    public static boolean isDebugEnabled() {
        return sDebug;
    }

    protected static void onDebugEnableChanged(boolean debug) {
        final Action action = sDebugAction;

        if (action != null) {
            setSelected(action, debug);
        }
    }

    /**
     * @return whether pref is changed or not
     * */
    public static boolean setDebug(boolean debug) {
        final boolean old = sDebug;
        if (old == debug)
            return false;

        sDebug = debug;
        onDebugEnableChanged(debug);
        return true;
    }

    /**
     * @return whether pref is changed or not
     * */
    public static boolean toggleDebug() {
        return setDebug(!sDebug);
    }

    @NotNull
    public static Action getDebugAction() {
        Action action = sDebugAction;
        if (action == null) {
            action = new DebugAction();
            sDebugAction = action;
        }

        return action;
    }


    /* Console */

    @NotNull
    private static ConsoleHandler createConsoleHandler() {
        final ConsoleHandler consoleHandler = new ConsoleHandler(WARN.intValue(), LogFormatter.getSingleton());
        consoleHandler.setLevel(Level.ALL);
        return consoleHandler;
    }

    @NotNull
    private static ConsoleHandler getConsoleHandler() {
        ConsoleHandler val = sConsoleHandler;
        if (val != null) {
            return val;
        }

        synchronized (Log.class) {
            val = sConsoleHandler;
            if (val != null) {
                return val;
            }

            val = createConsoleHandler();
            sConsoleHandler = val;
        }

        return val;
    }

    public static boolean isLoggingToConsole() {
        return sLogToConsole;
    }

    protected static void onLogToConsoleChanged(boolean logToConsole) {
        final Action action = sLogToConsoleAction;

        if (action != null) {
            setSelected(action, logToConsole);
        }
    }

    private static void setLogToConsoleInternal(boolean logToConsole) {
        if (logToConsole) {
            if (sConsoleHandlerAdded)
                return;

            sLogger.addHandler(getConsoleHandler());
            sConsoleHandlerAdded = true;
        } else {
            synchronized (Log.class) {
                final ConsoleHandler handler = sConsoleHandler;
                if (handler != null) {
                    sLogger.removeHandler(handler);
                    // DO not close console handler, as it will also close System.out and System.err
                    sConsoleHandlerAdded = false;
                }
            }
        }
    }

    /**
     * @return whether pref is changed or not
     * */
    public static boolean setLogToConsole(final boolean logToConsole) {
        boolean old = sLogToConsole;
        if (old == logToConsole)
            return false;

        synchronized (Log.class) {
            old = sLogToConsole;
            if (old == logToConsole)
                return false;

            setLogToConsoleInternal(logToConsole);

            sLogToConsole = logToConsole;
            onLogToConsoleChanged(logToConsole);
            return true;
        }
    }

    /**
     * @return whether pref is changed or not
     * */
    public static boolean toggleLogToConsole() {
        return setLogToConsole(!sLogToConsole);
    }

    @NotNull
    public static Action getLogToConsoleAction() {
        Action action = sLogToConsoleAction;
        if (action == null) {
            action = new LogToConsoleAction();
            sLogToConsoleAction = action;
        }

        return action;
    }



    /* File Logging */

    @Nullable
    private static FileHandler createFileHandler() {
        try {
            ensureLogsDir();

            final FileHandler handler = new FileHandler(createLogFilePath().toString(), true);
            handler.setFormatter(LogFormatter.getSingleton());
            return handler;
        } catch (Throwable t) {
            t.printStackTrace();
            // Failed to create Log file
        }

        return null;
    }

    @Nullable
    private static FileHandler getFileHandler() {
        FileHandler val = sFileHandler;
        if (val != null)
            return val;

        synchronized (Log.class) {
            val = sFileHandler;
            if (val != null)
                return val;

            val = createFileHandler();
            sFileHandler = val;
        }

        return val;
    }


    protected static void onLogToFileChanged(boolean logToFile) {
        final Action action = sLogToFileAction;

        if (action != null) {
            setSelected(action, logToFile);
        }
    }

    private static boolean setLogToFileInternal(boolean logToFile) {
        if (logToFile) {
            if (sFileHandlerAdded)
                return true;

            final FileHandler handler = getFileHandler();
            if (handler != null) {
                sLogger.addHandler(handler);
                sFileHandlerAdded = true;
            } else {
                logToFile = false;
            }
        } else {

            synchronized (Log.class) {
                final FileHandler handler = sFileHandler;
                if (handler != null) {
                    sLogger.removeHandler(handler);
                    handler.close();
                    sFileHandler = null;        // release
                    sFileHandlerAdded = false;
                }
            }

        }

        return logToFile;
    }

    public static boolean isLoggingToFile() {
        return sLogToFile;
    }

    /**
     * @return whether pref is changed or not
     * */
    public static boolean setLogToFile(final boolean logToFile) {
        boolean old = sLogToFile;
        if (old == logToFile) {
            return false;
        }

        synchronized (Log.class) {
            old = sLogToFile;
            if (old == logToFile) {
                return false;
            }

            final boolean logging = setLogToFileInternal(logToFile);
            old = sLogToFile;
            if (old != logging) {
                sLogToFile = logging;
                onLogToFileChanged(logging);
                return true;
            }

            return false;
        }
    }

    /**
     * @return whether pref is changed or not
     * */
    public static boolean toggleLogToFile() {
        return setLogToFile(!sLogToFile);
    }

    @NotNull
    public static Action getLogToFileAction() {
        Action action = sLogToFileAction;
        if (action == null) {
            action = new LogToFileAction();
            sLogToFileAction = action;
        }

        return action;
    }


    @NotNull
    public static JMenu createLogSettingsMenu() {
        final JMenu menu = new JMenu("Logs");

        menu.add(new JCheckBoxMenuItem(getDebugAction()));
        menu.add(new JCheckBoxMenuItem(getLogToConsoleAction()));
        menu.add(new JCheckBoxMenuItem(getLogToFileAction()));
        menu.addSeparator();
        menu.add(new JMenuItem(getResetAction()));
        return menu;
    }



    /* ..............................  LOGS ............................ */

    @NotNull
    private static LogRecord createRecord(@NotNull Level level, @Nullable String tag, @Nullable Object msg, @Nullable Throwable t) {
        final LogRecord record = new LogRecord(level, Objects.toString(msg));
        record.setLoggerName(LOGGER_NAME);
        record.setSourceClassName(tag);
        record.setThrown(t);

        return record;
    }

    @NotNull
    private static LogRecord createRecord(@NotNull Level level, @Nullable String tag, @Nullable Object msg) {
        return createRecord(level, tag, msg, null);
    }


    private static void log(@NotNull LogRecord record) {
        sLogger.log(record);
    }

    public static void v(String tag, Object msg) {
        log(createRecord(VERBOSE, tag, msg));
    }

    public static void v(Object msg) {
        v(null, msg);
    }


    public static void d(String tag, Object msg, Throwable t) {
        if (sDebug) {
            log(createRecord(DEBUG, tag, msg, t));
        }
    }

    public static void d(String tag, Object msg) {
        d(tag, msg, null);
    }

    public static void d(Object msg) {
        d(null, msg);
    }


    public static void w(String tag, Object msg, Throwable t) {
        log(createRecord(WARN, tag, msg, t));
    }

    public static void w(String tag, Object msg) {
        w(tag, msg, null);
    }

    public static void w(Object msg) {
        w(null, msg);
    }


    public static void stdErr(@NotNull LogRecord record) {
        final String msg = LogFormatter.getSingleton().format(record);
        System.err.println(msg);

        final Throwable t = record.getThrown();
        if (t != null) {
            t.printStackTrace(System.err);
        }
    }

    public static void stdErr(String tag, Object msg, Throwable t) {
        stdErr(createRecord(ERR, tag, msg, t));
    }

    public static void stdErr(String tag, Object msg) {
        stdErr(tag, msg, null);
    }

    public static void stdErr(Object msg) {
        stdErr(null, msg);
    }

    public static void e(String tag, Object msg, Throwable t) {
        final LogRecord record = createRecord(ERR, tag, msg, t);
        log(record);

        if (!(isLoggingToConsole() || isLoggingToFile())) {
            stdErr(record);
        }
    }

    public static void e(String tag, Object msg) {
        e(tag, msg, null);
    }

    public static void e(Object msg) {
        e(null, msg);
    }

    public static void e(String tag, Throwable t) {
        if (t == null)
            return;

        final Throwable cause = t.getCause();
        e(tag, t.getMessage(), cause != null? cause: t);
    }


    private static class LogFormatter extends Formatter {

        @Nullable
        private static volatile LogFormatter sInstance;

        @NotNull
        public static LogFormatter getSingleton() {
            LogFormatter formatter = sInstance;
            if (formatter != null) {
                return formatter;
            }

            synchronized (LogFormatter.class) {
                formatter = sInstance;
                if (formatter != null) {
                    return formatter;
                }

                formatter = new LogFormatter();
                sInstance = formatter;
            }

            return formatter;
        }

        private static String formatInstant(@NotNull Instant instant) {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());

            String zdt_format = zdt.format(FOrMATTER_LOG_INSTANT) + " " + zdt.getOffset().toString();
            if (!Objects.equals(zdt.getOffset(), zdt.getZone())) {
                zdt_format += ('[' + zdt.getZone().toString() + ']');
            }

            return zdt_format;
        }



        private LogFormatter() {
        }

        @Override
        public String getHead(Handler h) {
            if (h instanceof FileHandler || h instanceof ConsoleHandler) {
                final String time = formatInstant(Instant.now());
                return String.format("\n## LOG START -> %s", time);
            }

            return super.getHead(h);
        }

        @Override
        public String getTail(Handler h) {
            if (h instanceof FileHandler || h instanceof ConsoleHandler) {
                final String time = formatInstant(Instant.now());
                return String.format("\n## LOG END -> %s\n", time);
            }

            return super.getTail(h);
        }

        @Override
        public String format(@NotNull LogRecord record) {
            String source;
            if (record.getSourceClassName() != null) {
                source = record.getSourceClassName();
                if (record.getSourceMethodName() != null) {
                    source += " " + record.getSourceMethodName();
                }
            } else {
                source = record.getLoggerName();
            }

            String message = formatMessage(record);
            String throwable = "";
            if (record.getThrown() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println();
                record.getThrown().printStackTrace(pw);
                pw.close();
                throwable = sw.toString();
            }

            return String.format("\n%s:%s: %s: %s %s",
                    formatInstant(record.getInstant()),
                    record.getLevel().getLocalizedName(),
                    source,
                    message,
                    throwable);
        }
    }


    private static class ConsoleHandler extends StreamHandler {

        private volatile int mErrLevelValue;
        @Nullable
        private volatile StreamHandler mErrHandler;

        public ConsoleHandler(int errLevelValue, @NotNull Formatter formatter) {
            super(System.out, formatter);
            mErrLevelValue = errLevelValue;
        }

        @NotNull
        private StreamHandler getErrHandler() {
            StreamHandler err = mErrHandler;
            if (err != null) {
                return err;
            }

            err = new StreamHandler(System.err, getFormatter());
            err.setLevel(Level.ALL);
            mErrHandler = err;

            return err;
        }


        public ConsoleHandler setErrLevelValue(int errLevelValue) {
            mErrLevelValue = errLevelValue;
            return this;
        }

        public int getErrLevelValue() {
            return mErrLevelValue;
        }

        @Override
        public synchronized void publish(LogRecord record) {
            if (record == null)
                return;

            if (record.getLevel().intValue() >= mErrLevelValue) {
                final Handler handler = getErrHandler();
                handler.publish(record);
                handler.flush();
            } else {
                super.publish(record);
                super.flush();
            }
        }

        // Should not close this, as it will also close System.out and System.err
        @Override
        public synchronized void close() throws SecurityException {
            super.close();
            final StreamHandler err = mErrHandler;
            if (err != null) {
                err.close();
            }
        }
    }






    /* Actions */

    private static void setShortDes(@NotNull Action action, String shortDes) {
        action.putValue(Action.SHORT_DESCRIPTION, shortDes);
    }

    private static void setSelected(@NotNull Action action, boolean selected) {
        action.putValue(Action.SELECTED_KEY, selected);
    }

    private static class DebugAction extends AbstractAction {

        public DebugAction() {
            super("Debug");
            setShortDes(this, "Log debug messages");
            setSelected(this, isDebugEnabled());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            toggleDebug();
        }
    }

    private static class LogToConsoleAction extends AbstractAction {

        public LogToConsoleAction() {
            super("Console Logging");
            setShortDes(this, "Toggle Log to console");
            setSelected(this, isLoggingToConsole());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            toggleLogToConsole();
        }
    }


    private static class LogToFileAction extends AbstractAction {

        public LogToFileAction() {
            super("File Logging");
            setShortDes(this, "Toggle Log to file (see logs folder)");
            setSelected(this, isLoggingToFile());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            toggleLogToFile();
        }
    }

    private static class ResetAction extends AbstractAction {

        public ResetAction() {
            super("Reset Log Settings");
            setShortDes(this, "Reset Logging preferences");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            resetDefaults();
        }
    }



}
