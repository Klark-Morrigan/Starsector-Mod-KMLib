package kmlib.starsector.systems.claims;

import kmlib.testfixtures.starsector.systems.claims.ClaimStandingFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins {@link SystemClaimBreakdown}'s guarantee that the standings it carries are fixed, and the
 * questions it answers about its claimant. A breakdown is read repeatedly during a render pass, so
 * a caller that could see its scores change - or change them - would be reading a different contest
 * each time it looked. The questions are pinned because a reader asking one for itself could read a
 * blank claimant as somebody where its neighbour reads nobody.
 */
final class SystemClaimBreakdownTests {

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

            assertThat(breakdown.scores())
                .isEmpty();
        }
    }

    @Nested
    class FindStanding {

        @Test
        void answersTheNamedFactionsStanding() {

            var hegemony = ClaimStandingFixture.buildStandingOnOneMarket("hegemony", 6, true);
            var tritachyon = ClaimStandingFixture.buildStandingOnOneMarket("tritachyon", 4, true);
            var breakdown = new SystemClaimBreakdown(null, "hegemony", List.of(hegemony, tritachyon));

            assertThat(breakdown.findStanding("tritachyon"))
                .containsSame(tritachyon);
        }

        @Test
        void answersEmptyForAFactionHoldingNothingInTheSystem() {

            var breakdown = new SystemClaimBreakdown(
                "luddic_church",
                "luddic_church",
                List.of(ClaimStandingFixture.buildStandingOnOneMarket("hegemony", 6, true)));

            assertThat(breakdown.findStanding("luddic_church"))
                .isEmpty();
        }

        @Test
        void answersEmptyForANullFactionId() {

            var breakdown = new SystemClaimBreakdown(
                null,
                "hegemony",
                List.of(ClaimStandingFixture.buildStandingOnOneMarket("hegemony", 6, true)));

            assertThat(breakdown.findStanding(null))
                .isEmpty();
        }
    }

    @Nested
    class HasClaimant {

        @Test
        void isTrueWhereAFactionClaimsTheSystem() {

            assertThat(new SystemClaimBreakdown(null, "hegemony", null).hasClaimant())
                .isTrue();
        }

        @Test
        void isFalseWhereNobodyClaimsTheSystem() {

            assertThat(new SystemClaimBreakdown(null, null, null)
                .hasClaimant()).isFalse();
        }

        @Test
        void readsABlankClaimantAsNobody() {

            assertThat(new SystemClaimBreakdown(null, "", null)
                .hasClaimant()).isFalse();
            assertThat(new SystemClaimBreakdown(null, "   ", null)
                .hasClaimant()).isFalse();
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
    class IsClaimedByScore {

        @Test
        void isTrueForTheClaimantTheContestSettledOn() {

            assertThat(new SystemClaimBreakdown(null, "hegemony", null).isClaimedByScore("hegemony"))
                .isTrue();
        }

        @Test
        void isFalseForAnotherFaction() {

            assertThat(new SystemClaimBreakdown(null, "hegemony", null).isClaimedByScore("tritachyon"))
                .isFalse();
        }

        @Test
        void isFalseForTheClaimantADecreeImposed() {

            assertThat(new SystemClaimBreakdown("luddic_church", "luddic_church", null)
                    .isClaimedByScore("luddic_church"))
                .isFalse();
        }

        @Test
        void isFalseWhereNobodyClaimsTheSystem() {

            assertThat(new SystemClaimBreakdown(null, null, null).isClaimedByScore(null))
                .isFalse();
        }
    }

    @Nested
    class None {

        @Test
        void carriesNoClaimantNoOverrideAndNoStandings() {

            assertThat(SystemClaimBreakdown.NONE.overrideFactionId())
                .isNull();
            assertThat(SystemClaimBreakdown.NONE.claimantFactionId())
                .isNull();
            assertThat(SystemClaimBreakdown.NONE.scores())
                .isEmpty();
        }
    }
}
