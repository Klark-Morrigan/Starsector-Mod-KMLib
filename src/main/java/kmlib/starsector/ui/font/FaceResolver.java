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
 * Where it does not, the resolver walks on and takes the first face that does:
 *
 * <ol>
 *   <li>the face asked for;</li>
 *   <li>each smaller cut of its family, each cut naming the
 *       {@linkplain StarsectorFont#resolveLowerResolutionFont() next cut below it} - a smaller cut of the
 *       same design being the nearest face to the one asked for;</li>
 *   <li>the face the game's own settings declare as its default, whatever file that is - which a language
 *       pack may point at an atlas holding its script;</li>
 *   <li>{@link #LAST_RESORT_FONT}, named here.</li>
 * </ol>
 *
 * <p>Each face is tried once. Where nothing on the walk draws the text, the last resort is answered: a face
 * KMLib names itself, so no setting can make a broken or missing file the last word.
 *
 * <p>It reads the text, never a locale. An English build on a localised install keeps the face it asks
 * for wherever that face holds the text; the same build drawing localised faction names in an atlas no
 * localisation replaces walks down to a cut that holds them.
 *
 * <p>The probe reads every glyph of every text asked about, so a caller settles a face once for the text
 * it will draw and holds the answer rather than resolving per frame.
 */
public final class FaceResolver {

    /**
     * The last face any text can have, answered where nothing on its walk draws it: the face vanilla's own
     * settings name for paragraph text, and one every install carries.
     */
    public static final StarsectorFont LAST_RESORT_FONT = StarsectorFont.VANILLA_INSIGNIA_15;

    private final FontAtlas declaredDefaultAtlas;
    private final GlyphCoverageReader glyphCoverage;
    private final FaceLineHeightReader lineHeights;

    /**
     * @param lineHeights          where an installed atlas's line height is read, which says whether the
     *                             face loads at all
     * @param glyphCoverage        where an installed atlas's coverage of a text is read
     * @param declaredDefaultAtlas the face the game's settings declare as its default, tried after a face's
     *                             family and before the last resort
     */
    public FaceResolver(
            FaceLineHeightReader lineHeights,
            GlyphCoverageReader glyphCoverage,
            FontAtlas declaredDefaultAtlas) {

        this.lineHeights = lineHeights;
        this.glyphCoverage = glyphCoverage;
        this.declaredDefaultAtlas = declaredDefaultAtlas;
    }

    /**
     * The face a text draws in.
     *
     * @param requestedFont the face the caller would draw in, installs permitting
     * @param probeTexts    the text it draws, which the face has to hold
     * @return the first face on the walk that loads and holds the text, or the last resort
     */
    public FontAtlas resolveFont(StarsectorFont requestedFont, Collection<String> probeTexts) {

        for (var candidateAtlas : listFallbackWalk(requestedFont)) {
            if (isAtlasDrawingEveryText(candidateAtlas, probeTexts)) {
                return candidateAtlas;
            }
        }
        return LAST_RESORT_FONT;
    }

    /**
     * The faces a text asking for {@code requestedFont} is tried in, in order: the face itself, each
     * smaller cut of its family, the game's declared default, and the last resort - each once.
     *
     * @param requestedFont the face asked for
     * @return the walk
     */
    public List<FontAtlas> listFallbackWalk(StarsectorFont requestedFont) {

        var walk = new ArrayList<FontAtlas>();

        for (var font = requestedFont; font != null; font = font.resolveLowerResolutionFont().orElse(null)) {
            walk.add(font);
        }
        addIfAbsent(walk, declaredDefaultAtlas);
        addIfAbsent(walk, LAST_RESORT_FONT);

        return List.copyOf(walk);
    }

    // Appends a face the walk has not reached already, so no face is tried twice.
    private static void addIfAbsent(List<FontAtlas> walk, FontAtlas atlas) {
        if (!walk.contains(atlas)) {
            walk.add(atlas);
        }
    }

    // Whether a face loads and draws every character of every text asked about.
    private boolean isAtlasDrawingEveryText(FontAtlas atlas, Collection<String> probeTexts) {
        return FaceLineHeightReader.isFaceLoadable(lineHeights.readLineHeight(atlas))
            &&probeTexts.stream().allMatch(probeText -> glyphCoverage.coversText(atlas, probeText));
    }
}
