package kmlib.starsector.systems.claims;

import kmlib.starsector.colonies.ColonyVisibility;
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
 * a caller free of the mechanic while the binding stays the one place vanilla's is named.
 *
 * <p>The visibility rule travels with the index for the same reason the index does: both are the
 * opening pass's, and a reader given one but not the other would answer about the sector the pass
 * saw under a rule the pass never stated. What the rule reaches is only what a reader's breakdowns
 * report about the player's knowledge of each market - the claimant is resolved off the unfogged
 * set whatever it says - so this is a rule handed down rather than one a binding may invent.
 */
@FunctionalInterface
public interface ClaimReaderSource {

    /**
     * Opens a reader answering off one pass's colony walk, under that pass's own rule.
     *
     * @param visibility what the player may be shown of the colonies met, carried onto the
     *                   breakdowns the opened reader builds
     * @param colonies   the pass's colony index, discarded with the pass that opened it
     * @return a reader reading claims out of that index
     */
    ClaimReader openReaderOver(ColonyVisibility visibility, SystemColoniesIndex colonies);
}
