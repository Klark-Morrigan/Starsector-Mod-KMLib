package kmlib.starsector.ui.font.installed;

import kmlib.starsector.ui.font.FaceLineHeightReader;
import kmlib.starsector.ui.font.FaceResolver;
import kmlib.starsector.ui.font.FontAtlas;
import kmlib.starsector.ui.font.GlyphCoverageReader;
import kmlib.starsector.ui.font.StarsectorFont;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * An install's font atlases: which edition of the game's fonts it carries, and what its descriptor states
 * for every face KM text may draw in. A core localisation replaces several atlases under the same basenames
 * and each of its editions replaces them differently, so the checks on what KM text needs of an atlas are
 * run against every install a machine holds. {@link InstalledFontsReader} reads one off an install.
 *
 * <p>A reading answers {@link FaceLineHeightReader} and {@link GlyphCoverageReader} the way the running
 * game on that install would, and names the default face its settings declare, so a face can be settled
 * against a real edition's atlases with no game running. Its coverage is the declared glyph IDs: a
 * character is covered when its code point is one of them, whitespace never being asked about.
 *
 * @param edition              which edition of the game's fonts the install carries; names the install in a
 *                             failure
 * @param packVersion          the core localisation's version, absent for the game's own atlases
 * @param language             the language the localisation is in, absent for the game's own atlases
 * @param defaultAtlas         the face the install's settings declare as the game's default, by the rule the
 *                             running game's is read by
 * @param faceByFont           what the install states for each face the enum names that it carries
 * @param declaredDefaultFace  what the install states for a declared default the enum does not name, where
 *                             it carries one
 */
record InstalledFonts(
    String edition,
    Optional<String> packVersion,
    Optional<String> language,
    FontAtlas defaultAtlas,
    Map<StarsectorFont, InstalledFace> faceByFont,
    Optional<InstalledFace> declaredDefaultFace) {

    /** The edition of an install carrying the game's own atlases. */
    static final String VANILLA_EDITION = "vanilla";

    /**
     * Copies the faces into enum order, the order a walk over them takes.
     */
    InstalledFonts {

        var orderedFaceByFont = new EnumMap<StarsectorFont, InstalledFace>(StarsectorFont.class);

        orderedFaceByFont.putAll(faceByFont);

        faceByFont = Collections.unmodifiableMap(orderedFaceByFont);
    }

    /**
     * @return a resolver settling faces as the running game on this install would: over its atlases, walking
     *         a face's family, then the default its settings declare, then the last resort
     */
    FaceResolver createFaceResolver() {
        return new FaceResolver(createLineHeightReader(), createGlyphCoverageReader(), defaultAtlas);
    }

    /**
     * @return the edition and, where there is one, the pack version - how a failure names the install
     */
    String describeEdition() {
        return edition + packVersion.map(version -> " " + version).orElse("");
    }

    /**
     * @return a reader answering each face's line height as this install states it, and
     *         {@link FaceLineHeightReader#NO_LINE_HEIGHT} for a face it does not carry
     */
    FaceLineHeightReader createLineHeightReader() {
        return atlas -> findInstalledFace(atlas)
            .map(InstalledFace::lineHeight)
            .orElse(FaceLineHeightReader.NO_LINE_HEIGHT);
    }

    /**
     * @return a reader answering whether this install's atlas for a face declares every character of a
     *         text, and false for a face it does not carry
     */
    GlyphCoverageReader createGlyphCoverageReader() {
        return (atlas, text) -> findInstalledFace(atlas)
            .map(face -> text.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .allMatch(codePoint -> face.glyphIds().containsId(codePoint)))
            .orElse(false);
    }

    // What the install states for an atlas: a face the enum names by its entry, and the declared default by
    // the descriptor read for it.
    private Optional<InstalledFace> findInstalledFace(FontAtlas atlas) {

        if (atlas instanceof StarsectorFont font) {
            return Optional.ofNullable(faceByFont.get(font));
        }
        return atlas.equals(defaultAtlas)
            ? declaredDefaultFace
            : Optional.empty();
    }
}
