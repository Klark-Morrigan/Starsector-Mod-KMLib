package kmlib.testfixtures.localisation;

import kmlib.mods.console.ConsoleStrings;
import kmlib.testfixtures.mods.console.commands.ConsoleCommandsTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The English copy a translated console text carries after {@link ConsoleStrings#ENGLISH_COPY_MARKER}: the
 * help column of the console command table, and every string whose key starts with
 * {@link ConsoleStrings#CONSOLE_KEY_PREFIX}.
 *
 * <p>Console Commands draws in fonts of its own, holding no character past Latin-1. A translation drawing
 * one prints as a row of {@code ?}, so it has to carry the default's wording beneath it. That copy is what
 * the player reads, so it has to say what the default says, word for word.
 */
final class ConsoleEnglishCopyParity {

    // How the command table spells the marker: Console Commands reads each backslash-n in a help cell as a
    // line break.
    private static final String TABLE_MARKER = ConsoleStrings.ENGLISH_COPY_MARKER.replace("\n", "\\n");

    private final LocaleBundleReadings bundleReadings;

    ConsoleEnglishCopyParity(LocaleBundleReadings bundleReadings) {
        this.bundleReadings = Objects.requireNonNull(bundleReadings, "bundleReadings");
    }

    // The translation alone, without the English copy a console string may carry after it.
    static String cutEnglishCopy(String wording) {

        var markerIndex = wording.indexOf(ConsoleStrings.ENGLISH_COPY_MARKER);

        return markerIndex < 0
            ? wording
            : wording.substring(0, markerIndex);
    }

    List<String> findConsoleHelpEnglishCopyMismatches() {

        return bundleReadings.compareWithDefault(
            LocaleBundle.COMMANDS_FILE_NAME,
            ConsoleEnglishCopyParity::readHelpByCommand,
            readings -> describeCopyMismatches(readings, TABLE_MARKER, "command"));
    }

    List<String> findConsoleStringEnglishCopyMismatches() {

        return bundleReadings.compareWithDefault(
            LocaleBundle.STRINGS_FILE_NAME,
            ConsoleEnglishCopyParity::readConsoleStrings,
            readings -> describeCopyMismatches(readings, ConsoleStrings.ENGLISH_COPY_MARKER, "string"));
    }

    // An item only one side declares is the key and row checks' finding, so only items both declare are
    // compared.
    private static List<String> describeCopyMismatches(
            ComparedReadings<SortedMap<String, String>> readings,
            String marker,
            String itemNoun) {

        var findings = new ArrayList<String>();

        readings.reading().forEach((item, wording) -> {

            var referenceWording = readings.referenceReading().get(item);

            if (referenceWording == null || wording == null) {
                return;
            }
            var markerIndex = wording.indexOf(marker);

            if (markerIndex < 0) {

                if (CoreLocalisationParity.hasCharacterOutsideLatin1(wording)) {

                    findings.add(readings.describeFinding(itemNoun + " " + item
                        + " draws characters Console Commands' fonts lack, but carries no English copy"));
                }
                return;
            }
            var englishCopy = wording.substring(markerIndex + marker.length());

            if (!englishCopy.equals(referenceWording)) {

                findings.add(readings.describeFinding(itemNoun + " " + item
                    + " carries an English copy differing from " + readings.referenceTag() + ": \""
                    + englishCopy + "\" against \"" + referenceWording + "\""));
            }
        });
        return findings;
    }

    private static SortedMap<String, String> readConsoleStrings(LocaleBundle bundle) {

        var consoleStringsByCategory = new TreeMap<String, Map<String, String>>();

        bundle.readStrings().forEach((category, wordingsByKey) -> {

            var consoleWordingsByKey = new TreeMap<String, String>();

            wordingsByKey.forEach((key, wording) -> {

                if (key.startsWith(ConsoleStrings.CONSOLE_KEY_PREFIX)) {
                    consoleWordingsByKey.put(key, wording);
                }
            });
            consoleStringsByCategory.put(category, consoleWordingsByKey);
        });
        return StringsParity.flattenStrings(consoleStringsByCategory);
    }

    private static SortedMap<String, String> readHelpByCommand(LocaleBundle bundle) {

        var helpByCommand = new TreeMap<String, String>();

        bundle.readConsoleCommands().forEach((command, cells) ->
            helpByCommand.put(command, cells.get(ConsoleCommandsTable.HELP_COLUMN)));

        return helpByCommand;
    }
}
