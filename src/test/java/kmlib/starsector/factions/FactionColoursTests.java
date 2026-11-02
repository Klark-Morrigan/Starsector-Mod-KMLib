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
 * applied to a faction's bright/dark pair, its fallback-free twin, which answers null where the other
 * answers grey, and the comparison that answers as the twin would.
 */
class FactionColoursTests {

    // The shades mockFactionPainted authors, bright then dark.
    private static final ColourPair PAINTED_PALETTE = new ColourPair(new Color(10, 20, 30), new Color(40, 50, 60));

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

    @Nested
    class HasPalette {

        // The pair the faction was read in earlier, still authored on it.
        @Test
        void isTrueForThePairTheFactionIsPaintedIn() {

            // ACT
            var actualHasPalette = FactionColours.hasPalette(mockFactionPainted(), PAINTED_PALETTE);

            // ASSERT
            assertThat(actualHasPalette)
                .isTrue();
        }

        // A recolour of either shade alone moves the pair.
        @Test
        void isFalseWhenOneShadeMoved() {

            // ARRANGE
            var movedPalette = new ColourPair(new Color(10, 20, 30), new Color(99, 50, 60));

            // ACT
            var actualHasPalette = FactionColours.hasPalette(mockFactionPainted(), movedPalette);

            // ASSERT
            assertThat(actualHasPalette)
                .isFalse();
        }

        // Bright is primary and dark secondary, so the same two shades the other way round are
        // another pair.
        @Test
        void isFalseForTheShadesSwapped() {

            // ARRANGE
            var swappedPalette = new ColourPair(new Color(40, 50, 60), new Color(10, 20, 30));

            // ACT
            var actualHasPalette = FactionColours.hasPalette(mockFactionPainted(), swappedPalette);

            // ASSERT
            assertThat(actualHasPalette)
                .isFalse();
        }

        // findPalette answers null for an absent faction, so null is the pair it still paints in.
        @Test
        void isTrueForNoPairAndNoFaction() {

            // ACT
            var actualHasPalette = FactionColours.hasPalette(null, null);

            // ASSERT
            assertThat(actualHasPalette)
                .isTrue();
        }

        // A faction gone since its pair was read no longer paints in it.
        @Test
        void isFalseForAPairAndNoFaction() {

            // ACT
            var actualHasPalette = FactionColours.hasPalette(null, PAINTED_PALETTE);

            // ASSERT
            assertThat(actualHasPalette)
                .isFalse();
        }

        // A faction that appeared since it was read as absent now paints in a pair.
        @Test
        void isFalseForNoPairAndAFaction() {

            // ACT
            var actualHasPalette = FactionColours.hasPalette(mockFactionPainted(), null);

            // ASSERT
            assertThat(actualHasPalette)
                .isFalse();
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
