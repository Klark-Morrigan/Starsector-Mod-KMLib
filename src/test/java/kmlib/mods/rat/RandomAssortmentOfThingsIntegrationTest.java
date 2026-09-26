package kmlib.mods.rat;

import kmlib.starsector.systems.ModdedSystemAccessRoutes;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;
import kmlib.testfixtures.starsector.settings.StubbedModIds;

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
        ModdedSystemAccessRoutes.clearRoutes();
    }

    @AfterEach
    void tearDown() {
        ModdedSystemAccessRoutes.clearRoutes();
    }

    @Nested
    class InstallModdedSystemAccessRoutes {

        @Test
        void putsARouteInFrontOfTheReachabilityReadUnderTheModSName() {
            // The Abyssal Fracture is the one way this mod moves fleets that the engine does not
            // model. A registration missed here is a system quietly reading as cut off on a save
            // whose fleets get there every day. The name is asserted with it because what the
            // startup log is read for is which mod supplies the way in.
            RandomAssortmentOfThingsIntegration.installModdedSystemAccessRoutes(
                WITH_RANDOM_ASSORTMENT_OF_THINGS);

            assertThat(ModdedSystemAccessRoutes.readRouteNames())
                .containsExactly(INTEGRATION_NAME);
        }

        @Test
        void installsOneRouteWhenTheInstallIsComposedTwice() {
            // Keyed by name, so a second composition replaces this integration's own route rather
            // than adding a second one that would be walked for nothing on every read.
            RandomAssortmentOfThingsIntegration.installModdedSystemAccessRoutes(
                WITH_RANDOM_ASSORTMENT_OF_THINGS);
            RandomAssortmentOfThingsIntegration.installModdedSystemAccessRoutes(
                WITH_RANDOM_ASSORTMENT_OF_THINGS);

            assertThat(ModdedSystemAccessRoutes.readRouteNames())
                .containsExactly(INTEGRATION_NAME);
        }

        @Test
        void installsNothingOnAnInstallWithoutTheMod() {
            // What keeps the reachability read clear of a class naming a Random Assortment of
            // Things type: with nothing registered, no route is ever consulted and no such class
            // is ever reached.
            RandomAssortmentOfThingsIntegration.installModdedSystemAccessRoutes(
                WITHOUT_RANDOM_ASSORTMENT_OF_THINGS);

            assertThat(ModdedSystemAccessRoutes.readRouteNames())
                .isEmpty();
        }
    }

    @Nested
    class DescribeIntegration {

        @AfterEach
        void clearSettings() {

            StarsectorSettingsFake.clearSettings();
        }

        @Test
        void namesRandomAssortmentOfThingsAndTheRoutesFeatureItsRouteCosts() {
            // A transposition composes as plausibly as the right pairing, and reaches a player as a
            // report naming a mod that was working or a loss that did not happen.
            StarsectorSettingsFake.installSettings((category, key) -> "the sentence for " + key);

            var integration = RandomAssortmentOfThingsIntegration.describeIntegration();

            assertThat(integration.subjectModId())
                .isEqualTo(StubbedModIds.RANDOM_ASSORTMENT_OF_THINGS);
            assertThat(integration.subjectModName())
                .isEqualTo("Random Assortment of Things");
            assertThat(integration.consumer().consumerKey())
                .isEqualTo("kmlib:system-access-routes");
            assertThat(integration.consumer().lostFeature())
                .isEqualTo("the sentence for compatibility_lost_rat_access_routes");
            assertThat(integration.consumer().unaffectedFeature())
                .isEqualTo("the sentence for compatibility_unaffected_rat_access_routes");
        }
    }
}
