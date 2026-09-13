package kmlib.testfixtures.starsector.systems.claims;

import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.starsector.systems.claims.ClaimReader;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link ClaimReader} that reports claims from an in-memory table keyed by system ID, so claim
 * logic can be exercised against a known set of claimants without the vanilla {@code Misc} static
 * or a running game. Shipped from KMLib so both KMLib's and consuming mods' tests drive the claim
 * seam through one shared double.
 *
 * <p>An unmapped system - or a null system, or one with a null ID - reports as unclaimed, matching
 * the port's "no claim reads as {@code null}" contract.
 */
public final class ClaimReaderFake implements ClaimReader {
    private final Map<String, String> claimantIdBySystemId = new HashMap<>();

    /**
     * Records that {@code claimantFactionId} claims the system with the given id. A later call for
     * the same system replaces the earlier claim, so a test can set up exactly the claimants it
     * needs.
     */
    public void setClaim(String systemId, String claimantFactionId) {
        claimantIdBySystemId.put(systemId, claimantFactionId);
    }

    @Override
    public String readClaimingFactionId(StarSystemAPI system) {
        if (system == null || system.getId() == null) {
            return null;
        }
        return claimantIdBySystemId.get(system.getId());
    }
}
