package kmlib.starsector.markets.ownership;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

/**
 * A statement of which counters a colony trades over belonging to something other than this library
 * - the rule an installed mod decides its own colonies' submarkets by, offered an ownership change
 * before this library's own verdicts are applied.
 *
 * <p>It exists because which counters a colony has is partly a question about other mods. A mod that
 * runs its own diplomacy knows that this modded station trades without a black market, that that one
 * keeps a military counter whatever it runs, and that a given faction ships a military counter of
 * its own to be used in place of the game's. None of that is derivable from a market, so where such
 * a rule is present it decides the counters and this library's own table stands down.
 *
 * <p>What a rule here declines for: the mod is not installed. The decline leaves the counters
 * exactly as they were, so the library's own verdicts apply in its place rather than a colony
 * changing hands still trading over whatever the previous owner did.
 *
 * <p>Narrower than the ownership change it takes part in. The flag, the player-owned mark, the
 * outlying entities and the tariff are not offered here, having no such rule to defer to, and
 * neither is storage - a counter holding the player's own property, which no owner's rule opens or
 * closes.
 */
@FunctionalInterface
interface OwnerSubmarketRule {

    /**
     * Brings the colony's counters to what the incoming owner trades over if this rule is the one
     * that should decide them, and says whether it did.
     *
     * <p>Both owners are stated because a rule may restock a counter only where the colony has
     * actually changed hands, which the incoming id alone cannot say.
     *
     * @param market     the colony whose counters are being decided
     * @param oldOwnerId the outgoing owner's faction id, read before the incoming one landed
     * @param newOwnerId the incoming owner's faction id
     * @return true when this rule decided the counters and nothing further is to be done to them;
     *         false when it declined, leaving them exactly as they were for the caller to decide
     */
    boolean applySubmarkets(MarketAPI market, String oldOwnerId, String newOwnerId);
}
