package kmlib.testfixtures.localisation;

import kmlib.testfixtures.starsector.json.ShippedJson;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static kmlib.testfixtures.starsector.json.ShippedJson.locateMember;
import static kmlib.testfixtures.starsector.json.ShippedJson.requireObject;
import static kmlib.testfixtures.starsector.json.ShippedJson.requireOnlyKeys;
import static kmlib.testfixtures.starsector.json.ShippedJson.requireString;

/**
 * A mod's {@code l10n/manifest.json}: which locales exist, which is the default, and which files a
 * bundle holds and where each lands in the mod.
 *
 * <p>The single source of truth for all three. The build task that materialises a locale and the
 * suites that compare bundles both read this file, so the file map has one definition rather than a
 * Groovy copy and a Java copy that agree until one changes.
 *
 * <p>Read with the same parser as every other JSON file a mod ships, so one set of syntax rules covers
 * all of them. Every key the manifest may carry is known here and any other is refused, so a
 * misspelling - the American {@code coreLocalization} above all - fails the read rather than reading
 * as an absent field.
 *
 * <p>Both maps are sorted by key. The parser keeps no member order, so none is implied by the file.
 *
 * @param defaultLocaleTag          the locale a build falls back to when none is named
 * @param dataPathsByBundleFileName each file a bundle holds, keyed by its name inside the bundle,
 *                                  mapped to where materialisation copies it, relative to the mod root
 * @param declaredLocalesByTag      every locale the mod ships, keyed by tag
 */
