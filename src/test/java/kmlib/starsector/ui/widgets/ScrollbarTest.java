package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the scrollbar thumb geometry: {@link Scrollbar#computeThumb} sizes the thumb to the visible
 * fraction of the content and slides it down the track as the content scrolls, and {@link
 * Scrollbar#resolveOffsetForPointer} maps a pointer on the track back to a scroll offset. Round track
 * and content sizes keep every expected rectangle a hand-checkable fraction.
 */
final class ScrollbarTest {
    private static final float TOLERANCE = 0.01f;
    // A 100-tall track beside a 200-tall content scrolling within a 100-tall viewport: the content
    // overruns by 100, and half of it is visible, so the thumb is half the track.
    private static final Rectangle TRACK = new Rectangle(500f, 200f, 4f, 100f);
    private static final float CONTENT_HEIGHT = 200f;
    private static final float VIEWPORT_HEIGHT = 100f;
    private static final float OVERFLOW = CONTENT_HEIGHT - VIEWPORT_HEIGHT;

    @Nested
    class ComputeRightGutterTrack {
        // A 200-wide container (right edge at 300) over a viewport that covers a narrower list column,
        // so the track pins to the container's edge rather than the viewport's.
        private static final Rectangle CONTAINER = new Rectangle(100f, 0f, 200f, 500f);
        private static final Rectangle VIEWPORT = new Rectangle(110f, 50f, 120f, 300f);

        @Test
        void computeRightGutterTrackSetsTheTrackInFromTheContainerRightEdge() {
            var track = Scrollbar.computeRightGutterTrack(CONTAINER, VIEWPORT, 3f, 2f);
            // The track's right edge sits the margin in from the container's right edge (300), and it is
            // the requested width - so it lands in the container's gutter, not against the list column.
            assertThat(track.x() + track.width()).isCloseTo(298f, within(TOLERANCE));
            assertThat(track.width()).isCloseTo(3f, within(TOLERANCE));
        }

        @Test
        void computeRightGutterTrackSpansTheViewportVertically() {
            var track = Scrollbar.computeRightGutterTrack(CONTAINER, VIEWPORT, 3f, 2f);
            assertThat(track.y()).isCloseTo(VIEWPORT.y(), within(TOLERANCE));
            assertThat(track.height()).isCloseTo(VIEWPORT.height(), within(TOLERANCE));
        }
    }

    @Nested
    class ComputeThumb {

        @Test
        void computeThumbSizesTheThumbToTheVisibleFractionOfTheContent() {
            var thumb = Scrollbar.computeThumb(TRACK, CONTENT_HEIGHT, VIEWPORT_HEIGHT, 0f);
            // Half the content is visible, so the thumb is half the track height.
            assertThat(thumb.height()).isCloseTo(50f, within(TOLERANCE));
            assertThat(thumb.width()).isCloseTo(TRACK.width(), within(TOLERANCE));
            assertThat(thumb.x()).isCloseTo(TRACK.x(), within(TOLERANCE));
        }

        @Test
        void computeThumbHangsTheThumbFromTheTrackTopWhenScrolledToTheStart() {
            var thumb = Scrollbar.computeThumb(TRACK, CONTENT_HEIGHT, VIEWPORT_HEIGHT, 0f);
            // At offset 0 the thumb's top edge meets the track's top edge (UI y grows up).
            assertThat(thumb.y() + thumb.height())
                    .isCloseTo(TRACK.y() + TRACK.height(), within(TOLERANCE));
        }

        @Test
        void computeThumbDropsTheThumbToTheTrackBottomWhenFullyScrolled() {
            var thumb = Scrollbar.computeThumb(TRACK, CONTENT_HEIGHT, VIEWPORT_HEIGHT, OVERFLOW);
            // At the full overflow the thumb's bottom edge meets the track's bottom edge.
            assertThat(thumb.y()).isCloseTo(TRACK.y(), within(TOLERANCE));
        }

        @Test
        void computeThumbPlacesTheThumbMidTravelAtHalfTheOverflow() {
            var thumb = Scrollbar.computeThumb(TRACK, CONTENT_HEIGHT, VIEWPORT_HEIGHT, OVERFLOW / 2f);
            // Half-scrolled, the 50-tall thumb sits centred in the 100 track: its bottom is a quarter of
            // the track up from the bottom (25px of the 50px travel).
            assertThat(thumb.y()).isCloseTo(TRACK.y() + 25f, within(TOLERANCE));
        }

        @Test
        void computeThumbFloorsTheThumbHeightForAVeryLongList() {
            // A list far taller than the viewport would give a sub-minimum thumb; it floors at the
            // grabbable minimum so the thumb never shrinks to an unusable sliver.
            var thumb = Scrollbar.computeThumb(TRACK, 100000f, VIEWPORT_HEIGHT, 0f);
            assertThat(thumb.height()).isCloseTo(Scrollbar.MIN_THUMB_HEIGHT, within(TOLERANCE));
        }

        @Test
        void computeThumbFillsTheTrackWhenTheContentFits() {
            // No overflow (content shorter than the viewport) leaves nothing to scroll, so the thumb
            // fills the whole track.
            var thumb = Scrollbar.computeThumb(TRACK, 40f, VIEWPORT_HEIGHT, 0f);
            assertThat(thumb.height()).isCloseTo(TRACK.height(), within(TOLERANCE));
            assertThat(thumb.y()).isCloseTo(TRACK.y(), within(TOLERANCE));
        }
    }

    @Nested
    class ResolveOffsetForPointer {

        @Test
        void resolveOffsetForPointerIsZeroAtTheTrackTop() {
            // A pointer at the very top scrolls the content to its first row (offset 0).
            var offset = Scrollbar.resolveOffsetForPointer(TRACK, CONTENT_HEIGHT, VIEWPORT_HEIGHT,
                    TRACK.y() + TRACK.height());
            assertThat(offset).isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void resolveOffsetForPointerIsTheOverflowAtTheTrackBottom() {
            // A pointer at the very bottom scrolls the content to its last row (the full overflow).
            var offset = Scrollbar.resolveOffsetForPointer(TRACK, CONTENT_HEIGHT, VIEWPORT_HEIGHT,
                    TRACK.y());
            assertThat(offset).isCloseTo(OVERFLOW, within(TOLERANCE));
        }

        @Test
        void resolveOffsetForPointerClampsAPointerAboveTheTrack() {
            // A pointer past the top stays at 0 rather than a negative offset.
            var offset = Scrollbar.resolveOffsetForPointer(TRACK, CONTENT_HEIGHT, VIEWPORT_HEIGHT,
                    TRACK.y() + TRACK.height() + 500f);
            assertThat(offset).isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void resolveOffsetForPointerClampsAPointerBelowTheTrack() {
            var offset = Scrollbar.resolveOffsetForPointer(TRACK, CONTENT_HEIGHT, VIEWPORT_HEIGHT,
                    TRACK.y() - 500f);
            assertThat(offset).isCloseTo(OVERFLOW, within(TOLERANCE));
        }

        @Test
        void resolveOffsetForPointerIsZeroWhenTheContentFits() {
            // Nothing to scroll, so any pointer resolves to the top.
            var offset = Scrollbar.resolveOffsetForPointer(TRACK, 40f, VIEWPORT_HEIGHT, TRACK.y());
            assertThat(offset).isCloseTo(0f, within(TOLERANCE));
        }
    }
}
