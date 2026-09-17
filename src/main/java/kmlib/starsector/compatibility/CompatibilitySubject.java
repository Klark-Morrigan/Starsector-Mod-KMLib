package kmlib.starsector.compatibility;

import kmlib.text.KmlibStrings;

/**
 * The third party a compatibility report is about, with the two versions a mismatch is stated as:
 * the release KMLib was compiled against and the one installed now.
 *
 * <p>Its own value rather than three loose strings on the failure because it is what a probe
 * answers before anything has been decided about what broke, and because three same-typed strings
 * in a row are three that swap without a compile error. The wording rationale - why both versions,
 * why never the word "mod" - lives with the strings that render them, in
 * {@link kmlib.starsector.strings.KmlibStringKeys}.
 *
 * <p>Either version may be absent. Both are read from the third party rather than from a
 * convention, so neither is guaranteed, and a blank reads as absent so an empty self-report never
 * reaches a sentence as-is.
 *
 * @param name                the third party as a player would name it
 * @param builtAgainstVersion the release KMLib was compiled against, blank or {@code null} where
 *                            the build could not read one
 * @param installedVersion    the release installed now, blank or {@code null} where reading it
 *                            failed
 */
public record CompatibilitySubject(
    String name,
    String builtAgainstVersion,
    String installedVersion) {

    public CompatibilitySubject {

        KmlibStrings.requireText(
            name,
            "A subject with no name would report that something broke and not whose.");
    }

    /**
     * The built-against version, or {@code unknownWording} where the build could not read one.
     *
     * <p>The wording is the caller's because it differs by surface: a localised parenthetical in
     * the player's modal, a literal in the log.
     *
     * @param unknownWording what stands in the slot where no version can
     * @return the version or its stand-in, never blank
     */
    public String describeBuiltAgainstVersion(String unknownWording) {

        return KmlibStrings.hasText(builtAgainstVersion) ? builtAgainstVersion : unknownWording;
    }

    /**
     * The installed version, or {@code unknownWording} where reading it failed.
     *
     * @param unknownWording what stands in the slot where no version can
     * @return the version or its stand-in, never blank
     */
    public String describeInstalledVersion(String unknownWording) {

        return KmlibStrings.hasText(installedVersion) ? installedVersion : unknownWording;
    }

    /**
     * Whether the installed version was read at all, which decides between a sentence naming both
     * versions and one that holds without the second.
     *
     * @return {@code true} where {@link #installedVersion()} has text
     */
    public boolean hasInstalledVersion() {

        return KmlibStrings.hasText(installedVersion);
    }
}
