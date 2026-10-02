package kmlib.testfixtures.localisation;

import kmlib.testfixtures.starsector.strings.StringTemplates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import static kmlib.testfixtures.starsector.json.ShippedJson.locateMember;

/**
 * The strings file of every locale against the default's: its keys, and the arguments each string takes.
 */
final class StringsParity {

    private final DefaultLocaleComparison comparison;

    StringsParity(DefaultLocaleComparison comparison) {
        this.comparison = Objects.requireNonNull(comparison, "comparison");
    }

    // A string is named by its category and key together, because one key may stand in two categories.
    static SortedMap<String, String> flattenStrings(Map<String, Map<String, String>> stringsByCategory) {

        var wordingsByStringKey = new TreeMap<String, String>();

        stringsByCategory.forEach((category, wordingsByKey) ->
            wordingsByKey.forEach((key, wording) -> wordingsByStringKey.put(locateMember(category, key), wording)));

        return wordingsByStringKey;
    }

    List<String> findFormatArgumentMismatches() {

        return comparison.compareWithDefault(
            LocaleBundle.STRINGS_FILE_NAME,
            bundle -> flattenStrings(bundle.readStrings()),
            StringsParity::describeFormatArgumentMismatches);
    }

    List<String> findStringsKeyMismatches() {

        return comparison.compareWithDefault(
            LocaleBundle.STRINGS_FILE_NAME,
            bundle -> flattenStrings(bundle.readStrings()).keySet(),
            readings -> ComparedReadings.describeMissingAndAdded(readings, "string"));
    }

    // A string only one side declares is the key check's finding, so only strings both declare are compared.
    private static List<String> describeFormatArgumentMismatches(ComparedReadings<SortedMap<String, String>> readings) {

        var findings = new ArrayList<String>();

        readings.reading().forEach((stringKey, wording) -> {

            var referenceWording = readings.referenceReading().get(stringKey);

            if (referenceWording == null) {
                return;
            }
            var conversions = StringTemplates.readArgumentConversions(wording);
            var referenceConversions = StringTemplates.readArgumentConversions(referenceWording);

            if (!conversions.equals(referenceConversions)) {

                findings.add(readings.describeFinding(stringKey + " takes " + conversions
                    + " where " + readings.referenceTag() + " takes " + referenceConversions));
            }
        });
        return findings;
    }
}
