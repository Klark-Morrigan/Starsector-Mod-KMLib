package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.starsector.SectorWalkCounters;
import kmlib.starsector.colonies.Colonies;
import kmlib.starsector.colonies.SystemColonies;
import kmlib.text.KmlibStrings;

import java.util.Collections;
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

    // A read is of one system, and the counter takes an amount rather than a call.
    private static final long ONE_SYSTEM = 1L;

    private final Map<String, Colonies> coloniesBySystemId = new HashMap<>();
    private final SectorAPI sector;

    // Built on the first ask that needs the sector's systems resolved, and only then: a pass whose
    // readers all hold the system in hand never indexes the sector at all.
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
     * The sector every read below is made against.
     *
     * <p>Named here so a caller holding an index needs no sector of its own beside it. Two
     * fields for one fact are two things to keep in step, and the pair that drifted would have
     * one surface reading a system out of one sector while another priced it against a second.
     *
     * @return the sector this index was opened over; null when it was opened over none
     */
    public SectorAPI getSector() {
        return sector;
    }

    /**
     * The colonies in {@code system}, walked on the first ask and remembered thereafter.
     *
     * @param system the system to read; null yields {@link Colonies#NONE}
     * @return the system's colony set
     */
    public Colonies readColoniesIn(StarSystemAPI system) {

        if (system == null) {
            return Colonies.NONE;
        }
        var systemId = system.getId();

        if (!KmlibStrings.hasText(systemId)) {
            // Nothing to key the memo on. Reading afresh costs a walk a second ask would have
            // saved, which is the honest price of an unkeyable system - pooling every one of
            // them under a shared key would hand one system's colonies to another.
            return readAndCountColoniesIn(system);
        }
        return coloniesBySystemId.computeIfAbsent(
            systemId,
            id -> readAndCountColoniesIn(system));
    }

    /**
     * The colonies in the system with this id, for a reader holding an id rather than a system
     * - which is what a pass keyed by system id mostly holds.
     *
     * <p>Answers off the memo before resolving the id at all, so a system already read through
     * {@link #readColoniesIn} is never walked again for having been asked about the other way.
     *
     * @param systemId the system id, as {@code StarSystemAPI#getId} reports it; null or blank
     *                 yields {@link Colonies#NONE}
     * @return the system's colony set, or {@link Colonies#NONE} when no system has that id
     */
    public Colonies readColoniesById(String systemId) {

        if (!KmlibStrings.hasText(systemId)) {
            return Colonies.NONE;
        }
        var memoisedColonies = coloniesBySystemId.get(systemId);

        if (memoisedColonies != null) {
            return memoisedColonies;
        }
        return readColoniesIn(findSystemById(systemId));
    }

    /**
     * The sector's star systems keyed by id, traversed on the first ask and remembered thereafter.
     *
     * <p>Published for the same reason the colony sets are: a reader resolving ids off this pass
     * would otherwise open a traversal of its own, and two traversals for one pass is exactly what
     * an index exists to stop. The pass's readers share this one whether they came for a system or
     * for its colonies.
     *
     * @return each system keyed by its id, in the sector's star-system order; empty for an index
     *         opened over no sector. Read-only - the map is the index's own memo
     */
    public Map<String, StarSystemAPI> readSystemsById() {

        if (systemById == null) {
            systemById = Collections.unmodifiableMap(SectorStarSystems.indexById(sector));
        }
        return systemById;
    }

    // One system read for real, counted as the visit it is. Only the misses reach here, which is
    // what makes the counter say how many systems a pass actually walked rather than how many
    // times it asked - the difference between the two being the whole point of holding an index.
    private Colonies readAndCountColoniesIn(StarSystemAPI system) {

        SectorWalkCounters.countSystemsVisited(ONE_SYSTEM);

        return SystemColonies.readColoniesIn(sector, system);
    }

    // The system carrying this id, off the one index every reader of this pass shares. Matched on
    // getId for the same reason SectorStarSystems.indexById is: vanilla's own lookup matches the
    // optional unique id first and silently misses a system keyed by its base name.
    private StarSystemAPI findSystemById(String systemId) {
        return readSystemsById().get(systemId);
    }
}
