package kmlib.starsector.ui.widgets.tooltip;

import kmlib.starsector.ui.text.LabelRun;
import kmlib.starsector.ui.text.LabelRuns;
import kmlib.starsector.ui.text.TextSpan;
import kmlib.starsector.ui.widgets.LabelledRow;
import kmlib.starsector.ui.widgets.RowSlot;

import java.util.List;
import java.util.Objects;

/**
 * One line of a {@link CursorTooltip}: what it says, and which of the two ways the box lays it. A
 * {@link TableRow} sits in the box's columns - a leading crest, a label, a right-aligned value, at a
 * tier of its own - while a {@link CentredRow} speaks for the whole box and is set across its content
 * region on its own. The content model a caller fills to say what a tooltip shows, without saying how
 * it is measured or drawn: the geometry ({@link CursorTooltip}) reads the line and its columns to size
 * the box, and the renderer paints them.
 *
 * <p>Two types rather than one type carrying a placement, because a centred line has no columns to fill.
 * It is sized to its label alone, so a crest or a value on it would be drawn outside the box measured to
 * hold it - and a row that holds no slots is the only way to state that. Spelled as one record with a
 * placement, the pairing is expressible and can be refused only once the row is built, which turns a
 * fault the compiler could decline into one a player finds by hovering.
 *
 * <p>What the two share is what every line of a box has: the kind of line it is, and a label of one or
 * more runs read as one sentence. A stack of rows mixes them freely, so a box is a title over a table
 * without either kind knowing about the other. How far a line stands from its neighbours is not among
 * them: a line belongs to a {@link TooltipSection block}, and the parting between two blocks is a fact
 * about the two of them meeting rather than something either line carries.
 *
 * <p>Everything past the label is a refinement, never a parameter: a caller states what its line
 * <em>has</em> and never spells out the absences. That matters because most lines carry only some of
 * these - a plain status line has no crest, value, or second run - and a positional form would make each
 * of them pass a placeholder for every part it does not use, growing worse with every part the widget
 * learns.
 *
 * <p>The one thing a line says about its own look is which {@link TooltipLineStyle kind of line} it is,
 * and even that is a statement about content: a heading is a heading whichever face the host draws
 * headings in. The face, size, and casing that kind resolves to live on the host's {@link TooltipStyle},
 * so a line is authored by whatever knows the subject matter and never by whatever knows the typography.
 * A span carries no face for the same reason, so a whole line is spoken in one voice.
 */
public sealed interface TooltipRow {

    /**
     * Builds the plainest line there is: a one-run label laid as an entry in the box's columns, at no
     * indent and with neither flank filled. Every other part is layered on with a refinement, so what a
     * caller writes is exactly what the row carries.
     *
     * @param labelRun the label's one run - ordinarily a {@link TextSpan}, the text and the colour it
     *                 draws in before the tooltip's opacity fade
     * @return the bare table row
     */
    static TableRow createRow(LabelRun labelRun) {
        return new TableRow(
            TableRow.DEFAULT_LINE_STYLE,
            TableRow.DEFAULT_LABEL_PLACEMENT,
            TableRow.NO_INDENT,
            TableRow.NO_SUBORDINATION,
            LabelledRow.createRow(labelRun));
    }

    /**
     * Builds a line centred in the box's content region: a one-run label set between the content edges,
     * clear of every column the rows around it align to. For a title that names the whole box, or a lone
     * statement that speaks for it, rather than an entry in its table.
     *
     * <p>Its own factory rather than a refinement of a table row, because centring is not something done
     * to a row that already has columns - it is what a line without them is. A caller reaches for one or
     * the other knowing which it is authoring, and no chain of refinements can arrive at a centred line
     * carrying a crest.
     *
     * @param labelRun the label's one run - ordinarily a {@link TextSpan}, the text and the colour it
     *                 draws in before the tooltip's opacity fade
     * @return the bare centred row
     */
    static CentredRow createCentredRow(LabelRun labelRun) {
        return new CentredRow(
            TableRow.DEFAULT_LINE_STYLE,
            List.of(labelRun));
    }

    /**
     * The kind of line this is, which the host tooltip turns into a look.
     *
     * @return the kind of line
     */
    TooltipLineStyle lineStyle();

    /**
     * The label's runs in reading order - text in the colour it draws in before any opacity fade, or an
     * image squared off the line; never empty. What both kinds of line are measured and drawn from,
     * whatever else flanks them.
     *
     * @return the label's runs
     */
    List<LabelRun> labelRuns();

