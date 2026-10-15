package kmlib.starsector.factions.alliances;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reduces a set of alliances to a single change-detection token, so a poll can tell whether the
 * alliances moved since the last one with one int compare rather than re-deriving the whole
 * grouping.
 *
 * <p>Two things count: <em>who is allied</em> and <em>which member leads</em>. Each alliance's
 * canonical form is its ID, its members as a sorted set, and its lead member (element 0, the
 * dominant member by market size). The sorted set makes a reshuffle <em>below</em> the lead read as
 * no change - it alters neither the membership nor the lead - while the lead is carried explicitly
 * because it is a fact of its own, so a lead swap moves the token even when the membership set is
 * unchanged. A rename is not captured: the token answers who stands together, not what they are
 * called.
 *
 * <p>The canonical lines are sorted before hashing, so the token is independent of the order a
 * source reports the alliances in. That order is not guaranteed stable between polls, and a bare
 * positional hash would move on a pure reorder. Sorting a handful of alliances is trivial, so a
 * plain sorted hash is enough here. Pure over plain {@link AllianceRecord}s - no mod or Starsector
 * type - so the token holds whatever mod the records were read out of. No alliances fold to a fixed
 * token of their own, which is what an install without an alliance source polls steadily.
 */
public final class AllianceFingerprint {

    private AllianceFingerprint() {
    }

    /**
     * Computes the change token for a set of alliances.
     *
     * @param alliances the live alliance records, in any order
     * @return an int that is stable for a fixed membership and lead, and differs when an alliance
     *         forms, dissolves, gains or loses a member, or changes which member leads it
     */
    public static int computeFingerprint(List<AllianceRecord> alliances) {

        var canonicalLines = new ArrayList<String>(alliances.size());

        for (var alliance : alliances) {

            var members = alliance.membersSortedDescending();
            if (members.isEmpty()) {
                // A memberless alliance groups nobody, so it leaves no trace here either.
                continue;
            }

            var sortedMembers = new ArrayList<>(members);

            Collections.sort(sortedMembers);

            // The lead (element 0) is carried apart from the set, so a lead swap moves the token
            // even when the set is unchanged while a reshuffle below the lead does not.
            canonicalLines.add(alliance.allianceId()
                + "|" + members.get(0)
                + "|" + sortedMembers);
        }

        // Sorted so the token depends on membership and leads alone, not on the reporting order;
        // List.hashCode is then the standard positional 31-multiply fold.
        Collections.sort(canonicalLines);

        return canonicalLines.hashCode();
    }
}
