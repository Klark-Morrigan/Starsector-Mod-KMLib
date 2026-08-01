package kmlib.starsector.ui.highlight;

import java.awt.Color;
import java.util.Objects;

/**
 * Atomic pair of a highlight token and the colour it should render in.
 *
 * <p>The Starsector text APIs natively take two parallel arrays - one
 * of substrings to highlight, one of colours - which makes it easy for
 * the two to drift out of sync on the way to the API call. Binding
 * the substring and the colour into one immutable value at the call
 * site eliminates the alignment risk: the only place that sees them
 * separately is the renderer, which fans them back out into the
 * parallel arrays the engine wants.
 */
public final class Highlight {
    private final String text;
    private final Color colour;

    public Highlight(String text, Color colour) {
        this.text = Objects.requireNonNull(text, "text");
        this.colour = Objects.requireNonNull(colour, "colour");
    }

    /** Short factory for call sites that import statically. */
    public static Highlight of(String text, Color colour) {
        return new Highlight(text, colour);
    }

    public String getText() {
        return text;
    }

    public Color getColour() {
        return colour;
    }
}
