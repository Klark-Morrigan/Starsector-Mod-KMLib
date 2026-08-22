package kmlib.starsector.systems.claims;

import com.fs.starfarer.api.campaign.StarSystemAPI;

/**
 * Source port for the faction that lays claim to a star system - the same claim the vanilla
 * "establish colony" survey warns about. Resolving it means walking the system's economy and
 * reading third-party statics that only answer inside a running game, so callers that want to
 * reason about claims - map overlays, colony tools - depend on this role instead of that
 * computation, keeping their logic decoupled from it. {@link VanillaClaimBreakdownReader} is the
 * binding that answers by the vanilla mechanic at runtime.
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
