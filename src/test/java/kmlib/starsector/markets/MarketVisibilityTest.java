package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link MarketVisibility#isCountedAsColony},
 * {@link MarketVisibility#isDiscoveredByPlayer} and
 * {@link MarketVisibility#isKnownToPlayer}. The cases live in a {@link Nested} group per
 * method so the suite reports as a per-method tree; the shared mock builders stay on the
 * outer class.
 *
 * <p>The colony shapes are named for what they are rather than posed by flag, and share
 * their vocabulary with {@code ColonyMarketFixture} - a case reading "unfound open colony"
 * means the same thing in both packages, which is what lets a reader carry one mental model
 * across the fog and the projections over it.
 */
final class MarketVisibilityTest {

    @Nested
    class IsCountedAsColony {

        @Test
        void returns_true_for_a_known_owned_colony() {

            var market = buildVisibleColony();

            assertThat(MarketVisibility.isCountedAsColony(market, false))
                .isTrue();
        }

        @Test
        void returns_false_for_a_condition_only_market() {

            var market = buildConditionOnlyMarket();

            assertThat(MarketVisibility.isCountedAsColony(market, false))
                .isFalse();
        }

        @Test
        void returns_false_for_an_undiscovered_concealed_colony() {

            var market = buildUnfoundConcealedColony();

            assertThat(MarketVisibility.isCountedAsColony(market, false))
                .isFalse();
        }

        @Test
        void returns_true_for_an_undiscovered_concealed_colony_when_including_undiscovered() {

            var market = buildUnfoundConcealedColony();

            assertThat(MarketVisibility.isCountedAsColony(market, true))
                .isTrue();
        }

        @Test
        void returns_false_for_an_un_hidden_colony_whose_entity_is_undiscovered() {
            // The composed filter inherits the fog's own reading, so a colony that is merely
            // listed does not reach a surface that reports its system as settled before the
            // player has been anywhere near it.
            var market = buildUnfoundOpenColony();

            assertThat(MarketVisibility.isCountedAsColony(market, false))
                .isFalse();
        }

        @Test
        void returns_true_for_a_concealed_colony_the_player_has_found() {
            // A raided pirate base stays hidden for good, and stays known for good with it -
            // concealment is not what the fog reads.
            var market = buildFoundConcealedColony();

            assertThat(MarketVisibility.isCountedAsColony(market, false))
                .isTrue();
        }

        @Test
        void returns_true_for_a_colony_with_no_entity_to_find() {

            var market = buildEntitylessColony();

            assertThat(MarketVisibility.isCountedAsColony(market, false))
                .isTrue();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(MarketVisibility.isCountedAsColony(null, true))
                .isFalse();
        }
    }

    @Nested
    class IsDiscoveredByPlayer {

        @Test
        void returns_true_when_the_entity_is_discovered() {

            var market = buildFoundConcealedColony();

            assertThat(MarketVisibility.isDiscoveredByPlayer(market))
                .isTrue();
        }

        @Test
        void returns_true_when_the_primary_entity_is_null() {

            var market = buildEntitylessColony();

            assertThat(MarketVisibility.isDiscoveredByPlayer(market))
                .isTrue();
        }

        @Test
        void returns_false_when_the_entity_is_undiscovered_though_the_market_is_un_hidden() {
            // The two axes pulled apart: being publicly listed is not having been there, so an
            // un-hidden market whose entity is still to be found reads undiscovered.
            var market = buildUnfoundOpenColony();

            assertThat(MarketVisibility.isDiscoveredByPlayer(market))
                .isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(MarketVisibility.isDiscoveredByPlayer(null))
                .isFalse();
        }
    }

    @Nested
    class IsKnownToPlayer {

        @Test
        void returns_true_when_the_entity_is_discovered() {

            var market = buildFoundConcealedColony();

            assertThat(MarketVisibility.isKnownToPlayer(market))
                .isTrue();
        }

        @Test
        void returns_false_when_the_market_is_un_hidden_but_the_entity_is_still_discoverable() {
            // The whole of the leak this fog closes. A market that merely omits to hide itself
            // is listed in an economy the player has no sight of, and every derelict station in
            // the sector takes that shape - so listing is not an arm of the rule.
            var market = buildUnfoundOpenColony();

            assertThat(MarketVisibility.isKnownToPlayer(market))
                .isFalse();
        }

        @Test
        void returns_true_when_the_primary_entity_is_null() {

            var market = buildEntitylessColony();

            assertThat(MarketVisibility.isKnownToPlayer(market))
                .isTrue();
        }

        @Test
        void returns_false_when_the_market_is_hidden_on_a_discoverable_entity() {

            var market = buildUnfoundConcealedColony();

            assertThat(MarketVisibility.isKnownToPlayer(market))
                .isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(MarketVisibility.isKnownToPlayer(null))
                .isFalse();
        }
    }

    // A bare planet's placeholder, the condition-only market every uninhabited world carries to
    // hold its hazard and atmosphere. Owned, and rejected on the ownership arm all the same.
    private static MarketAPI buildConditionOnlyMarket() {
        return buildColonyMarket(true, false, buildFoundEntity());
    }

    // An owned colony with no entity at all: nothing is left to find, so the fog has nothing to
    // withhold.
    private static MarketAPI buildEntitylessColony() {
        return buildColonyMarket(false, false, null);
    }

    // A raided pirate base: permanently hidden, on an entity the player has found. Concealment
    // and the fog disagree here, and the fog is what these reads answer on.
    private static MarketAPI buildFoundConcealedColony() {
        return buildColonyMarket(false, true, buildFoundEntity());
    }

    // A base still to be found: concealed, and on an entity the player has not discovered.
    // Concealment and discovery agree here, so nothing whatever about it reaches the player.
    private static MarketAPI buildUnfoundConcealedColony() {
        return buildColonyMarket(false, true, buildUnfoundEntity());
    }

    // A market that declares itself to the economy while its entity is still to be found - the
    // shape every derelict station in the sector takes, and the one the fog withholds.
    private static MarketAPI buildUnfoundOpenColony() {
        return buildColonyMarket(false, false, buildUnfoundEntity());
    }

    // An ordinary colony: publicly listed, on an entity the player has found. The plain case
    // every one of these reads admits.
    private static MarketAPI buildVisibleColony() {
        return buildColonyMarket(false, false, buildFoundEntity());
    }

    // An owned colony wired for both arms the composed filter runs on: ownership (a faction owns
    // it, and whether it is the condition-only placeholder) and visibility (the market's hidden
    // flag, and the entity it is sited on). Reached through the named builders above - two
    // adjacent booleans say nothing at a call site, and are transposable without failing.
    //
    // The entity is passed in rather than posed by a third boolean, so the one builder covers a
    // market on a found entity, on an unfound one, and on none at all.
    private static MarketAPI buildColonyMarket(
            boolean isConditionOnly,
            boolean isHidden,
            SectorEntityToken entity) {

        // The faction finishes its own stubbing before the market's opens, so the two do not
        // nest into an unfinished-stubbing error.
        var factionMock = buildFaction();
        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(factionMock);
        when(marketMock.isPlanetConditionMarketOnly())
            .thenReturn(isConditionOnly);
        when(marketMock.isHidden())
            .thenReturn(isHidden);
        when(marketMock.getPrimaryEntity())
            .thenReturn(entity);

        return marketMock;
    }

    private static FactionAPI buildFaction() {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn("hegemony");

        return factionMock;
    }

    // An entity the player has already found: no longer flagged discoverable.
    private static SectorEntityToken buildFoundEntity() {
        return buildEntity(false);
    }

    // An entity still awaiting physical discovery: flagged discoverable.
    private static SectorEntityToken buildUnfoundEntity() {
        return buildEntity(true);
    }

    private static SectorEntityToken buildEntity(boolean isDiscoverable) {

        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.isDiscoverable())
            .thenReturn(isDiscoverable);

        return entityMock;
    }
}
