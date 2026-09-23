package kmlib.starsector.compatibility;

import kmlib.starsector.compatibility.CompatibilityNoticeLine.Emphasis;
import kmlib.starsector.compatibility.CompatibilityNoticeLine.EmphasisedRun;
import kmlib.starsector.settings.modmanager.InstalledMods;
import kmlib.starsector.strings.KmlibStringKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static kmlib.starsector.compatibility.CompatibilityNoticeLines.bringForward;
import static kmlib.starsector.compatibility.CompatibilityNoticeLines.buildLine;
import static kmlib.starsector.compatibility.CompatibilityNoticeLines.buildPhrase;
import static kmlib.starsector.compatibility.CompatibilityNoticeLines.buildSplicedLine;
import static kmlib.starsector.compatibility.CompatibilityNoticeLines.warn;

/**
 * One binding to third-party code that stopped holding, in the shape the notice shown to the player
 * and the block written to the log are both composed from.
 *
 * <p>Kept apart from the registry that stores it so the wording can be read without one, and so the
 * registry itself carries no formatting. What the notice says and why is documented on the strings
 * that render it, in {@link KmlibStringKeys}; this record only fills their slots.
 *
 * <p>The notice is answered in lines - a heading, a diagnosis, the rows, a closing line - each with
 * the runs of it that stand out named separately, so a surface built from widgets can tint them and
 * one that takes a single string can drop them. Which lines there are and what order they come in is
 * decided here and nowhere else, so both surfaces show one notice.
 *
 * @param subject   the third party and the two versions the mismatch is stated as
 * @param consumer  the mod that took the binding, as the record filed it: the key it was recorded
 *                  under - numbered where that mod had already used the key for another feature -
 *                  and the sentence naming what it loses. Held whole rather than unpacked into its
 *                  sentence, so a report can say which mod lost something as well as what
 * @param breakage  which guard caught the binding and what no longer holds
 * @param cause     what was thrown, or {@code null} where nothing was - a probe that finds a member
 *                  missing has no throwable to report. Handed to the logger beside
 *                  {@link #describeForLog()}, which is where a stack trace belongs
 */
