package kmlib.testfixtures.localisation;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Reads one bundle file out of the default locale and out of every other locale holding it, and hands each
 * pair of readings to a comparison. The strings and settings checks are built on it. A check that needs no
 * default to hold a locale to reads each locale's file on its own instead.
 */
final class DefaultLocaleComparison {

    private final LocalisationDirectory directory;

    DefaultLocaleComparison(LocalisationDirectory directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    // Whether the manifest maps a file and a bundle holds it. Only then is the file read; a gap in either
    // has a finding of its own.
    static boolean isFileHeld(LocaleManifest manifest, LocaleBundle bundle, String bundleFileName) {

        return manifest.dataPathsByBundleFileName().containsKey(bundleFileName)
            && Files.isRegularFile(bundle.resolveBundleFile(bundleFileName));
    }

    // Compares nothing when the manifest maps no such file or the default holds none.
    <T> List<String> compareWithDefault(
            String bundleFileName,
            Function<LocaleBundle, T> readBundleFile,
            Function<ComparedReadings<T>, List<String>> describeMismatches) {

        var manifest = directory.readManifest();
        var defaultLocale = manifest.getDefaultLocale();
        var referenceBundle = directory.openBundle(defaultLocale);

        if (!isFileHeld(manifest, referenceBundle, bundleFileName)) {
            return List.of();
        }
        var referenceReading = readBundleFile.apply(referenceBundle);
        var findings = new ArrayList<String>();

        for (var locale : manifest.declaredLocalesByTag().values()) {

            var bundle = directory.openBundle(locale);

            if (locale.equals(defaultLocale) || !isFileHeld(manifest, bundle, bundleFileName)) {
                continue;
            }
            var readings = new ComparedReadings<>(
                bundleFileName,
                locale.localeTag(),
                readBundleFile.apply(bundle),
                defaultLocale.localeTag(),
                referenceReading);

            findings.addAll(describeMismatches.apply(readings));
        }
        return findings;
    }

    // Reads the file out of every locale holding it, the default included, and hands each reading to a
    // check of that locale alone. Compares nothing when the manifest maps no such file.
    <T> List<String> inspectEveryLocale(
            String bundleFileName,
            Function<LocaleBundle, T> readBundleFile,
            BiFunction<String, T, List<String>> describeFindings) {

        var manifest = directory.readManifest();
        var findings = new ArrayList<String>();

        for (var locale : manifest.declaredLocalesByTag().values()) {

            var bundle = directory.openBundle(locale);

            if (isFileHeld(manifest, bundle, bundleFileName)) {
                findings.addAll(describeFindings.apply(locale.localeTag(), readBundleFile.apply(bundle)));
            }
        }
        return findings;
    }
}
