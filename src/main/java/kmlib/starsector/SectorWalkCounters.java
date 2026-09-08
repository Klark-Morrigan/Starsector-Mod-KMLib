package kmlib.starsector;

import kmlib.profiling.ActiveProfiler;
import kmlib.profiling.ProfileCounter;

/**
 * What the sector's shared reads report having traversed: the walks they made,
 * and the systems, markets, entities and colonies those walks touched.
 *
 * <p>A duration cannot be judged without the size of what it ran over - "40ms"
 * says nothing until it is "40ms over 2100 entities" - and the reads below this
 * are where the sector is actually traversed. Counted there rather than at the
 * call sites, so a caller that never wrote a profiling line still states how
 * much of the sector its work touched, and a second traversal nobody meant to
 * make shows up on the row that made it.
 *
 * <p>Counters rather than sections, because a shared read is not a beat anybody
 * is timing: what a reader wants of it is how many, charged to whichever row was
 * open. A count made with no row open is kept under the profiler's reserved
 * origin, so a traversal from an unprofiled path is seen rather than lost.
 *
 * <p>Named here rather than beside the profiler because the quantities are facts
 * about what a sector holds; profiling knows only that they are counters.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance
 * state.
 */
public final class SectorWalkCounters {

    /**
     * Colonies a read selected, whatever place it read.
     */
    public static final ProfileCounter COLONIES_READ = ProfileCounter.registerCounter("colonies");

    /**
     * Campaign entities a read scanned - what a location's own contents cost to
     * look through, as opposed to what the economy could answer directly.
     */
    public static final ProfileCounter ENTITIES_VISITED = ProfileCounter.registerCounter("entities");

    /**
     * Markets a read yielded, from either listing.
     */
    public static final ProfileCounter MARKETS_READ = ProfileCounter.registerCounter("markets");

    /**
     * Traversals of the sector's whole system list. The counter a budget is
     * stated against: a pass that walks the sector twice for one answer says so
     * here before it says it anywhere else.
     */
    public static final ProfileCounter SECTOR_WALKS = ProfileCounter.registerCounter("walks");

    /**
     * Star systems a read visited, whether it walked all of them or was handed
     * one.
     */
    public static final ProfileCounter SYSTEMS_VISITED = ProfileCounter.registerCounter("systems");

    private static final long NOTHING_COUNTED = 0L;

    // A walk is one walk however much it found.
    private static final long ONE_WALK = 1L;

    private SectorWalkCounters() {
        // utility class, no instances.
    }

    /**
     * Counts colonies a read selected.
     *
     * @param colonies how many the read yielded
     */
    public static void countColoniesRead(long colonies) {
        addCountToOpenScope(COLONIES_READ, colonies);
    }

    /**
     * Counts campaign entities a read scanned.
     *
     * @param entities how many the read looked at
     */
    public static void countEntitiesVisited(long entities) {
        addCountToOpenScope(ENTITIES_VISITED, entities);
    }

    /**
     * Counts markets a read yielded.
     *
     * @param markets how many the read found
     */
    public static void countMarketsRead(long markets) {
        addCountToOpenScope(MARKETS_READ, markets);
    }

    /**
     * Counts one traversal of the sector's system list, and the systems it went
     * over.
     *
     * <p>The two together, since neither answers the other's question: the walk
     * is what a budget bounds, and the systems are what its duration is read
     * against.
     *
     * @param systemsVisited how many systems the traversal went over, which is
     *                       what it reached rather than what it kept
     */
    public static void countSectorWalk(long systemsVisited) {

        addCountToOpenScope(SECTOR_WALKS, ONE_WALK);
        countSystemsVisited(systemsVisited);
    }

    /**
     * Counts star systems a read visited, stating no traversal beside them -
     * what a read handed its systems reports, where one that went looking for
     * them reports the walk as well through {@link #countSectorWalk}.
     *
     * @param systems how many the read went over
     */
    public static void countSystemsVisited(long systems) {
        addCountToOpenScope(SYSTEMS_VISITED, systems);
    }

    // Charged to whatever section is open, which is the point: the read knows
    // what it traversed and nothing about who wanted it.
    private static void addCountToOpenScope(ProfileCounter counter, long amount) {

        // Nothing counted is nothing to say. A counter opened at zero is a column
        // every other row is then read past, and a call that counted none of one
        // it does carry already reads as a zero in that row's spread.
        if (amount <= NOTHING_COUNTED) {
            return;
        }
        ActiveProfiler.resolveProfiler().addCountToOpenScope(counter, amount);
    }
}
