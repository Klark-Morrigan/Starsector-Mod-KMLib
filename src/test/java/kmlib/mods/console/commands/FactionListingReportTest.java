package kmlib.mods.console.commands;

import kmlib.starsector.settings.modmanager.ModSource;
import kmlib.testfixtures.starsector.markets.colonies.ColonyMarketFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static kmlib.mods.console.commands.FactionListingFixture.buildFaction;
import static kmlib.mods.console.commands.FactionListingFixture.buildNamedFaction;
import static kmlib.mods.console.commands.FactionListingFixture.buildTerritorialFaction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what the listing says: every faction with the places it holds, a body carrying several
 * markets counted once, concealment and discovery as separate overlapping columns, hyperspace named
 * where a colony sits outside every system, the counts kept whole under a filter while the systems
 * clause narrows, and each of the four filter keywords honoured.
 *
 * <p>Also what each option leaves out - the holdings line with its header clause, and the attitude
 * with the separator it sat behind - and how a faction is named: both names where the long one says
 * something more, the mod that declared it with its id where there is one, and the unattributed
 * mark where no row names it.
 *
 * <p>Colonies come from {@link ColonyMarketFixture}, so a colony posed here is the same shape the
 * sector's own colony read is posed against - what counts as one belongs to that read's suites, not
 * this one.
 */
final class FactionListingReportTest {

    // A colony bigger than the fixture's default, so a colliding pair reads as
    // "the larger of the two" rather than as two loose numbers.
    private static final int LARGER_COLONY_SIZE = 6;

    @Nested
    class DescribeFactions {

        @Test
        void namesEveryFactionWithThePlacesItHolds() {

            var sector = new FactionListingFixture();

            sector.addFaction(buildFaction("hegemony", "Hegemony"));
            sector.addFaction(buildFaction("derelict", "Derelict"));
            sector.addSystemHolding(
                "corvus",
                ColonyMarketFixture.buildVisibleColony("hegemony"),
                ColonyMarketFixture.buildVisibleColony("hegemony"));

            var report = readReport(sector, FactionListingFilter.ALL);

            assertThat(report)
                .contains("[hegemony] Hegemony - Neutral (0 / 100)"
                    + "\n    holdings: 2 (0 hidden, 0 discoverable)  systems: corvus");
        }

        @Test
        void printsNoVisibilityCountsOrSystemsForAFactionHoldingNothing() {
            // A row of noughts and an empty systems clause read as a finding rather
            // than as the absence they are, and "holdings: 0" already says it.
            var sector = new FactionListingFixture();

            sector.addFaction(buildFaction("derelict", "Derelict"));

            assertThat(readReport(sector, FactionListingFilter.ALL))
                .contains("[derelict] Derelict - Neutral (0 / 100)\n    holdings: 0")
                .doesNotContain("hidden")
                .doesNotContain("systems:");
        }

        @Test
        void countsTwoMarketsOnOneBodyAsOneHolding() {
            // A mod supersedes a colony by adding its own market beside vanilla's on
            // the same body. Counted per market, that place is banked twice and its
            // owner reads as holding twice what the player can see.
            var sector = new FactionListingFixture();
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
            var sector = new FactionListingFixture();

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
            var sector = new FactionListingFixture();

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
            var sector = new FactionListingFixture();

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

            var sector = new FactionListingFixture();

            sector.addFaction(buildTerritorialFaction("hegemony", "Hegemony"));

            assertThat(readReport(sector, FactionListingFilter.ALL))
                .contains("[hegemony] Hegemony [territorial] - Neutral (0 / 100)");
        }

        @Test
        void reportsThePlayerSOwnFactionAsSelf() {
            // The engine answers for a relationship with oneself; printed, it reads as
            // a finding about the player's standing with the player.
            var sector = new FactionListingFixture();
            var playerFactionMock = buildFaction("player", "Sindrian Diktat");

            sector.addFaction(playerFactionMock);
            sector.setPlayerFaction(playerFactionMock);

            assertThat(readReport(sector, FactionListingFilter.ALL))
                .contains("[player] Sindrian Diktat - (self)");
        }

