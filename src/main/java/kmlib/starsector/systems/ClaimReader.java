package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.StarSystemAPI;

/**
 * Source port for the faction that lays claim to a star system - the same claim the vanilla
 * "establish colony" survey warns about. Reading it is a call into a third-party static
 * ({@code Misc.getClaimingFaction}) that only answers inside a running game, so callers that
 * want to reason about claims - map overlays, colony tools - depend on this role instead of the
 * static, keeping their logic decoupled and testable. {@link VanillaClaimReader} is the binding
 * that routes to the vanilla mechanic at runtime.
 *
 * <p>The port yields a faction id rather than a {@code FactionAPI} on purpose: an id is all a
 * claimant is, and it frees callers from resolving a faction handle (and the sector) just to key
 * a claim by owner.
 */
public interface ClaimReader {

    /**
     * @param system the star system to read the claimant of
     * @return the id of the faction claiming {@code system}, or {@code null} when the system is
     *         unclaimed (or cannot be read - a null system reports as unclaimed rather than
     *         throwing, since "no claim" is the state callers already handle)
     */
    String readClaimingFactionId(StarSystemAPI system);
}
