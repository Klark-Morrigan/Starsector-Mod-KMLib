package kmlib.starsector.ui.widgets;

import kmlib.starsector.ui.text.TextSpan;

import java.awt.Color;
import java.util.Objects;

/**
 * One line of a {@link CursorTooltip}: the {@link LabelledRow content} it shows - a leading crest, a
 * label read as one sentence, a right-aligned value - and the four facts that are the tooltip's own
 * rather than any row's: the kind of line it is, where its label starts across the box, its indent for
 * its tier, and whether it opens a section. The content model a caller fills to say what a tooltip
 * shows, without saying how it is measured or drawn - the geometry ({@link CursorTooltip}) reads the
 * indent, the crest, the label's runs, and the value to size the box, and the renderer paints them.
 *
 * <p>The content is embedded rather than spread back out over this record, because a measurement or a
 * paint written against a labelled row then serves this row and any other widget's alike. What stays
 * here is what a tooltip line has and a row in general does not: a heading is a heading only within a
 * box that draws headings, and an indent is a tier within a stack that has tiers.
 *
 * <p>Everything past the label is a refinement on {@link #createRow}, not a parameter of it: a caller
 * states what its row <em>has</em> and never spells out the absences. That matters because most rows
 * carry only some of these - a plain status line has no crest, value, or second run - and a positional
 * form would make each of them pass a placeholder for every part it does not use, growing worse with
 * every part the widget learns.
 *
 * <p>Header and member rows differ only in these values - a header sits at zero indent in a bright
 * colour, a nested member indents in a plainer one - so a stack of rows carries a two-tier hierarchy
 * as flat data and the draw path never branches on tier. A row with no crest leaves its leading slot
 * unfilled and still reserves the icon column, so its label stays aligned with the crested rows around
 * it, unless it {@linkplain #clearsCrestColumn steps out of that column} - which a title heading the
 * whole box does, since it names the box rather than sitting in its table.
 *
 * <p>Where the label starts across the box is one decision, not a set of flags: its {@link
 * TooltipLabelPlacement} puts it past the crest gutter, at the content edge, or centred as a standalone
 * span. The decision is stated of the <em>label</em>, because the label is what a row is - the crest
 * leads it in and the value trails it, and neither of them is the thing being placed. The value column
 * is not part of that choice: every placement but the centred one keeps it clear to the right, so a
 * title carries a trailing value exactly as an ordinary row does.
 *
 * <p>The one thing a row says about its own look is which {@link TooltipLineStyle kind of line} it is,
 * and even that is a statement about content: a heading is a heading whichever face the host draws
 * headings in. The face, size, and casing that kind resolves to live on the host's {@link TooltipStyle},
 * so a row is authored by whatever knows the subject matter and never by whatever knows the typography.
 * A span carries no face for the same reason, so a whole row is spoken in one voice.
 *
 * @param lineStyle       the kind of line this is, which the host tooltip turns into a look
 * @param labelPlacement  where the label starts across the box - past the crest gutter, at the content
 *                        edge, or centred on its own
 * @param indent          the label's inset from the box's left content edge, in UI units - zero for a
 *                        top-tier row, a positive step for a nested one
 * @param hasSectionBreak whether the row opens a section, taking breathing room above it so it reads
 *                        as starting a block rather than continuing the one above
 * @param labelledRow     what the line carries: its crest, its label's runs, and its value
 */
