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
 * pair a save browser shows, and either half alone where the other is missing.
 *
 * <p>That it is never empty is the part with something riding on it. The label is what a capture's
 * rows are grouped under, and a group is registered by name - so a sector answering nothing would
 * take down the caller that asked to be told which game it was measuring, rather than costing it a
 * label it could not read.
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

        @Test
        void namesASectorWithNoSeedByItsPlayerAlone() {
            // A sector the game never generated - one built in memory rather than loaded from a
            // save - has no seed to give, and the player is then all there is to tell it by.
            var playerMock = mock(PersonAPI.class);

            when(playerMock.getNameString()).thenReturn(PLAYER_NAME);
            when(sectorMock.getSeedString()).thenReturn(null);
            when(sectorMock.getPlayerPerson()).thenReturn(playerMock);

            assertThat(SectorLabels.describeSector(sectorMock))
                .isEqualTo("Marat Kagan");
        }

        @Test
        void namesASectorWithNeitherHalfRatherThanAnsweringNothing() {
            // The answer a caller cannot be left without: the label names the group a capture's
            // rows sit in, and a group registered under nothing takes the caller down with it.
            when(sectorMock.getSeedString()).thenReturn(null);
            when(sectorMock.getPlayerPerson()).thenReturn(null);

            assertThat(SectorLabels.describeSector(sectorMock))
                .isEqualTo("unnamed sector");
        }
    }
}
