package kmlib.starsector.systems.claims;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import kmlib.starsector.colonies.ColonyVisibility;
import kmlib.starsector.colonies.RevelationGate;
import kmlib.starsector.systems.SystemColoniesIndex;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

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

    // Both spoiler gates raised, which is the rule a map pass hands down by default. Posed against
    // the fog alone below to show the contest is deaf to the difference.
    private static final ColonyVisibility BOTH_GATES_ON = new ColonyVisibility(
        false,
        Set.of(RevelationGate.SPACE_DERELICTS, RevelationGate.HIDDEN_COLONIES));

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

            var claimantId = new VanillaClaimReader(ColonyVisibility.BASE_FOG)
                .readClaimingFactionId(claimContest.getSystem());

            assertThat(claimantId)
                .isEqualTo("tritachyon");
        }

        @Test
        void reportsTheOverrideAheadOfAnyScore() {

            var hegemony = claimContest.buildFaction("hegemony", true);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(hegemony, 6));
            claimContest.overrideClaimingFaction("luddic_church");

            var claimantId = new VanillaClaimReader(ColonyVisibility.BASE_FOG)
                .readClaimingFactionId(claimContest.getSystem());

            assertThat(claimantId)
                .isEqualTo("luddic_church");
        }

        @Test
        void reportsUnclaimedWhenNoTerritorialFactionIsPresent() {

            var pirates = claimContest.buildFaction("pirates", false);

            claimContest.placeMarketsInSystem(claimContest.buildMarket(pirates, 9));

            var claimantId = new VanillaClaimReader(ColonyVisibility.BASE_FOG)
                .readClaimingFactionId(claimContest.getSystem());

            assertThat(claimantId)
                .isNull();
        }

        @Test
        void reportsUnclaimedForNullSystem() {

            assertThat(new VanillaClaimReader(ColonyVisibility.BASE_FOG)
                    .readClaimingFactionId(null))
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
                    ColonyVisibility.BASE_FOG,
                    new SystemColoniesIndex(Global.getSector()))
                .readClaimingFactionId(claimContest.getSystem());

            var throughOwnWalk = new VanillaClaimReader(ColonyVisibility.BASE_FOG)
                .readClaimingFactionId(claimContest.getSystem());

            // Both stated against the same literal rather than against each other, so a pair that
            // drifted together still fails.
            assertThat(throughPass)
                .isEqualTo("hegemony");
            assertThat(throughOwnWalk)
                .isEqualTo("hegemony");
        }

        @Test
        void reportsOneClaimantWhateverRuleTheColoniesAreShownUnder() {
            // The rule decides only what the breakdowns behind the claimant say the player may be
            // told; the contest is scored off the unfogged set. A claimant that moved with the rule
            // would hand a system to a different faction for no reason but which surface, holding
            // which gates, happened to open the reader.
            var hegemony = claimContest.buildFaction("hegemony", true);
            var tritachyon = claimContest.buildFaction("tritachyon", true);
            var derelict = claimContest.buildMarket(
                claimContest.buildFaction(Factions.NEUTRAL, false), 3);

            // A derelict in a system nobody has been seen in is exactly what the gates hold back,
            // so the two rules genuinely part company over this system's colonies.
            claimContest.markMarketAsAbandonedStation(derelict);
            claimContest.placeMarketsInSystem(
                claimContest.buildMarket(hegemony, 4),
                claimContest.buildMarket(tritachyon, 7),
                derelict);

            var underFogAlone = new VanillaClaimReader(ColonyVisibility.BASE_FOG)
                .readClaimingFactionId(claimContest.getSystem());

            var underBothGates = new VanillaClaimReader(BOTH_GATES_ON)
                .readClaimingFactionId(claimContest.getSystem());

            assertThat(underFogAlone)
                .isEqualTo("tritachyon");
            assertThat(underBothGates)
                .isEqualTo("tritachyon");
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
                    .openReaderOver(
                        ColonyVisibility.BASE_FOG,
                        new SystemColoniesIndex(Global.getSector()))
                    .readClaimingFactionId(claimContest.getSystem()))
                .isEqualTo("hegemony");
        }
    }
}
