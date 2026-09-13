package kmlib.testfixtures.starsector.systems.claims;

import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.starsector.systems.claims.ClaimBreakdownReader;
import kmlib.starsector.systems.claims.SystemClaimBreakdown;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link ClaimBreakdownReader} that reports breakdowns from an in-memory table keyed by
 * system ID, so claim-explaining logic can be exercised against a known contest without the
 * vanilla statics or a running game. Shipped from KMLib so both KMLib's and consuming mods'
 * tests drive the seam through one shared double.
 *
 * <p>An unmapped system - or a null system, or one with a null ID - reports
 * {@link SystemClaimBreakdown#NONE}, matching the port's "unreadable reads as nothing"
 * contract. The override read is derived from the stored breakdown rather than tracked
 * separately, so the two reads can never be set to disagree.
 */
public final class ClaimBreakdownReaderFake implements ClaimBreakdownReader {
    private final Map<String, SystemClaimBreakdown> breakdownBySystemId = new HashMap<>();

    /**
     * Records the breakdown the system with the given ID reports. A later call for the same
     * system replaces the earlier breakdown, so a test can set up exactly the contest it needs.
     */
    public void setBreakdown(String systemId, SystemClaimBreakdown breakdown) {
        breakdownBySystemId.put(systemId, breakdown);
    }

    @Override
    public SystemClaimBreakdown readBreakdown(StarSystemAPI system) {
        if (system == null || system.getId() == null) {
            return SystemClaimBreakdown.NONE;
        }
        return breakdownBySystemId.getOrDefault(system.getId(), SystemClaimBreakdown.NONE);
    }

    @Override
    public String readCoreFactionId(StarSystemAPI system) {
        return readBreakdown(system).overrideFactionId();
    }
}
