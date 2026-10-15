package kmlib.starsector.factions.alliances;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reduces the alliances standing to one int, so a poller can tell whether they moved since its last
 * poll without re-deriving the whole grouping.
 *
 * <p>Two things count: who stands in each alliance, and which member leads it. The lead counts apart
 * from the members, so a lead swap moves the token while a reshuffle below the lead does not. A rename
 * does not count: the token answers who stands together, not what they are called.
 *
 * <p>The order a source reports alliances in is not stable between polls, so it does not count
 * either.
 */
public final class AllianceFingerprint {

    private AllianceFingerprint() {
        // utility class, no instances.
    }

    /**
     * Computes the change token for a set of alliances.
     *
     * @param alliances the alliance records, in any order
     * @return the same int for the same alliances, members and leads, whatever order they arrive in
     */
    public static int computeFingerprint(List<AllianceRecord> alliances) {

        var allianceShapes = new HashSet<AllianceShape>();

        for (var alliance : alliances) {

            var leadFactionId = alliance.findLeadFactionId();

            // A memberless alliance has no lead and groups nobody, so it leaves no trace in the token.
            if (leadFactionId.isEmpty()) {
                continue;
            }

            allianceShapes.add(new AllianceShape(
                alliance.allianceId(),
                leadFactionId.get(),
                Set.copyOf(alliance.membersSortedDescending())));
        }

        // Set.hashCode sums its elements' hashes, so the reporting order cannot move it.
        return allianceShapes.hashCode();
    }

    // What the token reads of one alliance. The members are a set, so their order below the lead
    // does not count.
    private record AllianceShape(
        String allianceId,
        String leadFactionId,
        Set<String> memberFactionIds) {
    }
}
