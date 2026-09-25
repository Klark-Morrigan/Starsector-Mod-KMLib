package kmlib.testfixtures.localisation;

import kmlib.testfixtures.starsector.settings.LunaSettingsTable;
import kmlib.testfixtures.starsector.strings.ShippedStrings;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * One locale's directory under {@code localisation/}: every file the manifest maps, held whole, plus the
 * locale's launcher fragment.
 *
 * <p>Each file is read the way a mod's shipped copy of it is read, so a bundle file is never parsed a
 * second way: the strings file through {@link ShippedStrings}, and the settings table through
 * {@link LunaSettingsTable}.
 */
public final class LocaleBundle {

    // A bundle file is named bare: it sits directly in its locale's directory, and a separator or a
    // parent step would let one bundle reach into another's. The first character excludes the dot,
    // which is what keeps out "..".
    private static final Pattern BUNDLE_FILE_NAME = Pattern.compile("[A-Za-z0-9_-][A-Za-z0-9_.-]*");

    // The name every bundle gives its launcher fragment, after the file it is merged into.
    static final String MOD_INFO_FILE_NAME = "mod_info.json";

    // The name every bundle gives its settings table, and the name LunaLib gives it too.
    static final String SETTINGS_FILE_NAME = LunaSettingsTable.SETTINGS_CSV.getFileName().toString();

    // The name every bundle gives its strings file, and the name the game gives it too.
    static final String STRINGS_FILE_NAME = "strings.json";

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
     * Opens a reading of this locale's settings table.
     *
     * @param fieldIdPrefix what every one of the mod's field IDs starts with
     * @return the reading
     */
    public LunaSettingsTable openSettingsTable(String fieldIdPrefix) {
        return new LunaSettingsTable(resolveBundleFile(SETTINGS_FILE_NAME), fieldIdPrefix);
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

        requireBundleFileName(bundleFileName);

        return directory.resolve(bundleFileName);
    }

    /**
     * Refuses a name that is not a bare file name, which is the one shape a bundle file may take.
     * Stated here, where bundle files are resolved, and applied wherever one is named.
     *
     * @param bundleFileName the name to check
     */
    static void requireBundleFileName(String bundleFileName) {

        if (!BUNDLE_FILE_NAME.matcher(bundleFileName).matches()) {

            throw new IllegalArgumentException("\"" + bundleFileName + "\" is not a bare file name");
        }
    }
}
