package kmlib.starsector.systems.claims;

import com.fs.starfarer.api.campaign.StarSystemAPI;

/**
 * Source port for a star system's decree: the faction its claiming-faction memory flag imposes,
 * which settles the system before any market is weighed.
 *
 * <p>A port of its own because the decree is one memory read and needs no colony walk. A caller
 * asking only whether a system is held by decree holds this and pays for nothing else.
 * {@code StarSystems::readDecreedFactionId} is the vanilla binding, and every
 * {@link ClaimBreakdownReader} answers it too.
 */
@FunctionalInterface
public interface ClaimDecreeReader {

    /**
     * @param system the star system to read the decree of
     * @return the faction ID the flag names, as written - blank, or naming no faction, where the
     *         flag says so - or null when no decree is set or the system cannot be read
     */
    String readDecreedFactionId(StarSystemAPI system);
}
