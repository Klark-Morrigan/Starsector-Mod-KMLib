package kmlib.starsector.compatibility;

import kmlib.starsector.compatibility.CompatibilityNoticeLine.Emphasis;
import kmlib.starsector.strings.KmlibStringKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static kmlib.starsector.compatibility.CompatibilityNoticeLines.bringForward;
import static kmlib.starsector.compatibility.CompatibilityNoticeLines.buildLine;
import static kmlib.starsector.compatibility.CompatibilityNoticeLines.buildPhrase;
import static kmlib.starsector.compatibility.CompatibilityNoticeLines.buildSplicedLine;
import static kmlib.starsector.compatibility.CompatibilityNoticeLines.warn;
import static kmlib.starsector.compatibility.NoticeParts.buildConsequenceRows;
import static kmlib.starsector.compatibility.NoticeParts.buildRow;

/**
 * One of a mod's own features that threw and was switched off, in the shape the notice and the log
 * block are both composed from.
 *
 * <p>Needed because a mod that catches its own failure and carries on leaves the player looking at
 * a feature that is simply missing. Without a notice, the only trace is in the log, and a player
 * has no reason to look there.
 *
 * <p>There is no third party here and no versions to compare, so the notice has no diagnosis to
 * choose between. It says the fault is the mod's own and asks for a report, which is the one thing
 * the player can usefully do.
 *
 * @param consumer the mod whose feature failed: its ID, the feature's key, and its sentences on
 *                 what the player loses and what still works
 * @param cause    what the feature threw. Never null: a feature is switched off because something
 *                 was thrown, and the log block is not enough on its own to find where
 */
public record FeatureFailure(
    CompatibilityConsumer consumer,
    Throwable cause)
        implements ReportedFailure {

    public FeatureFailure {

        Objects.requireNonNull(
            consumer,
            "A feature failure with no consumer could not say whose feature it was.");

        Objects.requireNonNull(
            cause,
            "A feature failure with no cause leaves the log with no trace to find the fault from.");
    }

    /**
     * The paragraph the notice opens with: which mod ran into an error.
     *
     * @return the heading, bringing the mod forward and warning on the phrase naming the failure
     */
    @Override
    public CompatibilityNoticeLine describeHeadingForPlayer() {

        var modName = NoticeParts.describeModName(consumer);
        var failurePhrase = KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_FEATURE_TITLE_ERROR);

        return buildLine(
            KmlibStringKeys.format(KmlibStringKeys.COMPATIBILITY_NOTICE_FEATURE_TITLE, modName, failurePhrase),
            bringForward(modName),
            warn(failurePhrase));
    }

    /**
     * The one thing the player can do: report it to the mod's author.
     *
     * <p>Says the fault is the mod's own before asking. A player who has seen the compatibility
     * notice would otherwise go looking for a version to change, and there is none.
     *
     * @return the diagnosis, one line
     */
    @Override
    public List<CompatibilityNoticeLine> describeDiagnosisForPlayer() {

        var modName = NoticeParts.describeModName(consumer);

        var reportPhrase = buildPhrase(
            KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_PHRASE_REPORT_TO_DEVELOPER),
            Emphasis.WARNING,
            bringForward(modName));

        return List.of(buildSplicedLine(
            KmlibStringKeys.format(
                KmlibStringKeys.COMPATIBILITY_NOTICE_FEATURE_DIAGNOSIS,
                modName,
                reportPhrase.lineText()),
            List.of(bringForward(modName)),
            reportPhrase.emphasisedRuns()));
    }

    /**
     * The notice's body as labelled rows, in reading order: the mod, the feature, what is lost and
     * what is not.
     *
     * @return the rows, never empty
     */
    @Override
    public List<CompatibilityNoticeLine> describeRowsForPlayer() {

        var rows = new ArrayList<CompatibilityNoticeLine>();

        rows.add(buildRow(KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_MOD, NoticeParts.describeMod(consumer)));
        rows.add(buildRow(KmlibStringKeys.COMPATIBILITY_NOTICE_ROW_FEATURE, consumer.consumerKey()));
        rows.addAll(buildConsequenceRows(consumer));

        return rows;
    }

    /**
     * The same failure as the block written to the log. Composed by {@link CompatibilityLogBlock},
     * which is where that block's vocabulary and alignment live.
     *
     * @return a heading and the labelled rows, newline separated
     */
    @Override
    public String describeForLog() {

        return CompatibilityLogBlock.describe(this);
    }
}
