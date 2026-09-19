package kmlib.starsector.ui.input;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.specs.ControlSpec;
import kmlib.starsector.ui.controls.specs.InteractiveSpec;
import kmlib.starsector.ui.widgets.PanelPlacement;
import kmlib.starsector.ui.widgets.RadioRow;

/**
 * Which control of a laid-out panel a point is on, and which cell of that control - the one hit-test a
 * panel answers its body with, read by the press that fires a control, by the hover that lights one, and
 * by the header resolver of a tab panel. Two readers of one walk rather than two walks that happen to
 * agree: the control that lights and the control a press lands on are the same control because they are
 * the same answer.
 *
 * <p>It holds no state and belongs to no panel, which is why it is here rather than on one. A panel's
 * controller holds what one panel is currently doing - its scroll, its fades, its lifts - while this
 * answers a question about a placement and a point, and the same question is asked by a record that has
 * no controller at all and by a tab panel about its own header.
 *
 * <p>Geometry and visibility, and nothing else. It answers subject to what is actually on screen - a
 * folded body is behind its rail, a scrolled row is behind its viewport - and to nothing about what
 * pressing the cell it reports would <em>do</em>. That is {@link ControlActivation}'s question, and the
 * split is what lets a hover read exactly what a press reads: a tabs row lights the tab it is already
 * showing and fires nothing there, so a hover that took the press's answer would leave that tab dark.
 */
final class ControlHitResolver {

    // No cell: what a hit-test reports when the point missed every cell or landed on chrome that is not a
    // hit target, and what the firing step reports when the cell it was handed turned out to be inert. Null
    // rather than an index sentinel, because the answer is "which cell, if any": an out-of-range index reads
    // as a cell like any other to a caller keying anything by it, while a null cannot be keyed by at all.
    //
    // Named once here and read by every path that resolves or consumes a cell, a second name for one null
    // being a second place to explain why it is not an index.
    static final Integer NO_CELL_RESOLVED = null;

    private ControlHitResolver() {
    }

    /**
     * Resolves which cell of which body control a point lands on.
     *
     * <p>Taking the placement rather than a control is the whole point of it. Both of the things that decide
     * whether a laid-out control is on screen live on it - the box the body is drawn inside and {@link
     * PanelPlacement#flexViewport()} the scrolling list is clipped to - so each reaches the hit-test without
     * any caller having to remember to hand it over. A row scrolled up under a pinned control (or down under
     * a footer) keeps its segment exactly where the layout put it, and a folding panel narrows its box over
     * controls that keep their laid-out places: a caller walking the strip for itself would find both
     * hittable, and lightable, straight through whatever is drawn over them.
     *
     * @param placement the laid-out panel the renderer drew this frame
     * @param pointX    the point's x, in UI coordinates
     * @param pointY    the point's y, in UI coordinates
     * @return the control under the point and the slot it sits at, or {@code null} when it is over none
     */
    static ResolvedBodyCell resolveHitBodyCell(
            PanelPlacement placement,
            float pointX,
            float pointY) {

        // The body is drawn within its box and wiped with it, so a point outside the box is on none of the
        // controls laid inside: a collapsing panel narrows the box while its controls keep their laid-out
        // positions, leaving a strip of them behind the rail that is on screen nowhere.
        if (!placement.box().containsPoint(pointX, pointY)) {
            return null;
        }
        var bodyControls = placement.bodyControls();

        // Walked by index rather than over the list, the index being half of where the hit is: a fade is
        // held against the slot a control occupies, so the walk that finds the control reports the slot too
        // rather than leaving a reader to search the strip again for the position it just passed through.
        for (var controlIndex = 0; controlIndex < bodyControls.size(); controlIndex++) {

            var control = bodyControls.get(controlIndex);
            var resolvedCell = resolveHitCell(control, placement.flexViewport(), pointX, pointY);

            // A caption, a divider, and a scrolled-away row all report no cell, so the walk carries on past
            // them to the controls below rather than stopping on the first thing whose row the point is in.
            if (resolvedCell != NO_CELL_RESOLVED) {
                return new ResolvedBodyCell(control, new BodyCellSlot(controlIndex, resolvedCell));
            }
        }
        return null;
    }

