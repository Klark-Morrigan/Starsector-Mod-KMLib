package kmlib.mods.console.commands.targets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Pins the pairing of the two searches a command aims with: that both halves come back together,
 * that either refusal ends the run, and that the refusal a run with two mistakes reports is the
 * one about the place.
 *
 * <p>The two searches are stubbed through static mocks, so what makes a place qualify and what an
 * omitted argument means stay their own suites' business. What is asserted here is the pairing -
 * the order, and that nothing is looked up after a refusal. Cases live under a {@link Nested}
 * group named for the method under test.
 */
final class MarketOwnerTargetResolverTest {

    private static final String ENTITY_ID = "corvus_iii";
    private static final String FACTION_ID = "hegemony";
    private static final String NO_ENTITY_MESSAGE = "No entity with ID 'corvus_iv' in the sector.";
    private static final String NO_FACTION_MESSAGE = "No faction with ID 'hegmony'.";

    private MockedStatic<MarketTargetResolver> marketTargetResolverMock;
    private MockedStatic<FactionTargetResolver> factionTargetResolverMock;

    private SectorAPI sectorMock;
    private MarketAPI marketMock;
    private FactionAPI factionMock;

    @BeforeEach
    void setUp() {

        sectorMock = mock(SectorAPI.class);
        marketMock = mock(MarketAPI.class);
        factionMock = mock(FactionAPI.class);

        marketTargetResolverMock = mockStatic(MarketTargetResolver.class);
        marketTargetResolverMock
            .when(() -> MarketTargetResolver.resolveTargetMarket(any(), any(), any()))
            .thenReturn(new ResolvedTarget<>(marketMock));

        factionTargetResolverMock = mockStatic(FactionTargetResolver.class);
        factionTargetResolverMock
            .when(() -> FactionTargetResolver.resolveOwningFaction(any(), any()))
            .thenReturn(new ResolvedTarget<>(factionMock));
    }

    @AfterEach
    void tearDown() {
        factionTargetResolverMock.close();
        marketTargetResolverMock.close();
    }

    @Nested
    class ResolveMarketAndOwner {

        @Test
        void pairsThePlaceWithTheOwnerWhenBothAreFound() {

            var resolution = MarketOwnerTargetResolver.resolveMarketAndOwner(
                sectorMock,
                ENTITY_ID,
                FACTION_ID,
                MarketTargetRequirement.EXISTING_COLONY);

            assertThat(resolution)
                .isEqualTo(new ResolvedTarget<>(new MarketOwnerTarget(marketMock, factionMock)));
        }

        @Test
        void posesEachSearchTheArgumentItAnswersFor() {
            // The requirement goes to the place search and the faction ID to the owner search,
            // and an omitted argument is passed on as omitted rather than defaulted here.
            MarketOwnerTargetResolver.resolveMarketAndOwner(
                sectorMock,
                null,
                null,
                MarketTargetRequirement.COLONISABLE_BODY);

            marketTargetResolverMock
                .verify(() -> MarketTargetResolver.resolveTargetMarket(
                    sectorMock,
                    null,
                    MarketTargetRequirement.COLONISABLE_BODY));
            factionTargetResolverMock
                .verify(() -> FactionTargetResolver.resolveOwningFaction(sectorMock, null));
        }

        @Test
        void passesOnThePlaceSearchsRefusal() {

            refuseThePlace();

            var resolution = MarketOwnerTargetResolver.resolveMarketAndOwner(
                sectorMock,
                "corvus_iv",
                FACTION_ID,
                MarketTargetRequirement.EXISTING_COLONY);

            assertThat(resolution)
                .isEqualTo(new UnresolvedTarget<MarketOwnerTarget>(NO_ENTITY_MESSAGE));
        }

        @Test
        void looksForNoOwnerOnceThePlaceIsRefused() {
            // Nothing is resolved after a refusal: the run is over, and a second search would be
            // work done for an answer nobody reads.
            refuseThePlace();

            MarketOwnerTargetResolver.resolveMarketAndOwner(
                sectorMock,
                "corvus_iv",
                FACTION_ID,
                MarketTargetRequirement.EXISTING_COLONY);

            factionTargetResolverMock
                .verifyNoInteractions();
        }

        @Test
        void passesOnTheOwnerSearchsRefusal() {

            refuseTheOwner();

            var resolution = MarketOwnerTargetResolver.resolveMarketAndOwner(
                sectorMock,
                ENTITY_ID,
                "hegmony",
                MarketTargetRequirement.EXISTING_COLONY);

            assertThat(resolution)
                .isEqualTo(new UnresolvedTarget<MarketOwnerTarget>(NO_FACTION_MESSAGE));
        }

        @Test
        void reportsThePlaceRatherThanTheOwnerWhenBothAreWrong() {
            // A run with two mistakes in it has to report one of them, and the place is the
            // argument a player is likelier to have got wrong - so it is asked first and its
            // refusal is the one that gets said.
            refuseThePlace();
            refuseTheOwner();

            var resolution = MarketOwnerTargetResolver.resolveMarketAndOwner(
                sectorMock,
                "corvus_iv",
                "hegmony",
                MarketTargetRequirement.EXISTING_COLONY);

            assertThat(resolution)
                .isEqualTo(new UnresolvedTarget<MarketOwnerTarget>(NO_ENTITY_MESSAGE));
        }
    }

    // Has the place search answer that there is no such entity.
    private void refuseThePlace() {

        marketTargetResolverMock
            .when(() -> MarketTargetResolver.resolveTargetMarket(any(), any(), any()))
            .thenReturn(new UnresolvedTarget<MarketAPI>(NO_ENTITY_MESSAGE));
    }

    // Has the owner search answer that there is no such faction.
    private void refuseTheOwner() {

        factionTargetResolverMock
            .when(() -> FactionTargetResolver.resolveOwningFaction(any(), any()))
            .thenReturn(new UnresolvedTarget<FactionAPI>(NO_FACTION_MESSAGE));
    }
}
