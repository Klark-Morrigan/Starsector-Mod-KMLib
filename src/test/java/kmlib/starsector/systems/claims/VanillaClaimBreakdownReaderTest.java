package kmlib.starsector.systems.claims;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import kmlib.starsector.colonies.Colonies;
import kmlib.starsector.colonies.KnownColonyReader;
import kmlib.starsector.entities.EntityMapIcon;
import kmlib.starsector.systems.SystemColoniesIndex;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Pins {@link VanillaClaimBreakdownReader} to the mechanic it mirrors: the market filter, the
 * size / sibling / military score, the territoriality gate, and the strictly-greater
 * comparison that leaves a tied contest with the first market the economy lists. Drift in any
 * of those would make a claim explanation disagree with the map fill it explains, which is the
 * failure this suite exists to catch.
 *
 * <p>It also pins the two places the reader deliberately parts from vanilla - the player scored as
 * a barred presence, and a colony the economy does not list carried as one that took no part -
 * since both read as drift to anyone checking the two side by side, and would otherwise be quietly
 * "corrected" back into a mechanic that leaves real colonies out of its account.
 *
 * <p>What identifies a market rather than scoring it - its id, its name and the glyph the map marks
 * its entity with - is pinned here too, since a surface listing the markets behind a standing reads
 * them off this walk rather than looking the market up again. That reading them changes no outcome
 * is asserted head-on beside them.
 *
 * <p>The narrow claimant read and the {@link ClaimReaderSource} binding sit beside the breakdown
 * cases rather than in a suite of their own, the reader answering both claim ports off one
 * contest. Posing them here is what shows the narrow answer is the wide one's claimant and not a
 * second opinion.
 */
final class VanillaClaimBreakdownReaderTest {

    // The port a case poses where the player's knowledge is beside the point: every colony the
    // walk met may be named. What decides that in the running game is the consumer's rule, and no
    // case here is about one - what this suite pins is that the port reaches each breakdown and no
    // term of the contest.
    private static final KnownColonyReader EVERY_COLONY_KNOWN = Colonies::colonies;

    // The port at its other extreme, posed where a case is about what the reader reports of the
    // player's knowledge. Named apart from the constant it happens to equal so a case reads as
    // posing a port rather than as leaving one out.
    private static final KnownColonyReader NO_COLONY_KNOWN = KnownColonyReader.NOTHING_KNOWN;

    // A port admitting one named colony and nothing else - what shows the reader spends the
    // port's own answer per market rather than one verdict for the whole system.
    private static KnownColonyReader buildPortNaming(String knownMarketName) {

        return colonies -> colonies
            .colonies()
            .stream()
            .filter(colony -> knownMarketName.equals(colony.market().getName()))
            .toList();
    }

    private ClaimContestFixture claimContest;

    @BeforeEach
    void setUp() {
        claimContest = new ClaimContestFixture();
    }

    @AfterEach
    void tearDown() {
        claimContest.close();
    }

    @Nested
    class OpenReaderOver {

        @Test
        void bindsTheConstructorAsAClaimReaderSource() {
            // The map's holder providers hold a source rather than a reader so they can open one
            // per pass. The binding is what makes that possible at all, so it is pinned where the
            // reader it opens is rather than left to whichever caller happens to write it out.
            var hegemony = claimContest.buildFaction("hegemony", true);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(hegemony, 4));

            ClaimReaderSource source = VanillaClaimBreakdownReader::new;

