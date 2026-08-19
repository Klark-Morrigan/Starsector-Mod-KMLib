package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;

/**
 * What holding a colony makes true of it: whose flag it and its outlying entities fly, which
 * submarkets it trades through, and what its goods are taxed at.
 *
 * <p>Every difference between a colony the player holds and one a faction holds, gathered into a
 * single statement of it. The alternative is each operation that changes an owner restating the
 * difference for itself, and any two of those restatements being free to disagree - which is how
 * a colony ends up flying a new flag while still trading through the previous owner's submarkets.
 *
 * <p>Stated per aspect rather than as a sequence, which is what lets one method serve both
 * directions: each aspect is set from the incoming owner rather than toggled away from the
 * outgoing one. Applying the rule twice therefore changes nothing the second time, and a colony
 * that changes hands repeatedly never accumulates the leavings of the owners before. The shape is
 * Nexerelin's {@code SectorManager.updateSubmarkets}, which already states the submarket half as
 * a wanted-or-not verdict per submarket rather than as a list of additions.
 *
 * <p>What it deliberately leaves alone is everything about <em>leaving</em> an owner - the
 * administrator, the free-port flag, stockpile use, recent unrest. A colony founded a moment ago
 * has no previous owner to detach from, so those belong to a takeover rather than to the rule
 * for whoever holds the place now.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and
 * null-defensive like the rest of the library.
 */
public final class MarketOwnership {

    // Vanilla's own id for the tariff modifier a colony's base tax rate is filed under. No ids
    // class names it - both of vanilla's colonisation routines write the literal - so this is
    // the named home for it. The same key is rewritten on every ownership change rather than a
    // new one added: a flat modifier is keyed by its source, so writing it again replaces the
    // previous owner's rate instead of stacking the new rate on top of it.
    private static final String DEFAULT_TARIFF_MODIFIER_ID = "default_tariff";

    private MarketOwnership() {
        // utility class, no instances.
    }

    /**
     * Restates a market as held by the given faction: its flag, its submarkets and its tariff all
     * set from that owner.
     *
     * <p>The faction id lands first and everything downstream reads the market rather than the
     * argument, so the market is coherent from the first mutation onwards and the tariff is
     * charged at the incoming owner's rate rather than the outgoing one's.
     *
     * <p>Who counts as the player is decided by the id alone. Nexerelin's own submarket rule
     * additionally accepts a market that already reports itself player-owned, which serves its
     * case of the player running a faction of their own; here it would be a trap, because this
     * method is what sets that flag - reading it back would make the player arm sticky and a
     * colony handed away would keep the submarkets of the owner it just left.
     *
     * @param market    the market changing hands; null is left alone
     * @param factionId the incoming owner's faction id, {@link Factions#PLAYER} for the player;
     *                  null leaves the market alone rather than unowning it
     */
    public static void applyOwnership(MarketAPI market, String factionId) {

        if (market == null || factionId == null) {
            return;
        }

        var isPlayerHeld = Factions.PLAYER.equals(factionId);

        market.setFactionId(factionId);
        market.setPlayerOwned(isPlayerHeld);

        applyOwnerToEntities(market, factionId);
        applyOwnerSubmarkets(market, isPlayerHeld);
        applyOwnerTariff(market);
    }

    // Flies the owner's flag over the whole holding rather than over the colony alone. A market's
    // connected entities are its station, its comm relay and its sensor array as well as the body
    // itself, and an entity left on the old flag keeps rendering in the old colours and keeps
    // being treated as that faction's property by anything reading the entity rather than the
    // market. The primary entity is set in its own right because a market is not obliged to list
    // it among the connected ones, and setting a flag twice costs nothing.
    private static void applyOwnerToEntities(MarketAPI market, String factionId) {

        var primaryEntity = market.getPrimaryEntity();

        if (primaryEntity != null) {
            primaryEntity.setFaction(factionId);
        }

        if (market.getConnectedEntities() == null) {
            return;
        }

        for (var entity : market.getConnectedEntities()) {

            if (entity != null) {
                entity.setFaction(factionId);
            }
        }
    }

