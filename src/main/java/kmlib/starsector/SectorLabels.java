package kmlib.starsector;

import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.text.KmlibStrings;

/**
 * How a sector is named to a reader who has to match it back to a save.
 *
 * <p>A diagnostic taken across two games has to say which game each number came
 * from, and "the sector" is not an answer a player can act on. The pair a save
 * browser shows - the seed the sector was generated from and the name of the
 * character playing it - is, because it is the pair the player already reads
 * their saves by.
 *
 * <p>A label rather than an identity: two games could in principle read the
 * same, and nothing here depends on their being told apart. What it has to do
 * is be recognised.
 */
public final class SectorLabels {

    // What sits between the two facts, wide enough to be seen inside a seed
    // that carries digits and letters of its own.
    private static final String SEED_AND_PLAYER_SEPARATOR = " - ";

    private SectorLabels() {
    }

    /**
     * Describes {@code sector} as its seed with the player's name beside it.
     *
     * <p>The seed alone where there is no player: a sector generated but not yet
     * played into has nobody's name to give, and the seed still says which game
     * it is.
     *
     * @param sector the sector being described
     * @return what a reader matches back to a save
     */
    public static String describeSector(SectorAPI sector) {

        var playerName = readPlayerName(sector);

        return KmlibStrings.hasText(playerName)
            ? sector.getSeedString() + SEED_AND_PLAYER_SEPARATOR + playerName
            : sector.getSeedString();
    }

    // Absent before a character exists, which is every sector generated and not
    // yet played into.
    private static String readPlayerName(SectorAPI sector) {

        var player = sector.getPlayerPerson();

        return player == null ? null : player.getNameString();
    }
}
