package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.starsector.SectorWalkCounters;
import kmlib.starsector.markets.colonies.Colonies;
import kmlib.starsector.markets.colonies.SystemColonies;
import kmlib.text.KmlibStrings;

import java.util.Collections;
import java.util.Map;

/**
 * One pass's whole reading of a sector: which systems it holds, and the colonies in each. Every
 * question is answered off one traversal and one walk per system, however many readers ask.
 *
 * <p>A render pass asks the same question of one system several times over - who holds it, how
 * many colonies to draw for each, what to name in a hover - and answering each ask with its own
 * walk is what made a full rebuild cost several traversals per system. This is what makes the
 * shared answers affordable: the walk is paid on first ask and every later reader is handed the
 * answer already computed.
 *
 * <p>Held by the readers below instead of the sector, and that substitution is the point. A
 * reader given a {@link SectorAPI} is a reader that can still walk a system again; one given
 * this cannot, so the "one walk per system per pass" rule is enforced by what a reader is able
 * to reach rather than by everyone remembering it.
 *
 * <p>The system list and the colony sets are held together rather than in an object each, because
 * the colony reads resolve ids through that same list: split apart, a pass asking both would open
 * two traversals of it, which is the very cost an index exists to stop. The list is offered keyed
 * both ways a caller asks for it.
 *
 * <p>Memoised lazily rather than filled up front, so a pass that touches a handful of systems
 * pays for a handful rather than for the sector. Built for one pass and discarded with it: the
 * answers are a snapshot, and a reading that outlived its pass would keep reporting a sector that
 * has since moved on. Not safe for concurrent use, a pass being one thread's work.
 */
public final class SectorPassIndex {

    // A read is of one system, and the counter takes an amount rather than a call.
    private static final long ONE_SYSTEM = 1L;

    private final SystemKeyedMemo<Colonies> coloniesBySystem = new SystemKeyedMemo<>();
    private final SectorAPI sector;

    // Each built on the first ask that needs the sector's systems resolved that way, and only then:
    // a pass whose readers all hold the system in hand indexes the sector neither way. The id index
    // is taken off the key one, so a pass asking either way traverses the sector once and a pass
    // asking both ways pays the second index in a re-addressing rather than in a walk.
    private Map<String, StarSystemAPI> systemById;
    private Map<SystemKey, StarSystemAPI> systemByKey;

    /**
     * Opens an index over {@code sector} for one pass.
     *
     * @param sector the sector every read is made against; null yields an index answering an
     *               empty set for every system, matching how the reads below treat an
     *               unreachable sector
     */
    public SectorPassIndex(SectorAPI sector) {
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
     * <p>Remembered on {@link SystemKeyedMemo}'s terms: under the whole key rather than the id, and
     * walked afresh every ask for the system the sector states nothing about.
     *
     * @param system the system to read; null yields {@link Colonies#NONE}
     * @return the system's colony set
     */
    public Colonies readColoniesIn(StarSystemAPI system) {

        if (system == null) {
            return Colonies.NONE;
        }
        return coloniesBySystem.readValueFor(system, this::readAndCountColoniesIn);
    }

    /**
     * The colonies in the system with this id, for a reader holding an id rather than a system
     * - which is what a pass keyed by system id mostly holds.
     *
     * <p>Kept on the bare id because this is the arm anything addressed from outside speaks - an
     * override table, a saved preference, a console argument. Which system a repeated id names is
     * {@link #readSystemsById}'s answer, and this reads the colonies of that one.
     *
     * <p>Resolves the id to a system and then reads through {@link #readColoniesIn}, so a system
     * already read that way is never walked again for having been asked about the other way. The
     * resolution comes first because the colony memo is keyed by {@link SystemKey}, which an id
     * alone cannot address.
     *
     * @param systemId the system id, as {@code StarSystemAPI#getId} reports it; null or blank
     *                 yields {@link Colonies#NONE}
     * @return the colony set of the system that id resolves to, or {@link Colonies#NONE} when no
     *         system has it
     */
    public Colonies readColoniesById(String systemId) {

        if (!KmlibStrings.hasText(systemId)) {
            return Colonies.NONE;
        }
        // Resolved through the id index rather than by a match written here, so which system a
        // repeated id names is settled in one place rather than by a second rule free to disagree.
        return readColoniesIn(readSystemsById().get(systemId));
    }

    /**
     * {@link SectorStarSystems#indexById} held for the pass, traversed on the first ask and
     * remembered thereafter - the read for a caller holding an id, which is what anything addressed
     * from outside holds. It answers on that read's terms, this adding only the memo.
     *
     * <p>Published for the same reason the colony sets are: a reader resolving ids off this pass
     * would otherwise open a traversal of its own, and two traversals for one pass is exactly what
     * an index exists to stop. The pass's readers share this one whether they came for a system or
     * for its colonies.
     *
     * <p>Not every system in the sector; a pass that must account for each asks
     * {@link #readSystemsByKey}.
     *
     * @return what {@link SectorStarSystems#indexById} answers; empty for an index opened over no
     *         sector. Read-only - the map is the index's own memo
     */
    public Map<String, StarSystemAPI> readSystemsById() {

        if (systemById == null) {
            // Addressed off the systems the key index already holds rather than off a traversal of
            // its own. A pass whose readers ask both ways is bounded at one walk, and it is the
            // same bound whichever way its first reader asked; which system a repeated id names is
            // still settled by the read owning that rule rather than by a second one here.
            systemById = Collections.unmodifiableMap(
                SectorStarSystems.indexHeldSystemsById(readSystemsByKey().values()));
        }
        return systemById;
    }

    /**
     * {@link SectorStarSystems#indexByKey} held for the pass, traversed on the first ask and
     * remembered thereafter - the read for a pass that must account for every system rather than
     * for every id. It answers on that read's terms, this adding only the memo.
     *
     * <p>What every system-keyed structure a render pass derives is built from. Built on
     * {@link #readSystemsById} instead, such a pass is short the systems a repeated id displaces,
     * and the loss reads as a system missing from the map with nothing naming its cause.
     *
     * @return what {@link SectorStarSystems#indexByKey} answers; empty for an index opened over no
     *         sector. Read-only - the map is the index's own memo
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
