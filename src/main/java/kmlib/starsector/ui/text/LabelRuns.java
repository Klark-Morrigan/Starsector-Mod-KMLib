package kmlib.starsector.ui.text;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A label's runs read as one sentence: the floor every label is held to, how a run is added to one, the
 * single line the runs read as, and where they measure out to when laid side by side. Named once because
 * more than one kind of content carries a label - a row of a stack, a control of a strip - and how runs
 * compose is a fact about runs rather than about whichever content happens to hold them.
 *
 * <p>Runs <em>flow</em>: each starts one word space past where the one before it measured out, and none
 * of them is charged to a column. That is what parts them from the slots flanking a label, which are
 * columns reserved at one width across a whole stack so the labels between them line up. A surface that
 * laid runs as columns would align the second colour of every line, which is not what picking a stretch
 * of a sentence out means.
 *
 * <p>A run may {@linkplain LabelRun#isJoinedToPreviousRun join} the one before it instead, spending no
 * gap at all - which is what lets a label pick a stretch out of the <em>middle</em> of a word.
 *
 * <p>That space is the drawing face's own, measured on the line the runs sit on, so a label spaces its
 * runs the way the font spaces its words and a stack of lines at several sizes reads at one rhythm. A
 * fixed number cannot: it is a word space at whichever size it was chosen for and a column break at every
 * smaller one.
 *
 * <p>Both forms a label can be laid in are spaced here - as geometry where the runs are placed one by one
 * ({@link #measureRunOffsets}), and as a character where they are joined into a single draw
 * ({@link #resolveLineText}). Spacing is therefore never something an author writes into a phrase: a
 * caller that wrote its own would have to know which form its content ends up drawn in, and a label
 * carrying a separator of its own is spaced twice on the form that already spaces it.
 *
 * <p>What a run is - a stretch of text, a small image, a withheld name - is {@link LabelRun}'s sealed
 * set, and nothing here reads it: a run is asked its own width and whether it draws at all, so a label
 * composed of words and one composed of words around a crest are laid by the same walk.
 *
 * <p>A label carries at least one run and no null one, checked where the label is built rather than where
 * it is drawn: content with no runs at all is not a line, and a null run otherwise surfaces inside a
 * measurement or a draw call, well past the point that could say which line was meant.
 */
public final class LabelRuns {

    // The space charged between two drawn runs of one label: the face's own space glyph, measured on the
    // line the runs sit on. A word space rather than a column, so it is charged only where one run
    // actually follows another and asks to stand clear of it - and held here rather than per surface,
    // since a strip and a tooltip spacing the same runs differently would read as two different rules.
    //
    // Measured rather than fixed because the runs of a label are one sentence, and how wide a word space
    // is in a sentence is the font's statement, not the layout's. A flat number is only ever right at one
    // size: it reads tight under a 20pt heading and, on a stack that shrinks its deeper levels, grows to
    // two thirds the glyph height by the time it reaches a footnote - so the same rule that reads as a
    // space at the top of a box reads as a column break at the bottom of it.
    //
    // A colour is needed to build a span and says nothing about its width; nothing is ever drawn from
    // this one.
    private static final String WORD_SPACE_TEXT = " ";
    private static final TextSpan WORD_SPACE = new TextSpan(WORD_SPACE_TEXT, Color.WHITE);

    private LabelRuns() {
    }

    /**
     * Returns {@code labelRuns} with {@code labelRun} appended - the runs read as one sentence, so a run
     * is added to what is already there rather than replacing it, and a second colour (or a crest set
     * among the words) never costs a caller the first.
     *
     * @param labelRuns the label's runs so far, left as they are
     * @param labelRun  the run continuing the label
     * @return the runs with that one last, for the caller's own constructor to hold to the floor
     */
    public static List<LabelRun> appendRun(List<LabelRun> labelRuns, LabelRun labelRun) {
        var continuedRuns = new ArrayList<>(labelRuns);
        continuedRuns.add(labelRun);
        return continuedRuns;
    }

    /**
     * Copies a label's runs and rejects an empty or null-bearing one, where the caller that built the
     * label is still on the stack. The floor lives here rather than at each thing that carries a label,
     * so a control's own label cannot be held to a looser rule than a row's.
     *
     * @param labelRuns the label's runs in reading order
     * @return an immutable copy of the runs
     */
    public static List<LabelRun> copyRuns(List<LabelRun> labelRuns) {
        Objects.requireNonNull(labelRuns, "labelRuns");
        var copiedRuns = List.<LabelRun>copyOf(labelRuns);
        if (copiedRuns.isEmpty()) {
            throw new IllegalArgumentException("labelRuns must carry at least one run");
        }
        return copiedRuns;
    }

    /**
     * Whether a label draws anything at all - true where any one of its runs has something to show.
     *
     * <p>Asked of the runs rather than measured, the way a {@link kmlib.starsector.ui.widgets.RowSlot} is
     * asked whether it is filled: a caller deciding whether a label is there to be pointed at holds no
     * face to measure it against, and should not have to resolve one to learn that every run of it came
     * out blank. Which runs read as blank is each run's own rule, so a label answers here exactly as the
     * walk below charges it.
     *
     * @param labelRuns the label's runs in reading order
     * @return true when at least one run draws
     */
    public static boolean hasDrawnRun(List<LabelRun> labelRuns) {
        return labelRuns
            .stream()
            .anyMatch(LabelRun::hasContent);
    }

    /**
     * The gap one label charges between two of its drawn runs: the drawing face's own word space,
     * measured through the look the label is set in.
     *
     * <p>Offered beside the walk that spends it because a surface setting something <em>between</em> a
     * label and what follows it - a rule led from the end of a label across to the value it points at -
     * has to stand off the words by the very space the label parts its own runs by. Held off by a number
     * of its own, that mark reads as belonging to a second rhythm than the line it sits on, and does so
     * differently at every size the box draws at. Read off the same span the walk charges, so the two
     * cannot answer differently.
     *
     * @param measurer the width measurement already bound to the face the label draws in
     * @return the face's own word space, in UI units
     */
    public static float measureWordSpaceWidth(StyledSpanMeasurer measurer) {
        return (float) measurer.measureSpanWidth(WORD_SPACE);
    }

    /**
     * Where each of a label's runs sits relative to the label's own left edge, how wide each one came out,
     * and how wide they come to together. One walk, returned whole, because a caller re-deriving any part
     * of it from the rest would be re-deciding the gap rule - and a placement disagreeing with the width
     * its host was sized to is exactly the drift a measurement exists to prevent. The per-run widths ride
     * along for the same reason: a caller drawing something the width of a run it did not measure itself
     * would otherwise charge the face a second time for a number this walk already had.
     *
     * <p>A run with nothing to draw is charged neither gap nor width and anchors where its predecessor
     * ended, so a caller assembling a run from parts and coming up blank gets the line it would have had
     * without it rather than a gap reserved in front of no glyphs.
     *
     * @param labelRuns  the label's runs in reading order
     * @param lineHeight the height of the line the runs sit on, in UI units - what an image run squares
     *                   itself off, and ignored by a run of text
     * @param measurer   the width measurement already bound to the face the label draws in, which is
     *                   also asked for the face's own space - the gap charged between two drawn runs
     * @return each run's offset from the label's left edge, each run's own width, and the width the runs
     *         occupy together
     */
    public static LabelRunOffsets measureRunOffsets(
            List<LabelRun> labelRuns,
            float lineHeight,
            StyledSpanMeasurer measurer) {

        var runOffsetXs = new ArrayList<Float>(labelRuns.size());
        var eachRunWidths = new ArrayList<Float>(labelRuns.size());

        // Asked once for the whole label rather than per gap: every run of a label is spoken in the one
        // look, so a second measurement could only ever return the same number at a cost.
        var wordSpaceWidth = measureWordSpaceWidth(measurer);
        var runsWidth = 0f;
        var hasDrawnRun = false;

        for (var labelRun : labelRuns) {
            if (!labelRun.hasContent()) {
                runOffsetXs.add(runsWidth);
                eachRunWidths.add(LabelRun.NO_WIDTH);
                continue;
            }
            if (hasDrawnRun && !labelRun.isJoinedToPreviousRun()) {
                runsWidth += wordSpaceWidth;
            }
            var runWidth = labelRun.computeWidth(lineHeight, measurer);

            runOffsetXs.add(runsWidth);
            eachRunWidths.add(runWidth);
            runsWidth += runWidth;
            hasDrawnRun = true;
        }
        return new LabelRunOffsets(
            List.copyOf(runOffsetXs),
            List.copyOf(eachRunWidths),
            runsWidth);
    }

    /**
     * The whole label as one line: the text of its runs in reading order, one word space between each
     * two that draw. For a surface that lays a label in a single draw and charges it a single
     * measurement, where each run's own anchor is never worked out and so the runs read as the one
     * sentence they already are.
     *
     * <p>Spaced by the same rule the run-by-run form is placed by, spelled as a character here because
     * this form has no geometry to space with - there are no per-run anchors to set apart, so the one
     * thing that can part two runs is a space in the string. That the two forms agree is what lets a
     * label be authored once and laid either way: a caller writing its own separator would have to know
     * which of the two its content ends up on, and would double the space on the one and be right on the
     * other.
     *
     * <p>A run with nothing to draw is passed over rather than joined, so it costs the line neither a
     * space nor an empty stretch - the same reading {@link #measureRunOffsets} charges it nothing by.
     * Neither the colours nor any run without glyphs survives the join, since one line drawn once draws
     * in one colour and holds only glyphs; an image therefore takes no space here either, where the
     * run-by-run form squares one off its line. A surface sizing itself from this alone reserves nothing
     * for a label's images; one that shows them reads the runs themselves, measures through
     * {@link #measureRunOffsets}, and paints through {@link LabelRunPainter}.
     *
     * <p>A run that cannot be left out ({@link LabelRun#canBeLeftOutOfLineText}) is refused rather than
     * dropped. Dropping it would hand back a line that reads as complete while missing what the run was
     * put there to say, and the surface receiving it has no way to tell - so the label is rejected where
     * it is flattened, which is the last point that still knows a run was lost.
     *
     * @param labelRuns the label's runs in reading order
     * @return the text of the runs that draw, joined by one space each
     * @throws IllegalArgumentException where a run that draws cannot be left out of a flattened line
     */
    public static String resolveLineText(List<LabelRun> labelRuns) {
        var lineText = new StringBuilder();
        for (var labelRun : labelRuns) {

            // Asked only of runs that draw: a run that came out blank says nothing this form could lose,
            // whatever kind it is.
            if (labelRun.hasContent() && !labelRun.canBeLeftOutOfLineText()) {
                throw new IllegalArgumentException(
                    "labelRuns carries a run that cannot be read as one line of text: "
                        + labelRun.getClass().getSimpleName()
                        + " must be laid out run by run and painted through a LabelRunPainter");
            }
            if (!(labelRun instanceof TextSpan textSpan) || !textSpan.hasContent()) {
                continue;
            }
            // Spent between two runs that draw rather than in front of every one, so a label opening on
            // an image or on a run that came out blank still starts on its first word. Withheld from a
            // run that joins the one before it, for the reason the placed form withholds the width.
            if (!lineText.isEmpty() && !textSpan.isJoinedToPreviousRun()) {
                lineText.append(WORD_SPACE_TEXT);
            }
            lineText.append(textSpan.text());
        }
        return lineText.toString();
    }

    /**
     * One label's runs measured out from the label's own left edge: where each run starts, how wide each
     * one is, and how wide they come to together.
     *
     * <p>The per-run widths are held beside the anchors rather than left to be measured again, so a caller
     * that draws something the size of one run - a block standing in for a word, a rule under a stretch of
     * the line - takes the width this walk already charged the label. Two readings of one run's width are
     * two numbers that can disagree, and the one that reserved the room is the one the layout believed.
     *
     * @param runOffsetXs   each run's offset from the label's left edge, in run order
     * @param eachRunWidths each run's own width, in the same order, {@link LabelRun#NO_WIDTH} for a run
     *                      that draws nothing
     * @param runsWidth     the width the runs occupy together, gaps included
     */
    public record LabelRunOffsets(
        List<Float> runOffsetXs,
        List<Float> eachRunWidths,
        float runsWidth) {
    }
}
