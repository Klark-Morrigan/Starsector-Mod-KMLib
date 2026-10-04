package kmlib.starsector.compatibility;

import kmlib.starsector.compatibility.CompatibilityNoticeLine.Emphasis;
import kmlib.starsector.compatibility.CompatibilityNoticeLine.EmphasisedRun;
import kmlib.starsector.settings.modmanager.InstalledMods;
import kmlib.starsector.strings.KmlibStringKeys;

import java.util.ArrayList;
import java.util.List;

import static kmlib.starsector.compatibility.CompatibilityNoticeLines.bringForward;
import static kmlib.starsector.compatibility.CompatibilityNoticeLines.buildLine;

/**
 * The parts of a notice every kind of {@link ReportedFailure} words the same way: how the mod is
 * named, a labelled row, the rows saying what is lost and what is not, and the line pointing at the
 * log.
 *
 * <p>Kept in one place so the two kinds cannot drift apart on them. A player who has seen one
 * notice should find the mod, the effect and the log pointer in the same words on the next one.
 *
 * <p>Final class with a private constructor: operations over a value type, no instances.
 */
final class NoticeParts {

    // The log's own file name, which is where a report is written from. Not wording: it is the name
    // of a file on disk and reads the same in every localisation.
    private static final String LOG_FILE_NAME = "starsector.log";

    // How a report names a mod whose display name the game could answer for.
    private static final String NAME_WITH_ID = "%s (%s)";

    // A blank line between the lines of the notice that are paragraphs rather than rows.
    private static final String PARAGRAPH_BREAK = "\n\n";

    // Between the rows of a block, which are a list rather than a paragraph.
    private static final String ROW_BREAK = "\n";

    // Between the mod as named and the version it is at, where the game answered one.
    private static final String VERSION_SEPARATOR = " ";

    private NoticeParts() {
    }

    /**
     * One row, as the wording its key holds with its value in the slot, the value brought forward.
     *
     * @param rowKey   the strings.json key of the row's template
     * @param rowValue what fills the slot
     * @return the row
     */
    static CompatibilityNoticeLine buildRow(String rowKey, String rowValue) {

        return buildEmphasisedRow(rowKey, rowValue, Emphasis.HIGHLIGHT);
    }

    /**
     * The rows a player actually weighs, and the only two that are not neutral: what is lost warns,
     * and what is not is the one line of good news in the notice.
     *
     * <p>The second is left out rather than filled with a stand-in where the consumer said nothing
     * about what still works. A row reading "No effect: -" claims less than no row at all and takes
     * as much of the player's eye.
     *
     * @param consumer the mod that lost something, and its two sentences
     * @return one row or two, in reading order
     */
    static List<CompatibilityNoticeLine> buildConsequenceRows(CompatibilityConsumer consumer) {

        var rows = new ArrayList<CompatibilityNoticeLine>();

        rows.add(buildEmphasisedRow(
            KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_EFFECT,
            consumer.lostFeature(),
            Emphasis.WARNING));

        if (consumer.hasUnaffectedFeature()) {
            rows.add(buildEmphasisedRow(
                KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_NO_EFFECT,
                consumer.unaffectedFeature(),
                Emphasis.REASSURANCE));
        }

        return rows;
    }

    /**
     * The whole notice as plain text, a blank line between its paragraphs.
     *
     * @param failure what the notice is about
     * @return the notice with its emphasis dropped
     */
    static String composePlainNotice(ReportedFailure failure) {

        var paragraphs = new ArrayList<String>();
        paragraphs.add(failure.describeHeadingForPlayer().lineText());

        var diagnosis = failure.describeDiagnosisForPlayer();
        if (!diagnosis.isEmpty()) {
            paragraphs.add(joinLines(diagnosis));
        }

        paragraphs.add(joinLines(failure.describeRowsForPlayer()));
        paragraphs.add(failure.describeClosingForPlayer().lineText());

        return String.join(PARAGRAPH_BREAK, paragraphs);
    }

    /**
     * @return the line a notice closes with, pointing at the log and bringing its name forward
     */
    static CompatibilityNoticeLine describeClosing() {

        return buildLine(
            KmlibStringKeys.format(KmlibStringKeys.COMPATIBILITY_NOTICE_SEE_LOG, LOG_FILE_NAME),
            bringForward(LOG_FILE_NAME));
    }

    /**
     * Names the mod for a row: its own name beside its ID and the version it is at where the game
     * holds them, and the ID alone where it does not.
     *
     * <p>Resolved here rather than on {@link CompatibilityConsumer}, which stays plain data: a
     * value that read the mod manager to be read itself could not be built without a running game.
     * Resolved at report time rather than held, so the healthy path never reads the mod manager and
     * a report composed before the game is up still names something. An ID the game lists no mod for
     * shows as itself, which is how a misspelled or invented one surfaces.
     *
     * @param consumer the mod to name
     * @return the mod as a row names it, never blank
     */
    static String describeMod(CompatibilityConsumer consumer) {

        var modName = InstalledMods.readModName(consumer.modId());

        var named = modName == null
            ? consumer.modId()
            : String.format(NAME_WITH_ID, modName, consumer.modId());

        var modVersion = InstalledMods.readModVersion(consumer.modId());

        return modVersion == null
            ? named
            : named + VERSION_SEPARATOR + modVersion;
    }

    /**
     * Names the mod for a sentence: its own name where the game holds one, and its ID where it does
     * not.
     *
     * @param consumer the mod to name
     * @return the mod as a sentence names it, never blank
     */
    static String describeModName(CompatibilityConsumer consumer) {

        var modName = InstalledMods.readModName(consumer.modId());

        return modName == null
            ? consumer.modId()
            : modName;
    }

    // The same as a row, where the row's value stands out as something other than a plain answer.
    private static CompatibilityNoticeLine buildEmphasisedRow(
            String rowKey,
            String rowValue,
            Emphasis emphasis) {

        return buildLine(
            KmlibStringKeys.format(rowKey, rowValue),
            new EmphasisedRun(rowValue, emphasis));
    }

    // Lines of one block, each on its own row.
    private static String joinLines(List<CompatibilityNoticeLine> lines) {

        return String.join(
            ROW_BREAK,
            lines
                .stream()
                .map(CompatibilityNoticeLine::lineText)
                .toList());
    }
}
