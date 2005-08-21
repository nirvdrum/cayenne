package org.objectstyle.cayenne.util;

import org.apache.oro.text.perl.Perl5Util;

/**
 * A class that combines various utility methods using regular expressions processing.
 * This class serves to avoid direct dependency of Util class on Jakarta ORO package.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class RegexUtil {

    private static final Perl5Util regexUtil = new Perl5Util();

    /**
     * Replaces all backslashes "\" with forward slashes "/". Convenience method to
     * convert path Strings to URI format.
     */
    static String substBackslashes(String string) {
        if (string == null) {
            return null;
        }

        return regexUtil.match("/\\\\/", string) ? regexUtil.substitute(
                "s/\\\\/\\//g",
                string) : string;
    }

    /**
     * Returns package name for the Java class as a path separated with forward slash
     * ("/"). Method is used to lookup resources that are located in package
     * subdirectories. For example, a String "a/b/c" will be returned for class name
     * "a.b.c.ClassName".
     */
    static String getPackagePath(String className) {
        if (regexUtil.match("/\\./", className)) {
            String path = regexUtil.substitute("s/\\./\\//g", className);
            return path.substring(0, path.lastIndexOf("/"));
        }
        else {
            return "";
        }
    }

    /**
     * Converts a SQL-style pattern to a valid Perl regular expression. E.g.:
     * <p>
     * <code>"billing_%"</code> will become <code>/^billing_.*$/</code>
     * <p>
     * <code>"user?"</code> will become <code>/^user.?$/</code>
     * 
     * @since 1.0.6
     */
    public static String sqlPatternToRegex(String pattern, boolean ignoreCase) {
        if (pattern == null) {
            throw new NullPointerException("Null pattern.");
        }

        if (pattern.length() == 0) {
            throw new IllegalArgumentException("Empty pattern.");
        }

        StringBuffer buffer = new StringBuffer();

        // convert * into regex syntax
        // e.g. abc*x becomes /^abc.*x$/
        // or abc?x becomes /^abc.?x$/
        buffer.append("/^");
        for (int j = 0; j < pattern.length(); j++) {
            char nextChar = pattern.charAt(j);
            if (nextChar == '%') {
                nextChar = '*';
            }

            if (nextChar == '*' || nextChar == '?') {
                buffer.append('.');
            }
            // escape special chars
            else if (nextChar == '.'
                    || nextChar == '/'
                    || nextChar == '$'
                    || nextChar == '^') {
                buffer.append('\\');
            }

            buffer.append(nextChar);
        }

        buffer.append("$/");

        if (ignoreCase) {
            buffer.append('i');
        }

        String finalPattern = buffer.toString();

        // test the pattern
        try {
            regexUtil.match(finalPattern, "abc_123");
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Error converting pattern: "
                    + e.getLocalizedMessage());
        }

        return finalPattern;
    }

    private RegexUtil() {
        super();
    }
}
