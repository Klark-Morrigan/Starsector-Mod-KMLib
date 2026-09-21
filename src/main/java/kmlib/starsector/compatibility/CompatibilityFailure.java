package kmlib.starsector.compatibility;

import kmlib.starsector.settings.modmanager.InstalledMods;
import kmlib.starsector.strings.KmlibStringKeys;

import java.util.ArrayList;
import java.util.Objects;

/**
 * One binding to third-party code that stopped holding, in the shape the block shown to the player
 * and the block written to the log are both composed from.
 *
 * <p>Kept apart from the registry that stores it so the wording can be read without one, and so the
 * registry itself carries no formatting. What the two sentences say and why is documented on
 * the strings that render them, in {@link KmlibStringKeys}; this record only fills their slots.
 *
 * @param subject   the third party and the two versions the mismatch is stated as
 * @param consumer  the mod that took the binding, as the record filed it: the key it was recorded
 *                  under - numbered where that mod had already used the key for another feature -
 *                  for the log, and the sentence naming what it loses, for the player. Held whole
 *                  rather than unpacked into its sentence, so a report can say which mod lost
 *                  something as well as what - two mods over one broken binding file two reports,
 *                  and told apart by their sentences alone they are two the reader has to
 *                  recognise by prose
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

    // How a report names a mod whose display name the game could answer for.
    private static final String NAME_WITH_ID = "%s (%s)";

    // Between the two sentences of the heading paragraph: which subject mismatched, and where to
    // look. One paragraph rather than two, the second being about the first.
    private static final String HEADING_SENTENCE_BREAK = " ";

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
        rows.add(KmlibStringKeys.format(KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_MOD, describeMod()));
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
     * The same failure as the block written to the log, carrying the mechanics the modal leaves
     * out. Composed by {@link CompatibilityLogBlock}, which is where that block's vocabulary and
     * alignment live.
     *
     * @return a heading and seven labelled rows, newline separated
     */
    public String describeForLog() {

        return CompatibilityLogBlock.describe(this);
    }

    /**
     * Names the consuming mod for a report: its own name beside its ID where the game holds one,
     * and the ID alone where it does not.
     *
     * <p>Resolved here rather than on {@link CompatibilityConsumer}, which stays plain data: a
     * value that read the mod manager to be read itself could not be built or asserted without a
     * running game, and this is the only place the answer is wanted. Resolved at report time rather
     * than held, so the healthy path never reads the mod manager and a report composed before the
     * game is up still names something. An ID the game lists no mod for shows as itself, which is
     * how a misspelled or invented one surfaces.
     *
     * @return the mod as a report names it, never blank
     */
    String describeMod() {

        var modName = InstalledMods.readModName(consumer.modId());

        return modName == null
            ? consumer.modId()
            : String.format(NAME_WITH_ID, modName, consumer.modId());
    }

    // Two templates rather than one with a hole in it: "installed: (version unknown)" states a
    // reading that was never taken, where the second wording says what happened and what it implies.
    private String describeInstalledVersionRow() {

        return subject.hasInstalledVersion()
            ? KmlibStringKeys.format(
                KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_INSTALLED,
                subject.describeInstalledVersion(
                    KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_VERSION_UNKNOWN)))
            : KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_INSTALLED_UNREADABLE);
    }
}
