package kmlib.testfixtures.localisation;

import kmlib.testfixtures.starsector.settings.LunaSettingsHighlights;
import kmlib.testfixtures.starsector.ui.label.LabelHighlightRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Every locale's settings table against the game's highlight rule: each bracketed run must be one the
 * game can highlight where it stands. Held for every locale, the default included, because the rule is the
 * game's and not a translation's.
 */
final class SettingsHighlightParity {

    private final LocaleBundleReadings bundleReadings;
    private final SettingsParity settingsParity;

    SettingsHighlightParity(LocaleBundleReadings bundleReadings, SettingsParity settingsParity) {

        this.bundleReadings = Objects.requireNonNull(bundleReadings, "bundleReadings");
        this.settingsParity = Objects.requireNonNull(settingsParity, "settingsParity");
    }

    List<String> findUnhighlightedSettingsRuns() {

        return bundleReadings.inspectEveryLocale(
            LocaleBundle.SETTINGS_FILE_NAME,
            bundle -> settingsParity.openSettingsTable(bundle).readHighlightedTextsByFieldId(),
            SettingsHighlightParity::describeUnhighlightedRuns);
    }

    // One finding per bracketed run the game would leave plain, naming the row and what blocked it.
    private static List<String> describeUnhighlightedRuns(String localeTag, Map<String, String> cellTextsByFieldId) {

        var findings = new ArrayList<String>();

        cellTextsByFieldId.forEach((fieldId, cellText) -> {

            var highlights = LunaSettingsHighlights.parseHighlights(cellText);

            LabelHighlightRule
                .findUnhighlightedRuns(highlights.drawnText(), highlights.runTexts())
                .forEach(run -> findings.add(
                    localeTag + ": " + LocaleBundle.SETTINGS_FILE_NAME
                        + " row " + fieldId + " leaves [" + run.runText() + "] plain ("
                        + run.blockingNeighbours() + "); put a space or a line break beside it"));
        });
        return findings;
    }
}
