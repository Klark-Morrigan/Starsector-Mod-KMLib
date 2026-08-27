package kmlib.starsector.ui.widgets.tabs.style;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two rules a pointer can answer by, and what parts them: a strip's tabs converge on one named
 * shade whatever they were showing, where a row of buttons is lit by light added over the finished tab and
 * so keeps the shown one apart from the rest. Each rule answers on one channel and stays silent on the
 * other, which is what stops a tab being brightened twice; both are pinned at the far end of the fade and
 * part-way along it, since a scaling that ignored the fade is invisible at the ends alone.
 */
final class TabHoverTest {

    // Two settled looks far apart on every channel, so a rule reading the wrong one - or converging where
    // it should lift - lands on a number no case here expects.
    private static final TabLook RESTING_LOOK = new TabLook(
        new Color(20, 40, 60),
        new Color(30, 50, 70));

    private static final TabLook LIT_LOOK = new TabLook(
        new Color(80, 100, 120),
        new Color(90, 110, 130));

    private static final TabLook MEETING_LOOK = new TabLook(
        new Color(200, 200, 200),
        new Color(220, 220, 220));

    // The light a button adds, and how much of it: a colour unlike either settled look, at an amount whose
    // products land on whole channels, so an expectation reads as a literal rather than as a rounding.
    private static final Color GLOW_COLOUR = new Color(100, 200, 40);
    private static final float GLOW_AMOUNT = 0.5f;

    private static final float FULLY_HOVERED = 1f;
    private static final float HALF_HOVERED = 0.5f;
    private static final float NOT_HOVERED = 0f;

    @Nested
    class MeetingShade {

        @Test
        void TabHover_MeetingShade_computeHoveredLook_bringsEverySettledLookToTheOneShade() {
            // The whole point of the rule: two tabs starting from different shades arrive at the same one,
            // which is what lets a strip mark the shown tab by fill and still light whatever is hovered.
            var hover = new TabHover.MeetingShade(MEETING_LOOK);

            assertThat(hover.computeHoveredLook(RESTING_LOOK, FULLY_HOVERED))
                .isEqualTo(MEETING_LOOK);
            assertThat(hover.computeHoveredLook(LIT_LOOK, FULLY_HOVERED))
                .isEqualTo(MEETING_LOOK);
        }

        @Test
        void TabHover_MeetingShade_computeHoveredLook_leavesASettledLookAloneWithNoPointerOnIt() {
            // The near end has to be exactly the settled look, or a row at rest would sit a shade off the
            // one its palette named.
            assertThat(new TabHover.MeetingShade(MEETING_LOOK)
                    .computeHoveredLook(RESTING_LOOK, NOT_HOVERED))
                .isEqualTo(RESTING_LOOK);
        }

        @Test
        void TabHover_MeetingShade_computeAddedLight_addsNoLightAtAnyPointOfTheFade() {
            // A shade rule says everything it has to say in the look. Light on top of it would brighten a
            // tab that has already arrived at the shade it was meant to arrive at.
            assertThat(new TabHover.MeetingShade(MEETING_LOOK)
                    .computeAddedLight(FULLY_HOVERED))
                .isEqualTo(TabLight.NONE);
        }
    }

    @Nested
    class AddedGlow {

        @Test
        void TabHover_AddedGlow_computeHoveredLook_leavesTheSettledLookForTheLightToFinish() {
            // The surface is not where this rule lands: its light is added over the finished tab, so a look
            // brightened here as well would put the pointer on twice, once mixed in and once added.
            var hover = new TabHover.AddedGlow(GLOW_COLOUR, GLOW_AMOUNT);

            assertThat(hover.computeHoveredLook(RESTING_LOOK, FULLY_HOVERED))
                .isEqualTo(RESTING_LOOK);
            assertThat(hover.computeHoveredLook(LIT_LOOK, FULLY_HOVERED))
                .isEqualTo(LIT_LOOK);
        }

        @Test
        void TabHover_AddedGlow_computeAddedLight_addsItsOwnLightInFullWhenFullyHovered() {
            // What the rule actually contributes, and in the form a chrome can add rather than mix: the
            // colour as it comes, at the weight the palette named.
            assertThat(new TabHover.AddedGlow(GLOW_COLOUR, GLOW_AMOUNT)
                    .computeAddedLight(FULLY_HOVERED))
                .isEqualTo(new TabLight(GLOW_COLOUR, GLOW_AMOUNT));
        }

        @Test
        void TabHover_AddedGlow_computeAddedLight_scalesTheLightByHowFarTheFadeHasRun() {
            // A fade part-way in adds part of the light, so a button travels onto its lit state rather than
            // switching to it - half the weight at half the fade, on the one colour throughout.
            assertThat(new TabHover.AddedGlow(GLOW_COLOUR, GLOW_AMOUNT)
                    .computeAddedLight(HALF_HOVERED))
                .isEqualTo(new TabLight(GLOW_COLOUR, GLOW_AMOUNT / 2f));
        }

        @Test
        void TabHover_AddedGlow_computeAddedLight_addsNothingWithNoPointerOnIt() {
            // The near end has to add nothing at all rather than a little: a row at rest would otherwise
            // stand a shade above the palette it was built from, on every tab at once.
            assertThat(new TabHover.AddedGlow(GLOW_COLOUR, GLOW_AMOUNT)
                    .computeAddedLight(NOT_HOVERED)
                    .isLit())
                .isFalse();
        }
    }
}
