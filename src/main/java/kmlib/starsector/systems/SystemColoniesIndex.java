package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.starsector.SectorWalkCounters;
import kmlib.starsector.markets.colonies.Colonies;
import kmlib.starsector.markets.colonies.SystemColonies;
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

    private final Map<SystemKey, Colonies> coloniesBySystemKey = new HashMap<>();
    private final SectorAPI sector;

    // Each built on the first ask that needs the sector's systems resolved that way, and only then:
    // a pass whose readers all hold the system in hand indexes the sector neither way, and one that
    // never asks by id pays for no id index.
    private Map<String, StarSystemAPI> systemById;
    private Map<SystemKey, StarSystemAPI> systemByKey;

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
        // Memoised under the whole key rather than under the id, because an id is not unique: a
        // sector holding two systems under one id would otherwise pool them into a single entry and
        // hand the first one's colonies to the second. Every system has a key, and a system missing
        // both entity arms still has one, so there is no shape left that cannot be memoised.
        return coloniesBySystemKey.computeIfAbsent(
            SystemKey.readKeyOf(system),
            key -> readAndCountColoniesIn(system));
    }

    /**
     * The colonies in the system with this id, for a reader holding an id rather than a system
     * - which is what a pass keyed by system id mostly holds.
     *
     * <p>Kept on the bare id because this is the arm anything addressed from outside speaks - an
     * override table, a saved preference, a console argument. An id is not unique, so this answers
     * about the first system carrying it, the one {@link #readSystemsById} holds.
     *
     * <p>Resolves the id to a system and then reads through {@link #readColoniesIn}, so a system
     * already read that way is never walked again for having been asked about the other way. The
     * resolution comes first because the colony memo is keyed by {@link SystemKey}, which an id
     * alone cannot address.
     *
     * @param systemId the system id, as {@code StarSystemAPI#getId} reports it; null or blank
     *                 yields {@link Colonies#NONE}
     * @return the colony set of the first system with that id, or {@link Colonies#NONE} when no
     *         system has it
     */
    public Colonies readColoniesById(String systemId) {

        if (!KmlibStrings.hasText(systemId)) {
            return Colonies.NONE;
        }
        // Resolved through the id index rather than by a match written here, so what an id answers
        // is decided in one place: the index and SectorStarSystems#findSystemById both name the
        // first system under a repeated id, and a third rule here could only disagree with them.
        return readColoniesIn(readSystemsById().get(systemId));
    }

    /**
     * The sector's star systems keyed by id, traversed on the first ask and remembered thereafter -
     * the read for a caller holding an id, which is what anything addressed from outside holds.
     *
     * <p>Published for the same reason the colony sets are: a reader resolving ids off this pass
     * would otherwise open a traversal of its own, and two traversals for one pass is exactly what
     * an index exists to stop. The pass's readers share this one whether they came for a system or
     * for its colonies.
     *
     * <p>Not every system in the sector. An id is not unique and a modded install holds several
     * systems sharing one, of which this holds the first - the same one
     * {@link SectorStarSystems#findSystemById} answers with, so an id names one system whichever
     * way it is asked. A pass that must account for every system asks {@link #readSystemsByKey}.
     *
     * @return the first system found under each id, in the sector's star-system order; empty for an
     *         index opened over no sector. Read-only - the map is the index's own memo
     */
    public Map<String, StarSystemAPI> readSystemsById() {

        if (systemById == null) {
            systemById = Collections.unmodifiableMap(SectorStarSystems.indexById(sector));
        }
        return systemById;
    }

    /**
     * Every one of the sector's star systems keyed by its {@link SystemKey}, traversed on the first
     * ask and remembered thereafter - the read for a pass that must account for each system rather
     * than for each id.
     *
     * <p>What separates it from {@link #readSystemsById}: a key is unique where an id is not, so an
     * install holding several systems under one id has an entry here for each of them. A pass built
     * on the id index is short those systems in everything it derives, and the loss reads as a
     * system missing from the map with nothing naming its cause.
     *
     * @return each system keyed by its key, in the sector's star-system order; empty for an index
     *         opened over no sector. Read-only - the map is the index's own memo
     */
    public Map<SystemKey, StarSystemAPI> readSystemsByKey() {

        if (systemByKey == null) {
            systemByKey = Collections.unmodifiableMap(SectorStarSystems.indexByKey(sector));
        }
        return systemByKey;
    }

    // One system read for real, counted as the visit it is. Only the misses reach here, which is
    // what makes the counter say how many systems a pass actually walked rather than how many
    // times it asked - the difference between the two being the whole point of holding an index.
    private Colonies readAndCountColoniesIn(StarSystemAPI system) {

        SectorWalkCounters.countSystemsVisited(ONE_SYSTEM);

        return SystemColonies.readColoniesIn(sector, system);
    }
}
