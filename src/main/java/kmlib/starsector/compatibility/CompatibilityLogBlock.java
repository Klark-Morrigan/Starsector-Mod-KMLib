package kmlib.starsector.compatibility;

/**
 * A failed binding as the block written to the log: the mechanics a report to the third party's
 * author is composed from, which the player's modal deliberately leaves out.
 *
 * <p>Its own class rather than a second renderer on {@link CompatibilityFailure}, because the two
 * readings share nothing but the value they read. This one is built from literals, so that it holds
 * on a path where the game's settings may not be up yet and reads the same whatever the install's
 * localisation; the player's is built from KMLib's own strings category so its wording is editable
 * without a rebuild. Kept together they were one class carrying a record, two row vocabularies and
 * two sets of alignment rules.
 *
 * <p>The throwable is not in it. It goes to the logger beside this, which is what renders a stack
 * trace.
 */
final class CompatibilityLogBlock {

    // How an unread version is named here, which is written in English whatever the install's
    // localisation - the player-facing parenthetical lives in strings.json instead.
    private static final String UNKNOWN_VERSION = "an unknown version";

    // The rows, each carrying its own newline and indent, and each label padded so the values line
    // up in a column. Padded in the literal rather than formatted to a width, so the alignment is
    // visible where it is written and a row added beside these is lined up by eye rather than by a
    // constant whose effect is somewhere else.
    private static final String HEADING_SUFFIX = " compatibility failure.";
    private static final String BUILT_AGAINST_ROW = "\n    Built against: ";
    private static final String INSTALLED_ROW = "\n    Installed:     ";
    private static final String FAILED_WHILE_ROW = "\n    Failed while:  ";
    private static final String BROKEN_ROW = "\n    Broken:        ";
    private static final String MOD_ROW = "\n    Mod:           ";
    private static final String CONSUMER_ROW = "\n    Consumer:      ";
    private static final String EFFECT_ROW = "\n    Effect:        ";

    private CompatibilityLogBlock() {
    }

    /**
     * Words one failure as the block.
     *
     * <p>A block of labelled rows rather than one line, because these are seven separate readings
     * and the one that matters varies by report - a mismatch is diagnosed off the site and the
     * broken member, an "is it even mine" question off the versions. Run together they are a
     * sentence nobody finishes reading, and the values a reader is comparing across two logs do not
     * line up under each other.
     *
     * @param failure what stopped holding
     * @return a heading and seven labelled rows, newline separated
     */
    static String describe(CompatibilityFailure failure) {

        var subject = failure.subject();
        var consumer = failure.consumer();
        var breakage = failure.breakage();

        return subject.name() + HEADING_SUFFIX
            + BUILT_AGAINST_ROW + subject.describeBuiltAgainstVersion(UNKNOWN_VERSION)
            + INSTALLED_ROW + subject.describeInstalledVersion(UNKNOWN_VERSION)
            + FAILED_WHILE_ROW + breakage.failureSite()
            + BROKEN_ROW + breakage.brokenDetail()
            + MOD_ROW + failure.describeMod()
            + CONSUMER_ROW + consumer.consumerKey()
            + EFFECT_ROW + consumer.lostFeature();
    }
}
