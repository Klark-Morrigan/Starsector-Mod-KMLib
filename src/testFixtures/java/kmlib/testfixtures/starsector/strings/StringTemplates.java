package kmlib.testfixtures.starsector.strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * How many arguments a shipped template takes, and of what kind, read the way {@code String.format}
 * reads it.
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
     * One format specifier: an optional {@code n$} argument index or {@code <} relative index, any flags,
     * an optional width and precision, then the conversion character. The index, the relative marker and
     * the conversion are all captured: the first to count positional slots, the second to tell a reuse of
     * the previous argument from a new one, and the third to tell an argumentless conversion from a real
     * slot.
     */
    private static final Pattern FORMAT_SPECIFIER =
        Pattern.compile("%(?:(\\d+)\\$|(<))?[-#+ 0,(]*\\d*(?:\\.\\d+)?([a-zA-Z%])");

    private static final int INDEX_GROUP = 1;
    private static final int RELATIVE_GROUP = 2;
    private static final int CONVERSION_GROUP = 3;

    private StringTemplates() {
    }

    /**
     * Counts the arguments {@code String.format} would take from {@code template}.
     *
     * <p>A positional specifier addresses an argument by index and may repeat one, so the count it
     * implies is the highest index rather than the number of specifiers; a sequential one takes the next
     * argument each time. A template mixing both consumes whichever demands more. A relative specifier
     * ({@code %<s}) reuses the previous argument, so it adds nothing.
     *
     * @param template the template as shipped
     * @return how many arguments a call site must pass for every slot to be filled
     */
    public static int countFormatArguments(String template) {

        var sequential = 0;
        var highestIndex = 0;
        var specifiers = FORMAT_SPECIFIER.matcher(template);

        while (specifiers.find()) {

            if (ARGUMENTLESS_CONVERSIONS.contains(specifiers.group(CONVERSION_GROUP))
                    || specifiers.group(RELATIVE_GROUP) != null) {
                continue;
            }
            if (specifiers.group(INDEX_GROUP) == null) {
                sequential++;

            } else {
                highestIndex = Math.max(highestIndex, Integer.parseInt(specifiers.group(INDEX_GROUP)));
            }
        }
        return Math.max(sequential, highestIndex);
    }

    /**
     * Which argument each slot of {@code template} takes, and as what, whatever order the slots are
     * written in.
     *
     * <p>This is what two wordings of one string have to agree on for one call site to fill both. The
     * order they are written in is free - a translation reorders a sentence by numbering its slots - so
     * each slot is stated by the argument it addresses rather than by where it stands: a sequential slot
     * takes the next argument, a positional one names its own, and a relative one reuses the previous
     * slot's. Flags, width and precision are left out, being how a figure is laid out rather than what is
     * passed. A conversion is read in lower case, the upper-case form taking the same argument and only
     * upper-casing what it writes.
     *
     * @param template the template as shipped
     * @return one {@code %<index>$<conversion>} entry per distinct slot, by argument index
     */
    public static List<String> readArgumentConversions(String template) {

        var conversionsByIndex = new TreeMap<Integer, SortedSet<String>>();
        var nextSequentialIndex = 1;
        var previousIndex = 0;
        var specifiers = FORMAT_SPECIFIER.matcher(template);

        while (specifiers.find()) {

            var conversion = specifiers.group(CONVERSION_GROUP);

            if (ARGUMENTLESS_CONVERSIONS.contains(conversion)) {
                continue;
            }
            int index;

            if (specifiers.group(RELATIVE_GROUP) != null) {
                index = previousIndex;

            } else if (specifiers.group(INDEX_GROUP) != null) {
                index = Integer.parseInt(specifiers.group(INDEX_GROUP));

            } else {
                index = nextSequentialIndex++;
            }
            previousIndex = index;

            conversionsByIndex
                .computeIfAbsent(index, ignored -> new TreeSet<>())
                .add(conversion.toLowerCase(Locale.ROOT));
        }
        return describeArgumentConversions(conversionsByIndex);
    }

    private static List<String> describeArgumentConversions(SortedMap<Integer, SortedSet<String>> conversionsByIndex) {

        var descriptions = new ArrayList<String>();

        conversionsByIndex.forEach((index, conversions) ->
            conversions.forEach(conversion -> descriptions.add("%" + index + "$" + conversion)));

        return descriptions;
    }
}
