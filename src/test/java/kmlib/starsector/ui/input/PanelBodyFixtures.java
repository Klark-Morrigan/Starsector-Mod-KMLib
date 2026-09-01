package kmlib.starsector.ui.input;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.LabelledControlSpecs;
import kmlib.starsector.ui.controls.VerticalTableSpecs;
import kmlib.starsector.ui.widgets.PanelPlacement;

import java.util.Arrays;
import java.util.List;

/**
 * Laid-out body controls and the placements they stand in, for the cases about what a point on a panel's
 * body resolves to and what pressing it does. One set shared by the resolver, the activation and the
 * controller that drives both, since all three ask about the same strip: a fixture spelled twice is two
 * strips that can drift, and a case pinning the resolver would then be answering about a body the
 * controller's cases never lay.
 *
 * <p>Every control is laid at {@link #ROW}, so a case states which kind of control it is about and where it
 * points, never the geometry. The viewport and the box are the two gates a walk gets to fail on, so each
 * has a builder that widens one to nothing and narrows the other to what the case is exercising.
 */
final class PanelBodyFixtures {

    /** The row every control here is laid at - away from the origin, nothing turning on where it sits. */
    static final Rectangle ROW = new Rectangle(100f, 200f, 120f, 20f);

    /**
     * A viewport covering the whole row, so a non-scrolling control's hit-test ignores it; the cases about
     * the clip pass a viewport of their own.
     */
    static final Rectangle FULL_VIEWPORT = new Rectangle(0f, 0f, 10000f, 10000f);

    private PanelBodyFixtures() {
    }

    /**
     * A panel whose body holds the given controls and whose flex viewport covers everything, so the walk
     * over it is clipped only in the cases that build a viewport of their own.
     *
     * @param bodyControls the controls laid in the body, top to bottom
     * @return the placement
     */
    static PanelPlacement buildBodyPlacement(Control... bodyControls) {
        return buildBodyPlacement(FULL_VIEWPORT, bodyControls);
    }

    /**
     * The same panel with the flex viewport a case wants to exercise the clip with. Its box covers
     * everything, so the walk's own box gate passes and the case is about the clip it names.
     *
     * @param flexViewport the viewport a scrolling control is clipped to
     * @param bodyControls the controls laid in the body, top to bottom
     * @return the placement
     */
    static PanelPlacement buildBodyPlacement(Rectangle flexViewport, Control... bodyControls) {
        return buildBodyPlacement(FULL_VIEWPORT, flexViewport, bodyControls);
    }

    /**
     * The same panel narrowed to the box a case wants to exercise the fold with, its flex viewport covering
     * everything so the box is the only gate the walk can fail on.
     *
     * @param box          the box the body is drawn inside
     * @param bodyControls the controls laid in the body, top to bottom
     * @return the placement
     */
    static PanelPlacement buildBodyPlacementBoxedTo(Rectangle box, Control... bodyControls) {
        return buildBodyPlacement(box, FULL_VIEWPORT, bodyControls);
    }

    /**
     * The rail a fully docked panel leaves of its box: a border's width at the body's left edge, well clear
     * of {@link #ROW}, so a control still laid out there is one the fold has wiped off the screen.
     *
     * @return the docked rail's box
     */
    static Rectangle buildDockedRailBox() {
        return new Rectangle(ROW.x(), ROW.y(), 1f, ROW.height());
    }

    /**
     * A flex viewport sitting well above {@link #ROW}, so a scrolling control laid out there reads as a row
     * that has scrolled up out of sight under whatever is pinned above the list.
     *
     * @return the viewport above the row
     */
    static Rectangle buildViewportAboveRow() {
        return new Rectangle(ROW.x(), ROW.y() + 100f, ROW.width(), 40f);
    }

