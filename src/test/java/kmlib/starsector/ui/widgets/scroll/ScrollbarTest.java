package kmlib.starsector.ui.widgets.scroll;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the scrollbar geometry over a {@link ScrollRegion}: {@link Scrollbar#computeTrack} lands the track
 * in the container's right gutter, {@link Scrollbar#computeThumb} sizes the thumb to the visible fraction
 * and slides it down the track as the content scrolls, {@link Scrollbar#computeGrabColumn} runs the gutter
 * right of the content, and {@link Scrollbar#resolveOffsetForPointer} maps a pointer on the track back to a
 * scroll offset. A container wider than its viewport, and a content twice its viewport, make the expected
 * geometry hand-checkable.
 */
final class ScrollbarTest {
    private static final float TOLERANCE = 0.01f;
    // A 200-wide container (right edge at 300) over a viewport covering a narrower 120-wide list column
    // (right edge at 228), and a content that overruns its 100-tall viewport by 100 (content 200 tall).
    private static final Rectangle CONTAINER = new Rectangle(100f, 200f, 200f, 300f);
    private static final Rectangle VIEWPORT = new Rectangle(108f, 250f, 120f, 100f);
    private static final float OVERFLOW = 100f;

    private static ScrollRegion buildRegion(float offset) {
        return new ScrollRegion(CONTAINER, VIEWPORT, offset, OVERFLOW);
    }

    // The track is offset-independent (it comes from the container and viewport), so one track serves
    // every thumb and pointer case.
    private static Rectangle track() {
        return Scrollbar.computeTrack(buildRegion(0f));
    }

    @Nested
    class ComputeTrack {

        @Test
        void computeTrackSetsTheTrackInFromTheContainerRightEdge() {
            var track = track();
            // The track's right edge sits the default margin in from the container's right edge (300), at
            // the default width - so it lands in the container's gutter, not against the list column.
            assertThat(track.x() + track.width())
                .isCloseTo(300f - Scrollbar.DEFAULT_RIGHT_MARGIN, within(TOLERANCE));
            assertThat(track.width()).isCloseTo(Scrollbar.DEFAULT_TRACK_WIDTH, within(TOLERANCE));
        }

        @Test
        void computeTrackSpansTheViewportVertically() {
            var track = track();
            assertThat(track.y()).isCloseTo(VIEWPORT.y(), within(TOLERANCE));
            assertThat(track.height()).isCloseTo(VIEWPORT.height(), within(TOLERANCE));
        }
    }

    @Nested
    class ComputeThumb {

        @Test
        void computeThumbSizesTheThumbToTheVisibleFractionOfTheContent() {
            var thumb = Scrollbar.computeThumb(buildRegion(0f), track());
            // Half the content is visible, so the thumb is half the track height.
            assertThat(thumb.height()).isCloseTo(track().height() / 2f, within(TOLERANCE));
            assertThat(thumb.width()).isCloseTo(track().width(), within(TOLERANCE));
            assertThat(thumb.x()).isCloseTo(track().x(), within(TOLERANCE));
        }

        @Test
        void computeThumbHangsTheThumbFromTheTrackTopWhenScrolledToTheStart() {
            var thumb = Scrollbar.computeThumb(buildRegion(0f), track());
            // At offset 0 the thumb's top edge meets the track's top edge (UI y grows up).
            assertThat(thumb.y() + thumb.height())
                .isCloseTo(track().y() + track().height(), within(TOLERANCE));
        }

        @Test
        void computeThumbDropsTheThumbToTheTrackBottomWhenFullyScrolled() {
            var thumb = Scrollbar.computeThumb(buildRegion(OVERFLOW), track());
            // At the full overflow the thumb's bottom edge meets the track's bottom edge.
            assertThat(thumb.y()).isCloseTo(track().y(), within(TOLERANCE));
        }

        @Test
        void computeThumbPlacesTheThumbMidTravelAtHalfTheOverflow() {
            var thumb = Scrollbar.computeThumb(buildRegion(OVERFLOW / 2f), track());
            // Half-scrolled, the 50-tall thumb sits centred in the 100 track: its bottom is a quarter of
            // the track up from the bottom (25px of the 50px travel).
            assertThat(thumb.y()).isCloseTo(track().y() + 25f, within(TOLERANCE));
        }

        @Test
        void computeThumbFloorsTheThumbHeightForAVeryLongContent() {
            // A content far taller than the viewport would give a sub-minimum thumb; it floors at the
            // grabbable minimum so the thumb never shrinks to an unusable sliver.
            var longContent = new ScrollRegion(CONTAINER, VIEWPORT, 0f, 100000f);
            var thumb = Scrollbar.computeThumb(longContent, track());
            assertThat(thumb.height()).isCloseTo(Scrollbar.MIN_THUMB_HEIGHT, within(TOLERANCE));
        }

        @Test
        void computeThumbFillsTheTrackWhenTheContentFits() {
            // No overflow (content no taller than the viewport) leaves nothing to scroll, so the thumb
            // fills the whole track.
            var fits = new ScrollRegion(CONTAINER, VIEWPORT, 0f, 0f);
            var thumb = Scrollbar.computeThumb(fits, track());
            assertThat(thumb.height()).isCloseTo(track().height(), within(TOLERANCE));
            assertThat(thumb.y()).isCloseTo(track().y(), within(TOLERANCE));
        }
    }

    @Nested
    class ComputeGrabColumn {

        @Test
        void computeGrabColumnRunsTheGutterRightOfTheContentAtTheViewportHeight() {
            var grab = Scrollbar.computeGrabColumn(buildRegion(0f));
            // The grab column starts at the viewport's right edge (228) and runs to the container's right
            // edge (300), at the viewport's height - wider than the thin track so the thumb need not be
            // hit exactly, and right of the content so it never competes with a content click.
            assertThat(grab.x()).isCloseTo(VIEWPORT.x() + VIEWPORT.width(), within(TOLERANCE));
            assertThat(grab.x() + grab.width())
                .isCloseTo(CONTAINER.x() + CONTAINER.width(), within(TOLERANCE));
            assertThat(grab.width()).isGreaterThan(Scrollbar.DEFAULT_TRACK_WIDTH);
            assertThat(grab.y()).isCloseTo(VIEWPORT.y(), within(TOLERANCE));
            assertThat(grab.height()).isCloseTo(VIEWPORT.height(), within(TOLERANCE));
        }
    }

    @Nested
    class ResolveOffsetForPointer {

        @Test
        void resolveOffsetForPointerIsZeroAtTheTrackTop() {
            // A pointer at the very top scrolls the content to its first row (offset 0).
            var offset = Scrollbar.resolveOffsetForPointer(buildRegion(0f), track(),
                track().y() + track().height());
            assertThat(offset).isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void resolveOffsetForPointerIsTheOverflowAtTheTrackBottom() {
            // A pointer at the very bottom scrolls the content to its last row (the full overflow).
            var offset = Scrollbar.resolveOffsetForPointer(buildRegion(0f), track(), track().y());
            assertThat(offset).isCloseTo(OVERFLOW, within(TOLERANCE));
        }

        @Test
        void resolveOffsetForPointerClampsAPointerAboveTheTrack() {
            // A pointer past the top stays at 0 rather than a negative offset.
            var offset = Scrollbar.resolveOffsetForPointer(buildRegion(0f), track(),
                track().y() + track().height() + 500f);
            assertThat(offset).isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void resolveOffsetForPointerClampsAPointerBelowTheTrack() {
            var offset = Scrollbar.resolveOffsetForPointer(buildRegion(0f), track(), track().y() - 500f);
            assertThat(offset).isCloseTo(OVERFLOW, within(TOLERANCE));
        }

        @Test
        void resolveOffsetForPointerIsZeroWhenTheContentFits() {
            // Nothing to scroll, so any pointer resolves to the top.
            var fits = new ScrollRegion(CONTAINER, VIEWPORT, 0f, 0f);
            var offset = Scrollbar.resolveOffsetForPointer(fits, track(), track().y());
            assertThat(offset).isCloseTo(0f, within(TOLERANCE));
        }
    }
}
