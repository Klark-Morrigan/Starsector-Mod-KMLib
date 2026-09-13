package kmlib.mods.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.RelationshipAPI;

import kmlib.mods.console.commands.ListFactionsCommand.FactionListingFilter;
import kmlib.starsector.factions.FactionCustomFixture;
import kmlib.testfixtures.mods.console.commands.output.CommandOutputFake;
import kmlib.testfixtures.starsector.StubbedGlobalLogger;
import kmlib.testfixtures.starsector.markets.colonies.ColonyMarketFixture;
import kmlib.testfixtures.starsector.markets.colonies.ColonyPlacementFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins {@link ListFactionsCommand}: {@code buildReport} names every faction with
 * the places it holds, counts a body carrying several markets once, reports
 * concealment and discovery as separate overlapping columns, names hyperspace
 * where a colony sits outside every system, keeps the counts whole under a filter
 * while narrowing the systems clause, and honours each of the four filter
 * keywords; and {@code runCommand} prints the report, accepts one keyword, and
 * rejects a keyword pair and an unknown word as bad syntax.
 *
 * <p>Colonies come from {@link ColonyMarketFixture}, so a colony posed here is the
 * same shape the sector's own colony read is posed against - what counts as one
 * belongs to that read's suites, not this one. The factions are this suite's own:
 * the listing reads them for their name, territoriality and player relationship,
 * none of which a colony fixture has business carrying.
 */
final class ListFactionsCommandTest {

    // A colony bigger than the fixture's default, so a colliding pair reads as
    // "the larger of the two" rather than as two loose numbers.
    private static final int LARGER_COLONY_SIZE = 6;
    private static final int NEUTRAL_REPUTATION = 0;

    @Nested
    class BuildReport {

        @Test
        void namesEveryFactionWithThePlacesItHolds() {

            var sector = new SectorFixture();

            sector.addFaction(buildFaction("hegemony", "Hegemony"));
            sector.addFaction(buildFaction("derelict", "Derelict"));
            sector.addSystemHolding(
                "corvus",
                ColonyMarketFixture.buildVisibleColony("hegemony"),
                ColonyMarketFixture.buildVisibleColony("hegemony"));

            var report = readReport(sector, FactionListingFilter.ALL);

            assertThat(report)
                .contains("hegemony  -  Hegemony  Neutral (0 / 100)"
                    + "\n    holdings: 2 (0 hidden, 0 discoverable)  systems: corvus");
        }

        @Test
        void printsNoVisibilityCountsOrSystemsForAFactionHoldingNothing() {
            // A row of noughts and an empty systems clause read as a finding rather
            // than as the absence they are, and "holdings: 0" already says it.
            var sector = new SectorFixture();

            sector.addFaction(buildFaction("derelict", "Derelict"));

            assertThat(readReport(sector, FactionListingFilter.ALL))
                .contains("derelict  -  Derelict  Neutral (0 / 100)\n    holdings: 0")
                .doesNotContain("hidden")
                .doesNotContain("systems:");
        }

        @Test
        void countsTwoMarketsOnOneBodyAsOneHolding() {
            // A mod supersedes a colony by adding its own market beside vanilla's on
            // the same body. Counted per market, that place is banked twice and its
            // owner reads as holding twice what the player can see.
            var sector = new SectorFixture();
            var vanillaMarket = ColonyMarketFixture.buildVisibleColony("independent");
            var moddedMarket =
                ColonyMarketFixture.buildSiblingMarketOn(vanillaMarket, LARGER_COLONY_SIZE);

            sector.addFaction(buildFaction("independent", "Independents"));
            sector.addSystemHolding("galatia", vanillaMarket, moddedMarket);

            assertThat(readReport(sector, FactionListingFilter.ALL))
                .contains("holdings: 1 (0 hidden, 0 discoverable)  systems: galatia");
        }

        @Test
        void countsAFoundConcealedPlaceAsHiddenOnly() {
            // The hidden flag never clears, so a raided pirate base stays hidden while
            // being perfectly well known - it must not also count as undiscovered.
            var sector = new SectorFixture();

            sector.addFaction(buildFaction("pirates", "Pirates"));
            sector.addSystemHolding(
                "kumari_kandam",
                ColonyMarketFixture.buildFoundConcealedColony("pirates"));

            assertThat(readReport(sector, FactionListingFilter.ALL))
                .contains("holdings: 1 (1 hidden, 0 discoverable)");
        }

