package kmlib.starsector.ui.map.probes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the cap every probe's diagnostic line is held to, and the order it leaves what it names in.
 * Both are what a reader assumes without being told: a line that reordered would describe a tree
 * that does not exist, and one that capped at a different point than its neighbours would make two
 * lines in one log incomparable.
 */
class ProbeDescriptionsTest {

    // One more than the cap, so the description has to drop exactly one item.
    private static final int ITEMS_PAST_THE_CAP = 25;

    @Nested
    class DescribeUpToCap {

        @Test
        void describeUpToCapKeepsTheOrderItWasGiven() {
            assertThat(ProbeDescriptions.describeUpToCap(
                    List.of("outer", "middle", "inner"), item -> "<" + item + ">"))
                .containsExactly("<outer>", "<middle>", "<inner>");
        }

        @Test
        void describeUpToCapDescribesNothingForNoItems() {
            assertThat(ProbeDescriptions.describeUpToCap(List.of(), Object::toString))
                .isEmpty();
        }

        @Test
        void describeUpToCapStopsAtTheCapAndKeepsTheItemsBeforeIt() {
            // Stops rather than samples: the head of a walk is the part a reader can act on, since
            // it is where the outermost widgets and the earliest-seeded icons are.
            var describedItems = ProbeDescriptions.describeUpToCap(
                buildItems(ITEMS_PAST_THE_CAP), item -> item);

            assertThat(describedItems)
                .hasSize(24);
            assertThat(describedItems)
                .startsWith("item0", "item1");
            assertThat(describedItems)
                .endsWith("item23");
        }
    }

    private static List<String> buildItems(int itemCount) {

        var items = new ArrayList<String>();

        for (var index = 0; index < itemCount; index++) {
            items.add("item" + index);
        }
        return items;
    }
}
