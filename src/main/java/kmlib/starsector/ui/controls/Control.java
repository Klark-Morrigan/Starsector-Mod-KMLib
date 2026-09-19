package kmlib.starsector.ui.controls;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.specs.ControlSpec;
import kmlib.starsector.ui.controls.specs.ScrollingSectionSpec;

import java.util.List;

/**
 * One laid-out body control: the {@link ControlSpec} it came from, the {@code bounds} of its row,
 * and - for a radio - the per-option {@code segments} within that row (empty for a single-hit
 * control). The renderer draws the widget its kind names within these rectangles and the input
 * listener hit-tests the same rectangles, so the control the player sees is the one the click
 * resolves to.
 *
 * <p>{@code isScrolled} marks a control laid inside a {@link ScrollingSectionSpec}: it is
 * placed at its scrolled position, so it may sit outside the viewport the section was given, and both
 * the renderer that clips it and the hit-test that rejects points outside that viewport read this one
 * value. Carried by the laid-out control rather than derived from the spec, because the section is
 * flattened at placement and nothing downstream sees the group; carried rather than re-derived from
 * geometry, because a row scrolled exactly to the viewport edge would be classified by a rounding.
 *
 * @param spec       the control this was laid out from
 * @param bounds     the row the control occupies
 * @param segments   the per-option rectangles within the row, empty for a single-hit control
 * @param isScrolled whether the control was laid inside the strip's scrolling section
 */
public record Control(
    ControlSpec spec,
    Rectangle bounds,
    List<Rectangle> segments,
    boolean isScrolled) {

    /**
     * A pinned control - one laid outside any scrolling section, which is every control in a strip
     * that has none. The common case, so the scrolled flag is stated only where it is set.
     *
     * @param spec     the control this was laid out from
     * @param bounds   the row the control occupies
     * @param segments the per-option rectangles within the row, empty for a single-hit control
     */
    public Control(ControlSpec spec, Rectangle bounds, List<Rectangle> segments) {
        this(spec, bounds, segments, false);
    }
}
