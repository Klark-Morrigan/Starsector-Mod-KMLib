package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the panel-to-scrollbar bridge: {@link PanelScrollbars} projects a {@link PanelPlacement} onto its
 * {@link ScrollRegion} and computes the scrollbar geometry from it, so the track lands in the body's right
 * gutter, the thumb sizes to the visible fraction, the grab column runs right of the list, and a pointer
 * maps to a scroll offset. A body wider than its viewport, and a list twice its viewport, make the geometry
 * hand-checkable.
 */
final class PanelScrollbarsTest {
    private static final float TOLERANCE = 0.01f;
    // A 200-wide body (right edge 300) over a viewport covering a narrower 120-wide list column (right edge
    // 228), and a list that overruns its 100-tall viewport by 100 (content 200 tall).
    private static final Rectangle BODY = new Rectangle(100f, 200f, 200f, 300f);
    private static final Rectangle VIEWPORT = new Rectangle(108f, 250f, 120f, 100f);
    private static final float OVERFLOW = 100f;

    private static PanelPlacement placement(float scrollOffset) {
        // The box is unused by PanelScrollbars (it projects the body's gutter), so the body doubles as it.
        return new PanelPlacement(BODY, BODY, List.of(), VIEWPORT, scrollOffset, OVERFLOW);
    }

    @Nested
    class ComputeTrack {

        @Test
        void computeTrackLandsInTheBodyRightGutterSpanningTheViewport() {
            var track = PanelScrollbars.computeTrack(placement(0f));
            // Right of the list column (228), within the body's right edge (300), and as tall as the
            // viewport - so it sits in the gutter, projected from the placement's scroll region.
            assertThat(track.x()).isGreaterThan(VIEWPORT.x() + VIEWPORT.width());
            assertThat(track.x() + track.width())
                    .isCloseTo(BODY.x() + BODY.width() - Scrollbar.DEFAULT_RIGHT_MARGIN, within(TOLERANCE));
            assertThat(track.y()).isCloseTo(VIEWPORT.y(), within(TOLERANCE));
            assertThat(track.height()).isCloseTo(VIEWPORT.height(), within(TOLERANCE));
        }
    }

    @Nested
    class ComputeThumb {

        @Test
        void computeThumbSizesTheThumbToTheVisibleFractionOfTheContent() {
            var thumb = PanelScrollbars.computeThumb(placement(0f));
            // Half the content is visible, so the thumb is half the track (and the viewport) height.
            assertThat(thumb.height()).isCloseTo(VIEWPORT.height() / 2f, within(TOLERANCE));
        }

        @Test
        void computeThumbDropsTheThumbAsTheListScrolls() {
            var atTop = PanelScrollbars.computeThumb(placement(0f));
            var scrolled = PanelScrollbars.computeThumb(placement(OVERFLOW));
            assertThat(scrolled.y()).isLessThan(atTop.y());
        }
    }

    @Nested
    class ComputeGrabColumn {

        @Test
        void computeGrabColumnRunsTheGutterRightOfTheList() {
            var grab = PanelScrollbars.computeGrabColumn(placement(0f));
            // From the viewport's right edge (228) to the body's right edge (300), at the viewport height.
            assertThat(grab.x()).isCloseTo(VIEWPORT.x() + VIEWPORT.width(), within(TOLERANCE));
            assertThat(grab.x() + grab.width())
                    .isCloseTo(BODY.x() + BODY.width(), within(TOLERANCE));
            assertThat(grab.height()).isCloseTo(VIEWPORT.height(), within(TOLERANCE));
        }
    }

    @Nested
    class ResolveOffsetForPointer {

        @Test
        void resolveOffsetForPointerIsZeroAtTheTrackTop() {
            var track = PanelScrollbars.computeTrack(placement(0f));
            var offset = PanelScrollbars.resolveOffsetForPointer(placement(0f),
                    track.y() + track.height());
            assertThat(offset).isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void resolveOffsetForPointerIsTheOverflowAtTheTrackBottom() {
            var track = PanelScrollbars.computeTrack(placement(0f));
            var offset = PanelScrollbars.resolveOffsetForPointer(placement(0f), track.y());
            assertThat(offset).isCloseTo(OVERFLOW, within(TOLERANCE));
        }
    }
}
