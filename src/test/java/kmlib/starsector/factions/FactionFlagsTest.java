package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;

import org.json.JSONObject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins {@link FactionFlags#isTerritorial}: it finds the flag where vanilla stores it, nested
 * inside the punitive-expedition custom data, and reads every way that data can be missing as
 * "not territorial" - the answer that keeps a faction out of a claim it cannot make.
 */
final class FactionFlagsTest {

    @Nested
    class IsTerritorial {
        @Test
        void reportsTerritorialWhenTheNestedFlagIsSet() {
            assertThat(FactionFlags.isTerritorial(buildFaction(true))).isTrue();
        }

        @Test
        void reportsNotTerritorialWhenTheNestedFlagIsUnset() {
            assertThat(FactionFlags.isTerritorial(buildFaction(false))).isFalse();
        }

        @Test
        void reportsNotTerritorialWhenThePunitiveExpeditionDataIsAbsent() {
            var factionMock = mock(FactionAPI.class);
            when(factionMock.getCustom()).thenReturn(new JSONObject());

            assertThat(FactionFlags.isTerritorial(factionMock)).isFalse();
        }

        @Test
        void reportsNotTerritorialWhenTheFactionCarriesNoCustomData() {
            var factionMock = mock(FactionAPI.class);
            when(factionMock.getCustom()).thenReturn(null);

            assertThat(FactionFlags.isTerritorial(factionMock)).isFalse();
        }

        @Test
        void reportsNotTerritorialForANullFaction() {
            assertThat(FactionFlags.isTerritorial(null)).isFalse();
        }
    }

    private static FactionAPI buildFaction(boolean isTerritorial) {
        var factionMock = mock(FactionAPI.class);
        when(factionMock.getCustom())
            .thenReturn(FactionCustomFixture.buildPunitiveExpeditionCustom(isTerritorial));
        return factionMock;
    }
}
