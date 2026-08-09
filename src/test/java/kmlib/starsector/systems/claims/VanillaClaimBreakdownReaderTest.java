package kmlib.starsector.systems.claims;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
 */
final class VanillaClaimBreakdownReaderTest {

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
                new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            // The size-3 garrison outweighs the size-5 colony on the military bonus alone
            // (3 + 1 sibling + 10 against 5 + 1), and the faction stands on that one market -
            // holdings are never summed.
            assertThat(breakdown.scores())
                .extracting(FactionClaimScore::factionId, FactionClaimScore::score)
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

            var standing = new VanillaClaimBreakdownReader()
                .readBreakdown(claimContest.getSystem())
                .scores()
                .get(0);

            // The standing rests on the one strongest market, and the rest are carried beside
            // it: the sibling point inside its score is exactly the one market listed under it,
            // which is what lets a reader check the number rather than take it on trust.
            assertThat(standing.standingMarket().marketName())
                .isEqualTo("Chicomoztoc");
            assertThat(standing.standingMarket().marketSize())
                .isEqualTo(5);
            assertThat(standing.standingMarket().siblingMarketCount())
                .isEqualTo(1);
            assertThat(standing.otherMarkets())
                .extracting(MarketClaimBreakdown::marketName)
                .containsExactly("Kazeron");
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

            var standing = new VanillaClaimBreakdownReader()
                .readBreakdown(claimContest.getSystem())
                .scores()
                .get(0);

            // Economy order, not score order: the weakest colony leads because that is where
            // the economy put it. A caller wanting them ranked sorts them itself, and one that
            // read them as already ranked would be quietly wrong on every multi-colony system.
            assertThat(standing.standingMarket().marketName())
                .isEqualTo("Chicomoztoc");
            assertThat(standing.standingMarket().siblingMarketCount())
                .isEqualTo(2);
            assertThat(standing.otherMarkets())
                .extracting(MarketClaimBreakdown::marketName)
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

            var breakdown = new VanillaClaimBreakdownReader()
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

