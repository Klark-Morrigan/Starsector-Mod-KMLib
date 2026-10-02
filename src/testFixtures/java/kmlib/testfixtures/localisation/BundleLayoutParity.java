package kmlib.testfixtures.localisation;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The bundle directories against the manifest: a directory for every declared locale, a declaration for
 * every directory, and every mapped file in every bundle.
 */
final class BundleLayoutParity {

    private final LocalisationDirectory directory;

    BundleLayoutParity(LocalisationDirectory directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    List<String> findDeclaredLocalesWithoutBundleDirectory() {

        var bundleDirectoryNames = directory.listBundleDirectoryNames();

        return directory
            .readManifest()
            .declaredLocalesByTag()
            .keySet()
            .stream()
            .filter(localeTag -> !bundleDirectoryNames.contains(localeTag))
            .map(localeTag -> localeTag + ": declared by the manifest, but has no bundle directory")
            .toList();
    }

    List<String> findMissingBundleFiles() {

        var findings = new ArrayList<String>();
        var manifest = directory.readManifest();
        var bundleDirectoryNames = directory.listBundleDirectoryNames();

        for (var locale : manifest.declaredLocalesByTag().values()) {

            // A locale with no directory at all has its own finding.
            if (!bundleDirectoryNames.contains(locale.localeTag())) {
                continue;
            }
            var bundle = directory.openBundle(locale);

            for (var bundleFileName : manifest.dataPathsByBundleFileName().keySet()) {

                if (!Files.isRegularFile(bundle.resolveBundleFile(bundleFileName))) {

                    findings.add(locale.localeTag() + ": holds no " + bundleFileName
                        + ", which the manifest maps");
                }
            }
        }
        return findings;
    }

    List<String> findUndeclaredBundleDirectories() {

        var declaredLocaleTags = directory.readManifest().declaredLocalesByTag().keySet();

        return directory.listBundleDirectoryNames()
            .stream()
            .filter(directoryName -> !declaredLocaleTags.contains(directoryName))
            .map(directoryName -> directoryName + ": a bundle directory the manifest does not declare")
            .toList();
    }
}
