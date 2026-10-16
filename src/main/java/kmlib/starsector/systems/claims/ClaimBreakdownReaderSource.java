package kmlib.starsector.systems.claims;

import kmlib.starsector.markets.colonies.KnownColonyReader;
import kmlib.starsector.systems.SectorPassIndex;

/**
 * Source port for a {@link ClaimBreakdownReader} bound to one pass's colony walk.
 *
 * <p>A {@link ClaimReaderSource} whose readers can also explain the claims they read. A caller that
 * colours by claims and explains them holds one of these, so the fill and the explanation are
 * opened through one binding. Handed where a {@link ClaimReaderSource} is asked for, it opens the
 * same reader there.
 */
@FunctionalInterface
public interface ClaimBreakdownReaderSource
        extends ClaimReaderSource {

    /**
     * Opens a reader answering off one pass's colony walk, under that pass's own knowledge.
     *
     * @param knownColonyReader what the opened reader asks which of the colonies it meets the
     *                          player may be told about, carried onto the breakdowns it builds
     * @param sectorIndex       the pass's colony index, discarded with the pass that opened it
     * @return a reader reading claims and their breakdowns out of that index
     */
    @Override
    ClaimBreakdownReader openReaderOver(
        KnownColonyReader knownColonyReader,
        SectorPassIndex sectorIndex);
}