        @Test
        void namesAFactionCarryingAPlaceholderDisplayNameByItsId() {
            // A stock Nexerelin player.faction reports the literal "player" as its
            // display name, and vanilla reports "Independent" before the first colony.
            // Repeating the id says no more than is known, which is the point.
            var sector = new FactionListingFixture();

            sector.addFaction(buildFaction("player", "player"));

            assertThat(readReport(sector, FactionListingFilter.ALL))
                .contains("[player] player");
        }

        @Test
        void ordersTheFactionsById() {
            // The sector's own order is load order, which differs between installs and
            // makes two runs hard to compare.
            var sector = new FactionListingFixture();

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

            var sector = new FactionListingFixture();

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

            var sector = new FactionListingFixture();

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

            var sector = new FactionListingFixture();

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
            var sector = new FactionListingFixture();

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
            var sector = new FactionListingFixture();

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
        void namesBothNamesWhereAFactionCarriesADistinctLongOne() {
            // The long name is the one a player reads in prose and the short one what the UI
            // labels it with; a listing meant for looking a faction up is worth both.
            var sector = new FactionListingFixture();

            sector.addFaction(buildNamedFaction("tritachyon", "Tri-Tachyon Corporation", "Tri-Tachyon"));

            assertThat(readReport(sector, FactionListingFilter.ALL))
                .contains("[tritachyon] Tri-Tachyon Corporation / Tri-Tachyon - Neutral (0 / 100)");
        }

        @Test
        void namesOneNameWhereTheLongOneSaysNothingMore() {
            // Most factions declare no long name or the same one twice, and the pair repeated on
            // every line costs more width than it carries.
            var sector = new FactionListingFixture();

            sector.addFaction(buildNamedFaction("hegemony", "Hegemony", "Hegemony"));

            assertThat(readReport(sector, FactionListingFilter.ALL))
                .contains("[hegemony] Hegemony - Neutral (0 / 100)")
                .doesNotContain("Hegemony / Hegemony");
        }

        @Test
        void namesTheModAFactionWasDeclaredByWithItsId() {
            // The id alone says nothing about where a faction came from, which on a heavily
            // modded install is most of what a reader opens this listing to find out. The mod id
            // rides along because it is what another command takes as an argument.
            var sector = new FactionListingFixture();

            sector.addFaction(buildFaction("tahlan_greathouses", "Great Houses"));

            assertThat(readReport(
                    sector,
                    FactionListingFilter.ALL,
                    Map.of("tahlan_greathouses", new ModSource("Tahlan Shipworks", "tahlan"))))
                .contains("[tahlan_greathouses] Great Houses - Neutral (0 / 100)"
                    + " - from: Tahlan Shipworks [tahlan]");
        }

        @Test
        void namesASourceWithoutAModIdOnItsOwn() {
            // The base game is not a mod and has no id to give; a bracket around nothing would
            // read as one it failed to report.
            var sector = new FactionListingFixture();

            sector.addFaction(buildFaction("hegemony", "Hegemony"));

            assertThat(readReport(
                    sector,
                    FactionListingFilter.ALL,
                    Map.of("hegemony", new ModSource("vanilla", null))))
                .contains("[hegemony] Hegemony - Neutral (0 / 100) - from: vanilla")
                .doesNotContain("vanilla [");
        }

        @Test
        void marksAFactionNoDeclarationNamesAsUnattributed() {
            // A faction a mod built at runtime is in no spreadsheet, and saying so beats a blank
            // where every neighbouring line carries a name.
            var sector = new FactionListingFixture();

            sector.addFaction(buildFaction("hegemony", "Hegemony"));
            sector.addFaction(buildFaction("runtime_faction", "Someone's Own"));

            var report = readReport(
                sector,
                FactionListingFilter.ALL,
                Map.of("hegemony", new ModSource("vanilla", null)));

            assertThat(report)
                .contains("[hegemony] Hegemony - Neutral (0 / 100) - from: vanilla")
                .contains("[runtime_faction] Someone's Own - Neutral (0 / 100)"
                    + " - from: (unattributed)");
        }

        @Test
        void dropsTheSourceClauseWhenNothingWasDeclared() {
            // Nothing read is the read having failed or the game not being up, neither of which
            // is a fact about any faction - marking them all unattributed would report it as one.
            var sector = new FactionListingFixture();

            sector.addFaction(buildFaction("hegemony", "Hegemony"));

            assertThat(readReport(sector, FactionListingFilter.ALL, Map.of()))
                .doesNotContain("from:");
        }

        @Test
        void dropsTheHoldingsLineUnderTheNoHoldingsKeyword() {
            // One line per faction is what makes a sector's worth of them scannable side by side.
            // The header goes with it: a listing reporting no holdings must not open by saying it
            // does, which is the half a check for the line alone would miss.
            var sector = new FactionListingFixture();

            sector.addFaction(buildFaction("hegemony", "Hegemony"));
            sector.addSystemHolding(
                "corvus",
                ColonyMarketFixture.buildVisibleColony("hegemony"));

            assertThat(readReport(
                    sector,
                    FactionListingFilter.ALL,
                    EnumSet.of(ListingOption.OMIT_HOLDINGS),
                    Map.of()))
                .isEqualTo("Factions:\n[hegemony] Hegemony - Neutral (0 / 100)");
        }

        @Test
        void dropsTheAttitudeUnderTheNoAttitudeKeyword() {
            // Dropped with its separator rather than leaving the gap it sat in, which would read
            // as a standing the listing failed to report.
            var sector = new FactionListingFixture();

            sector.addFaction(buildFaction("hegemony", "Hegemony"));

            assertThat(readReport(
                    sector,
                    FactionListingFilter.ALL,
                    EnumSet.of(ListingOption.OMIT_ATTITUDE),
                    Map.of("hegemony", new ModSource("vanilla", null))))
                .contains("[hegemony] Hegemony - from: vanilla")
                .doesNotContain("Neutral");
        }

        @Test
        void keepsTheFilterAndTheOptionsIndependent() {
            // The filter decides who is listed and the options what is shown of them, which is
            // what lets a run name one of each - the pair a narrowed one-line listing needs.
            var sector = new FactionListingFixture();

            sector.addFaction(buildFaction("pirates", "Pirates"));
            sector.addFaction(buildFaction("hegemony", "Hegemony"));
            sector.addSystemHolding(
                "kumari_kandam",
                ColonyMarketFixture.buildFoundConcealedColony("pirates"));

            assertThat(readReport(
                    sector,
                    FactionListingFilter.HOLDS_HIDDEN,
                    EnumSet.of(ListingOption.OMIT_HOLDINGS),
                    Map.of()))
                .isEqualTo("Factions (hidden):\n[pirates] Pirates - Neutral (0 / 100)");
        }

        @Test
        void namesTheKeywordInTheHeader() {

            var sector = new FactionListingFixture();

            assertThat(readReport(sector, FactionListingFilter.HOLDS_HIDDEN))
                .contains("Factions and their holdings (hidden):");
        }

        @Test
        void reportsNoneWhenNoFactionMatches() {

            var sector = new FactionListingFixture();

            sector.addFaction(buildFaction("derelict", "Derelict"));

            assertThat(readReport(sector, FactionListingFilter.HOLDS_ANYTHING))
                .contains("(none)");
        }
    }

    private static String readReport(FactionListingFixture sector, FactionListingFilter filter) {
        return readReport(sector, filter, Map.of());
    }

    private static String readReport(
            FactionListingFixture sector,
            FactionListingFilter filter,
            Map<String, ModSource> sourcesByFactionId) {

        return readReport(sector, filter, EnumSet.noneOf(ListingOption.class), sourcesByFactionId);
    }

    private static String readReport(
            FactionListingFixture sector,
            FactionListingFilter filter,
            Set<ListingOption> options,
            Map<String, ModSource> sourcesByFactionId) {

        return new FactionListingReport(filter, options, sourcesByFactionId)
            .describeFactions(sector.getSector());
    }
}