        @Test
        void countsAnUndiscoveredConcealedPlaceInBothColumns() {
            // Neither column is a subset of the other: an undiscovered base is concealed AND
            // still to be found, which is why the two are reported side by side.
            var sector = new SectorFixture();

            sector.addFaction(buildFaction("pirates", "Pirates"));
            sector.addSystemHolding(
                "kumari_kandam",
                ColonyMarketFixture.buildUndiscoveredConcealedColony("pirates"));

            assertThat(readReport(sector, FactionListingFilter.ALL))
                .contains("holdings: 1 (1 hidden, 1 discoverable)");
        }

        @Test
        void namesHyperspaceAfterTheStarSystems() {
            // Vanilla puts no colony out there, but mods do, and a sector-wide count
            // that quietly dropped them would be wrong rather than merely incomplete.
            var sector = new SectorFixture();

            sector.addFaction(buildFaction("independent", "Independents"));
            sector.addSystemHolding(
                "corvus",
                ColonyMarketFixture.buildVisibleColony("independent"));
            sector.setHyperspaceHolding(
                ColonyMarketFixture.buildVisibleColony("independent"));

            assertThat(readReport(sector, FactionListingFilter.ALL))
                .contains("holdings: 2 (0 hidden, 0 discoverable)  systems: corvus, (hyperspace)");
        }

        @Test
        void marksATerritorialFaction() {

            var sector = new SectorFixture();

            sector.addFaction(buildTerritorialFaction("hegemony", "Hegemony"));

            assertThat(readReport(sector, FactionListingFilter.ALL))
                .contains("hegemony  -  Hegemony  [territorial]  Neutral (0 / 100)");
        }

        @Test
        void reportsThePlayerSOwnFactionAsSelf() {
            // The engine answers for a relationship with oneself; printed, it reads as
            // a finding about the player's standing with the player.
            var sector = new SectorFixture();
            var playerFactionMock = buildFaction("player", "Sindrian Diktat");

            sector.addFaction(playerFactionMock);
            sector.setPlayerFaction(playerFactionMock);

            assertThat(readReport(sector, FactionListingFilter.ALL))
                .contains("player  -  Sindrian Diktat  (self)");
        }

        @Test
        void namesAFactionCarryingAPlaceholderDisplayNameByItsId() {
            // A stock Nexerelin player.faction reports the literal "player" as its
            // display name, and vanilla reports "Independent" before the first colony.
            // Repeating the id says no more than is known, which is the point.
            var sector = new SectorFixture();

            sector.addFaction(buildFaction("player", "player"));

            assertThat(readReport(sector, FactionListingFilter.ALL))
                .contains("player  -  player");
        }

        @Test
        void ordersTheFactionsById() {
            // The sector's own order is load order, which differs between installs and
            // makes two runs hard to compare.
            var sector = new SectorFixture();

            sector.addFaction(buildFaction("tritachyon", "Tri-Tachyon"));
            sector.addFaction(buildFaction("hegemony", "Hegemony"));
            sector.addFaction(buildFaction("derelict", "Derelict"));

            var report = readReport(sector, FactionListingFilter.ALL);

            assertThat(report.indexOf("derelict"))
                .isLessThan(report.indexOf("hegemony"));
            assertThat(report.indexOf("hegemony"))
                .isLessThan(report.indexOf("tritachyon"));
        }

        @Test
        void listsOnlyFactionsHoldingSomethingUnderTheMarketsKeyword() {

            var sector = new SectorFixture();

            sector.addFaction(buildFaction("hegemony", "Hegemony"));
            sector.addFaction(buildFaction("derelict", "Derelict"));
            sector.addSystemHolding(
                "corvus",
                ColonyMarketFixture.buildVisibleColony("hegemony"));

            assertThat(readReport(sector, FactionListingFilter.HOLDS_ANYTHING))
                .contains("hegemony")
                .doesNotContain("derelict");
        }

