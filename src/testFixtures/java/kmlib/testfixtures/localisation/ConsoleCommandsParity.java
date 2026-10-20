package kmlib.testfixtures.localisation;

import kmlib.testfixtures.mods.console.commands.ConsoleCommandsTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The console command table of every locale against the default's: its commands, and what each one runs.
 *
 * <p>Only the help column is a locale's to translate. The command and its syntax are what a player types,
 * the class is what the console builds, and the tags are what its listings filter by, so a locale varying
 * any of them ships a different command under the same name.
 */
final class ConsoleCommandsParity {

    // The columns every locale keeps as the default states them.
    private static final List<String> FIXED_COLUMNS = List.of(
        ConsoleCommandsTable.CLASS_COLUMN,
        ConsoleCommandsTable.SYNTAX_COLUMN,
        ConsoleCommandsTable.TAGS_COLUMN);

    private final LocaleBundleReadings bundleReadings;

    ConsoleCommandsParity(LocaleBundleReadings bundleReadings) {
        this.bundleReadings = Objects.requireNonNull(bundleReadings, "bundleReadings");
    }

    List<String> findConsoleCommandBehaviourMismatches() {

        return bundleReadings.compareWithDefault(
            LocaleBundle.COMMANDS_FILE_NAME,
            LocaleBundle::readConsoleCommands,
            ConsoleCommandsParity::describeBehaviourMismatches);
    }

    List<String> findConsoleCommandRowMismatches() {

        return bundleReadings.compareWithDefault(
            LocaleBundle.COMMANDS_FILE_NAME,
            bundle -> bundle.readConsoleCommands().keySet(),
            readings -> ComparedReadings.describeMissingAndAdded(readings, "command"));
    }

    // A command only one side declares is the row check's finding, so only commands both declare are compared.
    private static List<String> describeBehaviourMismatches(
            ComparedReadings<Map<String, Map<String, String>>> readings) {

        var findings = new ArrayList<String>();

        readings.reading().forEach((command, cells) -> {

            var referenceCells = readings
                .referenceReading()
                .get(command);

            if (referenceCells == null) {
                return;
            }
            for (var column : FIXED_COLUMNS) {

                var value = cells.get(column);
                var referenceValue = referenceCells.get(column);

                if (!Objects.equals(value, referenceValue)) {

                    findings.add(readings.describeFinding("command " + command + " differs from "
                        + readings.referenceTag() + " in " + column + ": \"" + value + "\" against \""
                        + referenceValue + "\""));
                }
            }
        });
        return findings;
    }
}
