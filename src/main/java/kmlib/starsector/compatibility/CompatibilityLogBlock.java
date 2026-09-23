package kmlib.starsector.compatibility;

/**
 * A failed binding as the block written to the log: the same rows the player's notice carries, in
 * the same order and under the same labels, without the notice's advice or emphasis.
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
    // localisation - the player-facing word lives in strings.json instead.
    private static final String UNKNOWN_VERSION = "unknown";

    // The same heading the player's notice carries, so one failure does not arrive under two names
    // for the reader holding both. Without the notice's pointer at the log, which a log need not
    // make to itself.
    private static final String HEADING = "Error integrating %s with %s.";

    // The rows, each carrying its own newline and indent, and each label padded so the values line
    // up in a column. Padded in the literal rather than formatted to a width, so the alignment is
    // visible where it is written and a row added beside these is lined up by eye rather than by a
    // constant whose effect is somewhere else.
    private static final String MOD_ROW = "\n    Mod:           ";
    private static final String INTEGRATION_ROW = "\n    Integration:   ";
    private static final String TARGETED_ROW = "\n    Targeted:      ";
    private static final String DETECTED_ROW = "\n    Detected:      ";
    private static final String BROKEN_ROW = "\n    Broken:        ";
    private static final String FAILED_WHILE_ROW = "\n    Failed while:  ";
    private static final String EFFECT_ROW = "\n    Effect:        ";
    private static final String NO_EFFECT_ROW = "\n    No effect:     ";

    private CompatibilityLogBlock() {
    }

    /**
     * Words one failure as the block.
     *
     * <p>A block of labelled rows rather than one line, because these are separate readings and
     * the one that matters varies by report - a mismatch is diagnosed off the site and the broken
     * member, an "is it even mine" question off the versions. Run together they are a sentence
     * nobody finishes reading, and the values a reader is comparing across two logs do not line up
     * under each other.
     *
     * @param failure what stopped holding
     * @return a heading and the labelled rows, newline separated
     */
    static String describe(CompatibilityFailure failure) {

        var subject = failure.subject();
        var consumer = failure.consumer();
        var breakage = failure.breakage();

        var block = new StringBuilder()
            .append(String.format(HEADING, failure.describeModName(), subject.name()))
            .append(MOD_ROW).append(failure.describeMod())
            .append(INTEGRATION_ROW).append(consumer.consumerKey())
            .append(TARGETED_ROW).append(subject.describeBuiltAgainstVersion(UNKNOWN_VERSION))
            .append(DETECTED_ROW).append(subject.describeInstalledVersion(UNKNOWN_VERSION))
            .append(BROKEN_ROW).append(breakage.brokenDetail())
            .append(FAILED_WHILE_ROW).append(breakage.failureSite())
            .append(EFFECT_ROW).append(consumer.lostFeature());

        if (consumer.hasUnaffectedFeature()) {
            block.append(NO_EFFECT_ROW).append(consumer.unaffectedFeature());
        }

        return block.toString();
    }
}
