package kmlib.console;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

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
    @Test
    void omitsOrdinaryFullyVisibleSystems() {
        SectorAPI sector = sectorWith(system("Corvus", false,
                ownedMarket("Jangala", "Hegemony", Visibility.SHOWN)));

        String report = ListMapSpoilersCommand.buildReport(sector);

        assertThat(report).doesNotContain("Corvus");
        assertThat(report).contains("(none)");
    }

    @Test
    void listsCutOffSystemAndFlagsIt() {
        SectorAPI sector = sectorWith(system("Black Site", true,
                ownedMarket("Station", "Tri-Tachyon", Visibility.SHOWN)));

        String report = ListMapSpoilersCommand.buildReport(sector);

        assertThat(report).contains("Black Site  [cut off]");
        assertThat(report).contains("Station  (Tri-Tachyon)");
    }

    @Test
    void listsSystemWithHiddenMarketAndFlagsTheMarket() {
        SectorAPI sector = sectorWith(system("Hideout", false,
                ownedMarket("Pirate Base", "Pirates", Visibility.HIDDEN)));

        String report = ListMapSpoilersCommand.buildReport(sector);

        assertThat(report).contains("Hideout");
        assertThat(report).contains("Pirate Base  (Pirates)  [hidden]");
    }

    @Test
    void listsSystemWithUndiscoveredMarketAndFlagsTheMarket() {
        SectorAPI sector = sectorWith(system("Libra System", false,
                ownedMarket("Battlestar Libra", "Knights", Visibility.UNDISCOVERED)));

        String report = ListMapSpoilersCommand.buildReport(sector);

        assertThat(report).contains("Battlestar Libra  (Knights)  [undiscovered]");
    }

    @Test
    void excludesNeutralAndConditionOnlyMarketsFromTheOwnedList() {
        // The system lists (a real owned market plus being cut off), but the
        // neutral and condition-only markets are not counted as owned, so they
        // do not appear.
        MarketAPI real = ownedMarket("Colony", "Hegemony", Visibility.SHOWN);
        MarketAPI neutral = ownedMarket("Rock", "neutral", Visibility.SHOWN);
        MarketAPI conditionOnly = ownedMarket("Gas Giant", "Hegemony", Visibility.SHOWN);
        when(conditionOnly.isPlanetConditionMarketOnly()).thenReturn(true);
        SectorAPI sector = sectorWith(system("Bare", true, real, neutral, conditionOnly));

        String report = ListMapSpoilersCommand.buildReport(sector);

        assertThat(report).contains("Bare  [cut off]");
        assertThat(report).contains("Colony  (Hegemony)");
        assertThat(report).doesNotContain("Rock");
        assertThat(report).doesNotContain("Gas Giant");
    }

    private enum Visibility { SHOWN, HIDDEN, UNDISCOVERED }

    private static SectorAPI sectorWith(SystemWithMarkets... systems) {
        EconomyAPI economy = mock(EconomyAPI.class);
        List<StarSystemAPI> starSystems = new ArrayList<>();
        for (SystemWithMarkets entry : systems) {
            starSystems.add(entry.system);
            when(economy.getMarkets(entry.system)).thenReturn(entry.markets);
        }
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getStarSystems()).thenReturn(starSystems);
        when(sector.getEconomy()).thenReturn(economy);
        return sector;
    }

    private static SystemWithMarkets system(String name, boolean isCutOff, MarketAPI... markets) {
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(system.getName()).thenReturn(name);
        when(system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)).thenReturn(isCutOff);
        return new SystemWithMarkets(system, List.of(markets));
    }

    private static MarketAPI ownedMarket(String name, String factionId, Visibility visibility) {
        FactionAPI faction = mock(FactionAPI.class);
        when(faction.getId()).thenReturn(factionId);
        when(faction.getDisplayName()).thenReturn(factionId);

        SectorEntityToken entity = mock(SectorEntityToken.class);
        when(entity.isDiscoverable()).thenReturn(visibility == Visibility.UNDISCOVERED);

        MarketAPI market = mock(MarketAPI.class);
        when(market.getName()).thenReturn(name);
        when(market.getFaction()).thenReturn(faction);
        when(market.isHidden()).thenReturn(visibility == Visibility.HIDDEN);
        when(market.getPrimaryEntity()).thenReturn(entity);
        return market;
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
