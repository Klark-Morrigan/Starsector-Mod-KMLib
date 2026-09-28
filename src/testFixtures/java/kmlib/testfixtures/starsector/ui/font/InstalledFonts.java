package kmlib.testfixtures.starsector.ui.font;

import kmlib.starsector.ui.font.FaceLineHeightReader;
import kmlib.starsector.ui.font.GlyphCoverageReader;
import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.testfixtures.starsector.json.ShippedJson;

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
 * game on that install would, so a face can be settled against a real edition's atlases with no game
 * running. Its coverage is the declared glyph IDs: a character is covered when its code point is one of
 * them, whitespace never being asked about.
 *
 * @param edition      which edition of the game's fonts the install carries
 * @param packVersion  the core localisation's version, absent for the game's own atlases
 * @param faceByFont   what the install states for each face it carries
 */
public record InstalledFonts(
    String edition,
    Optional<String> packVersion,
    Map<StarsectorFont, InstalledFace> faceByFont) {

    /** The edition of an install carrying the game's own atlases. */
    public static final String VANILLA_EDITION = "vanilla";

    // Where a core localisation records itself, under the install root, and the two fields read from it.
    private static final String MARKER_PATH = "starsector-core/localization_version.json";
    private static final String MARKER_EDITION_KEY = "branch";
    private static final String MARKER_VERSION_KEY = "version";

    // Where a face's descriptor path resolves from under the install root.
    private static final String CORE_DIRECTORY = "starsector-core";

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
     * Reads the install at {@code starsectorRoot}: its edition, and every face KM text may draw in that it
     * carries a descriptor for. A face it lacks is left out rather than read as empty, which is how a
     * missing atlas is told from one declaring no glyphs.
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
        return new InstalledFonts(edition, packVersion, faceByFont);
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
        return font -> Optional.ofNullable(faceByFont.get(font))
            .map(InstalledFace::lineHeight)
            .orElse(NO_LINE_HEIGHT);
    }

    /**
     * @return a reader answering whether this install's atlas for a face declares every character of a
     *         text, and false for a face it does not carry
     */
    public GlyphCoverageReader createGlyphCoverageReader() {
        return (font, text) -> Optional.ofNullable(faceByFont.get(font))
            .map(face -> text.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .allMatch(codePoint -> face.glyphIds().containsId(codePoint)))
            .orElse(false);
    }
}
