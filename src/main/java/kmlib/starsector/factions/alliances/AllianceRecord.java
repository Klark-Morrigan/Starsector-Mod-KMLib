package kmlib.starsector.factions.alliances;

import java.util.List;

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
 * @param membersSortedDescending the member faction IDs ordered by descending market size, so
 *                                element 0 is the de-facto dominant member
 */
public record AllianceRecord(
    String allianceId,
    String name,
    List<String> membersSortedDescending) {

    /**
     * Defensively snapshots the member list into an immutable copy, so a record handed on cannot
     * change under its reader after the read.
     */
    public AllianceRecord {
        membersSortedDescending = List.copyOf(membersSortedDescending);
    }
}
