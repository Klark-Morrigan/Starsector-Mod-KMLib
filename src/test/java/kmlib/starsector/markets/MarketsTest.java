package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomEntitySpecAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;

import kmlib.starsector.entities.EntityMapIcon;
import kmlib.starsector.entities.EntityNameplate;
import kmlib.testfixtures.starsector.markets.MarketStateFixture;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link Markets#findAttachedStation},
 * {@link Markets#getStabilityFraction}, {@link Markets#hasAttachedStation},
 * {@link Markets#isAbandonedStation}, {@link Markets#isMilitary}, {@link Markets#isOwnedBy},
 * {@link Markets#isOwnedColony}, {@link Markets#isSettledColony}, {@link Markets#readNameplate}
 * and
 * {@link Markets#readSubmarketPlugin} - the reads that answer what one market is, plus the one
 * reach into how it is put together. The cases live in a
 * {@link Nested} group per method so the suite reports as a per-method tree; the shared mock
 * builders stay on the outer class.
 *
 * <p>What may be said about a market is pinned by {@link MarketVisibilityTest}, and which of
 * several markets speaks for a place by {@link MarketColocationTest}.
 */
final class MarketsTest {

    @Nested
    class FindAttachedStation {

        @Test
        void yieldsTheStationAMarketOwns() {

            var station = buildStationEntity();
            var market = buildMarketConnectedTo(station);

            assertThat(Markets.findAttachedStation(market))
                .contains(station);
        }

        @Test
        void picksTheStationOutOfTheMarketSOtherConnectedEntities() {

            var station = buildStationEntity();
            var market = buildMarketConnectedTo(buildNonStationEntity(), station);

            assertThat(Markets.findAttachedStation(market))
                .contains(station);
        }

        @Test
        void yieldsEmptyForAMarketWithNoConnectedEntities() {

            var market = buildMarketConnectedTo();

            assertThat(Markets.findAttachedStation(market))
                .isEmpty();
        }

        @Test
        void yieldsEmptyWhenNoConnectedEntityIsAStation() {

            var market = buildMarketConnectedTo(buildNonStationEntity());

            assertThat(Markets.findAttachedStation(market))
                .isEmpty();
        }

        @Test
        void yieldsEmptyForAStationTaggedEntityRaisingNoStationFleet() {
            // The tag alone says "this is a station", not "this defends the market". The
            // fleet an orbital-station industry raises is what says the second.
            var market = buildMarketConnectedTo(buildFleetlessStationEntity());

            assertThat(Markets.findAttachedStation(market))
                .isEmpty();
        }

        @Test
        void picksTheFleetedStationOverAFleetlessStationTaggedEntity() {
            // The shape a station-sited market with a real orbital station has: it is
            // connected to the station it is built on and to the one defending it, both
            // tagged. The scan has to qualify every tagged entity rather than settling on
            // the first one it meets.
            var station = buildStationEntity();
            var market = buildMarketConnectedTo(buildFleetlessStationEntity(), station);

            assertThat(Markets.findAttachedStation(market))
                .contains(station);
        }

        @Test
        void ignoresAStationTaggedNoOrbitalStation() {

            var market = buildMarketConnectedTo(buildOptedOutStationEntity());

            assertThat(Markets.findAttachedStation(market))
                .isEmpty();
        }

        @Test
        void yieldsEmptyForANullMarket() {
            assertThat(Markets.findAttachedStation(null))
                .isEmpty();
        }

        @Test
        void yieldsEmptyForNullConnectedEntities() {

            var marketMock = mock(MarketAPI.class);

            when(marketMock.getConnectedEntities())
                .thenReturn(null);

            assertThat(Markets.findAttachedStation(marketMock))
                .isEmpty();
        }
    }

    @Nested
    class GetStabilityFraction {

        @Test
        void fullStabilityIsOne() {
            assertThat(Markets.getStabilityFraction(buildMarketAtStability(10.0f)))
                .isEqualTo(1.0);
        }

        @Test
        void halfStabilityIsAHalf() {
            assertThat(Markets.getStabilityFraction(buildMarketAtStability(5.0f)))
                .isEqualTo(0.5);
        }

        @Test
        void noStabilityIsZero() {
            assertThat(Markets.getStabilityFraction(buildMarketAtStability(0.0f)))
                .isEqualTo(0.0);
        }

        @Test
        void aboveBandClampsToOne() {
            assertThat(Markets.getStabilityFraction(buildMarketAtStability(12.0f)))
                .isEqualTo(1.0);
        }

        @Test
        void belowBandClampsToZero() {
            assertThat(Markets.getStabilityFraction(buildMarketAtStability(-3.0f)))
                .isEqualTo(0.0);
        }

        @Test
        void nullMarketIsZero() {
            assertThat(Markets.getStabilityFraction(null))
                .isEqualTo(0.0);
        }
    }

    // The presence verdict is the entity read taken as a boolean, so this group pins that
    // pairing - a discovered station reads true, an undiscovered one false - plus the null contract
    // its own Javadoc states. The scan's edge cases (the NO_ORBITAL_STATION opt-out, absent
    // or null connected entities) belong to the read that runs the scan, above.
    @Nested
    class HasAttachedStation {

        @Test
        void returnsTrueForAMarketThatOwnsAStation() {

            var market = buildMarketConnectedTo(buildStationEntity());

            assertThat(Markets.hasAttachedStation(market))
                .isTrue();
        }

        @Test
        void returnsFalseWhenNoConnectedEntityIsAStation() {

            var market = buildMarketConnectedTo(buildNonStationEntity());

            assertThat(Markets.hasAttachedStation(market))
                .isFalse();
        }

        @Test
        void returnsFalseForANullMarket() {
            assertThat(Markets.hasAttachedStation(null))
                .isFalse();
        }
    }

    @Nested
    class IsAbandonedStation {

        @Test
        void returnsTrueForAMarketCarryingTheAbandonedStationCondition() {
            assertThat(Markets.isAbandonedStation(MarketStateFixture.buildAbandonedStation()))
                .isTrue();
        }

        @Test
        void returnsFalseForAColony() {
            assertThat(Markets.isAbandonedStation(MarketStateFixture.buildColony("hegemony")))
                .isFalse();
        }

        @Test
        void returnsFalseForADecivilisedWorld() {
            // The other unlisted neutral market a condition marks. Both are derelict in the
            // plain sense, and only the condition read tells the ruins from the hulk.
            assertThat(Markets.isAbandonedStation(MarketStateFixture.buildDecivilisedWorld()))
                .isFalse();
        }

        @Test
        void returnsFalseForANullMarket() {
            assertThat(Markets.isAbandonedStation(null))
                .isFalse();
        }
    }

    @Nested
    class IsMilitary {

        @BeforeEach
        void setUp() {
            // Misc's static initialiser reads Global.getSettings(), so the no-op proxy
            // must be installed before the delegating read loads the class.
            StarsectorSettingsFake.installSettings();
        }

        @AfterEach
        void tearDown() {
            StarsectorSettingsFake.clearSettings();
        }

        @Test
        void returnsTrueWhenTheMilitaryFlagIsSet() {

            var market = buildMarketWithMilitaryFlag(true);

            assertThat(Markets.isMilitary(market))
                .isTrue();
        }

        @Test
        void returnsFalseWhenTheMilitaryFlagIsUnset() {

            var market = buildMarketWithMilitaryFlag(false);

            assertThat(Markets.isMilitary(market))
                .isFalse();
        }

        @Test
        void returnsFalseForAMarketWithNoMemory() {
            // Vanilla's own read would throw here; the neighbouring flag reads absorb it, so
            // this one does too rather than being the single read a caller must defend.
            var marketMock = mock(MarketAPI.class);

            when(marketMock.getMemoryWithoutUpdate())
                .thenReturn(null);

            assertThat(Markets.isMilitary(marketMock))
                .isFalse();
        }

        @Test
        void returnsFalseForANullMarket() {
            assertThat(Markets.isMilitary(null))
                .isFalse();
        }
    }

    // The colonies here answer a faction id rather than only a faction object, which is what this
    // read asks for and what an ownership change writes - so they are MarketOwnershipFixture's.
    @Nested
    class IsOwnedBy {

        @Test
        void readsAColonyAsHeldByTheFactionWhoseFlagItFlies() {
            assertThat(Markets.isOwnedBy(
                    MarketOwnershipFixture.buildColonyHeldBy("hegemony"),
                    "hegemony"))
                .isTrue();
        }

        @Test
        void readsAColonyAsNotHeldByAnotherFaction() {
            assertThat(Markets.isOwnedBy(
                    MarketOwnershipFixture.buildColonyHeldBy("hegemony"),
                    "tritachyon"))
                .isFalse();
        }

        @Test
        void readsAColonyAsHeldByNobodyWhenNoFactionIsNamed() {
            // Asking whether a colony is held by nobody is not a question about its owner, and an
            // operation guarding on this must not read a missing id as a match.
            assertThat(Markets.isOwnedBy(
                    MarketOwnershipFixture.buildColonyHeldBy("hegemony"),
                    null))
                .isFalse();
        }

        @Test
        void readsANullMarketAsHeldByNobody() {
            assertThat(Markets.isOwnedBy(null, "hegemony"))
                .isFalse();
        }
    }

    @Nested
    class IsOwnedColony {

        @Test
        void returnsTrueForAFactionOwnedNonConditionMarket() {
            assertThat(Markets.isOwnedColony(MarketStateFixture.buildColony("hegemony")))
                .isTrue();
        }

        @Test
        void returnsFalseForAConditionOnlyMarket() {
            assertThat(Markets.isOwnedColony(MarketStateFixture.buildColonisableBody()))
                .isFalse();
        }

        @Test
        void returnsFalseWhenNoFactionOwnsTheMarket() {
            assertThat(Markets.isOwnedColony(MarketStateFixture.buildUnownedMarket()))
                .isFalse();
        }

        @Test
        void returnsFalseForANullMarket() {
            assertThat(Markets.isOwnedColony(null))
                .isFalse();
        }
    }

    @Nested
    class IsSettledColony {

        @Test
        void returnsTrueForAColonyAFactionHolds() {
            assertThat(Markets.isSettledColony(MarketStateFixture.buildColony("hegemony")))
                .isTrue();
        }

        @Test
        void returnsTrueForAColonyTheEconomyDoesNotList() {
            // Galatia Academy is settled by anyone's reckoning; registration is a separate
            // question and no part of this one.
            assertThat(Markets.isSettledColony(
                    MarketStateFixture.buildColonyUnlistedByEconomy("independent")))
                .isTrue();
        }

        @Test
        void returnsFalseForADerelictStationFlyingTheNeutralFlag() {
            // The case the ownership read alone gets wrong: a hulk has a faction like any other
            // market, and it is neutral - which is the game saying nobody lives here.
            assertThat(Markets.isSettledColony(MarketStateFixture.buildAbandonedStation()))
                .isFalse();
        }

        @Test
        void returnsFalseForABareWorldsPlaceholder() {
            assertThat(Markets.isSettledColony(MarketStateFixture.buildColonisableBody()))
                .isFalse();
        }

        @Test
        void returnsFalseForADecivilisedWorld() {
            // What is left where a colony was is not a colony: neutral holds it, and the market
            // is back to carrying the planet's conditions and nothing else.
            assertThat(Markets.isSettledColony(MarketStateFixture.buildDecivilisedWorld()))
                .isFalse();
        }

        @Test
        void returnsFalseWhenNoFactionHoldsTheMarket() {
            assertThat(Markets.isSettledColony(MarketStateFixture.buildUnownedMarket()))
                .isFalse();
        }

        @Test
        void returnsFalseForANullMarket() {
            assertThat(Markets.isSettledColony(null))
                .isFalse();
        }
    }

    @Nested
    class ReadNameplate {

        @Test
        void readsTheMarketSOwnNameWithItsPrimaryEntitySGlyph() {
            // The pairing is the whole of the read: a market is named in its own right while the
            // glyph belongs to the entity it sits on, and joining the two here is what stops a
            // surface pairing one colony's name with another's mark.
            // The entity is built before the market's own stubbing opens, so the two do not nest
            // into an unfinished-stubbing error.
            var entity = buildEntityWithMapIcon(
                "graphics/icons/station0.png",
                new Color(200, 200, 255));

            var market = buildMarketNamed("Ancyra");

            when(market.getPrimaryEntity())
                .thenReturn(entity);

            assertThat(Markets.readNameplate(market))
                .isEqualTo(new EntityNameplate(
                    "Ancyra",
                    Optional.of(new EntityMapIcon(
                        "graphics/icons/station0.png",
                        new Color(200, 200, 255)))));
        }

        @Test
        void namesAMarketWhoseEntityCarriesNoGlyph() {

            var entityMock = mock(SectorEntityToken.class);
            var market = buildMarketNamed("Jangala");

            when(market.getPrimaryEntity())
                .thenReturn(entityMock);

            assertThat(Markets.readNameplate(market))
                .isEqualTo(EntityNameplate.createUnmarkedNameplate("Jangala"));
        }

        @Test
        void readsAMarketWithNoPrimaryEntityAsNamedAndUnmarked() {
            // A market the game has not sited yet answers no entity, which is the absence the icon
            // read already handles - the name still identifies it.
            var market = buildMarketNamed("Kazeron");

            when(market.getPrimaryEntity())
                .thenReturn(null);

            assertThat(Markets.readNameplate(market))
                .isEqualTo(EntityNameplate.createUnmarkedNameplate("Kazeron"));
        }

        @Test
        void readsANullMarketAsBlankAndUnmarked() {
            assertThat(Markets.readNameplate(null))
                .isEqualTo(EntityNameplate.BLANK);
        }
    }

    // The colonies here are MarketOwnershipFixture's rather than this suite's own builders: what
    // is being read is a counter and the plugin behind it, and that fixture is where this package
    // states what a colony's counters look like - including which of them are kept by a plugin
    // worth speaking to.
    @Nested
    class ReadSubmarketPlugin {

        @Test
        void readsThePluginKeepingTheCounter() {

            var market = MarketOwnershipFixture.buildColonyTradingThrough(
                "player",
                "storage");

            assertThat(Markets.readSubmarketPlugin(market, "storage", StoragePlugin.class))
                .isSameAs(market.getSubmarket("storage").getPlugin());
        }

        @Test
        void readsNothingWhereTheCounterIsKeptByAnotherKindOfPlugin() {
            // The case the kind is asked for at all: a mod may put its own plugin behind a counter
            // vanilla defines, and an operation that cast it outright would fail on that install
            // rather than pass the counter over.
            var market = MarketOwnershipFixture.buildColonyTradingThrough(
                "player",
                "local_resources");

            assertThat(Markets.readSubmarketPlugin(market, "local_resources", StoragePlugin.class))
                .isNull();
        }

        @Test
        void readsNothingWhereTheMarketKeepsNoSuchCounter() {

            var market = MarketOwnershipFixture.buildColonyHeldBy("hegemony");

            assertThat(Markets.readSubmarketPlugin(market, "storage", StoragePlugin.class))
                .isNull();
        }

        @Test
        void readsNothingForANullMarket() {
            assertThat(Markets.readSubmarketPlugin(null, "storage", StoragePlugin.class))
                .isNull();
        }
    }

    // An entity the map marks with the given glyph, authored on a custom-entity spec - where
    // vanilla keeps a station's icon, and the arm the identity read reaches for a non-planet.
    private static SectorEntityToken buildEntityWithMapIcon(String iconName, Color iconColour) {

        var entityMock = mock(SectorEntityToken.class);
        var entitySpecMock = mock(CustomEntitySpecAPI.class);

        when(entityMock.getCustomEntitySpec())
            .thenReturn(entitySpecMock);
        when(entitySpecMock.getIconName())
            .thenReturn(iconName);
        when(entitySpecMock.getIconColor())
            .thenReturn(iconColour);

        return entityMock;
    }

    // A "station"-tagged entity with no station fleet in memory - the shape a market's own
    // primary entity takes when the place is itself a station. It says what the entity is,
    // not that it defends anything, so the scan must pass over it.
    private static SectorEntityToken buildFleetlessStationEntity() {
        return buildStationTaggedEntity(null);
    }

    private static MarketAPI buildMarketAtStability(float stability) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getStabilityValue())
            .thenReturn(stability);

        return marketMock;
    }

    private static MarketAPI buildMarketConnectedTo(SectorEntityToken... entities) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getConnectedEntities())
            .thenReturn(Set.of(entities));

        return marketMock;
    }

    // A market answering only its display name, which is the one half of an identity the market
    // itself supplies - the other belongs to whatever entity a case goes on to site it on.
    private static MarketAPI buildMarketNamed(String name) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getName())
            .thenReturn(name);

        return marketMock;
    }

    // A market whose memory carries the $military flag at the given value - the signal a
    // military industry raises, and the one vanilla's own classification reads.
    private static MarketAPI buildMarketWithMilitaryFlag(boolean isMilitary) {

        var memoryMock = mock(MemoryAPI.class);

        when(memoryMock.getBoolean(MemFlags.MARKET_MILITARY))
            .thenReturn(isMilitary);

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getMemoryWithoutUpdate())
            .thenReturn(memoryMock);

        return marketMock;
    }

    // A connected entity that is not a station (e.g. the market's planet).
    private static SectorEntityToken buildNonStationEntity() {
        return mock(SectorEntityToken.class);
    }

    // A "station"-tagged entity flagged NO_ORBITAL_STATION, vanilla's own opt-out.
    private static SectorEntityToken buildOptedOutStationEntity() {

        var entityMock = buildStationEntity();

        when(entityMock.hasTag("NO_ORBITAL_STATION"))
            .thenReturn(true);

        return entityMock;
    }

    // A station entity: carries the "station" tag, no opt-out, and the station fleet an
    // orbital-station industry raises - the shape the scan admits.
    private static SectorEntityToken buildStationEntity() {
        return buildStationTaggedEntity(mock(CampaignFleetAPI.class));
    }

    // The shared wiring behind the two station shapes: the tag, plus whatever the entity's
    // memory answers for the station-fleet key. Vanilla's fleet read dereferences that memory
    // unconditionally, so it is always stubbed even where the fleet itself is absent.
    private static SectorEntityToken buildStationTaggedEntity(CampaignFleetAPI stationFleet) {

        var memoryMock = mock(MemoryAPI.class);

        when(memoryMock.get(MemFlags.STATION_FLEET))
            .thenReturn(stationFleet);

        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.hasTag(Tags.STATION))
            .thenReturn(true);
        when(entityMock.getMemoryWithoutUpdate())
            .thenReturn(memoryMock);

        return entityMock;
    }
}
