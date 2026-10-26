package kmlib.starsector.fleet;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.testfixtures.starsector.fleet.PlayerCreditsFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the credits reads against the player fleet's cargo: the live handle, its absence without a fleet or cargo, and
 * affordability at, below and above the balance.
 */
final class PlayerCreditsTests {

    private static final float BALANCE = 1_000f;

    @Nested
    class CanPlayerAfford {

        @Test
        void affordsAPriceEqualToTheBalance() {

            // ARRANGE
            var sector = buildSectorHoldingBalance();

            // ACT
            var actualCanAfford = PlayerCredits.canPlayerAfford(sector, BALANCE);

            // ASSERT
            assertThat(actualCanAfford)
                .isTrue();
        }

        @Test
        void affordsAPriceBelowTheBalance() {

            // ARRANGE
            var sector = buildSectorHoldingBalance();

            // ACT
            var actualCanAfford = PlayerCredits.canPlayerAfford(sector, BALANCE - 1f);

            // ASSERT
            assertThat(actualCanAfford)
                .isTrue();
        }

        @Test
        void cannotAffordAPriceAboveTheBalance() {

            // ARRANGE
            var sector = buildSectorHoldingBalance();

            // ACT
            var actualCanAfford = PlayerCredits.canPlayerAfford(sector, BALANCE + 1f);

            // ASSERT
            assertThat(actualCanAfford)
                .isFalse();
        }

        // No player fleet means no credits to pay from, so even a free purchase is refused.
        @Test
        void cannotAffordAnythingWithoutAPlayerFleet() {

            // ACT
            var actualCanAfford = PlayerCredits.canPlayerAfford(mock(SectorAPI.class), 0f);

            // ASSERT
            assertThat(actualCanAfford)
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
    class FindPlayerCredits {

        @Test
        void findsTheCreditsInThePlayerFleetsCargo() {

            // ARRANGE
            var sectorMock = mock(SectorAPI.class);
            var expectedCredits = PlayerCreditsFixture.stubPlayerCredits(sectorMock);

            // ACT
            var actualCredits = PlayerCredits.findPlayerCredits(sectorMock);

            // ASSERT
            assertThat(actualCredits)
                .containsSame(expectedCredits);
        }

        // A sector has no player fleet while a new game is being generated.
        @Test
        void findsNothingWithoutAPlayerFleet() {

            // ACT
            var actualCredits = PlayerCredits.findPlayerCredits(mock(SectorAPI.class));

            // ASSERT
            assertThat(actualCredits)
                .isEmpty();
        }

        @Test
        void findsNothingWithoutCargo() {

            // ARRANGE
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getPlayerFleet())
                .thenReturn(mock(CampaignFleetAPI.class));

            // ACT
            var actualCredits = PlayerCredits.findPlayerCredits(sectorMock);

            // ASSERT
            assertThat(actualCredits)
                .isEmpty();
        }
    }
}
