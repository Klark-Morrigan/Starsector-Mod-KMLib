package kmlib.starsector.ui.widgets;

import kmlib.text.KmlibStrings;

import java.awt.Color;

/**
 * One line of a {@link CursorTooltip}: a coloured label, and around it whatever else the line carries -
 * a leading crest, a marker trailing the label in its own colour, a right-aligned value, an indent for
 * its tier, a break opening a section above it. The content model a caller fills to say what a tooltip
 * shows, without saying how it is measured or drawn - the geometry ({@link CursorTooltip}) reads the
 * indent, the crest presence, and the three texts to size the box, and the renderer reads the colours
 * and the crest path to paint it.
 *
 * <p>Everything past the label is a refinement on {@link #createRow}, not a parameter of it: a caller
 * states what its row <em>has</em> and never spells out the absences. That matters because most rows
 * carry only some of these - a plain status line has no crest, value, or marker - and a positional
 * form would make each of them pass a placeholder for every part it does not use, growing worse with
 * every part the widget learns.
 *
 * <p>Header and member rows differ only in these values - a header sits at zero indent in a bright
 * colour, a nested member indents in a plainer one - so a stack of rows carries a two-tier hierarchy
 * as flat data and the draw path never branches on tier. A row with no crest leaves the path null and
 * still reserves the icon column, so its label stays aligned with the crested rows around it, unless
 * it {@linkplain #clearsCrestColumn steps out of that column} - which a title heading the whole box
 * does, since it names the box rather than sitting in its table.
 *
 * <p>The marker is the row's second voice: a short qualifier that has to read as part of the label's
 * sentence rather than as a column of its own, so it flows immediately after the label instead of
 * right-aligning like the value. That is what a caller reaches for when one span of a line - a status
 * word, a called-out flag - must be picked out in a different colour while the rest stays plain.
 *
 * <p>A {@linkplain #centred centred} row steps out of the table altogether: it is a standalone
 * line, so the layout ignores its indent, its crest column, and the value column, and lays its label
 * (with any marker) centred between the box's content edges. That is for a line stating something about
 * the whole box - a status, a "nothing here" - which reading as an entry of the list above or below it
 * would misname.
 *
 * @param indent               the label's inset from the box's left content edge, in UI units - zero
 *                             for a top-tier row, a positive step for a nested one
 * @param isOutsideCrestColumn whether the label lays flush at the box's content edge, outside the
 *                             crest column the box reserves for its crested rows
 * @param isLabelCentred       whether the label is a standalone line centred in the box's content
 *                             region, rather than an entry laid into its columns
 * @param hasSectionBreak      whether the row opens a section, taking breathing room above it so it
 *                             reads as starting a block rather than continuing the one above
 * @param crestSpritePath      the leading crest's {@code graphics} texture path, or null for no crest
 * @param text                 the row's label
 * @param textColor            the label's colour before the tooltip's opacity fade
 * @param marker               the qualifier drawn just after the label, or the empty string for none
 * @param markerColor          the marker's colour before the opacity fade
 * @param value                the right-aligned value, or the empty string for a row with none
 * @param valueColor           the value's colour before the opacity fade
 */
