package kmlib.starsector.ui.font;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Settles which face a text draws in, from the face its caller asks for and what the install holds. The
 * one place a missing glyph and an unloadable face are answered, so every text KM draws falls back by the
 * same rules.
 *
 * <p>The face asked for is kept where its installed atlas loads and draws every character of the text.
 * Where it does not, the resolver walks down the face's family - each cut naming the
 * {@linkplain StarsectorFont#resolveLowerResolutionFont() next cut below it} - and takes the first that
 * does, a smaller cut of the same design being the nearest face to the one asked for. Past the family's
 * smallest cut comes the game's own default face, and where nothing before it draws the text, the default
 * is answered regardless: it is what vanilla text on the same install is drawn in, so it is the last
 * face any text can have.
 *
 * <p>It reads the text, never a locale. An English build on a localised install keeps the face it asks
 * for wherever that face holds the text; the same build drawing localised faction names in an atlas no
 * localisation replaces walks down to a cut that holds them.
 *
 * <p>The probe reads every glyph of every text asked about, so a caller settles a face once for the text
 * it will draw and holds the answer rather than resolving per frame.
 */
public final class FaceResolver {

    // The line height the reader states for a face that will not load, and so the floor a loadable face
    // stands above.
    private static final double NO_LINE_HEIGHT = 0d;

    private final StarsectorFont defaultFont;
    private final GlyphCoverageReader glyphCoverage;
    private final FaceLineHeightReader lineHeights;

    /**
     * @param lineHeights   where an installed atlas's line height is read, which says whether the face
     *                      loads at all
     * @param glyphCoverage where an installed atlas's coverage of a text is read
     * @param defaultFont   the face every walk ends at - the game's own default on a running game
     */
    public FaceResolver(
            FaceLineHeightReader lineHeights,
            GlyphCoverageReader glyphCoverage,
            StarsectorFont defaultFont) {

        this.lineHeights = lineHeights;
        this.glyphCoverage = glyphCoverage;
        this.defaultFont = defaultFont;
    }

    /**
     * @return a resolver reading the running game's installed atlases through LazyLib, ending every walk
     *         at the face the game's settings name as its default
     */
    public static FaceResolver createInstalledFaceResolver() {

        return new FaceResolver(
            LazyFontLineHeightReader::readLineHeight,
            LazyFontGlyphCoverageReader::coversText,
            GameDefaultFontReader.readDefaultFont());
    }

    /**
     * The face a text draws in.
     *
     * @param requestedFont the face the caller would draw in, installs permitting
     * @param probeTexts    the text it draws, which the face has to hold
     * @return the first face down the walk that loads and holds the text, or the default face
     */
    public StarsectorFont resolveFont(StarsectorFont requestedFont, Collection<String> probeTexts) {

        for (var candidateFont : listFallbackWalk(requestedFont)) {
            if (isFontDrawingEveryText(candidateFont, probeTexts)) {
                return candidateFont;
            }
        }
        return defaultFont;
    }

    /**
     * The faces a text asking for {@code requestedFont} is tried in, in order: the face itself, each
     * smaller cut of its family, and the default face last, once.
     *
     * @param requestedFont the face asked for
     * @return the walk
     */
    public List<StarsectorFont> listFallbackWalk(StarsectorFont requestedFont) {

        var walk = new ArrayList<StarsectorFont>();

        for (var font = requestedFont; font != null; font = font.resolveLowerResolutionFont().orElse(null)) {
            walk.add(font);
        }
        if (!walk.contains(defaultFont)) {
            walk.add(defaultFont);
        }
        return List.copyOf(walk);
    }

    // Whether a face loads and draws every character of every text asked about.
    private boolean isFontDrawingEveryText(StarsectorFont font, Collection<String> probeTexts) {
        return lineHeights.readLineHeight(font) > NO_LINE_HEIGHT
            && probeTexts.stream().allMatch(probeText -> glyphCoverage.coversText(font, probeText));
    }
}
