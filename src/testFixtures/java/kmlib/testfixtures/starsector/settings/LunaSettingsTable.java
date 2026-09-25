package kmlib.testfixtures.starsector.settings;

import kmlib.testfixtures.starsector.spreadsheets.ShippedSpreadsheet;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A mod's shipped LunaLib settings file, read as rows and columns.
 *
 * <p>Offered to every mod on these conventions because the file's shape is LunaLib's rather than any
 * one mod's: the column order, the row types that draw without storing, and the way a section binds
 * its rows by file order are the same wherever the table is shipped. What each row ought to hold is
 * the mod's own business, and none of it is decided here - this reads, and the suite above judges.
 *
 * <p>The column vocabulary stays inside: a caller asks for a row's default, its bounds or its
 * options, never for cell fourteen. Nothing outside is phrased in column numbers, and a reading that
 * handed them out would put the file's shape back into every check that reads one cell of it.
 *
 * <p>Built per mod rather than answered statically, since the file to read and the ID prefix that
 * tells a field row from the file's own furniture are both the consumer's. {@link
 * LunaSettingsSourceText} is the other reading a settings check is made of, over the Java that names
 * these rows.
 */
public final class LunaSettingsTable {

    /** Where a mod on these conventions ships its settings table. */
    public static final Path SETTINGS_CSV = Path.of("data", "config", "LunaSettings.csv");

    public static final String BOOLEAN_FIELD_TYPE = "Boolean";
    public static final String DOUBLE_FIELD_TYPE = "Double";
    public static final String INT_FIELD_TYPE = "Int";
    public static final String KEYCODE_FIELD_TYPE = "Keycode";
    public static final String RADIO_FIELD_TYPE = "Radio";

    // The CSV's own column order, as its header row declares it.
    private static final int FIELD_ID_COLUMN = 0;
    private static final int FIELD_NAME_COLUMN = 4;
    private static final int FIELD_TYPE_COLUMN = 6;
    private static final int DEFAULT_VALUE_COLUMN = 7;
    private static final int OPTIONS_COLUMN = 8;
    private static final int FIELD_DESCRIPTION_COLUMN = 11;
    private static final int MIN_VALUE_COLUMN = 14;
    private static final int MAX_VALUE_COLUMN = 15;
    private static final int TAB_COLUMN = 16;

    // The header row's names for the columns a behaviour is compared over, so a difference names the cell
    // a reader of the file finds rather than an index.
    private static final String FIELD_TYPE_HEADER = "fieldType";
    private static final String DEFAULT_VALUE_HEADER = "defaultValue";
    private static final String OPTIONS_HEADER = "secondaryValue";
    private static final String MIN_VALUE_HEADER = "minValue";
    private static final String MAX_VALUE_HEADER = "maxValue";

    // The two row types that carry an ID so LunaLib can place them but store nothing, so no source
    // reads either: a section caption, and a run of prose standing among the knobs. Every other row
    // holds a value. They are told apart as well as together - a caption owns the rows under it,
    // while prose owns nothing and is free to stand ahead of every caption on its tab.
    private static final String HEADER_FIELD_TYPE = "Header";
    private static final String TEXT_FIELD_TYPE = "Text";

    // LunaLib splits a Radio's options on commas; authored rows space them out for readability.
    private static final String OPTION_SEPARATOR = ",";

    private final String fieldIdPrefix;
    private final Path settingsCsv;

    /**
     * Opens a reading of one mod's table.
     *
     * @param settingsCsv   the shipped file, relative to the module the suite runs in;
     *                      {@link #SETTINGS_CSV} is where these conventions put it
     * @param fieldIdPrefix what every one of the mod's field IDs starts with, which is also what
     *                      tells a field row from the spacing rows and the column-header line
     */
    public LunaSettingsTable(Path settingsCsv, String fieldIdPrefix) {
        this.fieldIdPrefix = Objects.requireNonNull(fieldIdPrefix, "fieldIdPrefix");
        this.settingsCsv = Objects.requireNonNull(settingsCsv, "settingsCsv");
    }

    /**
     * The section captions whose two caption cells hold different text. A Header is drawn through
     * addSectionHeading(defaultValue), so the name column beside it is inert for this row type
     * alone - every other row type shows its name column and stores its default. Both are authored
     * to the same text so that the row reads the same however it is skimmed.
     *
     * @return the offending captions' field IDs, in file order
     */
    public List<String> findHeaderRowsWhoseCaptionColumnsDisagree() {
        return readFieldRows()
            .stream()
            .filter(row -> HEADER_FIELD_TYPE.equals(row.get(FIELD_TYPE_COLUMN)))
            .filter(row -> !row.get(FIELD_NAME_COLUMN).equals(row.get(DEFAULT_VALUE_COLUMN)))
            .map(row -> row.get(FIELD_ID_COLUMN))
            .toList();
    }

