package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.StarSystemAPI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Pins the contracts of {@link SystemAccessRoutes#registerRoute},
 * {@link SystemAccessRoutes#readRouteNames}, {@link SystemAccessRoutes#clearRoutes} and
 * {@link SystemAccessRoutes#isReachedByAnyRoute}.
 *
 * <p>What is load-bearing here is that the set accumulates. A single-slot point would keep only
 * the last registered, which on an install running two mods that each add a way in would silently
 * drop one of them - so the two-route cases are asserted rather than assumed. The keying is the
 * other half: an integration composing itself twice is one route, not two.
 *
 * <p>The routes are one set per running game, so every case empties them before and after itself.
 * Each method's cases live in a {@link Nested} group so the suite reports as a per-method tree.
 */
final class SystemAccessRoutesTest {

    private static final SystemAccessRoute DECLINING_ROUTE = anySystem -> false;
    private static final SystemAccessRoute GRANTING_ROUTE = anySystem -> true;

    @BeforeEach
    void setUp() {
        SystemAccessRoutes.clearRoutes();
    }

    @AfterEach
    void tearDown() {
        SystemAccessRoutes.clearRoutes();
    }

    @Nested
    class RegisterRoute {

        @Test
        void keeps_every_route_registered_under_a_distinct_name() {
            // The difference from a single-slot extension point: a second mod adding a way in
            // must not displace the first mod's.
            SystemAccessRoutes.registerRoute("first mod", DECLINING_ROUTE);
            SystemAccessRoutes.registerRoute("second mod", DECLINING_ROUTE);

            assertThat(SystemAccessRoutes.readRouteNames())
                .containsExactly("first mod", "second mod");
        }

        @Test
        void replaces_a_route_registered_again_under_the_same_name() {
            // An integration composing itself twice - a reload, a settings save that re-runs the
            // composition - is one way in rather than two identical ones stacked.
            SystemAccessRoutes.registerRoute("a mod", DECLINING_ROUTE);
            SystemAccessRoutes.registerRoute("a mod", GRANTING_ROUTE);

            assertThat(SystemAccessRoutes.readRouteNames())
                .containsExactly("a mod");
            assertThat(SystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class)))
                .isTrue();
        }

        @Test
        void passes_over_a_null_route() {
            // An absent integration is a state to leave alone. Registering nothing must not
            // disturb what another mod did install, so the standing route survives.
            SystemAccessRoutes.registerRoute("a mod", GRANTING_ROUTE);
            SystemAccessRoutes.registerRoute("an absent mod", null);

            assertThat(SystemAccessRoutes.readRouteNames())
                .containsExactly("a mod");
        }

        @Test
        void records_an_unnamed_route_under_its_own_type() {
            // A blank name still has to leave a line worth reading, so it falls back to the
            // route's class rather than being refused or recorded as nothing at all.
            SystemAccessRoutes.registerRoute("  ", GRANTING_ROUTE);

            assertThat(SystemAccessRoutes.readRouteNames())
                .hasSize(1);
            assertThat(SystemAccessRoutes.readRouteNames().get(0))
                .contains(GRANTING_ROUTE.getClass().getName());
        }
    }

    @Nested
    class ReadRouteNames {

        @Test
        void returns_empty_on_an_install_that_registered_none() {
            assertThat(SystemAccessRoutes.readRouteNames())
                .isEmpty();
        }

        @Test
        void returns_the_names_in_the_order_they_were_registered() {
            SystemAccessRoutes.registerRoute("second mod", DECLINING_ROUTE);
            SystemAccessRoutes.registerRoute("first mod", DECLINING_ROUTE);

            assertThat(SystemAccessRoutes.readRouteNames())
                .containsExactly("second mod", "first mod");
        }
    }

    @Nested
    class ClearRoutes {

        @Test
        void empties_the_set_so_an_install_can_be_composed_again_from_nothing() {
            SystemAccessRoutes.registerRoute("a mod", GRANTING_ROUTE);

            SystemAccessRoutes.clearRoutes();

            assertThat(SystemAccessRoutes.readRouteNames())
                .isEmpty();
            assertThat(SystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class)))
                .isFalse();
        }
    }

    @Nested
    class IsReachedByAnyRoute {

        @Test
        void returns_false_on_an_install_that_registered_none() {
            assertThat(SystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class)))
                .isFalse();
        }

        @Test
        void returns_false_for_a_null_system() {
            SystemAccessRoutes.registerRoute("a mod", GRANTING_ROUTE);

            assertThat(SystemAccessRoutes.isReachedByAnyRoute(null))
                .isFalse();
        }

        @Test
        void returns_false_when_every_route_declines() {
            SystemAccessRoutes.registerRoute("first mod", DECLINING_ROUTE);
            SystemAccessRoutes.registerRoute("second mod", DECLINING_ROUTE);

            assertThat(SystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class)))
                .isFalse();
        }

        @Test
        void returns_true_when_one_route_of_several_grants_access() {
            // One route answering false leaves the question where it found it, so the granting
            // route behind it still decides the answer.
            SystemAccessRoutes.registerRoute("first mod", DECLINING_ROUTE);
            SystemAccessRoutes.registerRoute("second mod", GRANTING_ROUTE);

            assertThat(SystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class)))
                .isTrue();
        }

        @Test
        void hands_the_system_asked_about_to_the_route() {
            // A route decides per system, so it has to be given the one being asked about rather
            // than being consulted as a standing yes-or-no about the install.
            var reachableSystemMock = mock(StarSystemAPI.class);
            var unreachableSystemMock = mock(StarSystemAPI.class);

            SystemAccessRoutes.registerRoute(
                "a mod",
                askedSystem -> askedSystem == reachableSystemMock);

            assertThat(SystemAccessRoutes.isReachedByAnyRoute(reachableSystemMock))
                .isTrue();
            assertThat(SystemAccessRoutes.isReachedByAnyRoute(unreachableSystemMock))
                .isFalse();
        }
    }
}
