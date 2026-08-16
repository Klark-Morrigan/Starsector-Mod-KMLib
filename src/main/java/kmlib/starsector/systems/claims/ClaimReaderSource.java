package kmlib.starsector.systems.claims;

import kmlib.starsector.systems.SystemColoniesIndex;

/**
 * Source port for a {@link ClaimReader} bound to one pass's colony walk - what a caller holds
 * when it needs a reader per pass rather than a reader for the life of the game.
 *
 * <p>A reader is only as current as the colonies behind it. An index is a snapshot of the sector
 * the pass that opened it saw, so a reader built over one has to be discarded with that pass; a
 * caller that instead held a single reader would go on answering off a sector that has since
 * moved on. Holding this rather than a reader is what makes that impossible: there is no reader
 * to keep, only the means of opening one when a pass exists to open it for.
 *
 * <p>A port rather than a direct call on the vanilla binding for the usual reason - a caller
 * depending on this can be handed a reader with no running game behind it, which is what keeps
 * the callers testable while the binding stays the one place vanilla's mechanic is named.
 */
@FunctionalInterface
public interface ClaimReaderSource {

    /**
     * Opens a reader answering off one pass's colony walk.
     *
     * @param colonies the pass's colony index, discarded with the pass that opened it
     * @return a reader reading claims out of that index
     */
    ClaimReader openReaderOver(SystemColoniesIndex colonies);
}
