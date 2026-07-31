package kmlib.starsector.systems.claims;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link VanillaClaimReader#readClaimingFactionId}: it reports the
 * claimant the scored contest resolves to, honours an override outright, and treats an
 * unreadable system as unclaimed.
 *
 * <p>The cases drive the real contest rather than a stubbed claimant. The narrow read exists to
 * give a caller the winner without the reasoning, and a stubbed winner would assert nothing
 * about it being the same winner the reasoning arrives at.
 */
class VanillaClaimReaderTest {

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
    class ReadClaimingFactionId {
        @Test
        void reportsTheTopScoringTerritorialFaction() {
            var hegemony = claimContest.buildFaction("hegemony", true);
            var tritachyon = claimContest.buildFaction("tritachyon", true);
            claimContest.placeMarketsInSystem(claimContest.buildMarket(hegemony, 4),
                claimContest.buildMarket(tritachyon, 7));

            var claimantId = new VanillaClaimReader()
                .readClaimingFactionId(claimContest.getSystem());

            assertThat(claimantId).isEqualTo("tritachyon");
        }

        @Test
        void reportsTheOverrideAheadOfAnyScore() {
            var hegemony = claimContest.buildFaction("hegemony", true);
            claimContest.placeMarketsInSystem(claimContest.buildMarket(hegemony, 6));
            claimContest.overrideClaimingFaction("luddic_church");

            var claimantId = new VanillaClaimReader()
                .readClaimingFactionId(claimContest.getSystem());

            assertThat(claimantId).isEqualTo("luddic_church");
        }

        @Test
        void reportsUnclaimedWhenNoTerritorialFactionIsPresent() {
            var pirates = claimContest.buildFaction("pirates", false);
            claimContest.placeMarketsInSystem(claimContest.buildMarket(pirates, 9));

            var claimantId = new VanillaClaimReader()
                .readClaimingFactionId(claimContest.getSystem());

            assertThat(claimantId).isNull();
        }

        @Test
        void reportsUnclaimedForNullSystem() {
            assertThat(new VanillaClaimReader().readClaimingFactionId(null)).isNull();
        }
    }
}