public record TooltipRow(
        TooltipLineStyle lineStyle,
        TooltipLabelPlacement labelPlacement,
        float indent,
        boolean hasSectionBreak,
        LabelledRow labelledRow) {

    // What a row that carries none of the optional parts holds: a line of the body, its label starting
    // where the crested rows' labels start, continuing the row above it, and with no crest, second run,
    // or value. Body text is the default kind because most lines of a tooltip are its body, and
    // crest-aligned is the default placement because that is what an ordinary content row is - a heading
    // and a title are each the exception a caller states.
    private static final TooltipLineStyle DEFAULT_LINE_STYLE = TooltipLineStyle.PARAGRAPH;
    private static final TooltipLabelPlacement DEFAULT_LABEL_PLACEMENT =
            TooltipLabelPlacement.ALIGNED_WITH_CRESTS;
    private static final float NO_INDENT = 0f;

    /**
     * Rejects a null content at construction, where the caller that built the row is still on the
     * stack. What the content itself must hold - a label of at least one run, a slot rather than a null
     * on each flank - is {@link LabelledRow}'s own rule, checked where that content is built.
     */
    public TooltipRow {
        Objects.requireNonNull(labelledRow, "labelledRow");
    }

    /**
     * Builds the plainest row there is: a one-run label, laid as an entry in the box's columns at no
     * indent. Every other part is layered on with a refinement below, so what a caller writes is
     * exactly what the row carries.
     *
     * @param labelText   the row's label
     * @param labelColour the label's colour before the tooltip's opacity fade
     * @return the bare row
     */
    public static TooltipRow createRow(String labelText, Color labelColour) {
        return new TooltipRow(
                DEFAULT_LINE_STYLE,
                DEFAULT_LABEL_PLACEMENT,
                NO_INDENT,
                false,
                LabelledRow.createRow(labelText, labelColour));
    }

    /**
     * Whether the row leads with a crest. One rule read by the column the crests share, the square each
     * one hangs in, and the draw that paints it, so none of the three can disagree about which rows have
     * one.
     *
     * @return true when the row leads with a crest
     */
    public boolean hasCrest() {
        return labelledRow.leadingRowSlot() instanceof RowSlot.Image;
    }

    /**
     * Returns a copy of this row leading with {@code crestSpritePath} - the faction crest, icon, or
     * other small image drawn in the column before the label. A null path leaves the row crest-less, so
     * a caller resolving a crest that a faction may simply not have hands the result straight over
     * rather than branching around this.
     *
     * @param crestSpritePath the crest's {@code graphics} texture path, or null for no crest
     * @return an otherwise-identical row carrying that crest
     */
    public TooltipRow carriesCrest(String crestSpritePath) {
        var crestRowSlot = crestSpritePath == null
                ? RowSlot.EMPTY
                : new RowSlot.Image(crestSpritePath);

        return new TooltipRow(
                lineStyle,
                labelPlacement,
                indent,
                hasSectionBreak,
                labelledRow.leadsWith(crestRowSlot));
    }

    /**
     * Returns a copy of this row whose label runs on into {@code runText} in {@code runColour} - the
     * next stretch of the same sentence, picked out in its own colour while what came before it stays
     * as it was. Applied twice, a line reads in three colours at no extra cost to the model.
     *
     * @param runText   the text continuing the label
     * @param runColour the run's colour before the tooltip's opacity fade
     * @return an otherwise-identical row whose label carries that run last
     */
    public TooltipRow continuesWith(String runText, Color runColour) {
        return new TooltipRow(
                lineStyle,
                labelPlacement,
                indent,
                hasSectionBreak,
                labelledRow.continuesWith(runText, runColour));
    }

    /**
     * Returns a copy of this row carrying {@code valueText} right-aligned to the box's content edge, in
     * {@code valueColour} - the number or short text a stack of rows reads as its value column. Text
     * that comes out blank fills the slot with a run that draws nothing, rather than emptying it: a
     * caller assembling a value from parts said its row has one, and a row that never says so leaves
     * the slot unfilled instead.
     *
     * @param valueText   the right-aligned value
     * @param valueColour the value's colour before the tooltip's opacity fade
     * @return an otherwise-identical row carrying that value
     */
    public TooltipRow carriesValue(String valueText, Color valueColour) {
        return new TooltipRow(
                lineStyle,
                labelPlacement,
                indent,
                hasSectionBreak,
                labelledRow.trailsWith(new RowSlot.Text(new TextSpan(valueText, valueColour))));
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
                lineStyle,
                labelPlacement,
                indent,
                hasSectionBreak,
                labelledRow);
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
                lineStyle,
                labelPlacement,
                indent,
                true,
                labelledRow);
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
                lineStyle,
                TooltipLabelPlacement.AT_CONTENT_EDGE,
                indent,
                hasSectionBreak,
                labelledRow);
    }

    /**
     * Returns a copy of this row centred in the box's content region as a standalone line: the layout
     * drops its indent, its crest column, and the value column, and centres the label's runs together
     * between the content edges. For a line that speaks for the whole box rather than sitting as an
     * entry in its table - a row so centred carries no crest and no value, since both are columns of
     * that table.
     *
     * @return an otherwise-identical row centred as a standalone line
     */
    public TooltipRow centred() {
        return new TooltipRow(
                lineStyle,
                TooltipLabelPlacement.CENTRED,
                indent,
                hasSectionBreak,
                labelledRow);
    }

    /**
     * Returns a copy of this row reading as {@code lineStyle} - a heading that names the box rather than
     * a line of its body, say. What that kind draws in is the host tooltip's decision, so a caller
     * marking a row as a heading is describing the row rather than choosing a face for it.
     *
     * @param lineStyle the kind of line this row is
     * @return an otherwise-identical row reading as that kind of line
     */
    public TooltipRow readsAs(TooltipLineStyle lineStyle) {
        return new TooltipRow(
                lineStyle,
                labelPlacement,
                indent,
                hasSectionBreak,
                labelledRow);
    }
}