            var breakdown = new VanillaClaimBreakdownReader()
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
                new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            // The bonus is what turns a small garrison into a claimant, so the box explaining a
            // border has to be able to name it as the term that did it - and an ordinary colony
            // must carry no bonus at all rather than one worth nothing.
            assertThat(breakdown.scores().get(0).standingMarket().militaryBonus())
                .hasValue(10);
            assertThat(breakdown.scores().get(1).standingMarket().militaryBonus())
                .isEmpty();
        }

        @Test
        void countsHiddenSiblingMarketsTowardsAScore() {

            var hegemony = claimContest.buildFaction("hegemony", true);

            claimContest.placeMarketsInSystem(
                claimContest.buildMarket(hegemony, 3),
                claimContest.buildHiddenMarket(hegemony, 2));

            var breakdown =
                new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            // A hidden market cannot claim on its own account but still counts as presence for
            // the faction holding it, so the visible colony scores 3 + 1 rather than 3.
            assertThat(breakdown.scores())
                .extracting(FactionClaimScore::factionId, FactionClaimScore::score)
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

            var standing = new VanillaClaimBreakdownReader()
                .readBreakdown(claimContest.getSystem())
                .scores()
                .get(0);

            // The larger base never stands for the faction, yet it is what the sibling point is
            // made of, so it is listed with the rest: dropping it would leave a count with
            // nothing beneath it to account for.
            assertThat(standing.standingMarket().marketName())
                .isEqualTo("Kanta's Den");
            assertThat(standing.standingMarket().siblingMarketCount())
                .isEqualTo(1);
            assertThat(standing.otherMarkets())
                .extracting(MarketClaimBreakdown::marketName)
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

            claimContest.placeMarketsInSystem(claimContest.buildHiddenMarket(pirates, 6));

            var breakdown =
                new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            // A standing of its own would count the base twice - it already reaches the contest
            // through the sibling count - and would let it displace whatever visible colony its
            // faction actually contests the system with.
            assertThat(breakdown.scores())
                .isEmpty();
            assertThat(breakdown.claimantFactionId())
                .isNull();
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

            var standing = new VanillaClaimBreakdownReader()
                .readBreakdown(claimContest.getSystem())
                .scores()
                .get(0);

            // The station is on the map in the faction's colours, so an account of the system that
            // never mentions it says less than the player can already see. It takes no standing all
            // the same: vanilla's walk covers the economy, and it was never registered.
            assertThat(standing.standingMarket().marketName())
                .isEqualTo("Ancyra");
            assertThat(standing.otherMarkets())
                .extracting(MarketClaimBreakdown::marketName)
                .containsExactly("Galatia Academy");
            assertThat(standing.otherMarkets().get(0).isOffEconomyMarket())
                .isTrue();
            assertThat(standing.standingMarket().isOffEconomyMarket())
                .isFalse();
        }

        @Test
        void keepsAMarketTheEconomyDoesNotListOutOfTheSiblingCount() {

            var independent = claimContest.buildFaction("independent", true);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(independent, 5));
            claimContest.placeOffEconomyMarketsInSystem(claimContest.buildMarket(independent, 6));

            var breakdown =
                new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            // Vanilla counts siblings over the economy's own markets, so admitting an unregistered
            // one would raise a real colony above the score the game scores it at - and could hand
            // the system to a different faction, which is a mechanic change, not a fuller account.
            assertThat(breakdown.scores())
                .extracting(FactionClaimScore::factionId, FactionClaimScore::score)
                .containsExactly(tuple("independent", 5));
        }

        @Test
        void leavesTheClaimWithATerritorialFactionAnUnlistedMarketOutscores() {

            var hegemony = claimContest.buildFaction("hegemony", true);
            var independent = claimContest.buildFaction("independent", true);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(hegemony, 3));
            claimContest.placeOffEconomyMarketsInSystem(claimContest.buildMarket(independent, 9));

            var breakdown =
                new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            // The unlisted colony out-sizes everything present and still takes nothing: it never
            // reaches a standing at all, so the claimant is the one vanilla itself would name.
            assertThat(breakdown.scores())
                .extracting(FactionClaimScore::factionId)
                .containsExactly("hegemony");
            assertThat(breakdown.claimantFactionId())
                .isEqualTo("hegemony");
        }

        @Test
        void scoresThePlayerAsAPresenceThatCannotClaim() {

            var player = claimContest.buildFaction("player", true);

            claimContest.markFactionAsPlayer(player);
            claimContest.placeMarketsInSystem(claimContest.buildMarket(player, 8));

            var breakdown =
                new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            // Scored, so a system the player holds a colony in never reports as one they have
            // no presence in - but non-territorial however the faction is configured, since the
            // mechanic bars a player colony from claiming outright.
            assertThat(breakdown.scores())
                .extracting(
                    FactionClaimScore::factionId,
                    FactionClaimScore::score,
                    FactionClaimScore::isTerritorial)
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
                new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            // The player tops the standings and still loses the system: scoring it widens what
            // the breakdown reports without touching the winner vanilla would name.
            assertThat(breakdown.scores())
                .extracting(FactionClaimScore::factionId, FactionClaimScore::score)
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
                new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            assertThat(breakdown.scores())
                .extracting(FactionClaimScore::factionId, FactionClaimScore::score)
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
                new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            // The pirates out-score everyone and still cannot take the system: territoriality
            // is a gate on claiming, not a discount on the score.
            assertThat(breakdown.scores())
                .extracting(FactionClaimScore::factionId, FactionClaimScore::isTerritorial)
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

            var reader = new VanillaClaimBreakdownReader();

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
                new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            // Both factions stand at 6 - Tri-Tachyon on its size-6 colony, the Hegemony on its
            // size-5 one plus a sibling - and the mechanic walks market by market, so the tie
            // goes to whichever market reached the score first. Settling it over the finished
            // standings instead would hand the system to the Hegemony, whose first market comes
            // earlier in the listing but scores lower: the same ranking, the wrong claimant.
            assertThat(breakdown.scores())
                .extracting(FactionClaimScore::factionId, FactionClaimScore::score)
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
                new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            // Ranking is by score alone, and the sort has to be stable: a tie in the listing
            // must read in the order the tie was decided in, or the ranking would contradict
            // the claimant sitting above it.
            assertThat(breakdown.scores())
                .extracting(FactionClaimScore::factionId)
                .containsExactly("tritachyon", "hegemony");
        }

        @Test
        void skipsAMarketWithNoOwningFaction() {

            var hegemony = claimContest.buildFaction("hegemony", true);

            claimContest.placeMarketsInSystem(
                claimContest.buildMarket(null, 9),
                claimContest.buildMarket(hegemony, 3));

            var breakdown =
                new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            // An unowned market belongs to nobody's standing, and the mechanic would throw on
            // one; skipping it keeps a malformed economy from taking down a hover read.
            assertThat(breakdown.scores())
                .extracting(FactionClaimScore::factionId)
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
                new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            // The override settles the claim without being scored for it, and the faction it
            // displaces keeps its score - that context is the whole point of the breakdown.
            assertThat(breakdown.overrideFactionId())
                .isEqualTo("luddic_church");
            assertThat(breakdown.claimantFactionId())
                .isEqualTo("luddic_church");
            assertThat(breakdown.scores())
                .extracting(FactionClaimScore::factionId, FactionClaimScore::score)
                .containsExactly(tuple("hegemony", 6));
        }

        @Test
        void reportsNoClaimantWhenOnlyNonTerritorialFactionsArePresent() {

            var pirates = claimContest.buildFaction("pirates", false);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(pirates, 9));

            var breakdown =
                new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            assertThat(breakdown.overrideFactionId())
                .isNull();
            assertThat(breakdown.claimantFactionId())
                .isNull();
            assertThat(breakdown.scores())
                .hasSize(1);
        }

        @Test
        void reportsNothingForANullSystem() {

            var breakdown = new VanillaClaimBreakdownReader().readBreakdown(null);

            assertThat(breakdown)
                .isEqualTo(SystemClaimBreakdown.NONE);
        }
    }

    @Nested
    class ReadCoreFactionId {
        @Test
        void reportsTheFactionIdTheFlagImposes() {

            claimContest.overrideClaimingFaction("luddic_church");

            assertThat(new VanillaClaimBreakdownReader()
                    .readCoreFactionId(claimContest.getSystem()))
                .isEqualTo("luddic_church");
        }

        @Test
        void reportsNoCoreWhenTheFlagIsUnset() {
            assertThat(new VanillaClaimBreakdownReader()
                    .readCoreFactionId(claimContest.getSystem()))
                .isNull();
        }

        @Test
        void reportsNoCoreForANullSystem() {
            assertThat(new VanillaClaimBreakdownReader().readCoreFactionId(null))
                .isNull();
        }
    }

    // One faction's place in a resolved contest, for a case posing several factions at once and
    // asserting on each - reaching by rank would tie the case to an ordering it is not about.
    private static FactionClaimScore readStanding(
            SystemClaimBreakdown breakdown,
            String factionId) {

        return breakdown
            .scores()
            .stream()
            .filter(score -> score.factionId().equals(factionId))
            .findFirst()
            .orElseThrow();
    }
}
