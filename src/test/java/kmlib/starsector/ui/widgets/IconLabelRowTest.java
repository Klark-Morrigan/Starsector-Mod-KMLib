package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link IconLabelRow}'s geometry: the icon is a square inset off the row's top and bottom and
 * flush with its left padding, the label anchors past the icon (or at the left inset for an icon-less
 * option), and the measured row width reserves the icon and its gap only when the option has one, so
 * the drawn icon, the drawn label, and the sized column all read the same layout.
 */
class IconLabelRowTest {
    // A 24-tall row: the icon inset is 2 off each edge, so the icon square is 20 and the label sits
    // 6 past it. Reused across the cases so their numbers line up.
    private final Rectangle row = new Rectangle(10f, 20f, 100f, 24f);

    @Nested
    class ComputeIconBox {
        @Test
        void placesASquareInsetOffTheTopAndBottomFlushWithTheLeftPadding() {
            // Left padding 4 -> x=14; vertical inset 2 -> y=22 and side 24-2*2=20.
            assertThat(IconLabelRow.computeIconBox(row))
                    .isEqualTo(new Rectangle(14f, 22f, 20f, 20f));
        }
    }

    @Nested
    class ComputeLabelAnchorX {
        @Test
        void anchorsPastTheIconAndItsGapWhenTheOptionHasAnIcon() {
            // Left padding 4 + icon side 20 + gap 6 = 30 past the row's left edge (x=10) -> 40.
            assertThat(IconLabelRow.computeLabelAnchorX(row, true)).isEqualTo(40f);
        }

        @Test
        void anchorsAtTheLeftPaddingWhenTheOptionHasNoIcon() {
            assertThat(IconLabelRow.computeLabelAnchorX(row, false)).isEqualTo(14f);
        }
    }

    @Nested
    class MeasureRowWidth {
        @Test
        void reservesTheIconAndItsGapAheadOfTheLabelWhenTheOptionHasAnIcon() {
            // Left padding 4 + (icon side 20 + gap 6) + label 50 + trailing padding 4 = 84.
            assertThat(IconLabelRow.measureRowWidth(24f, 50f, true)).isEqualTo(84f);
        }

        @Test
        void reservesOnlyTheLabelAndPaddingWhenTheOptionHasNoIcon() {
            // Left padding 4 + label 50 + trailing padding 4 = 58, no icon extent.
            assertThat(IconLabelRow.measureRowWidth(24f, 50f, false)).isEqualTo(58f);
        }
    }
}
