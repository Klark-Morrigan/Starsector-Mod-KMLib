package kmlib.starsector;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.characters.PersonAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the label a diagnostic names a sector by: the seed with the player beside it, which is the
 * pair a save browser shows, and the seed alone where there is nobody to name yet.
 */
final class SectorLabelsTest {

    private static final String SEED = "MN-6220";
    private static final String PLAYER_NAME = "Marat Kagan";

    private final SectorAPI sectorMock = mock(SectorAPI.class);

    @Nested
    class DescribeSector {

        @Test
        void composesTheSeedWithThePlayerName() {

            var playerMock = mock(PersonAPI.class);

            when(playerMock.getNameString()).thenReturn(PLAYER_NAME);
            when(sectorMock.getSeedString()).thenReturn(SEED);
            when(sectorMock.getPlayerPerson()).thenReturn(playerMock);

            assertThat(SectorLabels.describeSector(sectorMock))
                .isEqualTo("MN-6220 - Marat Kagan");
        }

        @Test
        void namesASectorWithNoPlayerByItsSeedAlone() {
            // A sector generated and not yet played into has nobody to name, and a label trailing
            // an empty half would read as a name that went missing rather than as one never given.
            when(sectorMock.getSeedString()).thenReturn(SEED);
            when(sectorMock.getPlayerPerson()).thenReturn(null);

            assertThat(SectorLabels.describeSector(sectorMock))
                .isEqualTo("MN-6220");
        }

        @Test
        void namesASectorWhosePlayerIsUnnamedByItsSeedAlone() {

            var playerMock = mock(PersonAPI.class);

            when(playerMock.getNameString()).thenReturn("");
            when(sectorMock.getSeedString()).thenReturn(SEED);
            when(sectorMock.getPlayerPerson()).thenReturn(playerMock);

            assertThat(SectorLabels.describeSector(sectorMock))
                .isEqualTo("MN-6220");
        }
    }
}
