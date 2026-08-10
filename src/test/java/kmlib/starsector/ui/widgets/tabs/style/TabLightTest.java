package kmlib.starsector.ui.widgets.tabs.style;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what a light guarantees whoever paints it: a weight inside the unit range, and a straight answer to
 * whether it is worth a pass at all. Both matter because the value is composed from live interaction - a
 * fade part-way along, scaled by a palette's own amount - and a chrome reading it decides between drawing a
 * pass and skipping one.
 */
final class TabLightTest {

    private static final Color LIGHT_COLOUR = new Color(100, 200, 40);

    @Nested
    class Constructor {

        @Test
        void TabLight_constructor_confinesAWeightAboveTheRange() {
            // A composed fraction can overshoot; unclamped it would drive an additive pass past the light
            // the palette named, and the further past it went the whiter the tab would wash.
            assertThat(new TabLight(LIGHT_COLOUR, 1.4f).weight())
                .isEqualTo(1f);
        }

        @Test
        void TabLight_constructor_confinesAWeightBelowTheRange() {
            // The other end matters more: an additive pass at a negative weight is a colour subtracted from
            // whatever is behind the tab, which reads as a hole rather than as no light at all.
            assertThat(new TabLight(LIGHT_COLOUR, -0.3f).weight())
                .isEqualTo(0f);
        }

        @Test
        void TabLight_constructor_leavesAWeightInsideTheRangeAlone() {
            assertThat(new TabLight(LIGHT_COLOUR, 0.17f).weight())
                .isEqualTo(0.17f);
        }
    }

    @Nested
    class IsLit {

        @Test
        void TabLight_isLit_answersFalseForALightThatWouldChangeNothing() {
            // What a chrome skips a whole pass on, so it has to be false at exactly the weight that adds
            // nothing rather than near it.
            assertThat(TabLight.NONE.isLit())
                .isFalse();
            assertThat(new TabLight(LIGHT_COLOUR, 0f).isLit())
                .isFalse();
        }

        @Test
        void TabLight_isLit_answersTrueForAnyWeightAtAll() {
            // A fade only just begun is still something to draw: skipping it would make the first frames of
            // every hover missing rather than faint.
            assertThat(new TabLight(LIGHT_COLOUR, 0.01f).isLit())
                .isTrue();
        }
    }
}
