package kmlib.starsector.markets.colonisation;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;

import kmlib.extensions.DeclinedWork;
import kmlib.extensions.ExecutedWork;
import kmlib.starsector.markets.MarketOwnershipFixture;
import kmlib.testfixtures.starsector.markets.MarketStateFixture;
import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static kmlib.testfixtures.starsector.settings.StubbedModIds.NEXERELIN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Pins the contract of {@link MarketColoniser}. The cases live in one {@link Nested} group per
 * method so the suite reports as a per-method tree; the markets an eligibility read is posed
 * against are {@link MarketStateFixture}'s, named for the shape each one is, and the bodies a
 * founding is posed against are {@link MarketColonisationFixture}'s, which answer from what has
 * been done to them.
 */
final class MarketColoniserTest {

    // The size a colony is founded at, spelt here rather than read off the class under test: what
    // is being pinned is that founding produces the size the game's own routines produce, and a
    // case reading the number from the code it checks would pass at any size at all.
    private static final int BASELINE_COLONY_SIZE = 3;

    // What registering a colony asks the economy for besides the listing itself: the orbital junk
    // and radio chatter of a place that has just become inhabited. Spelt here for the same reason
    // as the size - a case reading the flag off the code it checks would pass under either value.
    private static final boolean WITH_ORBITAL_JUNK_AND_CHATTER = true;

    @Nested
    class IsReadyForColonisation {

        @Test
        void acceptsABodyCarryingOnlySurveyData() {
            // What procgen leaves on every uninhabited world: a market holding the planet's
            // conditions and nothing else, hung on the entity and never registered.
            assertThat(MarketColoniser.isReadyForColonisation(
                    MarketStateFixture.buildColonisableBody()))
                .isTrue();
        }

        @Test
        void rejectsAColonyThatAlreadyExists() {

            assertThat(MarketColoniser.isReadyForColonisation(
                    MarketStateFixture.buildColony("hegemony")))
                .isFalse();
        }

        @Test
        void rejectsAColonyTheEconomyDoesNotList() {
            // Vanilla builds Galatia Academy this way on purpose. Only the condition-only arm
            // rejects it, which is why registration cannot be the whole of the read.
            assertThat(MarketColoniser.isReadyForColonisation(
                    MarketStateFixture.buildColonyUnlistedByEconomy("independent")))
                .isFalse();
        }

        @Test
        void rejectsSurveyDataTheEconomyAlreadyLists() {
            // Nothing vanilla builds, and founding on it would register the market a second
            // time - so the registration arm answers a shape the flag alone would admit.
            assertThat(MarketColoniser.isReadyForColonisation(
                    MarketStateFixture.buildRegisteredSurveyData()))
                .isFalse();
        }

        @Test
        void rejectsANullMarket() {
            assertThat(MarketColoniser.isReadyForColonisation(null))
                .isFalse();
        }
    }

    @Nested
    class FoundColony {

        // Where the colonies these cases found are founded, and the economy they are registered
        // with. Held as fields rather than arranged case by case: every case but the two posing
        // their absence founds into the same running game, and what a case is about is what
        // founding did to the market, not where it was founded.
        private final SectorAPI sector = MarketColonisationFixture.buildSectorWithEconomy();
        private final EconomyAPI economy = MarketColonisationFixture.readEconomy(sector);

        @Test
        void turnsSurveyDataIntoASettledColonyOfTheBaselineSize() {
            // The whole of what founding means, read off the market afterwards: no longer the
            // placeholder it was, populated, sized, and as old as a colony founded this moment.
            var market = MarketColonisationFixture.buildColonisableWorld();

            MarketColoniser.foundColony(sector, market);

            assertThat(market.isPlanetConditionMarketOnly())
                .isFalse();
            assertThat(market.hasCondition(Conditions.POPULATION_3))
                .isTrue();
            assertThat(market.hasIndustry(Industries.POPULATION))
                .isTrue();
            assertThat(market.getSize())
                .isEqualTo(BASELINE_COLONY_SIZE);
            assertThat(market.getDaysInExistence())
                .isZero();
        }

        @Test
        void namesTheColonyAfterTheBodyItStandsOn() {

            var market = MarketColonisationFixture.buildColonisableWorld();

            MarketColoniser.foundColony(sector, market);

            assertThat(market.getName())
                .isEqualTo(MarketColonisationFixture.BODY_NAME);
        }

