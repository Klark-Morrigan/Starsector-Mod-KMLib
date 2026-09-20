package kmlib.mods.nexerelin;

import kmlib.starsector.factions.alliances.AllianceRecord;
import kmlib.starsector.factions.alliances.AllianceSource;

import java.util.List;

/**
 * Nexerelin's live alliances, as plain {@link AllianceRecord}s - the soft-dependency gate in front
 * of the only code that names {@code exerelin.*} for them.
 *
 * <p>Vanilla keeps no alliances, so a caller wanting them has to ask a mod that does. This answers
 * whether that mod is here and, when it is, reads what it holds; a caller folds the records to its
 * own shape and never learns which mod produced them.
 *
 * <p>The sole reference to {@link NexerelinAllianceReader} lives in the nested holder, which the
 * classloader does not resolve until the gate has passed - so an install without Nexerelin never
 * seeks an {@code exerelin.*} class. Nothing here names one, not even in a method signature, which
 * is why the reader is a class of its own rather than a method on this one. A deferred-reference
 * holder rather than reflection, which is banned.
 */
public final class NexerelinAllianceSource {

    private NexerelinAllianceSource() {
        // utility class, no instances.
    }

    /**
     * Whether Nexerelin is enabled, and so whether alliances can be read at all.
     *
     * <p>Stable for a session, a mod set not changing in play, so a caller may compute it once.
     *
     * @return true when Nexerelin is present
     */
    public static boolean isModEnabled() {
        return NexerelinPresence.isModEnabled();
    }

    /**
     * The alliances standing right now.
     *
     * <p>Read afresh on every ask rather than snapshotted: alliances form and dissolve in play, and
     * a reader holding a stale set goes on crediting a partnership that ended cycles ago.
     *
     * @return one record per live alliance; empty where there are none, where Nexerelin is absent,
     *         and where its alliance manager has not been created yet - which is the answer that
     *         leaves a caller behaving as it does without the mod
     */
    public static List<AllianceRecord> readAllianceRecords() {

        // Gated before the holder is named, so a Nexerelin-free install never seeks exerelin.*.
        if (!isModEnabled()) {
            return List.of();
        }
        return Holder.SOURCE.readAlliances();
    }

    // Isolates the only reference to the Nexerelin-coupled reader. The classloader resolves this
    // holder on first call, which the gate above defers until Nexerelin is known present.
    private static final class Holder {

        private static final AllianceSource SOURCE = new NexerelinAllianceReader();
    }
}
