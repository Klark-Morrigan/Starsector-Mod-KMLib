package kmlib.starsector.ui.map.probes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * How this package's probes turn what they found into the items of one diagnostic line.
 *
 * <p>One helper rather than a loop per probe because the cap is the whole of the rule and it is
 * shared: a probe that capped its own list would be free to drift from what every other line in the
 * same log truncates at, and the drift would show up as a description that looked complete.
 */
final class ProbeDescriptions {

    private ProbeDescriptions() {
    }

    /**
     * Describes the items given, in the order given, up to the shared cap.
     *
     * <p>Order is preserved because every caller's list is an order that means something -
     * outermost-first for a cursor walk, or the order the map will draw in - so a described list
     * that reordered would be describing a different tree. A caller whose total matters past the
     * cap reports that itself, since only the caller knows whether a truncated line still answers
     * its question.
     *
     * @param items        what the probe found, in the order it found it
     * @param describeItem how one item is worded
     * @param <T>          the kind of item, which this neither inspects nor constrains
     * @return the first {@code ProbeLimits.MAX_DESCRIBED_ITEMS} of them, described
     */
    static <T> List<String> describeUpToCap(
            List<T> items,
            Function<? super T, String> describeItem) {

        var describedItems = new ArrayList<String>();
        for (var item : items) {
            if (describedItems.size() >= ProbeLimits.MAX_DESCRIBED_ITEMS) {
                break;
            }
            describedItems.add(describeItem.apply(item));
        }
        return describedItems;
    }
}
