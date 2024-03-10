package util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.text.NumberFormat;
import java.util.regex.Pattern;

public class Format {

    public static final char ARROW_UP = '\u02C4';
    public static final char ARROW_DOWN = '\u02C5';
    public static final char ARROW_LEFT = '<';
    public static final char ARROW_RIGHT = '>';

    public static final char ELLIPSE_CHAR = '\u2026';

    private static Pattern sWhiteSpacePattern;

    private static NumberFormat sNumberFormat000;
    private static NumberFormat sNumberFormat001;
    private static NumberFormat sNumberFormat002;

    public static boolean isEmpty(@Nullable CharSequence sequence) {
        return sequence == null || sequence.isEmpty();
    }

    public static boolean notEmpty(@Nullable CharSequence sequence) {
        return !isEmpty(sequence);
    }

    @NotNull
    public static String toString(CharSequence seq) {
        return isEmpty(seq)? "": seq.toString();
    }

    @NotNull
    public static CharSequence ellipse(@NotNull CharSequence val, int length) {
        if (val.isEmpty() || length < 0 || val.length() <= length)
            return val;

        final StringBuilder sb = new StringBuilder(val);
        sb.setLength(length - 1);
        sb.append(ELLIPSE_CHAR);
        sb.trimToSize();
        return sb;
    }

    @NotNull
    public static Pattern getWhiteSpacePattern() {
        if (sWhiteSpacePattern == null) {
            sWhiteSpacePattern = Pattern.compile("\\s");
        }

        return sWhiteSpacePattern;
    }

    @NotNull
    public static String replaceAllWhiteSpaces(@NotNull CharSequence s, @NotNull String replacement) {
        return getWhiteSpacePattern().matcher(s).replaceAll(replacement);
    }

    @NotNull
    public static String removeAllWhiteSpaces(@NotNull CharSequence s) {
        return replaceAllWhiteSpaces(s, "");
    }

    @NotNull
    public static String nf(float num) {
        int inum = (int)num;
        return num == (float)inum ? String.valueOf(inum) : String.valueOf(num);
    }

    public static String nf(float num, int minIntegerDigits, int minFracDigits, int maxFracDigits) {
        final NumberFormat float_nf = NumberFormat.getInstance();
        float_nf.setGroupingUsed(false);

        if (minIntegerDigits != 0)
            float_nf.setMinimumIntegerDigits(minIntegerDigits);
        if (minFracDigits != 0) {
            float_nf.setMinimumFractionDigits(minFracDigits);
        }

        if (maxFracDigits != 0) {
            float_nf.setMaximumFractionDigits(maxFracDigits);
        }

        if (num == -0f) {
            num = 0;
        }

        return float_nf.format(num);
    }

    public static String nf000(float num) {
        NumberFormat nf = sNumberFormat000;
        if (nf == null) {
            nf = NumberFormat.getInstance();
            nf.setGroupingUsed(false);
            sNumberFormat000 = nf;
        }

        if (num == -0f) {
            num = 0;
        }

        return nf.format(num);
    }

    public static String nf001(float num) {
        NumberFormat nf = sNumberFormat001;
        if (nf == null) {
            nf = NumberFormat.getInstance();
            nf.setGroupingUsed(false);
            nf.setMaximumFractionDigits(1);

            sNumberFormat001 = nf;
        }

        if (num == -0f) {
            num = 0;
        }

        return nf.format(num);
    }

    public static String nf002(float num) {
        NumberFormat nf = sNumberFormat002;
        if (nf == null) {
            nf = NumberFormat.getInstance();
            nf.setGroupingUsed(false);
            nf.setMaximumFractionDigits(2);

            sNumberFormat002 = nf;
        }


        if (num == -0f) {
            num = 0;
        }

        return nf.format(num);
    }



    public static Clipboard getSystemClipboard() {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        return defaultToolkit.getSystemClipboard();
    }

    public static void copyToClipboard(String text) {
        Clipboard clipboard = getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
    }

    public static boolean copyToClipboardNoThrow(String text) {
        try {
            copyToClipboard(text);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

}
