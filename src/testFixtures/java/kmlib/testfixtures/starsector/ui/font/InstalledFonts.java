package kmlib.testfixtures.starsector.ui.font;

import kmlib.starsector.ui.font.AtlasSmoothing;
import kmlib.starsector.ui.font.FaceLineHeightReader;
import kmlib.starsector.ui.font.FaceResolver;
import kmlib.starsector.ui.font.FontAtlas;
import kmlib.starsector.ui.font.GameDefaultFontReader;
import kmlib.starsector.ui.font.GlyphCoverageReader;
import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.testfixtures.starsector.json.ShippedJson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * An install's font atlases, read off its descriptors: which edition of the game's fonts it carries, and
 * what its descriptor states for every face KM text may draw in. A core localisation replaces several
 * atlases under the same basenames and each of its editions replaces them differently, so the checks on
 * what KM text needs of an atlas are run against every install a machine holds.
 *
 * <p>The edition is the {@code branch} a core localisation records in
 * {@code starsector-core/localization_version.json}, and the pack version that file's {@code version}.
 * An install with no such file carries the game's own atlases, which is the {@value #VANILLA_EDITION}
 * edition, and has no pack version. Both only name the install in a failure.
 *
 * <p>A reading answers {@link FaceLineHeightReader} and {@link GlyphCoverageReader} the way the running
 * game on that install would, and names the default face its settings declare, so a face can be settled
 * against a real edition's atlases with no game running. Its coverage is the declared glyph IDs: a
 * character is covered when its code point is one of them, whitespace never being asked about.
 *
 * <p>A declared default the enum does not name is read where the install carries it under
 * {@code starsector-core}. One a mod ships sits in that mod's folder, which is the game's to merge and not
 * this reading's to find, so it reads as a face that will not load - which every walk passes over.
 *
 * @param edition              which edition of the game's fonts the install carries
 * @param packVersion          the core localisation's version, absent for the game's own atlases
 * @param defaultAtlas         the face the install's settings declare as the game's default, by the rule the
 *                             running game's is read by
 * @param faceByFont           what the install states for each face the enum names that it carries
 * @param declaredDefaultFace  what the install states for a declared default the enum does not name, where
 *                             it carries one
 */
