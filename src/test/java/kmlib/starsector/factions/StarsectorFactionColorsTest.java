package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

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
 */
class StarsectorFactionColorsTest {
    private static final Color NEUTRAL_BASE = new Color(150, 150, 150);

    @Nested
    class ResolveNeutralColor {

        @Test
        void resolveNeutralColorReturnsNeutralFactionBaseColor() {
            var neutralMock = mock(FactionAPI.class);
            when(neutralMock.getBaseUIColor()).thenReturn(NEUTRAL_BASE);
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getFaction("neutral")).thenReturn(neutralMock);

            assertThat(StarsectorFactionColors.resolveNeutralColor(sectorMock))
                    .isEqualTo(NEUTRAL_BASE);
        }

        @Test
        void resolveNeutralColorFallsBackToGrayForNullSector() {
            assertThat(StarsectorFactionColors.resolveNeutralColor(null))
                    .isEqualTo(Color.GRAY);
        }

        @Test
        void resolveNeutralColorFallsBackToGrayWhenNeutralFactionAbsent() {
            // A sector with no "neutral" faction (a bare double, or a stripped
            // modded launcher) must not NPE - the grey fallback stands in.
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getFaction("neutral")).thenReturn(null);

            assertThat(StarsectorFactionColors.resolveNeutralColor(sectorMock))
                    .isEqualTo(Color.GRAY);
        }
    }
}
