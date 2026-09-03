package kmlib.testfixtures.starsector.ui.label;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

/**
 * The drawable words a widget holds: what they say, and which runs of them are lit and in what
 * colour. Shipped from KMLib so both KMLib's and consuming mods' tests read a widget's words the
 * same way.
 *
 * <p>A {@link LabelAPI}, because that is how the engine hands a widget's words to anything outside
 * it - obfuscated widget or not - so a stand-in that were not one could not stand where one does.
 *
 * <p>It keeps the text and the lit runs and nothing else. Everything a label does on its own behalf
 * - drawing, measuring, placing, flashing - throws, because a subject that strayed into any of it
 * would be doing something no reader of a widget's words has cause to do, and answering silently
 * would let that pass unnoticed.
 *
 * <p>The lit runs are kept as handed over rather than resolved against the text. Whether a run
 * actually occurs in the words is the engine's business at draw time and is exactly what a caller
 * can get wrong, so a fixture that quietly dropped a run that did not match would hide the mistake
 * it exists to expose.
 */
public final class ButtonLabelFake implements LabelAPI {

    private static final String NOT_DRAWN_HERE =
        "A fixture for a widget's words models what they say and what is lit in them, not how they "
            + "are drawn.";

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

    @Override
    public void setHighlight(String... runs) {
        highlightedRuns = List.of(runs);
    }

    @Override
    public void setHighlightColors(Color... colours) {
        highlightColours = Arrays.asList(colours);
    }

    @Override
    public PositionAPI autoSizeToWidth(float width) {
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
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
        throw new UnsupportedOperationException(NOT_DRAWN_HERE);
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
    public void highlightFirst(String run) {
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
    public void setHighlightColor(Color colour) {
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
