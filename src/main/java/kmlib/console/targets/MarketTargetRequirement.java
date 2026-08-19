package kmlib.console.targets;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.markets.MarketColoniser;
import kmlib.starsector.markets.Markets;

import java.util.function.Predicate;

/**
 * What a command needs of the market it is about to act on: the rule a candidate has to meet,
 * and the phrase a message names that rule by.
 *
 * <p>The two are one value because a message about a rejected target is only honest if it
 * describes the rule that rejected it. Bound together, a command says what it is looking for
 * once and every message the search produces - the named place that does not qualify, the
 * search that turned nothing up - words itself from the same phrase, with no way to pair one
 * command's rule with another's wording.
 *
 * @param eligibility       whether a market qualifies as this kind of target
 * @param requirementPhrase what a market has to be, worded to complete both "... is not
 *                          &lt;phrase&gt;" and "Nothing here is &lt;phrase&gt;", so it carries
 *                          its own article
 */
public record MarketTargetRequirement(
    Predicate<MarketAPI> eligibility,
    String requirementPhrase) {

    /** A body still carrying only survey data, which a colony can be founded on. */
    public static final MarketTargetRequirement COLONISABLE_BODY = new MarketTargetRequirement(
        MarketColoniser::isReadyForColonisation,
        "a body ready for colonisation");

    /**
     * A colony a faction already holds, which can be moved to another owner. Ownership is the
     * whole of it: registration with the economy is no part of the rule, vanilla building
     * Galatia Academy as a real colony it never lists.
     */
    public static final MarketTargetRequirement EXISTING_COLONY = new MarketTargetRequirement(
        Markets::isOwnedColony,
        "an existing colony");

    /**
     * Whether {@code market} is the kind of target this requirement describes.
     *
     * @param market the market to weigh; whatever the rule makes of null is the answer, every
     *               rule reachable here being null-defensive
     * @return true when the market qualifies
     */
    public boolean isMetBy(MarketAPI market) {
        return eligibility.test(market);
    }
}
