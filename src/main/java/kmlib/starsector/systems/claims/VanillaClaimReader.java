package kmlib.starsector.systems.claims;

import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;

import kmlib.starsector.colonies.ColonyVisibility;
import kmlib.starsector.systems.SystemColoniesIndex;

/**
 * {@link ClaimReader} binding backed by vanilla's claim mechanic - the same claim the colony
 * survey panel warns a player about when they settle in another faction's space, so claims
 * surface here as the base game defines them.
 *
 * <p>The claimant is taken from {@link VanillaClaimBreakdownReader}, which mirrors
 * {@link Misc#getClaimingFaction} rather than calling it. One computation therefore answers
 * both "who claims this" and "why", so a map coloured by this reader and a tooltip explaining
 * that colour cannot contradict each other - not on the override, and not on the
 * iteration-order tie the mechanic resolves ties by.
 *
 * <p>The two constructors carry the breakdown reader's own choice about where its colonies come
 * from, since that is the whole of what this adds to it: a reader built for one pass shares that
 * pass's walk of each system, and one built to outlive any pass walks afresh rather than answering
 * off a snapshot nothing refreshes.
 */
public final class VanillaClaimReader implements ClaimReader {

    private final ClaimBreakdownReader breakdownReader;

    /** A reader with no pass behind it, walking each system afresh on every ask. */
    public VanillaClaimReader() {
        this(null);
    }

    /**
     * A reader sharing one pass's colony walk.
     *
     * @param coloniesIndex the pass's colony index, discarded with the pass that opened it;
     *                      null reads each system afresh, as the no-index reader does
     */
    public VanillaClaimReader(SystemColoniesIndex coloniesIndex) {
        // The breakdown reader's visibility rule decides only what its breakdowns report about
        // the player's knowledge of each market, and no breakdown leaves this class - only the
        // claimant does, resolved off the unfogged set. So the rule that adds nothing is the
        // honest one to state here: any other would imply this reader had a fog to apply.
        breakdownReader =
            new VanillaClaimBreakdownReader(ColonyVisibility.BASE_FOG, coloniesIndex);
    }

    @Override
    public String readClaimingFactionId(StarSystemAPI system) {
        return breakdownReader.readBreakdown(system).claimantFactionId();
    }
}
