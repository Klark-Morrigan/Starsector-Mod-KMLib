package kmlib.starsector.factions.alliances;

import java.util.List;

/**
 * A supplier of the alliances standing right now, as plain {@link AllianceRecord}s.
 *
 * <p>The seam that keeps a reader of alliances off the mod that maintains them: an implementation
 * naming that mod is the only place its types are reached, and every other implementation supplies
 * hand-built records, so whatever folds or weighs them runs with no game around it.
 *
 * <p>A port rather than a snapshot, because alliances form and dissolve in play: a set taken once
 * would answer for the rest of the session, and a reader of it would go on crediting a partnership
 * that ended cycles ago.
 */
@FunctionalInterface
public interface AllianceSource {

    /** No alliances at all - what an install with nothing wired reads through. */
    AllianceSource NO_ALLIANCES = List::of;

    /**
     * The alliances in play right now, each already flattened to plain data.
     *
     * @return the current alliance records; empty where there are none, and never null
     */
    List<AllianceRecord> readAlliances();
}
