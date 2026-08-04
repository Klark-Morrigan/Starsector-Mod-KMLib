package kmlib.starsector.ui.widgets.tabs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link TabLook#computeWashedLook}: a pulse moves the fill and the label by the same lift, so a
 * brightening tab reads as one piece. A lift applied to the fill alone would slide the surface out from
 * under text that stayed put, which is the failure this rules out.
 */
final class TabLookTest {

    // Distinct values in every channel of both shades, so a lift applied to the wrong one, or applied
    // twice to the same one, shows as a wrong number rather than as a coincidence.
    private static final TabLook LOOK = new TabLook(
        new Color(0, 100, 200, 128),
        new Color(200, 100, 0, 255));

    private static final Color WASH_TARGET = new Color(100, 200, 0);

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
