package kmlib.starsector.factions.alliances;

import java.util.List;
import java.util.Optional;

/**
 * One alliance flattened to plain data, so whatever reads alliances can be built and tested without
 * a game around it.
 *
 * <p>Vanilla keeps no such arrangement, so every record here comes from a mod that does. The shape
 * is stated once, in the game's own terms rather than any one mod's, and an {@link AllianceSource}
 * is what turns a particular mod's alliances into these. Every field is a snapshot taken at read
 * time, never a live handle back into whatever maintains the alliance.
 *
 * @param allianceId              the alliance's stable ID, which every member faction shares
 * @param name                    the alliance's display name
 * @param membersSortedDescending the member faction IDs as the source ranks them, so the first is
 *                                the lead {@link #findLeadFactionId()} names
 */
public record AllianceRecord(
    String allianceId,
    String name,
    List<String> membersSortedDescending) {

    /**
     * Defensively snapshots the member list into an immutable copy, so a record handed on cannot
     * change under its reader after the read. An absent list reads as no members, so an alliance a
     * source reported without a roster folds like an empty one.
     */
    public AllianceRecord {
        membersSortedDescending = membersSortedDescending == null
            ? List.of()
            : List.copyOf(membersSortedDescending);
    }

    /**
     * Finds the member that leads the alliance: the one its source ranks first.
     *
     * @return the lead's faction ID, or empty for an alliance with no members
     */
    public Optional<String> findLeadFactionId() {

        return membersSortedDescending.isEmpty()
            ? Optional.empty()
            : Optional.of(membersSortedDescending.get(0));
    }
}
