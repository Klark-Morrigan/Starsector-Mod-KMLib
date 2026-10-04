package kmlib.testfixtures.starsector.ui.label;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The drawable words a widget holds: what they say, and which runs of them are lit and in what
 * colour. Published as a fixture variant so both KMLib's and consuming mods' tests read a widget's words the
 * same way.
 *
 * <p>A {@link LabelAPI}, because that is how the engine hands a widget's words to anything outside
 * it - obfuscated widget or not - so a stand-in that were not one could not stand where one does.
 *
 * <p>It keeps the text, the lit runs and the width it was last fitted to, and nothing else. It measures
 * at {@link #CHARACTER_WIDTH} a character, so a case can state the width words of a given length fit.
 * A writer lengthening the words has to re-fit the label to them, the game having sized it once to the
 * words it was built with. Everything else a label does on its own behalf - drawing, placing, flashing -
 * throws, because a subject that strayed into any of it would be doing something no reader of a
 * widget's words has cause to do, and answering silently would let that pass unnoticed.
 *
 * <p>The lit runs are kept as handed over rather than resolved against the text. Whether a run
 * actually occurs in the words is the engine's business at draw time and is exactly what a caller
 * can get wrong, so a fixture that quietly dropped a run that did not match would hide the mistake
 * it exists to expose.
 */
public final class ButtonLabelFake implements LabelAPI {

    /** How wide each character of the words measures. */
    public static final float CHARACTER_WIDTH = 7f;

    private static final String NOT_DRAWN_HERE =
        "A fixture for a widget's words models what they say and what is lit in them, not how they "
            + "are drawn.";

    private Optional<Float> fittedWidth = Optional.empty();

    private List<Color> highlightColours = List.of();
    private List<String> highlightedRuns = List.of();

    private String text;

    /**
     * @param text the words the widget was built with
     */
    public ButtonLabelFake(String text) {
        this.text = text;
    }

    /**
     * @return the width the label was last fitted to, or empty where it was never re-fitted
     */
    public Optional<Float> readFittedWidth() {
        return fittedWidth;
    }

    /**
     * @return the colours the lit runs were given, in the order they were handed over
     */
    public List<Color> readHighlightColours() {
        return highlightColours;
    }

    /**
     * @return the runs currently lit, in the order they were handed over
     */
    public List<String> readHighlightedRuns() {
        return highlightedRuns;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        this.text = text;
    }

    // The two ways a label takes a lit run - a list of runs matched into a mask, and one run lit
    // as a range - both land in the same readers here. The fixture records what was asked to be
    // lit and does not model that the game draws the two differently; which of them a subject
    // reaches for is the subject's decision, stated in the subject.
    @Override
    public void highlightFirst(String run) {
        highlightedRuns = List.of(run);
    }

    @Override
    public void setHighlight(String... runs) {
        highlightedRuns = List.of(runs);
    }

    @Override
    public void setHighlightColor(Color colour) {
        highlightColours = List.of(colour);
    }

    @Override
    public void setHighlightColors(Color... colours) {
        highlightColours = Arrays.asList(colours);
    }

    // No placement to hand back: the fixture models no position, and a subject reading one would be
    // placing the label rather than saying something in it.
    @Override
    public PositionAPI autoSizeToWidth(float width) {
        fittedWidth = Optional.of(width);
        return null;
    }

    @Override
    public void advance(float amount) {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
    }

    @Override
    public float computeTextHeight(String text) {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
    }

    @Override
    public float computeTextWidth(String text) {
        return text.length() * CHARACTER_WIDTH;
    }

    @Override
    public void flash(float brightness, float duration) {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
    }

    @Override
    public Color getColor() {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
    }

    @Override
    public float getOpacity() {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
    }

    @Override
    public PositionAPI getPosition() {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
    }

    @Override
    public void highlightLast(String run) {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
    }

    @Override
    public void italicize() {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
    }

    @Override
    public void italicize(float slant) {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
    }

    @Override
    public void render(float alphaMult) {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
    }

    @Override
    public void setAlignment(Alignment alignment) {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
    }

    @Override
    public void setColor(Color colour) {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
    }

    @Override
    public void setHighlight(int start, int end) {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
    }

    @Override
    public void setHighlightOnMouseover(boolean isHighlighted) {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
    }

    @Override
    public void setOpacity(float opacity) {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
    }

    @Override
    public void unhighlightIndex(int index) {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
    }

    @Override
    public void unitalicize() {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
    }
}
