package kmlib.testfixtures.localisation;

import kmlib.testfixtures.starsector.settings.LunaSettingsTable;
import kmlib.testfixtures.starsector.settings.LunaSettingsTable.FieldBehaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The settings table of every locale against the default's: its rows, what each row stores, and its tabs.
 */
final class SettingsParity {

    private final DefaultLocaleComparison comparison;

    // Empty for a mod shipping no settings table, which has no field IDs to name. A table is read only where
    // the manifest maps one, and that read refuses a missing prefix.
    private final Optional<String> settingsFieldIdPrefix;

    SettingsParity(DefaultLocaleComparison comparison, Optional<String> settingsFieldIdPrefix) {

        this.comparison = Objects.requireNonNull(comparison, "comparison");
        this.settingsFieldIdPrefix = Objects.requireNonNull(settingsFieldIdPrefix, "settingsFieldIdPrefix");
    }

    List<String> findSettingsBehaviourMismatches() {

        return comparison.compareWithDefault(
            LocaleBundle.SETTINGS_FILE_NAME,
            bundle -> openSettingsTable(bundle).readBehavioursByFieldId(),
            SettingsParity::describeBehaviourMismatches);
    }

    List<String> findSettingsRowMismatches() {

        return comparison.compareWithDefault(
            LocaleBundle.SETTINGS_FILE_NAME,
            bundle -> openSettingsTable(bundle).readDeclaredFieldIds(),
            SettingsParity::describeRowMismatches);
    }

    List<String> findSettingsTabMismatches() {

        return comparison.compareWithDefault(
            LocaleBundle.SETTINGS_FILE_NAME,
            bundle -> openSettingsTable(bundle).readTabsByFieldId(),
            SettingsParity::describeTabMismatches);
    }

    // Fails naming the fix when a comparison was opened without a prefix, rather than reading every row as
    // spacing.
    LunaSettingsTable openSettingsTable(LocaleBundle bundle) {

        var fieldIdPrefix = settingsFieldIdPrefix.orElseThrow(() -> new IllegalStateException(
            "The manifest maps " + LocaleBundle.SETTINGS_FILE_NAME
                + ", so the comparison needs the mod's settings field ID prefix"));

        return bundle.openSettingsTable(fieldIdPrefix);
    }

    // A row only one side declares is the row check's finding, so only rows both declare are compared.
    private static List<String> describeBehaviourMismatches(ComparedReadings<Map<String, FieldBehaviour>> readings) {

        var findings = new ArrayList<String>();

        readings.reading().forEach((fieldId, behaviour) -> {

            var referenceBehaviour = readings.referenceReading().get(fieldId);

            if (referenceBehaviour != null) {

                behaviour.describeDifferencesFrom(referenceBehaviour).forEach(difference -> findings.add(
                    readings.describeFinding("row " + fieldId + " differs from " + readings.referenceTag()
                        + " in " + difference)));
            }
        });
        return findings;
    }

    // Only the first misplaced row is reported, since the rows after it usually move with it. Both sides hold
    // the same IDs by now, so lists of different lengths mean one declares a row twice.
    private static List<String> describeFirstMisplacedRow(ComparedReadings<List<String>> readings) {

        var fieldIds = readings.reading();
        var referenceFieldIds = readings.referenceReading();

        for (var index = 0; index < Math.min(fieldIds.size(), referenceFieldIds.size()); index++) {

            var fieldId = fieldIds.get(index);
            var referenceFieldId = referenceFieldIds.get(index);

            if (!fieldId.equals(referenceFieldId)) {

                return List.of(readings.describeFinding("places " + fieldId + " at row " + (index + 1)
                    + ", where " + readings.referenceTag() + " places " + referenceFieldId));
            }
        }
        if (fieldIds.size() != referenceFieldIds.size()) {

            return List.of(readings.describeFinding("declares " + fieldIds.size() + " rows, where "
                + readings.referenceTag() + " declares " + referenceFieldIds.size()));
        }
        return List.of();
    }

    // Order is compared only once both sides hold the same rows: a missing row shifts every position after it.
    private static List<String> describeRowMismatches(ComparedReadings<List<String>> readings) {

        var findings = ComparedReadings.describeMissingAndAdded(readings, "row");

        return findings.isEmpty()
            ? describeFirstMisplacedRow(readings)
            : findings;
    }

    // Maps each default tab to the tabs its rows land on in the locale, and back. More than one either way is
    // a split or a merge.
    private static List<String> describeTabMismatches(ComparedReadings<Map<String, String>> readings) {

        var tabsByReferenceTab = new TreeMap<String, SortedSet<String>>();
        var referenceTabsByTab = new TreeMap<String, SortedSet<String>>();

        readings.referenceReading().forEach((fieldId, referenceTab) -> {

            var tab = readings.reading().get(fieldId);

            if (tab != null) {

                tabsByReferenceTab.computeIfAbsent(referenceTab, ignored -> new TreeSet<>()).add(tab);
                referenceTabsByTab.computeIfAbsent(tab, ignored -> new TreeSet<>()).add(referenceTab);
            }
        });
        var findings = new ArrayList<String>();

        tabsByReferenceTab.forEach((referenceTab, tabs) -> {

            if (tabs.size() > 1) {
                findings.add(readings.describeFinding("splits " + readings.referenceTag() + " tab " + referenceTab
                    + " across tabs " + tabs));
            }
        });
        referenceTabsByTab.forEach((tab, referenceTabs) -> {

            if (referenceTabs.size() > 1) {
                findings.add(readings.describeFinding("merges " + readings.referenceTag() + " tabs " + referenceTabs
                    + " into tab " + tab));
            }
        });
        return findings;
    }
}
