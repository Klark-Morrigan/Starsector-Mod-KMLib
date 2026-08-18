package kmlib.starsector.ui.coreui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.SectorAPI;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins what counts as game space, over each signal that can end it and each way the read can fail
 * closed.
 *
 * <p>The two signals are what the whole answer rests on, and neither is derivable from the other: a
 * core screen raises a tab and a scripted dialog raises none while covering the world completely, so
 * a reading that dropped either would report game space over a screen the player is looking at.
 *
 * <p>The pause menu is the case worth stating explicitly, because it is the one that looks like an
 * omission. A raised menu is neither a core screen nor a dialog, so it leaves game space standing -
 * deliberately, the menu being a caller's to stand aside for rather than this read's to fold in.
 */
class CampaignScreenViewTest {

    private CampaignUIAPI campaignUiMock;
    private SectorAPI sectorMock;

    private MockedStatic<Global> globalMock;

    @BeforeEach
    void setUp() {

        campaignUiMock = mock(CampaignUIAPI.class);
        sectorMock = mock(SectorAPI.class);

        globalMock = mockStatic(Global.class);
        globalMock
            .when(Global::getSector)
            .thenReturn(sectorMock);

        // Answer the logger even though this class holds none. The mock is in force for every class
        // the JVM happens to initialise while it is open, and a class holding Global.getLogger in a
        // static field would take the mock's unstubbed null and keep it for the rest of the run -
        // failing suites that never mention Global. Which classes those are is decided by load
        // order rather than by this test, so the stub is owed regardless of what is under test.
        globalMock
            .when(() -> Global.getLogger(any(Class.class)))
            .thenReturn(mock(Logger.class));

        when(sectorMock.getCampaignUI())
            .thenReturn(campaignUiMock);

        // Game space by default - no core tab, no dialog - so each case below relaxes one signal
        // and what it observes is that signal rather than the arrangement around it.
        when(campaignUiMock.getCurrentCoreTab())
            .thenReturn(null);
        when(campaignUiMock.isShowingDialog())
            .thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Nested
    class IsShowingGameSpace {

        @Test
        void isShowingGameSpaceIsTrueWithNoScreenAndNoDialogUp() {
            // The player flying the sector, which is the whole of what this answers yes to.
            assertThat(CampaignScreenView.isShowingGameSpace())
                .isTrue();
        }

        @Test
        void isShowingGameSpaceIsFalseWhileACoreScreenIsUp() {
            // Any core screen ends it, not the map alone: the tab is read for its presence rather
            // than for which one it names, so a screen added by a later game build ends it too.
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.INTEL);

            assertThat(CampaignScreenView.isShowingGameSpace())
                .isFalse();
        }

        @Test
        void isShowingGameSpaceIsFalseWhileADialogIsUp() {
            // The second signal earning its place: a scripted dialog hosts no core UI, so it raises
            // no tab while covering the world completely.
            when(campaignUiMock.isShowingDialog())
                .thenReturn(true);

            assertThat(CampaignScreenView.isShowingGameSpace())
                .isFalse();
        }

        @Test
        void isShowingGameSpaceIsTrueWhileThePauseMenuIsUp() {
            // Deliberate, and the reason this read names no menu: the menu is raised over whatever
            // was on screen without taking it down, so it is neither of the two signals. A caller
            // that must stand aside for it reads the menu itself.
            when(campaignUiMock.isShowingMenu())
                .thenReturn(true);

            assertThat(CampaignScreenView.isShowingGameSpace())
                .isTrue();
        }

        @Test
        void isShowingGameSpaceIsFalseBeforeThereIsASector() {
            // Failing closed. The reads only ever widen what a caller does, and a widening taken on
            // a campaign that is not stood up would act on a screen nobody can see.
            globalMock.when(Global::getSector)
                .thenReturn(null);

            assertThat(CampaignScreenView.isShowingGameSpace())
                .isFalse();
        }

        @Test
        void isShowingGameSpaceIsFalseBeforeThereIsACampaignUi() {
            // The same failure one hop further in, which a sector answers for during load.
            when(sectorMock.getCampaignUI())
                .thenReturn(null);

            assertThat(CampaignScreenView.isShowingGameSpace())
                .isFalse();
        }
    }
}
