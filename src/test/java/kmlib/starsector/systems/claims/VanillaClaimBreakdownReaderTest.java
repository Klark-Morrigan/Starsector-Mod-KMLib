package kmlib.starsector.systems.claims;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

/**
 * Pins {@link VanillaClaimBreakdownReader} to the mechanic it mirrors: the market filter, the
 * size / sibling / military score, the territoriality gate, and the strictly-greater
 * comparison that leaves a tied contest with the first market the economy lists. Drift in any
 * of those would make a claim explanation disagree with the map fill it explains, which is the
 * failure this suite exists to catch.
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
            claimContest.placeMarketsInSystem(claimContest.buildMarket(hegemony, 5),
                    garrison);

            var breakdown =
                    new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            // The size-3 garrison outweighs the size-5 colony on the military bonus alone
            // (3 + 1 sibling + 10 against 5 + 1), and the faction stands on that one market -
            // holdings are never summed.
            assertThat(breakdown.scores())
                    .extracting(FactionClaimScore::factionId, FactionClaimScore::score)
                    .containsExactly(tuple("hegemony", 14));
            assertThat(breakdown.claimantFactionId()).isEqualTo("hegemony");
        }

        @Test
        void countsHiddenSiblingMarketsTowardsAScore() {
            var hegemony = claimContest.buildFaction("hegemony", true);
            claimContest.placeMarketsInSystem(claimContest.buildMarket(hegemony, 3),
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
        void excludesHiddenAndPlayerMarketsFromTheContest() {
            var pirates = claimContest.buildFaction("pirates", true);
            var player = claimContest.buildFaction("player", true);
            when(player.isPlayerFaction()).thenReturn(true);
            claimContest.placeMarketsInSystem(claimContest.buildHiddenMarket(pirates, 6),
                    claimContest.buildMarket(player, 8));

            var breakdown =
                    new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            assertThat(breakdown.scores()).isEmpty();
            assertThat(breakdown.claimantFactionId()).isNull();
        }

        @Test
        void ranksPresentFactionsByScoreDescending() {
            var hegemony = claimContest.buildFaction("hegemony", true);
            var tritachyon = claimContest.buildFaction("tritachyon", true);
            claimContest.placeMarketsInSystem(claimContest.buildMarket(hegemony, 4),
                    claimContest.buildMarket(tritachyon, 7));

            var breakdown =
                    new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            assertThat(breakdown.scores())
                    .extracting(FactionClaimScore::factionId, FactionClaimScore::score)
                    .containsExactly(tuple("tritachyon", 7), tuple("hegemony", 4));
            assertThat(breakdown.claimantFactionId()).isEqualTo("tritachyon");
        }

        @Test
        void reportsANonTerritorialFactionWithoutLettingItClaim() {
            var pirates = claimContest.buildFaction("pirates", false);
            var hegemony = claimContest.buildFaction("hegemony", true);
            claimContest.placeMarketsInSystem(claimContest.buildMarket(pirates, 9),
                    claimContest.buildMarket(hegemony, 3));

            var breakdown =
                    new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            // The pirates out-score everyone and still cannot take the system: territoriality
            // is a gate on claiming, not a discount on the score.
            assertThat(breakdown.scores())
                    .extracting(FactionClaimScore::factionId, FactionClaimScore::isTerritorial)
                    .containsExactly(tuple("pirates", false), tuple("hegemony", true));
            assertThat(breakdown.claimantFactionId()).isEqualTo("hegemony");
        }

        @Test
        void resolvesATiedContestToTheFirstMarketTheEconomyLists() {
            var hegemonyColony =
                    claimContest.buildMarket(claimContest.buildFaction("hegemony", true), 5);
            var tritachyonColony = claimContest.buildMarket(
                    claimContest.buildFaction("tritachyon", true), 5);
            var reader = new VanillaClaimBreakdownReader();

            claimContest.placeMarketsInSystem(hegemonyColony, tritachyonColony);
            var hegemonyFirst = reader.readBreakdown(claimContest.getSystem());
            claimContest.placeMarketsInSystem(tritachyonColony, hegemonyColony);
            var tritachyonFirst = reader.readBreakdown(claimContest.getSystem());

            // Equal scores never displace the leader, so the winner is decided purely by which
            // market the economy hands over first - the one part of the mechanic with no
            // in-world justification, and so the easiest to "improve" by accident.
            assertThat(hegemonyFirst.claimantFactionId()).isEqualTo("hegemony");
            assertThat(tritachyonFirst.claimantFactionId()).isEqualTo("tritachyon");
        }

        @Test
        void leavesEquallyScoredFactionsInEconomyOrder() {
            var hegemony = claimContest.buildFaction("hegemony", true);
            var tritachyon = claimContest.buildFaction("tritachyon", true);
            claimContest.placeMarketsInSystem(claimContest.buildMarket(tritachyon, 5),
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
            claimContest.placeMarketsInSystem(claimContest.buildMarket(null, 9),
                    claimContest.buildMarket(hegemony, 3));

            var breakdown =
                    new VanillaClaimBreakdownReader().readBreakdown(claimContest.getSystem());

            // An unowned market belongs to nobody's standing, and the mechanic would throw on
            // one; skipping it keeps a malformed economy from taking down a hover read.
            assertThat(breakdown.scores())
                    .extracting(FactionClaimScore::factionId)
                    .containsExactly("hegemony");
            assertThat(breakdown.claimantFactionId()).isEqualTo("hegemony");
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
            assertThat(breakdown.overrideFactionId()).isEqualTo("luddic_church");
            assertThat(breakdown.claimantFactionId()).isEqualTo("luddic_church");
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

            assertThat(breakdown.overrideFactionId()).isNull();
            assertThat(breakdown.claimantFactionId()).isNull();
            assertThat(breakdown.scores()).hasSize(1);
        }

        @Test
        void reportsNothingForANullSystem() {
            var breakdown = new VanillaClaimBreakdownReader().readBreakdown(null);

            assertThat(breakdown).isEqualTo(SystemClaimBreakdown.NONE);
        }
    }

    @Nested
    class ReadCoreFactionId {
        @Test
        void reportsTheFactionIdTheFlagImposes() {
            claimContest.overrideClaimingFaction("luddic_church");

            assertThat(new VanillaClaimBreakdownReader()
                    .readCoreFactionId(claimContest.getSystem())).isEqualTo("luddic_church");
        }

        @Test
        void reportsNoCoreWhenTheFlagIsUnset() {
            assertThat(new VanillaClaimBreakdownReader()
                    .readCoreFactionId(claimContest.getSystem())).isNull();
        }

        @Test
        void reportsNoCoreForANullSystem() {
            assertThat(new VanillaClaimBreakdownReader().readCoreFactionId(null)).isNull();
        }
    }
}
