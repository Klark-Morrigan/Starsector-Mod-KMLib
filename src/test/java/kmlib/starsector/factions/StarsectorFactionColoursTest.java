package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

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
 * Also pins the named-faction palette resolver, the same fallback shape applied
 * to a faction's bright/dark pair instead of the neutral faction's single shade.
 */
class StarsectorFactionColoursTest {
    private static final Color NEUTRAL_BASE = new Color(150, 150, 150);
    private static final String FACTION_ID = "independent";

    @Nested
    class ResolveNeutralColour {

        @Test
        void resolveNeutralColourReturnsNeutralFactionBaseColour() {
            var neutralMock = mock(FactionAPI.class);
            when(neutralMock.getBaseUIColor()).thenReturn(NEUTRAL_BASE);
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getFaction(Factions.NEUTRAL)).thenReturn(neutralMock);

            assertThat(StarsectorFactionColours.resolveNeutralColour(sectorMock))
                .isEqualTo(NEUTRAL_BASE);
        }

        @Test
        void resolveNeutralColourFallsBackToGrayForNullSector() {
            assertThat(StarsectorFactionColours.resolveNeutralColour(null))
                .isEqualTo(Color.GRAY);
        }

        @Test
        void resolveNeutralColourFallsBackToGrayWhenNeutralFactionAbsent() {
            // A sector with no "neutral" faction (a bare double, or a stripped
            // modded launcher) must not NPE - the grey fallback stands in.
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getFaction(Factions.NEUTRAL)).thenReturn(null);

            assertThat(StarsectorFactionColours.resolveNeutralColour(sectorMock))
                .isEqualTo(Color.GRAY);
        }
    }

    @Nested
    class ResolvePalette {

        @Test
        void resolvePaletteReturnsTheFactionsBrightAndDarkColours() {
            var factionMock = mock(FactionAPI.class);
            when(factionMock.getBrightUIColor()).thenReturn(new Color(10, 20, 30));
            when(factionMock.getDarkUIColor()).thenReturn(new Color(40, 50, 60));
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getFaction(FACTION_ID)).thenReturn(factionMock);

            assertThat(StarsectorFactionColours.resolvePalette(sectorMock, FACTION_ID))
                .isEqualTo(new FactionPalette(new Color(10, 20, 30), new Color(40, 50, 60)));
        }

        @Test
        void resolvePaletteFallsBackToGrayPairForNullSector() {
            assertThat(StarsectorFactionColours.resolvePalette(null, FACTION_ID))
                .isEqualTo(new FactionPalette(Color.GRAY, Color.GRAY));
        }

        @Test
        void resolvePaletteFallsBackToGrayPairWhenFactionAbsent() {
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getFaction(FACTION_ID)).thenReturn(null);

            assertThat(StarsectorFactionColours.resolvePalette(sectorMock, FACTION_ID))
                .isEqualTo(new FactionPalette(Color.GRAY, Color.GRAY));
        }
    }
}
