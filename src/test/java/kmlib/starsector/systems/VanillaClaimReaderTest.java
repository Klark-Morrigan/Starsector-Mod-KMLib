package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;

import kmlib.starsector.testing.StarsectorSettingsFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract of {@link VanillaClaimReader#readClaimingFactionId}: it reduces vanilla's
 * {@link Misc#getClaimingFaction} to a faction id and treats an unreadable system as unclaimed.
 */
class VanillaClaimReaderTest {

    @Nested
    class ReadClaimingFactionId {
        @Test
        void reportsClaimantIdWhenVanillaResolvesAFaction() {
            var centerMock = mock(SectorEntityToken.class);
            var systemMock = mock(StarSystemAPI.class);
            when(systemMock.getCenter()).thenReturn(centerMock);
            var factionMock = mock(FactionAPI.class);
            when(factionMock.getId()).thenReturn("hegemony");

            // Misc.<clinit> reads Global.getSettings(), so the no-op proxy must be
            // installed before Mockito instruments the class or init NPEs.
            StarsectorSettingsFake.installSettings();
            try (var miscMock = Mockito.mockStatic(Misc.class)) {
                miscMock.when(() -> Misc.getClaimingFaction(centerMock)).thenReturn(factionMock);

                assertThat(new VanillaClaimReader().readClaimingFactionId(systemMock))
                        .isEqualTo("hegemony");
            } finally {
                StarsectorSettingsFake.clearSettings();
            }
        }

        @Test
        void reportsUnclaimedWhenVanillaResolvesNoFaction() {
            var centerMock = mock(SectorEntityToken.class);
            var systemMock = mock(StarSystemAPI.class);
            when(systemMock.getCenter()).thenReturn(centerMock);

            StarsectorSettingsFake.installSettings();
            try (var miscMock = Mockito.mockStatic(Misc.class)) {
                miscMock.when(() -> Misc.getClaimingFaction(centerMock)).thenReturn(null);

                assertThat(new VanillaClaimReader().readClaimingFactionId(systemMock)).isNull();
            } finally {
                StarsectorSettingsFake.clearSettings();
            }
        }

        @Test
        void reportsUnclaimedForNullSystem() {
            assertThat(new VanillaClaimReader().readClaimingFactionId(null)).isNull();
        }

        @Test
        void reportsUnclaimedWhenSystemHasNoCenter() {
            var systemMock = mock(StarSystemAPI.class);
            when(systemMock.getCenter()).thenReturn(null);

            assertThat(new VanillaClaimReader().readClaimingFactionId(systemMock)).isNull();
        }
    }
}
