package kmlib;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.mods.nexerelin.NexerelinIntegration;
import kmlib.mods.rat.RandomAssortmentOfThingsIntegration;
import kmlib.opengl.FastRendering;
import kmlib.settings.KmlibLunaSettings;
import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.starsector.compatibility.CompatibilityNotice;
import kmlib.testfixtures.starsector.StubbedGlobalLogger;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;
import kmlib.testfixtures.starsector.compatibility.CompatibilitySlotTemplates;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the composition the entry point names: which steps a launch is made of, which third party
 * each of them integrates with, and the wiring it owns for the compatibility notice - that a game
 * load puts one on the loaded sector, that it is transient and exactly one, and that it drains the
 * session's record rather than one of its own.
 *
 * <p>What each launch step does is pinned beside that step, what the notice does on a frame beside
 * the notice, and what a transient install clears and adds beside {@code SectorScripts}. None is
 * re-asserted here: the launch list is held over every step stood in for at once, and the notice
 * cases read the outcome off a sector that keeps its transient scripts the way the engine does.
 *
 * <p>The record case is the one nothing else would catch. A notice built on a fresh record would
 * install, run every frame and show nothing for the session, and the record the bindings write into
 * would fill unread - both halves looking healthy from their own suites.
 *
 * <p>The integration descriptions are the other such case. Which third party a step is paired with,
 * and which of the library's sentences goes with it, compose and read as plausibly transposed as
 * they do right - and a transposition reaches a player as a report naming a mod that was working.
 */
final class KMLib_ModPluginTest {

    private static final float ONE_FRAME = 0.016f;

    // Latched on the session's record for the rest of the run, so it is a key no binding records
    // under and no other case reads.
    private static final String SUBJECT_KEY = "kmlib-mod-plugin-subject";

    @Nested
    class OnApplicationLoad {

        @Test
        void standsUpEveryStepALaunchIsMadeOf() {
            // The list is the whole subject: a step nobody named is one that silently never runs,
            // and nothing else says so - the setting still reads, the integration still compiles.
            // Held over every step at once, so a step dropped from the wiring fails here rather
            // than in play.
            try (var lunaSettingsMock = mockStatic(KmlibLunaSettings.class);
                    var fastRenderingMock = mockStatic(FastRendering.class);
                    var nexerelinMock = mockStatic(NexerelinIntegration.class);
                    var ratMock = mockStatic(RandomAssortmentOfThingsIntegration.class)) {

                new KMLib_ModPlugin().onApplicationLoad();

                lunaSettingsMock.verify(KmlibLunaSettings::installBindings);
                fastRenderingMock.verify(FastRendering::isFastRenderingActive);
                nexerelinMock.verify(NexerelinIntegration::installRoutines);
                ratMock.verify(RandomAssortmentOfThingsIntegration::installModdedSystemAccessRoutes);
            }
        }

        @Test
        void standsUpTheRemainingStepsAfterOneIntegrationRefusesToInstall() {
            // The guard's isolation, asserted where the steps are actually listed: a wiring whose
            // first failing install aborted the rest would leave the library half composed, and
            // every step after the failure looks installed from its own suite.
            try (var lunaSettingsMock = mockStatic(KmlibLunaSettings.class);
                    var fastRenderingMock = mockStatic(FastRendering.class);
                    var nexerelinMock = mockStatic(NexerelinIntegration.class);
                    var ratMock = mockStatic(RandomAssortmentOfThingsIntegration.class)) {

                nexerelinMock.when(NexerelinIntegration::installRoutines)
                    .thenThrow(new IllegalStateException("no routines to register with"));

                new KMLib_ModPlugin().onApplicationLoad();

                ratMock.verify(RandomAssortmentOfThingsIntegration::installModdedSystemAccessRoutes);
            }
        }
    }

    @Nested
    class IntegrationDescriptions {

        @AfterEach
        void clearSettings() {

            StarsectorSettingsFake.clearSettings();
        }

        @BeforeEach
        void answerEveryStringWithItsOwnKey() {
            // Every key answers a sentence naming itself, so a case reads as which wording landed
            // in which slot without the shipped wording being known here - that file is a matter
            // for the suite that walks it.
            StarsectorSettingsFake.installSettings((category, key) -> "the sentence for " + key);
        }