    /**
     * A single-row checkbox occupying {@link #ROW}, so each case states only the label and action that
     * distinguish it rather than repeating the spec-and-bounds construction.
     *
     * @param label  the checkbox's label
     * @param action what a click on the row does
     * @return the laid-out checkbox
     */
    static Control buildCheckboxControl(String label, ControlAction action) {
        return new Control(LabelledControlSpecs.buildCheckbox(label, false, action), ROW, List.of());
    }

    /**
     * A caption occupying {@link #ROW} - chrome, drawn but never clickable, and so what every case about a
     * control that is not a hit target reads. Its label says nothing, no case turning on the words.
     *
     * @return the laid-out caption
     */
    static Control buildCaptionControl() {
        return new Control(LabelledControlSpecs.buildLabel("Caption"), ROW, List.of());
    }

    /**
     * A divider occupying {@link #ROW} - the other chrome, and the one that matters to a walk because it
     * spans the whole body width, so it is what a press between two controls actually lands on.
     *
     * @return the laid-out divider
     */
    static Control buildDividerControl() {
        return new Control(new ControlSpec.Divider(), ROW, List.of());
    }

    /**
     * A one-option scrolling list laid out at {@link #ROW}: a vertical icon table marked as the scroll
     * region, its single segment the whole row, so a point at the row hits option 0 unless the viewport
     * clips it.
     *
     * @param action what a click on the option does
     * @return the laid-out scrolling list
     */
    static Control buildScrollingListAtRow(ControlAction action) {

        var spec = VerticalTableSpecs.buildIconList(
                List.of("Opt"),
                Arrays.asList((String) null),
                ControlSpec.NO_SELECTION,
                action)
            .asScrolling();

        return new Control(spec, ROW, List.of(ROW));
    }

    /**
     * A two-segment horizontal radio occupying {@link #ROW}, split into two equal segment boxes (left,
     * right), so a point in a segment's box hits that segment - and a press on the lit segment fires or is
     * swallowed by the radio's own reselect. The caller supplies the spec so a case picks the deselectable
     * or the inert row.
     *
     * @param spec the radio spec to lay out
     * @return the laid-out radio
     */
    static Control buildTwoSegmentHorizontalRadioAtRow(ControlSpec.HorizontalRadio spec) {
        return new Control(spec, ROW, buildTwoHalvesOfRow());
    }

    /**
     * A two-tab row occupying {@link #ROW}, split into two equal per-tab boxes (left tab, right tab), the
     * lit tab at {@code selectedIndex}, so a point in a tab's box hits that tab unless its own inert-on-lit
     * reselect swallows it. Shortcuts are irrelevant to the hit-test, so the tabs carry none.
     *
     * @param selectedIndex the lit tab's index
     * @param action        what a click on a tab does
     * @return the laid-out tabs row
     */
    static Control buildTwoTabRowAtRow(int selectedIndex, ControlAction action) {

        var spec = new ControlSpec.Tabs(
            List.of("Political Map", "Alliances"),
            List.of(),
            selectedIndex,
            action);

        return new Control(spec, ROW, buildTwoHalvesOfRow());
    }

    // The panel every case is walked against: a box the body is drawn inside, a flex viewport its scrolling
    // control is clipped to, and the controls laid in it. The body region is the box, nothing reading it.
    private static PanelPlacement buildBodyPlacement(
            Rectangle box,
            Rectangle flexViewport,
            Control... bodyControls) {

        return new PanelPlacement(
            box,
            box,
            List.of(bodyControls),
            flexViewport,
            0f,
            0f);
    }

    // The row split into two equal boxes, which is how both segmented fixtures lay their cells - a radio's
    // pair and a tabs row's pair being the same geometry under different specs.
    private static List<Rectangle> buildTwoHalvesOfRow() {

        var leftHalf = new Rectangle(ROW.x(), ROW.y(), ROW.width() / 2f, ROW.height());
        var rightHalf = new Rectangle(
            ROW.x() + ROW.width() / 2f,
            ROW.y(),
            ROW.width() / 2f,
            ROW.height());

        return List.of(leftHalf, rightHalf);
    }
}
