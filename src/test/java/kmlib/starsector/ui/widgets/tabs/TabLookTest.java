package kmlib.starsector.ui.widgets.tabs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link TabLook}'s two blends: a pulse lifts the fill and the label by the same amount, and a fade
 * moves both the same way toward another look. Either applied to the fill alone would slide the surface out
 * from under text that stayed put, which is the failure these rule out.
 */
final class TabLookTest {

    // Distinct values in every channel of both shades, so a lift applied to the wrong one, or applied
    // twice to the same one, shows as a wrong number rather than as a coincidence.
    private static final TabLook LOOK = new TabLook(
        new Color(0, 100, 200, 128),
        new Color(200, 100, 0, 255));

    // The far end of a fade, its channels clear of the look above so a half-way blend lands on a value
    // neither end could produce on its own.
    private static final TabLook TARGET_LOOK = new TabLook(
        new Color(100, 200, 0, 32),
        new Color(0, 200, 100, 64));

    private static final Color WASH_TARGET = new Color(100, 200, 0);

    @Nested
    class ComputeBlendedLook {

        @Test
        void computeBlendedLookPlacesTheFillBetweenTheTwoLooks() {

            var blended = LOOK.computeBlendedLook(TARGET_LOOK, 0.5f);

            assertThat(blended.fill())
                .isEqualTo(new Color(50, 150, 100, 128));
        }

        @Test
        void computeBlendedLookMovesTheLabelTheSameWay() {

            var blended = LOOK.computeBlendedLook(TARGET_LOOK, 0.5f);

            assertThat(blended.label())
                .isEqualTo(new Color(100, 150, 50, 255));
        }

        @Test
        void computeBlendedLookKeepsEachShadesOwnAlphaRatherThanTheTargets() {
            // The strip fades as a unit through its own opacity, so a look's alpha is the shade's own and
            // must survive a blend - a fade that carried the target's alpha across would make a hovered tab
            // change transparency as well as colour.
            var blended = LOOK.computeBlendedLook(TARGET_LOOK, 1f);

            assertThat(blended)
                .isEqualTo(
                    new TabLook(
                        new Color(100, 200, 0, 128),
                        new Color(0, 200, 100, 255)));
        }

        @Test
        void computeBlendedLookLeavesTheLookWhereItIsAtNoTravel() {

            var blended = LOOK.computeBlendedLook(TARGET_LOOK, 0f);

            assertThat(blended)
                .isEqualTo(
                    new TabLook(
                        new Color(0, 100, 200, 128),
                        new Color(200, 100, 0, 255)));
        }

        @Test
        void computeBlendedLookSettlesOnTheTargetForAnOvershootingFraction() {
            // A fraction composed from more than one live channel can overshoot; blending past the target
            // would land on a colour neither end named.
            var blended = LOOK.computeBlendedLook(TARGET_LOOK, 1.5f);

            assertThat(blended.fill())
                .isEqualTo(new Color(100, 200, 0, 128));
        }
    }

    @Nested
    class ComputeWashedLook {

        @Test
        void computeWashedLookMovesTheFillTowardTheWashTarget() {

            var washed = LOOK.computeWashedLook(new TabWash(WASH_TARGET, 0.5f));

            assertThat(washed.fill())
                .isEqualTo(new Color(50, 150, 100, 128));
        }

        @Test
        void computeWashedLookMovesTheLabelByTheSameLift() {

            var washed = LOOK.computeWashedLook(new TabWash(WASH_TARGET, 0.5f));

            assertThat(washed.label())
                .isEqualTo(new Color(150, 150, 0, 255));
        }

        @Test
        void computeWashedLookLeavesBothShadesWhereTheyAreForARestingTab() {
            
            var washed = LOOK.computeWashedLook(TabWash.NONE);

            assertThat(washed)
                .isEqualTo(
                    new TabLook(
                        new Color(0, 100, 200, 128),
                        new Color(200, 100, 0, 255)));
        }
    }
}
