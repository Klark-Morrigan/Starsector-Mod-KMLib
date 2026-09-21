package kmlib.starsector.compatibility;

import kmlib.starsector.strings.KmlibStringKeys;
import kmlib.text.KmlibStrings;

import java.util.Objects;

/**
 * One binding to third-party code that stopped holding, in the shape the sentence shown to the
 * player and the line written to the log are both composed from.
 *
 * <p>Kept apart from the registry that stores it so the wording can be read without one, and so the
 * registry itself carries no formatting. What the two sentences say and why is documented on
 * the strings that render them, in {@link KmlibStringKeys}; this record only fills their slots.
 *
 * @param subject      the third party and the two versions the mismatch is stated as
 * @param lostFeature  a whole sentence, in the consumer's own wording, naming what stops working
 *                     this session - the consumer's to supply because what a mismatch costs is
 *                     knowledge of the feature that broke
 * @param failureSite  where the binding stopped holding, as a phrase completing "failed while", for
 *                     the log alone. A binding is guarded at more than one point and fails
 *                     differently at each, so which guard caught it says more about the mismatch
 *                     than the throwable does: a release that moved a member and one that declares
 *                     it and refuses the call are told apart by this and by nothing else on the
 *                     record
 * @param brokenDetail the member or detail that stopped holding, for the log alone: a short phrase
 *                     rather than a sentence, since it is a row of a block
 * @param cause        what was thrown, or {@code null} where nothing was - a probe that finds a
 *                     member missing has no throwable to report. Handed to the logger beside
 *                     {@link #describeForLog()}, which is where a stack trace belongs
 */
public record CompatibilityFailure(
    CompatibilitySubject subject,
    String lostFeature,
    String failureSite,
    String brokenDetail,
    Throwable cause) {

    // A blank line between the heading, the versions and the consequence: three separate readings,
    // which run together read as one long sentence.
    private static final String PARAGRAPH_BREAK = "\n\n";

    // How an unread version is named in the log, which is written in English whatever the install's
    // localisation - the player-facing parenthetical lives in strings.json instead.
    private static final String UNKNOWN_VERSION_IN_LOG = "an unknown version";

    // The log block's rows, each carrying its own newline and indent, and each label padded so the
    // values line up in a column. Padded in the literal rather than formatted to a width, so the
    // alignment is visible where it is written and a row added beside these is lined up by eye
    // rather than by a constant whose effect is somewhere else.
    private static final String BUILT_AGAINST_ROW = "\n    Built against: ";
    private static final String INSTALLED_ROW = "\n    Installed:     ";
    private static final String FAILED_WHILE_ROW = "\n    Failed while:  ";
    private static final String BROKEN_ROW = "\n    Broken:        ";
    private static final String PLAYER_TOLD_ROW = "\n    Player told:   ";

    public CompatibilityFailure {

        Objects.requireNonNull(
            subject,
            "A failure with no subject would report that something broke and not whose.");
        KmlibStrings.requireText(
            lostFeature,
            "A failure naming nothing lost would tell the player to worry without saying about what.");
        KmlibStrings.requireText(
            failureSite,
            "A failure naming no site says a binding broke and not which guard caught it.");
        KmlibStrings.requireText(
            brokenDetail,
            "A failure naming no detail leaves the log with nothing to diagnose from.");
    }

    /**
     * The whole modal: what mismatched, which versions, and what it costs.
     *
     * <p>Localised through KMLib's own category, so the wording is editable without a rebuild - it
     * is drawn on a path that runs on almost no install, which is exactly where wording turns out
     * to need changing.
     *
     * @return heading, versions and consequence, a blank line apart
     */
    public String describeForPlayer() {

        var heading = KmlibStringKeys.format(KmlibStringKeys.COMPATIBILITY_NOTICE_TITLE, subject.name());

        var builtAgainstVersion = subject.describeBuiltAgainstVersion(
            KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_VERSION_UNKNOWN));

        // Two templates rather than one with a hole in it: "this install has (version unknown)"
        // states a reading that was never taken, where the second wording says what happened.
        var versions = subject.hasInstalledVersion()
            ? KmlibStringKeys.format(
                KmlibStringKeys.COMPATIBILITY_NOTICE_BUILT_AGAINST,
                subject.name(),
                builtAgainstVersion,
                subject.installedVersion())
            : KmlibStringKeys.format(
                KmlibStringKeys.COMPATIBILITY_NOTICE_BUILT_AGAINST_UNREADABLE,
                subject.name(),
                builtAgainstVersion);

        var consequence = KmlibStringKeys.format(
            KmlibStringKeys.COMPATIBILITY_NOTICE_CONSEQUENCE,
            lostFeature);

        return String.join(PARAGRAPH_BREAK, heading, versions, consequence);
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
            + FAILED_WHILE_ROW + failureSite
            + BROKEN_ROW + brokenDetail
            + PLAYER_TOLD_ROW + lostFeature;
    }
}
