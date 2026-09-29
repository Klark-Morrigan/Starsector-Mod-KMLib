package kmlib.starsector.systems.claims;

import kmlib.testfixtures.starsector.systems.claims.ClaimStandingFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins {@link SystemClaimBreakdown}'s guarantee that the standings it carries are fixed. A
 * breakdown is read repeatedly during a render pass, so a caller that could see its scores
 * change - or change them - would be reading a different contest each time it looked.
 */
final class SystemClaimBreakdownTest {

    @Nested
    class Construct {
        @Test
        void keepsTheStandingsItWasBuiltWithWhenTheSourceListChangesLater() {
            var scores = new ArrayList<FactionClaimStanding>();
            scores.add(ClaimStandingFixture.buildStandingOnOneMarket("hegemony", 6, true));
            var breakdown = new SystemClaimBreakdown(null, "hegemony", scores);

            scores.clear();

            assertThat(breakdown.scores())
                .extracting(FactionClaimStanding::factionId)
                .containsExactly("hegemony");
        }

        @Test
        void rejectsAnAttemptToChangeTheStandings() {
            var breakdown = new SystemClaimBreakdown(
                null,
                "hegemony",
                List.of(ClaimStandingFixture.buildStandingOnOneMarket("hegemony", 6, true)));

            assertThatThrownBy(() -> breakdown.scores().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void readsAbsentStandingsAsNone() {
            var breakdown = new SystemClaimBreakdown("luddic_church", "luddic_church", null);

            assertThat(breakdown.scores()).isEmpty();
        }
    }

    @Nested
    class IsSettledByDecree {

        @Test
        void isTrueWhereTheMemoryFlagNamedAFaction() {
            assertThat(new SystemClaimBreakdown("luddic_church", "luddic_church", null)
                    .isSettledByDecree())
                .isTrue();
        }

        @Test
        void isFalseWhereTheContestSettledTheSystem() {
            assertThat(new SystemClaimBreakdown(null, "hegemony", null).isSettledByDecree())
                .isFalse();
        }

        @Test
        void readsAnEmptyOverrideAsNoDecreeAtAll() {
            // The reading the question is named for. A flag present but naming nobody imposes
            // nothing, and a reader testing the field for being set rather than for saying anything
            // would report a decree here while its neighbour reported none.
            assertThat(new SystemClaimBreakdown("", "hegemony", null).isSettledByDecree())
                .isFalse();
            assertThat(new SystemClaimBreakdown("   ", "hegemony", null).isSettledByDecree())
                .isFalse();
        }
    }

    @Nested
    class IsClaimedByDecree {

        @Test
        void isTrueWhereTheClaimantIsTheDecreedFaction() {
            assertThat(new SystemClaimBreakdown("luddic_church", "luddic_church", null)
                    .isClaimedByDecree())
                .isTrue();
        }

        @Test
        void isFalseWhereAnotherFactionIsNamedAsTheClaimant() {
            // The narrowing this question exists for: a line naming the claimant may only mark it
            // as holding by decree where the decree is what put it there.
            assertThat(new SystemClaimBreakdown("luddic_church", "hegemony", null)
                    .isClaimedByDecree())
                .isFalse();
        }

        @Test
        void isFalseWhereNobodyClaimsTheSystem() {
            assertThat(new SystemClaimBreakdown("luddic_church", null, null).isClaimedByDecree())
                .isFalse();
        }

        @Test
        void isFalseWhereTheContestSettledTheSystem() {
            assertThat(new SystemClaimBreakdown(null, "hegemony", null).isClaimedByDecree())
                .isFalse();
        }
    }

    @Nested
    class None {
        @Test
        void carriesNoClaimantNoOverrideAndNoStandings() {
            assertThat(SystemClaimBreakdown.NONE.overrideFactionId()).isNull();
            assertThat(SystemClaimBreakdown.NONE.claimantFactionId()).isNull();
            assertThat(SystemClaimBreakdown.NONE.scores()).isEmpty();
        }
    }
}
