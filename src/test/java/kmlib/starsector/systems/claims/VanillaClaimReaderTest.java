package kmlib.starsector.systems.claims;

import com.fs.starfarer.api.Global;

import kmlib.starsector.systems.SystemColoniesIndex;

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
 *
 * <p>Beside them sits the {@link ClaimReaderSource} binding, which is this class's constructor
 * seen as a port: a caller holds the source and opens a reader over a pass, so the binding is
 * pinned where the reader it opens is.
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

            claimContest.placeMarketsInSystem(
                claimContest.buildMarket(hegemony, 4),
                claimContest.buildMarket(tritachyon, 7));

            var claimantId = new VanillaClaimReader()
                .readClaimingFactionId(claimContest.getSystem());

            assertThat(claimantId)
                .isEqualTo("tritachyon");
        }

        @Test
        void reportsTheOverrideAheadOfAnyScore() {

            var hegemony = claimContest.buildFaction("hegemony", true);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(hegemony, 6));
            claimContest.overrideClaimingFaction("luddic_church");

            var claimantId = new VanillaClaimReader()
                .readClaimingFactionId(claimContest.getSystem());

            assertThat(claimantId)
                .isEqualTo("luddic_church");
        }

        @Test
        void reportsUnclaimedWhenNoTerritorialFactionIsPresent() {

            var pirates = claimContest.buildFaction("pirates", false);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(pirates, 9));

            var claimantId = new VanillaClaimReader()
                .readClaimingFactionId(claimContest.getSystem());

            assertThat(claimantId)
                .isNull();
        }

        @Test
        void reportsUnclaimedForNullSystem() {

            assertThat(new VanillaClaimReader().readClaimingFactionId(null))
                .isNull();
        }

        @Test
        void reportsOneClaimantWhetherOrNotAPassSuppliedTheColonies() {
            // Where a reader's colonies come from is a cost decision. A claimant that came out
            // differently through a pass would let the map and a box over it disagree about who
            // claims a system on nothing but which of them was built inside one.
            var hegemony = claimContest.buildFaction("hegemony", true);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(hegemony, 4));

            var throughPass = new VanillaClaimReader(
                    new SystemColoniesIndex(Global.getSector()))
                .readClaimingFactionId(claimContest.getSystem());

            var throughOwnWalk = new VanillaClaimReader()
                .readClaimingFactionId(claimContest.getSystem());

            // Both stated against the same literal rather than against each other, so a pair that
            // drifted together still fails.
            assertThat(throughPass)
                .isEqualTo("hegemony");
            assertThat(throughOwnWalk)
                .isEqualTo("hegemony");
        }
    }

    @Nested
    class OpenReaderOver {

        @Test
        void bindsTheConstructorAsAClaimReaderSource() {
            // The providers hold a source rather than a reader so they can open one per pass. The
            // binding is what makes that possible at all, so it is pinned here rather than left
            // to whichever caller happens to write it out.
            var hegemony = claimContest.buildFaction("hegemony", true);
            
            claimContest.placeMarketsInSystem(claimContest.buildMarket(hegemony, 4));

            ClaimReaderSource source = VanillaClaimReader::new;

            assertThat(source
                    .openReaderOver(new SystemColoniesIndex(Global.getSector()))
                    .readClaimingFactionId(claimContest.getSystem()))
                .isEqualTo("hegemony");
        }
    }
}
