package kmlib.starsector.ui.render.gl.style;

import kmlib.starsector.ui.colour.AccentColours;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the treatment a body control takes under the pointer: the alpha its wash reaches at a given point of
 * its fade, and what the accent factory builds one from. The wash's own arithmetic is the whole of what a
 * unit test can hold here - laying the quad is GL, exercised in-engine - and it is the half that matters,
 * since a wash resolved at the wrong strength is a panel lit wrongly on every frame rather than a draw that
 * fails.
 */
final class ControlHoverWashTest {

    private static final float TOLERANCE = 0.0001f;

    // A wash at a strength of its own rather than at the accent factory's, so a case reading an alpha reads
    // the number this fixture states and not one the factory could change under it.
    private static final Color WASH_COLOUR = new Color(40, 80, 160);
    private static final float FULL_HOVER_ALPHA = 0.4f;
    private static final ControlHoverWash WASH = new ControlHoverWash(WASH_COLOUR, FULL_HOVER_ALPHA);

    private static final float FULLY_OPAQUE = 1f;
    private static final float FULLY_HOVERED = 1f;
    private static final float HALF_HOVERED = 0.5f;
    private static final float NOT_HOVERED = 0f;

    @Nested
    class ResolvePaintAtHoverFraction {

        @Test
        void resolvePaintAtHoverFractionReachesTheStatedAlphaOnAFullHover() {

            assertThat(WASH.resolvePaintAtHoverFraction(FULLY_HOVERED, FULLY_OPAQUE).alpha())
                .isCloseTo(0.4f, within(TOLERANCE));
        }

        @Test
        void resolvePaintAtHoverFractionScalesTheAlphaByHowFarTheFadeHasRun() {
            // A fraction rather than a flag is the whole point of the channel: a cell part-way onto its
            // hovered look washes part-way, so the lift eases in with the pointer rather than switching on.
            assertThat(WASH.resolvePaintAtHoverFraction(HALF_HOVERED, FULLY_OPAQUE).alpha())
                .isCloseTo(0.2f, within(TOLERANCE));
        }

        @Test
        void resolvePaintAtHoverFractionFadesTheWashWithThePanelsOwnOpacity() {
            // The wash is part of the panel, so a panel drawn half faded washes half as hard - a lift that
            // ignored the opacity would be the one thing on a translucent panel painting at full strength.
            assertThat(WASH.resolvePaintAtHoverFraction(FULLY_HOVERED, 0.5f).alpha())
                .isCloseTo(0.2f, within(TOLERANCE));
        }

        @Test
        void resolvePaintAtHoverFractionHidesTheWashOfARestingCell() {
            // What keeps a resting strip free: a hidden paint is skipped by the fill primitives, so a
            // control nobody is pointing at emits nothing rather than compositing a run the blend discards.
            assertThat(WASH.resolvePaintAtHoverFraction(NOT_HOVERED, FULLY_OPAQUE).isHidden())
                .isTrue();
        }

        @Test
        void resolvePaintAtHoverFractionKeepsTheWashsOwnColour() {
            // Only the alpha answers to the fade: the shade is the look's, so a lift can never move a
            // control toward a colour the panel names nowhere.
            assertThat(WASH.resolvePaintAtHoverFraction(HALF_HOVERED, FULLY_OPAQUE).colour())
                .isEqualTo(WASH_COLOUR);
        }
    }

    @Nested
    class CreateAccentHoverWash {

        // The three steps of an accent, each distinguishable, so a factory taking the wrong one is a case
        // failure rather than one colour standing in for another.
        private static final AccentColours ACCENT = new AccentColours(
            new Color(20, 20, 20),
            new Color(100, 100, 100),
            new Color(220, 220, 220));

        @Test
        void createAccentHoverWashWashesInTheAccentsBaseStep() {
            // The step every control on the panel already washes and strokes with: a hover is more of what
            // the control wears, not a second colour. The bright step is for marks that must read against
            // the base, which a wash laid under the chrome is not.
            assertThat(ControlHoverWash.createAccentHoverWash(ACCENT).colour())
                .isEqualTo(new Color(100, 100, 100));
        }

        @Test
        void createAccentHoverWashStandsBelowTheSelectedWashsOwnStrength() {
            // The ordering the treatment rests on: one colour at two amounts, so a hovered cell reads as
            // lit-without-being-picked and a hovered selected cell lifts past both. The selected wash sits
            // at 0.30, so this must stay under it.
            assertThat(ControlHoverWash.createAccentHoverWash(ACCENT).fullHoverAlpha())
                .isCloseTo(0.15f, within(TOLERANCE));
        }
    }
}
