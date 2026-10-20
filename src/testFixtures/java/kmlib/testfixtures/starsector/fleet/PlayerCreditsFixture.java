package kmlib.testfixtures.starsector.fleet;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.util.MutableValue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The player's credits behind a sector mock: {@code sector.getPlayerFleet().getCargo().getCredits()} answers one
 * credits mock, so every walk down that chain reaches the handle a case verifies charges and refunds against.
 */
public final class PlayerCreditsFixture {

    private PlayerCreditsFixture() {
    }

    /**
     * Wires a player fleet and cargo under {@code sectorMock} and hands back the credits they answer.
     *
     * @param sectorMock the sector mock to wire
     * @return the credits mock the chain answers
     */
    public static MutableValue stubPlayerCredits(SectorAPI sectorMock) {

        var creditsMock = mock(MutableValue.class);
        var cargoMock = mock(CargoAPI.class);
        var playerFleetMock = mock(CampaignFleetAPI.class);

        when(cargoMock.getCredits())
            .thenReturn(creditsMock);
        when(playerFleetMock.getCargo())
            .thenReturn(cargoMock);
        when(sectorMock.getPlayerFleet())
            .thenReturn(playerFleetMock);

        return creditsMock;
    }
}
