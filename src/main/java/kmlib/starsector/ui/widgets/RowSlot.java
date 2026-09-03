package kmlib.starsector.ui.widgets;

import kmlib.starsector.ui.text.LabelRun;
import kmlib.starsector.ui.text.LabelRuns;
import kmlib.starsector.ui.text.LabelRuns.LabelRunOffsets;
import kmlib.starsector.ui.text.StyledSpanMeasurer;
import kmlib.starsector.ui.text.TextSpan;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

/**
 * What a row carries beside its label - a small image, a run of text, a tick, a direction triangle, or
 * nothing. The two flanking columns of a row hold none of these exclusively: the trailing one holds a
 * value in one stack of rows and a sort marker in the next, and the leading one a crest here and a tick
 * box there. Modelled as one field per kind, a row grows a field for every kind that ever arrives and
 * carries all but one of them unset on every row that is built.
 *
 * <p>A sealed set instead: a new kind of flanking element is a new member, and every layout that
 * branches on the kind stops compiling until it handles the new one - which is the whole difference
 * between a model that can absorb a new kind and one that quietly draws nothing for it. That promise is
 * {@link #paintSlot}'s to keep and not the sealing's: the mod targets Java 17, whose {@code switch}
 * is not exhaustive over a sealed set, so a painter branching by {@code instanceof} compiles perfectly
 * well with a kind unhandled and silently leaves that column empty.
 *
 * <p>Every member answers its own width for a line of a given height, because that width is the one
 * fact a stack of rows needs to reserve a column all of them share, and the arithmetic behind it
 * differs per kind: an image squares off the line, a triangle takes a fraction of it, and a run of text
 * is as wide as its glyphs measure. What a slot does <em>not</em> answer is the padding around it or the
 * gap to the label - those belong to whatever lays out the columns, since they are facts about a stack
 * of rows rather than about anything a row holds.
 *
 * <p>Slots are visual only. A tick is clickable where an image is not, but which cell was hit and what
 * a click does stay with {@link kmlib.starsector.ui.controls.ControlSpec} and the container that owns
 * the row's footprint - the same split that keeps {@code ControlAction} off the control's own content.
 */
public sealed interface RowSlot {

    /**
     * What a slot with nothing to draw is charged. Named here rather than per member so that an
     * unfilled slot and a run that came out blank cannot be charged two different nothings, and so a
     * container comparing a measured width against it reads the intent rather than a bare zero.
     */
    float NO_WIDTH = 0f;

    /**
     * The slot a row leaves unfilled. Shared rather than built per row: an empty slot holds no state
     * that could tell two of them apart, and one spelling of "nothing here" is what stops a null slot
     * and an empty one both meaning the same thing in different places.
     */
    RowSlot EMPTY = new Empty();

    /**
     * How wide this slot draws on a line {@code lineHeight} tall, before any padding or gap the columns
     * around it add. Sized off the line height rather than a fixed step so that a stack of rows drawn at
     * one size shows even slots, and a stack drawn larger scales with its text.
     *
     * @param lineHeight   the height of the line the slot sits on, in UI units
     * @param spanMeasurer the width measurement already bound to the look that line draws in, spent
     *                     only by a slot whose width depends on its glyphs
     * @return the slot's width, in UI units, and nothing at all for a slot with nothing to draw
     */
    float computeWidth(float lineHeight, StyledSpanMeasurer spanMeasurer);

    /**
     * Whether the slot has anything to draw. One rule for both what reserves the slot's column and what
     * paints into it, so the two cannot disagree over which rows fill a flank - the reserved gutter and
     * the drawn content are the same decision read twice.
     *
     * <p>Asked of the slot rather than measured, because a caller deciding where a label starts holds no
     * face to measure a run against and should not have to resolve one to learn that a slot is empty.
     *
     * @return true when the slot draws something
     */
    boolean isFilled();

    /**
     * Hands this slot to the method {@code rowSlotPainter} wrote for its kind, so a surface paints a
     * flanking column by saying what each kind looks like rather than by testing what the slot happens
     * to be.
     *
     * <p>How the seal's promise is actually kept - see {@link RowSlotPainter} for why a branch at the
     * surface cannot keep it.
     *
     * @param rowSlotPainter what each kind looks like on the surface drawing this row
     */
    void paintSlot(RowSlotPainter rowSlotPainter);

