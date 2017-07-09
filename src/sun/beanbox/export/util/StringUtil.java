package sun.beanbox.export.util;

import sun.beanbox.export.datastructure.ExportConstraintViolation;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Created by Andreas Ertlschweiger on 07.07.2017.
 * <p>
 * This class offers some required functionality regarding Strings that could also be used anywhere else.
 */
public class StringUtil {

    /**
     * This method generates a String consisting of a prefix and a random number.
     *
     * @param prefix     the prefix
     * @param lowerBound the lower bound of the suffix number
     * @param upperBound the upper bound of the suffix number
     * @return returns a string
     */
    public static String generateName(String prefix, int lowerBound, int upperBound) {
        Random rand = new Random();
        int random = lowerBound + rand.nextInt(upperBound);
        return prefix + random;
    }

    /**
     * This method turns an absolute path into a relative path. The absolute path is taken and reduced by the string.
     * It also replaces any backslashes with easier to use forward slashes.
     *
     * @param base         the base string to crop from the path
     * @param file         the file to get the relative path from
     * @param leadingSlash if there should be a leading slash on the relative path
     * @return returns a string of a relative path
     */
    public static String getRelativePath(String base, File file, boolean leadingSlash) {
        String[] split = file.getAbsolutePath().split(Pattern.quote(base));
        String relativePath = split.length > 1 ? split[1] : split[0];
        if (relativePath != null && relativePath.length() > 2 && !leadingSlash) {
            relativePath = relativePath.substring(1, relativePath.length());
        }
        if (relativePath != null && relativePath.contains("\\")) {
            return relativePath.replace('\\', '/');
        }
        return relativePath;
    }

    /**
     * This method returns a fully escaped String representation of any primitive type.
     *
     * @param object an object of any type to get a string representation from
     * @return returns a string representation if possible, null if its null or void or returns the .toString representation
     * if its not a primitive type
     */
    public static String convertPrimitive(Object object) {
        if (object == null || object instanceof Void) return null;
        if (object instanceof Character) {
            return "'" + purifyString(object.toString()) + "'";
        } else if (object instanceof Float) {
            return object.toString() + "f";
        } else if (object instanceof Long) {
            return object.toString() + "L";
        } else if (object instanceof String) {
            return "\"" + purifyString(object.toString()) + "\"";
        } else if (object instanceof Short) {
            return "(short) " + object.toString();
        }
        return object.toString();
    }

    /**
     * Escapes any characters of a string that need escaping to get its literal meaning.
     *
     * @param in the string to escape
     * @return an escaped string
     */
    public static String purifyString(String in) {
        in = in.replace("\\", "\\\\");
        in = in.replace(Pattern.quote("\""), "\\\"");
        in = in.replace(Pattern.quote("\'"), "\\\'");
        return in;
    }

    public static String uppercaseFirst(String text) {
        char c[] = text.toCharArray();
        if (Character.isLetter(c[0])) {
            c[0] = Character.toUpperCase(c[0]);
        }
        return new String(c);
    }

    public static String lowercaseFirst(String text) {
        char c[] = text.toCharArray();
        if (Character.isLetter(c[0])) {
            c[0] = Character.toLowerCase(c[0]);
        }
        return new String(c);
    }

    public static String concatenateViolations(List<ExportConstraintViolation> violations) {
        StringBuilder text = new StringBuilder();
        for (ExportConstraintViolation violation : violations) {
            text.append(violation.getMessage()).append(" ");
        }
        return text.toString();
    }
}