    /**
     * The value rows placed on a different tab from the caption that heads their section. LunaLib
     * draws a section as the caption plus the rows following it, so the file's order is what binds
     * the two - a row moved between tabs on its own leaves its heading behind, and a row added under
     * the wrong caption inherits a tab nobody chose for it.
     *
     * @return the stranded rows' field IDs, in file order
     */
    public List<String> findRowsStrandedFromTheirSection() {
        var strandedFieldIds = new ArrayList<String>();

        // Empty until the first caption, so a value row ahead of every caption reads as stranded -
        // it has no section to belong to.
        var sectionTab = "";

        for (var row : readFieldRows()) {
            if (HEADER_FIELD_TYPE.equals(row.get(FIELD_TYPE_COLUMN))) {
                sectionTab = row.get(TAB_COLUMN);
            } else if (isStoredValueRow(row) && !sectionTab.equals(row.get(TAB_COLUMN))) {
                strandedFieldIds.add(row.get(FIELD_ID_COLUMN));
            }
        }
        return strandedFieldIds;
    }

    /**
     * The tabs whose rows are split into more than one run by another tab's. LunaLib places a row
     * by its tab column alone, so an interleaved file still draws the same screen - what breaks is
     * reading it. A file authored one unbroken block per tab is what makes a misplaced section show
     * up as a stray run of its own rather than as a handful of cells among a hundred-odd
     * identical-looking ones.
     *
     * @return the tab names declared in more than one run, in file order
     */
    public List<String> findTabsDeclaredInMoreThanOneRun() {
        var runsPerTab = new LinkedHashMap<String, Integer>();

        // Empty rather than any tab name, so the file's first row opens a run instead of joining
        // one. No tab is named by the empty string, the spacer rows carrying no prefixed id.
        var previousTab = "";

        for (var tab : readDeclaredTabs()) {
            if (!tab.equals(previousTab)) {
                runsPerTab.merge(tab, 1, Integer::sum);
                previousTab = tab;
            }
        }
        return runsPerTab
            .entrySet()
            .stream()
            .filter(tabRuns -> tabRuns.getValue() > 1)
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * The prose rows whose words would not reach the screen. LunaLib draws a Text row through
     * addPara(defaultValue) and shows no name column for it, so words authored beside it are
     * invisible and an empty drawn column is a blank note taking up space. Neither shows as an
     * error anywhere - the row loads, it simply says nothing.
     *
     * @return the offending prose rows' field IDs, in file order
     */
    public List<String> findTextRowsWhoseWordsAreNotDrawn() {
        return readFieldRows()
            .stream()
            .filter(row -> TEXT_FIELD_TYPE.equals(row.get(FIELD_TYPE_COLUMN)))
            .filter(row -> !row.get(FIELD_NAME_COLUMN).isEmpty()
                    || row.get(DEFAULT_VALUE_COLUMN).isEmpty())
            .map(row -> row.get(FIELD_ID_COLUMN))
            .toList();
    }

    /**
     * What each row does as against what it says, keyed by field ID: the columns that decide what is
     * stored and how, which no wording of the row may change.
     *
     * @return each row's behaviour by field ID, in file order
     */
    public Map<String, FieldBehaviour> readBehavioursByFieldId() {

        var behavioursByFieldId = new LinkedHashMap<String, FieldBehaviour>();

        for (var row : readFieldRows()) {

            // A draw-only row's default is the words it draws, not a value, so it is text rather than
            // behaviour and stays out.
            var storedDefaultValue = isStoredValueRow(row)
                ? Optional.of(row.get(DEFAULT_VALUE_COLUMN))
                : Optional.<String>empty();

            var behaviour = new FieldBehaviour(
                row.get(FIELD_TYPE_COLUMN),
                storedDefaultValue,
                row.get(OPTIONS_COLUMN),
                row.get(MIN_VALUE_COLUMN),
                row.get(MAX_VALUE_COLUMN));

            putOnce(behavioursByFieldId, row.get(FIELD_ID_COLUMN), behaviour);
        }
        return behavioursByFieldId;
    }

    /**
     * Every prefixed ID the file declares a row for, section captions included: a caption stores
     * nothing, but it is still a row the file declares, so a source naming one is not naming a key
     * that does not exist.
     *
     * @return those IDs, in file order
     */
    public List<String> readDeclaredFieldIds() {
        return readFieldRows()
            .stream()
            .map(row -> row.get(FIELD_ID_COLUMN))
            .toList();
    }

    /**
     * The tab every row asks to be placed on, section captions included: a caption is what carries
     * a section onto a tab, so it is placed the same way a value row is.
     *
     * @return those tab names, in file order
     */
    public List<String> readDeclaredTabs() {
        return readFieldRows()
            .stream()
            .map(row -> row.get(TAB_COLUMN))
            .toList();
    }

    /**
     * The value a fresh player is given for the named row.
     *
     * @param fieldId           the row to read
     * @param expectedFieldType the type the caller reads the row as
     * @return the default cell's text
     */
    public String readDefaultValue(String fieldId, String expectedFieldType) {
        return readColumn(fieldId, DEFAULT_VALUE_COLUMN, expectedFieldType);
    }

    /**
     * Every piece of text the settings screen draws from the file: each row's tab, a value row's name,
     * description and, for a Radio, its options, and a caption's or a prose row's words. The cells
     * nothing draws - IDs, types, bounds, a stored default - are left out.
     *
     * @return those texts, in file order, blanks dropped
     */
    public List<String> readDisplayedTexts() {

        var displayedTexts = new ArrayList<String>();

        for (var row : readFieldRows()) {

            displayedTexts.add(row.get(TAB_COLUMN));

            if (isStoredValueRow(row)) {

                displayedTexts.add(row.get(FIELD_NAME_COLUMN));
                displayedTexts.add(row.get(FIELD_DESCRIPTION_COLUMN));

                if (RADIO_FIELD_TYPE.equals(row.get(FIELD_TYPE_COLUMN))) {
                    displayedTexts.add(row.get(OPTIONS_COLUMN));
                }
            } else {
                displayedTexts.add(row.get(DEFAULT_VALUE_COLUMN));
            }
        }
        displayedTexts.removeIf(String::isEmpty);

        return displayedTexts;
    }

    /**
     * Every row the file declares, as the pair a caller walking the file by type asks about. Two
     * cells rather than the row, so a check that wants the Boolean rows or the numeric ones says so
     * without also being handed the file's shape.
     *
     * @return those pairs, in file order
     */
    public List<FieldRow> readFieldIdsAndTypes() {
        return readFieldRows()
            .stream()
            .map(row -> new FieldRow(row.get(FIELD_ID_COLUMN), row.get(FIELD_TYPE_COLUMN)))
            .toList();
    }

    /**
     * The high end of the named row's slider.
     *
     * @param fieldId           the row to read
     * @param expectedFieldType the type the caller reads the row as
     * @return the maximum cell's text
     */
    public String readMaxValue(String fieldId, String expectedFieldType) {
        return readColumn(fieldId, MAX_VALUE_COLUMN, expectedFieldType);
    }

    /**
     * The low end of the named row's slider.
     *
     * @param fieldId           the row to read
     * @param expectedFieldType the type the caller reads the row as
     * @return the minimum cell's text
     */
    public String readMinValue(String fieldId, String expectedFieldType) {
        return readColumn(fieldId, MIN_VALUE_COLUMN, expectedFieldType);
    }

    /**
     * The row's offered option labels, trimmed of the spacing authored rows use.
     *
     * @param fieldId the Radio row to read
     * @return its labels, in the order the dropdown offers them
     */
    public List<String> readOptions(String fieldId) {
        return Arrays
            .stream(readColumn(fieldId, OPTIONS_COLUMN, RADIO_FIELD_TYPE).split(OPTION_SEPARATOR))
            .map(String::trim)
            .filter(option -> !option.isEmpty())
            .toList();
    }

    /** @return every Radio field the shipped file declares, in file order */
    public List<String> readRadioFieldIds() {
        return readFieldRows()
            .stream()
            .filter(row -> RADIO_FIELD_TYPE.equals(row.get(FIELD_TYPE_COLUMN)))
            .map(row -> row.get(FIELD_ID_COLUMN))
            .toList();
    }

    /**
     * The tab each row is placed on, keyed by field ID.
     *
     * @return each row's tab name by field ID, in file order
     */
    public Map<String, String> readTabsByFieldId() {

        var tabsByFieldId = new LinkedHashMap<String, String>();

        for (var row : readFieldRows()) {
            putOnce(tabsByFieldId, row.get(FIELD_ID_COLUMN), row.get(TAB_COLUMN));
        }
        return tabsByFieldId;
    }

    /** @return every field the screen stores a value for, in file order */
    public List<String> readValueFieldIds() {
        return readFieldRows()
            .stream()
            .filter(LunaSettingsTable::isStoredValueRow)
            .map(row -> row.get(FIELD_ID_COLUMN))
            .toList();
    }

    // Whether a row is one the screen stores a value for, as against the caption and prose rows that
    // only draw. Both of those carry an ID and neither belongs to any section, so the readings that
    // ask what a field is worth, and the one that asks which caption owns it, have to leave them out.
    private static boolean isStoredValueRow(List<String> row) {

        var fieldType = row.get(FIELD_TYPE_COLUMN);

        return !HEADER_FIELD_TYPE.equals(fieldType) && !TEXT_FIELD_TYPE.equals(fieldType);
    }

    // A reading keyed by field ID holds one row per ID, so a second row under one would be dropped from
    // it unseen - and which of the two LunaLib honours is nothing the file states.
    private static <V> void putOnce(Map<String, V> valuesByFieldId, String fieldId, V value) {

        if (valuesByFieldId.put(fieldId, value) != null) {

            throw new AssertionError("Field " + fieldId + " is declared by more than one row");
        }
    }

    private List<String> findRow(String fieldId) {

        var rows = readFieldRows()
            .stream()
            .filter(row -> fieldId.equals(row.get(FIELD_ID_COLUMN)))
            .toList();

        if (rows.size() != 1) {
            throw new AssertionError(
                "Expected exactly one row for field " + fieldId + " in " + settingsCsv
                    + " but found " + rows.size());
        }
        return rows.get(0);
    }

    // One cell of the named field's row. Fails outright when the row is missing or is not of the
    // type the caller reads it as, since either means the caller's tables no longer describe the
    // shipped file - and a cell read off a row of the wrong type would otherwise be held against a
    // column that means something else there.
    private String readColumn(String fieldId, int column, String expectedFieldType) {

        var row = findRow(fieldId);
        var declaredType = row.get(FIELD_TYPE_COLUMN);

        if (!declaredType.equals(expectedFieldType)) {
            throw new AssertionError(
                "Field " + fieldId + " in " + settingsCsv + " is declared " + declaredType
                    + " but was read as " + expectedFieldType);
        }
        return row.get(column);
    }

    // Every row the file declares for one of the mod's fields, section captions included, in file
    // order - the one reading every other goes through, so no reading judges a row differently. The
    // spacing rows between sections and the file's own column-header line carry no prefixed ID, so
    // the prefix is also what tells a row from the file's furniture.
    //
    // Read by column position rather than by header name, because every reading above is written
    // against the column indices the file lays out. The parse itself - the description and option
    // columns both carry commas inside quotes, which a split on the comma would shift every later
    // column past - is ShippedSpreadsheet's, so it is one parser's business rather than this
    // fixture's.
    private List<List<String>> readFieldRows() {
        return ShippedSpreadsheet
            .readCells(settingsCsv)
            .stream()
            .filter(row -> row.size() > TAB_COLUMN)
            .filter(row -> row.get(FIELD_ID_COLUMN).startsWith(fieldIdPrefix))
            .toList();
    }

    /**
     * The cells of one row that decide what LunaLib stores and how, as against the words it draws.
     *
     * @param fieldType              the row's declared type
     * @param storedDefaultValueText the default a fresh player is given; empty for a caption or a prose
     *                               row, whose default cell is the words it draws rather than a value
     * @param optionsText            the options cell as written, which for a Radio is also the stored value
     *                               of whichever option is picked
     * @param minValueText           the low end of a slider
     * @param maxValueText           the high end of a slider
     */
    public record FieldBehaviour(
        String fieldType,
        Optional<String> storedDefaultValueText,
        String optionsText,
        String minValueText,
        String maxValueText) {

        /**
         * Each column in which this row does something other than {@code referenceBehaviour}, named as
         * the file's header names it so the message points at the cell to fix.
         *
         * @param referenceBehaviour the row to hold this one to
         * @return one {@code <column>: "<this>" against "<reference>"} entry per differing column, in
         *         column order
         */
        public List<String> describeDifferencesFrom(FieldBehaviour referenceBehaviour) {

            var differences = new ArrayList<String>();

            describeDifference(differences, FIELD_TYPE_HEADER, fieldType, referenceBehaviour.fieldType);
            describeDifference(
                differences,
                DEFAULT_VALUE_HEADER,
                storedDefaultValueText.orElse(""),
                referenceBehaviour.storedDefaultValueText.orElse(""));
            describeDifference(differences, OPTIONS_HEADER, optionsText, referenceBehaviour.optionsText);
            describeDifference(differences, MIN_VALUE_HEADER, minValueText, referenceBehaviour.minValueText);
            describeDifference(differences, MAX_VALUE_HEADER, maxValueText, referenceBehaviour.maxValueText);

            return differences;
        }

        private static void describeDifference(
                List<String> differences,
                String columnName,
                String text,
                String referenceText) {

            if (!text.equals(referenceText)) {
                differences.add(columnName + ": \"" + text + "\" against \"" + referenceText + "\"");
            }
        }
    }

    /**
     * One declared row, as a check walking the file by type needs it.
     *
     * @param fieldId   the LunaLib field ID the row is stored under
     * @param fieldType the row's declared type, which is what says how its cells are read
     */
    public record FieldRow(String fieldId, String fieldType) {
    }
}
