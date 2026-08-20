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
        void yields_the_station_a_market_owns() {

            var station = buildStationEntity();
            var market = buildMarketConnectedTo(station);

            assertThat(Markets.findAttachedStation(market))
                .contains(station);
        }

        @Test
        void picks_the_station_out_of_the_market_s_other_connected_entities() {

            var station = buildStationEntity();
            var market = buildMarketConnectedTo(buildNonStationEntity(), station);

            assertThat(Markets.findAttachedStation(market))
                .contains(station);
        }

        @Test
        void yields_empty_for_a_market_with_no_connected_entities() {

            var market = buildMarketConnectedTo();

            assertThat(Markets.findAttachedStation(market))
                .isEmpty();
        }

        @Test
        void yields_empty_when_no_connected_entity_is_a_station() {

            var market = buildMarketConnectedTo(buildNonStationEntity());

            assertThat(Markets.findAttachedStation(market))
                .isEmpty();
        }

        @Test
        void yields_empty_for_a_station_tagged_entity_raising_no_station_fleet() {
            // The tag alone says "this is a station", not "this defends the market". The
            // fleet an orbital-station industry raises is what says the second.
            var market = buildMarketConnectedTo(buildFleetlessStationEntity());

            assertThat(Markets.findAttachedStation(market))
                .isEmpty();
        }

        @Test
        void picks_the_fleeted_station_over_a_fleetless_station_tagged_entity() {
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
        void ignores_a_station_tagged_no_orbital_station() {

            var market = buildMarketConnectedTo(buildOptedOutStationEntity());

            assertThat(Markets.findAttachedStation(market))
                .isEmpty();
        }

        @Test
        void yields_empty_for_a_null_market() {
            assertThat(Markets.findAttachedStation(null))
                .isEmpty();
        }

        @Test
        void yields_empty_for_null_connected_entities() {

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
        void full_stability_is_one() {
            assertThat(Markets.getStabilityFraction(buildMarketAtStability(10.0f)))
                .isEqualTo(1.0);
        }

        @Test
        void half_stability_is_a_half() {
            assertThat(Markets.getStabilityFraction(buildMarketAtStability(5.0f)))
                .isEqualTo(0.5);
        }

        @Test
        void no_stability_is_zero() {
            assertThat(Markets.getStabilityFraction(buildMarketAtStability(0.0f)))
                .isEqualTo(0.0);
        }

        @Test
        void above_band_clamps_to_one() {
            assertThat(Markets.getStabilityFraction(buildMarketAtStability(12.0f)))
                .isEqualTo(1.0);
        }

        @Test
        void below_band_clamps_to_zero() {
            assertThat(Markets.getStabilityFraction(buildMarketAtStability(-3.0f)))
                .isEqualTo(0.0);
        }

        @Test
        void null_market_is_zero() {
            assertThat(Markets.getStabilityFraction(null))
                .isEqualTo(0.0);
        }
    }

    // The presence verdict is the entity read taken as a boolean, so this group pins that
    // pairing - a found station reads true, an unfound one false - plus the null contract
    // its own Javadoc states. The scan's edge cases (the NO_ORBITAL_STATION opt-out, absent
    // or null connected entities) belong to the read that runs the scan, above.
    @Nested
    class HasAttachedStation {

        @Test
        void returns_true_for_a_market_that_owns_a_station() {

            var market = buildMarketConnectedTo(buildStationEntity());

            assertThat(Markets.hasAttachedStation(market))
                .isTrue();
        }

        @Test
        void returns_false_when_no_connected_entity_is_a_station() {

            var market = buildMarketConnectedTo(buildNonStationEntity());

            assertThat(Markets.hasAttachedStation(market))
                .isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(Markets.hasAttachedStation(null))
                .isFalse();
        }
    }

    @Nested
    class IsAbandonedStation {

        @Test
        void returns_true_for_a_market_carrying_the_abandoned_station_condition() {
            assertThat(Markets.isAbandonedStation(MarketStateFixture.buildAbandonedStation()))
                .isTrue();
        }

        @Test
        void returns_false_for_a_colony() {
            assertThat(Markets.isAbandonedStation(MarketStateFixture.buildColony("hegemony")))
                .isFalse();
        }

        @Test
        void returns_false_for_a_decivilised_world() {
            // The other unlisted neutral market a condition marks. Both are derelict in the
            // plain sense, and only the condition read tells the ruins from the hulk.
            assertThat(Markets.isAbandonedStation(MarketStateFixture.buildDecivilisedWorld()))
                .isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
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
        void returns_true_when_the_military_flag_is_set() {

            var market = buildMarketWithMilitaryFlag(true);

            assertThat(Markets.isMilitary(market))
                .isTrue();
        }

        @Test
        void returns_false_when_the_military_flag_is_unset() {

            var market = buildMarketWithMilitaryFlag(false);

            assertThat(Markets.isMilitary(market))
                .isFalse();
        }

        @Test
        void returns_false_for_a_market_with_no_memory() {
            // Vanilla's own read would throw here; the neighbouring flag reads absorb it, so
            // this one does too rather than being the single read a caller must defend.
            var marketMock = mock(MarketAPI.class);

            when(marketMock.getMemoryWithoutUpdate())
                .thenReturn(null);

            assertThat(Markets.isMilitary(marketMock))
                .isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(Markets.isMilitary(null))
                .isFalse();
        }
    }

    // The colonies here answer a faction id rather than only a faction object, which is what this
    // read asks for and what an ownership change writes - so they are MarketOwnershipFixture's.
    @Nested
    class IsOwnedBy {

        @Test
        void reads_a_colony_as_held_by_the_faction_whose_flag_it_flies() {
            assertThat(Markets.isOwnedBy(
                    MarketOwnershipFixture.buildColonyHeldBy("hegemony"),
                    "hegemony"))
                .isTrue();
        }

        @Test
        void reads_a_colony_as_not_held_by_another_faction() {
            assertThat(Markets.isOwnedBy(
                    MarketOwnershipFixture.buildColonyHeldBy("hegemony"),
                    "tritachyon"))
                .isFalse();
        }

        @Test
        void reads_a_colony_as_held_by_nobody_when_no_faction_is_named() {
            // Asking whether a colony is held by nobody is not a question about its owner, and an
            // operation guarding on this must not read a missing id as a match.
            assertThat(Markets.isOwnedBy(
                    MarketOwnershipFixture.buildColonyHeldBy("hegemony"),
                    null))
                .isFalse();
        }

        @Test
        void reads_a_null_market_as_held_by_nobody() {
            assertThat(Markets.isOwnedBy(null, "hegemony"))
                .isFalse();
        }
    }

    @Nested
    class IsOwnedColony {

        @Test
        void returns_true_for_a_faction_owned_non_condition_market() {
            assertThat(Markets.isOwnedColony(MarketStateFixture.buildColony("hegemony")))
                .isTrue();
        }

        @Test
        void returns_false_for_a_condition_only_market() {
            assertThat(Markets.isOwnedColony(MarketStateFixture.buildColonisableBody()))
                .isFalse();
        }

        @Test
        void returns_false_when_no_faction_owns_the_market() {
            assertThat(Markets.isOwnedColony(MarketStateFixture.buildUnownedMarket()))
                .isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(Markets.isOwnedColony(null))
                .isFalse();
        }
    }

    @Nested
    class IsSettledColony {

        @Test
        void returns_true_for_a_colony_a_faction_holds() {
            assertThat(Markets.isSettledColony(MarketStateFixture.buildColony("hegemony")))
                .isTrue();
        }

        @Test
        void returns_true_for_a_colony_the_economy_does_not_list() {
            // Galatia Academy is settled by anyone's reckoning; registration is a separate
            // question and no part of this one.
            assertThat(Markets.isSettledColony(
                    MarketStateFixture.buildColonyUnlistedByEconomy("independent")))
                .isTrue();
        }

        @Test
        void returns_false_for_a_derelict_station_flying_the_neutral_flag() {
            // The case the ownership read alone gets wrong: a hulk has a faction like any other
            // market, and it is neutral - which is the game saying nobody lives here.
            assertThat(Markets.isSettledColony(MarketStateFixture.buildAbandonedStation()))
                .isFalse();
        }

        @Test
        void returns_false_for_a_bare_worlds_placeholder() {
            assertThat(Markets.isSettledColony(MarketStateFixture.buildColonisableBody()))
                .isFalse();
        }

        @Test
        void returns_false_for_a_decivilised_world() {
            // What is left where a colony was is not a colony: neutral holds it, and the market
            // is back to carrying the planet's conditions and nothing else.
            assertThat(Markets.isSettledColony(MarketStateFixture.buildDecivilisedWorld()))
                .isFalse();
        }

        @Test
        void returns_false_when_no_faction_holds_the_market() {
            assertThat(Markets.isSettledColony(MarketStateFixture.buildUnownedMarket()))
                .isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(Markets.isSettledColony(null))
                .isFalse();
        }
    }

    @Nested
    class ReadNameplate {

        @Test
        void reads_the_market_s_own_name_with_its_primary_entity_s_glyph() {
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
        void names_a_market_whose_entity_carries_no_glyph() {

            var entityMock = mock(SectorEntityToken.class);
            var market = buildMarketNamed("Jangala");

            when(market.getPrimaryEntity())
                .thenReturn(entityMock);

            assertThat(Markets.readNameplate(market))
                .isEqualTo(EntityNameplate.createUnmarkedNameplate("Jangala"));
        }

        @Test
        void reads_a_market_with_no_primary_entity_as_named_and_unmarked() {
            // A market the game has not sited yet answers no entity, which is the absence the icon
            // read already handles - the name still identifies it.
            var market = buildMarketNamed("Kazeron");

            when(market.getPrimaryEntity())
                .thenReturn(null);

            assertThat(Markets.readNameplate(market))
                .isEqualTo(EntityNameplate.createUnmarkedNameplate("Kazeron"));
        }

        @Test
        void reads_a_null_market_as_blank_and_unmarked() {
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
        void reads_the_plugin_keeping_the_counter() {

            var market = MarketOwnershipFixture.buildColonyTradingThrough(
                "player",
                "storage");

            assertThat(Markets.readSubmarketPlugin(market, "storage", StoragePlugin.class))
                .isSameAs(market.getSubmarket("storage").getPlugin());
        }

        @Test
        void reads_nothing_where_the_counter_is_kept_by_another_kind_of_plugin() {
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
        void reads_nothing_where_the_market_keeps_no_such_counter() {

            var market = MarketOwnershipFixture.buildColonyHeldBy("hegemony");

            assertThat(Markets.readSubmarketPlugin(market, "storage", StoragePlugin.class))
                .isNull();
        }

        @Test
        void reads_nothing_for_a_null_market() {
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