        @Test
        void surveysEveryConditionTheBodyWasCarrying() {
            // A colony founded from outside the survey panel can stand on a body nobody has
            // looked at, and its owner would then be unable to read the conditions they are
            // living with - so founding surveys what it settles. The population the colony gains
            // afterwards is not among them, arriving after the survey exactly as it does in the
            // game's own routine, and carrying nothing a survey would have revealed.
            var market = MarketColonisationFixture.buildColonisableWorldCarrying(
                Conditions.HABITABLE,
                Conditions.ORE_MODERATE);

            MarketColoniser.foundColony(sector, market);

            assertThat(MarketColonisationFixture.readSurveyedConditionIds(market))
                .containsExactlyInAnyOrder(Conditions.HABITABLE, Conditions.ORE_MODERATE);
            assertThat(market.getSurveyLevel())
                .isEqualTo(MarketAPI.SurveyLevel.FULL);
        }

        @Test
        void resettlesARuinedWorldAsTheDescendantsOfWhoWasLeft() {
            // The ruins condition and the colony standing on them cannot both be true, so one
            // replaces the other rather than being added beside it.
            var market = MarketColonisationFixture.buildColonisableWorldCarrying(
                Conditions.DECIVILIZED);

            MarketColoniser.foundColony(sector, market);

            assertThat(market.hasCondition(Conditions.DECIVILIZED))
                .isFalse();
            assertThat(market.hasCondition(Conditions.DECIVILIZED_SUBPOP))
                .isTrue();
        }

        @Test
        void leavesAWorldThatWasNeverSettledCarryingNoRuins() {
            // The swap is conditional on ruins being there: an untouched world must not come out
            // of founding carrying the descendants of a colony it never had.
            var market = MarketColonisationFixture.buildColonisableWorld();

            MarketColoniser.foundColony(sector, market);

            assertThat(market.hasCondition(Conditions.DECIVILIZED_SUBPOP))
                .isFalse();
        }

        @Test
        void registersTheColonyWithTheEconomyAndStepsIt() {
            // Registration alone leaves a colony the economy lists and never feeds until the next
            // monthly tick, so the steps that derive its supply and demand go with it.
            var market = MarketColonisationFixture.buildColonisableWorld();

            MarketColoniser.foundColony(sector, market);

            verify(economy)
                .addMarket(market, WITH_ORBITAL_JUNK_AND_CHATTER);
            verify(economy)
                .tripleStep();
            verify(market)
                .advance(0f);
        }

        @Test
        void queuesTheColonySFirstSpaceport() {
            // Queued rather than built: a colony with no spaceport is cut off from everything
            // that reaches it by ship, and both of the game's own colonisation routines leave the
            // same one waiting.
            var market = MarketColonisationFixture.buildColonisableWorld();

            MarketColoniser.foundColony(sector, market);

            assertThat(market.getConstructionQueue().hasItem(Industries.SPACEPORT))
                .isTrue();
        }

        @Test
        void pointsTheColonyAndItsBodyAtEachOther() {

            var market = MarketColonisationFixture.buildColonisableWorld();
            var body = MarketColonisationFixture.readBody(market);

            MarketColoniser.foundColony(sector, market);

            verify(body)
                .setMarket(market);
            verify(market)
                .setPrimaryEntity(body);
        }

        @Test
        void foundsAColonyOnAPlaceTheGameGaveNoBody() {
            // A market can stand for a place with no entity: there is no name to take and no link
            // to state, and the colony is still founded and registered.
            var market = MarketColonisationFixture.buildColonisablePlaceWithNoBody();

            MarketColoniser.foundColony(sector, market);

            assertThat(market.isPlanetConditionMarketOnly())
                .isFalse();
            verify(economy)
                .addMarket(market, WITH_ORBITAL_JUNK_AND_CHATTER);
        }

        @Test
        void leavesAMarketThatIsAlreadyAColonyAlone() {
            // Founding on a colony would enter it in the economy a second time, so a market that
            // is not survey data is passed over rather than founded again.
            MarketColoniser.foundColony(
                sector,
                MarketStateFixture.buildColony(MarketColonisationFixture.FACTION_OWNER_ID));

            verifyNoInteractions(economy);
        }

        @Test
        void leavesSurveyDataAloneWhenThereIsNoEconomyToRegisterItWith() {
            // Half a founding is worse than none - a settled market the economy never feeds - so
            // the whole sequence is withheld rather than run up to the registration.
            var market = MarketColonisationFixture.buildColonisableWorld();

            MarketColoniser.foundColony(
                MarketColonisationFixture.buildSectorWithoutEconomy(),
                market);

            assertThat(market.isPlanetConditionMarketOnly())
                .isTrue();
        }

        @Test
        void leavesANullMarketAlone() {
            assertThatCode(() -> MarketColoniser.foundColony(sector, null))
                .doesNotThrowAnyException();
        }

        @Test
        void leavesAMarketAloneWhenThereIsNoSector() {

            var market = MarketColonisationFixture.buildColonisableWorld();

            MarketColoniser.foundColony(null, market);

            assertThat(market.isPlanetConditionMarketOnly())
                .isTrue();
        }
    }

    @Nested
    class EstablishColony {

