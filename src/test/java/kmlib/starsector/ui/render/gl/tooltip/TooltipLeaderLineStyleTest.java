package kmlib.starsector.ui.render.gl.tooltip;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the floor both leader weights are held to, and that the standard pair is the one a rule reads as
 * greyed-out text at.
 *
 * <p>The floor is worth fixing because these are the two values a player moves directly: a slider is
 * bounded, but the value it writes is read back through a settings store that answers with whatever it
 * holds, and a negative reaching the paint would have it fill a quad running backwards or blend toward a
 * colour it was never given. Zero is deliberately left standing - it is how the rules are turned off.
 */
class TooltipLeaderLineStyleTest {

    @Nested
    class Construct {

        @Test
        void constructKeepsTheWeightsAHostStates() {

            var leaderLineStyle = new TooltipLeaderLineStyle(2f, 0.4f);

            assertThat(leaderLineStyle.thickness())
                .isEqualTo(2f);
            assertThat(leaderLineStyle.alphaMult())
                .isEqualTo(0.4f);
        }

        @Test
        void constructKeepsNothingAsAWeightInItsOwnRight() {
            // Not floored away, because it is a statement rather than a mistake: a host - or a player at
            // the near end of either slider - turns the rules off by asking for none of them.
            var switchedOff = new TooltipLeaderLineStyle(0f, 0f);

            assertThat(switchedOff.thickness())
                .isEqualTo(0f);
            assertThat(switchedOff.alphaMult())
                .isEqualTo(0f);
        }

        @Test
        void constructFloorsANegativeThickness() {

            var leaderLineStyle = new TooltipLeaderLineStyle(-3f, 0.65f);

            assertThat(leaderLineStyle.thickness())
                .isEqualTo(0f);
        }

        @Test
        void constructFloorsANegativeAlphaMultiplier() {

            var leaderLineStyle = new TooltipLeaderLineStyle(1f, -0.5f);

            assertThat(leaderLineStyle.alphaMult())
                .isEqualTo(0f);
        }
    }

    @Nested
    class TextWeighted {

        @Test
        void textWeightedRulesAWholeUnitLetDownTowardTheText() {
            // A whole unit, because a run thinner than a pixel would strengthen and fade with where the
            // row landed on the pixel grid as the box followed the cursor; the weight comes off the alpha
            // instead, which composites the same wherever the run lands.
            assertThat(TooltipLeaderLineStyle.TEXT_WEIGHTED.thickness())
                .isEqualTo(1f);
            assertThat(TooltipLeaderLineStyle.TEXT_WEIGHTED.alphaMult())
                .isEqualTo(0.65f);
        }
    }
}
