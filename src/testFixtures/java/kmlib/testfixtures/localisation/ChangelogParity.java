package kmlib.testfixtures.localisation;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Every translated locale's changelog against the mod's own, by outline.
 */
final class ChangelogParity {

    private final LocalisationDirectory directory;

    ChangelogParity(LocalisationDirectory directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    List<String> findChangelogMismatches() {

        var referenceOutline = directory.readChangelogOutline();

        if (referenceOutline.isEmpty()) {
            return List.of();
        }
        var findings = new ArrayList<String>();
        var manifest = directory.readManifest();
        var defaultLocale = manifest.getDefaultLocale();
        var bundleDirectoryNames = directory.listBundleDirectoryNames();

        for (var locale : manifest.declaredLocalesByTag().values()) {

            // The default's changelog is the root one. A locale with no directory has its own finding.
            if (locale.equals(defaultLocale) || !bundleDirectoryNames.contains(locale.localeTag())) {
                continue;
            }
            var bundle = directory.openBundle(locale);
            var filePrefix = locale.localeTag() + ": " + LocalisationDirectory.CHANGELOG_FILE_NAME;

            if (!Files.isRegularFile(bundle.resolveBundleFile(LocalisationDirectory.CHANGELOG_FILE_NAME))) {

                findings.add(locale.localeTag() + ": holds no " + LocalisationDirectory.CHANGELOG_FILE_NAME
                    + ", the translation of the mod's own");
                continue;
            }
            bundle.readChangelogOutline()
                .describeDifferencesFrom(referenceOutline.get(), defaultLocale.localeTag())
                .forEach(difference -> findings.add(filePrefix + " " + difference));
        }
        return findings;
    }
}
