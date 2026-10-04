package kmlib.starsector.compatibility;

import java.util.List;

/**
 * Something a mod lost this session, in the shape the notice shown to the player and the block
 * written to the log are both composed from.
 *
 * <p>Two things end up here. A binding to third-party code can stop holding, which is a
 * {@link CompatibilityFailure}. Or a mod's own feature can fail and be switched off, which is a
 * {@link FeatureFailure}. Either way the player is left without something and should be told what.
 * The two say different things, but both surfaces draw them the same way, so they draw this.
 *
 * <p>Sealed so that every kind of report is one this package words. A surface relies on each line
 * coming with its emphasis already named, and a kind written elsewhere could not promise that.
 */
public sealed interface ReportedFailure
        permits CompatibilityFailure, FeatureFailure {

    /**
     * @return the mod that lost something, as the record filed it
     */
    CompatibilityConsumer consumer();

    /**
     * @return what was thrown, or {@code null} where nothing was. Handed to the logger beside
     *         {@link #describeForLog()}, which is where a stack trace belongs
     */
    Throwable cause();

    /**
     * @return the paragraph the notice opens with
     */
    CompatibilityNoticeLine describeHeadingForPlayer();

    /**
     * @return what the player can do about it, in reading order, empty where there is nothing to
     *         advise
     */
    List<CompatibilityNoticeLine> describeDiagnosisForPlayer();

    /**
     * @return the notice's body as labelled rows, in reading order, never empty
     */
    List<CompatibilityNoticeLine> describeRowsForPlayer();

    /**
     * @return the same report as the block written to the log, built from literals so it reads the
     *         same in every localisation
     */
    String describeForLog();

    /**
     * The line the notice closes with, pointing at the log. The same for every report, since every
     * reporter writes the log block before it shows anything.
     *
     * @return the closing line, bringing the log's name forward
     */
    default CompatibilityNoticeLine describeClosingForPlayer() {

        return NoticeParts.describeClosing();
    }

    /**
     * The whole notice as plain text, for a surface that takes one string: the heading, the
     * diagnosis where there is one, the rows, and the closing line, a blank line apart.
     *
     * @return the notice with its emphasis dropped
     */
    default String describeForPlayer() {

        return NoticeParts.composePlainNotice(this);
    }
}
