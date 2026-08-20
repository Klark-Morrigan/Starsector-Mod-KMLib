package kmlib.starsector.nexerelin;

import kmlib.starsector.markets.colonisation.ColonisationRoutines;
import kmlib.starsector.markets.ownership.OwnerSubmarketRules;
import kmlib.starsector.markets.ownership.OwnershipTransferRoutines;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one thing this library does about Nexerelin before anything is founded or handed over:
 * an install with the mod gets its adapters in front of all three operations, under a name a log
 * line can be read by, and an install without it gets nothing at all.
 *
 * <p>That second case is the load-bearing one. An operation on an install without Nexerelin must
 * never reach a routine that names a Nexerelin type, and the only thing standing between the two
 * is that nothing was installed here - so it is asserted rather than assumed.
 *
 * <p>Posed against a stated answer about the mod rather than the live one, which is what lets both
 * installs be arranged on a machine that has whichever mods it happens to have. The points are one
 * per running game, so each case empties all three before and after itself.
 */
final class NexerelinIntegrationTest {

    private static final String INTEGRATION_NAME = "Nexerelin";
    private static final boolean WITHOUT_NEXERELIN = false;
    private static final boolean WITH_NEXERELIN = true;

    @BeforeEach
    void setUp() {
        clearEveryExtensionPoint();
    }

    @AfterEach
    void tearDown() {
        clearEveryExtensionPoint();
    }

    private static void clearEveryExtensionPoint() {
        ColonisationRoutines.clearRoutine();
        OwnershipTransferRoutines.clearRoutine();
        OwnerSubmarketRules.clearRule();
    }

    @Nested
    class InstallRoutines {

        @Test
        void puts_an_adapter_in_front_of_every_operation_it_covers() {
            // Three operations may defer to this mod - founding, handing over, and the counters
            // half of an ownership change - and an install with the mod is meant to reach it on
            // all three. A registration missed here is a colony quietly built the vanilla way on
            // a save that expects Nexerelin's.
            NexerelinIntegration.installRoutines(WITH_NEXERELIN);

            assertThat(ColonisationRoutines.readRoutine())
                .isNotNull();
            assertThat(OwnershipTransferRoutines.readRoutine())
                .isNotNull();
            assertThat(OwnerSubmarketRules.readRule())
                .isNotNull();
        }

        @Test
        void installs_every_adapter_under_a_name_a_log_line_can_be_read_by() {
            // What the startup log is read for here is which mod founds colonies and moves them on
            // this install, so the name has to be the mod's rather than a lambda's type.
            NexerelinIntegration.installRoutines(WITH_NEXERELIN);

            assertThat(ColonisationRoutines.readRoutineName())
                .isEqualTo(INTEGRATION_NAME);
            assertThat(OwnershipTransferRoutines.readRoutineName())
                .isEqualTo(INTEGRATION_NAME);
            assertThat(OwnerSubmarketRules.readRuleName())
                .isEqualTo(INTEGRATION_NAME);
        }

        @Test
        void installs_nothing_on_an_install_without_the_mod() {
            // What keeps every operation clear of a class naming a Nexerelin type: with nothing
            // installed, no offer is ever made and no such class is ever reached.
            NexerelinIntegration.installRoutines(WITHOUT_NEXERELIN);

            assertThat(ColonisationRoutines.readRoutine())
                .isNull();
            assertThat(OwnershipTransferRoutines.readRoutine())
                .isNull();
            assertThat(OwnerSubmarketRules.readRule())
                .isNull();
        }
    }
}
