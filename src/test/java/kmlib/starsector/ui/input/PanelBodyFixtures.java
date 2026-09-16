package kmlib.starsector.ui.input;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlAction;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.LabelledControlSpecs;
import kmlib.starsector.ui.controls.VerticalTableSpecs;
import kmlib.starsector.ui.widgets.PanelPlacement;
import kmlib.starsector.ui.widgets.scroll.ScrollbarThickness;

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
 *
 * <p>Beside them the bodies with somewhere to scroll, for the cases about the wheel and the scrollbar: one
 * whose viewport is the whole row, and one with the row narrowed off the box's right edge to leave the gutter
 * a drag grabs the bar by. Shared by the end that moves the list and the controller that routes to it, since
 * a case about the routing has to lay the same body the rule it routes to was pinned on.
 */
final class PanelBodyFixtures {

    /** The row every control here is laid at - away from the origin, nothing turning on where it sits. */
    static final Rectangle ROW = new Rectangle(100f, 200f, 120f, 20f);

    /**
     * A viewport covering the whole row, so a non-scrolling control's hit-test ignores it; the cases about
     * the clip pass a viewport of their own.
     */
    static final Rectangle FULL_VIEWPORT = new Rectangle(0f, 0f, 10000f, 10000f);

    /**
     * How far the scrolling bodies overrun their viewport: less than one wheel notch scrolls, so a single
     * wheel turn takes the list to its end and the one after it has nowhere to go. That pair is what tells a
     * list that moved from a list already against its stop.
     */
    static final float SHORT_SCROLL_OVERFLOW = 20f;

    /**
     * How wide a gutter the guttered body leaves right of its list, so a press there lands in the column a
     * drag grabs the scrollbar by. Wider than the track, which is what the real grab column is.
     */
    static final float SCROLLBAR_GUTTER_WIDTH = 20f;

    /**
     * A point over the list itself - inside the box and inside the scroll region, the only place a wheel
     * reaches the list at all.
     */
    static final float ON_LIST_X = ROW.x() + ROW.width() / 2f;
    static final float ON_LIST_Y = ROW.y() + ROW.height() / 2f;

    /**
     * A point in the guttered body's grab column: right of the list and still inside the box, and low in the
     * row so a drag mapped from it carries the list toward its end rather than leaving it where it was.
     */
    static final float IN_GRAB_COLUMN_X = ROW.x() + ROW.width() - SCROLLBAR_GUTTER_WIDTH / 2f;
    static final float IN_GRAB_COLUMN_Y = ROW.y() + 1f;

    // A body whose content fits, which is what leaves a panel with no scrollbar and a wheel with nothing to
    // move.
    private static final float NO_SCROLL_OVERFLOW = 0f;

    private PanelBodyFixtures() {
    }

    /**
     * A panel whose body has somewhere to scroll and nothing laid in it: its viewport is the row and its
     * content overruns it by {@link #SHORT_SCROLL_OVERFLOW}, so one notch takes the list to its end and the
     * next has nowhere to go.
     *
     * @return the placement
     */
    static PanelPlacement buildScrollingPlacement() {
        return buildScrollingPlacementOver();
    }

    /**
     * The same panel with the given controls laid in it, for a case that has to tell what the wheel and the
     * scrollbar answer from what a control does - a body with nothing in it would be silent either way.
     *
     * @param bodyControls the controls laid in the body, top to bottom
     * @return the placement
     */
    static PanelPlacement buildScrollingPlacementOver(Control... bodyControls) {
        return buildScrollingPlacementAtThickness(ScrollbarThickness.DEFAULT, bodyControls);
    }

