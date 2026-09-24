package kmlib.testfixtures.localisation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * A mod's {@code l10n/} directory: the manifest at its root and one bundle directory per locale.
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
    public static final Path L10N_DIRECTORY = Path.of("l10n");

    /** The manifest's name inside that directory. */
    public static final String MANIFEST_FILE_NAME = "manifest.json";

    private final Path directory;

    /**
     * Opens a reading of one mod's localisation directory.
     *
     * @param directory the directory, relative to the module the suite runs in;
     *                  {@link #L10N_DIRECTORY} is where these conventions put it
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
}
