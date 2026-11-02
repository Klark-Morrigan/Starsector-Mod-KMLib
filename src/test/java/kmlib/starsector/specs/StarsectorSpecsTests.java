package kmlib.starsector.specs;

import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins each lookup's two unknown-ID answers - the game's throw and its {@code null} - to an empty result, and a known
 * ID to its spec.
 */
final class StarsectorSpecsTests {

    private static final String UNKNOWN_ID = "from_removed_mod";

    private SettingsAPI settingsMock;

    @BeforeEach
    void setUp() {

        settingsMock = mock(SettingsAPI.class);
    }

    @Nested
    final class FindFighterWingSpec {

        // The game answers an unknown wing ID with null rather than a throw.
        @Test
        void returnsNothingWhenTheLookupAnswersNull() {

            // ACT
            var actualSpec = StarsectorSpecs.findFighterWingSpec(settingsMock, UNKNOWN_ID);

            // ASSERT
            assertThat(actualSpec)
                .isEmpty();
        }

        @Test
        void returnsTheSpecOfAKnownId() {

            // ARRANGE
            var expectedSpecMock = mock(FighterWingSpecAPI.class);

            when(settingsMock.getFighterWingSpec("talon_wing"))
                .thenReturn(expectedSpecMock);

            // ACT
            var actualSpec = StarsectorSpecs.findFighterWingSpec(settingsMock, "talon_wing");

            // ASSERT
            assertThat(actualSpec)
                .containsSame(expectedSpecMock);
        }
    }

    @Nested
    final class FindHullSpec {

        // The game throws "Ship hull spec [id] not found!" for an unknown hull ID.
        @Test
        void returnsNothingWhenTheLookupThrows() {

            // ARRANGE
            when(settingsMock.getHullSpec(UNKNOWN_ID))
                .thenThrow(new RuntimeException("Ship hull spec [from_removed_mod] not found!"));

            // ACT
            var actualSpec = StarsectorSpecs.findHullSpec(settingsMock, UNKNOWN_ID);

            // ASSERT
            assertThat(actualSpec)
                .isEmpty();
        }

        @Test
        void returnsTheSpecOfAKnownId() {

            // ARRANGE
            var expectedSpecMock = mock(ShipHullSpecAPI.class);

            when(settingsMock.getHullSpec("wolf"))
                .thenReturn(expectedSpecMock);

            // ACT
            var actualSpec = StarsectorSpecs.findHullSpec(settingsMock, "wolf");

            // ASSERT
            assertThat(actualSpec)
                .containsSame(expectedSpecMock);
        }
    }

    @Nested
    final class FindWeaponSpec {

        // The game throws "Weapon spec [id] not found!" for an unknown weapon ID.
        @Test
        void returnsNothingWhenTheLookupThrows() {

            // ARRANGE
            when(settingsMock.getWeaponSpec(UNKNOWN_ID))
                .thenThrow(new RuntimeException("Weapon spec [from_removed_mod] not found!"));

            // ACT
            var actualSpec = StarsectorSpecs.findWeaponSpec(settingsMock, UNKNOWN_ID);

            // ASSERT
            assertThat(actualSpec)
                .isEmpty();
        }

        @Test
        void returnsTheSpecOfAKnownId() {

            // ARRANGE
            var expectedSpecMock = mock(WeaponSpecAPI.class);

            when(settingsMock.getWeaponSpec("lightmortar"))
                .thenReturn(expectedSpecMock);

            // ACT
            var actualSpec = StarsectorSpecs.findWeaponSpec(settingsMock, "lightmortar");

            // ASSERT
            assertThat(actualSpec)
                .containsSame(expectedSpecMock);
        }
    }
}