    // Which counters a colony trades over, per owner. Local resources are the player's own
    // production made buyable and belong to a player colony alone; the open and black markets are
    // how an NPC colony trades and are what a player colony replaces with local resources. The
    // open market is the one aspect that is not simply the inverse of the other side: a player
    // colony running commerce has one too, which is why each submarket is asked for a verdict
    // rather than the player set being derived by negating the faction set.
    //
    // Storage is absent from the verdicts on purpose. It holds the player's own cargo and hulls,
    // vanilla's own takeover leaves it in place, vanilla gives even a Hegemony colony one, and
    // Nexerelin's submarket rule does not name it at all - stripping it on a hand-over would
    // destroy player property rather than transfer it.
    private static void applyOwnerSubmarkets(MarketAPI market, boolean isPlayerHeld) {

        applySubmarketPresence(
            market,
            Submarkets.LOCAL_RESOURCES,
            isPlayerHeld);

        applySubmarketPresence(
            market,
            Submarkets.SUBMARKET_OPEN,
            !isPlayerHeld || market.hasIndustry(Industries.COMMERCE));

        applySubmarketPresence(
            market,
            Submarkets.SUBMARKET_BLACK,
            !isPlayerHeld);

        applySubmarketPresence(
            market,
            Submarkets.GENERIC_MILITARY,
            shouldHaveMilitarySubmarket(market, isPlayerHeld));

        if (isPlayerHeld) {
            unlockStorageForPlayer(market);
        }
    }

    // Whether the colony sells military hardware over a counter of its own. Two upstream rules
    // meet here: vanilla's takeover grants the military submarket to a garrison - one flagged
    // military, or running a military base or high command - and Nexerelin's rule adds that a
    // player-held colony never has one whatever it runs, the player's own garrison equipping
    // itself rather than trading with itself.
    private static boolean shouldHaveMilitarySubmarket(MarketAPI market, boolean isPlayerHeld) {

        if (isPlayerHeld) {
            return false;
        }

        return Markets.isMilitary(market)
            || market.hasIndustry(Industries.MILITARYBASE)
            || market.hasIndustry(Industries.HIGHCOMMAND);
    }

    // One submarket brought to the state the owner calls for, whichever state it is in now. The
    // cargo is touched on arrival so the submarket is stocked from the moment it exists rather
    // than at whatever later read first asks for it, which is what Nexerelin's own add does.
    private static void applySubmarketPresence(
            MarketAPI market,
            String submarketId,
            boolean shouldHave) {

        var hasSubmarket = market.hasSubmarket(submarketId);

        if (hasSubmarket && !shouldHave) {

            market.removeSubmarket(submarketId);
            return;
        }

        if (!hasSubmarket && shouldHave) {

            market.addSubmarket(submarketId);

            var submarket = market.getSubmarket(submarketId);

            if (submarket != null) {
                submarket.getCargo();
            }
        }
    }

    // Storage on a player colony comes already paid for. Vanilla charges the fee at the counter
    // for storage the player did not build, and a colony the player holds is theirs - so the flag
    // is set rather than the player being billed to reach their own hold. Storage is added when
    // the colony has none, which is the case of a market that changed hands from a faction that
    // never opened one.
    private static void unlockStorageForPlayer(MarketAPI market) {

        if (!market.hasSubmarket(Submarkets.SUBMARKET_STORAGE)) {
            market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
        }

        var storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);

        if (storage == null) {
            return;
        }

        // Guarded rather than cast outright: the storage submarket is vanilla's, but a mod is
        // free to replace the plugin behind it, and a colony changing hands is not worth a
        // ClassCastException over a fee.
        if (storage.getPlugin() instanceof StoragePlugin storagePlugin) {
            storagePlugin.setPlayerPaidToUnlock(true);
        }
    }

    // The tax rate the new owner levies, read off the market once it flies their flag rather than
    // off the id passed in - the faction is what carries the fraction, and vanilla's own
    // colonisation reads it the same way round.
    private static void applyOwnerTariff(MarketAPI market) {

        var faction = market.getFaction();

        if (faction == null || market.getTariff() == null) {
            return;
        }

        market
            .getTariff()
            .modifyFlat(DEFAULT_TARIFF_MODIFIER_ID, faction.getTariffFraction());
    }
}