public record CompatibilityFailure(
    CompatibilitySubject subject,
    CompatibilityConsumer consumer,
    CompatibilityBreakage breakage,
    Throwable cause) {

    // The log's own file name, which is where a report is written from. Not wording: it is the name
    // of a file on disk and reads the same in every localisation.
    private static final String LOG_FILE_NAME = "starsector.log";

    // A blank line between the lines of the notice that are paragraphs rather than rows.
    private static final String PARAGRAPH_BREAK = "\n\n";

    // Between the rows of a block, which are a list rather than a paragraph.
    private static final String ROW_BREAK = "\n";

    // How a report names a mod whose display name the game could answer for.
    private static final String NAME_WITH_ID = "%s (%s)";

    // Between the mod as named and the version it is at, where the game answered one.
    private static final String VERSION_SEPARATOR = " ";

    public CompatibilityFailure {

        Objects.requireNonNull(
            subject,
            "A failure with no subject would report that something broke and not whose.");

        Objects.requireNonNull(
            consumer,
            "A failure with no consumer could not say whose feature the binding cost.");

        Objects.requireNonNull(
            breakage,
            "A failure with no breakage leaves the log with nothing to diagnose from.");
    }

    /**
     * The whole notice as plain text, for a surface that takes one string: the heading, the
     * diagnosis where there is one, the rows, and the closing line, a blank line apart.
     *
     * @return the notice with its emphasis dropped
     */
    public String describeForPlayer() {

        var paragraphs = new ArrayList<String>();
        paragraphs.add(describeHeadingForPlayer().lineText());

        var diagnosis = describeDiagnosisForPlayer();
        if (!diagnosis.isEmpty()) {
            paragraphs.add(joinLines(diagnosis));
        }

        paragraphs.add(joinLines(describeRowsForPlayer()));
        paragraphs.add(describeClosingForPlayer().lineText());

        return String.join(PARAGRAPH_BREAK, paragraphs);
    }

    /**
     * The paragraph the notice opens with: which mod could not integrate with which third party.
     *
     * @return the heading, warning on the phrase that names the failure and bringing both names
     *         forward
     */
    public CompatibilityNoticeLine describeHeadingForPlayer() {

        var failurePhrase = KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_TITLE_ERROR);
        var modName = describeModName();

        return buildLine(
            KmlibStringKeys.format(
                KmlibStringKeys.COMPATIBILITY_NOTICE_TITLE,
                failurePhrase,
                modName,
                subject.name()),
            warn(failurePhrase),
            bringForward(modName),
            bringForward(subject.name()));
    }

    /**
     * What the player can do about it, chosen on how the installed version stands to the targeted
     * one: update where it is behind, downgrade or wait where it is ahead, and both where it could
     * not be read.
     *
     * <p>Where it could not be read the answer runs to three lines - a lead and the two cases under
     * it - because both directions are live at once and a sentence carrying both reads as one
     * tangled claim. Where a version was read it is a single line, there being one thing to say.
     *
     * <p>Where the two name one release there is no version to move to, and the line asks for a
     * report instead. It is the one shape that needs saying out loud: the rows above invite the
     * player to compare the two versions, and here the versions are the one thing already known to
     * be right.
     *
     * <p>Empty where the build stamped no target, since every sentence names the release to move
     * to and there is none to name.
     *
     * @return the diagnosis in reading order, empty where none can be given
     */
    public List<CompatibilityNoticeLine> describeDiagnosisForPlayer() {

        if (!subject.hasBuiltAgainstVersion()) {
            return List.of();
        }

        return switch (subject.resolveInstalledVersionRelation()) {
            case OLDER -> List.of(describeTooOld());

            case NEWER -> List.of(describeTooNew());

            case UNKNOWN -> describeBothDirections();

            case SAME -> List.of(describeSameVersion());
        };
    }

    /**
     * The notice's body as labelled rows, in reading order, each bringing its value forward.
     *
     * @return the rows, never empty
     */
    public List<CompatibilityNoticeLine> describeRowsForPlayer() {

        var unknownVersion = describeUnknownVersion();
        var rows = new ArrayList<CompatibilityNoticeLine>();

        rows.add(buildRow(KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_MOD, describeMod()));
        rows.add(buildRow(KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_INTEGRATION, consumer.consumerKey()));

        rows.add(buildRow(
            KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_TARGETED,
            subject.describeBuiltAgainstVersion(unknownVersion)));

        rows.add(buildRow(
            KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_DETECTED,
            subject.describeInstalledVersion(unknownVersion)));

        rows.add(buildRow(KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_BROKEN, breakage.brokenDetail()));
        rows.add(buildRow(KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_FAILED_WHILE, breakage.failureSite()));

        // The two rows a player actually weighs, and the only two that are not neutral: what is
        // lost warns, what is not is the one line of good news in the notice.
        rows.add(buildEmphasisedRow(
            KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_EFFECT,
            consumer.lostFeature(),
            Emphasis.WARNING));

        // Left out rather than filled with a stand-in where the consumer said nothing about what
        // still works. A row reading "No effect: -" claims less than no row at all and takes as
        // much of the player's eye.
        if (consumer.hasUnaffectedFeature()) {
            rows.add(buildEmphasisedRow(
                KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_NO_EFFECT,
                consumer.unaffectedFeature(),
                Emphasis.REASSURANCE));
        }

        return rows;
    }

    /**
     * The line the notice closes with, pointing at the log.
     *
     * @return the closing line, bringing the log's name forward
     */
    public CompatibilityNoticeLine describeClosingForPlayer() {

        return buildLine(
            KmlibStringKeys.format(KmlibStringKeys.COMPATIBILITY_NOTICE_SEE_LOG, LOG_FILE_NAME),
            bringForward(LOG_FILE_NAME));
    }

    /**
     * The same failure as the block written to the log, carrying the mechanics in the same order
     * the notice does. Composed by {@link CompatibilityLogBlock}, which is where that block's
     * vocabulary and alignment live.
     *
     * @return a heading and the labelled rows, newline separated
     */
    public String describeForLog() {

        return CompatibilityLogBlock.describe(this);
    }

    /**
     * Names the consuming mod for a row: its own name beside its ID and the version it is at where
     * the game holds them, and the ID alone where it does not.
     *
     * <p>Resolved here rather than on {@link CompatibilityConsumer}, which stays plain data: a
     * value that read the mod manager to be read itself could not be built without a running game,
     * and this is the only place the answer is wanted. Resolved at report time rather than held, so
     * the healthy path never reads the mod manager and a report composed before the game is up still
     * names something. An ID the game lists no mod for shows as itself, which is how a misspelled or
     * invented one surfaces.
     *
     * @return the mod as a row names it, never blank
     */
    String describeMod() {

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
     * Names the consuming mod for a sentence: its own name where the game holds one, and its ID
     * where it does not.
     *
     * @return the mod as a sentence names it, never blank
     */
    String describeModName() {

        var modName = InstalledMods.readModName(consumer.modId());

        return modName == null
            ? consumer.modId()
            : modName;
    }

    // The one line for an install known to be behind: the state warns, and so does the instruction.
    private CompatibilityNoticeLine describeTooOld() {

        var tooOldPhrase = KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_PHRASE_TOO_OLD);
        var updatePhrase = describeUpdatePhrase();

        return buildLine(
            KmlibStringKeys.format(
                KmlibStringKeys.COMPATIBILITY_NOTICE_DIAGNOSIS_OLDER_VERSION,
                subject.name(),
                tooOldPhrase,
                updatePhrase),
            bringForward(subject.name()),
            warn(tooOldPhrase),
            warn(updatePhrase));
    }

    // The one line for an install known to be ahead: what it did warns either side of the mod it
    // did it to, and the choice the player has follows.
    private CompatibilityNoticeLine describeTooNew() {

        var carriesChangesPhrase =
            KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_PHRASE_CARRIES_CHANGES);
        var dependsOnPhrase = KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_PHRASE_DEPENDS_ON);

        var modName = describeModName();
        var choicePhrase = describeChoicePhrase();

        return buildSplicedLine(
            KmlibStringKeys.format(
                KmlibStringKeys.COMPATIBILITY_NOTICE_DIAGNOSIS_NEWER_VERSION,
                subject.name(),
                carriesChangesPhrase,
                modName,
                dependsOnPhrase,
                choicePhrase.lineText()),
            List.of(
                bringForward(subject.name()),
                warn(carriesChangesPhrase),
                bringForward(modName),
                warn(dependsOnPhrase)),
            choicePhrase.emphasisedRuns());
    }

    // The one line for an install reporting the very version the build targeted: no version to
    // move to, so the instruction is to report it. Built as runs for the same reason the choice is
    // - the mod it names is brought forward inside the warning rather than taking its colour.
    private CompatibilityNoticeLine describeSameVersion() {

        var reportPhrase = buildPhrase(
            KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_PHRASE_REPORT_TO_DEVELOPER),
            Emphasis.WARNING,
            bringForward(describeModName()));

        return buildSplicedLine(
            KmlibStringKeys.format(
                KmlibStringKeys.COMPATIBILITY_NOTICE_DIAGNOSIS_SAME_VERSION,
                subject.name(),
                reportPhrase.lineText()),
            List.of(bringForward(subject.name())),
            reportPhrase.emphasisedRuns());
    }

    // The lead and the two cases under it, for an install whose version could not be read. Both
    // directions are stated because either could be true and nothing here can tell which.
    private List<CompatibilityNoticeLine> describeBothDirections() {

        var tooOldPhrase = KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_PHRASE_TOO_OLD);
        var carriesChangesPhrase =
            KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_PHRASE_NEW_AND_CARRIES_CHANGES);

        var dependsOnPhrase = KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_PHRASE_DEPENDS_ON);

        var modName = describeModName();
        var updatePhrase = describeUpdatePhrase();
        var choicePhrase = describeChoicePhrase();

        return List.of(
            buildLine(
                KmlibStringKeys.format(
                    KmlibStringKeys.COMPATIBILITY_NOTICE_DIAGNOSIS_UNKNOWN_VERSION,
                    subject.name()),
                bringForward(subject.name())),

            buildLine(
                KmlibStringKeys.format(
                    KmlibStringKeys.COMPATIBILITY_NOTICE_DIAGNOSIS_UNKNOWN_OLDER,
                    tooOldPhrase,
                    updatePhrase),
                warn(tooOldPhrase),
                warn(updatePhrase)),

            buildSplicedLine(
                KmlibStringKeys.format(
                    KmlibStringKeys.COMPATIBILITY_NOTICE_DIAGNOSIS_UNKNOWN_NEWER,
                    carriesChangesPhrase,
                    modName,
                    dependsOnPhrase,
                    choicePhrase.lineText()),
                List.of(warn(carriesChangesPhrase), bringForward(modName), warn(dependsOnPhrase)),
                choicePhrase.emphasisedRuns()));
    }

    // The instruction naming the release to move up to, which both shapes that can advise an update
    // state the same way.
    private String describeUpdatePhrase() {

        return KmlibStringKeys.format(
            KmlibStringKeys.COMPATIBILITY_NOTICE_ACTION_UPDATE,
            describeTargetedVersion());
    }

    // The choice an install ahead of the build leaves, which both shapes that can advise one state
    // the same way. Built as runs rather than as one warned phrase, so the two mods it names are
    // brought forward inside it as they are everywhere else. The version stays part of the warning:
    // it is what the player is being told to move to, not a party to the mismatch.
    private CompatibilityNoticeLine describeChoicePhrase() {

        return buildPhrase(
            KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_ACTION_DOWNGRADE_OR_WAIT),
            Emphasis.WARNING,
            bringForward(subject.name()),
            warn(describeTargetedVersion()),
            bringForward(describeModName()));
    }

    private String describeTargetedVersion() {

        return subject.describeBuiltAgainstVersion(describeUnknownVersion());
    }

    // One row, as the wording its key holds with its value in the slot, the value brought forward.
    private static CompatibilityNoticeLine buildRow(String rowKey, String rowValue) {

        return buildEmphasisedRow(rowKey, rowValue, Emphasis.HIGHLIGHT);
    }

    // The same, where the row's value stands out as something other than a plain answer.
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

    private static String describeUnknownVersion() {

        return KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_VERSION_UNKNOWN);
    }
}
