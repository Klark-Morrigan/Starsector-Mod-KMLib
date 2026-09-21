package kmlib.starsector.compatibility;

import kmlib.starsector.strings.KmlibStringKeys;

import java.util.ArrayList;
import java.util.Objects;

/**
 * One binding to third-party code that stopped holding, in the shape the sentence shown to the
 * player and the line written to the log are both composed from.
 *
 * <p>Kept apart from the registry that stores it so the wording can be read without one, and so the
 * registry itself carries no formatting. What the two sentences say and why is documented on
 * the strings that render them, in {@link KmlibStringKeys}; this record only fills their slots.
 *
 * @param subject   the third party and the two versions the mismatch is stated as
 * @param consumer  the mod that took the binding: the key its record latched under, for the log,
 *                  and the sentence naming what it loses, for the player. Held whole rather than
 *                  unpacked into its sentence, so a report can say which mod lost something as well
 *                  as what - two mods over one broken binding file two reports, and told apart by
 *                  their sentences alone they are two the reader has to recognise by prose
 * @param breakage  which guard caught the binding and what no longer holds, for the log alone
 * @param cause     what was thrown, or {@code null} where nothing was - a probe that finds a member
 *                  missing has no throwable to report. Handed to the logger beside
 *                  {@link #describeForLog()}, which is where a stack trace belongs
 */
public record CompatibilityFailure(
    CompatibilitySubject subject,
    CompatibilityConsumer consumer,
    CompatibilityBreakage breakage,
    Throwable cause) {

    // A blank line between the heading, the rows and the reassurance: three separate readings,
    // which run together read as one long sentence.
    private static final String PARAGRAPH_BREAK = "\n\n";

    // Between the rows of a block, which are a list rather than a paragraph.
    private static final String ROW_BREAK = "\n";

    // Between the two sentences of the heading paragraph: which subject mismatched, and where to
    // look. One paragraph rather than two, the second being about the first.
    private static final String HEADING_SENTENCE_BREAK = " ";

    // How an unread version is named in the log, which is written in English whatever the install's
    // localisation - the player-facing parenthetical lives in strings.json instead.
    private static final String UNKNOWN_VERSION_IN_LOG = "an unknown version";

    // The log block's rows, each carrying its own newline and indent, and each label padded so the
    // values line up in a column. Padded in the literal rather than formatted to a width, so the
    // alignment is visible where it is written and a row added beside these is lined up by eye
    // rather than by a constant whose effect is somewhere else.
    private static final String MOD_ROW = "\n    Mod:           ";
    private static final String BUILT_AGAINST_ROW = "\n    Built against: ";
    private static final String INSTALLED_ROW = "\n    Installed:     ";
    private static final String FAILED_WHILE_ROW = "\n    Failed while:  ";
    private static final String BROKEN_ROW = "\n    Broken:        ";
    private static final String CONSUMER_ROW = "\n    Consumer:      ";
    private static final String EFFECT_ROW = "\n    Effect:        ";

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
     * The whole modal, as the same block of labelled rows the log carries, in the player's half of
     * the readings: which mod lost something, the two versions, and what it costs.
     *
     * <p>A block rather than prose for the reason the log's is: these are separate readings, and a
     * player scanning for "is my save in danger" should not have to read a paragraph to find that
     * the answer is a row. The mechanics rows - which guard caught it, which member moved - are not
     * here. They mean nothing to a player and everything to whoever fixes it, and that reader has
     * the log.
     *
     * <p>Every row is a whole template in KMLib's own strings category, label and padding together,
     * so the wording is editable without a rebuild and a translation whose labels are longer keeps
     * its own alignment rather than inheriting a column width measured in English.
     *
     * @return the heading, the rows, and the reassurance, a blank line apart
     */
    public String describeForPlayer() {

        var heading = KmlibStringKeys.format(KmlibStringKeys.COMPATIBILITY_NOTICE_TITLE, subject.name())
            + HEADING_SENTENCE_BREAK
            + KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_SEE_LOG);

        var rows = new ArrayList<String>();
        rows.add(KmlibStringKeys.format(KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_MOD, consumer.describeMod()));
        rows.add(KmlibStringKeys.format(
            KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_BUILT_FOR,
            subject.describeBuiltAgainstVersion(
                KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_VERSION_UNKNOWN))));
        rows.add(describeInstalledVersionRow());
        rows.add(KmlibStringKeys.format(KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_EFFECT, consumer.lostFeature()));

        // Left out rather than filled with a stand-in where the consumer said nothing about what
        // still works. A row reading "No effect: -" claims less than no row at all and takes as
        // much of the player's eye.
        if (consumer.hasUnaffectedFeature()) {
            rows.add(KmlibStringKeys.format(
                KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_NO_EFFECT,
                consumer.unaffectedFeature()));
        }

        return heading + PARAGRAPH_BREAK + String.join(ROW_BREAK, rows);
    }

    /**
     * The same failure as a block for the log, carrying the mechanics the modal deliberately leaves
     * out: which guard caught the binding and what no longer holds, beside the two versions.
     *
     * <p>A block of labelled rows rather than one line, because these are five separate readings and
     * the one that matters varies by report - a mismatch is diagnosed off the site and the broken
     * member, an "is it even mine" question off the versions. Run together they are a sentence
     * nobody finishes reading, and the values a reader is comparing across two logs do not line up
     * under each other.
     *
     * <p>Built from literals rather than from strings.json: this is what a report to the third
     * party's author is written from, so it has to read the same whatever the install's localisation
     * and has to hold on a path where the game's settings may not be up yet. The one row that is not
     * a literal is what the player was told, which is here so that a report pairs the screenshot
     * with the mechanics behind it.
     *
     * <p>The throwable is not in it. It goes to the logger beside this, which is what renders a
     * stack trace.
     *
     * @return a heading and five labelled rows, newline separated
     */
    public String describeForLog() {

        return subject.name() + " compatibility failure."
            + BUILT_AGAINST_ROW + subject.describeBuiltAgainstVersion(UNKNOWN_VERSION_IN_LOG)
            + INSTALLED_ROW + subject.describeInstalledVersion(UNKNOWN_VERSION_IN_LOG)
            + FAILED_WHILE_ROW + breakage.failureSite()
            + BROKEN_ROW + breakage.brokenDetail()
            + MOD_ROW + consumer.describeMod()
            + CONSUMER_ROW + consumer.consumerKey()
            + EFFECT_ROW + consumer.lostFeature();
    }

    // Two templates rather than one with a hole in it: "installed: (version unknown)" states a
    // reading that was never taken, where the second wording says what happened and what it implies.
    private String describeInstalledVersionRow() {

        return subject.hasInstalledVersion()
            ? KmlibStringKeys.format(
                KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_INSTALLED,
                subject.describeInstalledVersion(""))
            : KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_INSTALLED_UNREADABLE);
    }
}