    /**
     * Returns a copy of this line whose label runs on into {@code labelRun} - the next stretch of the
     * same sentence, a stretch of text picked out in its own colour or an image set among the words,
     * while what came before it stays as it was. Applied twice, a line reads in three runs at no extra
     * cost to the model.
     *
     * @param labelRun the run continuing the label
     * @return an otherwise-identical line whose label carries that run last
     */
    TooltipRow continuesWith(LabelRun labelRun);

    /**
     * Returns a copy of this line reading as {@code lineStyle} - a heading that names the box rather
     * than a line of its body, say. What that kind draws in is the host tooltip's decision, so a caller
     * marking a line as a heading is describing it rather than choosing a face for it.
     *
     * @param lineStyle the kind of line this is
     * @return an otherwise-identical line reading as that kind of line
     */
    TooltipRow readsAs(TooltipLineStyle lineStyle);

    /**
     * A line laid in the box's columns: the {@link LabelledRow content} it shows - a leading crest, a
     * label, a right-aligned value - plus where its label starts across the box and how far its tier
     * sets in.
     *
     * <p>The content is embedded rather than spread back out over this record, because a measurement or
     * a paint written against a labelled row then serves this line and any other widget's alike. What
     * stays here is what a tooltip entry has and a row in general does not: an indent is a tier within a
     * stack that has tiers, and a crest gutter is a column within a box that reserves one.
     *
     * <p>Header and member lines differ only in these values - a header sits at zero indent in a bright
     * colour, a nested member indents in a plainer one - so a stack of rows carries a two-tier hierarchy
     * as flat data and the draw path never branches on tier. A line with no crest leaves its leading slot
     * unfilled and still reserves the icon column, so its label stays aligned with the crested lines
     * around it, unless it {@linkplain #clearsCrestColumn steps out of that column} - which a title
     * heading the whole box does, since it names the box rather than sitting in its table.
     *
     * <p>Where the label starts across the box is one decision, not a set of flags: its {@link
     * TooltipLabelPlacement} puts it past the crest gutter or at the content edge. The decision is stated
     * of the <em>label</em>, because the label is what a row is - the crest leads it in and the value
     * trails it, and neither of them is the thing being placed. The value column is not part of that
     * choice: both placements keep it clear to the right, so a title carries a trailing value exactly as
     * an ordinary line does.
     *
     * <p>Where a line sits and how far it stands under the box's own voice are two facts, not one, and a
     * stack of rows needs both. A line may step in without being subordinated at all - the members of a
     * group are inset beneath it while remaining the same kind of statement it is - so the indent and the
     * demotion move independently and neither can stand for the other. Nor is subordination tree depth:
     * those members are children of the line above them without being subordinate to it.
     *
     * <p>It says nothing about <em>why</em> a line stands under another. A stack of rows may be a
     * breakdown, a hierarchy, a list with sub-items, or an aside beneath a finding; all this carries is
     * that the line is one further step under the voice the box speaks in, which is the only part every
     * one of those has in common - and the only part a widget that knows none of them could name.
     *
     * <p>It is likewise a fact about the content rather than a request about the look: the host decides
     * what a step under looks like ({@link TooltipStyle#shrunkPerLevel}), exactly as it decides what a
     * heading looks like. A row naming its own size would be the row choosing its typography, which is
     * the one thing this model keeps it out of.
     *
     * @param lineStyle          the kind of line this is, which the host tooltip turns into a look
     * @param labelPlacement     where the label starts across the box - past the crest gutter or at the
     *                           content edge
     * @param indent             the label's inset from the box's left content edge, in UI units - zero
     *                           for a top-tier line, a positive step for a nested one
     * @param subordinationLevel how many steps this line stands under the box's own voice - zero for a
     *                           line speaking in it; never negative
     * @param labelledRow        what the line carries: its crest, its label's runs, and its value
     */
    record TableRow(
        TooltipLineStyle lineStyle,
        TooltipLabelPlacement labelPlacement,
        float indent,
        int subordinationLevel,
        LabelledRow labelledRow) implements TooltipRow {

        // What a line that carries none of the optional parts holds: a line of the body, its label
        // starting where the crested lines' labels start, and with no crest, second run, or value. Body
        // text is the default kind because most lines of a tooltip are its body, and crest-aligned is
        // the default placement because that is what an ordinary content line is - a heading and a title
        // are each the exception a caller states.
        private static final TooltipLineStyle DEFAULT_LINE_STYLE = TooltipLineStyle.PARAGRAPH;
        private static final TooltipLabelPlacement DEFAULT_LABEL_PLACEMENT =
            TooltipLabelPlacement.ALIGNED_WITH_CRESTS;
        private static final float NO_INDENT = 0f;

        /**
         * A line speaking in the box's own voice - what a row is until a caller puts it under
         * something, and the floor every deeper level is measured from.
         *
         * <p>Offered to the package rather than kept private because the same zero is the answer in
         * three places: the floor a row is held to here, the level at which a host resolves no shrink
         * at all ({@link TooltipStyle}), and what a line holding no table position of its own reads at
         * ({@link CursorTooltip}). Three copies would agree until one of them was edited.
         */
        static final int NO_SUBORDINATION = 0;

        /**
         * Rejects a null content at construction, where the caller that built the line is still on the
         * stack, and floors the subordination level at the box's own voice: a negative one would have the host
         * resolve a size larger than the body's for a line that is meant to be quieter than it. What the
         * content itself must hold - a label of at least one run, a slot rather than a null on each
         * flank - is {@link LabelledRow}'s own rule, checked where that content is built.
         */
        public TableRow {
            Objects.requireNonNull(labelledRow, "labelledRow");
            subordinationLevel = Math.max(NO_SUBORDINATION, subordinationLevel);
        }

        @Override
        public List<LabelRun> labelRuns() {
            return labelledRow.labelRuns();
        }

        /**
         * Returns a copy of this line leading with {@code crestSpritePath} - the faction crest, icon, or
         * other small image drawn in the column before the label. A null path leaves the line
         * crest-less, so a caller resolving a crest that a faction may simply not have hands the result
         * straight over rather than branching around this.
         *
         * @param crestSpritePath the crest's {@code graphics} texture path, or null for no crest
         * @return an otherwise-identical line carrying that crest
         */
        public TableRow carriesCrest(String crestSpritePath) {
            var crestRowSlot = crestSpritePath == null
                ? RowSlot.EMPTY
                : new RowSlot.Image(crestSpritePath);

            return rebuildWithContent(labelledRow.leadsWith(crestRowSlot));
        }

        /**
         * Returns a copy of this line carrying {@code valueTextSpan} right-aligned to the box's content
         * edge - the number or short text a stack of rows reads as its value column. A span that comes
         * out blank fills the slot with a run that draws nothing, rather than emptying it: a caller
         * assembling a value from parts said its line has one, and a line that never says so leaves the
         * slot unfilled instead.
         *
         * <p>Takes the same span a label run does, since a caller reaching for either is holding one run
         * of text and the colour it draws in - what differs is which rule places it, the value being
         * charged a column of its own where a run flows on from the one before it.
         *
         * @param valueTextSpan the right-aligned value and the colour it draws in
         * @return an otherwise-identical line carrying that value
         */
        public TableRow carriesValue(TextSpan valueTextSpan) {
            return rebuildWithContent(labelledRow.trailsWith(new RowSlot.Text(valueTextSpan)));
        }

        /**
         * Returns a copy of this line carrying {@code valueTextSpans} right-aligned to the box's content
         * edge as one column - the runs reading left to right inside it, each in its own colour, for a
         * value that states two things at once: a finding and the working behind it, say.
         *
         * <p>Its own refinement rather than a list-taking {@link #carriesValue}, because a value of one
         * run is what almost every line carries and would otherwise be composed as a list of one at every
         * call site. Both fill the same column, so a box mixing the two still aligns its values.
         *
         * @param valueTextSpans the value's runs in reading order, each with the colour it draws in;
         *                       never empty
         * @return an otherwise-identical line carrying that value
         */
        public TableRow carriesValueRuns(List<TextSpan> valueTextSpans) {
            return rebuildWithContent(
                labelledRow.trailsWith(new RowSlot.TextRuns(valueTextSpans)));
        }

        @Override
        public TableRow continuesWith(LabelRun labelRun) {
            return rebuildWithContent(labelledRow.continuesWith(labelRun));
        }

        /**
         * Returns a copy of this line inset under the line above it, for a member or entry that reads as
         * belonging to that line. The step is the caller's, since how far a tier sets in is a decision of
         * the layout the rows are authored for, not of the row itself.
         *
         * @param indent the label's inset from the box's left content edge, in UI units
         * @return an otherwise-identical line at that indent
         */
        public TableRow indentsBy(float indent) {
            return rebuild(lineStyle, labelPlacement, indent, subordinationLevel, labelledRow);
        }

        /**
         * Returns a copy of this line laying its label flush at the box's left content edge, outside the
         * crest column - for a title that names the whole box rather than an entry aligned inside its
         * table of crested lines. It stays an entry in the value column, like the lines it heads.
         *
         * @return an otherwise-identical line clear of the crest column
         */
        public TableRow clearsCrestColumn() {
            return rebuild(
                lineStyle,
                TooltipLabelPlacement.AT_CONTENT_EDGE,
                indent,
                subordinationLevel,
                labelledRow);
        }

        /**
         * Returns a copy of this line standing {@code subordinationLevel} steps under the box's own
         * voice, which the host draws progressively quieter.
         *
         * <p>Stated apart from the indent because the two do not move together: a group's members are
         * inset beneath it while remaining the same kind of statement it is, and would be demoted along
         * with a genuinely subordinate line if one number stood for both.
         *
         * @param subordinationLevel how many steps the line stands under the box's own voice; floored at
         *                           that voice
         * @return an otherwise-identical line at that subordination level
         */
        public TableRow subordinatedAt(int subordinationLevel) {
            return rebuild(lineStyle, labelPlacement, indent, subordinationLevel, labelledRow);
        }

        @Override
        public TableRow readsAs(TooltipLineStyle lineStyle) {
            return rebuild(lineStyle, labelPlacement, indent, subordinationLevel, labelledRow);
        }

        // Rebuilds the line around new content, carrying every row-level fact over untouched. The three
        // refinements that change only what the line carries share it rather than each restating the
        // facts they leave alone - one of which would eventually be restated wrongly.
        private TableRow rebuildWithContent(LabelledRow labelledRow) {
            return rebuild(lineStyle, labelPlacement, indent, subordinationLevel, labelledRow);
        }

        // The one place a refined line is constructed. Every refinement passes the whole of what the new
        // line is, so a part added to the record is added here and at the factory rather than at each
        // refinement that merely carries it over.
        private static TableRow rebuild(
                TooltipLineStyle lineStyle,
                TooltipLabelPlacement labelPlacement,
                float indent,
                int subordinationLevel,
                LabelledRow labelledRow) {

            return new TableRow(lineStyle, labelPlacement, indent, subordinationLevel, labelledRow);
        }
    }

