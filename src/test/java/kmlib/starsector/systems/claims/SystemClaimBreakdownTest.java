package kmlib.starsector.systems.claims;

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
            var scores = new ArrayList<FactionClaimScore>();
            scores.add(new FactionClaimScore("hegemony", 6, true));
            var breakdown = new SystemClaimBreakdown(null, "hegemony", scores);

            scores.clear();

            assertThat(breakdown.scores())
                    .extracting(FactionClaimScore::factionId)
                    .containsExactly("hegemony");
        }

        @Test
        void rejectsAnAttemptToChangeTheStandings() {
            var breakdown = new SystemClaimBreakdown(
                    null, "hegemony", List.of(new FactionClaimScore("hegemony", 6, true)));

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
    class None {
        @Test
        void carriesNoClaimantNoOverrideAndNoStandings() {
            assertThat(SystemClaimBreakdown.NONE.overrideFactionId()).isNull();
            assertThat(SystemClaimBreakdown.NONE.claimantFactionId()).isNull();
            assertThat(SystemClaimBreakdown.NONE.scores()).isEmpty();
        }
    }
}
