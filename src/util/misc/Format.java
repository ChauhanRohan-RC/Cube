package util.misc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.regex.Pattern;

public class Format {

    public static final char ARROW_UP = '\u02C4';
    public static final char ARROW_DOWN = '\u02C5';
    public static final char ARROW_LEFT = '<';
    public static final char ARROW_RIGHT = '>';

    public static final char ELLIPSE_CHAR = '\u2026';

    private static Pattern sWhiteSpacePattern;


    public static boolean isEmpty(@Nullable CharSequence sequence) {
        return sequence == null || sequence.length() == 0;
    }

    public static boolean notEmpty(@Nullable CharSequence sequence) {
        return !isEmpty(sequence);
    }

    public static boolean isZero(@NotNull String s) {
        try {
            return Integer.parseInt(s) == 0;
        } catch (Throwable ignored) {
            return false;
        }
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
    public static String removeAllWhiteSpaces(@NotNull CharSequence s) {
        return getWhiteSpacePattern().matcher(s).replaceAll("");
    }

    @NotNull
    public static String removeAllLinedComments(@NotNull String str, @NotNull String commentToken, boolean removeNewLineChar) {
        int comment_token_i = str.indexOf(commentToken);
        if (comment_token_i == -1)
            return str;

        final StringBuilder sb = new StringBuilder(str);

        do {
            int line_i = sb.indexOf("\n", comment_token_i + 1);
            if (line_i == -1) {
                sb.delete(comment_token_i, sb.length());
                break;
            }

            sb.delete(comment_token_i, line_i + (removeNewLineChar? 1: 0));
            comment_token_i = sb.indexOf(commentToken, comment_token_i + (removeNewLineChar? 0: 1));
        } while (comment_token_i != -1);

        return sb.toString();
    }


    @NotNull
    public static String createScientificDecimalFormatString(int digitsBeforeDecimals, int digitAfterDecimals) {
        return "0".repeat(digitsBeforeDecimals) +
                "." +
                "#".repeat(digitAfterDecimals) +
                "E0";
    }

    @NotNull
    public static DecimalFormat createScientificDecimalFormat(int maxIntegerDigits, int maxFractionDigits) {
        final DecimalFormat df = new DecimalFormat(createScientificDecimalFormatString(maxFractionDigits, maxFractionDigits));
        df.setMinimumIntegerDigits(0);
        df.setMaximumIntegerDigits(maxIntegerDigits);
        df.setMaximumFractionDigits(0);
        df.setMaximumFractionDigits(maxFractionDigits);

        return df;
    }

    @NotNull
    public static String formatScientific(@NotNull NumberFormat format, double value) {
        String result = format.format(value);
        int i = result.lastIndexOf('E');
        if (i == -1)
            i = result.lastIndexOf('e');

        if (i != -1 && isZero(result.substring(i + 1))) {
            result = result.substring(0, i);
        }

        return result;
    }

//    public static String formatScientific(double value, int digitsBeforeDecimals, int digitAfterDecimals) {
//        final String format = decimalFormat(digitsBeforeDecimals, digitAfterDecimals);
//
//
//    }
}
