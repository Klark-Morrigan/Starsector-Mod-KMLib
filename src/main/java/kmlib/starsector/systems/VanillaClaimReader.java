package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * {@link ClaimReader} binding backed by vanilla's claim mechanic,
 * {@link Misc#getClaimingFaction}. That is the exact call the colony survey panel uses to warn a
 * player which faction claims the system they are settling in, so this binding surfaces claims
 * as the base game defines them, with no parallel derivation of our own.
 *
 * <p>{@code Misc.getClaimingFaction} works off a {@link com.fs.starfarer.api.campaign.SectorEntityToken}
 * and reads the system it belongs to, so the system's centre is the token handed to it.
 */
public final class VanillaClaimReader implements ClaimReader {

    /**
     * Reads the system's claimant through {@link Misc#getClaimingFaction} and reduces it to its
     * id. A system with no centre cannot be posed to the mechanic (it resolves the claim off the
     * centre's containing system), so it reports as unclaimed rather than tripping an NPE inside
     * the static.
     */
    @Override
    public String readClaimingFactionId(StarSystemAPI system) {
        if (system == null || system.getCenter() == null) {
            return null;
        }
        FactionAPI claimant = Misc.getClaimingFaction(system.getCenter());
        return claimant == null ? null : claimant.getId();
    }
}
