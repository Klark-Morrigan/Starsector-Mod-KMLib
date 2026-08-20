package kmlib.starsector.markets.colonisation;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.extensions.WorkOutcome;

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
 * <p>What a routine here declines for: the mod is not installed, or the body is not the shape its
 * own colonisation can found on. Both leave the market exactly as it was, so the composed sequence
 * founds the colony in its place.
 */
@FunctionalInterface
public interface ColonisationRoutine {

    /**
     * Founds the colony if this routine is the one that should, and says whether it did.
     *
     * <p>The size is stated by the caller rather than by the routine, so a colony is the same size
     * whichever routine founded it - the one property of a founding that would otherwise be
     * decided twice, once here and once by the sequence this stands in for.
     *
     * @param sector     the sector the colony is founded in
     * @param market     the survey data to found on
     * @param factionId  the owner the colony is founded under
     * @param colonySize the size the colony is founded at
     * @return a performed founding, or a decline saying what about this call it could not do -
     *         which the caller reads back to whoever is diagnosing the install
     */
    WorkOutcome establishColony(
        SectorAPI sector,
        MarketAPI market,
        String factionId,
        int colonySize);
}
