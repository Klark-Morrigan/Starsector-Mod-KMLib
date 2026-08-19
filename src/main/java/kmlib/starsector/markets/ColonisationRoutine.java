package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

/**
 * A colonisation sequence belonging to something other than this library - the routine an installed
 * mod founds its own colonies with, offered a founding before the game's own sequence is composed.
 *
 * <p>It exists because a colony is more than a market with people on it once a mod is running its
 * own colonisation. Such a mod records things about a colony that nothing can put there afterwards:
 * which faction founded it, whether its trading counters follow that mod's rules rather than the
 * game's, the condition its own diplomacy reads it through. A colony founded by transcribing the
 * game's sequence on that install is one the mod will never fully recognise, so where such a
 * routine is present it takes the whole founding rather than having ours layered under it.
 *
 * <p>One method that answers whether it acted, rather than a question and a command. A routine
 * declines for reasons the caller has an answer to - the mod is not installed, or the body is not
 * the shape its routine can found on - and reporting the decline is what lets the composed sequence
 * run in its place instead of a colonisation being quietly skipped. Split in two, the pair would
 * also be open to a caller asking and then not calling, which is a colony founded by nobody.
 *
 * <p>Deliberately not part of the library's public surface. It is the seam that lets the branch be
 * posed both ways without a mod installed, not an extension point: a routine is bound here because
 * this library knows how to defer to that mod, and a caller supplying its own would be choosing a
 * colonisation the rest of the library cannot reason about.
 */
@FunctionalInterface
interface ColonisationRoutine {

    /**
     * Founds the colony if this routine is the one that should, and says whether it did.
     *
     * @param sector    the sector the colony is founded in
     * @param market    the survey data to found on
     * @param factionId the owner the colony is founded under
     * @return true when this routine founded the colony and nothing further is to be done to the
     *         market; false when it declined, leaving the market exactly as it was for the caller
     *         to found itself
     */
    boolean establishColony(SectorAPI sector, MarketAPI market, String factionId);
}
