package kmlib.starsector.ui.render.gl;

import kmlib.starsector.ui.text.TextAlignment;

import org.lazywizard.lazylib.ui.LazyFont;

/**
 * The GL half of the alignment seam: it says how a substrate-neutral {@link TextAlignment} spells
 * itself as the anchor LazyFont's drawable strings take. Kept here rather than on the alignment
 * itself so the value type owes nothing to LazyFont and can ride on a style handed to the game's own
 * UI widgets just as readily; kept in one class rather than at each drawer so every GL surface
 * anchors the same alignment identically.
 */
public final class TextAnchors {

    private TextAnchors() {
    }

    /**
     * @param alignment where the text box sits relative to the point it is drawn at
     * @return the LazyFont anchor that positions a drawable string the same way
     */
    public static LazyFont.TextAnchor resolveAnchor(TextAlignment alignment) {
        // Written out per value rather than matched on name or ordinal. The two enums belong to
        // different projects, so a rename or a reorder on either side has to surface as a compile
        // error here - a name-matched lookup would instead fail at runtime, and an ordinal-matched
        // one would quietly anchor text somewhere else.
        return switch (alignment) {
        case TOP_LEFT -> LazyFont.TextAnchor.TOP_LEFT;
        case TOP_CENTER -> LazyFont.TextAnchor.TOP_CENTER;
        case TOP_RIGHT -> LazyFont.TextAnchor.TOP_RIGHT;
        case CENTER_LEFT -> LazyFont.TextAnchor.CENTER_LEFT;
        case CENTER -> LazyFont.TextAnchor.CENTER;
        case CENTER_RIGHT -> LazyFont.TextAnchor.CENTER_RIGHT;
        case BOTTOM_LEFT -> LazyFont.TextAnchor.BOTTOM_LEFT;
        case BOTTOM_CENTER -> LazyFont.TextAnchor.BOTTOM_CENTER;
        case BOTTOM_RIGHT -> LazyFont.TextAnchor.BOTTOM_RIGHT;
        };
    }
}
