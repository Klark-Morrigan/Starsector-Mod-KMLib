package kmlib.starsector.fleet;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.util.MutableValue;

/**
 * The player's credits, which the game keeps in the player fleet's cargo.
 *
 * <p>Every charge, refund and affordability check walks the same fleet-to-cargo-to-credits chain. Reading it here
 * gives that walk one home, so a caller states what it does with the credits rather than where they are kept.
 */
public final class PlayerCredits {

    private PlayerCredits() {
    }

    /**
     * @param sector the sector whose player fleet holds the credits
     * @param amount the credits a purchase costs
     * @return whether the player holds at least {@code amount}
     */
    public static boolean canPlayerAfford(SectorAPI sector, float amount) {

        return readPlayerCredits(sector).get() >= amount;
    }

    /**
     * @param sector the sector whose player fleet holds the credits
     * @return the player's credits, live: adding to or subtracting from it moves the player's balance
     */
    public static MutableValue readPlayerCredits(SectorAPI sector) {

        return sector
            .getPlayerFleet()
            .getCargo()
            .getCredits();
    }
}