    /**
     * The same panel with the bar's width named, for the cases about a host that has set it away. Taken as
     * an argument rather than written into a second placement, so a barless case and the cases above differ
     * in that one number and in nothing else.
     *
     * @param thickness    how wide the bar draws
     * @param bodyControls the controls laid in the body, top to bottom
     * @return the placement
     */
    static PanelPlacement buildScrollingPlacementAtThickness(
            ScrollbarThickness thickness,
            Control... bodyControls) {

        return new PanelPlacement(
            ROW,
            ROW,
            List.of(bodyControls),
            ROW,
            0f,
            SHORT_SCROLL_OVERFLOW,
            thickness);
    }

    /**
     * The same panel whose content fits, so there is no scrollbar and the wheel moves nothing.
     *
     * @return the placement
     */
    static PanelPlacement buildUnscrollablePlacement() {
        return new PanelPlacement(
            ROW,
            ROW,
            List.of(),
            ROW,
            0f,
            NO_SCROLL_OVERFLOW,
            ScrollbarThickness.DEFAULT);
    }

    /**
     * The same scrolling panel with its list narrowed off the box's right edge, leaving the gutter a drag
     * grabs the scrollbar by. The scrolling body above lays the viewport across the whole box, which leaves
     * no gutter at all - so the drag and the off-the-list wheel need a body shaped like the real one.
     *
     * @return the placement
     */
    static PanelPlacement buildGutteredPlacement() {
        return buildGutteredPlacementOver();
    }

    /**
     * The guttered panel with the given controls laid across the whole row, gutter included - which is where
     * the real ones sit, the grab column being drawn over the body rather than beside it. What a press in
     * that column answers is then a question the placement can actually pose.
     *
     * @param bodyControls the controls laid in the body, top to bottom
     * @return the placement
     */
    static PanelPlacement buildGutteredPlacementOver(Control... bodyControls) {
        return buildGutteredPlacementAtThickness(ScrollbarThickness.DEFAULT, bodyControls);
    }

    /**
     * The guttered panel with the bar's width named, for the same reason the scrolling one takes it: the
     * barless cases have to be the drawn ones with one number changed, the gutter and the list being where
     * they always were.
     *
     * @param thickness    how wide the bar draws
     * @param bodyControls the controls laid in the body, top to bottom
     * @return the placement
     */
    static PanelPlacement buildGutteredPlacementAtThickness(
            ScrollbarThickness thickness,
            Control... bodyControls) {

        var list = new Rectangle(ROW.x(), ROW.y(), ROW.width() - SCROLLBAR_GUTTER_WIDTH, ROW.height());
        return new PanelPlacement(
            ROW,
            ROW,
            List.of(bodyControls),
            list,
            0f,
            SHORT_SCROLL_OVERFLOW,
            thickness);
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
            action);

        // Laid inside a scrolling section, which is what the viewport-limited hit-test reads.
        return new Control(spec, ROW, List.of(ROW), true);
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
            0f,
            ScrollbarThickness.DEFAULT);
    }

    // The row split into two equal boxes, which is how both segmented fixtures lay their cells - a radio's
    // pair and a tabs row's pair being the same geometry under different specs.
    /**
     * A two-cell stacked radio occupying {@link #ROW}, its cells the row's top and bottom halves, so a
     * point in a cell's box hits that cell. The stacked counterpart of {@link
     * #buildTwoSegmentHorizontalRadioAtRow}, for the cases about which way a radio's cells run.
     *
     * @param spec the stacked radio to lay out
     * @return the laid-out stacked radio
     */
    static Control buildTwoCellVerticalRadioAtRow(ControlSpec.VerticalRadio spec) {
        return new Control(spec, ROW, buildTwoStackedHalvesOfRow());
    }

    // The row's two stacked halves, top cell first - UI y grows up, so the first cell hangs from the top
    // edge exactly as the grid splitter lays one.
    private static List<Rectangle> buildTwoStackedHalvesOfRow() {

        var half = ROW.height() / 2f;
        var topHalf = new Rectangle(ROW.x(), ROW.y() + half, ROW.width(), half);
        var bottomHalf = new Rectangle(ROW.x(), ROW.y(), ROW.width(), half);

        return List.of(topHalf, bottomHalf);
    }

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
