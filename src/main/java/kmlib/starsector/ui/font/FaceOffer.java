package kmlib.starsector.ui.font;

import java.util.Optional;

/**
 * The faces the automatic choice for one category of text may land on: the face it prefers, and the one
 * face it falls back to where the text needs a glyph the preferred face's installed atlas lacks.
 *
 * <p>What a player may pick is not part of an offer. Every face the enum names is offered to every
 * category's Radio, since a player choosing a face picks it knowing what it looks like, and
 * {@link FaceChoice#listChoiceLabels()} states those options once. What is per category is where the
 * automatic choice goes, because it goes there unasked: a face stretched across a map and a face set in a
 * fixed button box want different answers, and only the category knows which.
 *
 * <p>One fallback rather than a list, because a second would only answer the first also lacking the
 * text, and the resolver already answers that - by keeping the preferred face. A category with no
 * fallback keeps its preferred face whatever the text, which is the right answer wherever the preferred
 * atlas is one a core localisation replaces with one holding its script.
 *
 * @param preferredFont the face the category draws in whenever it can
 * @param fallbackFont  the face it draws in where the preferred face's atlas lacks a glyph of the text,
 *                      if it has one; never the preferred face itself
 */
public record FaceOffer(
    StarsectorFont preferredFont,
    Optional<StarsectorFont> fallbackFont) {

    /**
     * Refuses a fallback naming the preferred face, which would fall back to the face that just failed.
     */
    public FaceOffer {
        if (fallbackFont.filter(font -> font == preferredFont).isPresent()) {
            throw new IllegalArgumentException(
                "Face falls back to itself: " + preferredFont.getBasename());
        }
    }

    /**
     * @param preferredFont the face the category always draws in
     * @return an offer with no fallback
     */
    public static FaceOffer createOfferWithoutFallback(StarsectorFont preferredFont) {
        return new FaceOffer(preferredFont, Optional.empty());
    }

    /**
     * @param preferredFont the face the category draws in whenever it can
     * @param fallbackFont  the face it draws in where the text needs a glyph the preferred face lacks
     * @return an offer falling back to that face
     */
    public static FaceOffer createOfferFallingBackTo(StarsectorFont preferredFont, StarsectorFont fallbackFont) {
        return new FaceOffer(preferredFont, Optional.of(fallbackFont));
    }
}
