package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
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
 * <p>The failure cases are the third. The read runs on every map refresh, so a route that throws
 * would take each one down; it has to be taken out, reported once, and leave the routes beside it
 * still asked.
 *
 * <p>The routes are one set per running game, so every case empties them before and after itself.
 * Each method's cases live in a {@link Nested} group so the suite reports as a per-method tree.
 */
final class ModdedSystemAccessRoutesTest {

    private static final ModdedSystemAccessRoute DECLINING_ROUTE = anySystem -> false;
    private static final ModdedSystemAccessRoute GRANTING_ROUTE = anySystem -> true;

    // A route whose mod moved what it reads, as the first read after the move meets it.
    private static final ModdedSystemAccessRoute UNLINKABLE_ROUTE = anySystem -> {
        throw new NoSuchMethodError("the mod moved what the route reads");
    };

    // Where every route here reports, so a failing case records into a record of its own rather
    // than into the session's.
    private final CompatibilityFailures failureRecord = new CompatibilityFailures();

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
            installRoute("first mod", DECLINING_ROUTE);
            installRoute("second mod", DECLINING_ROUTE);

            assertThat(ModdedSystemAccessRoutes.readRouteNames())
                .containsExactly("first mod", "second mod");
        }

        @Test
        void replacesARouteRegisteredAgainUnderTheSameName() {
            // An integration composing itself twice - a reload, a settings save that re-runs the
            // composition - is one way in rather than two identical ones stacked.
            installRoute("a mod", DECLINING_ROUTE);
            installRoute("a mod", GRANTING_ROUTE);

            assertThat(ModdedSystemAccessRoutes.readRouteNames())
                .containsExactly("a mod");
            assertThat(ModdedSystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class)))
                .isTrue();
        }

        @Test
        void passesOverANullRoute() {
            // An absent integration is a state to leave alone. Registering nothing must not
            // disturb what another mod did install, so the standing route survives.
            installRoute("a mod", GRANTING_ROUTE);
            installRoute("an absent mod", null);

            assertThat(ModdedSystemAccessRoutes.readRouteNames())
                .containsExactly("a mod");
        }

        @Test
        void refusesARouteWithNoIntegrationToReportItsFailureUnder() {

            assertThatNullPointerException()
                .isThrownBy(() -> ModdedSystemAccessRoutes.registerRoute("a mod", GRANTING_ROUTE, null));
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
            installRoute("second mod", DECLINING_ROUTE);
            installRoute("first mod", DECLINING_ROUTE);

            assertThat(ModdedSystemAccessRoutes.readRouteNames())
                .containsExactly("second mod", "first mod");
        }
    }

    @Nested
    class ClearRoutes {

        @Test
        void emptiesTheSetSoAnInstallCanBeComposedAgainFromNothing() {
            installRoute("a mod", GRANTING_ROUTE);

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
            installRoute("first mod", DECLINING_ROUTE);
            installRoute("second mod", DECLINING_ROUTE);

            assertThat(ModdedSystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class)))
                .isFalse();
        }

        @Test
        void returnsTrueWhenOneRouteOfSeveralGrantsAccess() {
            // One route answering false leaves the question where it found it, so the granting
            // route behind it still decides the answer.
            installRoute("first mod", DECLINING_ROUTE);
            installRoute("second mod", GRANTING_ROUTE);

            assertThat(ModdedSystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class)))
                .isTrue();
        }

        @Test
        void handsTheSystemAskedAboutToTheRoute() {
            // A route decides per system, so it has to be given the one being asked about rather
            // than being consulted as a standing yes-or-no about the install.
            var reachableSystemMock = mock(StarSystemAPI.class);
            var unreachableSystemMock = mock(StarSystemAPI.class);

            installRoute("a mod", askedSystem -> askedSystem == reachableSystemMock);

            assertThat(ModdedSystemAccessRoutes.isReachedByAnyRoute(reachableSystemMock))
                .isTrue();
            assertThat(ModdedSystemAccessRoutes.isReachedByAnyRoute(unreachableSystemMock))
                .isFalse();
        }

        @Test
        void asksTheRoutesAfterOneThatFailed() {
            // A failed route is one mod's way in gone, not every mod's: the walk goes on past it,
            // so a second mod's route still reaches the system.
            installRoute("first mod", UNLINKABLE_ROUTE);
            installRoute("second mod", GRANTING_ROUTE);

            assertThat(ModdedSystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class)))
                .isTrue();
        }

        @Test
        void readsARouteThatThrewAsNotReachingTheSystem() {
            // A read has no half-done work to protect, so a throw of any kind answers as the mod
            // being absent would.
            installRoute("a mod", anySystem -> {
                throw new IllegalStateException("the route's own state was not ready");
            });

            assertThat(ModdedSystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class)))
                .isFalse();
        }

        @Test
        void neverAsksARouteAgainOnceItFailed() {
            // The read runs on every map refresh, and a link failure recurs on each - so the route
            // is out for the session rather than asked and failing forever.
            var readsReceived = new AtomicInteger();
            installRoute("a mod", anySystem -> {
                readsReceived.incrementAndGet();
                throw new NoSuchMethodError("the mod moved what the route reads");
            });

            ModdedSystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class));
            ModdedSystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class));

            assertThat(readsReceived)
                .hasValue(1);
            assertThat(ModdedSystemAccessRoutes.readRouteNames())
                .isEmpty();
        }

        @Test
        void reportsAFailedRouteOnceUnderTheIntegrationThatRegisteredIt() {

            installRoute("a mod", UNLINKABLE_ROUTE);

            ModdedSystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class));

            var failure = failureRecord.takeNextUnreported();

            assertThat(failure.subject().name())
                .isEqualTo(CompatibilityFailureFixture.INTEGRATED_MOD_NAME);
            assertThat(failure.breakage().failureSite())
                .isEqualTo("reading whether a star system is reachable");
            assertThat(failureRecord.takeNextUnreported())
                .isNull();
        }

        @Test
        void answersRatherThanPropagatingAReportThatThrew() {
            // The report is composed from wording that may not have loaded, on a read every map
            // refresh repeats - a report that threw would take the read down with it.
            ModdedSystemAccessRoutes.registerRoute(
                "a mod",
                UNLINKABLE_ROUTE,
                () -> {
                    throw new IllegalArgumentException("the wording did not load");
                },
                failureRecord);

            assertThatCode(() -> ModdedSystemAccessRoutes.isReachedByAnyRoute(mock(StarSystemAPI.class)))
                .doesNotThrowAnyException();
        }
    }

    private void installRoute(String integrationName, ModdedSystemAccessRoute accessRoute) {

        ModdedSystemAccessRoutes.registerRoute(
            integrationName,
            accessRoute,
            () -> CompatibilityFailureFixture.MOD_INTEGRATION,
            failureRecord);
    }
}