    /**
     * Resolves which cell of a control in a scrollable strip a point lands on: a control laid inside the
     * strip's scrolling section ({@link Control#isScrolled()}) counts only inside {@code flexViewport},
     * and otherwise resolves as {@link #resolveHitCell(Control, float, float)}. A scrolled control clips
     * because a row scrolled up under a pinned header (or down under a footer) is drawn away, so its
     * segment - still laid out at its scrolled position - must not stay hittable through the control that
     * hides it. Every pinned control ignores the viewport, so the clip bites only the scrolled run.
     *
     * @param control      the laid-out control to hit-test
     * @param flexViewport the scrolling section's viewport; a scrolled control only counts inside it
     * @param pointX       the point's x, in UI coordinates
     * @param pointY       the point's y, in UI coordinates
     * @return the cell under the point, or {@code null} when it lands on none
     */
    static Integer resolveHitCell(
            Control control,
            Rectangle flexViewport,
            float pointX,
            float pointY) {

        if (control.isScrolled() && !flexViewport.containsPoint(pointX, pointY)) {
            return NO_CELL_RESOLVED;
        }
        return resolveHitCell(control, pointX, pointY);
    }

    /**
     * Resolves which cell of a control a point lands on, without firing anything. A radio or a tabs row hits
     * by segment over the segments the layout laid; a single-cell checkbox or toggle hits anywhere on its
     * row, reported as {@link ControlSpec#SINGLE_CELL}. A caption label or a divider is not a hit target and
     * resolves to no cell, so a press falls through to a control below rather than being swallowed on an
     * inert action.
     *
     * <p>Geometry and nothing else: the lit segment of an inert radio resolves to itself here, even though a
     * press on it fires nothing. Whether a cell would act is {@link ControlActivation}'s question, which is
     * what lets a hover and a press share this one answer - a control that lights the cell it is already
     * showing is the common case, not the exception.
     *
     * <p>The one resolver that takes no viewport, for a control that never scrolls: a tab panel's header is
     * one, laid outside the body and clipped by the fold rather than by a viewport.
     *
     * @param control the laid-out control to hit-test
     * @param pointX  the point's x, in UI coordinates
     * @param pointY  the point's y, in UI coordinates
     * @return the cell under the point, or {@code null} when it lands on none
     */
    static Integer resolveHitCell(Control control, float pointX, float pointY) {
        // A caption row and a divider are drawn but not clickable - they are not Interactive - so a press
        // over either hits nothing and falls through to let the loop try the controls below, never
        // consuming a click as if it acted. The divider matters here because it spans the whole body width.
        if (!(control.spec() instanceof InteractiveSpec interactive)) {
            return NO_CELL_RESOLVED;
        }
        // A radio or a tabs row hits by segment over the segments the layout laid - a radio's equal cells
        // or a tabs row's per-tab boxes.
        if (interactive.isSegmented()) {

            var segmentIndex = RadioRow.findSegmentIndexAt(control.segments(), pointX, pointY);

            // Branched rather than a ternary: a conditional mixing the boxed no-cell answer with the int
            // index unboxes both arms, so the miss case would throw on the null instead of reporting it.
            if (segmentIndex == RadioRow.NO_SEGMENT) {
                return NO_CELL_RESOLVED;
            }
            return segmentIndex;
        }
        if (!control.bounds().containsPoint(pointX, pointY)) {
            return NO_CELL_RESOLVED;
        }
        return ControlSpec.SINGLE_CELL;
    }

    /**
     * Whether a laid-out control is one of the segmented kinds - a radio, a table, or a tabs row - as
     * opposed to a whole-row control hit anywhere on its bounds. The same rule the hit-tests above turn on,
     * asked of the control rather than of its spec, for a reader holding one and nothing to narrow.
     *
     * <p>Chrome answers no. A caption or a divider has no cells at all, so nothing about it is one of many
     * alike - and nothing about it ever resolves to a cell to ask this of in the first place.
     *
     * @param control the laid-out control
     * @return whether its cells are segments laid side by side
     */
    static boolean isSegmentedControl(Control control) {
        return control.spec() instanceof InteractiveSpec interactive
            && interactive.isSegmented();
    }
}
