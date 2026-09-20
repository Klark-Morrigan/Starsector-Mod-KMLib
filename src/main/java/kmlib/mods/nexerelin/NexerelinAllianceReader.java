package kmlib.mods.nexerelin;

import kmlib.starsector.factions.alliances.AllianceRecord;
import kmlib.starsector.factions.alliances.AllianceSource;

import java.util.ArrayList;
import java.util.List;

import exerelin.campaign.AllianceManager;
import exerelin.campaign.alliances.Alliance;

/**
 * The only class that names {@code exerelin.*} for alliances: it reads Nexerelin's live alliance
 * manager and flattens each {@link Alliance} to a plain {@link AllianceRecord}.
 *
 * <p>Kept behind {@link NexerelinAllianceSource}'s gate so the classloader never resolves it - and
 * so never seeks a Nexerelin class - on an install without the mod.
 *
 * <p>The live read and the flattening are two steps, and only the first needs a running game.
 * Nexerelin's manager reads its own configuration in a static initialiser, so naming that class at
 * all is what commits a caller to a game being up; turning alliances already in hand into records
 * commits it to nothing but the alliances, which is what makes that half testable.
 */
final class NexerelinAllianceReader implements AllianceSource {

    /**
     * Snapshots every current alliance.
     *
     * <p>Guards the manager itself because {@link AllianceManager#getManager()} returns null before
     * the manager is created - early in a session - while {@link AllianceManager#getAllianceList()}
     * would dereference it unchecked. So an empty list here means "no alliances yet", never a crash.
     *
     * @return one record per live alliance, or empty when none exist yet
     */
    @Override
    public List<AllianceRecord> readAlliances() {

        if (AllianceManager.getManager() == null) {
            return List.of();
        }
        return flattenAlliances(AllianceManager.getAllianceList());
    }

    /**
     * Turns alliances already in hand into records, reading each one's stable ID, its display name
     * and its members in rank order.
     *
     * @param alliances the alliances to flatten, in the order the records should carry
     * @return one record per alliance
     */
    static List<AllianceRecord> flattenAlliances(List<Alliance> alliances) {

        var allianceRecords = new ArrayList<AllianceRecord>(alliances.size());

        for (var alliance : alliances) {
            // uuId is the stable alliance ID; getMembersSorted() ranks members by descending market
            // size, so element 0 is the dominant member a reader colours or names off.
            allianceRecords.add(new AllianceRecord(
                alliance.uuId,
                alliance.getName(),
                alliance.getMembersSorted()));
        }
        return allianceRecords;
    }
}
