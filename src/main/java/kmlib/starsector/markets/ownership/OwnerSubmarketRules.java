package kmlib.starsector.markets.ownership;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.extensions.ExtensionPoint;
import kmlib.extensions.FallbackToDefaults;
import kmlib.extensions.WorkOutcome;

/**
 * Which rule this install decides a colony's trading counters by, and the offer made to it before
 * this library's own table decides them.
 *
 * <p>Narrower than the two routine points beside it, and deliberately so: what is offered here is
 * one aspect of an ownership change rather than the whole of it. A mod running its own diplomacy
 * has a rule for which counters a colony trades over and nothing for the flag, the outlying
 * entities or the tariff, so that aspect is the only one there is anything to defer to about.
 *
 * <p>One rule, the last registered. A colony's counters are decided once, and two rules opening
 * and closing the same counters would leave whichever ran last in charge of a colony neither
 * describes - so the last registered decides them outright and the displacement is logged.
 *
 * <p>A rule that declines, and an install that registered none, both leave the counters to this
 * library's own table.
 *
 * <p>Final class with a private constructor: the point is the state, and it is one point per
 * running game rather than one per holder of a reference to it.
 */
public final class OwnerSubmarketRules {

    private static final ExtensionPoint<OwnerSubmarketRule> INSTALLED_RULE =
        new ExtensionPoint<>("owner submarket rule");

    private OwnerSubmarketRules() {
        // utility class, no instances.
    }

    /**
     * Empties the point, so an install can be composed again from nothing.
     */
    public static void clearRule() {
        INSTALLED_RULE.clearImplementation();
    }

    /**
     * @return the rule this install decides a colony's counters by, or null where nothing
     *         registered one
     */
    public static OwnerSubmarketRule readRule() {
        return INSTALLED_RULE.readImplementation();
    }

    /**
     * @return the name that rule was registered under, or null where nothing is installed
     */
    public static String readRuleName() {
        return INSTALLED_RULE.readImplementationName();
    }

    /**
     * Installs the rule this install decides a colony's counters by, replacing whatever was there.
     *
     * @param integrationName    who is deciding the counters now, for the log - a mod's name
     * @param ownerSubmarketRule the rule to offer a colony's counters to; null is passed over
     * @param fallbackToDefaults whether counters this rule declines may be decided by this
     *                           library's own table instead - forbidden by a mod whose colonies
     *                           trade over counters the table does not know about
     */
    public static void registerRule(
            String integrationName,
            OwnerSubmarketRule ownerSubmarketRule,
            FallbackToDefaults fallbackToDefaults) {

        INSTALLED_RULE.registerImplementation(
            integrationName,
            ownerSubmarketRule,
            fallbackToDefaults);
    }

    // Offers the counters to whatever is installed, and says whether they were decided. Shaped as
    // one rule itself, so the ownership change asks one question of its own package rather than
    // reading a point it would then have to know the rules of.
    static WorkOutcome offerSubmarkets(MarketAPI market, String oldOwnerId, String newOwnerId) {

        var ownerSubmarketRule = readRule();

        var outcome = ownerSubmarketRule != null
            ? ownerSubmarketRule.applySubmarkets(market, oldOwnerId, newOwnerId)
            : null;

        return INSTALLED_RULE.settleWorkOutcome(outcome);
    }
}