    /**
     * A small image drawn in the slot - a faction crest, an icon, whatever the host supplies. It hangs
     * as a square as tall as its line, so it sits level with the label beside it whatever face that
     * label draws in, and a stack of rows shows equally-sized images without any row stating a size.
     *
     * <p>It carries an optional tint the draw multiplies it by, which is how a row states that its
     * mark reads back from the rest of the stack: a receded row's crest has to recede with the words
     * beside it, or the row reads as a rendering slip rather than as a state. The tint is stated per
     * slot because the state it stands for is per row, while the gutter stays even because a stack's
     * tints come from one palette in whatever builds the rows rather than from each item.
     *
     * <p>A tint darkens; it does not fade. Alpha stays the host's, so a row cannot quietly composite
     * at an opacity of its own - which also means a tint carrying alpha of its own would re-introduce
     * exactly the fade this separation avoids, and callers pass an opaque colour.
     *
     * @param spritePath the image's {@code graphics} texture path
     * @param tintColour the colour the texture is multiplied by, or null to draw it as authored
     */
    record Image(
        String spritePath,
        Color tintColour) implements RowSlot {

        /**
         * Rejects a null path, since a slot holding no image is {@link #EMPTY} rather than an image
         * with nothing to load. A null otherwise surfaces at the texture lookup inside a draw call,
         * well past the point that could say which row was meant.
         */
        public Image {
            Objects.requireNonNull(spritePath, "spritePath");
        }

        /**
         * The image drawn in its own colours - the ordinary case, since most marks are authored in
         * the shade they are meant to read at and a stack that tints none of its rows would
         * otherwise have to spell out a no-op multiply on every one of them.
         *
         * @param spritePath the image's {@code graphics} texture path
         */
        public Image(String spritePath) {
            this(spritePath, null);
        }

        @Override
        public float computeWidth(float lineHeight, StyledSpanMeasurer spanMeasurer) {
            return lineHeight;
        }

        @Override
        public boolean isFilled() {
            return true;
        }

        @Override
        public void paintSlot(RowSlotPainter rowSlotPainter) {
            rowSlotPainter.paintImageSlot(this);
        }
    }

    /**
     * A run of text drawn in the slot - a value, a count, a short caption. It carries its colour with
     * it as a {@link TextSpan} does, so a slot's text and the colour it draws in cannot be reached for
     * separately and drift. One colour for the whole of it: a value with a stretch picked out in its own
     * shade is {@link TextRuns}.
     *
     * @param textSpan the run and the colour it draws in, before any opacity fade the host applies
     */
    record Text(
        TextSpan textSpan) implements RowSlot {

        /**
         * Rejects a null span, since a slot holding no text is {@link #EMPTY} - and a blank-but-present
         * span is how a run that draws nothing is spelled, so a null is a mistake rather than an
         * unstated absence.
         */
        public Text {
            Objects.requireNonNull(textSpan, "textSpan");
        }

        @Override
        public float computeWidth(float lineHeight, StyledSpanMeasurer spanMeasurer) {
            // A run that came out blank reserves nothing, so a caller that assembled one from parts and
            // came up empty gets the column it would have had without it rather than a gap held open in
            // front of no glyphs. Which runs read as blank is TextSpan's rule, not a second one here.
            if (!textSpan.hasContent()) {
                return NO_WIDTH;
            }
            return (float) spanMeasurer.measureSpanWidth(textSpan);
        }

        // A run that came out blank fills nothing, so a slot holding one is charged and drawn as the
        // absence it is - the same rule its width answers by, read here without a face to measure with.
        @Override
        public boolean isFilled() {
            return textSpan.hasContent();
        }

        @Override
        public void paintSlot(RowSlotPainter rowSlotPainter) {
            rowSlotPainter.paintTextSlot(this);
        }
    }

