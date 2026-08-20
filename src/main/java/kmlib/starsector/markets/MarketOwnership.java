package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;

import kmlib.starsector.nexerelin.NexerelinSubmarkets;

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
 * that changes hands repeatedly never accumulates the leavings of the owners before.
 *
 * <p>The counters are the one aspect an installed mod may decide instead, because which counters a
 * colony trades over is partly a question about other mods and a mod running its own diplomacy is
 * what holds the answers - see {@link OwnerSubmarketRule}. Everything else here stands whatever is
 * installed, there being no such rule to defer to.
 *
 * <p>What it deliberately leaves alone is everything about <em>leaving</em> an owner - the
 * administrator, the free-port flag, stockpile use, recent unrest, and the account outstanding at
 * the counter that owner's own production was sold over. A colony founded a moment ago has no
 * previous owner to detach from, so those belong to {@link MarketOwnershipTransfer} rather than to
 * the rule for whoever holds the place now.
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

    // The submarket rule this install supplies, offered every ownership change before the verdicts
    // below are applied. Bound to the one mod this library knows how to defer to, and reached
    // through the seam rather than named at the call site so the branch can be posed both ways
    // under test - an install whose mod decides the counters, and one where nothing does.
    private static final OwnerSubmarketRule INSTALLED_OWNER_SUBMARKET_RULE =
        NexerelinSubmarkets::applySubmarkets;

    private MarketOwnership() {
        // utility class, no instances.
    }

    /**
     * Restates a market as held by the given faction: its flag, its submarkets and its tariff all
     * set from that owner.
     *
     * <p>The outgoing owner is read first and nothing else is: an installed submarket rule may
     * treat a colony that has actually changed hands differently from one restated under the owner
     * it already had, and the one thing that says which this is stops being readable the moment the
     * incoming id lands. Everything after that reads the market rather than the argument, so the
     * market is coherent from the first mutation onwards and the tariff is charged at the incoming
     * owner's rate rather than the outgoing one's.
     *
     * <p>Who counts as the player is decided by the id alone. Nexerelin's own submarket rule
     * additionally accepts a market that already reports itself player-owned, which serves its
     * case of the player running a faction of their own; here it would be a trap, because this
     * method is what sets that flag - reading it back would make the player arm sticky and a
     * colony handed away would keep the submarkets of the owner it just left. Setting the flag
     * before that rule is offered the change is what keeps the two readings in step where it does
     * run: what it reads back is the incoming owner, not the outgoing one.
     *
     * @param market    the market changing hands; null is left alone
     * @param factionId the incoming owner's faction id, {@link Factions#PLAYER} for the player;
     *                  null leaves the market alone rather than unowning it
     */
    public static void applyOwnership(MarketAPI market, String factionId) {
        applyOwnership(market, factionId, INSTALLED_OWNER_SUBMARKET_RULE);
    }

    // The same change against a stated submarket rule rather than the installed one, which is what
    // lets both branches be posed on a machine that has whichever mods it happens to have.
    static void applyOwnership(
            MarketAPI market,
            String factionId,
            OwnerSubmarketRule ownerSubmarketRule) {

        if (market == null || factionId == null) {
            return;
        }

        // Read before the incoming id lands, because it is gone the moment it does. A rule an
        // install supplies may restock the counters only where the colony has actually changed
        // hands, and the outgoing owner is the only thing that says whether it has.
        var outgoingFactionId = market.getFactionId();

        market.setFactionId(factionId);
        market.setPlayerOwned(isHeldByPlayer(factionId));

        applyOwnerToEntities(market, factionId);
        applyOwnerSubmarkets(market, outgoingFactionId, factionId, ownerSubmarketRule);
        applyOwnerTariff(market);
    }

    // Whether the id names the player, which is the whole of the axis every aspect below turns on.
    // Asked of the id at each point of use rather than worked out once and handed down: a colony is
    // the player's because of who owns it, so an aspect deciding otherwise from a flag it was
    // passed is an aspect free to disagree with the id the change was made under.
    private static boolean isHeldByPlayer(String factionId) {
        return Factions.PLAYER.equals(factionId);
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

        var connectedEntities = market.getConnectedEntities();

        if (connectedEntities == null) {
            return;
        }

        for (var entity : connectedEntities) {

            if (entity != null) {
                entity.setFaction(factionId);
            }
        }
    }

    // Which counters the colony trades over, decided by whatever the install has to decide them
    // with: a mod's own rule where one is present, and the verdicts below otherwise.
    //
    // Storage sits outside that choice and is applied either way. It holds the player's own cargo
    // and hulls, vanilla's own takeover leaves it in place, vanilla gives even a Hegemony colony
    // one, and the mod rule this library defers to does not name it at all - so a colony that
    // changed hands through that rule would otherwise be left with no hold the player can reach.
    private static void applyOwnerSubmarkets(
            MarketAPI market,
            String outgoingFactionId,
            String factionId,
            OwnerSubmarketRule ownerSubmarketRule) {

        var isPlayerHeld = isHeldByPlayer(factionId);

        if (!ownerSubmarketRule.applySubmarkets(market, outgoingFactionId, factionId)) {
            applySubmarketVerdicts(market, isPlayerHeld);
        }

        if (isPlayerHeld) {
            unlockStorageForPlayer(market);
        }
    }

    // What this library holds a colony's counters to be, per owner. Local resources are the
    // player's own production made buyable and belong to a player colony alone; the open and black
    // markets are how an NPC colony trades and are what a player colony replaces with local
    // resources. The open market is the one aspect that is not simply the inverse of the other
    // side: a player colony running commerce has one too, which is why each submarket is asked for
    // a verdict rather than the player set being derived by negating the faction set.
    private static void applySubmarketVerdicts(MarketAPI market, boolean isPlayerHeld) {

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
    // is set rather than the player being billed to reach their own hold. The counter is asked for
    // through the same presence rule as the rest, since a market that changed hands from a faction
    // that never opened one has none to set the flag on.
    private static void unlockStorageForPlayer(MarketAPI market) {

        applySubmarketPresence(market, Submarkets.SUBMARKET_STORAGE, true);

        // Asked for as vanilla's own plugin rather than assumed to be one: the storage counter is
        // vanilla's, but a mod is free to replace the plugin behind it, and a colony changing
        // hands is not worth failing over a fee.
        var storagePlugin = Markets.readSubmarketPlugin(
            market,
            Submarkets.SUBMARKET_STORAGE,
            StoragePlugin.class);

        if (storagePlugin != null) {
            storagePlugin.setPlayerPaidToUnlock(true);
        }
    }

    // The tax rate the new owner levies, read off the market once it flies their flag rather than
    // off the id passed in - the faction is what carries the fraction, and vanilla's own
    // colonisation reads it the same way round.
    private static void applyOwnerTariff(MarketAPI market) {

        var faction = market.getFaction();
        var tariff = market.getTariff();

        if (faction == null || tariff == null) {
            return;
        }

        tariff.modifyFlat(DEFAULT_TARIFF_MODIFIER_ID, faction.getTariffFraction());
    }
}
