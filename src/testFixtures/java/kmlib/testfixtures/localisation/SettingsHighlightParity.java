package kmlib.testfixtures.localisation;

import kmlib.testfixtures.starsector.settings.LunaSettingsHighlights;
import kmlib.testfixtures.starsector.ui.label.LabelHighlightRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Every locale's settings table against the game's highlight rule: each bracketed run must be one the
 * game can highlight where it stands. Held for every locale, the default included, because the rule is the
 * game's and not a translation's.
 */
final class SettingsHighlightParity {

    private final LocalisationDirectory directory;
    private final SettingsParity settingsParity;

    SettingsHighlightParity(LocalisationDirectory directory, SettingsParity settingsParity) {

        this.directory = Objects.requireNonNull(directory, "directory");
        this.settingsParity = Objects.requireNonNull(settingsParity, "settingsParity");
    }

    List<String> findUnhighlightedSettingsRuns() {

        var findings = new ArrayList<String>();
        var manifest = directory.readManifest();

        for (var locale : manifest.declaredLocalesByTag().values()) {

            var bundle = directory.openBundle(locale);

            if (!DefaultLocaleComparison.isFileHeld(manifest, bundle, LocaleBundle.SETTINGS_FILE_NAME)) {
                continue;
            }

            settingsParity
                .openSettingsTable(bundle)
                .readHighlightedTextsByFieldId()
                .forEach((fieldId, cellText) -> {

                    var highlights = LunaSettingsHighlights.parseHighlights(cellText);

                    LabelHighlightRule
                        .findUnhighlightedRuns(highlights.drawnText(), highlights.runTexts())
                        .forEach(run -> findings.add(
                            locale.localeTag() + ": " + LocaleBundle.SETTINGS_FILE_NAME
                                + " row " + fieldId + " leaves [" + run.runText() + "] plain ("
                                + run.blockingNeighbours() + "); put a space or a line break beside it"));
                });
        }
        return findings;
    }
}
