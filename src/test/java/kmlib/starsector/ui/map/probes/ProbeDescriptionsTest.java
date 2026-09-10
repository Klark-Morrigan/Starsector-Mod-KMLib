package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.testfixtures.starsector.ui.layout.PositionFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
                    List.of("outer", "middle", "inner"),
                    item -> "<" + item + ">"))
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

    @Nested
    class DescribeComponent {

        @Test
        void describeComponentNamesTheBoxAndOpacityOfADrawnWidget() {

            assertThat(ProbeDescriptions.describeComponent(buildWidgetMock(10f, 20f, 30f, 40f, 1f)))
                .contains("x=10 y=20 w=30 h=40")
                .contains("opacity=1.0");
        }

        @Test
        void describeComponentStillNamesTheOpacityOfAWidgetFadedToNothing() {
            // The reading that explains a host nobody can see: it renders its whole subtree at zero
            // opacity exactly as it does at one, so "there but invisible" has to be sayable.
            assertThat(ProbeDescriptions.describeComponent(buildWidgetMock(5f, 6f, 7f, 8f, 0f)))
                .contains("opacity=0.0");
        }

        @Test
        void describeComponentSaysSoWhenTheLayoutNeverPositionedTheWidget() {

            var unpositionedWidgetMock = mock(UIComponentAPI.class);

            when(unpositionedWidgetMock.getPosition())
                .thenReturn(null);

            assertThat(ProbeDescriptions.describeComponent(unpositionedWidgetMock))
                .contains("unpositioned");
        }

        @Test
        void describeComponentNamesAnEntryThatIsNotAComponentAtAll() {
            // A children list promises nothing about what is in it, and an entry with no box is
            // still part of the tree the line is describing.
            assertThat(ProbeDescriptions.describeComponent("not a widget"))
                .isEqualTo("java.lang.String[not a component]");
        }

        private UIComponentAPI buildWidgetMock(
                float x,
                float y,
                float width,
                float height,
                float opacity) {

            var widgetMock = mock(UIComponentAPI.class);

            when(widgetMock.getPosition())
                .thenReturn(new PositionFake(new Rectangle(x, y, width, height)));
            when(widgetMock.getOpacity())
                .thenReturn(opacity);

            return widgetMock;
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
