package kmlib.starsector.fleet;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.util.MutableValue;

import java.util.Optional;

/**
 * The player's credits, which the game keeps in the player fleet's cargo.
 *
 * <p>Every charge, refund and affordability check walks the same fleet-to-cargo-to-credits chain. Reading it here
 * gives that walk one home, so a caller states what it does with the credits rather than where they are kept.
 *
 * <p>A sector without a player fleet, or a fleet without cargo, holds no credits. The read answers empty rather than
 * throwing, so the caller can refuse its action and tell the player why instead of ending their game.
 */
public final class PlayerCredits {

    private PlayerCredits() {
    }

    /**
     * @param sector the sector whose player fleet holds the credits
     * @param amount the credits a purchase costs
     * @return whether the player holds at least {@code amount}; {@code false} where the sector holds no credits
     */
    public static boolean canPlayerAfford(SectorAPI sector, float amount) {

        return findPlayerCredits(sector)
            .map(credits -> credits.get() >= amount)
            .orElse(false);
    }

    /**
     * @param sector the sector whose player fleet holds the credits
     * @return the player's credits, live: adding to or subtracting from it moves the player's balance. Empty where the
     *     sector has no player fleet or the fleet no cargo.
     */
    public static Optional<MutableValue> findPlayerCredits(SectorAPI sector) {

        var playerFleet = sector.getPlayerFleet();
        if (playerFleet == null || playerFleet.getCargo() == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(playerFleet
            .getCargo()
            .getCredits());
    }
}
