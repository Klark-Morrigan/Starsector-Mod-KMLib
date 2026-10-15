package kmlib.mods.nexerelin;

import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.starsector.markets.colonisation.ColonisationRoutines;
import kmlib.starsector.markets.ownership.OwnerSubmarketRules;
import kmlib.starsector.markets.ownership.OwnershipTransferRoutines;
import kmlib.testfixtures.starsector.listeners.RecordingListenerManager;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;
import kmlib.testfixtures.starsector.settings.StubbedModIds;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the one thing this library does about Nexerelin before anything is founded or handed over:
 * an install with the mod gets its adapters in front of all three operations, under a name a log
 * line can be read by, and an install without it gets nothing at all.
 *
 * <p>That second case is the load-bearing one. An operation on an install without Nexerelin must
 * never reach a routine that names a Nexerelin type, and the only thing standing between the two
 * is that nothing was installed here - so it is asserted rather than assumed.
 *
 * <p>The colony-transfer relay is held to the same split: one transient relay per sector with the
 * mod, and nothing without it.
 *
 * <p>Posed against a stated answer about the mod rather than the live one, which is what lets both
 * installs be arranged on a machine that has whichever mods it happens to have. The points are one
 * per running game, so each case empties all three before and after itself.
 */
final class NexerelinIntegrationTests {

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
        void putsAnAdapterInFrontOfEveryOperationItCovers() {
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
        void installsEveryAdapterUnderANameALogLineCanBeReadBy() {
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
        void installsNothingOnAnInstallWithoutTheMod() {
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

    @Nested
    class InstallTransferRelay {

        private final RecordingListenerManager listenerManager = new RecordingListenerManager();
        private final SectorAPI sectorMock = mock(SectorAPI.class);

        @BeforeEach
        void answerWithTheListenerManager() {

            when(sectorMock.getListenerManager())
                .thenReturn(listenerManager);
        }

        @Test
        void putsOneTransientRelayOnTheSector() {
            // Transient, because a save holding a class that implements a Nexerelin interface would
            // fail to load once Nexerelin was removed.
            NexerelinIntegration.installTransferRelay(sectorMock, WITH_NEXERELIN);

            assertThat(listenerManager.getListeners(NexerelinMarketTransferRelay.class))
                .hasSize(1);
            assertThat(listenerManager.getAddedTransientFlags())
                .containsExactly(true);
        }

        @Test
        void keepsOneRelayWhenInstalledTwice() {
            // Two relays would tell every listener of each transfer twice.
            NexerelinIntegration.installTransferRelay(sectorMock, WITH_NEXERELIN);
            NexerelinIntegration.installTransferRelay(sectorMock, WITH_NEXERELIN);

            assertThat(listenerManager.getListeners(NexerelinMarketTransferRelay.class))
                .hasSize(1);
        }

        @Test
        void installsNothingOnAnInstallWithoutTheMod() {
            // The relay implements a Nexerelin interface, so an install without the mod must never
            // load it.
            NexerelinIntegration.installTransferRelay(sectorMock, WITHOUT_NEXERELIN);

            assertThat(listenerManager.getAddedListeners())
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
        void namesNexerelinAndTheRoutinesFeatureItsAdaptersCost() {
            // A transposition composes as plausibly as the right pairing, and reaches a player as a
            // report naming a mod that was working or a loss that did not happen.
            StarsectorSettingsFake.installSettings((category, key) -> "the sentence for " + key);

            var integration = NexerelinIntegration.describeIntegration();

            assertThat(integration.subjectModId())
                .isEqualTo(StubbedModIds.NEXERELIN);
            assertThat(integration.subjectModName())
                .isEqualTo("Nexerelin");
            assertThat(integration.consumer().consumerKey())
                .isEqualTo("kmlib:nexerelin-routines");
            assertThat(integration.consumer().lostFeature())
                .isEqualTo("the sentence for compatibility_lost_nexerelin_routines");
            assertThat(integration.consumer().unaffectedFeature())
                .isEqualTo("the sentence for compatibility_unaffected_nexerelin_routines");
        }
    }

    @Nested
    class DescribeTransferRelayIntegration {

        @AfterEach
        void clearSettings() {

            StarsectorSettingsFake.clearSettings();
        }

        @Test
        void namesNexerelinAndTheColonyTransfersFeatureTheRelayCosts() {
            // Apart from the routines' feature key, so a failed relay and a failed routine latch
            // and report separately.
            StarsectorSettingsFake.installSettings((category, key) -> "the sentence for " + key);

            var integration = NexerelinIntegration.describeTransferRelayIntegration();

            assertThat(integration.subjectModId())
                .isEqualTo(StubbedModIds.NEXERELIN);
            assertThat(integration.subjectModName())
                .isEqualTo("Nexerelin");
            assertThat(integration.consumer().consumerKey())
                .isEqualTo("kmlib:nexerelin-colony-transfers");
            assertThat(integration.consumer().lostFeature())
                .isEqualTo("the sentence for compatibility_lost_nexerelin_colony_transfers");
            assertThat(integration.consumer().unaffectedFeature())
                .isEqualTo("the sentence for compatibility_unaffected_nexerelin_colony_transfers");
        }
    }
}
