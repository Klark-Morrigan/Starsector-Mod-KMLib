package kmlib.starsector.ui.input;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TabPanelController}'s construction seams: the default opens the panel expanded and the
 * docked-start factory opens it collapsed, so a host picks the initial fold through construction rather
 * than driving the animation to reach it. The pointer routing and scroll delegation run against live input
 * events and are exercised in-engine, so only the collapse seed is unit-pinned here.
 */
final class TabPanelControllerTest {
    private static final float TOLERANCE = 0.0001f;

    @Nested
    class Constructor {

        @Test
        void tabPanelControllerOpensFullyExpandedAtZeroCollapseFractionByDefault() {
            assertThat(new TabPanelController().getCollapseFraction()).isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class CreateStartingDocked {

        @Test
        void createStartingDockedOpensFullyCollapsedAtTheDockedRail() {
            assertThat(TabPanelController.createStartingDocked().getCollapseFraction())
                    .isCloseTo(1f, within(TOLERANCE));
        }
    }
}
