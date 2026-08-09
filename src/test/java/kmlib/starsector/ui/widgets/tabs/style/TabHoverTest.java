package kmlib.starsector.ui.widgets.tabs.style;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two rules a pointer can answer by, and what parts them: a strip's tabs converge on one shade
 * whatever they were showing, where a row of buttons lights each from where it already stands and so keeps
 * the shown one apart from the rest. Both are pinned at the far end of the fade and part-way along it,
 * since the difference between converging and lifting is invisible at either end alone.
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
    }

    @Nested
    class AddedGlow {

        @Test
        void TabHover_AddedGlow_computeHoveredLook_liftsEachLookFromWhereItStands() {
            // Added light, so two buttons lit by the same amount stay as far apart as they started - the
            // difference from the shade above, and what keeps the shown button readable while the pointer
            // is on one of its neighbours.
            var hover = new TabHover.AddedGlow(GLOW_COLOUR, GLOW_AMOUNT);

            assertThat(hover.computeHoveredLook(RESTING_LOOK, FULLY_HOVERED).fill())
                .isEqualTo(new Color(70, 140, 80));
            assertThat(hover.computeHoveredLook(LIT_LOOK, FULLY_HOVERED).fill())
                .isEqualTo(new Color(130, 200, 140));
        }

        @Test
        void TabHover_AddedGlow_computeHoveredLook_liftsTheLabelWithTheFill() {
            // A button brightens as one piece: light landing on the surface but not on the text over it
            // would read as a fill sliding out from under its own label.
            assertThat(new TabHover.AddedGlow(GLOW_COLOUR, GLOW_AMOUNT)
                    .computeHoveredLook(RESTING_LOOK, FULLY_HOVERED)
                    .label())
                .isEqualTo(new Color(80, 150, 90));
        }

        @Test
        void TabHover_AddedGlow_computeHoveredLook_scalesTheLightByHowFarTheFadeHasRun() {
            // A fade part-way in adds part of the light, so a button travels onto its lit shade rather than
            // switching to it - half the amount at half the fade.
            assertThat(new TabHover.AddedGlow(GLOW_COLOUR, GLOW_AMOUNT)
                    .computeHoveredLook(RESTING_LOOK, HALF_HOVERED)
                    .fill())
                .isEqualTo(new Color(45, 90, 70));
        }

        @Test
        void TabHover_AddedGlow_computeHoveredLook_paintsAnUnpaintedSurfaceWithTheLightItself() {
            // A chrome stating its resting interior as a fill at zero alpha has nothing for the pointer to
            // brighten, so the light has to be what paints it: brightened channels on a surface still drawn
            // at nothing would leave an unshown button answering with its label alone.
            var unpainted = new TabLook(new Color(80, 100, 120, 0), new Color(90, 110, 130));

            assertThat(new TabHover.AddedGlow(GLOW_COLOUR, GLOW_AMOUNT)
                    .computeHoveredLook(unpainted, FULLY_HOVERED)
                    .fill()
                    .getAlpha())
                .isEqualTo(128);
        }

        @Test
        void TabHover_AddedGlow_computeHoveredLook_takesTheLightBackOffAsThePointerLeaves() {
            // The far end matters as much as the near one for an unpainted surface: what the light painted
            // has to come back off, or a button once pointed at would keep a shade of its own for good.
            var unpainted = new TabLook(new Color(80, 100, 120, 0), new Color(90, 110, 130));

            assertThat(new TabHover.AddedGlow(GLOW_COLOUR, GLOW_AMOUNT)
                    .computeHoveredLook(unpainted, NOT_HOVERED))
                .isEqualTo(unpainted);
        }
    }
}
