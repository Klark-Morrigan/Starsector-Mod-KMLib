package kmlib.starsector.systems.claims;

import com.fs.starfarer.api.campaign.StarSystemAPI;

/**
 * Source port for the reasoning behind a star system's claim - the scored contest between the
 * factions present and the decree that can settle it outright. {@link ClaimReader} answers
 * <em>who</em> claims a system; this answers <em>why</em>, for callers that have to justify a
 * claim to a player rather than merely colour by it.
 *
 * <p>Resolving that reasoning means walking a system's economy and reading third-party statics
 * that only answer inside a running game, so callers depend on this role rather than on the
 * computation. {@link VanillaClaimBreakdownReader} is the binding that mirrors the game's own
 * mechanic at runtime.
 *
 * <p>It extends {@link ClaimReader} and {@link ClaimDecreeReader} because a reader that can say
 * why a system is claimed can say who claims it and whether a decree did. One reader then serves a
 * fill, the box explaining it and the decree heading over that box, so none of them can be read
 * through a different binding. An implementer answers the two narrow reads without scoring the
 * system where it can, since a caller colouring a whole sector asks for nothing more.
 */
public interface ClaimBreakdownReader
        extends ClaimReader, ClaimDecreeReader {

    /**
     * @param system the star system to explain the claim of
     * @return the full scored breakdown; a null or unreadable system reports
     *         {@link SystemClaimBreakdown#NONE} rather than throwing, since "nothing claims
     *         this" is a state callers already handle
     */
    SystemClaimBreakdown readBreakdown(StarSystemAPI system);
}