    /**
     * A value drawn as several runs of text, each in its own colour - a finding and the working behind
     * it, say, or a reading and the unit it is stated in. The runs read left to right inside the one
     * column the slot occupies, spaced by the same word gap a label's runs are, so a stack of rows still
     * lines its values up however many runs each of them is made of.
     *
     * <p>Kept apart from {@link Text} rather than replacing it, because a value of one run is what almost
     * every row carries: spelled as a list of one, every caller building a plain value would compose a
     * list and every reader of one would unwrap it. What the two must not differ on is what a run is
     * worth, so both are measured by the rule a run of text is measured by and both charge a blank run
     * nothing.
     *
     * <p>How the runs compose - the gap between two of them, and a blank one costing neither gap nor
     * width - is {@link LabelRuns}' rule rather than a second one stated here, so a value picked out in
     * two colours is spaced exactly as a label picked out in two colours is.
     *
     * @param textSpans the runs in reading order, each with the colour it draws in before any opacity
     *                  fade the host applies; never empty
     */
    record TextRuns(
        List<TextSpan> textSpans) implements RowSlot {

        /**
         * Copies the runs and rejects an empty or null-bearing list, since a slot holding no value is
         * {@link #EMPTY} and a value that draws nothing is a blank run - so neither absence needs an
         * empty list to spell it, and a null would otherwise surface inside a measurement or a draw.
         */
        public TextRuns {
            textSpans = List.copyOf(textSpans);
            if (textSpans.isEmpty()) {
                throw new IllegalArgumentException("textSpans must carry at least one run");
            }
        }

        /**
         * Where each run sits relative to the slot's own left edge, and how wide the runs come to
         * together. Offered beside the width because a renderer laying the runs inside the column needs
         * both, and answering them from one walk is what stops the room the column reserved and the runs
         * painted into it from disagreeing.
         *
         * @param lineHeight   the height of the line the slot sits on, in UI units
         * @param spanMeasurer the width measurement already bound to the look that line draws in
         * @return each run's offset from the slot's left edge, and the width the runs occupy together
         */
        public LabelRunOffsets measureRunOffsets(float lineHeight, StyledSpanMeasurer spanMeasurer) {
            return LabelRuns.measureRunOffsets(
                List.<LabelRun>copyOf(textSpans),
                lineHeight,
                spanMeasurer);
        }

        // The column is reserved from the same walk the runs are laid out by, so a value of two runs is
        // charged the gap between them rather than the sum of the glyphs alone.
        @Override
        public float computeWidth(float lineHeight, StyledSpanMeasurer spanMeasurer) {
            return measureRunOffsets(lineHeight, spanMeasurer).runsWidth();
        }

        // Filled where any one run has something to draw, which is exactly when the walk above charges
        // the slot a width - so what reserves the column and what paints into it cannot disagree.
        @Override
        public boolean isFilled() {
            return textSpans
                .stream()
                .anyMatch(TextSpan::hasContent);
        }

        @Override
        public void paintSlot(RowSlotPainter rowSlotPainter) {
            rowSlotPainter.paintTextRunsSlot(this);
        }
    }

    /**
     * A tick box drawn in the slot, ticked or clear. It squares off its line as an image does, so a row
     * that leads with a tick lays its label exactly where a crested row lays its own. The lit state
     * rides along because it is what the box draws as, not what a click on it means - whether the row
     * is clickable at all, and what the click does, stay with the container.
     *
     * @param isTicked whether the box draws ticked
     */
    record Tick(
        boolean isTicked) implements RowSlot {

        @Override
        public float computeWidth(float lineHeight, StyledSpanMeasurer spanMeasurer) {
            return lineHeight;
        }

        @Override
        public boolean isFilled() {
            return true;
        }

        @Override
        public void paintSlot(RowSlotPainter rowSlotPainter) {
            rowSlotPainter.paintTickSlot(this);
        }
    }

    /**
     * A small filled triangle drawn in the slot, pointing up or down - the marker a sort selector shows
     * where the body font renders no up/down glyph. What the direction stands for is the host's, so the
     * slot names the shape and nothing more.
     *
     * @param triangleDirection which way the triangle points
     */
    record Triangle(
        TriangleDirection triangleDirection) implements RowSlot {

        /**
         * Rejects a null direction, since a slot showing no triangle is {@link #EMPTY} rather than a
         * triangle pointing nowhere - and a renderer given one has no shape to fall back on.
         */
        public Triangle {
            Objects.requireNonNull(triangleDirection, "triangleDirection");
        }

        @Override
        public float computeWidth(float lineHeight, StyledSpanMeasurer spanMeasurer) {
            // The triangle's slot is sized where its box is, so the width reserved for the marker and
            // the width it is drawn at cannot disagree.
            return IconLabelRow.computeDirectionTriangleSlotWidth(lineHeight);
        }

        @Override
        public boolean isFilled() {
            return true;
        }

        @Override
        public void paintSlot(RowSlotPainter rowSlotPainter) {
            rowSlotPainter.paintTriangleSlot(this);
        }
    }

    /**
     * A slot a row does not fill. It is a member of the set rather than a null so that measuring and
     * placing a row take one path whether or not each of its columns is filled, and so a row that fills
     * neither flanking column is still a row of the same shape as one that fills both.
     */
    record Empty() implements RowSlot {

        // The column an unfilled slot sits in may still be reserved by another row that fills it, which
        // is the container's decision rather than this slot's - this one is worth nothing either way.
        @Override
        public float computeWidth(float lineHeight, StyledSpanMeasurer spanMeasurer) {
            return NO_WIDTH;
        }

        @Override
        public boolean isFilled() {
            return false;
        }

        @Override
        public void paintSlot(RowSlotPainter rowSlotPainter) {
            rowSlotPainter.paintEmptySlot();
        }
    }
}
