package kmlib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import kmlib.testfixtures.console.output.CommandOutputFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins {@link ListMapSpoilersCommand}: {@code buildReport} lists only
 * spoiler-worthy systems (cut off, or holding an undiscovered owned market),
 * surfaces a cut-off system even with no owned market, skips ordinary visible
 * systems (including a known econ-hidden market like the Galatia Academy and a
 * discovered pirate base) and neutral/condition-only markets, and flags each
 * reason;
 * and {@code runCommand} prints the report for a bare invocation, rejects a
 * surplus argument as bad syntax, and returns the validation result outside a
 * campaign. The report is read back through a recording {@code CommandOutput},
 * so no live console is needed.
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
        void listsUndiscoveredHiddenBaseAsUndiscovered() {
            // A concealed pirate base sets its entity discoverable, so before the
            // player finds it the entity-discoverability signal surfaces it.
            var sector = sectorWith(system("Hideout", false,
                    ownedMarket("Pirate Base", "Pirates", Visibility.UNDISCOVERED)));

            var report = ListMapSpoilersCommand.buildReport(sector);

            assertThat(report).contains("Pirate Base  (Pirates)  [undiscovered]");
        }

        @Test
        void omitsDiscoveredHiddenBase() {
            // The $core_hiddenBase flag never clears, so a found pirate base must
            // not linger in the list: once its entity is no longer discoverable
            // it drops out, exactly like any other discovered colony.
            var sector = sectorWith(system("Hideout", false,
                    ownedMarket("Pirate Base", "Pirates", Visibility.SHOWN)));

            var report = ListMapSpoilersCommand.buildReport(sector);

            assertThat(report).doesNotContain("Pirate Base");
            assertThat(report).contains("(none)");
        }

        @Test
        void listsSystemWithUndiscoveredMarketAndFlagsTheMarket() {
            var sector = sectorWith(system("Libra System", false,
                    ownedMarket("Battlestar Libra", "Knights", Visibility.UNDISCOVERED)));

            var report = ListMapSpoilersCommand.buildReport(sector);

            assertThat(report).contains("Battlestar Libra  (Knights)  [undiscovered]");
        }

        @Test
        void omitsKnownEconHiddenMarketSuchAsGalatiaAcademy() {
            // The Galatia Academy market carries isHidden()==true yet is fully
            // known and dockable: discovery, not the econ flag, decides what is
            // spoiler-worthy, so a known econ-hidden market must not be flagged
            // and its ordinary system is omitted.
            var academy = ownedMarket("Galatia Academy", "Independent", Visibility.SHOWN);
            when(academy.isHidden()).thenReturn(true);
            var sector = sectorWith(system("Galatia", false, academy));

            var report = ListMapSpoilersCommand.buildReport(sector);

            assertThat(report).doesNotContain("Galatia Academy");
            assertThat(report).contains("(none)");
        }

        @Test
        void omitsConcealedColonyOnceDiscovered() {
            // A formerly concealed colony (e.g. a Holdout Forgeship station)
            // clears its discoverable flag on discovery, so it must drop out of
            // the spoiler list rather than linger - the entity is no longer
            // discoverable and is not a hidden base.
            var sector = sectorWith(system("Mia's Star", false,
                    ownedMarket("Forgeship", "holdout", Visibility.SHOWN)));

            var report = ListMapSpoilersCommand.buildReport(sector);

            assertThat(report).doesNotContain("Forgeship");
            assertThat(report).contains("(none)");
        }

        @Test
        void listsCutOffSystemEvenWithNoOwnedMarkets() {
            // A void or story system holds no owned market, so requiring one
            // would hide it; being cut off from hyperspace is enough to surface
            // it on its own.
            var sector = sectorWith(system("Limbo", true));

            var report = ListMapSpoilersCommand.buildReport(sector);

            assertThat(report).contains("Limbo  [cut off]");
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

    @Nested
    class RunCommand {
        private MockedStatic<Global> globalMock;
        private CommandOutputFake outputFake;
        private ListMapSpoilersCommand command;

        @BeforeEach
        void setUp() {
            var sectorMock = mock(SectorAPI.class);
            when(sectorMock.getStarSystems()).thenReturn(new ArrayList<>());
            globalMock = mockStatic(Global.class);
            globalMock.when(Global::getSector).thenReturn(sectorMock);

            outputFake = new CommandOutputFake();
            command = new ListMapSpoilersCommand(outputFake);
        }

        @AfterEach
        void tearDown() {
            globalMock.close();
        }

        @Test
        void prints_the_report_for_a_bare_invocation() {
            var result = command.runCommand("", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.SUCCESS);
            assertThat(outputFake.getMessages())
                    .anyMatch(message -> message.contains("Map spoilers"));
        }

        @Test
        void reports_a_surplus_argument_as_bad_syntax() {
            var result = command.runCommand("bogus", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                    .anyMatch(message -> message.contains("Too many arguments"));
        }

        @Test
        void returns_the_validation_result_outside_a_campaign() {
            var result = command.runCommand("", CommandContext.COMBAT_MISSION);

            assertThat(result).isEqualTo(CommandResult.WRONG_CONTEXT);
            assertThat(outputFake.getMessages())
                    .anyMatch(message -> message.contains("can only run in a campaign"));
        }
    }

    private enum Visibility { SHOWN, UNDISCOVERED }

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

        // A market is a hidden base only when a test stubs Misc.isHiddenBase for
        // it; left unstubbed, the static mock returns false here.
        var marketMock = mock(MarketAPI.class);
        when(marketMock.getName()).thenReturn(name);
        when(marketMock.getFaction()).thenReturn(factionMock);
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
