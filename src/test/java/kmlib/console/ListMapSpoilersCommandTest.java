package kmlib.console;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins {@link ListMapSpoilersCommand#buildReport}: it lists only spoiler-worthy
 * systems (cut off, or holding a hidden/undiscovered owned market), skips
 * ordinary visible systems and neutral/condition-only markets, and flags each
 * reason.
 */
final class ListMapSpoilersCommandTest {

    @Nested
    class BuildReport {
        @Test
        void omitsOrdinaryFullyVisibleSystems() {
            var sector = sectorWith(system("Corvus", false,
                    ownedMarket("Jangala", "Hegemony", Visibility.SHOWN)));

            var report = ListMapSpoilersCommand.buildReport(sector);

            assertThat(report).doesNotContain("Corvus");
            assertThat(report).contains("(none)");
        }

        @Test
        void listsCutOffSystemAndFlagsIt() {
            var sector = sectorWith(system("Black Site", true,
                    ownedMarket("Station", "Tri-Tachyon", Visibility.SHOWN)));

            var report = ListMapSpoilersCommand.buildReport(sector);

            assertThat(report).contains("Black Site  [cut off]");
            assertThat(report).contains("Station  (Tri-Tachyon)");
        }

        @Test
        void listsSystemWithHiddenMarketAndFlagsTheMarket() {
            var sector = sectorWith(system("Hideout", false,
                    ownedMarket("Pirate Base", "Pirates", Visibility.HIDDEN)));

            var report = ListMapSpoilersCommand.buildReport(sector);

            assertThat(report).contains("Hideout");
            assertThat(report).contains("Pirate Base  (Pirates)  [hidden]");
        }

        @Test
        void listsSystemWithUndiscoveredMarketAndFlagsTheMarket() {
            var sector = sectorWith(system("Libra System", false,
                    ownedMarket("Battlestar Libra", "Knights", Visibility.UNDISCOVERED)));

            var report = ListMapSpoilersCommand.buildReport(sector);

            assertThat(report).contains("Battlestar Libra  (Knights)  [undiscovered]");
        }

        @Test
        void excludesNeutralAndConditionOnlyMarketsFromTheOwnedList() {
            // The system lists (a real owned market plus being cut off), but the
            // neutral and condition-only markets are not counted as owned, so they
            // do not appear.
            var real = ownedMarket("Colony", "Hegemony", Visibility.SHOWN);
            var neutral = ownedMarket("Rock", "neutral", Visibility.SHOWN);
            var conditionOnly = ownedMarket("Gas Giant", "Hegemony", Visibility.SHOWN);
            when(conditionOnly.isPlanetConditionMarketOnly()).thenReturn(true);
            var sector = sectorWith(system("Bare", true, real, neutral, conditionOnly));

            var report = ListMapSpoilersCommand.buildReport(sector);

            assertThat(report).contains("Bare  [cut off]");
            assertThat(report).contains("Colony  (Hegemony)");
            assertThat(report).doesNotContain("Rock");
            assertThat(report).doesNotContain("Gas Giant");
        }
    }

    private enum Visibility { SHOWN, HIDDEN, UNDISCOVERED }

    private static SectorAPI sectorWith(SystemWithMarkets... systems) {
        var economyMock = mock(EconomyAPI.class);
        var starSystems = new ArrayList<StarSystemAPI>();
        for (var entry : systems) {
            starSystems.add(entry.system);
            when(economyMock.getMarkets(entry.system)).thenReturn(entry.markets);
        }
        var sectorMock = mock(SectorAPI.class);
        when(sectorMock.getStarSystems()).thenReturn(starSystems);
        when(sectorMock.getEconomy()).thenReturn(economyMock);
        return sectorMock;
    }

    private static SystemWithMarkets system(String name, boolean isCutOff, MarketAPI... markets) {
        var systemMock = mock(StarSystemAPI.class);
        when(systemMock.getName()).thenReturn(name);
        when(systemMock.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)).thenReturn(isCutOff);
        return new SystemWithMarkets(systemMock, List.of(markets));
    }

    private static MarketAPI ownedMarket(String name, String factionId, Visibility visibility) {
        var factionMock = mock(FactionAPI.class);
        when(factionMock.getId()).thenReturn(factionId);
        when(factionMock.getDisplayName()).thenReturn(factionId);

        var entityMock = mock(SectorEntityToken.class);
        when(entityMock.isDiscoverable()).thenReturn(visibility == Visibility.UNDISCOVERED);

        var marketMock = mock(MarketAPI.class);
        when(marketMock.getName()).thenReturn(name);
        when(marketMock.getFaction()).thenReturn(factionMock);
        when(marketMock.isHidden()).thenReturn(visibility == Visibility.HIDDEN);
        when(marketMock.getPrimaryEntity()).thenReturn(entityMock);
        return marketMock;
    }

    // Pairs a stubbed system with the market list its economy returns, so the
    // sector wiring can register both without nested stubbing.
    private static final class SystemWithMarkets {
        private final StarSystemAPI system;
        private final List<MarketAPI> markets;

        private SystemWithMarkets(StarSystemAPI system, List<MarketAPI> markets) {
            this.system = system;
            this.markets = markets;
        }
    }
}
