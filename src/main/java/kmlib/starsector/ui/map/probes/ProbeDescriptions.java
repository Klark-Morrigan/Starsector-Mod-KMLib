package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.math.geometry.Rectangles;

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

    /**
     * Describes one entry off a children list: what it is, where it is drawn, and how solidly.
     *
     * <p>Held here rather than in one probe because more than one of them names components in a
     * line, and a reader comparing two such lines is entitled to assume the same widget reads the
     * same way in both. The three cases it folds together - not a component at all, a component the
     * layout never positioned, and a drawn one - are one question to whoever reads the line: what,
     * if anything, is on screen there.
     *
     * <p>Opacity is named even for something faded to nothing, because that is the reading which
     * explains a host nobody can see: a widget renders its whole subtree at zero opacity exactly as
     * it does at one, so "present but invisible" is a state a line has to be able to say.
     *
     * @param component an entry off a children list, which the list does not promise is a component
     * @return the component's class with its box and opacity, or with what it lacks
     */
    static String describeComponent(Object component) {

        if (!(component instanceof UIComponentAPI widget)) {
            // Named anyway rather than skipped, so a list is what the tree holds rather than a
            // filtered view of it.
            return component.getClass().getName() + "[not a component]";
        }
        var box = DrawnWidgets.resolveBoxOf(widget);
        return widget.getClass().getName()
            + "[" + (box == null ? "unpositioned" : Rectangles.describe(box))
            + " opacity=" + widget.getOpacity()
            + "]";
    }

}
