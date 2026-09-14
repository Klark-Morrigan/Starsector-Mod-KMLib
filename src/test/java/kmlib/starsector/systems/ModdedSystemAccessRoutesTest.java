package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.StarSystemAPI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Pins the contracts of {@link ModdedSystemAccessRoutes#registerRoute},
 * {@link ModdedSystemAccessRoutes#readRouteNames}, {@link ModdedSystemAccessRoutes#clearRoutes} and
 * {@link ModdedSystemAccessRoutes#isReachedByAnyRoute}.
 *
 * <p>What is load-bearing here is that the set accumulates. A single-slot point would keep only
 * the last registered, which on an install running two mods that each add a way in would silently
 * drop one of them - so the two-route cases are asserted rather than assumed. The keying is the
 * other half: an integration composing itself twice is one route, not two.
 *
 * <p>The routes are one set per running game, so every case empties them before and after itself.
 * Each method's cases live in a {@link Nested} group so the suite reports as a per-method tree.
 */
final class ModdedSystemAccessRoutesTest {

    private static final ModdedSystemAccessRoute DECLINING_ROUTE = anySystem -> false;
    private static final ModdedSystemAccessRoute GRANTING_ROUTE = anySystem -> true;

    @BeforeEach
    void setUp() {
        ModdedSystemAccessRoutes.clearRoutes();
    }

    @AfterEach
    void tearDown() {
        ModdedSystemAccessRoutes.clearRoutes();
    }

    @Nested
    class RegisterRoute {

        @Test
        void keepsEveryRouteRegisteredUnderADistinctName() {
            // The difference from a single-slot extension point: a second mod adding a way in
            // must not displace the first mod's.
            ModdedSystemAccessRoutes.registerRoute("first mod", DECLINING_ROUTE);
            ModdedSystemAccessRoutes.registerRoute("second mod", DECLINING_ROUTE);

            assertThat(ModdedSystemAccessRoutes.readRouteNames())
                .containsExactly("first mod", "second mod");
        }

        @Test
        void replacesARouteRegisteredAgainUnderTheSameName() {
            // An integration composing itself twice - a reload, a settings save that re-runs the
            // composition - is one way in rather than two identical ones stacked.
            ModdedSystemAccessRoutes.registerRoute("a mod", DECLINING_ROUTE);
            ModdedSystemAccessRoutes.registerRoute("a mod", GRANTING_ROUTE);

            assertThat(ModdedSystemAccessRoutes.readRouteNames())
                .containsExactly("a mod");
            assertThat(ModdedSystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class)))
                .isTrue();
        }

        @Test
        void passesOverANullRoute() {
            // An absent integration is a state to leave alone. Registering nothing must not
            // disturb what another mod did install, so the standing route survives.
            ModdedSystemAccessRoutes.registerRoute("a mod", GRANTING_ROUTE);
            ModdedSystemAccessRoutes.registerRoute("an absent mod", null);

            assertThat(ModdedSystemAccessRoutes.readRouteNames())
                .containsExactly("a mod");
        }

    }

    @Nested
    class ReadRouteNames {

        @Test
        void returnsEmptyOnAnInstallThatRegisteredNone() {
            assertThat(ModdedSystemAccessRoutes.readRouteNames())
                .isEmpty();
        }

        @Test
        void returnsTheNamesInTheOrderTheyWereRegistered() {
            ModdedSystemAccessRoutes.registerRoute("second mod", DECLINING_ROUTE);
            ModdedSystemAccessRoutes.registerRoute("first mod", DECLINING_ROUTE);

            assertThat(ModdedSystemAccessRoutes.readRouteNames())
                .containsExactly("second mod", "first mod");
        }
    }

    @Nested
    class ClearRoutes {

        @Test
        void emptiesTheSetSoAnInstallCanBeComposedAgainFromNothing() {
            ModdedSystemAccessRoutes.registerRoute("a mod", GRANTING_ROUTE);

            ModdedSystemAccessRoutes.clearRoutes();

            assertThat(ModdedSystemAccessRoutes.readRouteNames())
                .isEmpty();
            assertThat(ModdedSystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class)))
                .isFalse();
        }
    }

    @Nested
    class IsReachedByAnyRoute {

        @Test
        void returnsFalseOnAnInstallThatRegisteredNone() {
            assertThat(ModdedSystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class)))
                .isFalse();
        }

        @Test
        void returnsFalseWhenEveryRouteDeclines() {
            ModdedSystemAccessRoutes.registerRoute("first mod", DECLINING_ROUTE);
            ModdedSystemAccessRoutes.registerRoute("second mod", DECLINING_ROUTE);

            assertThat(ModdedSystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class)))
                .isFalse();
        }

        @Test
        void returnsTrueWhenOneRouteOfSeveralGrantsAccess() {
            // One route answering false leaves the question where it found it, so the granting
            // route behind it still decides the answer.
            ModdedSystemAccessRoutes.registerRoute("first mod", DECLINING_ROUTE);
            ModdedSystemAccessRoutes.registerRoute("second mod", GRANTING_ROUTE);

            assertThat(ModdedSystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class)))
                .isTrue();
        }

        @Test
        void handsTheSystemAskedAboutToTheRoute() {
            // A route decides per system, so it has to be given the one being asked about rather
            // than being consulted as a standing yes-or-no about the install.
            var reachableSystemMock = mock(StarSystemAPI.class);
            var unreachableSystemMock = mock(StarSystemAPI.class);

            ModdedSystemAccessRoutes.registerRoute(
                "a mod",
                askedSystem -> askedSystem == reachableSystemMock);

            assertThat(ModdedSystemAccessRoutes.isReachedByAnyRoute(reachableSystemMock))
                .isTrue();
            assertThat(ModdedSystemAccessRoutes.isReachedByAnyRoute(unreachableSystemMock))
                .isFalse();
        }
    }
}
