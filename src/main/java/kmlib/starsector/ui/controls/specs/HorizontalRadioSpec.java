package kmlib.starsector.ui.controls.specs;

import kmlib.text.KmlibStrings;

import java.util.List;

/**
 * A row of mutually exclusive option segments laid side by side. Its {@link SegmentSizing} picks how
 * the segments size: {@link SegmentSizing#UNIFORM} gives every segment the widest label's width (even
 * cells, the default an option pair reads as), {@link SegmentSizing#SNAPPED} gives each its own
 * label's width (a ragged row that would waste space as even cells). Its {@link ReselectBehaviour}
 * sets what a re-pick of the lit segment does: an option pair is {@link ReselectBehaviour#INERT}
 * (always one lit, the standard row), while a {@link ReselectBehaviour#DESELECT} row clears to
 * nothing on a re-pick - so a horizontal radio can read as the on/off selector a {@link
 * VerticalTableSpec} did, only laid across one row.
 *
 * @param labels        the option labels, left to right, in segment order
 * @param selectedIndex the lit option's index, or {@link #NO_SELECTION} when nothing is picked
 * @param action        what a click on an option does, keyed by the option index
 * @param trailingLabel a caption drawn after the row, or {@link #NO_TRAILING_CAPTION} for none
 * @param segmentSizing how the segments size (uniform cells, or each snapped to its own label)
 * @param reselect      what a re-pick of the lit segment does (inert for an option pair, deselect for
 *                      a clearable selector)
 */
public record HorizontalRadioSpec(
    List<String> labels,
    int selectedIndex,
    ControlAction action,
    String trailingLabel,
    SegmentSizing segmentSizing,
    ReselectBehaviour reselect) implements RadioSpec {

    /** Copies the label list defensively, so a later edit to a caller's list cannot mutate the spec. */
    public HorizontalRadioSpec {
        labels = List.copyOf(labels);
    }

    /**
     * Builds the plain option row a host reaches for by default: even cells, no trailing caption, and
     * a re-pick of the lit segment inert, so the row always holds one option once one is picked.
     *
     * <p>The caption, the segment sizing, and the re-pick behaviour are three independent refinements
     * a host layers on with {@link #showsCaption}, {@link #sizesSegments}, and {@link
     * #handlesReselect} - so any combination of the three is reachable, rather than only the
     * combinations a fixed set of factories happened to name.
     *
     * @param labels        the option labels, left to right, in segment order
     * @param selectedIndex the lit option's index, or {@link #NO_SELECTION} when nothing is picked
     * @param action        what a click on an option does, keyed by the option index
     * @return the plain horizontal radio spec
     */
    public static HorizontalRadioSpec of(
            List<String> labels,
            int selectedIndex,
            ControlAction action) {

        return new HorizontalRadioSpec(
            labels,
            selectedIndex,
            action,
            NO_TRAILING_CAPTION,
            SegmentSizing.UNIFORM,
            ReselectBehaviour.INERT);
    }

    /**
     * Whether the row draws a trailing caption - any text past its segments. One rule read by both
     * the layout that reserves the caption's footprint and the renderer that draws it, so the two
     * cannot disagree on which rows carry one. Blank-but-present text reads as no caption, so a host
     * that assembles a caption from parts and comes up empty gets the uncaptioned row it should.
     *
     * @return true when the row carries a trailing caption
     */
    public boolean hasTrailingCaption() {
        return KmlibStrings.hasText(trailingLabel);
    }

    /**
     * Returns a copy of this radio captioned with {@code trailingLabel}, drawn after the row - for a
     * row whose segment labels alone do not say what the options choose between.
     *
     * @param trailingLabel the caption drawn after the row
     * @return an otherwise-identical radio carrying that caption
     */
    public HorizontalRadioSpec showsCaption(String trailingLabel) {
        return rebuildAsLaidOut(trailingLabel, segmentSizing, reselect);
    }

    /**
     * Returns a copy of this radio sizing its segments the given way - {@link SegmentSizing#SNAPPED}
     * for a ragged row whose labels differ enough in width that even cells would waste space.
     *
     * @param segmentSizing how the segments size (uniform cells, or each snapped to its own label)
     * @return an otherwise-identical radio sized that way
     */
    public HorizontalRadioSpec sizesSegments(SegmentSizing segmentSizing) {
        return rebuildAsLaidOut(trailingLabel, segmentSizing, reselect);
    }

    /**
     * Returns a copy of this radio handling a re-pick of its lit segment the given way - {@link
     * ReselectBehaviour#DESELECT} for a clearable selector, where a re-click of the active option
     * turns the row off rather than leaving it lit.
     *
     * @param reselect what a re-pick of the lit segment does
     * @return an otherwise-identical radio handling a re-pick that way
     */
    public HorizontalRadioSpec handlesReselect(ReselectBehaviour reselect) {
        return rebuildAsLaidOut(trailingLabel, segmentSizing, reselect);
    }

    // Rebuilds the row around how it is laid out and driven, carrying what it holds - its labels,
    // the lit segment, and the click action - over untouched. The three refinements share it rather
    // than each restating all six components, one of which would eventually be restated wrongly.
    private HorizontalRadioSpec rebuildAsLaidOut(
            String trailingLabel,
            SegmentSizing segmentSizing,
            ReselectBehaviour reselect) {

        return new HorizontalRadioSpec(
            labels,
            selectedIndex,
            action,
            trailingLabel,
            segmentSizing,
            reselect);
    }
}
