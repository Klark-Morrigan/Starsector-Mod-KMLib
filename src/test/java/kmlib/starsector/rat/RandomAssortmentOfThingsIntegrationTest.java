package kmlib.starsector.rat;

import kmlib.starsector.systems.SystemAccessRoutes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one thing this library does about Random Assortment of Things before any system is
 * asked whether it can be reached: an install with the mod gets its route in front of the
 * reachability read, under a name a log line can be read by, and an install without it gets
 * nothing at all.
 *
 * <p>That second case is the load-bearing one. A read on an install without the mod must never
 * reach a class that names a Random Assortment of Things type, and the only thing standing between
 * the two is that nothing was registered here - so it is asserted rather than assumed.
 *
 * <p>Posed against a stated answer about the mod rather than the live one, which is what lets both
 * installs be arranged on a machine that has whichever mods it happens to have. The routes are one
 * set per running game, so each case empties them before and after itself.
 */
final class RandomAssortmentOfThingsIntegrationTest {

    private static final String INTEGRATION_NAME = "Random Assortment of Things";
    private static final boolean WITHOUT_RANDOM_ASSORTMENT_OF_THINGS = false;
    private static final boolean WITH_RANDOM_ASSORTMENT_OF_THINGS = true;

    @BeforeEach
    void setUp() {
        SystemAccessRoutes.clearRoutes();
    }

    @AfterEach
    void tearDown() {
        SystemAccessRoutes.clearRoutes();
    }

    @Nested
    class InstallSystemAccessRoutes {

        @Test
        void puts_a_route_in_front_of_the_reachability_read_under_the_mod_s_name() {
            // The Abyssal Fracture is the one way this mod moves fleets that the engine does not
            // model. A registration missed here is a system quietly reading as cut off on a save
            // whose fleets get there every day. The name is asserted with it because what the
            // startup log is read for is which mod supplies the way in.
            RandomAssortmentOfThingsIntegration.installSystemAccessRoutes(
                WITH_RANDOM_ASSORTMENT_OF_THINGS);

            assertThat(SystemAccessRoutes.readRouteNames())
                .containsExactly(INTEGRATION_NAME);
        }

        @Test
        void installs_one_route_when_the_install_is_composed_twice() {
            // Keyed by name, so a second composition replaces this integration's own route rather
            // than adding a second one that would be walked for nothing on every read.
            RandomAssortmentOfThingsIntegration.installSystemAccessRoutes(
                WITH_RANDOM_ASSORTMENT_OF_THINGS);
            RandomAssortmentOfThingsIntegration.installSystemAccessRoutes(
                WITH_RANDOM_ASSORTMENT_OF_THINGS);

            assertThat(SystemAccessRoutes.readRouteNames())
                .containsExactly(INTEGRATION_NAME);
        }

        @Test
        void installs_nothing_on_an_install_without_the_mod() {
            // What keeps the reachability read clear of a class naming a Random Assortment of
            // Things type: with nothing registered, no route is ever consulted and no such class
            // is ever reached.
            RandomAssortmentOfThingsIntegration.installSystemAccessRoutes(
                WITHOUT_RANDOM_ASSORTMENT_OF_THINGS);

            assertThat(SystemAccessRoutes.readRouteNames())
                .isEmpty();
        }
    }
}
