package kmlib.starsector.ui.render.gl.style;

import kmlib.starsector.ui.colour.AccentColours;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the treatment a body control takes for the press it answered: the alpha its light reaches at a given
 * point of the lift, and what the accent factory builds one from. The light's own arithmetic is the whole of
 * what a unit test can hold here - laying the quad is GL, exercised in-engine - and it is the half that
 * matters, since a light resolved at the wrong strength is a press that shows nothing rather than a draw
 * that fails.
 */
final class ControlPressLightTest {

    private static final float TOLERANCE = 0.0001f;

    // A light at a strength of its own rather than at the accent factory's, so a case reading an alpha reads
    // the number this fixture states and not one the factory could change under it.
    private static final Color LIGHT_COLOUR = new Color(200, 220, 255);
    private static final float FULL_PRESS_ALPHA = 0.4f;
    private static final ControlPressLight LIGHT = new ControlPressLight(LIGHT_COLOUR, FULL_PRESS_ALPHA);

    private static final float FULLY_OPAQUE = 1f;
    private static final float AT_THE_PEAK = 1f;
    private static final float HALF_SPENT = 0.5f;
    private static final float NOT_PRESSED = 0f;

    @Nested
    class ResolvePaintAtPressFraction {

        @Test
        void resolvePaintAtPressFractionReachesTheStatedAlphaAtTheLiftsPeak() {

            assertThat(LIGHT.resolvePaintAtPressFraction(AT_THE_PEAK, FULLY_OPAQUE).alpha())
                .isCloseTo(0.4f, within(TOLERANCE));
        }

        @Test
        void resolvePaintAtPressFractionScalesTheAlphaByHowFarTheLiftHasRun() {
            // A fraction rather than a flag is the whole point of the channel: a press falls away over its
            // own cycle, so the light has to come down with it rather than switch off at the end.
            assertThat(LIGHT.resolvePaintAtPressFraction(HALF_SPENT, FULLY_OPAQUE).alpha())
                .isCloseTo(0.2f, within(TOLERANCE));
        }

        @Test
        void resolvePaintAtPressFractionFadesTheLightWithThePanelsOwnOpacity() {
            // The light is part of the panel, so a panel drawn half faded flashes half as hard - a press that
            // ignored the opacity would be the one thing on a translucent panel painting at full strength.
            assertThat(LIGHT.resolvePaintAtPressFraction(AT_THE_PEAK, 0.5f).alpha())
                .isCloseTo(0.2f, within(TOLERANCE));
        }

        @Test
        void resolvePaintAtPressFractionHidesTheLightOfACellNoPressIsRunningOn() {
            // What keeps a strip nobody is clicking free: a hidden paint is skipped by the fill primitives,
            // so a spent lift emits nothing rather than compositing a run the blend discards.
            assertThat(LIGHT.resolvePaintAtPressFraction(NOT_PRESSED, FULLY_OPAQUE).isHidden())
                .isTrue();
        }

        @Test
        void resolvePaintAtPressFractionKeepsTheLightsOwnColour() {
            // Only the alpha answers to the lift: the shade is the look's, so a press can never move a
            // control toward a colour the panel names nowhere.
            assertThat(LIGHT.resolvePaintAtPressFraction(HALF_SPENT, FULLY_OPAQUE).colour())
                .isEqualTo(LIGHT_COLOUR);
        }
    }

    @Nested
    class CreateAccentPressLight {

        // The three steps of an accent, each distinguishable, so a factory taking the wrong one is a case
        // failure rather than one colour standing in for another.
        private static final AccentColours ACCENT = new AccentColours(
            new Color(20, 20, 20),
            new Color(100, 100, 100),
            new Color(220, 220, 220));

        @Test
        void createAccentPressLightLightsInTheAccentsBrightStep() {
            // The step reserved for what must read against the base, which is exactly this treatment's
            // problem: the cell it lands on is already washed in the base, so a light of that same step
            // would be a press into what the player is already looking at.
            assertThat(ControlPressLight.createAccentPressLight(ACCENT).colour())
                .isEqualTo(new Color(220, 220, 220));
        }

        @Test
        void createAccentPressLightStandsAboveTheHoverWashsOwnStrength() {
            // The ordering the treatment rests on: a press is made on a cell the pointer is already holding
            // fully washed, so its light has to reach past that wash. The hovered wash sits at 0.15.
            assertThat(ControlPressLight.createAccentPressLight(ACCENT).fullPressAlpha())
                .isCloseTo(0.25f, within(TOLERANCE));
        }
    }
}
