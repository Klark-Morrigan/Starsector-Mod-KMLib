package kmlib.testfixtures.localisation;

import kmlib.testfixtures.starsector.strings.ShippedStrings;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * One locale's directory under {@code l10n/}: every file the manifest maps, held whole, plus the
 * locale's launcher fragment.
 *
 * <p>Each file is read the way a mod's shipped copy of it is read, so a bundle file is never parsed a
 * second way: the strings file through {@link ShippedStrings}, and a settings table through
 * {@link kmlib.testfixtures.starsector.settings.LunaSettingsTable} over {@link #resolveBundleFile}.
 */
public final class LocaleBundle {

    /** The name every bundle gives its strings file, and the name the game gives it too. */
    public static final String STRINGS_FILE_NAME = "strings.json";

    /** The name every bundle gives its launcher fragment, after the file it is merged into. */
    public static final String MOD_INFO_FILE_NAME = "mod_info.json";

    private final Path directory;
    private final DeclaredLocale locale;

    /**
     * Opens the bundle of one declared locale.
     *
     * @param directory the bundle's directory
     * @param locale    the locale it holds
     */
    LocaleBundle(Path directory, DeclaredLocale locale) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    /**
     * The locale this bundle holds.
     *
     * @return its declaration
     */
    public DeclaredLocale getLocale() {
        return locale;
    }

    /**
     * The launcher fields this locale translates. Absent file, absent fields: a locale translating
     * none of them needs no fragment.
     *
     * @return the fragment
     */
    public ModInfoFragment readModInfoFragment() {
        return ModInfoFragment.readFragment(resolveBundleFile(MOD_INFO_FILE_NAME));
    }

    /**
     * The strings file's wording, by category and then by key, both sorted.
     *
     * @return key to wording, per category
     */
    public Map<String, Map<String, String>> readStrings() {
        return ShippedStrings.readStringsByCategory(resolveBundleFile(STRINGS_FILE_NAME));
    }

    /**
     * Where one of this bundle's files is, whether or not it exists.
     *
     * @param bundleFileName the file's bare name, as the manifest keys it
     * @return its path
     */
    public Path resolveBundleFile(String bundleFileName) {

        var bundleFile = directory.resolve(bundleFileName).normalize();

        // A name with a separator or a parent step would read another bundle's file, or none of them.
        if (!directory.normalize().equals(bundleFile.getParent())) {

            throw new IllegalArgumentException(
                "\"" + bundleFileName + "\" is not a bare file name inside " + directory);
        }
        return bundleFile;
    }
}
