package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.starsector.memory.SectorMemoryAccess;
import kmlib.text.KmlibStrings;

/**
 * Tells whether the player has set up a faction identity in a sector, by the rule the game itself uses.
 *
 * <p>The player faction always reports a display name. Until the player names it, that name is the one
 * its spec ships with: {@code "Your"} in vanilla, a translation of it under a localised core, and
 * {@code "player"} under Nexerelin. Put into prose, it reads as a real name. No fixed list of such names
 * holds across every locale and mod set, so the check reads no name at all.
 *
 * <p>It reads the sector memory key the faction-naming dialog leaves behind, which is what
 * {@code Misc.isPlayerFactionSetUp()} reads. Vanilla shows the dialog when the first outpost is founded,
 * and mods that show the same dialog set the same key. Mods that guard the player faction's name read it
 * too, so a surface reading it agrees with them about when the player has a name.
 *
 * <p>Both reads take the sector rather than reaching {@code Global}, so a caller drawing a sector the
 * game is not running is told about the sector it is drawing.
 */
public final class StarsectorPlayerFactionResolver {

    // The sector memory key the faction-naming dialog sets, read by Misc.isPlayerFactionSetUp().
    private static final String FACTION_CONFIG_SHOWN_KEY = "$shownFactionConfigDialog";

    private StarsectorPlayerFactionResolver() {
    }

    /**
     * The player faction's ID, once the player has set that faction up.
     *
     * @param sector the sector to ask about; null yields null
     * @return the player faction's ID, or null while the player has not set it up or the sector holds
     *         no player faction to name
     */
    public static String findEstablishedPlayerFactionId(SectorAPI sector) {

        if (!isPlayerFactionEstablished(sector)) {
            return null;
        }

        var playerFaction = sector.getPlayerFaction();

        if (playerFaction == null || !KmlibStrings.hasText(playerFaction.getId())) {
            return null;
        }
        return playerFaction.getId();
    }

    /**
     * Whether the player has set up a faction identity in one sector. See the class doc for the rule.
     *
     * @param sector the sector to ask about; no sector holds no player identity, so it answers
     *               {@code false} rather than throwing
     * @return true once that sector records the faction-naming dialog as shown
     */
    public static boolean isPlayerFactionEstablished(SectorAPI sector) {

        var memory = SectorMemoryAccess.readSectorMemory(sector);

        return memory != null
            && memory.contains(FACTION_CONFIG_SHOWN_KEY);
    }
}