public record LocaleManifest(
    String defaultLocaleTag,
    Map<String, Path> dataPathsByBundleFileName,
    Map<String, DeclaredLocale> declaredLocalesByTag) {

    private static final String DEFAULT_LOCALE_KEY = "defaultLocale";
    private static final String FILES_KEY = "files";
    private static final String LOCALES_KEY = "locales";
    private static final Set<String> MANIFEST_KEYS = Set.of(DEFAULT_LOCALE_KEY, FILES_KEY, LOCALES_KEY);

    private static final String DISPLAY_NAME_KEY = "displayName";
    private static final String CORE_LOCALISATION_KEY = "coreLocalisation";
    private static final Set<String> LOCALE_KEYS = Set.of(DISPLAY_NAME_KEY, CORE_LOCALISATION_KEY);

    // A bundle file is named bare: it sits directly in its locale's directory, and a separator or a
    // parent step would let one bundle reach into another's.
    private static final Pattern BUNDLE_FILE_NAME = Pattern.compile("[A-Za-z0-9_-][A-Za-z0-9_.-]*");

    // Materialisation copies onto these paths, so a path that climbs out of the mod root or names a
    // drive is a write outside the repository. Forward slashes only, the manifest being read on every
    // platform and a backslash being a filename character on most of them.
    private static final String PARENT_SEGMENT = "..";
    private static final String BACKSLASH = "\\";

    /**
     * Holds the manifest to the invariants the tooling depends on.
     *
     * @param defaultLocaleTag          see the record
     * @param dataPathsByBundleFileName see the record
     * @param declaredLocalesByTag      see the record
     */
    public LocaleManifest {

        Objects.requireNonNull(defaultLocaleTag, "defaultLocaleTag");
        Objects.requireNonNull(dataPathsByBundleFileName, "dataPathsByBundleFileName");
        Objects.requireNonNull(declaredLocalesByTag, "declaredLocalesByTag");

        if (dataPathsByBundleFileName.isEmpty()) {

            throw new IllegalArgumentException("The manifest maps no bundle files");
        }
        if (declaredLocalesByTag.isEmpty()) {

            throw new IllegalArgumentException("The manifest declares no locales");
        }
        if (!declaredLocalesByTag.containsKey(defaultLocaleTag)) {

            throw new IllegalArgumentException(
                "Default locale " + defaultLocaleTag
                    + " is not among the declared locales " + declaredLocalesByTag.keySet());
        }
        declaredLocalesByTag.forEach(LocaleManifest::requireKeyedByOwnTag);
        requireDistinctDataPaths(dataPathsByBundleFileName);

        // Sorted, so every reading walks the same order however the maps were built.
        dataPathsByBundleFileName = Collections.unmodifiableMap(new TreeMap<>(dataPathsByBundleFileName));
        declaredLocalesByTag = Collections.unmodifiableMap(new TreeMap<>(declaredLocalesByTag));
    }

    /**
     * Reads and checks a manifest file.
     *
     * @param manifestFile the manifest to read
     * @return the manifest it declares
     */
    public static LocaleManifest readManifest(Path manifestFile) {

        var location = manifestFile.toString();
        var manifest = ShippedJson.readObjectFile(manifestFile);

        requireOnlyKeys(manifest, MANIFEST_KEYS, location);

        var defaultLocaleLocation = locateMember(location, DEFAULT_LOCALE_KEY);
        var filesLocation = locateMember(location, FILES_KEY);
        var localesLocation = locateMember(location, LOCALES_KEY);

        try {
            return new LocaleManifest(
                requireString(manifest.get(DEFAULT_LOCALE_KEY), defaultLocaleLocation),
                readDataPaths(manifest.get(FILES_KEY), filesLocation),
                readDeclaredLocales(manifest.get(LOCALES_KEY), localesLocation));

        } catch (IllegalArgumentException illegalArgunentException) {

            // The records state what is wrong; the file is what a reader has to open to fix it.
            throw new AssertionError(
                location + ": " + illegalArgunentException.getMessage(),
                illegalArgunentException);
        }
    }

    /**
     * The declared locale a build falls back to.
     *
     * @return the default locale's declaration
     */
    public DeclaredLocale getDefaultLocale() {
        return declaredLocalesByTag.get(defaultLocaleTag);
    }

    // Absent means the vanilla install suffices, which is why this is the one optional locale field.
    private static Optional<URI> readCoreLocalisation(Object coreLocalisationValue, String location) {

        if (coreLocalisationValue == null) {
            return Optional.empty();
        }
        var projectText = requireString(coreLocalisationValue, location);

        try {
            return Optional.of(new URI(projectText));

        } catch (URISyntaxException uriSyntaxException) {

            throw new AssertionError(
                location + " is not a URL: " + projectText,
                uriSyntaxException);
        }
    }

    private static Path readDataPath(String dataPathText, String location) {

        var dataPath = Path.of(dataPathText);
        var hasParentSegment = false;

        for (var segment : dataPath) {

            hasParentSegment |= PARENT_SEGMENT.equals(segment.toString());
        }
        if (dataPathText.isBlank() || dataPathText.contains(BACKSLASH) || dataPath.getRoot() != null
                || hasParentSegment) {

            throw new AssertionError(
                location
                    + " maps to \"" + dataPathText
                    + "\", which is not a forward-slashed path inside the mod root");
        }
        return dataPath;
    }

    private static Map<String, Path> readDataPaths(Object filesValue, String location) {

        var dataPathsByBundleFileName = new TreeMap<String, Path>();

        requireObject(filesValue, location)
            .forEach((bundleFileName, dataPathValue) -> {

                var entryLocation = locateMember(location, bundleFileName);

                requireBundleFileName(bundleFileName, entryLocation);

                dataPathsByBundleFileName.put(
                    bundleFileName,
                    readDataPath(requireString(dataPathValue, entryLocation), entryLocation));
            });

        return dataPathsByBundleFileName;
    }

    private static Map<String, DeclaredLocale> readDeclaredLocales(Object localesValue, String location) {

        var declaredLocalesByTag = new TreeMap<String, DeclaredLocale>();

        requireObject(localesValue, location).forEach((localeTag, localeValue) -> {

            var entryLocation = locateMember(location, localeTag);
            var locale = requireObject(localeValue, entryLocation);

            requireOnlyKeys(locale, LOCALE_KEYS, entryLocation);

            var displayName = requireString(
                locale.get(DISPLAY_NAME_KEY),
                locateMember(entryLocation, DISPLAY_NAME_KEY));

            var coreLocalisation = readCoreLocalisation(
                locale.get(CORE_LOCALISATION_KEY),
                locateMember(entryLocation, CORE_LOCALISATION_KEY));

            declaredLocalesByTag.put(localeTag, new DeclaredLocale(localeTag, displayName, coreLocalisation));
        });
        return declaredLocalesByTag;
    }

    // The launcher's own file is merged from a base rather than copied, so it has a reading of its own
    // and is never one of the copied files - mapping it would overwrite the merged result.
    private static void requireBundleFileName(String bundleFileName, String location) {

        if (!BUNDLE_FILE_NAME.matcher(bundleFileName).matches()) {

            throw new AssertionError(location + " is not a bare file name");
        }
        if (LocaleBundle.MOD_INFO_FILE_NAME.equals(bundleFileName)) {

            throw new AssertionError(
                location + " maps the launcher file, which is merged from its base rather than copied");
        }
    }

    // Two bundle files landing on one path would leave whichever was copied last, in an order nothing
    // states.
    private static void requireDistinctDataPaths(Map<String, Path> dataPathsByBundleFileName) {

        var seenDataPaths = new HashSet<Path>();

        dataPathsByBundleFileName.forEach((bundleFileName, dataPath) -> {

            if (!seenDataPaths.add(dataPath.normalize())) {

                throw new IllegalArgumentException(
                    "Bundle file " + bundleFileName
                        + " maps to " + dataPath
                        + ", which another bundle file already maps to");
            }
        });
    }

    private static void requireKeyedByOwnTag(String localeTag, DeclaredLocale locale) {

        if (!localeTag.equals(locale.localeTag())) {

            throw new IllegalArgumentException("Locale " + locale.localeTag()
                + " is keyed under " + localeTag);
        }
    }
}
