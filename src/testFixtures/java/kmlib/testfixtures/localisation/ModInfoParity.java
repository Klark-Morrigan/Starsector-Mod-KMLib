package kmlib.testfixtures.localisation;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Every locale's launcher fragment against the base it merges over: what cannot merge is a finding, and
 * what falls back to the base is only listed.
 */
final class ModInfoParity {

    private final LocalisationDirectory directory;

    ModInfoParity(LocalisationDirectory directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    List<String> findModInfoFragmentMismatches() {

        var findings = new ArrayList<String>();
        var base = directory.readModInfoBase();

        for (var locale : directory.readManifest().declaredLocalesByTag().values()) {

            var bundle = directory.openBundle(locale);
            var filePrefix = locale.localeTag() + ": " + LocaleBundle.MOD_INFO_FILE_NAME;

            if (base.isEmpty()) {

                if (Files.exists(bundle.resolveBundleFile(LocaleBundle.MOD_INFO_FILE_NAME))) {

                    findings.add(filePrefix + " has no " + LocalisationDirectory.MOD_INFO_BASE_FILE_NAME
                        + " to be merged over");
                }
                continue;
            }
            var declaredDependencyIds = base.get().dependencyIds();

            for (var dependencyId : bundle.readModInfoFragment().dependencyNamesById().keySet()) {

                if (!declaredDependencyIds.contains(dependencyId)) {

                    findings.add(filePrefix + " names dependency " + dependencyId + ", which "
                        + LocalisationDirectory.MOD_INFO_BASE_FILE_NAME + " does not declare");
                }
            }
        }
        return findings;
    }

    Map<String, List<String>> listModInfoFallbackFieldNames() {

        var fallbackFieldNamesByTag = new TreeMap<String, List<String>>();
        var base = directory.readModInfoBase();

        if (base.isEmpty()) {
            return fallbackFieldNamesByTag;
        }
        for (var locale : directory.readManifest().declaredLocalesByTag().values()) {

            fallbackFieldNamesByTag.put(
                locale.localeTag(),
                directory.openBundle(locale).readModInfoFragment().listFallbackFieldNames(base.get()));
        }
        return fallbackFieldNamesByTag;
    }
}
