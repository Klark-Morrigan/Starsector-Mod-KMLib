package kmlib.testfixtures.localisation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A mod's {@code localisation/} directory: the manifest at its root and one bundle directory per locale.
 *
 * <p>Offered to every mod on these conventions because the layout is the tooling's rather than any one
 * mod's. What a mod translates is its own business, and none of it is decided here - this reads, and
 * the suite above judges.
 *
 * <p>Bundles are opened by declaration rather than by name, so only a locale the manifest declares can
 * be read as one. A directory the manifest never names is still listed, since a bundle nobody declared
 * is exactly what a check over the two has to find.
 */
public final class LocalisationDirectory {

    /** Where a mod on these conventions keeps its locale bundles, relative to its root. */
    public static final Path LOCALISATION_DIRECTORY = Path.of("localisation");

    /** The manifest's name inside that directory. */
    public static final String MANIFEST_FILE_NAME = "manifest.json";

    /** The launcher file every locale's fragment is merged over, beside the directory at the mod root. */
    public static final String MOD_INFO_BASE_FILE_NAME = "mod_info.base.json";

    private final Path directory;

    /**
     * Opens a reading of one mod's localisation directory.
     *
     * @param directory the directory, relative to the module the suite runs in;
     *                  {@link #LOCALISATION_DIRECTORY} is where these conventions put it
     */
    public LocalisationDirectory(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    /**
     * Every directory standing beside the manifest, declared or not.
     *
     * @return their names, sorted
     */
    public List<String> listBundleDirectoryNames() {

        try (var entries = Files.list(directory)) {

            return entries
                .filter(Files::isDirectory)
                .map(entry -> entry.getFileName().toString())
                .sorted()
                .toList();

        } catch (IOException ioException) {

            // Surfaced rather than swallowed: an unreadable directory means the reading is looking in
            // the wrong place, not that the mod ships no locales.
            throw new UncheckedIOException(
                "Could not list " + directory.toAbsolutePath(),
                ioException);
        }
    }

    /**
     * Opens the bundle holding one declared locale.
     *
     * @param locale the locale, as the manifest declares it
     * @return its bundle, which need not exist on disk yet
     */
    public LocaleBundle openBundle(DeclaredLocale locale) {
        return new LocaleBundle(directory.resolve(locale.localeTag()), locale);
    }

    /**
     * Reads and checks the manifest.
     *
     * @return what it declares
     */
    public LocaleManifest readManifest() {
        return LocaleManifest.readManifest(directory.resolve(MANIFEST_FILE_NAME));
    }

    /**
     * Reads the launcher base beside this directory. Absent for a mod still committing its launcher file
     * whole, which then has nothing for a fragment to be merged over.
     *
     * @return what a fragment can reach of it, or nothing where the mod commits none
     */
    public Optional<ModInfoBase> readModInfoBase() {

        var baseFile = directory.resolveSibling(MOD_INFO_BASE_FILE_NAME);

        return Files.exists(baseFile)
            ? Optional.of(ModInfoBase.readBase(baseFile))
            : Optional.empty();
    }
}
