package kmlib.starsector.ui.widgets;

import kmlib.starsector.ui.text.TextSpan;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One line of a {@link CursorTooltip}: a label read as one sentence, and around it whatever else the
 * line carries - a leading crest, a right-aligned value, an indent for its tier, a break opening a
 * section above it. The content model a caller fills to say what a tooltip shows, without saying how it
 * is measured or drawn - the geometry ({@link CursorTooltip}) reads the indent, the crest presence, the
 * label's runs, and the value to size the box, and the renderer paints them.
 *
 * <p>A label is a list of {@link TextSpan} runs rather than one span, because a line often has to pick
 * one stretch of itself out in another colour - a status word, a called-out flag - while the rest stays
 * plain. Runs are laid inline: each starts where the one before it measured out, so the whole label
 * reads as one sentence rather than as columns the rows around it align to. A one-colour label is the
 * list of one that {@link #createRow} builds, so a caller that never needs a second colour never sees
 * the list.
 *
 * <p>Each run is a {@link TextSpan} rather than a text field beside a colour field, because a run and
 * the colour it draws in are one thing: split across two components they can be reached for separately,
 * refined separately, and eventually disagree, and neither half can be handed anywhere on its own. A
 * row with nothing to say in its value still holds a span - a blank one - so measuring, styling, and
 * drawing take the same path whether or not the run is filled.
 *
 * <p>Everything past the label is a refinement on {@link #createRow}, not a parameter of it: a caller
 * states what its row <em>has</em> and never spells out the absences. That matters because most rows
 * carry only some of these - a plain status line has no crest, value, or second run - and a positional
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
 * @param crestSpritePath the leading crest's {@code graphics} texture path, or null for no crest
 * @param labelTextSpans  the label's runs in reading order, each in the colour it draws in before the
 *                        tooltip's opacity fade; never empty
 * @param valueTextSpan   the right-aligned value, blank for a row carrying none
 */
public record TooltipRow(
        TooltipLineStyle lineStyle,
        TooltipLabelPlacement labelPlacement,
        float indent,
        boolean hasSectionBreak,
        String crestSpritePath,
        List<TextSpan> labelTextSpans,
        TextSpan valueTextSpan) {

    // What a row that carries none of the optional parts holds: a line of the body, its label starting
    // where the crested rows' labels start, continuing the row above it, and with no crest, second run,
    // or value. Body text is the default kind because most lines of a tooltip are its body, and
    // crest-aligned is the default placement because that is what an ordinary content row is - a heading
    // and a title are each the exception a caller states.
    private static final TooltipLineStyle DEFAULT_LINE_STYLE = TooltipLineStyle.PARAGRAPH;
    private static final TooltipLabelPlacement DEFAULT_LABEL_PLACEMENT =
            TooltipLabelPlacement.ALIGNED_WITH_CRESTS;
    private static final float NO_INDENT = 0f;
    private static final String NO_CREST = null;

    /**
     * Copies the label's runs and rejects an empty or null-bearing label at construction, where the
     * caller that built the row is still on the stack. A row is a label with things around it, so a
     * label with no runs at all is not a row - and a null run, or a null value span, otherwise surfaces
     * inside a measurement or a draw call, well past the point that could say which row was meant. The
     * value is always present as a span, an unfilled one being blank rather than absent. The crest path
     * is the one part that is genuinely optional, and it keeps null as its spelling of absence.
     */
    public TooltipRow {
        Objects.requireNonNull(labelTextSpans, "labelTextSpans");
        labelTextSpans = List.copyOf(labelTextSpans);
        if (labelTextSpans.isEmpty()) {
            throw new IllegalArgumentException("labelTextSpans carries at least one run");
        }
        Objects.requireNonNull(valueTextSpan, "valueTextSpan");
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
        // The unfilled value takes the label's own colour rather than none: a blank span still has to
        // answer what it would draw in, so nothing downstream needs a branch for the empty case.
        return new TooltipRow(
                DEFAULT_LINE_STYLE,
                DEFAULT_LABEL_PLACEMENT,
                NO_INDENT,
                false,
                NO_CREST,
                List.of(new TextSpan(labelText, labelColour)),
                TextSpan.createBlank(labelColour));
    }

    /**
     * Whether the row leads with a crest. One rule read by the column the crests share, the square each
     * one hangs in, and the draw that paints it, so none of the three can disagree about which rows have
     * one.
     *
     * @return true when the row leads with a crest
     */
    public boolean hasCrest() {
        return crestSpritePath != null;
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
                lineStyle,
                labelPlacement,
                indent,
                hasSectionBreak,
                crestSpritePath,
                labelTextSpans,
                valueTextSpan);
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
        var continuedTextSpans = new ArrayList<>(labelTextSpans);
        continuedTextSpans.add(new TextSpan(runText, runColour));

        return new TooltipRow(
                lineStyle,
                labelPlacement,
                indent,
                hasSectionBreak,
                crestSpritePath,
                continuedTextSpans,
                valueTextSpan);
    }

    /**
     * Returns a copy of this row carrying {@code valueText} right-aligned to the box's content edge, in
     * {@code valueColour} - the number or short text a stack of rows reads as its value column.
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
                crestSpritePath,
                labelTextSpans,
                new TextSpan(valueText, valueColour));
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
                crestSpritePath,
                labelTextSpans,
                valueTextSpan);
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
                crestSpritePath,
                labelTextSpans,
                valueTextSpan);
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
                crestSpritePath,
                labelTextSpans,
                valueTextSpan);
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
                crestSpritePath,
                labelTextSpans,
                valueTextSpan);
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
                crestSpritePath,
                labelTextSpans,
                valueTextSpan);
    }
}
