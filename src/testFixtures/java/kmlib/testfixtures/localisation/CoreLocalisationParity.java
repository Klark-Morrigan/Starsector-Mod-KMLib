package kmlib.testfixtures.localisation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/**
 * The locales that draw characters the vanilla atlases lack, against the core localisation each names to
 * supply them. Reads every file a player sees text from: the strings and the settings table.
 */
final class CoreLocalisationParity {

    // The last character the vanilla atlases can be relied on for. Anything past it draws as the fallback
    // glyph unless a core localisation has replaced the atlases.
    private static final int LATIN_1_LAST_CODE_POINT = 0xFF;

    private final LocalisationDirectory directory;
    private final SettingsParity settingsParity;

    CoreLocalisationParity(LocalisationDirectory directory, SettingsParity settingsParity) {

        this.directory = Objects.requireNonNull(directory, "directory");
        this.settingsParity = Objects.requireNonNull(settingsParity, "settingsParity");
    }

    List<String> findLocalesMissingCoreLocalisation() {

        var findings = new ArrayList<String>();
        var manifest = directory.readManifest();

        for (var locale : manifest.declaredLocalesByTag().values()) {

            if (locale.coreLocalisation().isPresent()) {
                continue;
            }
            var bundle = directory.openBundle(locale);
            var displayedTextsByFileName = new TreeMap<String, Collection<String>>();

            if (DefaultLocaleComparison.isFileHeld(manifest, bundle, LocaleBundle.STRINGS_FILE_NAME)) {

                displayedTextsByFileName.put(
                    LocaleBundle.STRINGS_FILE_NAME,
                    StringsParity.flattenStrings(bundle.readStrings()).values());
            }
            if (DefaultLocaleComparison.isFileHeld(manifest, bundle, LocaleBundle.SETTINGS_FILE_NAME)) {

                displayedTextsByFileName.put(
                    LocaleBundle.SETTINGS_FILE_NAME,
                    settingsParity.openSettingsTable(bundle).readDisplayedTexts());
            }
            displayedTextsByFileName.forEach((fileName, displayedTexts) -> {

                if (displayedTexts.stream().anyMatch(CoreLocalisationParity::hasCharacterOutsideLatin1)) {

                    findings.add(locale.localeTag() + ": " + fileName
                        + " draws characters outside Latin-1, but the manifest names no coreLocalisation"
                        + " for the locale");
                }
            });
        }
        return findings;
    }

    private static boolean hasCharacterOutsideLatin1(String text) {
        return text.codePoints().anyMatch(codePoint -> codePoint > LATIN_1_LAST_CODE_POINT);
    }
}
