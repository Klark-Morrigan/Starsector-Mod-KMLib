package kmlib.starsector.fleet;

import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.testfixtures.starsector.fleet.PlayerCreditsFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the credits reads against the player fleet's cargo: the live handle, and affordability at, below and above the
 * balance.
 */
final class PlayerCreditsTests {

    private static final float BALANCE = 1_000f;

    @Nested
    class CanPlayerAfford {

        @Test
        void affordsAPriceEqualToTheBalance() {

            assertThat(PlayerCredits.canPlayerAfford(buildSectorHoldingBalance(), BALANCE))
                .isTrue();
        }

        @Test
        void affordsAPriceBelowTheBalance() {

            assertThat(PlayerCredits.canPlayerAfford(buildSectorHoldingBalance(), BALANCE - 1f))
                .isTrue();
        }

        @Test
        void cannotAffordAPriceAboveTheBalance() {

            assertThat(PlayerCredits.canPlayerAfford(buildSectorHoldingBalance(), BALANCE + 1f))
                .isFalse();
        }

        private SectorAPI buildSectorHoldingBalance() {

            var sectorMock = mock(SectorAPI.class);
            var creditsMock = PlayerCreditsFixture.stubPlayerCredits(sectorMock);

            when(creditsMock.get())
                .thenReturn(BALANCE);

            return sectorMock;
        }
    }

    @Nested
    class ReadPlayerCredits {

        @Test
        void readsTheCreditsInThePlayerFleetsCargo() {

            var sectorMock = mock(SectorAPI.class);
            var creditsMock = PlayerCreditsFixture.stubPlayerCredits(sectorMock);

            assertThat(PlayerCredits.readPlayerCredits(sectorMock))
                .isSameAs(creditsMock);
        }
    }
}
