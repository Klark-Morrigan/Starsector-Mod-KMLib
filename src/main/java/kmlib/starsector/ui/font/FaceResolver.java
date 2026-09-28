package kmlib.starsector.ui.font;

import java.util.Collection;

/**
 * Settles which face a category of text draws in, from what the player picked and what the install
 * holds. The one place a category's Radio and its automatic choice are turned into an atlas, so every
 * category answers a missing glyph and an unloadable face by the same rules.
 *
 * <p>The automatic choice reads the text, never a locale. It keeps the category's preferred face unless
 * that face's installed atlas lacks a character the category draws, and then takes the category's
 * fallback where that face holds every one. So an English build on a vanilla install keeps the preferred
 * face, while the same build on a localised install drawing localised names gets a face that holds their
 * glyphs.
 *
 * <p>A face that will not load is never an answer while another will: the reader states such a face at
 * no line height, and the resolver treats that as absent. A named face the player picked that will not
 * load resolves as the automatic choice would. No path leaves a category without a face: where neither
 * face covers the text the preferred face stays, gaps in the face meant for the job reading no worse
 * than the same gaps in another, and where the preferred face will not load the fallback stands in.
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
     * @param lineHeights   where an installed atlas's line height is read, which says whether the face
     *                      loads at all
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
     * @param offer      where the category's automatic choice may land
     * @param choice     what the player picked for it
     * @param probeTexts the text the category draws, which the automatic choice needs covered
     * @return the face to draw in
     */
    public StarsectorFont resolveFont(FaceOffer offer, FaceChoice choice, Collection<String> probeTexts) {

        return choice.selectByCase(
            () -> resolveAutomaticFont(offer, probeTexts),
            namedFont -> isFontLoadable(namedFont)
                ? namedFont
                : resolveAutomaticFont(offer, probeTexts));
    }

    // The automatic choice: the preferred face where it loads and covers the text, the fallback where
    // that one does, and otherwise whichever of the two still draws, the preferred face first.
    private StarsectorFont resolveAutomaticFont(FaceOffer offer, Collection<String> probeTexts) {

        var preferredFont = offer.preferredFont();
        if (isFontDrawingEveryText(preferredFont, probeTexts)) {
            return preferredFont;
        }
        var coveringFallbackFont = offer.fallbackFont()
            .filter(fallbackFont -> isFontDrawingEveryText(fallbackFont, probeTexts));
        if (coveringFallbackFont.isPresent()) {
            return coveringFallbackFont.get();
        }
        if (isFontLoadable(preferredFont)) {
            return preferredFont;
        }
        return offer.fallbackFont()
            .filter(this::isFontLoadable)
            .orElse(preferredFont);
    }

    // Whether a face loads and draws every character of every text the category draws.
    private boolean isFontDrawingEveryText(StarsectorFont font, Collection<String> probeTexts) {
        return isFontLoadable(font)
            && probeTexts.stream().allMatch(probeText -> glyphCoverage.coversText(font, probeText));
    }

    private boolean isFontLoadable(StarsectorFont font) {
        return lineHeights.readLineHeight(font) > NO_LINE_HEIGHT;
    }
}
