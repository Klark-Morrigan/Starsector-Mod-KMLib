package kmlib.mods.nexerelin;

import kmlib.KmlibMod;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.IntegrationFailureReporter;
import kmlib.starsector.compatibility.ModIntegration;
import kmlib.starsector.factions.alliances.AllianceRecord;
import kmlib.starsector.factions.alliances.AllianceSource;
import kmlib.starsector.strings.KmlibStringKeys;

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
 *
 * <p>The same deferral means a Nexerelin release that moved what the reader reaches is met on a
 * read rather than at load, so the read runs behind a guard: a failed read answers no alliances for
 * the rest of the session and is reported once, with the library as the mod that lost something.
 */
public final class NexerelinAllianceSource {

    // Which of the library's features a failed read costs, as the half of a latch key the mod ID
    // does not cover. Its own key rather than the routines', so a failed read and a failed routine
    // are two reports carrying the two different sentences.
    private static final String NEXERELIN_ALLIANCES_FEATURE_KEY = "nexerelin-alliances";

    // The session's read. The lambda names the holder only in its body, so building this resolves
    // nothing of Nexerelin's - the holder is still first resolved by a read the gate let through.
    private static final AllianceSource SESSION_READ = new GuardedAllianceSource(
        () -> Holder.SOURCE.readAlliances(),
        new IntegrationFailureReporter(NexerelinAllianceSource::describeAllianceIntegration));

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
     *         where its alliance manager has not been created yet, and for the rest of the session
     *         once a read has failed - which is the answer that leaves a caller behaving as it does
     *         without the mod
     */
    public static List<AllianceRecord> readAllianceRecords() {

        // Gated before the holder is named, so a Nexerelin-free install never seeks exerelin.*.
        if (!isModEnabled()) {
            return List.of();
        }
        return SESSION_READ.readAlliances();
    }

    // The read as the channel states it: Nexerelin as the third party, and the library as the mod
    // that loses something by it. Composed only once a read has failed, so the wording read out of
    // strings.json and the mod manager read behind the installed version stay off every read that
    // worked.
    //
    // Open to the suite because a transposition here reads as plausibly as the right pairing, and
    // reaches a player as a report naming the wrong feature.
    static ModIntegration describeAllianceIntegration() {

        return new ModIntegration(
            NexerelinPresence.MOD_ID,
            NexerelinPresence.MOD_NAME,
            new CompatibilityConsumer(
                KmlibMod.MOD_ID,
                NEXERELIN_ALLIANCES_FEATURE_KEY,
                KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_LOST_NEXERELIN_ALLIANCES),
                KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_UNAFFECTED_NEXERELIN_ALLIANCES)));
    }

    // Isolates the only reference to the Nexerelin-coupled reader. The classloader resolves this
    // holder on first call, which the gate above defers until Nexerelin is known present.
    private static final class Holder {

        private static final AllianceSource SOURCE = new NexerelinAllianceReader();
    }
}
