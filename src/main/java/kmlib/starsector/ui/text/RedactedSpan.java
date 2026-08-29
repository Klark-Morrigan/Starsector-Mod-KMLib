package kmlib.starsector.ui.text;

import kmlib.colour.Colours;
import kmlib.math.geometry.Rectangle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A name withheld from the line it belongs on, carried as the lengths of the words it was made of. It
 * draws as one filled block per word, so the shape says how many words there were and how long each ran
 * and nothing at all about which letters they were.
 *
 * <p>Lengths rather than the name itself, and that is the whole point of the type: a run holding the text
 * it must not show is a run some later change draws. What reaches this is a count per word, taken where
 * the line was built and the name dropped there, so there is nothing here to leak however it is measured,
 * logged, or painted.
 *
 * <p>Charged as that many <em>representative</em> characters rather than as the name's own measured
 * width, because character counts are what the row shows: measuring the real name would drag it through
 * the layout to arrive at a width, which is the one thing the run exists not to hold. The blocks
 * therefore stand where the words stood without matching their exact reach.
 *
 * <p>Spaced and blanked by the label's own rules rather than by arithmetic of its own - the words are
 * measured as the runs of a small sentence ({@link LabelRuns#measureRunOffsets}), so the gap between two
 * blocks is the face's word space, the same one parting the words either side of the redaction, and a
 * word of no characters is charged nothing exactly as a blank run of text is.
 *
 * @param wordLengths how many characters each withheld word ran to, in reading order
 * @param colour      the colour the line the redaction stands on is written in, which its blocks are
 *                    filled a step short of - see {@link #resolveBlockFillColour(float)}
 */
public record RedactedSpan(
    List<Integer> wordLengths,
    Color colour) implements LabelRun {

    // The character every withheld word is charged by. A digit rather than a letter because a run is
    // measured through the look the label is spoken in, and a look that shouts its lines would case-shift
    // a letter into a different width - while digits stand at one width in the faces this draws in, which
    // is also what makes a block's width read as a character count rather than as a guess at the name.
    private static final String REPRESENTATIVE_CHARACTER = "0";

    // The length at which a word has nothing to stand for. Named so the empty reading is stated once
    // rather than as a bare zero at each place that has to take it.
    private static final int NO_CHARACTERS = 0;

    // How much of the line's colour a block may keep: none of it, through all of it. The bounds a
    // darkening strength is read within - one taken off the whole leaves the kept share, and past either
    // end the arithmetic still yields a colour, the channels clamping, but no longer the reading the
    // caller asked for. A slider handing over 1.4 would otherwise sit at black for its whole top third
    // with nothing on screen saying why.
    private static final float NO_COLOUR = 0f;
    private static final float WHOLE_COLOUR = 1f;

    /**
     * The strength a block fills at the weight of the line it stands in at, on the faces a KM label is
     * ordinarily set in - what a surface takes unless it has measured the balance itself or put it in the
     * player's hands.
     *
     * <p>Well short of the line's own colour, because a block covers every pixel of its band outright
     * where the glyphs it stands in for spend much of their own footprint at partial alpha: at equal
     * colour the block is the heavier mark by some way, and a withheld name reading louder than the words
     * either side of it draws the eye to exactly the thing the line is declining to say.
     *
     * <p>Offered as a value rather than left for each surface to spell, because it is a finding about how
     * a solid block reads beside glyphs rather than a taste: a surface restating it would be restating a
     * measurement someone else made, and two surfaces restating it would eventually disagree.
     */
    public static final float TEXT_WEIGHT_DARKENING_STRENGTH = 0.4f;

    /**
     * Copies the lengths and rejects a shape no redaction can have, where the caller that derived them is
     * still on the stack: a null length or a negative one otherwise surfaces inside a measurement or a
     * draw call, well past the point that could say which name was meant. An empty list is allowed and
     * reads as a run with nothing to draw, the same absence a blank {@link TextSpan} spells.
     */
    public RedactedSpan {
        Objects.requireNonNull(wordLengths, "wordLengths");
        Objects.requireNonNull(colour, "colour");
        wordLengths = List.copyOf(wordLengths);
        if (wordLengths.stream().anyMatch(wordLength -> wordLength < NO_CHARACTERS)) {
            throw new IllegalArgumentException("wordLengths must not carry a negative length");
        }
    }

    /**
     * Never - the blocks are the only way a withheld name says anything, so a line that leaves them out
     * does not read as redacted, it reads as though the name had never been there. A surface that can only
     * draw a line of glyphs is refused this run rather than quietly shortened.
     *
     * @return false, always
     */
    @Override
    public boolean canBeLeftOutOfLineText() {
        return false;
    }

    /**
     * How wide the blocks and the gaps between them come to together, measured through the face the label
     * is spoken in. A run standing for no words at all is charged nothing.
     *
     * <p>The line height is not read: a block stands as wide as the characters it replaces, so what sizes
     * an image run against its line has no bearing on this one.
     */
    @Override
    public float computeWidth(float lineHeight, StyledSpanMeasurer spanMeasurer) {
        return LabelRuns
            .measureRunOffsets(composeWordSpans(), lineHeight, spanMeasurer)
            .runsWidth();
    }

    /**
     * Whether the redaction has anything to stand for - true where at least one word ran to a character.
     * Asked without a face, like every run, so a caller deciding whether a line draws at all needs none.
     *
     * @return true when a word of the withheld name had characters
     */
    @Override
    public boolean hasContent() {
        return wordLengths
            .stream()
            .anyMatch(wordLength -> wordLength > NO_CHARACTERS);
    }

    /**
     * The block standing for each withheld word, laid left to right from {@code leftX} in the band the
     * line occupies. A word of no characters yields no block, so the caller paints exactly what the
     * measurement charged room for.
     *
     * <p>The band is the caller's to state rather than derived here, because where a line's text sits and
     * how tall it stands are facts about the surface drawing it - a box anchoring its rows by their tops
     * and a strip centring them on a row stand the same run on two different footings.
     *
     * @param leftX        the run's left edge, as its label measured it out
     * @param bottomY      the bottom of the band the blocks fill, in UI units
     * @param barHeight    how tall the blocks stand, in UI units
     * @param spanMeasurer the width measurement already bound to the face the label draws in
     * @return one block per word that has characters, in reading order
     */
    public List<Rectangle> layOutWordBars(
            float leftX,
            float bottomY,
            float barHeight,
            StyledSpanMeasurer spanMeasurer) {

        var wordOffsets = LabelRuns.measureRunOffsets(composeWordSpans(), barHeight, spanMeasurer);
        var eachWordWidths = wordOffsets.eachRunWidths();
        var wordBars = new ArrayList<Rectangle>(eachWordWidths.size());

        for (var index = 0; index < eachWordWidths.size(); index++) {
            var wordWidth = eachWordWidths.get(index);

            // A word that ran to nothing was charged nothing, so it takes no block: the walk above already
            // said which words draw, and asking the spans a second time would be a second answer to it.
            if (wordWidth <= LabelRun.NO_WIDTH) {
                continue;
            }
            wordBars.add(new Rectangle(
                leftX + wordOffsets.runOffsetXs().get(index),
                bottomY,
                wordWidth,
                barHeight));
        }
        return List.copyOf(wordBars);
    }

    /**
     * Hands this run to {@code labelRunPainter} as the kind it is, so a surface paints a redaction by
     * saying what one looks like rather than by testing what the run happens to be.
     */
    @Override
    public void paintRun(LabelRunPainter labelRunPainter, float runX) {
        labelRunPainter.paintRedactedSpan(this, runX);
    }

    /**
     * The colour a block is actually filled in: the line's own, sunk toward black by
     * {@code darkeningStrength} so a solid block weighs what the glyphs either side of it weigh rather
     * than shouting over them.
     *
     * <p>How the correction is made is this type's - toward black rather than by fading, so a surface
     * drawing at part opacity does not thin the redaction twice over - while how far to make it is the
     * surface's, because it turns on the face, the size it draws at, and how that atlas was rasterised,
     * none of which a run can read. {@link #TEXT_WEIGHT_DARKENING_STRENGTH} is that judgement made on the
     * faces a KM label is ordinarily set in.
     *
     * @param darkeningStrength how much of the line's colour to take off the blocks, 0..1 - 0 fills them
     *                          in the line's own colour and 1 fills them black; read outside that range
     *                          as its nearer end
     * @return the colour every block of this redaction is filled in
     */
    public Color resolveBlockFillColour(float darkeningStrength) {
        var keptColour = Math.max(
            NO_COLOUR,
            Math.min(WHOLE_COLOUR, WHOLE_COLOUR - darkeningStrength));

        return Colours.darken(colour, keptColour);
    }

    // The withheld words as the runs of a small sentence: each word a throwaway span of as many
    // representative characters as it ran to, in this run's own colour. Composed rather than measured
    // directly so the words are spaced and blanked by the same rules the label parts its own runs by -
    // one gap rule for the whole line, wherever a redaction sits in it.
    private List<LabelRun> composeWordSpans() {
        var wordSpans = new ArrayList<LabelRun>(wordLengths.size());
        for (var wordLength : wordLengths) {
            wordSpans.add(new TextSpan(REPRESENTATIVE_CHARACTER.repeat(wordLength), colour));
        }
        return wordSpans;
    }
}