        @Test
        void listsOnlyFactionsHoldingNothingUnderTheNoMarketsKeyword() {

            var sector = new SectorFixture();

            sector.addFaction(buildFaction("hegemony", "Hegemony"));
            sector.addFaction(buildFaction("derelict", "Derelict"));
            sector.addSystemHolding(
                "corvus",
                ColonyMarketFixture.buildVisibleColony("hegemony"));

            assertThat(readReport(sector, FactionListingFilter.HOLDS_NOTHING))
                .contains("derelict")
                .doesNotContain("hegemony");
        }

        @Test
        void listsOnlyFactionsHoldingAConcealedPlaceUnderTheHiddenKeyword() {

            var sector = new SectorFixture();

            sector.addFaction(buildFaction("pirates", "Pirates"));
            sector.addFaction(buildFaction("hegemony", "Hegemony"));
            sector.addSystemHolding(
                "kumari_kandam",
                ColonyMarketFixture.buildFoundConcealedColony("pirates"));
            sector.addSystemHolding(
                "corvus",
                ColonyMarketFixture.buildVisibleColony("hegemony"));

            assertThat(readReport(sector, FactionListingFilter.HOLDS_HIDDEN))
                .contains("pirates")
                .doesNotContain("hegemony");
        }

        @Test
        void listsOnlyFactionsHoldingAnUndiscoveredPlaceUnderTheDiscoverableKeyword() {
            // A found concealed base is hidden but no longer findable, so it must not
            // satisfy this keyword - which is what keeps the two columns distinct.
            var sector = new SectorFixture();

            sector.addFaction(buildFaction("pathers", "Luddic Path"));
            sector.addFaction(buildFaction("pirates", "Pirates"));
            sector.addSystemHolding(
                "hybrasil",
                ColonyMarketFixture.buildUndiscoveredConcealedColony("pathers"));
            sector.addSystemHolding(
                "kumari_kandam",
                ColonyMarketFixture.buildFoundConcealedColony("pirates"));

            assertThat(readReport(sector, FactionListingFilter.HOLDS_DISCOVERABLE))
                .contains("pathers")
                .doesNotContain("pirates");
        }

        @Test
        void keepsTheCountsWholeAndNarrowsTheSystemsClauseUnderAKeyword() {
            // The keyword decides who is listed, not what is reported about them - but
            // the clause does narrow, since finding WHERE the matches are is what a
            // filtered run is for.
            var sector = new SectorFixture();

            sector.addFaction(buildFaction("pirates", "Pirates"));
            sector.addSystemHolding(
                "corvus",
                ColonyMarketFixture.buildVisibleColony("pirates"));
            sector.addSystemHolding(
                "kumari_kandam",
                ColonyMarketFixture.buildFoundConcealedColony("pirates"));

            assertThat(readReport(sector, FactionListingFilter.HOLDS_HIDDEN))
                .contains("holdings: 2 (1 hidden, 0 discoverable)  systems: kumari_kandam");
        }

        @Test
        void namesTheModAFactionWasDeclaredBy() {
            // The id alone says nothing about where a faction came from, which on a heavily
            // modded install is most of what a reader opens this listing to find out.
            var sector = new SectorFixture();

            sector.addFaction(buildFaction("tahlan_greathouses", "Great Houses"));

            assertThat(readReport(
                    sector,
                    FactionListingFilter.ALL,
                    Map.of("tahlan_greathouses", "Tahlan Shipworks")))
                .contains("tahlan_greathouses  -  Great Houses  Neutral (0 / 100)"
                    + "  from: Tahlan Shipworks");
        }

        @Test
        void marksAFactionNoDeclarationNamesAsUnattributed() {
            // A faction a mod built at runtime is in no spreadsheet, and saying so beats a blank
            // where every neighbouring line carries a name.
            var sector = new SectorFixture();

            sector.addFaction(buildFaction("hegemony", "Hegemony"));
            sector.addFaction(buildFaction("runtime_faction", "Someone's Own"));

            var report = readReport(
                sector,
                FactionListingFilter.ALL,
                Map.of("hegemony", "vanilla"));

            assertThat(report)
                .contains("hegemony  -  Hegemony  Neutral (0 / 100)  from: vanilla")
                .contains("runtime_faction  -  Someone's Own  Neutral (0 / 100)"
                    + "  from: (unattributed)");
        }