        @Test
        void namesLunaLibAsTheThirdPartyTheSettingsBindingIsWith() {

            var integration = KMLib_ModPlugin.describeLunaLibIntegration();

            assertThat(integration.subjectModId())
                .isEqualTo("lunalib");
            assertThat(integration.subjectModName())
                .isEqualTo("LunaLib");
            assertThat(integration.consumer().consumerKey())
                .isEqualTo("kmlib:lunalib-settings");
            assertThat(integration.consumer().lostFeature())
                .isEqualTo("the sentence for compatibility_lost_lunalib_settings");
            assertThat(integration.consumer().unaffectedFeature())
                .isEqualTo("the sentence for compatibility_unaffected_lunalib_settings");
        }

        @Test
        void namesNexerelinAsTheThirdPartyTheRoutinesAreWith() {

            var integration = KMLib_ModPlugin.describeNexerelinIntegration();

            assertThat(integration.subjectModId())
                .isEqualTo("nexerelin");
            assertThat(integration.subjectModName())
                .isEqualTo("Nexerelin");
            assertThat(integration.consumer().consumerKey())
                .isEqualTo("kmlib:nexerelin-routines");
            assertThat(integration.consumer().lostFeature())
                .isEqualTo("the sentence for compatibility_lost_nexerelin_routines");
            assertThat(integration.consumer().unaffectedFeature())
                .isEqualTo("the sentence for compatibility_unaffected_nexerelin_routines");
        }

        @Test
        void namesRandomAssortmentOfThingsAsTheThirdPartyTheRoutesAreWith() {

            var integration = KMLib_ModPlugin.describeRandomAssortmentOfThingsIntegration();

            assertThat(integration.subjectModId())
                .isEqualTo("assortment_of_things");
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

    @Nested
    class OnGameLoad {

        @Test
        void installsTheCompatibilityNoticeOnTheLoadedSector() {

            var sectorMock = mock(SectorAPI.class);

            try (var globalMock = StubbedGlobalLogger.openGlobalAnsweringLoggers()) {
                globalMock.when(Global::getSector)
                    .thenReturn(sectorMock);

                new KMLib_ModPlugin().onGameLoad(false);
            }

            verify(sectorMock)
                .addTransientScript(any(CompatibilityNotice.class));
        }
    }

    @Nested
    class InstallCompatibilityNotice {

        private final List<EveryFrameScript> transientScripts = new ArrayList<>();

        private SectorAPI sectorMock;

        @BeforeEach
        void setUp() {

            sectorMock = buildSectorHoldingTransientScripts(transientScripts);
        }

        @AfterEach
        void clearSettings() {

            StarsectorSettingsFake.clearSettings();
        }

        @Test
        void registersExactlyOneNoticeAsATransientScript() {

            KMLib_ModPlugin.installCompatibilityNotice(sectorMock);

            assertThat(transientScripts)
                .hasSize(1)
                .first()
                .isInstanceOf(CompatibilityNotice.class);
            verify(sectorMock, never())
                .addScript(any());
        }

        @Test
        void replacesRatherThanDoublesWhenAskedTwice() {

            KMLib_ModPlugin.installCompatibilityNotice(sectorMock);
            var firstNotice = transientScripts.get(0);

            KMLib_ModPlugin.installCompatibilityNotice(sectorMock);

            assertThat(transientScripts)
                .hasSize(1)
                .first()
                .isNotSameAs(firstNotice);
        }

        @Test
        void drainsTheSessionRecordRatherThanOneOfItsOwn() {

            var campaignUiMock = mock(CampaignUIAPI.class);
            when(sectorMock.getCampaignUI())
                .thenReturn(campaignUiMock);
            CompatibilitySlotTemplates.installSlotTemplates();

            KMLib_ModPlugin.installCompatibilityNotice(sectorMock);
            CompatibilityFailures.SESSION_RECORD.recordOnce(
                SUBJECT_KEY,
                CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER,
                CompatibilityFailureFixture::createFailure);
            transientScripts.get(0).advance(ONE_FRAME);

            verify(campaignUiMock)
                .showMessageDialog(anyString());
        }
    }

    // A sector that keeps its transient scripts the way the engine does: added to a list, cleared by
    // exact class. The list is the caller's, so a case reads what landed there after the call.
    private static SectorAPI buildSectorHoldingTransientScripts(List<EveryFrameScript> transientScripts) {

        var sectorMock = mock(SectorAPI.class);

        doAnswer(call -> transientScripts.add(call.<EveryFrameScript>getArgument(0)))
            .when(sectorMock)
            .addTransientScript(any());
        doAnswer(call -> transientScripts.removeIf(
                script -> script.getClass() == call.<Class<?>>getArgument(0)))
            .when(sectorMock)
            .removeTransientScriptsOfClass(any());

        return sectorMock;
    }
}
