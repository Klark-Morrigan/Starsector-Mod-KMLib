package kmlib.testfixtures.mods.console.commands;

import kmlib.testfixtures.starsector.spreadsheets.ShippedSpreadsheet;

import java.nio.file.Path;
import java.util.Map;

/**
 * The table a mod registers its console commands in, read the way Console Commands reads it.
 *
 * <p>Console Commands finds the table by its path and keys each row by the command a player types. Both are
 * stated here once, for every suite reading a table: the guard pinning a mod's table against its command
 * classes, and the locale checks holding a translated table to the default's.
 */
public final class ConsoleCommandsTable {

    /** The column naming the class the console builds to run a command. */
    public static final String CLASS_COLUMN = "class";

    /** The column naming the command a player types, which keys every row. */
    public static final String COMMAND_COLUMN = "command";

    /** Where a mod ships its table, relative to the mod root. */
    public static final Path COMMANDS_CSV = Path.of("data", "console", "commands.csv");

    /** The column the console's help prints for a command. */
    public static final String HELP_COLUMN = "help";

    /** The column stating the arguments a command takes, as a player types them. */
    public static final String SYNTAX_COLUMN = "syntax";

    /** The column the console's listings filter commands by. */
    public static final String TAGS_COLUMN = "tags";

    private ConsoleCommandsTable() {
    }

    /**
     * Every row of a command table, keyed by its command.
     *
     * @param table the table to read
     * @return each row's cells by column name, keyed by the row's command, in file order
     */
    public static Map<String, Map<String, String>> readRowsByCommand(Path table) {
        return ShippedSpreadsheet.readRowsById(table, COMMAND_COLUMN);
    }
}