        @Test
        void dropsTheSourceClauseWhenNothingWasDeclared() {
            // Nothing read is the read having failed or the game not being up, neither of which
            // is a fact about any faction - marking them all unattributed would report it as one.
            var sector = new SectorFixture();

            sector.addFaction(buildFaction("hegemony", "Hegemony"));

            assertThat(readReport(sector, FactionListingFilter.ALL, Map.of()))
                .doesNotContain("from:");
        }

        @Test
        void namesTheKeywordInTheHeader() {

            var sector = new SectorFixture();

            assertThat(readReport(sector, FactionListingFilter.HOLDS_HIDDEN))
                .contains("Factions and their holdings (hidden):");
        }

        @Test
        void reportsNoneWhenNoFactionMatches() {

            var sector = new SectorFixture();

            sector.addFaction(buildFaction("derelict", "Derelict"));

            assertThat(readReport(sector, FactionListingFilter.HOLDS_ANYTHING))
                .contains("(none)");
        }
    }

    @Nested
    class RunCommand {

        private MockedStatic<Global> globalMock;
        private CommandOutputFake outputFake;
        private ListFactionsCommand command;

        @BeforeEach
        void setUp() {

            // The fixture finishes its own stubbing before the static mock's opens,
            // so the two do not nest into an unfinished-stubbing error.
            var sector = new SectorFixture().getSector();

            globalMock = mockStatic(Global.class);
            globalMock
                .when(Global::getSector)
                .thenReturn(sector);

            // Owed because the run reaches the source read, whose logger is resolved once for the
            // JVM - left as the stand-in's null, every later suite logging through that class
            // faults on a line it never wrote.
            StubbedGlobalLogger.answerLoggersOn(globalMock);

            outputFake = new CommandOutputFake();
            command = new ListFactionsCommand(outputFake);
        }

        @AfterEach
        void tearDown() {
            globalMock.close();
        }

        @Test
        void printsTheReportForABareInvocation() {

            var result = command.runCommand("", CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.SUCCESS);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Factions and their holdings:"));
        }

        // Every keyword spelled out, and each asserted to reach the filter that names
        // itself back in the header. That pairing is the one thing the spec's
        // flag-to-filter walk could get wrong without any single-keyword case
        // noticing: a flag bound to the wrong filter still parses and still prints.
        @ParameterizedTest
        @ValueSource(strings = {"markets", "hidden", "discoverable", "no_markets"})
        void acceptsEachFilterKeywordAndSelectsTheFilterItNames(String keyword) {

            var result = command.runCommand(keyword, CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.SUCCESS);
            assertThat(outputFake.getMessages())
                .anyMatch(message ->
                    message.contains("Factions and their holdings (" + keyword + "):"));
        }

        @Test
        void reportsTwoFilterKeywordsAsBadSyntax() {
            // The keywords are alternatives: a pair has no single honest answer for the
            // systems clause, and no_markets contradicts the other three outright. The
            // usage line is asserted as a literal because it is built from the filters
            // rather than written out, so nothing else pins what the player is offered.
            var result = command.runCommand("hidden discoverable", CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Give at most one filter. "
                    + "Usage: kmlib_list_factions [markets|hidden|discoverable|no_markets]."));
        }

        @Test
        void reportsAnUnknownWordAsBadSyntax() {

            var result = command.runCommand("bogus", CommandContext.CAMPAIGN_MAP);

            assertThat(result)
                .isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Too many arguments"));
        }

