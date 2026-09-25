package kmlib.starsector.time;

import com.fs.starfarer.api.campaign.CampaignClockAPI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the countdown arithmetic against a clock that answers whatever elapsed-days value a case needs: the
 * remainder, its zero clamp, and where the slack moves "complete".
 */
final class CampaignCountdownTest {

    private static final float DURATION_DAYS = 5f;
    private static final float SLACK_DAYS = 0.1f;
    private static final long START_TIMESTAMP = 1_000L;

    private CampaignClockAPI clockMock;

    private static CampaignCountdown startCountdown(float slackDays) {

        return new CampaignCountdown(START_TIMESTAMP, DURATION_DAYS, slackDays);
    }

    @BeforeEach
    void setUp() {

        clockMock = mock(CampaignClockAPI.class);
    }

    @Nested
    class Constructor {

        @Test
        void rejectsANegativeSlack() {

            assertThatThrownBy(() -> new CampaignCountdown(START_TIMESTAMP, DURATION_DAYS, -0.1f))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class ComputeRemainingDays {

        @Test
        void subtractsTheElapsedDaysFromTheDuration() {

            when(clockMock.getElapsedDaysSince(START_TIMESTAMP))
                .thenReturn(2f);

            assertThat(startCountdown(SLACK_DAYS).computeRemainingDays(clockMock))
                .isEqualTo(3f);
        }

        @Test
        void clampsAtZeroWhenTheElapsedDaysExceedTheDuration() {

            when(clockMock.getElapsedDaysSince(START_TIMESTAMP))
                .thenReturn(12f);

            assertThat(startCountdown(SLACK_DAYS).computeRemainingDays(clockMock))
                .isZero();
        }
    }

    @Nested
    class IsComplete {

        @Test
        void isFalseWhileTheRemainingDaysSitAboveTheSlack() {

            when(clockMock.getElapsedDaysSince(START_TIMESTAMP))
                .thenReturn(4.85f);

            assertThat(startCountdown(SLACK_DAYS).isComplete(clockMock))
                .isFalse();
        }

        @Test
        void isTrueOnceTheRemainingDaysFallInsideTheSlack() {

            // Half the slack left, so the verdict does not hang on single-precision fuzz at the exact boundary.
            when(clockMock.getElapsedDaysSince(START_TIMESTAMP))
                .thenReturn(4.95f);

            assertThat(startCountdown(SLACK_DAYS).isComplete(clockMock))
                .isTrue();
        }

        @Test
        void isFalseJustShortOfTheDurationWithNoSlack() {

            when(clockMock.getElapsedDaysSince(START_TIMESTAMP))
                .thenReturn(4.95f);

            assertThat(startCountdown(0f).isComplete(clockMock))
                .isFalse();
        }

        @Test
        void isTrueFromTheStartForANegativeDuration() {

            when(clockMock.getElapsedDaysSince(START_TIMESTAMP))
                .thenReturn(0f);

            assertThat(new CampaignCountdown(START_TIMESTAMP, -1f, 0f).isComplete(clockMock))
                .isTrue();
        }

        @Test
        void isTrueOnceTheElapsedDaysReachTheDurationWithNoSlack() {

            when(clockMock.getElapsedDaysSince(START_TIMESTAMP))
                .thenReturn(DURATION_DAYS);

            assertThat(startCountdown(0f).isComplete(clockMock))
                .isTrue();
        }
    }
}
