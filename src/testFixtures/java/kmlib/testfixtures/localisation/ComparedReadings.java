package kmlib.testfixtures.localisation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * One locale's reading of a bundle file beside the default locale's reading of the same file.
 *
 * @param bundleFileName   the file both were read from
 * @param localeTag        the locale compared
 * @param reading          what that locale's file holds
 * @param referenceTag     the default locale, which the other is held to
 * @param referenceReading what the default's file holds
 * @param <T>              the reading's shape
 */
record ComparedReadings<T>(
    String bundleFileName,
    String localeTag,
    T reading,
    String referenceTag,
    T referenceReading) {

    // Every finding names the locale and the file first, so it reads as where to look.
    String describeFinding(String findingText) {
        return localeTag + ": " + bundleFileName + " " + findingText;
    }

    // The items the locale lacks against the default, then the ones it adds.
    static List<String> describeMissingAndAdded(
            ComparedReadings<? extends Collection<String>> readings,
            String itemNoun) {

        var items = readings.reading();
        var referenceItems = readings.referenceReading();
        var findings = new ArrayList<String>();

        referenceItems.stream()
            .filter(item -> !items.contains(item))
            .forEach(item -> findings.add(readings.describeFinding(
                "lacks " + itemNoun + " " + item + ", which " + readings.referenceTag() + " declares")));

        items.stream()
            .filter(item -> !referenceItems.contains(item))
            .forEach(item -> findings.add(readings.describeFinding(
                "declares " + itemNoun + " " + item + ", which " + readings.referenceTag() + " does not")));

        return findings;
    }
}
