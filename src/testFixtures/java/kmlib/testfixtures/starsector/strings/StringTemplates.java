package kmlib.testfixtures.starsector.strings;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * How many arguments a shipped template takes, read the way {@code String.format} reads it.
 *
 * <p>A template's slot count is a contract with the call site that fills it, and nothing checks it at
 * compile time. A template that gained a slot throws inside the formatter; one that lost a slot is worse,
 * because the formatter ignores surplus arguments and the line renders with its last figure silently
 * missing. A guard holding the two together needs the template's half counted exactly, and the count has
 * edges that a hand-rolled regex gets wrong in ways that only show on the one template using them.
 */
public final class StringTemplates {

    /**
     * The two conversions that consume no argument: {@code %%} writes a literal per cent sign and
     * {@code %n} a line separator.
     */
    private static final Set<String> ARGUMENTLESS_CONVERSIONS = Set.of("%", "n");

    /**
     * One format specifier: an optional {@code n$} argument index, any flags, an optional width and
     * precision, then the conversion character. Both the index and the conversion are captured, the
     * first to count positional slots and the second to tell an argumentless conversion from a real
     * slot. A relative index ({@code %<s}) reuses the previous argument and so is deliberately not
     * matched at all.
     */
    private static final Pattern FORMAT_SPECIFIER =
        Pattern.compile("%(?:(\\d+)\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?([a-zA-Z%])");

    private StringTemplates() {
    }

    /**
     * Counts the arguments {@code String.format} would take from {@code template}.
     *
     * <p>A positional specifier addresses an argument by index and may repeat one, so the count it
     * implies is the highest index rather than the number of specifiers; a sequential one takes the next
     * argument each time. A template mixing both consumes whichever demands more.
     *
     * @param template the template as shipped
     * @return how many arguments a call site must pass for every slot to be filled
     */
    public static int countFormatArguments(String template) {

        var sequential = 0;
        var highestIndex = 0;
        var specifiers = FORMAT_SPECIFIER.matcher(template);

        while (specifiers.find()) {

            if (ARGUMENTLESS_CONVERSIONS.contains(specifiers.group(2))) {
                continue;
            }
            if (specifiers.group(1) == null) {
                sequential++;

            } else {
                highestIndex = Math.max(highestIndex, Integer.parseInt(specifiers.group(1)));
            }
        }
        return Math.max(sequential, highestIndex);
    }
}
