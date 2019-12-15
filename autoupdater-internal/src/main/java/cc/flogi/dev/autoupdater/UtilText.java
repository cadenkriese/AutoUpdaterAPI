package cc.flogi.dev.autoupdater;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 9/2/19
 */
final class UtilText {
    public static String format(String toFormat, String... variables) {
        for (int i = 0; i < variables.length; i += 2) {
            String variable = "%" + variables[i] + "%";
            String replacement = variables[i + 1];

            toFormat = toFormat.replace(variable, replacement);
        }
        return toFormat;
    }
}