    /**
     * A line centred in the box's content region as a standalone span: its label's runs centred together
     * between the content edges, clear of the crest gutter and the value column the lines around it align
     * to. For a line that speaks for the whole box rather than sitting as an entry in its table.
     *
     * <p>It holds its label and nothing else. A crest and a value are columns of the table this line has
     * left, and an indent is a tier within it, so none of the three has a meaning here - and a line that
     * cannot hold them is what keeps the layout from having to ignore them. The box is sized to the
     * centred span alone, which is exactly why: anything charged to a column would be drawn past an edge
     * the box was never widened for.
     *
     * <p>A crest is not lost by leaving the table, only re-read: set as an {@link
     * kmlib.starsector.ui.text.ImageSpan} run it travels inside the label and centres with the words
     * rather than sitting in a gutter to their left, which is what a line speaking for the whole box
     * wants. Being a run, it is charged to the span the box was sized to, so it cannot land past an edge.
     *
     * @param lineStyle the kind of line this is, which the host tooltip turns into a look
     * @param labelRuns the label's runs in reading order - text in the colour it draws in before the
     *                  tooltip's opacity fade, or an image squared off the line; never empty
     */
    record CentredRow(
        TooltipLineStyle lineStyle,
        List<LabelRun> labelRuns) implements TooltipRow {

        /**
         * Copies the label's runs and rejects an empty or null-bearing label at construction, through
         * the same rule a labelled row's own label is held to - a line with no label is not a line,
         * whichever way the box lays it.
         */
        public CentredRow {
            labelRuns = LabelRuns.copyRuns(labelRuns);
        }

        @Override
        public CentredRow continuesWith(LabelRun labelRun) {
            return new CentredRow(
                lineStyle,
                LabelRuns.appendRun(labelRuns, labelRun));
        }

        @Override
        public CentredRow readsAs(TooltipLineStyle lineStyle) {
            return new CentredRow(lineStyle, labelRuns);
        }
    }
}
