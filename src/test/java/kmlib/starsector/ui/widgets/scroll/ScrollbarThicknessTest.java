package kmlib.starsector.ui.widgets.scroll;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins what a {@link ScrollbarThickness} answers about the bar it sizes: {@link
 * ScrollbarThickness#isTrackDrawn} separates a bar from no bar at all, and {@link
 * ScrollbarThickness#computeGutterWidth} states the width the bar needs clear of the content - the track
 * plus the two fixed gaps flanking it, and nothing about whatever padding a container already has. The
 * width itself floors at no bar, so every reading below is taken of a thickness of 0 or more.
 */
final class ScrollbarThicknessTest {

    private static final float TOLERANCE = 0.01f;

    @Nested
    class Pixels {

        @Test
        void pixelsIsThreeAtTheDefault() {
            // The width the bar has always drawn at, so a host naming no thickness sees no change.
            assertThat(ScrollbarThickness.DEFAULT.pixels())
                .isCloseTo(3f, within(TOLERANCE));
        }

        @Test
        void pixelsFloorsANegativeWidthAtNoBar() {
            // "No bar" is the thinnest a bar gets, so a width below it settles there rather than travelling
            // on into a gutter narrower than the gaps flanking the track.
            assertThat(new ScrollbarThickness(-5f).pixels())
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class IsTrackDrawn {

        @Test
        void isTrackDrawnIsTrueAtTheDefault() {

            assertThat(ScrollbarThickness.DEFAULT.isTrackDrawn())
                .isTrue();
        }

        @Test
        void isTrackDrawnIsFalseAtZero() {
            // Zero means no bar at all rather than a bar of no width, so nothing is drawn and nothing
            // claims the gutter.
            assertThat(new ScrollbarThickness(0f).isTrackDrawn())
                .isFalse();
        }
    }

    @Nested
    class ComputeGutterWidth {

        @Test
        void computeGutterWidthIsEightAtTheDefault() {
            // 3 of track, the 3 margin off the container's edge, and the 2 of clearance off the content.
            assertThat(ScrollbarThickness.DEFAULT.computeGutterWidth())
                .isCloseTo(8f, within(TOLERANCE));
        }

        @Test
        void computeGutterWidthGrowsWithTheThickness() {
            // The two gaps are constants, so the gutter grows pixel for pixel with the bar: 12 + 3 + 2.
            assertThat(new ScrollbarThickness(12f).computeGutterWidth())
                .isCloseTo(17f, within(TOLERANCE));
        }

        @Test
        void computeGutterWidthIsTheGapsAloneAtZeroThickness() {
            // No track to hold clear, but the arithmetic states the gaps regardless - what a container
            // does with a gutter its own padding already covers is the layout's call, not this type's.
            assertThat(new ScrollbarThickness(0f).computeGutterWidth())
                .isCloseTo(5f, within(TOLERANCE));
        }
    }
}
