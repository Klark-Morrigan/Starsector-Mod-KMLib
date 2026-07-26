package kmlib.starsector.systems.claims;

import com.fs.starfarer.api.campaign.StarSystemAPI;

/**
 * Source port for the reasoning behind a star system's claim - the scored contest between the
 * factions present and the override that can settle it outright. {@link ClaimReader} answers
 * <em>who</em> claims a system; this answers <em>why</em>, for callers that have to justify a
 * claim to a player rather than merely colour by it.
 *
 * <p>Resolving that reasoning means walking a system's economy and reading third-party statics
 * that only answer inside a running game, so callers depend on this role rather than on the
 * computation. {@link VanillaClaimBreakdownReader} is the binding that mirrors the game's own
 * mechanic at runtime.
 *
 * <p>The two reads are separate because they cost wildly different amounts: the override is a
 * single memory read, while the breakdown scores every market in the system. A caller that
 * only needs to know whether a system is held by decree should not pay for the scoring.
 */
public interface ClaimBreakdownReader {

    /**
     * @param system the star system to explain the claim of
     * @return the full scored breakdown; a null or unreadable system reports
     *         {@link SystemClaimBreakdown#NONE} rather than throwing, since "nothing claims
     *         this" is a state callers already handle
     */
    SystemClaimBreakdown readBreakdown(StarSystemAPI system);

    /**
     * @param system the star system to read the claiming-faction override of
     * @return the id of the faction the system's claiming-faction memory flag imposes, or
     *         {@code null} when no override is set (or the system cannot be read)
     */
    String readCoreFactionId(StarSystemAPI system);
}