public record TooltipRow(
        float indent,
        boolean isOutsideCrestColumn,
        boolean isLabelCentred,
        boolean hasSectionBreak,
        String crestSpritePath,
        String text,
        Color textColor,
        String marker,
        Color markerColor,
        String value,
        Color valueColor) {

    // What a row that carries none of the optional parts holds: flush against the content edge, inside
    // the crest column, continuing the row above it, and with no crest, marker, or value. An absent
    // marker or value takes the label's own colour, so no colour is ever null even where nothing draws.
    private static final float NO_INDENT = 0f;
    private static final String NO_CREST = null;
    private static final String NO_MARKER = "";
    private static final String NO_VALUE = "";

    /**
     * Builds the plainest row there is: a label alone, flush with the box's left content edge. Every
     * other part is layered on with a refinement below, so what a caller writes is exactly what the
     * row carries.
     *
     * @param text      the row's label
     * @param textColor the label's colour before the tooltip's opacity fade
     * @return the bare row
     */
    public static TooltipRow createRow(String text, Color textColor) {
        return new TooltipRow(
                NO_INDENT,
                false,
                false,
                false,
                NO_CREST,
                text,
                textColor,
                NO_MARKER,
                textColor,
                NO_VALUE,
                textColor);
    }

    /**
     * Whether the row carries a marker worth drawing. One rule read by both the measurement that
     * charges the marker's span and the placement that anchors it, so the two cannot disagree about
     * which rows carry one. Blank-but-present text reads as no marker, so a caller assembling one
     * from parts and coming up empty gets the unmarked row it should rather than a reserved gap
     * before nothing.
     *
     * @return true when the row carries a marker
     */
    public boolean hasMarker() {
        return KmlibStrings.hasText(marker);
    }

    /**
     * Returns a copy of this row leading with {@code crestSpritePath} - the faction crest, icon, or
     * other small image drawn in the column before the label.
     *
     * @param crestSpritePath the crest's {@code graphics} texture path
     * @return an otherwise-identical row carrying that crest
     */
    public TooltipRow carriesCrest(String crestSpritePath) {
        return new TooltipRow(
                indent,
                isOutsideCrestColumn,
                isLabelCentred,
                hasSectionBreak,
                crestSpritePath,
                text,
                textColor,
                marker,
                markerColor,
                value,
                valueColor);
    }

    /**
     * Returns a copy of this row trailing {@code marker} after its label in {@code markerColor} - a
     * qualifier picked out in its own colour while the label stays plain.
     *
     * @param marker      the qualifier drawn just after the label
     * @param markerColor the marker's colour before the tooltip's opacity fade
     * @return an otherwise-identical row carrying that marker
     */
    public TooltipRow carriesMarker(String marker, Color markerColor) {
        return new TooltipRow(
                indent,
                isOutsideCrestColumn,
                isLabelCentred,
                hasSectionBreak,
                crestSpritePath,
                text,
                textColor,
                marker,
                markerColor,
                value,
                valueColor);
    }

    /**
     * Returns a copy of this row carrying {@code value} right-aligned to the box's content edge, in
     * {@code valueColor} - the number or short text a stack of rows reads as its value column.
     *
     * @param value      the right-aligned value
     * @param valueColor the value's colour before the tooltip's opacity fade
     * @return an otherwise-identical row carrying that value
     */
    public TooltipRow carriesValue(String value, Color valueColor) {
        return new TooltipRow(
                indent,
                isOutsideCrestColumn,
                isLabelCentred,
                hasSectionBreak,
                crestSpritePath,
                text,
                textColor,
                marker,
                markerColor,
                value,
                valueColor);
    }

    /**
     * Returns a copy of this row inset under the line above it, for a member or entry that reads as
     * belonging to that line. The step is the caller's, since how far a tier sets in is a decision of
     * the layout the rows are authored for, not of the row itself.
     *
     * @param indent the label's inset from the box's left content edge, in UI units
     * @return an otherwise-identical row at that indent
     */
    public TooltipRow indentsBy(float indent) {
        return new TooltipRow(
                indent,
                isOutsideCrestColumn,
                isLabelCentred,
                hasSectionBreak,
                crestSpritePath,
                text,
                textColor,
                marker,
                markerColor,
                value,
                valueColor);
    }

    /**
     * Returns a copy of this row opening a section: the layout takes breathing room above it, so it
     * reads as starting a block rather than continuing the rows before it. Nothing is taken above the
     * box's first row, which already has the box's own padding above it.
     *
     * @return an otherwise-identical row opening a section
     */
    public TooltipRow opensSection() {
        return new TooltipRow(
                indent,
                isOutsideCrestColumn,
                isLabelCentred,
                true,
                crestSpritePath,
                text,
                textColor,
                marker,
                markerColor,
                value,
                valueColor);
    }

    /**
     * Returns a copy of this row laying its label flush at the box's left content edge, outside the
     * crest column - for a title that names the whole box rather than an entry aligned inside its
     * table of crested rows.
     *
     * @return an otherwise-identical row clear of the crest column
     */
    public TooltipRow clearsCrestColumn() {
        return new TooltipRow(
                indent,
                true,
                isLabelCentred,
                hasSectionBreak,
                crestSpritePath,
                text,
                textColor,
                marker,
                markerColor,
                value,
                valueColor);
    }

    /**
     * Returns a copy of this row centred in the box's content region as a standalone line: the layout
     * drops its indent, its crest column, and the value column, and centres the label with any marker
     * between the content edges. For a line that speaks for the whole box rather than sitting as an
     * entry in its table - a row so centred carries no crest and no value, since both are columns of
     * that table.
     *
     * @return an otherwise-identical row centred as a standalone line
     */
    public TooltipRow centred() {
        return new TooltipRow(
                indent,
                isOutsideCrestColumn,
                true,
                hasSectionBreak,
                crestSpritePath,
                text,
                textColor,
                marker,
                markerColor,
                value,
                valueColor);
    }
}
