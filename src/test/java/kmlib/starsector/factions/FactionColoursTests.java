package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;

import kmlib.colour.ColourPair;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the base-colour resolver: it forwards the faction's own base UI colour, and falls back to a
 * mid grey for an absent faction. Also pins the faction palette resolver, the same fallback shape
 * applied to a faction's bright/dark pair, and its fallback-free twin, which answers null where the
 * other answers grey.
 */
class FactionColoursTests {

    @Nested
    class ResolveBaseColour {

        @Test
        void returnsTheFactionsBaseColour() {

            // ARRANGE
            var expectedColour = new Color(150, 150, 150);

            var factionMock = mock(FactionAPI.class);

            when(factionMock.getBaseUIColor())
                .thenReturn(new Color(150, 150, 150));

            // ACT
            var actualColour = FactionColours.resolveBaseColour(factionMock);

            // ASSERT
            assertThat(actualColour)
                .isEqualTo(expectedColour);
        }

        // A faction gone from the sector, or a sector built without it, looks up as null.
        @Test
        void fallsBackToGreyForNullFaction() {

            // ARRANGE
            var expectedColour = Color.GRAY;

            // ACT
            var actualColour = FactionColours.resolveBaseColour(null);

            // ASSERT
            assertThat(actualColour)
                .isEqualTo(expectedColour);
        }
    }

    @Nested
    class ResolvePalette {

        @Test
        void returnsTheFactionsBrightAndDarkColours() {

            // ARRANGE
            var expectedPalette = new ColourPair(new Color(10, 20, 30), new Color(40, 50, 60));

            // ACT
            var actualPalette = FactionColours.resolvePalette(mockFactionPainted());

            // ASSERT
            assertThat(actualPalette)
                .isEqualTo(expectedPalette);
        }

        @Test
        void fallsBackToGreyPairForNullFaction() {

            // ARRANGE
            var expectedPalette = new ColourPair(Color.GRAY, Color.GRAY);

            // ACT
            var actualPalette = FactionColours.resolvePalette(null);

            // ASSERT
            assertThat(actualPalette)
                .isEqualTo(expectedPalette);
        }
    }

    @Nested
    class FindPalette {

        @Test
        void returnsTheFactionsBrightAndDarkColours() {

            // ARRANGE
            var expectedPalette = new ColourPair(new Color(10, 20, 30), new Color(40, 50, 60));

            // ACT
            var actualPalette = FactionColours.findPalette(mockFactionPainted());

            // ASSERT
            assertThat(actualPalette)
                .isEqualTo(expectedPalette);
        }

        @Test
        void returnsNullForNullFaction() {

            // ACT
            var actualPalette = FactionColours.findPalette(null);

            // ASSERT
            assertThat(actualPalette)
                .isNull();
        }
    }

    // A faction authored with a distinct bright and dark shade, so a swapped pair fails.
    private static FactionAPI mockFactionPainted() {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getBrightUIColor())
            .thenReturn(new Color(10, 20, 30));
        when(factionMock.getDarkUIColor())
            .thenReturn(new Color(40, 50, 60));

        return factionMock;
    }
}
