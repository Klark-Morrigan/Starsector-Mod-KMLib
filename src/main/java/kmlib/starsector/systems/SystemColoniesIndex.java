package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.text.KmlibStrings;

import java.util.HashMap;
import java.util.Map;

/**
 * The colony sets of a whole pass, each system selected at most once however many readers ask
 * about it.
 *
 * <p>A render pass asks the same question of one system several times over - who holds it, how
 * many colonies to draw for each, what to name in a hover - and answering each ask with its own
 * walk is what made a full rebuild cost several traversals per system. This is what makes the
 * shared set affordable: the walk is paid on first ask and every later reader is handed the
 * answer already computed.
 *
 * <p>Held by the readers below instead of the sector, and that substitution is the point. A
 * reader given a {@link SectorAPI} is a reader that can still walk a system again; one given
 * this cannot, so the "one walk per system per pass" rule is enforced by what a reader is able
 * to reach rather than by everyone remembering it.
 *
 * <p>Memoised lazily rather than filled up front, so a pass that touches a handful of systems
 * pays for a handful rather than for the sector. Built for one pass and discarded with it: the
 * answers are a snapshot, and a set that outlived its pass would keep reporting a sector that
 * has since moved on. Not safe for concurrent use, a pass being one thread's work.
 */
public final class SystemColoniesIndex {

    private final Map<String, SystemColonies> coloniesBySystemId = new HashMap<>();
    private final SectorAPI sector;

    // Built on the first ask made by id alone, and only then: a pass that always asks with the
    // system in hand never needs the sector's systems indexed at all.
    private Map<String, StarSystemAPI> systemById;

    /**
     * Opens an index over {@code sector} for one pass.
     *
     * @param sector the sector every read is made against; null yields an index answering an
     *               empty set for every system, matching how the reads below treat an
     *               unreachable sector
     */
    public SystemColoniesIndex(SectorAPI sector) {
        this.sector = sector;
    }

    /**
     * The colonies in {@code system}, walked on the first ask and remembered thereafter.
     *
     * @param system the system to read; null yields {@link SystemColonies#NONE}
     * @return the system's colony set
     */
    public SystemColonies readColoniesIn(StarSystemAPI system) {

        if (system == null) {
            return SystemColonies.NONE;
        }
        var systemId = system.getId();

        if (!KmlibStrings.hasText(systemId)) {
            // Nothing to key the memo on. Reading afresh costs a walk a second ask would have
            // saved, which is the honest price of an unkeyable system - pooling every one of
            // them under a shared key would hand one system's colonies to another.
            return SystemColonies.readColoniesIn(sector, system);
        }
        return coloniesBySystemId.computeIfAbsent(
            systemId,
            id -> SystemColonies.readColoniesIn(sector, system));
    }

    /**
     * The colonies in the system with this id, for a reader holding an id rather than a system
     * - which is what a pass keyed by system id mostly holds.
     *
     * <p>Answers off the memo before resolving the id at all, so a system already read through
     * {@link #readColoniesIn} is never walked again for having been asked about the other way.
     *
     * @param systemId the system id, as {@code StarSystemAPI#getId} reports it; null or blank
     *                 yields {@link SystemColonies#NONE}
     * @return the system's colony set, or {@link SystemColonies#NONE} when no system has that id
     */
    public SystemColonies readColoniesById(String systemId) {

        if (!KmlibStrings.hasText(systemId)) {
            return SystemColonies.NONE;
        }
        var memoisedColonies = coloniesBySystemId.get(systemId);

        if (memoisedColonies != null) {
            return memoisedColonies;
        }
        return readColoniesIn(findSystemById(systemId));
    }

    // The system carrying this id, resolving the sector's systems once and keeping the result.
    // Matched on getId for the same reason StarSystems.indexById is: vanilla's own lookup
    // matches the optional unique id first and silently misses a system keyed by its base name.
    private StarSystemAPI findSystemById(String systemId) {

        if (systemById == null) {
            systemById = StarSystems.indexById(sector);
        }
        return systemById.get(systemId);
    }
}