        // The same running game the founding cases pose, for the same reason - see FoundColony.
        private final SectorAPI sector = MarketColonisationFixture.buildSectorWithEconomy();
        private final EconomyAPI economy = MarketColonisationFixture.readEconomy(sector);

        @Test
        void foundsTheColonyAndHandsItToThePlayer() {

            var market = MarketColonisationFixture.buildColonisableWorld();

            try (var listenerUtilMock = mockStatic(ListenerUtil.class)) {
                MarketColoniser.establishColony(sector, market, Factions.PLAYER);
            }

            assertThat(market.isPlanetConditionMarketOnly())
                .isFalse();
            assertThat(market.isPlayerOwned())
                .isTrue();
            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .contains("local_resources");
        }

        @Test
        void foundsTheColonyAndHandsItToAFaction() {

            var market = MarketColonisationFixture.buildColonisableWorld();

            MarketColoniser.establishColony(
                sector,
                market,
                MarketColonisationFixture.FACTION_OWNER_ID);

            assertThat(market.isPlanetConditionMarketOnly())
                .isFalse();
            assertThat(market.getFactionId())
                .isEqualTo(MarketColonisationFixture.FACTION_OWNER_ID);
            assertThat(market.isPlayerOwned())
                .isFalse();
            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .contains("open_market", "black_market");
        }

        @Test
        void addsAStorageSubmarketToAColonyFoundedForAFaction() {
            // The game's own faction colony has one - its story rulecmd opens it alongside the
            // trading counters, and the market setup every procgen faction market comes from lists
            // it in the same breath. A colony founded without one is a place nothing can be stored
            // at, which is not a shape the game has.
            var market = MarketColonisationFixture.buildColonisableWorld();

            MarketColoniser.establishColony(
                sector,
                market,
                MarketColonisationFixture.FACTION_OWNER_ID);

            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .contains("storage");
        }

        @Test
        void namesTheOwnerBeforeTheEconomyHearsAboutTheColony() {
            // The order both of the game's own colonisation routines take: the economy is stepped
            // against a colony already flying its flag and trading over its owner's counters,
            // rather than against an ownerless one that is re-flagged afterwards.
            var market = MarketColonisationFixture.buildColonisableWorld();

            MarketColoniser.establishColony(
                sector,
                market,
                MarketColonisationFixture.FACTION_OWNER_ID);

            var order = inOrder(market, economy);

            order.verify(market)
                .setFactionId(MarketColonisationFixture.FACTION_OWNER_ID);
            order.verify(economy)
                .addMarket(market, WITH_ORBITAL_JUNK_AND_CHATTER);
        }

        @Test
        void reportsThePlayerSColonisationOfAPlanet() {

            var market = MarketColonisationFixture.buildColonisableWorld();
            var world = MarketColonisationFixture.readWorld(market);

            try (var listenerUtilMock = mockStatic(ListenerUtil.class)) {

                MarketColoniser.establishColony(sector, market, Factions.PLAYER);

                listenerUtilMock.verify(() -> ListenerUtil.reportPlayerColonizedPlanet(world));
            }
        }

        @Test
        void reportsNothingWhenAFactionFoundsTheColony() {
            // The report is named for the player colonising and its listeners file it as the
            // player's doing, so a faction's colony must not raise it.
            var market = MarketColonisationFixture.buildColonisableWorld();

            try (var listenerUtilMock = mockStatic(ListenerUtil.class)) {

                MarketColoniser.establishColony(
                    sector,
                    market,
                    MarketColonisationFixture.FACTION_OWNER_ID);

                listenerUtilMock.verifyNoInteractions();
            }
        }

        @Test
        void foundsTheColonySilentlyWhenItsBodyIsNotAPlanet() {
            // The report carries a planet, so a colony founded on anything else has nothing to
            // report with - which is a colony founded quietly rather than one refused.
            var market = MarketColonisationFixture.buildColonisableStation();

            try (var listenerUtilMock = mockStatic(ListenerUtil.class)) {

                MarketColoniser.establishColony(sector, market, Factions.PLAYER);

                listenerUtilMock.verifyNoInteractions();
            }

            assertThat(market.isPlanetConditionMarketOnly())
                .isFalse();
        }

