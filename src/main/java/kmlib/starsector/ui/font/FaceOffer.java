package kmlib.starsector.ui.font;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * The faces one category of text may draw in: the face it prefers, and the curated alternatives a
 * player may pick or the automatic choice may fall to. Curated per category rather than every atlas the
 * install holds, because each category has a job - a label stretched across a map wants a
 * high-resolution atlas, a compact row wants a face that fits its box - and a face wrong for the job is
 * no answer even where it holds every glyph.
 *
 * <p>The preferred face is its own component rather than one member of a list, so an offer cannot prefer
 * a face it does not offer.
 *
 * @param preferredFont    the face the category draws in whenever it can
 * @param alternativeFonts the other faces offered, in the order a Radio lists them; neither repeating
 *                         one another nor the preferred face
 */
public record FaceOffer(
    StarsectorFont preferredFont,
    List<StarsectorFont> alternativeFonts) {

    /**
     * Copies the alternatives and refuses a face offered twice, which a Radio would list twice under one
     * stored label.
     */
    public FaceOffer {

        alternativeFonts = List.copyOf(alternativeFonts);

        var offeredFonts = new HashSet<StarsectorFont>();
        offeredFonts.add(preferredFont);

        for (var alternativeFont : alternativeFonts) {

            if (!offeredFonts.add(alternativeFont)) {

                throw new IllegalArgumentException(
                    "Face offered twice: " + alternativeFont.getBasename());
            }
        }
    }

    /**
     * @return every face offered, the preferred one first
     */
    public List<StarsectorFont> listOfferedFonts() {

        var offeredFonts = new ArrayList<StarsectorFont>(alternativeFonts.size() + 1);

        offeredFonts.add(preferredFont);
        offeredFonts.addAll(alternativeFonts);

        return List.copyOf(offeredFonts);
    }

    /**
     * The labels a Radio offering this category lists, in order: the automatic choice, then each offered
     * face by basename. The one statement of the options, so a settings table can be held to it.
     *
     * @return the Radio's option labels
     */
    public List<String> listChoiceLabels() {

        var choiceLabels = new ArrayList<String>();
        choiceLabels.add(FaceChoice.AUTO_FACE.getLabel());

        for (var offeredFont : listOfferedFonts()) {
            choiceLabels.add(new FaceChoice.NamedFace(offeredFont).getLabel());
        }
        return List.copyOf(choiceLabels);
    }

    /**
     * @param font a face
     * @return whether this offer lists it
     */
    public boolean isFontOffered(StarsectorFont font) {
        return preferredFont == font || alternativeFonts.contains(font);
    }
}
