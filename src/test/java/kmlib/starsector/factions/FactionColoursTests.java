package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import kmlib.colour.ColourPair;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the neutral-colour resolver: it forwards the neutral faction's own base
 * UI colour when present, and falls back to a mid grey whenever the sector or
 * its neutral faction is absent (the two null gaps the vanilla lifecycle opens).
 * Also pins the faction palette resolver, the same fallback shape applied
 * to a faction's bright/dark pair instead of the neutral faction's single shade,
 * and its fallback-free twin, which answers null where the other answers grey.
 */
class FactionColoursTests {

    private static final Color NEUTRAL_BASE = new Color(150, 150, 150);

    @Nested
    class ResolveNeutralColour {

        @Test
        void returnsNeutralFactionBaseColour() {

            var neutralMock = mock(FactionAPI.class);

            when(neutralMock.getBaseUIColor())
                .thenReturn(NEUTRAL_BASE);

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getFaction(Factions.NEUTRAL))
                .thenReturn(neutralMock);

            assertThat(FactionColours.resolveNeutralColour(sectorMock))
                .isEqualTo(NEUTRAL_BASE);
        }

        @Test
        void fallsBackToGreyForNullSector() {

            assertThat(FactionColours.resolveNeutralColour(null))
                .isEqualTo(Color.GRAY);
        }

        @Test
        void fallsBackToGreyWhenNeutralFactionAbsent() {
            // A sector with no "neutral" faction (a bare double, or a stripped
            // modded launcher) must not NPE - the grey fallback stands in.
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getFaction(Factions.NEUTRAL))
                .thenReturn(null);

            assertThat(FactionColours.resolveNeutralColour(sectorMock))
                .isEqualTo(Color.GRAY);
        }
    }

    @Nested
    class ResolvePalette {

        @Test
        void returnsTheFactionsBrightAndDarkColours() {

            assertThat(FactionColours.resolvePalette(mockFactionPainted()))
                .isEqualTo(new ColourPair(new Color(10, 20, 30), new Color(40, 50, 60)));
        }

        @Test
        void fallsBackToGreyPairForNullFaction() {

            assertThat(FactionColours.resolvePalette(null))
                .isEqualTo(new ColourPair(Color.GRAY, Color.GRAY));
        }
    }

    @Nested
    class FindPalette {

        @Test
        void returnsTheFactionsBrightAndDarkColours() {

            assertThat(FactionColours.findPalette(mockFactionPainted()))
                .isEqualTo(new ColourPair(new Color(10, 20, 30), new Color(40, 50, 60)));
        }

        @Test
        void returnsNullForNullFaction() {

            assertThat(FactionColours.findPalette(null))
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
