package kmlib.mods.console.commands;

import kmlib.testfixtures.mods.console.commands.output.CommandOutputFake;
import kmlib.testfixtures.starsector.spreadsheets.ShippedSpreadsheet;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shipped command table against the classes it names, because nothing else does: no
 * compiler reads that file, and every way it can drift fails silently in play.
 *
 * <p>A class named there under a typo, or moved to another package, leaves the command missing
 * from the console with no error anywhere - the mod loads, the class exists, and the row simply
 * points at nothing. A command shipped without a row is the same failure from the other side:
 * code that runs perfectly and can never be invoked. Both are caught here by walking the two
 * directions between the table and the package.
 *
 * <p>A row's class also has to be instantiable the way the console instantiates it - a public
 * no-argument constructor, called reflectively - so a command whose only constructor is the
 * output-injecting one used by these suites would register and then fail at the moment a player
 * typed it.
 *
 * <p>A row can also drift in what it says rather than in what it points at: the arguments a command
 * accepts are published here by hand and derived in the command from its own keywords, so help can
 * go on describing a grammar the parser has moved past. That pair is pinned too.
 *
 * <p>Reads the real data file and the real source directory rather than fixtures: a fixture would
 * agree with the code while the shipped table did not, which is precisely the failure.
 */
class ConsoleCommandsCsvIntegrationTest {

    private static final Path COMMANDS_CSV = Path.of("data", "console", "commands.csv");

    private static final Path COMMAND_SOURCE_DIRECTORY =
        Path.of("src", "main", "java", "kmlib", "mods", "console", "commands");

    private static final String COMMAND_PACKAGE = "kmlib.mods.console.commands.";

    private static final String JAVA_SUFFIX = ".java";

    // The columns this suite reads, by the names the table's own header gives them.
    private static final String CLASS_COLUMN = "class";

    private static final String COMMAND_COLUMN = "command";

    private static final String SYNTAX_COLUMN = "syntax";

    private static final String LIST_FACTIONS_COMMAND = "kmlib_list_factions";

    // Spelled out rather than read off either surface, which is what makes the pair below a gate:
    // a keyword added to a listing enum breaks the usage case, moving the literal breaks the table
    // case, and the table is updated to clear it.
    private static final String LIST_FACTIONS_SYNTAX =
        "kmlib_list_factions [markets|hidden|discoverable|no_markets]"
            + " [no_holdings] [no_attitude] [to_log]";

    // Every class the shipped table registers, in the order the file lists them. An empty answer
    // is a failure rather than a table with nothing in it: every case here walks what it is given,
    // so a read that came back with nothing would pass all of them without checking anything.
    private static List<String> readRegisteredClassNames() {

        var registeredClassNames = ShippedSpreadsheet.listColumnValues(COMMANDS_CSV, CLASS_COLUMN);

        if (registeredClassNames.isEmpty()) {
            throw new AssertionError("No command rows read from " + COMMANDS_CSV);
        }
        return registeredClassNames;
    }

    // The syntax column of one command's row, read by header name. The tags and help columns around it
    // both carry commas inside quotes, which is the parser's business rather than this suite's.
    private static String readSyntaxColumnOf(String commandName) {

        var row = ShippedSpreadsheet
            .readRowsById(COMMANDS_CSV, COMMAND_COLUMN)
            .get(commandName);

        if (row == null) {
            throw new AssertionError("No row for " + commandName + " in " + COMMANDS_CSV);
        }
        return row.get(SYNTAX_COLUMN);
    }

    // Every command this package actually ships: a concrete class the console could run. The
    // base class and the collaborators around it are excluded by asking what they are rather
    // than by name, so a second base class or a renamed one does not quietly become a command
    // the table is then required to register.
    private static List<Class<?>> readShippedCommandClasses() {

        var commandClasses = new ArrayList<Class<?>>();

        for (var className : readSourceClassNames()) {

            var commandClass = loadClass(className);

            if (BaseCommand.class.isAssignableFrom(commandClass)
                    && !Modifier.isAbstract(commandClass.getModifiers())) {
                commandClasses.add(commandClass);
            }
        }

        if (commandClasses.isEmpty()) {
            throw new AssertionError("No command classes found in " + COMMAND_SOURCE_DIRECTORY);
        }
        return commandClasses;
    }

