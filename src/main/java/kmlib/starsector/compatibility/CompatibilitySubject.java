package kmlib.starsector.compatibility;

import kmlib.text.KmlibStrings;

import java.util.ArrayList;
import java.util.List;

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
 * <p>How the two stand to each other is answered here too, as one of four relations, so a report
 * can say whether the install is behind or ahead of what the build targeted. Wording only: nothing
 * gates on the answer, so a self-report that lies costs a sentence rather than behaviour.
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

    // The prefix a self-reported version carries and a report does not want. Third parties spell
    // their own versions, and most spell them with it, so stripping it here is what keeps two
    // subjects from being shown one with a v and one without in the same block. Dropped only where
    // a digit follows, so a version that genuinely opens with a letter keeps it.
    private static final char VERSION_PREFIX = 'v';

    // What a version is split on to be compared: every run of digits is a number, and everything
    // between is a separator. Lenient by design, since the two versions come from two authors who
    // need not agree about dots, dashes or suffixes.
    private static final String NON_DIGIT_RUN = "[^0-9]+";

    // What a number segment one version has and the other lacks compares as: 1.2 and 1.2.0 are one
    // release, not two.
    private static final int MISSING_SEGMENT = 0;

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

        return describeVersion(builtAgainstVersion, unknownWording);
    }

    /**
     * The installed version, or {@code unknownWording} where reading it failed.
     *
     * @param unknownWording what stands in the slot where no version can
     * @return the version or its stand-in, never blank
     */
    public String describeInstalledVersion(String unknownWording) {

        return describeVersion(installedVersion, unknownWording);
    }

    /**
     * Whether the build stamped a version at all, which decides whether a report can say what
     * release to update or downgrade to.
     *
     * @return {@code true} where {@link #builtAgainstVersion()} has text
     */
    public boolean hasBuiltAgainstVersion() {

        return KmlibStrings.hasText(builtAgainstVersion);
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

    /**
     * How the installed version stands to the one the build targeted.
     *
     * <p>Compared as runs of digits, in order, with a run one side lacks read as zero - so
     * {@code 0.8.9} is behind {@code 0.9.1}, and {@code 1.2} is the same release as {@code 1.2.0}.
     * A suffix such as {@code rc1} counts as a further number, which reads a candidate as ahead of
     * the release it precedes; that is the cost of not knowing every author's scheme, and it costs a
     * sentence rather than behaviour.
     *
     * @return the relation, or {@link VersionRelation#UNKNOWN} where either version was not read
     *         or carries no number to compare by
     */
    public VersionRelation resolveInstalledVersionRelation() {

        var installedNumbers = parseVersionNumbers(installedVersion);
        var targetedNumbers = parseVersionNumbers(builtAgainstVersion);

        if (installedNumbers == null || targetedNumbers == null) {
            return VersionRelation.UNKNOWN;
        }

        var order = compareVersionNumbers(installedNumbers, targetedNumbers);

        if (order < 0) {
            return VersionRelation.OLDER;
        }
        return order > 0 ? VersionRelation.NEWER : VersionRelation.SAME;
    }

    // Segment by segment, the shorter padded with zeros, which is what makes 1.2 and 1.2.0 equal.
    private static int compareVersionNumbers(List<Integer> left, List<Integer> right) {

        var segments = Math.max(left.size(), right.size());

        for (var i = 0; i < segments; i++) {
            var leftSegment = i < left.size() ? left.get(i) : MISSING_SEGMENT;
            var rightSegment = i < right.size() ? right.get(i) : MISSING_SEGMENT;

            if (leftSegment != rightSegment) {
                return Integer.compare(leftSegment, rightSegment);
            }
        }
        return 0;
    }

    // Blank and null are one case: a slot nothing filled, whichever side it is on.
    private static String describeVersion(String version, String unknownWording) {

        return KmlibStrings.hasText(version) ? stripVersionPrefix(version.trim()) : unknownWording;
    }

    // Every run of digits in the version, in order, or null where there is no version or no digit
    // in it to compare by.
    private static List<Integer> parseVersionNumbers(String version) {

        if (!KmlibStrings.hasText(version)) {
            return null;
        }

        var numbers = new ArrayList<Integer>();

        for (var run : version.trim().split(NON_DIGIT_RUN)) {
            if (!run.isEmpty()) {
                numbers.add(Integer.parseInt(run));
            }
        }
        return numbers.isEmpty() ? null : numbers;
    }

    // The prefix off, where what follows it is a number. A version is shown beside a label that
    // already says it is one, so the letter is noise - and the two versions in a report come from
    // two places that need not agree about carrying it.
    private static String stripVersionPrefix(String version) {

        var opensWithPrefix = Character.toLowerCase(version.charAt(0)) == VERSION_PREFIX;

        return opensWithPrefix && version.length() > 1 && Character.isDigit(version.charAt(1))
            ? version.substring(1)
            : version;
    }

    /** How an installed release stands to the one a build targeted. */
    public enum VersionRelation {

        /** One of the two was not read, or carries nothing to compare by. */
        UNKNOWN,

        /** The install is behind the build. */
        OLDER,

        /** The install is ahead of the build. */
        NEWER,

        /** The two name one release. */
        SAME
    }
}
