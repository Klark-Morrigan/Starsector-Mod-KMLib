package kmlib.starsector.ui.font.installed;

import kmlib.starsector.ui.font.AtlasSmoothing;
import kmlib.starsector.ui.font.FontAtlas;
import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.testfixtures.starsector.json.ShippedJson;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Optional;

/**
 * Reads an install's {@link InstalledFonts} off its files: the localisation marker, the game's settings,
 * and the descriptor of every face KM text may draw in.
 *
 * <p>The edition is the {@code branch} a core localisation records in
 * {@code starsector-core/localization_version.json}, and the pack version that file's {@code version}.
 * An install with no such file carries the game's own atlases, which is the
 * {@value InstalledFonts#VANILLA_EDITION} edition, and has no pack version.
 *
 * <p>A declared default the enum does not name is read where the install carries it under
 * {@code starsector-core}. One a mod ships sits in that mod's folder, which is the game's to merge and not
 * this reading's to find, so it reads as a face that will not load - which every walk passes over.
 */
final class InstalledFontsReader {

    // Where a core localisation records itself, under the install root, and the two fields read from it.
    private static final String MARKER_PATH = "starsector-core/localization_version.json";
    private static final String MARKER_EDITION_KEY = "branch";
    private static final String MARKER_VERSION_KEY = "version";

    // Where a face's descriptor path resolves from under the install root.
    private static final String CORE_DIRECTORY = "starsector-core";

    // The game's own settings under the install root, and the key it names its default face under.
    private static final String SETTINGS_PATH = "starsector-core/data/config/settings.json";
    private static final String DEFAULT_FONT_KEY = "defaultFont";

    // Reads only; never instantiated.
    private InstalledFontsReader() {
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
    static InstalledFonts readInstall(Path starsectorRoot) {

        var markerFile = starsectorRoot.resolve(MARKER_PATH);
        var edition = InstalledFonts.VANILLA_EDITION;
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
            readCoreFace(starsectorRoot, font.resolvePath()).ifPresent(face -> faceByFont.put(font, face));
        }
        var defaultAtlas = readDefaultAtlas(starsectorRoot);

        return new InstalledFonts(
            edition,
            packVersion,
            defaultAtlas,
            faceByFont,
            defaultAtlas instanceof StarsectorFont
                ? Optional.empty()
                : readCoreFace(starsectorRoot, defaultAtlas.resolvePath()));
    }

    // The default face the install's settings declare, read through the game's own parser and mapped by the
    // rule the running game's setting is, so a check settles faces against the same walk. A declared file's
    // smoothing is its descriptor's, and antialiased where the install does not carry the file - the running
    // game's answer too, the file then loading for neither.
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

        return GameDefaultFontReader.resolveDefaultFont(
            defaultFontPath,
            declaredPath -> readCoreFace(starsectorRoot, declaredPath)
                .map(InstalledFace::smoothing)
                .orElse(AtlasSmoothing.SMOOTHED));
    }

    // What the install states for the descriptor at a game path, resolved under starsector-core as the
    // game resolves it, where the install carries one.
    private static Optional<InstalledFace> readCoreFace(Path starsectorRoot, String descriptorPath) {

        var descriptorFile = starsectorRoot.resolve(CORE_DIRECTORY).resolve(descriptorPath);

        return Files.isRegularFile(descriptorFile)
            ? Optional.of(InstalledFace.readDescriptor(descriptorFile))
            : Optional.empty();
    }
}
