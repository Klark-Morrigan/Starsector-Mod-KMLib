package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link IconLabelRow}'s geometry: the icon is a square inset off the row's top and bottom and
 * flush with its left padding, the label anchors past the icon (or at the left inset for an icon-less
 * option), a trailing value right-aligns to the row's right inset, and the measured row width reserves
 * the icon and the value only when the option has each - so the drawn icon, the drawn label, the drawn
 * value, and the sized column all read the same layout.
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
    class ComputeTrailingAnchorX {
        @Test
        void anchorsAtTheRightEdgeLessTheTrailingInset() {
            // Row right edge 10 + 100 = 110, less trailing padding 4 -> 106; the value right-aligns
            // here so a stack of equal-width rows lines its values up.
            assertThat(IconLabelRow.computeTrailingAnchorX(row)).isEqualTo(106f);
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

        @Test
        void reservesTheTrailingValueAndItsGapWhenTheOptionHasAValue() {
            // Left padding 4 + (icon 20 + gap 6) + label 50 + (value gap 6 + value 15) + trailing
            // padding 4 = 105, the icon-and-value case the picker's ranked rows take.
            assertThat(IconLabelRow.measureRowWidth(24f, 50f, true, 15f)).isEqualTo(105f);
        }

        @Test
        void reservesNoTrailingRoomWhenTheValueWidthIsZero() {
            // A zero-width value is "no value", so the four-arg width matches the three-arg one and a
            // value-less row is sized exactly as before.
            assertThat(IconLabelRow.measureRowWidth(24f, 50f, true, 0f))
                    .isEqualTo(IconLabelRow.measureRowWidth(24f, 50f, true));
        }
    }
}
