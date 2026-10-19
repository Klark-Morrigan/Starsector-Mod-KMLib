package kmlib.starsector.economy;

import com.fs.starfarer.api.SettingsAPI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers telling the month's last economy step from the rest, against vanilla's ten steps a month.
 */
final class EconomyIterationsTests {

    private SettingsAPI settingsMock;

    @BeforeEach
    void setUp() {

        settingsMock = mock(SettingsAPI.class);

        when(settingsMock.getFloat("economyIterPerMonth"))
            .thenReturn(10f);
    }

    @Nested
    class IsLastIterationOfMonth {

        @Test
        void holdsForTheLastStep() {

            assertThat(EconomyIterations.isLastIterationOfMonth(settingsMock, 9))
                .isTrue();
        }

        @Test
        void failsForAnEarlierStep() {

            assertThat(EconomyIterations.isLastIterationOfMonth(settingsMock, 8))
                .isFalse();
        }

        @Test
        void failsForTheFirstStep() {

            assertThat(EconomyIterations.isLastIterationOfMonth(settingsMock, 0))
                .isFalse();
        }
    }
}
