package kmlib.console.targets;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.markets.Markets;
import kmlib.starsector.markets.colonisation.MarketColoniser;

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

    /**
     * A body still carrying only survey data, which a colony can be founded on.
     *
     * <p>One side of the settled/unsettled axis {@link #EXISTING_COLONY} states the other of.
     * Anywhere somebody already lives is refused here, whoever they are and however small the
     * place, because founding is what turns an unsettled body into a settled one and there is
     * nothing left for it to do to a settled one.
     */
    public static final MarketTargetRequirement COLONISABLE_BODY = new MarketTargetRequirement(
        MarketColoniser::isReadyForColonisation,
        "a body ready for colonisation");

    /**
     * A colony somebody already holds, which can be moved to another owner.
     *
     * <p>The other side of the same axis, and the neutral faction is where the two meet: it is
     * how the game says nobody has settled here, so a bare world's placeholder, a decivilised
     * world and a derelict station are all things to found on rather than things to hand over.
     * A read that asked only whether some faction held the market would offer a derelict hulk as
     * a colony to give away, neutral being a faction like any other to that question.
     *
     * <p>Registration with the economy is no part of the rule, vanilla building Galatia Academy
     * as a real colony it never lists.
     */
    public static final MarketTargetRequirement EXISTING_COLONY = new MarketTargetRequirement(
        Markets::isSettledColony,
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
