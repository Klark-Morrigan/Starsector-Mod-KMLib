package kmlib.starsector.ui.render.gl.tooltip;

import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.starsector.ui.font.TextFace;
import kmlib.starsector.ui.text.RedactedSpan;
import kmlib.starsector.ui.text.TextAlignment;
import kmlib.starsector.ui.text.TextStyle;
import kmlib.starsector.ui.widgets.tooltip.TooltipStyle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins what a box states for itself against what it is given: that a look built the ordinary way rules
 * its leaders and sinks its redactions at the standard weights without the caller naming them, that a
 * host tuning either changes nothing else about the look, and that the leader weights can never arrive
 * missing.
 *
 * <p>The defaults are the cases worth fixing. They are what keeps one finding - how heavily a solid run
 * reads beside glyphs - in one place rather than in every host that ever builds a box, and a default that
 * quietly stopped being applied would show as leaders at whatever a fresh record happens to hold, which
 * on a float is no rule drawn at all, and as withheld names filled at the full weight of their line.
 */
class CursorTooltipStyleTest {

    private static final float OPACITY = 0.9f;
    private static final float BORDER_WIDTH = 1f;
    private static final Color FILL_COLOUR = Color.BLACK;
    private static final Color BORDER_COLOUR = new Color(170, 222, 255);

    // Weights unlike the standard pair in both parts, so a look carrying them says a host's own
    // statement reached it rather than the default having happened to match.
    private static final float TUNED_THICKNESS = 3f;
    private static final float TUNED_ALPHA_MULT = 0.2f;

    // A strength unlike the standard one, for the same reason: a look carrying it says the host's own
    // statement reached it rather than the default having happened to match.
    private static final float TUNED_DARKENING_STRENGTH = 0.55f;

    private static TooltipStyle createTypography() {
        // Built by hand rather than through TextStyle's own factory: its baseline colour resolves from
        // the running game's palette, which a value test has no business standing up for a colour it
        // never reads.
        var textStyle = new TextStyle(
            new TextFace(StarsectorFont.VANILLA_INSIGNIA_15, 15d),
            Color.WHITE,
            TextAlignment.TOP_LEFT,
            false);

        return TooltipStyle.createStyle(textStyle, textStyle);
    }

    private static CursorTooltipStyle createStyle() {
        return CursorTooltipStyle.createStyle(
            createTypography(),
            OPACITY,
            BORDER_WIDTH,
            FILL_COLOUR,
            BORDER_COLOUR);
    }

    @Nested
    class CreateStyle {

        @Test
        void createStyleRulesItsLeadersAtTheStandardWeights() {
            // The whole point of the factory: a host states its typography and its frame, and the rules
            // between label and value come out at the pair they read as greyed-out text at without it
            // having to know that pair exists.
            assertThat(createStyle().leaderLineStyle())
                .isEqualTo(TooltipLeaderLineStyle.TEXT_WEIGHTED);
        }

        @Test
        void createStyleRedactsAtTheStandardStrength() {
            // The same reasoning as the leaders above, over the other solid mark a box draws among its
            // glyphs: a host states its typography and its frame, and a withheld name comes out at the
            // strength it reads level with its own line at.
            assertThat(createStyle().redactionDarkeningStrength())
                .isEqualTo(RedactedSpan.TEXT_WEIGHT_DARKENING_STRENGTH);
        }

        @Test
        void createStyleKeepsWhatTheHostStated() {

            var style = createStyle();

            assertThat(style.opacity())
                .isEqualTo(OPACITY);
            assertThat(style.borderWidth())
                .isEqualTo(BORDER_WIDTH);
            assertThat(style.fillColour())
                .isEqualTo(FILL_COLOUR);
            assertThat(style.borderColour())
                .isEqualTo(BORDER_COLOUR);
        }
    }

    @Nested
    class RuledBy {

        @Test
        void ruledByRulesTheLeadersAtTheWeightsGiven() {

            var tuned = createStyle()
                .ruledBy(new TooltipLeaderLineStyle(TUNED_THICKNESS, TUNED_ALPHA_MULT));

            assertThat(tuned.leaderLineStyle())
                .isEqualTo(new TooltipLeaderLineStyle(TUNED_THICKNESS, TUNED_ALPHA_MULT));
        }

        @Test
        void ruledByLeavesTheRestOfTheLookAsItWas() {
            // A refinement states one thing and carries the rest over. A host moving a slider must not
            // find its frame or its fade moved with it.
            var style = createStyle();
            var tuned = style.ruledBy(new TooltipLeaderLineStyle(TUNED_THICKNESS, TUNED_ALPHA_MULT));

            assertThat(tuned.typography())
                .isEqualTo(style.typography());
            assertThat(tuned.opacity())
                .isEqualTo(OPACITY);
            assertThat(tuned.borderWidth())
                .isEqualTo(BORDER_WIDTH);
            assertThat(tuned.fillColour())
                .isEqualTo(FILL_COLOUR);
            assertThat(tuned.borderColour())
                .isEqualTo(BORDER_COLOUR);
        }
    }

    @Nested
    class RedactedAt {

        @Test
        void redactedAtSinksWithheldNamesByTheStrengthGiven() {

            assertThat(createStyle()
                .redactedAt(TUNED_DARKENING_STRENGTH)
                .redactionDarkeningStrength())
                .isEqualTo(TUNED_DARKENING_STRENGTH);
        }

        @Test
        void redactedAtLeavesTheRestOfTheLookAsItWas() {
            // A refinement states one thing and carries the rest over - the leaders included, so the two
            // sliders a host may put on a box cannot overwrite one another.
            var style = createStyle();
            var tuned = style.redactedAt(TUNED_DARKENING_STRENGTH);

            assertThat(tuned.typography())
                .isEqualTo(style.typography());
            assertThat(tuned.opacity())
                .isEqualTo(OPACITY);
            assertThat(tuned.borderWidth())
                .isEqualTo(BORDER_WIDTH);
            assertThat(tuned.fillColour())
                .isEqualTo(FILL_COLOUR);
            assertThat(tuned.borderColour())
                .isEqualTo(BORDER_COLOUR);
            assertThat(tuned.leaderLineStyle())
                .isEqualTo(style.leaderLineStyle());
        }
    }

    @Nested
    class Construct {

        @Test
        void constructRejectsAMissingLeaderLook() {
            // A box ruling no leaders states the weights at nothing rather than leaving them unstated,
            // so a null is a look built wrongly - and would otherwise surface inside a draw call, past
            // the point that could say which box was meant.
            assertThatThrownBy(() -> new CursorTooltipStyle(
                createTypography(),
                OPACITY,
                BORDER_WIDTH,
                FILL_COLOUR,
                BORDER_COLOUR,
                null,
                RedactedSpan.TEXT_WEIGHT_DARKENING_STRENGTH))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("leaderLineStyle");
        }
    }
}
