package kmlib.starsector.compatibility;

import kmlib.starsector.strings.KmlibStringKeys;
import kmlib.text.KmlibStrings;

import java.util.Objects;

/**
 * One binding to third-party code that stopped holding, in the shape the sentence shown to the
 * player and the line written to the log are both composed from.
 *
 * <p>Kept apart from the registry that stores it so the wording can be asserted without one, and so
 * the registry itself carries no formatting. What the report has to do is name the third party: a
 * "something went wrong" modal on a modded install sends the player to whoever they installed last,
 * which is rarely the subject, and a stack trace naming KM classes sends them to KM.
 *
 * <p>Two versions, not one. Runtime can read only what is installed now, so the version KMLib was
 * compiled against has to travel beside it or a mismatch names one side of itself. Naming both also
 * removes any need to classify the failure as too-new or too-old - the two numbers say which it is.
 *
 * <p>Either version may be absent, and both are read from the third party rather than from a
 * convention, so neither is guaranteed. An absent one renders as an explicit unknown rather than as
 * {@code null} or as a build's sentinel, and an unreadable installed version switches the sentence
 * to the wording that holds without it.
 *
 * @param subjectName         the third party as a player would name it, never with "mod" appended:
 *                            some subjects are install patches with no folder under {@code mods\},
 *                            so calling one a mod sends the player to a manager that does not list
 *                            it
 * @param builtAgainstVersion the subject's release KMLib was compiled against, blank or
 *                            {@code null} where the build could not read one
 * @param installedVersion    the release installed now, blank or {@code null} where reading it
 *                            failed - itself a third-party binding, and one that can break on its
 *                            own
 * @param lostFeature         a whole sentence, in the consumer's own wording, naming what stops
 *                            working this session. The consumer's to supply because what a
 *                            mismatch costs is knowledge of the feature that broke
 * @param brokenDetail        the member or detail that stopped holding, for the log alone: a short
 *                            phrase rather than a sentence, since it is appended to one
 * @param cause               what was thrown, or {@code null} where nothing was - a probe that
 *                            finds a member missing has no throwable to report. Handed to the
 *                            logger beside {@link #describeForLog()}, which is where a stack trace
 *                            belongs
 */
public record CompatibilityFailure(
    String subjectName,
    String builtAgainstVersion,
    String installedVersion,
    String lostFeature,
    String brokenDetail,
    Throwable cause) {

    // A blank line between the heading, the versions and the consequence. They are three separate
    // readings, and a modal running them together reads as one long sentence at the moment the
    // player is least inclined to parse one.
    private static final String PARAGRAPH_BREAK = "\n\n";

    // How an unread version is named in the log, which is written in English whatever the install's
    // localisation - the player-facing parenthetical lives in strings.json instead.
    private static final String UNKNOWN_VERSION_IN_LOG = "an unknown version";

    public CompatibilityFailure {

        Objects.requireNonNull(
            subjectName,
            "A failure naming no subject would report that something broke and not what.");
        Objects.requireNonNull(
            lostFeature,
            "A failure naming nothing lost would tell the player to worry without saying about what.");
        Objects.requireNonNull(
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

        var heading = KmlibStringKeys.format(KmlibStringKeys.COMPATIBILITY_NOTICE_TITLE, subjectName);

        // Two templates rather than one with a hole in it: a sentence reading "this install has
        // (version unknown)" states a reading that was never taken, where the second wording says
        // what actually happened and covers both directions the mismatch can run in.
        var versions = KmlibStrings.hasText(installedVersion)
            ? KmlibStringKeys.format(
                KmlibStringKeys.COMPATIBILITY_NOTICE_BUILT_AGAINST,
                subjectName,
                describeBuiltAgainstVersionForPlayer(),
                installedVersion)
            : KmlibStringKeys.format(
                KmlibStringKeys.COMPATIBILITY_NOTICE_BUILT_AGAINST_UNREADABLE,
                subjectName,
                describeBuiltAgainstVersionForPlayer());

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

        return subjectName + " compatibility failure."
            + " KMLib was built against " + describeVersionForLog(builtAgainstVersion) + ","
            + " and this install reports " + describeVersionForLog(installedVersion) + "."
            + " Broken: " + brokenDetail;
    }

    private static String describeVersionForLog(String version) {

        return KmlibStrings.hasText(version) ? version : UNKNOWN_VERSION_IN_LOG;
    }

    // The parenthetical that keeps the surrounding sentence grammatical where the version slot
    // cannot be filled, rather than the word null or a sentinel a player has no reading of.
    private String describeBuiltAgainstVersionForPlayer() {

        return KmlibStrings.hasText(builtAgainstVersion)
            ? builtAgainstVersion
            : KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_VERSION_UNKNOWN);
    }
}
