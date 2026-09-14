package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import kmlib.starsector.strings.StarsectorStrings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins {@link EntityNameGenerator#generateJumpPointName}: the focus label leads,
 * then the localised jump-point word, then the orbit radius in scientific
 * notation - and a focus with no display name of its own falls back to its
 * containing location's name (vanilla's "unknown location" placeholder counts as
 * no name) and finally its ID, so a nameless abyssal center still yields a
 * readable label.
 *
 * <p>The localised word is pulled through {@code StarsectorStrings}, which reads
 * strings.json via {@code Global}; it is stubbed to a fixed word so the test
 * pins the assembly, not the localisation lookup. Cases live under a
 * {@link Nested} group named for the method under test.
 */
final class EntityNameGeneratorTest {

    private MockedStatic<StarsectorStrings> stringsMock;

    @BeforeEach
    void setUp() {

        stringsMock = mockStatic(StarsectorStrings.class);
        stringsMock
            .when(() -> StarsectorStrings.get(anyString(), anyString()))
            .thenReturn("Jump-point");
    }

    @AfterEach
    void tearDown() {

        stringsMock.close();
    }

    @Nested
    class GenerateJumpPointName {

        @Test
        void leadsWithTheFocusNameWhenItHasOne() {

            var focusMock = mock(SectorEntityToken.class);

            when(focusMock.getName())
                .thenReturn("Corvus A");

            var name = EntityNameGenerator.generateJumpPointName(focusMock, 1500f);

            assertThat(name)
                .isEqualTo("Corvus A Jump-point 1.5e3");
        }

        @Test
        void fallsBackToTheLocationNameWhenTheFocusHasNoNameOfItsOwn() {

            var locationMock = mock(LocationAPI.class);
            var focusMock = mock(SectorEntityToken.class);

            when(locationMock.getName())
                .thenReturn("The Abyssal Depths");

            when(focusMock.getName())
                .thenReturn(null);
            when(focusMock.getContainingLocation())
                .thenReturn(locationMock);

            var name = EntityNameGenerator.generateJumpPointName(focusMock, 18f);

            assertThat(name)
                .isEqualTo("The Abyssal Depths Jump-point 1.8e1");
        }

        @Test
        void treatsVanillasUnknownLocationPlaceholderAsNoNameOnTheFocus() {

            var locationMock = mock(LocationAPI.class);
            var focusMock = mock(SectorEntityToken.class);

            when(locationMock.getName())
                .thenReturn("The Abyssal Depths");

            // Vanilla's BaseLocation returns this literal for an unnamed center.
            when(focusMock.getName())
                .thenReturn("unknown location");
            when(focusMock.getContainingLocation())
                .thenReturn(locationMock);

            var name = EntityNameGenerator.generateJumpPointName(focusMock, 18f);

            assertThat(name)
                .isEqualTo("The Abyssal Depths Jump-point 1.8e1");
        }

        @Test
        void fallsBackToTheFocusIdWhenNeitherFocusNorLocationIsNamed() {

            var locationMock = mock(LocationAPI.class);
            var focusMock = mock(SectorEntityToken.class);

            when(locationMock.getName())
                .thenReturn("unknown location");

            when(focusMock.getName())
                .thenReturn(null);
            when(focusMock.getContainingLocation())
                .thenReturn(locationMock);
            when(focusMock.getId())
                .thenReturn("barycenter");

            var name = EntityNameGenerator.generateJumpPointName(focusMock, 1500f);

            assertThat(name)
                .isEqualTo("barycenter Jump-point 1.5e3");
        }

        @Test
        void fallsBackToTheFocusIdWhenTheFocusStandsInNoLocation() {
            // An entity the game holds outside any location has no second name to fall back on,
            // so the ID is reached without the location step being asked for a name it has not
            // got - the reading has to survive a focus that is not anywhere.
            var focusMock = mock(SectorEntityToken.class);

            when(focusMock.getName())
                .thenReturn(null);
            when(focusMock.getContainingLocation())
                .thenReturn(null);
            when(focusMock.getId())
                .thenReturn("adrift");

            var name = EntityNameGenerator.generateJumpPointName(focusMock, 1500f);

            assertThat(name)
                .isEqualTo("adrift Jump-point 1.5e3");
        }
    }
}