            assertThat(source
                    .openReaderOver(
                        EVERY_COLONY_KNOWN,
                        new SystemColoniesIndex(Global.getSector()))
                    .readClaimingFactionId(claimContest.getSystem()))
                .isEqualTo("hegemony");
        }
    }

    @Nested
    class ReadBreakdown {

        @Test
        void scoresAFactionOnItsStrongestMarket() {

            var hegemony = claimContest.buildFaction("hegemony", true);
            var garrison = claimContest.buildMarket(hegemony, 3);

            claimContest.markMarketAsMilitary(garrison);
            claimContest.placeMarketsInSystem(
                claimContest.buildMarket(hegemony, 5),
                garrison);

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            // The size-3 garrison outweighs the size-5 colony on the military bonus alone
            // (3 + 1 sibling + 10 against 5 + 1), and the faction stands on that one market -
            // holdings are never summed.
            assertThat(breakdown.scores())
                .extracting(FactionClaimStanding::factionId, FactionClaimStanding::score)
                .containsExactly(tuple("hegemony", 14));
            assertThat(breakdown.claimantFactionId())
                .isEqualTo("hegemony");
        }

        @Test
        void namesTheMarketAStandingRestsOnAndListsTheFactionsOthers() {

            var hegemony = claimContest.buildFaction("hegemony", true);
            var capital = claimContest.buildMarket(hegemony, 5);
            var outpost = claimContest.buildMarket(hegemony, 3);

            claimContest.nameMarket(capital, "Chicomoztoc");
            claimContest.nameMarket(outpost, "Kazeron");
            claimContest.placeMarketsInSystem(capital, outpost);

            var standing = readTopStanding(
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem()));

            // The standing rests on the one strongest market, and the rest are carried beside
            // it: the sibling point inside its score is exactly the one market listed under it,
            // which is what lets a reader check the number rather than take it on trust.
            assertThat(standing.standingMarket().marketNameplate().displayName())
                .isEqualTo("Chicomoztoc");
            assertThat(standing.standingMarket().marketSize())
                .isEqualTo(5);
            assertThat(standing.standingMarket().siblingMarketCount())
                .isEqualTo(1);
            assertThat(standing.otherMarkets())
                .extracting(market -> market.marketNameplate().displayName())
                .containsExactly("Kazeron");
        }

        @Test
        void carriesTheIdOfEveryMarketAStandingRestsOn() {
            // A surface pairing a claim row back to anything else it knows about the system - how
            // old its news of that colony is, say - needs an identity rather than a label, and
            // vanilla names a station colony and its defending station alike. Read on the walk
            // that met the market, so the id and the name on a row describe one colony.
            var hegemony = claimContest.buildFaction("hegemony", true);
            var colony = claimContest.buildMarket(hegemony, 5);
            var defendingStation = claimContest.buildMarket(hegemony, 3);

            claimContest.nameMarket(colony, "Chicomoztoc");
            claimContest.nameMarket(defendingStation, "Chicomoztoc");
            claimContest.identifyMarket(colony, "chicomoztoc");
            claimContest.identifyMarket(defendingStation, "chicomoztoc_station");
            claimContest.placeMarketsInSystem(colony, defendingStation);

            var standing = readTopStanding(
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem()));

            assertThat(standing.standingMarket().marketId())
                .isEqualTo("chicomoztoc");
            assertThat(standing.otherMarkets())
                .extracting(MarketClaimBreakdown::marketId)
                .containsExactly("chicomoztoc_station");
        }

        @Test
        void carriesTheGlyphTheMapMarksAMarketWith() {
            // A box listing the markets behind a standing leads each with the map's own glyph, and
            // reads it off the breakdown rather than looking the market up a second time - so the
            // icon drawn can only belong to the colony whose score is stated beside it. The colour
            // travels with the path because vanilla draws a whole family from one sprite and tells
            // the types apart by nothing else.
            var independent = claimContest.buildFaction("independent", true);
            var ancyra = claimContest.buildMarket(independent, 5);

            claimContest.giveMarketAMapIcon(
                ancyra,
                "graphics/icons/station0.png",
                new Color(200, 200, 255));

            claimContest.placeMarketsInSystem(ancyra);

            var standing = readTopStanding(
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem()));

            assertThat(standing.standingMarket().marketNameplate().mapIcon())
                .contains(new EntityMapIcon(
                    "graphics/icons/station0.png",
                    new Color(200, 200, 255)));
        }

        @Test
        void carriesNoGlyphForAMarketTheMapMarksWithNone() {
            // An entity with no icon spec at all reaches the box as an absence rather than as a path
            // to a sprite that does not exist, which is what lets the line open on its name.
            var hegemony = claimContest.buildFaction("hegemony", true);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(hegemony, 4));

            var standing = readTopStanding(
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem()));

            assertThat(standing.standingMarket().marketNameplate().mapIcon())
                .isEmpty();
        }

        @Test
        void resolvesTheSameContestWhicheverMarketsTheMapMarks() {
            // The glyph is what identifies a colony in a list and nothing the mechanic weighs, so
            // reading it on the walk must leave the claimant and the ranking exactly where they were.
            var hegemony = claimContest.buildFaction("hegemony", true);
            var tritachyon = claimContest.buildFaction("tritachyon", true);
            var weaker = claimContest.buildMarket(hegemony, 4);

            claimContest.giveMarketAMapIcon(
                weaker,
                "graphics/icons/station0.png",
                new Color(200, 200, 255));

            claimContest.placeMarketsInSystem(weaker, claimContest.buildMarket(tritachyon, 7));

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            assertThat(breakdown.scores())
                .extracting(FactionClaimStanding::factionId, FactionClaimStanding::score)
                .containsExactly(tuple("tritachyon", 7), tuple("hegemony", 4));
            assertThat(breakdown.claimantFactionId())
                .isEqualTo("tritachyon");
        }

        @Test
        void listsAFactionsOtherMarketsInTheOrderTheEconomyDoes() {

            var hegemony = claimContest.buildFaction("hegemony", true);
            var relay = claimContest.buildMarket(hegemony, 1);
            var capital = claimContest.buildMarket(hegemony, 5);
            var outpost = claimContest.buildMarket(hegemony, 3);

            claimContest.nameMarket(relay, "Sindria");
            claimContest.nameMarket(capital, "Chicomoztoc");
            claimContest.nameMarket(outpost, "Kazeron");
            claimContest.placeMarketsInSystem(relay, capital, outpost);

            var standing = readTopStanding(
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem()));

            // Economy order, not score order: the weakest colony leads because that is where
            // the economy put it. A caller wanting them ranked sorts them itself, and one that
            // read them as already ranked would be quietly wrong on every multi-colony system.
            assertThat(standing.standingMarket().marketNameplate().displayName())
                .isEqualTo("Chicomoztoc");
            assertThat(standing.standingMarket().siblingMarketCount())
                .isEqualTo(2);
            assertThat(standing.otherMarkets())
                .extracting(market -> market.marketNameplate().displayName())
                .containsExactly("Sindria", "Kazeron");
        }

        @Test
        void numbersEveryOwnedMarketByItsPlaceInTheEconomysListing() {
            // The one thing that separates two markets on the same score: the contest is settled on
            // a strictly greater score, so the earlier-listed of a tied pair wins and nothing else
            // about either of them says why. Numbered across factions rather than within one,
            // because a tie between two factions' best markets is settled the same way.
            var hegemony = claimContest.buildFaction("hegemony", true);
            var tritachyon = claimContest.buildFaction("tritachyon", true);
            var relay = claimContest.buildMarket(hegemony, 1);
            var rival = claimContest.buildMarket(tritachyon, 4);
            var capital = claimContest.buildMarket(hegemony, 5);

            claimContest.nameMarket(relay, "Sindria");
            claimContest.nameMarket(rival, "Eventide");
            claimContest.nameMarket(capital, "Chicomoztoc");
            claimContest.placeMarketsInSystem(relay, rival, capital);

            var breakdown = new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                .readBreakdown(claimContest.getSystem());

            assertThat(readStanding(breakdown, "hegemony").standingMarket().listingPosition())
                .isEqualTo(3);
            assertThat(readStanding(breakdown, "hegemony").otherMarkets())
                .extracting(MarketClaimBreakdown::listingPosition)
                .containsExactly(1);
            assertThat(readStanding(breakdown, "tritachyon").standingMarket().listingPosition())
                .isEqualTo(2);
        }

        @Test
        void numbersTheOwnedMarketsWithoutAGapWhereAnUnownedOneSits() {
            // An unowned market takes no part in the contest and so takes no place in the numbering.
            // Left in, it would open a gap the player could not account for by looking at the box,
            // since nothing in it would ever carry that number.
            var hegemony = claimContest.buildFaction("hegemony", true);
            var unowned = claimContest.buildMarket(null, 4);
            var capital = claimContest.buildMarket(hegemony, 5);

            claimContest.nameMarket(capital, "Chicomoztoc");
            claimContest.placeMarketsInSystem(unowned, capital);

            var breakdown = new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                .readBreakdown(claimContest.getSystem());

            assertThat(readStanding(breakdown, "hegemony").standingMarket().listingPosition())
                .isEqualTo(1);
        }

        @Test
        void carriesTheGarrisonBonusOnlyForAMilitaryMarket() {

            var hegemony = claimContest.buildFaction("hegemony", true);
            var garrison = claimContest.buildMarket(hegemony, 3);
            var tritachyon = claimContest.buildFaction("tritachyon", true);

            claimContest.markMarketAsMilitary(garrison);
            claimContest.placeMarketsInSystem(
                garrison,
                claimContest.buildMarket(tritachyon, 3));

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            // The bonus is what turns a small garrison into a claimant, so the box explaining a
            // border has to be able to name it as the term that did it - and an ordinary colony
            // must carry no bonus at all rather than one worth nothing.
            assertThat(readStanding(breakdown, "hegemony").standingMarket().militaryBonus())
                .hasValue(10);
            assertThat(readStanding(breakdown, "tritachyon").standingMarket().militaryBonus())
                .isEmpty();
        }

        @Test
        void countsHiddenSiblingMarketsTowardsAScore() {

            var hegemony = claimContest.buildFaction("hegemony", true);

            claimContest.placeMarketsInSystem(
                claimContest.buildMarket(hegemony, 3),
                claimContest.buildHiddenMarket(hegemony, 2));

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            // A hidden market cannot claim on its own account but still counts as presence for
            // the faction holding it, so the visible colony scores 3 + 1 rather than 3.
            assertThat(breakdown.scores())
                .extracting(FactionClaimStanding::factionId, FactionClaimStanding::score)
                .containsExactly(tuple("hegemony", 4));
        }

        @Test
        void listsAHiddenMarketAmongAFactionsOthersWithoutLettingItStandForIt() {

            var pirates = claimContest.buildFaction("pirates", true);
            var haven = claimContest.buildMarket(pirates, 3);
            var base = claimContest.buildHiddenMarket(pirates, 6);

            claimContest.nameMarket(haven, "Kanta's Den");
            claimContest.nameMarket(base, "Tigra City");
            claimContest.placeMarketsInSystem(haven, base);

            var standing = readTopStanding(
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem()));

            // The larger base never stands for the faction, yet it is what the sibling point is
            // made of, so it is listed with the rest: dropping it would leave a count with
            // nothing beneath it to account for.
            assertThat(standing.standingMarket().marketNameplate().displayName())
                .isEqualTo("Kanta's Den");
            assertThat(standing.standingMarket().siblingMarketCount())
                .isEqualTo(1);
            assertThat(standing.otherMarkets())
                .extracting(market -> market.marketNameplate().displayName())
                .containsExactly("Tigra City");

            // The hiddenness rides the listed market itself, so a reader of the parts can tell a
            // market that lost a listing tie from one the mechanic never compared at all.
            assertThat(standing.standingMarket().isHiddenMarket())
                .isFalse();
            assertThat(standing.otherMarkets().get(0).isHiddenMarket())
                .isTrue();
        }

        @Test
        void excludesHiddenMarketsFromTheContest() {

            var pirates = claimContest.buildFaction("pirates", true);
            var base = claimContest.buildHiddenMarket(pirates, 6);

            claimContest.nameMarket(base, "Tigra City");
            claimContest.placeMarketsInSystem(base);

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            // A scoring standing of its own would count the base twice - it already reaches the
            // contest through the sibling count - and would let it displace whatever visible colony
            // its faction actually contests the system with. So it takes no score, and nothing it
            // is worth reaches the claimant.
            assertThat(breakdown.scores())
                .extracting(FactionClaimStanding::factionId, FactionClaimStanding::score)
                .containsExactly(tuple("pirates", 0));
            assertThat(breakdown.claimantFactionId())
                .isNull();

            // The faction is present all the same, through the very colony that took no part: a
            // system whose account named nobody would be emptier than what the map draws in it.
            assertThat(breakdown.scores().get(0))
                .isInstanceOf(PresenceOnlyClaimStanding.class);
            assertThat(breakdown.scores().get(0).readHeldMarkets())
                .extracting(market -> market.marketNameplate().displayName())
                .containsExactly("Tigra City");
        }

        @Test
        void leavesASurveyedRocksConditionMarketOutOfTheContestEntirely() {
            // The one case the presence widening would otherwise be catastrophic on: a
            // condition-only market is owned - by `neutral` - and sits off the economy, so a walk
            // admitting off-economy markets on ownership alone would stand a faction on every
            // surveyed rock in the sector. It is the shared colony set that rules them out, which
            // is what makes reading through that set rather than walking here load-bearing.
            var hegemony = claimContest.buildFaction("hegemony", true);
            var neutral = claimContest.buildFaction(Factions.NEUTRAL, false);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(hegemony, 3));
            claimContest.placeOffEconomyMarketsInSystem(
                claimContest.buildConditionOnlyMarket(neutral, 0));

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            assertThat(breakdown.scores())
                .extracting(FactionClaimStanding::factionId)
                .containsExactly("hegemony");
            assertThat(readTopStanding(breakdown).otherMarkets())
                .isEmpty();
        }

        @Test
        void standsAFactionHoldingOnlyAnUnlistedColonyOnItsPresenceAlone() {

            var independent = claimContest.buildFaction("independent", true);
            var academy = claimContest.buildMarket(independent, 6);

            claimContest.nameMarket(academy, "Galatia Academy");
            claimContest.placeOffEconomyMarketsInSystem(academy);

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            // The other way a colony goes unweighed, and it lands the same way: the walk never
            // reached the station at all, so its owner is present at nought rather than absent.
            assertThat(breakdown.scores())
                .singleElement()
                .isInstanceOf(PresenceOnlyClaimStanding.class);
            assertThat(breakdown.scores().get(0).score())
                .isZero();
            assertThat(breakdown.scores().get(0).readHeldMarkets())
                .extracting(market -> market.marketNameplate().displayName())
                .containsExactly("Galatia Academy");
            assertThat(breakdown.claimantFactionId())
                .isNull();
        }

        @Test
        void standsAFactionHoldingBothKindsOfUnweighedColonyOnOnePresence() {

            var pirates = claimContest.buildFaction("pirates", true);
            var base = claimContest.buildHiddenMarket(pirates, 6);
            var cache = claimContest.buildMarket(pirates, 4);

            claimContest.nameMarket(base, "Tigra City");
            claimContest.nameMarket(cache, "Kanta's Den");
            claimContest.placeMarketsInSystem(base);
            claimContest.placeOffEconomyMarketsInSystem(cache);

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            // One standing per faction whatever mixture of unweighed colonies it holds, listed in
            // the order the walk met them - the economy's own ahead of what it does not list.
            assertThat(breakdown.scores())
                .singleElement()
                .isInstanceOf(PresenceOnlyClaimStanding.class);
            assertThat(breakdown.scores().get(0).readHeldMarkets())
                .extracting(market -> market.marketNameplate().displayName())
                .containsExactly("Tigra City", "Kanta's Den");
            assertThat(breakdown.claimantFactionId())
                .isNull();
        }

        @Test
        void ranksPresenceOnlyStandingsBeneathEveryScoreInListingOrder() {

            var hegemony = claimContest.buildFaction("hegemony", true);
            var pirates = claimContest.buildFaction("pirates", false);
            var independent = claimContest.buildFaction("independent", true);

            claimContest.placeMarketsInSystem(
                claimContest.buildHiddenMarket(pirates, 9),
                claimContest.buildMarket(hegemony, 1));
            claimContest.placeOffEconomyMarketsInSystem(claimContest.buildMarket(independent, 9));

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            // The size-1 colony is the only thing anybody was weighed on, so it leads however large
            // the two unweighed holdings are, and it takes the system on a score of one. The pair
            // behind it share a nought and settle between themselves the way every other tie does -
            // in the order the listing reached them, the economy's own market ahead of what it does
            // not list. Territoriality rides each of them, read off the faction as it is for a
            // scored standing, and claims nothing either way.
            assertThat(breakdown.scores())
                .extracting(FactionClaimStanding::factionId, FactionClaimStanding::isTerritorial)
                .containsExactly(
                    tuple("hegemony", true),
                    tuple("pirates", false),
                    tuple("independent", true));
            assertThat(breakdown.claimantFactionId())
                .isEqualTo("hegemony");
        }

        @Test
        void keepsAFactionWithAScoredMarketOnOneWeighedStandingHoweverElseItIsPresent() {

            var independent = claimContest.buildFaction("independent", true);
            var ancyra = claimContest.buildMarket(independent, 5);
            var base = claimContest.buildHiddenMarket(independent, 6);
            var academy = claimContest.buildMarket(independent, 7);

            claimContest.nameMarket(ancyra, "Ancyra");
            claimContest.nameMarket(base, "Tigra City");
            claimContest.nameMarket(academy, "Galatia Academy");
            claimContest.placeMarketsInSystem(ancyra, base);
            claimContest.placeOffEconomyMarketsInSystem(academy);

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            // A faction takes one standing or the other, never both: the scored market carries it,
            // and the two unweighed colonies ride along as the holdings behind that standing. A
            // second, presence-only standing for the same faction would name it twice in every box.
            assertThat(breakdown.scores())
                .singleElement()
                .isInstanceOf(WeighedClaimStanding.class);
            assertThat(readTopStanding(breakdown).standingMarket().marketNameplate().displayName())
                .isEqualTo("Ancyra");
            assertThat(readTopStanding(breakdown).otherMarkets())
                .extracting(market -> market.marketNameplate().displayName())
                .containsExactly("Tigra City", "Galatia Academy");
        }

        @Test
        void listsAMarketTheEconomyDoesNotListAmongAFactionsOthersWithoutScoringIt() {

            var independent = claimContest.buildFaction("independent", true);
            var ancyra = claimContest.buildMarket(independent, 5);
            var academy = claimContest.buildMarket(independent, 6);

            claimContest.nameMarket(ancyra, "Ancyra");
            claimContest.nameMarket(academy, "Galatia Academy");
            claimContest.placeMarketsInSystem(ancyra);
            claimContest.placeOffEconomyMarketsInSystem(academy);

            var standing = readTopStanding(
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem()));

            // The station is on the map in the faction's colours, so an account of the system that
            // never mentions it says less than the player can already see. It takes no standing all
            // the same: vanilla's walk covers the economy, and it was never registered.
            assertThat(standing.standingMarket().marketNameplate().displayName())
                .isEqualTo("Ancyra");
            assertThat(standing.otherMarkets())
                .extracting(market -> market.marketNameplate().displayName())
                .containsExactly("Galatia Academy");
            assertThat(standing.otherMarkets().get(0).isOffEconomyMarket())
                .isTrue();
            assertThat(standing.standingMarket().isOffEconomyMarket())
                .isFalse();
        }

        @Test
        void numbersAMarketTheEconomyDoesNotListAfterEveryMarketItDoes() {
            // The place is stated to the player, so it has to run without a gap and in the order the
            // contest was walked - the economy's own markets first, since those are the ones a tie
            // between them is settled by, and what the economy never listed after all of them.
            var hegemony = claimContest.buildFaction("hegemony", true);
            var capital = claimContest.buildMarket(hegemony, 5);
            var outpost = claimContest.buildMarket(hegemony, 3);
            var academy = claimContest.buildMarket(hegemony, 4);

            claimContest.nameMarket(capital, "Chicomoztoc");
            claimContest.nameMarket(outpost, "Kazeron");
            claimContest.nameMarket(academy, "Galatia Academy");
            claimContest.placeMarketsInSystem(capital, outpost);
            claimContest.placeOffEconomyMarketsInSystem(academy);

            var standing = readTopStanding(
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem()));

            assertThat(standing.standingMarket().listingPosition())
                .isEqualTo(1);
            assertThat(standing.otherMarkets())
                .extracting(
                    market -> market.marketNameplate().displayName(),
                    MarketClaimBreakdown::listingPosition)
                .containsExactly(tuple("Kazeron", 2), tuple("Galatia Academy", 3));
        }

        @Test
        void skipsAMarketTheEconomyDoesNotListThatHasNoOwningFaction() {
            // The unowned case reaches the walk down the entity half too, and the mechanic would
            // throw on one there exactly as it would on an unowned market of the economy's own.
            var hegemony = claimContest.buildFaction("hegemony", true);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(hegemony, 3));
            claimContest.placeOffEconomyMarketsInSystem(claimContest.buildMarket(null, 9));

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            assertThat(breakdown.scores())
                .extracting(FactionClaimStanding::factionId)
                .containsExactly("hegemony");
            assertThat(readStanding(breakdown, "hegemony").otherMarkets())
                .isEmpty();
        }

        @Test
        void keepsAMarketTheEconomyDoesNotListOutOfTheSiblingCount() {

            var independent = claimContest.buildFaction("independent", true);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(independent, 5));
            claimContest.placeOffEconomyMarketsInSystem(claimContest.buildMarket(independent, 6));

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            // Vanilla counts siblings over the economy's own markets, so admitting an unregistered
            // one would raise a real colony above the score the game scores it at - and could hand
            // the system to a different faction, which is a mechanic change, not a fuller account.
            assertThat(breakdown.scores())
                .extracting(FactionClaimStanding::factionId, FactionClaimStanding::score)
                .containsExactly(tuple("independent", 5));
        }

        @Test
        void leavesTheClaimWithATerritorialFactionAnUnlistedMarketOutscores() {

            var hegemony = claimContest.buildFaction("hegemony", true);
            var independent = claimContest.buildFaction("independent", true);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(hegemony, 3));
            claimContest.placeOffEconomyMarketsInSystem(claimContest.buildMarket(independent, 9));

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            // The unlisted colony out-sizes everything present and still takes nothing: its owner
            // is listed for being there, at a nought that can neither win a tie nor take a system,
            // so the claimant is the one vanilla itself would name.
            assertThat(breakdown.scores())
                .extracting(FactionClaimStanding::factionId, FactionClaimStanding::score)
                .containsExactly(tuple("hegemony", 3), tuple("independent", 0));
            assertThat(breakdown.claimantFactionId())
                .isEqualTo("hegemony");
        }

        @Test
        void scoresThePlayerAsAPresenceThatCannotClaim() {

            var player = claimContest.buildFaction("player", true);

            claimContest.markFactionAsPlayer(player);
            claimContest.placeMarketsInSystem(claimContest.buildMarket(player, 8));

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            // Scored, so a system the player holds a colony in never reports as one they have
            // no presence in - but non-territorial however the faction is configured, since the
            // mechanic bars a player colony from claiming outright.
            assertThat(breakdown.scores())
                .extracting(
                    FactionClaimStanding::factionId,
                    FactionClaimStanding::score,
                    FactionClaimStanding::isTerritorial)
                .containsExactly(tuple("player", 8, false));
            assertThat(breakdown.claimantFactionId())
                .isNull();
        }

        @Test
        void leavesTheClaimWithATerritorialFactionThePlayerOutscores() {

            var hegemony = claimContest.buildFaction("hegemony", true);
            var player = claimContest.buildFaction("player", true);

            claimContest.markFactionAsPlayer(player);
            claimContest.placeMarketsInSystem(
                claimContest.buildMarket(player, 8),
                claimContest.buildMarket(hegemony, 3));

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            // The player tops the standings and still loses the system: scoring it widens what
            // the breakdown reports without touching the winner vanilla would name.
            assertThat(breakdown.scores())
                .extracting(FactionClaimStanding::factionId, FactionClaimStanding::score)
                .containsExactly(tuple("player", 8), tuple("hegemony", 3));
            assertThat(breakdown.claimantFactionId())
                .isEqualTo("hegemony");
        }

        @Test
        void ranksPresentFactionsByScoreDescending() {

            var hegemony = claimContest.buildFaction("hegemony", true);
            var tritachyon = claimContest.buildFaction("tritachyon", true);

            claimContest.placeMarketsInSystem(
                claimContest.buildMarket(hegemony, 4),
                claimContest.buildMarket(tritachyon, 7));

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            assertThat(breakdown.scores())
                .extracting(FactionClaimStanding::factionId, FactionClaimStanding::score)
                .containsExactly(tuple("tritachyon", 7), tuple("hegemony", 4));
            assertThat(breakdown.claimantFactionId())
                .isEqualTo("tritachyon");
        }

        @Test
        void reportsANonTerritorialFactionWithoutLettingItClaim() {

            var pirates = claimContest.buildFaction("pirates", false);
            var hegemony = claimContest.buildFaction("hegemony", true);

            claimContest.placeMarketsInSystem(
                claimContest.buildMarket(pirates, 9),
                claimContest.buildMarket(hegemony, 3));

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            // The pirates out-score everyone and still cannot take the system: territoriality
            // is a gate on claiming, not a discount on the score.
            assertThat(breakdown.scores())
                .extracting(FactionClaimStanding::factionId, FactionClaimStanding::isTerritorial)
                .containsExactly(tuple("pirates", false), tuple("hegemony", true));
            assertThat(breakdown.claimantFactionId())
                .isEqualTo("hegemony");
        }

        @Test
        void resolvesATiedContestToTheFirstMarketTheEconomyLists() {

            var hegemonyColony = claimContest.buildMarket(
                claimContest.buildFaction("hegemony", true),
                5);

            var tritachyonColony = claimContest.buildMarket(
                claimContest.buildFaction("tritachyon", true),
                5);

            var reader = new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN);

            claimContest.placeMarketsInSystem(hegemonyColony, tritachyonColony);

            var hegemonyFirst = reader.readBreakdown(claimContest.getSystem());

            claimContest.placeMarketsInSystem(tritachyonColony, hegemonyColony);

            var tritachyonFirst = reader.readBreakdown(claimContest.getSystem());

            // Equal scores never displace the leader, so the winner is decided purely by which
            // market the economy hands over first - the one part of the mechanic with no
            // in-world justification, and so the easiest to "improve" by accident.
            assertThat(hegemonyFirst.claimantFactionId())
                .isEqualTo("hegemony");
            assertThat(tritachyonFirst.claimantFactionId())
                .isEqualTo("tritachyon");
        }

        @Test
        void settlesATieOnMarketOrderRatherThanOnEachFactionsBestMarket() {

            var hegemony = claimContest.buildFaction("hegemony", true);
            var tritachyon = claimContest.buildFaction("tritachyon", true);

            claimContest.placeMarketsInSystem(
                claimContest.buildMarket(hegemony, 3),
                claimContest.buildMarket(tritachyon, 6),
                claimContest.buildMarket(hegemony, 5));

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            // Both factions stand at 6 - Tri-Tachyon on its size-6 colony, the Hegemony on its
            // size-5 one plus a sibling - and the mechanic walks market by market, so the tie
            // goes to whichever market reached the score first. Settling it over the finished
            // standings instead would hand the system to the Hegemony, whose first market comes
            // earlier in the listing but scores lower: the same ranking, the wrong claimant.
            assertThat(breakdown.scores())
                .extracting(FactionClaimStanding::factionId, FactionClaimStanding::score)
                .containsExactly(tuple("hegemony", 6), tuple("tritachyon", 6));
            assertThat(breakdown.claimantFactionId())
                .isEqualTo("tritachyon");
        }

        @Test
        void leavesEquallyScoredFactionsInEconomyOrder() {

            var hegemony = claimContest.buildFaction("hegemony", true);
            var tritachyon = claimContest.buildFaction("tritachyon", true);

            claimContest.placeMarketsInSystem(
                claimContest.buildMarket(tritachyon, 5),
                claimContest.buildMarket(hegemony, 5));

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            // Ranking is by score alone, and the sort has to be stable: a tie in the listing
            // must read in the order the tie was decided in, or the ranking would contradict
            // the claimant sitting above it.
            assertThat(breakdown.scores())
                .extracting(FactionClaimStanding::factionId)
                .containsExactly("tritachyon", "hegemony");
        }

        @Test
        void skipsAMarketWithNoOwningFaction() {

            var hegemony = claimContest.buildFaction("hegemony", true);

            claimContest.placeMarketsInSystem(
                claimContest.buildMarket(null, 9),
                claimContest.buildMarket(hegemony, 3));

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            // An unowned market belongs to nobody's standing, and the mechanic would throw on
            // one; skipping it keeps a malformed economy from taking down a hover read.
            assertThat(breakdown.scores())
                .extracting(FactionClaimStanding::factionId)
                .containsExactly("hegemony");
            assertThat(breakdown.claimantFactionId())
                .isEqualTo("hegemony");
        }

        @Test
        void reportsAnOverrideAsClaimantWhileStillScoringPresentFactions() {

            var hegemony = claimContest.buildFaction("hegemony", true);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(hegemony, 6));
            claimContest.overrideClaimingFaction("luddic_church");

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            // The override settles the claim without being scored for it, and the faction it
            // displaces keeps its score - that context is the whole point of the breakdown.
            assertThat(breakdown.overrideFactionId())
                .isEqualTo("luddic_church");
            assertThat(breakdown.claimantFactionId())
                .isEqualTo("luddic_church");
            assertThat(breakdown.scores())
                .extracting(FactionClaimStanding::factionId, FactionClaimStanding::score)
                .containsExactly(tuple("hegemony", 6));
        }

        @Test
        void reportsNoClaimantWhenOnlyNonTerritorialFactionsArePresent() {

            var pirates = claimContest.buildFaction("pirates", false);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(pirates, 9));

            var breakdown =
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem());

            assertThat(breakdown.overrideFactionId())
                .isNull();
            assertThat(breakdown.claimantFactionId())
                .isNull();
            assertThat(breakdown.scores())
                .hasSize(1);
        }

        @Test
        void resolvesTheSameContestThroughAPassIndexAsWithoutOne() {
            // Sharing a pass's colony walk is a cost decision and has to stay one. A contest that
            // came out differently through an index would let the map and a box over it disagree
            // about who claims a system on nothing but which of them was built inside a pass.
            var hegemony = claimContest.buildFaction("hegemony", true);
            var pirates = claimContest.buildFaction("pirates", false);

            claimContest.placeMarketsInSystem(
                claimContest.buildMarket(hegemony, 5),
                claimContest.buildMarket(pirates, 4));

            var throughIndex = new VanillaClaimBreakdownReader(
                    EVERY_COLONY_KNOWN,
                    new SystemColoniesIndex(Global.getSector()))
                .readBreakdown(claimContest.getSystem());
            var throughOwnWalk = new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                .readBreakdown(claimContest.getSystem());

            // Both stated against the same literals rather than against each other, so a pair that
            // drifted together still fails.
            assertThat(throughIndex.claimantFactionId())
                .isEqualTo("hegemony");
            assertThat(throughIndex.scores())
                .extracting(FactionClaimStanding::factionId, FactionClaimStanding::score)
                .containsExactly(tuple("hegemony", 5), tuple("pirates", 4));
            assertThat(throughOwnWalk.claimantFactionId())
                .isEqualTo("hegemony");
            assertThat(throughOwnWalk.scores())
                .extracting(FactionClaimStanding::factionId, FactionClaimStanding::score)
                .containsExactly(tuple("hegemony", 5), tuple("pirates", 4));
        }

        @Test
        void reportsAnUnweighedDerelictThePortWithholdsAsUnknown() {
            // The mechanic only ever weighs what the economy lists, and a listed station is an
            // outpost - so a derelict reaches a contest as an unweighed presence and never as a
            // score. What the port still does is decline to name it.
            var neutral = claimContest.buildFaction(Factions.NEUTRAL, false);
            var derelict = claimContest.buildMarket(neutral, 3);

            claimContest.markMarketAsAbandonedStation(derelict);
            claimContest.placeOffEconomyMarketsInSystem(derelict);

            var breakdown = new VanillaClaimBreakdownReader(NO_COLONY_KNOWN)
                .readBreakdown(claimContest.getSystem());

            assertThat(breakdown.scores())
                .singleElement()
                .isInstanceOf(PresenceOnlyClaimStanding.class);
            assertThat(breakdown.scores().get(0).readHeldMarkets())
                .extracting(MarketClaimBreakdown::isKnownToPlayer)
                .containsExactly(false);
        }

        @Test
        void weighsAnOutpostLikeAnyColony() {
            // A station a faction keeps and the economy lists. Vanilla reads only the listing and
            // tests no condition, so it scores at its size like any colony - the derelict
            // condition reaching no term of the arithmetic.
            var hegemony = claimContest.buildFaction("hegemony", true);
            var outpost = claimContest.buildMarket(hegemony, 3);

            claimContest.markMarketAsAbandonedStation(outpost);
            claimContest.placeMarketsInSystem(outpost);

            var standing = readTopStanding(
                new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readBreakdown(claimContest.getSystem()));

            assertThat(standing.standingMarket().computeTotalScore())
                .isEqualTo(3);
            assertThat(standing.standingMarket().isKnownToPlayer())
                .isTrue();
        }

        @Test
        void reportsEachMarketAsThePortAnswersForIt() {
            // Knowledge is answered over the whole place and spent per market, so a port naming
            // one of two colonies leaves the other unnamed - the reader carrying whichever answer
            // came back rather than one verdict for the system.
            var hegemony = claimContest.buildFaction("hegemony", true);
            var tritachyon = claimContest.buildFaction("tritachyon", true);
            var capital = claimContest.buildMarket(hegemony, 5);
            var rival = claimContest.buildMarket(tritachyon, 4);

            claimContest.nameMarket(capital, "Chicomoztoc");
            claimContest.nameMarket(rival, "Eventide");
            claimContest.placeMarketsInSystem(capital, rival);

            var breakdown = new VanillaClaimBreakdownReader(buildPortNaming("Chicomoztoc"))
                .readBreakdown(claimContest.getSystem());

            assertThat(breakdown.scores())
                .flatExtracting(FactionClaimStanding::readHeldMarkets)
                .extracting(
                    market -> market.marketNameplate().displayName(),
                    MarketClaimBreakdown::isKnownToPlayer)
                .containsExactlyInAnyOrder(
                    tuple("Chicomoztoc", true),
                    tuple("Eventide", false));
        }

        @Test
        void resolvesTheSameContestUnderEitherPort() {
            // Widening or narrowing what may be named never moves what is scored. The port's
            // answer rides on each market for a display to read, and vanilla's own - claimant,
            // scores and the order they rank in - is left exactly where it was.
            var hegemony = claimContest.buildFaction("hegemony", true);
            var pirates = claimContest.buildFaction("pirates", true);
            var derelict = claimContest.buildMarket(hegemony, 4);

            claimContest.markMarketAsAbandonedStation(derelict);
            claimContest.placeMarketsInSystem(
                derelict,
                claimContest.buildHiddenMarket(pirates, 6));

            var namingNothing = new VanillaClaimBreakdownReader(NO_COLONY_KNOWN)
                .readBreakdown(claimContest.getSystem());
            var namingEverything = new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                .readBreakdown(claimContest.getSystem());

            // Both stated against the same literals rather than against each other, so a pair
            // that drifted together still fails.
            assertThat(namingNothing.claimantFactionId())
                .isEqualTo("hegemony");
            assertThat(namingNothing.scores())
                .extracting(FactionClaimStanding::factionId, FactionClaimStanding::score)
                .containsExactly(tuple("hegemony", 4), tuple("pirates", 0));
            assertThat(namingEverything.claimantFactionId())
                .isEqualTo("hegemony");
            assertThat(namingEverything.scores())
                .extracting(FactionClaimStanding::factionId, FactionClaimStanding::score)
                .containsExactly(tuple("hegemony", 4), tuple("pirates", 0));
        }

        @Test
        void reportsNothingForANullSystem() {

            var breakdown = new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                .readBreakdown(null);

            assertThat(breakdown)
                .isEqualTo(SystemClaimBreakdown.NONE);
        }
    }

    @Nested
    class ReadClaimingFactionId {

        @Test
        void reportsTheTopScoringTerritorialFaction() {

            var hegemony = claimContest.buildFaction("hegemony", true);
            var tritachyon = claimContest.buildFaction("tritachyon", true);

            claimContest.placeMarketsInSystem(
                claimContest.buildMarket(hegemony, 4),
                claimContest.buildMarket(tritachyon, 7));

            assertThat(new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readClaimingFactionId(claimContest.getSystem()))
                .isEqualTo("tritachyon");
        }

        @Test
        void reportsTheOverrideAheadOfAnyScore() {

            var hegemony = claimContest.buildFaction("hegemony", true);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(hegemony, 6));
            claimContest.overrideClaimingFaction("luddic_church");

            assertThat(new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readClaimingFactionId(claimContest.getSystem()))
                .isEqualTo("luddic_church");
        }

        @Test
        void reportsUnclaimedWhenNoTerritorialFactionIsPresent() {

            var pirates = claimContest.buildFaction("pirates", false);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(pirates, 9));

            assertThat(new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readClaimingFactionId(claimContest.getSystem()))
                .isNull();
        }

        @Test
        void reportsUnclaimedForANullSystem() {
            assertThat(new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readClaimingFactionId(null))
                .isNull();
        }

        @Test
        void reportsTheSameFactionWhateverThePlayerHasFound() {
            // The claimant is vanilla's, and vanilla weighs what is there rather than what has been
            // found - so the answer cannot move with the knowledge port however little it admits.
            // What the port would otherwise buy on this walk is a projection of every colony in the
            // system, built for a set the claimant comparison never opens.
            var hegemony = claimContest.buildFaction("hegemony", true);
            var tritachyon = claimContest.buildFaction("tritachyon", true);

            claimContest.placeMarketsInSystem(
                claimContest.buildMarket(hegemony, 4),
                claimContest.buildMarket(tritachyon, 7));

            assertThat(new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readClaimingFactionId(claimContest.getSystem()))
                .isEqualTo("tritachyon");
            assertThat(new VanillaClaimBreakdownReader(NO_COLONY_KNOWN)
                    .readClaimingFactionId(claimContest.getSystem()))
                .isEqualTo("tritachyon");
        }

        @Test
        void passesOverAHiddenMarketThatWouldOtherwiseHaveTakenTheSystem() {
            // The claimant walk skips a concealed market before scoring, exactly as the breakdown's
            // does - the two share that comparison rather than each stating it - so the system falls
            // to the largest colony held in the open however big the base beside it is.
            var hegemony = claimContest.buildFaction("hegemony", true);
            var pirates = claimContest.buildFaction("pirates", true);

            claimContest.placeMarketsInSystem(
                claimContest.buildMarket(hegemony, 4),
                claimContest.buildHiddenMarket(pirates, 9));

            assertThat(new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readClaimingFactionId(claimContest.getSystem()))
                .isEqualTo("hegemony");
        }
    }

    @Nested
    class ReadCoreFactionId {

        @Test
        void reportsTheFactionIdTheFlagImposes() {

            claimContest.overrideClaimingFaction("luddic_church");

            assertThat(new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readCoreFactionId(claimContest.getSystem()))
                .isEqualTo("luddic_church");
        }

        @Test
        void reportsNoCoreWhenTheFlagIsUnset() {
            assertThat(new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readCoreFactionId(claimContest.getSystem()))
                .isNull();
        }

        @Test
        void reportsNoCoreForANullSystem() {
            assertThat(new VanillaClaimBreakdownReader(EVERY_COLONY_KNOWN)
                    .readCoreFactionId(null))
                .isNull();
        }
    }

    // The top-ranked standing, as the weighed kind every case reaching for it goes on to read a
    // market off. A contest that ranked a presence-only standing first fails the cast, which is
    // the finding those cases want rather than a null they would have to check for.
    private static WeighedClaimStanding readTopStanding(SystemClaimBreakdown breakdown) {
        return (WeighedClaimStanding) breakdown.scores().get(0);
    }

    // One faction's weighed place in a resolved contest, for a case posing several factions at once
    // and asserting on each - reaching by rank would tie the case to an ordering it is not about.
    // Narrowed to the weighed kind because every case reaching for it goes on to read the market
    // the standing rests on, which is the one thing only that kind has.
    private static WeighedClaimStanding readStanding(
            SystemClaimBreakdown breakdown,
            String factionId) {
        return breakdown
            .scores()
            .stream()
            .filter(WeighedClaimStanding.class::isInstance)
            .map(WeighedClaimStanding.class::cast)
            .filter(score -> score.factionId().equals(factionId))
            .findFirst()
            .orElseThrow();
    }
}
