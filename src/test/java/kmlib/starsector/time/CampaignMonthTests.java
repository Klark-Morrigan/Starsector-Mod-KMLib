package kmlib.starsector.time;

import com.fs.starfarer.api.campaign.CampaignClockAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins how a month is read off the clock and the key it is recorded under, the key being what saves already hold.
 */
final class CampaignMonthTests {

    @Nested
    class FormatKey {

        @Test
        void joinsTheCycleAndTheMonthWithAHyphen() {

            assertThat(new CampaignMonth(206, 1).formatKey())
                .isEqualTo("206-1");
        }

        @Test
        void keepsTwoDigitMonthsUnpadded() {

            // Unpadded, as the records already in saves were written; padding would miss every one of them.
            assertThat(new CampaignMonth(207, 12).formatKey())
                .isEqualTo("207-12");
        }
    }

    @Nested
    class ReadCurrentMonth {

        @Test
        void takesTheCycleAndTheMonthTheClockIsIn() {

            var clockMock = mock(CampaignClockAPI.class);

            when(clockMock.getCycle())
                .thenReturn(206);
            when(clockMock.getMonth())
                .thenReturn(3);

            assertThat(CampaignMonth.readCurrentMonth(clockMock))
                .isEqualTo(new CampaignMonth(206, 3));
        }
    }
}
