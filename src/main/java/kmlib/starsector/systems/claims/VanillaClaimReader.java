package kmlib.starsector.systems.claims;

import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;

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
 */
public final class VanillaClaimReader implements ClaimReader {

    private final ClaimBreakdownReader breakdownReader = new VanillaClaimBreakdownReader();

    @Override
    public String readClaimingFactionId(StarSystemAPI system) {
        return breakdownReader.readBreakdown(system).claimantFactionId();
    }
}