        @Test
        void returnsTheValidationResultOutsideACampaign() {

            var result = command.runCommand("", CommandContext.COMBAT_MISSION);

            assertThat(result)
                .isEqualTo(CommandResult.WRONG_CONTEXT);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("can only run in a campaign"));
        }
    }

    // The unattributed run, which is what every case not about the source clause poses: an empty
    // map drops the clause, so those cases assert on the line the listing had before it existed.
    private static String readReport(SectorFixture sector, FactionListingFilter filter) {
        return readReport(sector, filter, Map.of());
    }

    private static String readReport(
            SectorFixture sector,
            FactionListingFilter filter,
            Map<String, String> sourceNamesByFactionId) {

        return ListFactionsCommand.buildReport(
            sector.getSector(),
            filter,
            sourceNamesByFactionId);
    }

    /** A faction the listing can name: not territorial, and neutral to the player. */
    private static FactionAPI buildFaction(String id, String displayName) {
        return buildFaction(id, displayName, RepLevel.NEUTRAL, NEUTRAL_REPUTATION, false);
    }

    /** A faction that treats the space around its holdings as its own. */
    private static FactionAPI buildTerritorialFaction(String id, String displayName) {
        return buildFaction(id, displayName, RepLevel.NEUTRAL, NEUTRAL_REPUTATION, true);
    }

    // A faction wired the way the listing reads one. The relationship is stubbed
    // through the live-relationship arm on purpose: the fallback arm ends at
    // Misc's colour palette, which reads from settings the test JVM never loads.
    private static FactionAPI buildFaction(
            String id,
            String displayName,
            RepLevel level,
            int reputation,
            boolean isTerritorial) {

        // The relationship finishes its own stubbing before the faction's opens, so
        // the two do not nest into an unfinished-stubbing error.
        var relationshipMock = mock(RelationshipAPI.class);

        when(relationshipMock.getLevel())
            .thenReturn(level);
        when(relationshipMock.getRepInt())
            .thenReturn(reputation);

        var custom = isTerritorial
            ? FactionCustomFixture.buildPunitiveExpeditionCustom(true)
            : null;
        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(id);
        when(factionMock.getDisplayName())
            .thenReturn(displayName);
        when(factionMock.getRelToPlayer())
            .thenReturn(relationshipMock);
        when(factionMock.getCustom())
            .thenReturn(custom);

        return factionMock;
    }

    /**
     * A sector holding as many factions and places as a case needs: factions added
     * in any order (the listing sorts them), star systems each under their own id,
     * and at most one hyperspace.
     *
     * <p>Places are wired the way the game wires one - the economy lists the
     * colonies, and the location carries the entities they sit on - so both halves
     * of the colony read find them. Each colony is also told which system it is in,
     * since that is what the systems clause is built from; a colony sited in
     * hyperspace is told nothing, which is exactly how the game answers for one.
     */
    private static final class SectorFixture {

        private final EconomyAPI economyMock = mock(EconomyAPI.class);
        private final SectorAPI sectorMock = mock(SectorAPI.class);

        // Handed to the sector mock once and added to afterwards. Mockito answers the
        // same list instance every call, so a faction or system added later is still
        // listed - which is what lets a case read as "open a sector, then fill it".
        private final List<FactionAPI> factions = new ArrayList<>();
        private final List<StarSystemAPI> systems = new ArrayList<>();

        private SectorFixture() {

            when(sectorMock.getAllFactions())
                .thenReturn(factions);
            when(sectorMock.getEconomy())
                .thenReturn(economyMock);
            when(sectorMock.getStarSystems())
                .thenReturn(systems);
        }

        private SectorAPI getSector() {
            return sectorMock;
        }

        private void addFaction(FactionAPI faction) {
            factions.add(faction);
        }

        private void addSystemHolding(String systemId, MarketAPI... locationColonies) {

            var systemMock = mock(StarSystemAPI.class);

            when(systemMock.getId())
                .thenReturn(systemId);

            // Each colony names the system it sits in, as a market does once its
            // entity is in one; the sector read itself never asks, so this is the
            // listing's own input rather than the read's.
            for (var colony : locationColonies) {
                when(colony.getStarSystem())
                    .thenReturn(systemMock);
            }
            placeColoniesIn(systemMock, locationColonies);
            systems.add(systemMock);
        }

        private void setHyperspaceHolding(MarketAPI... locationColonies) {

            var hyperspaceMock = mock(LocationAPI.class);

            placeColoniesIn(hyperspaceMock, locationColonies);

            when(sectorMock.getHyperspace())
                .thenReturn(hyperspaceMock);
        }

        private void setPlayerFaction(FactionAPI faction) {

            when(sectorMock.getPlayerFaction())
                .thenReturn(faction);
        }

        // Sites the colonies in one location: the economy lists them, and the
        // location carries the entity each sits on. Both halves, since every case
        // here poses ordinary registered colonies - the listed-versus-unlisted split
        // is the colony read's own suites' business.
        private void placeColoniesIn(LocationAPI location, MarketAPI[] locationColonies) {

            ColonyPlacementFixture.placeColonies(location, locationColonies);
            ColonyPlacementFixture.listColonies(economyMock, location, locationColonies);
        }
    }
}