    private static List<String> readSourceClassNames() {
        try (Stream<Path> sourceFiles = Files.list(COMMAND_SOURCE_DIRECTORY)) {
            return sourceFiles
                .map(sourceFile -> sourceFile.getFileName().toString())
                .filter(fileName -> fileName.endsWith(JAVA_SUFFIX))
                .map(fileName -> COMMAND_PACKAGE
                    + fileName.substring(0, fileName.length() - JAVA_SUFFIX.length()))
                .toList();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException failure) {
            throw new AssertionError("No class " + className + " on the classpath", failure);
        }
    }

    @Nested
    class RegisteredClasses {

        @Test
        void nameAClassThatExistsAndIsAConsoleCommand() {
            // The row is the only thing tying a command to the console, and it is a string: a
            // typo or a package move leaves it pointing at nothing, silently.
            for (var className : readRegisteredClassNames()) {

                assertThat(loadClass(className))
                    .describedAs("row for %s in %s", className, COMMANDS_CSV)
                    .matches(BaseCommand.class::isAssignableFrom);
            }
        }

        @Test
        void canBeInstantiatedTheWayTheConsoleInstantiatesThem() {
            // Console Commands builds a command reflectively through its public no-arg
            // constructor. A command carrying only the output-injecting one would register and
            // then fail the first time a player typed it.
            for (var className : readRegisteredClassNames()) {

                var constructor = readNoArgumentConstructor(loadClass(className));

                assertThat(constructor)
                    .describedAs("public no-arg constructor on %s", className)
                    .isNotNull();
                assertThat(Modifier.isPublic(constructor.getModifiers()))
                    .describedAs("public no-arg constructor on %s", className)
                    .isTrue();
            }
        }

        private Constructor<?> readNoArgumentConstructor(Class<?> commandClass) {
            try {
                return commandClass.getConstructor();
            } catch (NoSuchMethodException failure) {
                throw new AssertionError(
                    "No public no-arg constructor on " + commandClass.getName(),
                    failure);
            }
        }
    }

    /**
     * The syntax the table publishes for {@code kmlib_list_factions}, and the usage line the
     * command itself prints - one statement of the same grammar made twice.
     *
     * <p>The command derives its usage from the keyword enums, while the table's column is typed by
     * hand. Only the typed one can be wrong, and nothing in play says so: a player reading help
     * sees the table, while the parser answers to the enums. The command whose help omits an
     * argument it accepts is the quieter half of the same drift the rest of this suite catches.
     *
     * <p>Both are asserted against a literal spelled out above rather than against each other,
     * which would let the pair move together and reach a player undocumented.
     */
    @Nested
    class ListFactionsSyntax {

        @Test
        void isWhatTheShippedTablePublishes() {

            assertThat(readSyntaxColumnOf(LIST_FACTIONS_COMMAND))
                .isEqualTo(LIST_FACTIONS_SYNTAX);
        }

        @Test
        void isWhatTheCommandPrintsAsItsUsage() {
            // Reached through the two-filter correction, the one path that prints the usage. No
            // sector is needed for it: the command reaches the sector only after that check.
            var outputFake = new CommandOutputFake();

            new ListFactionsCommand(outputFake).runCommand(
                "hidden discoverable",
                BaseCommand.CommandContext.CAMPAIGN_MAP);

            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Usage: " + LIST_FACTIONS_SYNTAX + "."));
        }
    }

    @Nested
    class ShippedCommands {

        @Test
        void areAllRegisteredByTheShippedTable() {
            // The other direction: a command written, tested and shipped without a row runs
            // perfectly and can never be invoked.
            var registeredClassNames = readRegisteredClassNames();

            for (var commandClass : readShippedCommandClasses()) {

                assertThat(registeredClassNames)
                    .describedAs("row in %s for %s", COMMANDS_CSV, commandClass.getName())
                    .contains(commandClass.getName());
            }
        }
    }
}
