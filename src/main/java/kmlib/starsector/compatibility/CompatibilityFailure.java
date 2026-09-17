package kmlib.starsector.compatibility;

import kmlib.starsector.strings.KmlibStringKeys;
import kmlib.text.KmlibStrings;

import java.util.Objects;

/**
 * One binding to third-party code that stopped holding, in the shape the sentence shown to the
 * player and the line written to the log are both composed from.
 *
 * <p>Kept apart from the registry that stores it so the wording can be asserted without one, and so
 * the registry itself carries no formatting. What the two sentences say and why is documented on
 * the strings that render them, in {@link KmlibStringKeys}; this record only fills their slots.
 *
 * @param subject      the third party and the two versions the mismatch is stated as
 * @param lostFeature  a whole sentence, in the consumer's own wording, naming what stops working
 *                     this session - the consumer's to supply because what a mismatch costs is
 *                     knowledge of the feature that broke
 * @param brokenDetail the member or detail that stopped holding, for the log alone: a short phrase
 *                     rather than a sentence, since it is appended to one
 * @param cause        what was thrown, or {@code null} where nothing was - a probe that finds a
 *                     member missing has no throwable to report. Handed to the logger beside
 *                     {@link #describeForLog()}, which is where a stack trace belongs
 */
public record CompatibilityFailure(
    CompatibilitySubject subject,
    String lostFeature,
    String brokenDetail,
    Throwable cause) {

    // A blank line between the heading, the versions and the consequence: three separate readings,
    // which run together read as one long sentence.
    private static final String PARAGRAPH_BREAK = "\n\n";

    // How an unread version is named in the log, which is written in English whatever the install's
    // localisation - the player-facing parenthetical lives in strings.json instead.
    private static final String UNKNOWN_VERSION_IN_LOG = "an unknown version";

    public CompatibilityFailure {

        Objects.requireNonNull(
            subject,
            "A failure with no subject would report that something broke and not whose.");
        KmlibStrings.requireText(
            lostFeature,
            "A failure naming nothing lost would tell the player to worry without saying about what.");
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
     * The same failure as one line for the log, naming the detail the modal deliberately leaves out.
     *
     * <p>Built from literals rather than from strings.json: this line is what a report to the third
     * party's author is written from, so it has to read the same whatever the install's localisation
     * and has to hold on a path where the game's settings may not be up yet.
     *
     * @return subject, both versions and the broken detail, on one line
     */
    public String describeForLog() {

        return subject.name() + " compatibility failure."
            + " KMLib was built against " + subject.describeBuiltAgainstVersion(UNKNOWN_VERSION_IN_LOG) + ","
            + " and this install reports " + subject.describeInstalledVersion(UNKNOWN_VERSION_IN_LOG) + "."
            + " Broken: " + brokenDetail;
    }
}