        @Test
        void handsTheWholeFoundingToAColonisationRoutineTheInstallSupplies() {
            // A mod that founds its own colonies records things about one that nothing can add
            // afterwards, so its routine takes the founding whole and none of this sequence runs
            // under it - the market is left exactly as that routine found it.
            var market = MarketColonisationFixture.buildColonisableWorld();
            var offeredMarket = new AtomicReference<MarketAPI>();
            var offeredFactionId = new AtomicReference<String>();
            var offeredSize = new AtomicInteger();

            MarketColoniser.establishColony(
                sector,
                market,
                MarketColonisationFixture.FACTION_OWNER_ID,
                (routineSector, routineMarket, routineFactionId, routineColonySize) -> {
                    offeredMarket.set(routineMarket);
                    offeredFactionId.set(routineFactionId);
                    offeredSize.set(routineColonySize);
                    return new ExecutedWork();
                });

            assertThat(offeredMarket.get())
                .isSameAs(market);
            assertThat(offeredFactionId.get())
                .isEqualTo(MarketColonisationFixture.FACTION_OWNER_ID);
            // The size a routine is handed is the one the composed sequence founds at, so a colony
            // is the same size whichever of the two produced it.
            assertThat(offeredSize.get())
                .isEqualTo(BASELINE_COLONY_SIZE);
            assertThat(market.isPlanetConditionMarketOnly())
                .isTrue();
            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .isEmpty();
            verifyNoInteractions(economy);
        }

        @Test
        void foundsTheColonyItselfWhenTheInstalledRoutineDeclinesIt() {
            // The answer on every install without such a mod, and on a body its routine cannot
            // found on - so the composed sequence runs in its place and the colony still arrives.
            var market = MarketColonisationFixture.buildColonisableWorld();

            MarketColoniser.establishColony(
                sector,
                market,
                MarketColonisationFixture.FACTION_OWNER_ID,
                (routineSector, routineMarket, routineFactionId, routineColonySize) ->
                    new DeclinedWork("this stub founds nothing"));

            assertThat(market.isPlanetConditionMarketOnly())
                .isFalse();
            assertThat(market.getFactionId())
                .isEqualTo(MarketColonisationFixture.FACTION_OWNER_ID);
            verify(economy)
                .addMarket(market, WITH_ORBITAL_JUNK_AND_CHATTER);
        }

        @Test
        void foundsTheColonyItselfThroughTheLiveBindingWhenNoSuchModIsInstalled() {
            // The public entry point rather than the seam beneath it: nothing else here exercises
            // the routine the library actually binds, and an install without that mod is what the
            // fallback exists for.
            var market = MarketColonisationFixture.buildColonisableWorld();

            ModStateScopes.runWithModEnabled(NEXERELIN, false, () ->
                MarketColoniser.establishColony(
                    sector,
                    market,
                    MarketColonisationFixture.FACTION_OWNER_ID));

            assertThat(market.isPlanetConditionMarketOnly())
                .isFalse();
            verify(economy)
                .addMarket(market, WITH_ORBITAL_JUNK_AND_CHATTER);
        }

        @Test
        void offersNothingToTheInstalledRoutineWhenThereIsNoColonyToFound() {
            // The founding conditions are asked first, so a market this library would refuse is
            // not one a mod is handed - a colony founded a second time being the outcome either
            // sequence has to avoid.
            var routineOfferCount = new AtomicInteger();

            MarketColoniser.establishColony(
                sector,
                MarketStateFixture.buildColony(MarketColonisationFixture.FACTION_OWNER_ID),
                Factions.PLAYER,
                (routineSector, routineMarket, routineFactionId, routineColonySize) -> {
                    routineOfferCount.incrementAndGet();
                    return new ExecutedWork();
                });

            assertThat(routineOfferCount.get())
                .isZero();
        }

        @Test
        void leavesSurveyDataAloneWhenNoOwnerIsNamed() {
            // Naming nobody would found a colony no faction holds, which is neither what a
            // colonisable body is nor what a colony is.
            var market = MarketColonisationFixture.buildColonisableWorld();

            MarketColoniser.establishColony(sector, market, null);

            assertThat(market.isPlanetConditionMarketOnly())
                .isTrue();
            verifyNoInteractions(economy);
        }

        @Test
        void leavesSurveyDataUnownedWhenThereIsNoEconomyToFoundInto() {
            // The owner has to be withheld along with the founding, or a body that could not be
            // colonised is left flying a flag and trading over an owner's counters with no colony
            // under them - which is why the founding conditions are asked before the hand-over
            // rather than only inside the founding it precedes.
            var market = MarketColonisationFixture.buildColonisableWorld();

            MarketColoniser.establishColony(
                MarketColonisationFixture.buildSectorWithoutEconomy(),
                market,
                Factions.PLAYER);

            assertThat(market.isPlanetConditionMarketOnly())
                .isTrue();
            assertThat(market.isPlayerOwned())
                .isFalse();
            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .isEmpty();
        }

        @Test
        void leavesAMarketThatIsAlreadyAColonyAlone() {

            var market = MarketStateFixture.buildColony(MarketColonisationFixture.FACTION_OWNER_ID);

            MarketColoniser.establishColony(sector, market, Factions.PLAYER);

            verifyNoInteractions(economy);
        }

        @Test
        void leavesANullMarketAlone() {
            assertThatCode(() -> MarketColoniser.establishColony(sector, null, Factions.PLAYER))
                .doesNotThrowAnyException();
        }
    }
}
