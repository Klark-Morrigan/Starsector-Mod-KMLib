package kmlib.starsector.ui.font;

import java.util.Collection;

/**
 * Settles which face a category of text draws in, from what the player picked and what the install
 * holds. The one place a category's Radio and its automatic choice are turned into an atlas, so every
 * category answers a missing glyph and an unloadable face by the same rules.
 *
 * <p>The automatic choice reads the text, never a locale. It keeps the category's preferred face unless
 * that face's installed atlas lacks a character the category draws, and then falls to the largest
 * offered face that covers every one - largest by the line height its installed atlas states, because
 * the size an atlas was rasterised at is what decides how far it can be drawn before it blurs. So an
 * English build on a vanilla install keeps the preferred face, while the same build on a localised
 * install drawing localised names gets a face that holds their glyphs.
 *
 * <p>A face that will not load is never an answer while another will: the reader states such a face at
 * no line height, and the resolver treats that as absent. A named face the player picked that will not
 * load, or that the category no longer offers, resolves as the automatic choice would. No path leaves a
 * category without a face; if nothing offered loads at all, the preferred face is answered and draws
 * nothing, as it would have without this.
 *
 * <p>The probe reads every glyph of every text asked about, so a caller settles a category once for the
 * text it will draw and holds the answer rather than resolving per frame.
 */
public final class FaceResolver {

    // The line height the reader states for a face that will not load, and so the floor a loadable face
    // stands above.
    private static final double NO_LINE_HEIGHT = 0d;

    private final GlyphCoverageReader glyphCoverage;
    private final FaceLineHeightReader lineHeights;

    /**
     * @param lineHeights   where an installed atlas's line height is read, which also says whether the
     *                      face loads at all
     * @param glyphCoverage where an installed atlas's coverage of a text is read
     */
    public FaceResolver(FaceLineHeightReader lineHeights, GlyphCoverageReader glyphCoverage) {

        this.lineHeights = lineHeights;
        this.glyphCoverage = glyphCoverage;
    }

    /**
     * @return a resolver reading the running game's installed atlases through LazyLib
     */
    public static FaceResolver createInstalledFaceResolver() {

        return new FaceResolver(
            LazyFontLineHeightReader::readLineHeight,
            LazyFontGlyphCoverageReader::coversText);
    }

    /**
     * The face a category draws in.
     *
     * @param offer      the faces the category offers
     * @param choice     what the player picked for it
     * @param probeTexts the text the category draws, which the automatic choice needs covered
     * @return the face to draw in
     */
    public StarsectorFont resolveFont(FaceOffer offer, FaceChoice choice, Collection<String> probeTexts) {

        return choice.selectByCase(
            () -> resolveAutomaticFont(offer, probeTexts),
            namedFont -> isFontOfferedAndLoadable(offer, namedFont)
                ? namedFont
                : resolveAutomaticFont(offer, probeTexts));
    }

    // The automatic choice: the preferred face where it loads and covers the text, else the largest
    // offered face that does, else whatever still draws.
    private StarsectorFont resolveAutomaticFont(FaceOffer offer, Collection<String> probeTexts) {

        var preferredFont = offer.preferredFont();
        if (isFontLoadable(preferredFont) && isEveryTextCovered(preferredFont, probeTexts)) {
            return preferredFont;
        }
        StarsectorFont largestCoveringFont = null;
        var largestLineHeight = NO_LINE_HEIGHT;

        for (var offeredFont : offer.listOfferedFonts()) {

            var lineHeight = lineHeights.readLineHeight(offeredFont);

            // Strictly larger, so a tie keeps the face the offer lists first.
            if (lineHeight > largestLineHeight && isEveryTextCovered(offeredFont, probeTexts)) {

                largestCoveringFont = offeredFont;
                largestLineHeight = lineHeight;
            }
        }
        return largestCoveringFont != null
            ? largestCoveringFont
            : resolveAnyDrawingFont(offer);
    }

    // What a category draws in when no offered face covers its text: the preferred face where it loads,
    // since a partial row of glyphs in the face meant for the job reads better than the same gaps in
    // another; the first offered face that loads otherwise; and the preferred face when nothing loads.
    private StarsectorFont resolveAnyDrawingFont(FaceOffer offer) {

        for (var offeredFont : offer.listOfferedFonts()) {

            if (isFontLoadable(offeredFont)) {
                return offeredFont;
            }
        }
        return offer.preferredFont();
    }

    // Whether a face draws every character of every text the category draws.
    private boolean isEveryTextCovered(StarsectorFont font, Collection<String> probeTexts) {
        return probeTexts.stream()
            .allMatch(probeText -> glyphCoverage.coversText(font, probeText));
    }

    private boolean isFontLoadable(StarsectorFont font) {
        return lineHeights.readLineHeight(font) > NO_LINE_HEIGHT;
    }

    private boolean isFontOfferedAndLoadable(FaceOffer offer, StarsectorFont font) {
        return offer.isFontOffered(font) && isFontLoadable(font);
    }
}