public record InstalledFonts(
    String edition,
    Optional<String> packVersion,
    FontAtlas defaultAtlas,
    Map<StarsectorFont, InstalledFace> faceByFont,
    Optional<InstalledFace> declaredDefaultFace) {

    /** The edition of an install carrying the game's own atlases. */
    public static final String VANILLA_EDITION = "vanilla";

    // Where a core localisation records itself, under the install root, and the two fields read from it.
    private static final String MARKER_PATH = "starsector-core/localization_version.json";
    private static final String MARKER_EDITION_KEY = "branch";
    private static final String MARKER_VERSION_KEY = "version";

    // Where a face's descriptor path resolves from under the install root.
    private static final String CORE_DIRECTORY = "starsector-core";

    // The game's own settings under the install root, and the key it names its default face under.
    private static final String SETTINGS_PATH = "starsector-core/data/config/settings.json";
    private static final String DEFAULT_FONT_KEY = "defaultFont";

    // What the readers answer for a face the install does not carry: the ports' own answers for a face
    // that will not load.
    private static final double NO_LINE_HEIGHT = 0d;

    /**
     * Copies the faces into enum order, the order a walk over them takes.
     */
    public InstalledFonts {

        var orderedFaceByFont = new EnumMap<StarsectorFont, InstalledFace>(StarsectorFont.class);

        orderedFaceByFont.putAll(faceByFont);

        faceByFont = Collections.unmodifiableMap(orderedFaceByFont);
    }

    /**
     * Reads the install at {@code starsectorRoot}: its edition, the default face its settings name, and
     * every face KM text may draw in that it carries a descriptor for. A face it lacks is left out rather
     * than read as empty, which is how a missing atlas is told from one declaring no glyphs. Settings
     * naming no default - or no settings file at all - read as vanilla's default, as the running game's
     * would.
     *
     * @param starsectorRoot the install's root, the folder holding {@code starsector-core}
     * @return the reading
     */
    public static InstalledFonts readInstall(Path starsectorRoot) {

        var markerFile = starsectorRoot.resolve(MARKER_PATH);
        var edition = VANILLA_EDITION;
        Optional<String> packVersion = Optional.empty();

        if (Files.isRegularFile(markerFile)) {
            var marker = ShippedJson.readObjectFile(markerFile);
            var location = markerFile.toString();

            edition = ShippedJson.requireString(
                marker.get(MARKER_EDITION_KEY),
                ShippedJson.locateMember(location, MARKER_EDITION_KEY));
            packVersion = Optional.of(ShippedJson.requireString(
                marker.get(MARKER_VERSION_KEY),
                ShippedJson.locateMember(location, MARKER_VERSION_KEY)));
        }
        var faceByFont = new EnumMap<StarsectorFont, InstalledFace>(StarsectorFont.class);

        for (var font : StarsectorFont.values()) {
            var descriptorFile = starsectorRoot.resolve(CORE_DIRECTORY).resolve(font.resolvePath());
            if (Files.isRegularFile(descriptorFile)) {
                faceByFont.put(font, InstalledFace.readDescriptor(descriptorFile));
            }
        }
        var defaultAtlas = readDefaultAtlas(starsectorRoot);

        return new InstalledFonts(
            edition,
            packVersion,
            defaultAtlas,
            faceByFont,
            readDeclaredDefaultFace(starsectorRoot, defaultAtlas));
    }

    /**
     * @return a resolver settling faces as the running game on this install would: over its atlases, trying
     *         the default face its settings declare before the last resort
     */
    public FaceResolver createFaceResolver() {
        return new FaceResolver(createLineHeightReader(), createGlyphCoverageReader(), defaultAtlas);
    }

    /**
     * @return the edition and, where there is one, the pack version - how a failure names the install
     */
    public String describeEdition() {
        return edition + packVersion.map(version -> " " + version).orElse("");
    }

    /**
     * @return a reader answering each face's line height as this install states it, and zero for a face
     *         it does not carry
     */
    public FaceLineHeightReader createLineHeightReader() {
        return atlas -> findInstalledFace(atlas)
            .map(InstalledFace::lineHeight)
            .orElse(NO_LINE_HEIGHT);
    }

    /**
     * @return a reader answering whether this install's atlas for a face declares every character of a
     *         text, and false for a face it does not carry
     */
    public GlyphCoverageReader createGlyphCoverageReader() {
        return (atlas, text) -> findInstalledFace(atlas)
            .map(face -> text.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .allMatch(codePoint -> face.glyphIds().containsId(codePoint)))
            .orElse(false);
    }

    // The default face the install's settings declare, read through the game's own parser and mapped by the
    // rule the running game's setting is, so a check settles faces against the same walk. A declared file's
    // smoothing is read off its descriptor in the install, as the running game's is off the one it opens.
    private static FontAtlas readDefaultAtlas(Path starsectorRoot) {

        var settingsFile = starsectorRoot.resolve(SETTINGS_PATH);
        var defaultFontValue = Files.isRegularFile(settingsFile)
            ? ShippedJson.readObjectFile(settingsFile).get(DEFAULT_FONT_KEY)
            : null;
        var defaultFontPath = defaultFontValue == null
            ? null
            : ShippedJson.requireString(
                defaultFontValue,
                ShippedJson.locateMember(settingsFile.toString(), DEFAULT_FONT_KEY));
        var coreDirectory = starsectorRoot.resolve(CORE_DIRECTORY);

        return GameDefaultFontReader.resolveDefaultFont(
            defaultFontPath,
            declaredPath -> readInstalledSmoothing(coreDirectory.resolve(declaredPath)));
    }

    // What the install states for a declared default the enum does not name, where it carries one.
    private static Optional<InstalledFace> readDeclaredDefaultFace(Path starsectorRoot, FontAtlas defaultAtlas) {

        if (defaultAtlas instanceof StarsectorFont) {
            return Optional.empty();
        }
        var descriptorFile = starsectorRoot.resolve(CORE_DIRECTORY).resolve(defaultAtlas.resolvePath());

        return Files.isRegularFile(descriptorFile)
            ? Optional.of(InstalledFace.readDescriptor(descriptorFile))
            : Optional.empty();
    }

    // A declared file's smoothing as its descriptor's first line states it, or antialiased where the install
    // does not carry the file - the running game's answer too, the file then loading for neither.
    private static AtlasSmoothing readInstalledSmoothing(Path descriptorFile) {

        if (!Files.isRegularFile(descriptorFile)) {
            return AtlasSmoothing.SMOOTHED;
        }
        try (BufferedReader descriptor = Files.newBufferedReader(descriptorFile, StandardCharsets.ISO_8859_1)) {
            return AtlasSmoothing.resolveFromInfoLine(descriptor.readLine());

        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read " + descriptorFile, exception);
        }
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
