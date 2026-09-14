package kmlib.starsector.ui.intel;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the two rules the class applies on its own: {@link VanillaIntelScreenView#isIntelTabOpen} -
 * the one method that reads published API - across each way it fails closed,
 * {@link VanillaIntelScreenView#isMapVisorLit}, which decides whether the two widget readings
 * amount to a visor worth drawing over, and
 * {@link VanillaIntelScreenView#warnOnceAboutUnreachableIntelPanel} and
 * {@link VanillaIntelScreenView#warnOnceAboutUnreadableCoreUi}, which decide when a panel the walk
 * did not reach is worth saying so about - and, between them, keep a walk that came back empty
 * apart from one that failed outright, the second being the only one with a cause to print.
 *
 * <p>{@link VanillaIntelScreenView#getMapVisorWidget} and
 * {@link VanillaIntelScreenView#isMapStarscapeModeOn} are not unit-tested: both name the obfuscated
 * {@code EventsPanel}, whose dotted member names fail class-load under a verifying JVM (it only
 * loads under the game's non-verifying one), so not even their fail-closed branches can be entered
 * here and the walk itself is exercised in-game. {@link VanillaIntelScreenView#getMapVisorRect} is
 * out of reach with them, being derived from the widget read. Only that widget-fetching is out of
 * reach, which is why the rules applied around it are reachable on their own.
 */
class VanillaIntelScreenViewTest {

    // The class caches its logger in a static field on first load, so a fresh per-test mock would be
    // handed to the class only in whichever test happened to load it first. One instance for the
    // whole class, cleared per test, is what makes the warning assertions see what was written.
    private static final Logger loggerMock = mock(Logger.class);

    private MockedStatic<Global> globalMock;
    private SectorAPI sectorMock;
    private CampaignUIAPI campaignUiMock;

    @BeforeEach
    void setUp() {
        reset(loggerMock);

        sectorMock = mock(SectorAPI.class);
        campaignUiMock = mock(CampaignUIAPI.class);

        globalMock = mockStatic(Global.class);
        globalMock
            .when(Global::getSector)
            .thenReturn(sectorMock);

        // The class logs a one-shot warning when the intel tab is up but its panel is unreachable;
        // give it a logger so that static field init and that warn path do not dereference null
        // under the mock.
        globalMock
            .when(() -> Global.getLogger(any(Class.class)))
            .thenReturn(loggerMock);

        when(sectorMock.getCampaignUI())
            .thenReturn(campaignUiMock);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Nested
    class IsIntelTabOpen {
        @Test
        void isTrueWhenTheIntelTabIsActive() {

            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.INTEL);

            assertThat(new VanillaIntelScreenView().isIntelTabOpen())
                .isTrue();
        }

        @Test
        void isFalseWhenAnotherTabIsActive() {

            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.MAP);

            assertThat(new VanillaIntelScreenView().isIntelTabOpen())
                .isFalse();
        }

        @Test
        void isFalseWhenTheSectorIsMissing() {

            globalMock.when(Global::getSector)
                .thenReturn(null);

            assertThat(new VanillaIntelScreenView().isIntelTabOpen())
                .isFalse();
        }

        @Test
        void isFalseWhenTheCampaignUiIsMissing() {
            when(sectorMock.getCampaignUI())
                .thenReturn(null);

            assertThat(new VanillaIntelScreenView().isIntelTabOpen())
                .isFalse();
        }
    }

    @Nested
    class IsMapVisorLit {
        // The game snaps both readings, so these are the values the live widgets actually carry.
        private static final float DARK = 0f;
        private static final float LIT = 1f;

        @Test
        void isMapVisorLitIsTrueWhenTheIntelSubtabShowsAndThePreviewIsLit() {
            assertThat(VanillaIntelScreenView.isMapVisorLit(LIT, LIT))
                .isTrue();
        }

        @Test
        void isMapVisorLitIsFalseWhenASiblingSubtabIsShowing() {
            // The Planets and Factions sub-tabs share the intel tab's container, and switching to one
            // only fades the events panel out - its map widget stays at full opacity behind them. Reading
            // that opacity alone therefore reports a visor that is not on screen, and the sidebar drew
            // over those sub-tabs. The dark panel is what rules them out.
            assertThat(VanillaIntelScreenView.isMapVisorLit(DARK, LIT))
                .isFalse();
        }

        @Test
        void isMapVisorLitIsFalseWhenThePreviewIsBlanked() {
            // The Intel sub-tab is showing, but a large-description item has blanked the preview, so
            // there is a lit panel with no lit canvas inside it.
            assertThat(VanillaIntelScreenView.isMapVisorLit(LIT, DARK))
                .isFalse();
        }

        @Test
        void isMapVisorLitIsFalseWhenNeitherReadingIsLit() {
            assertThat(VanillaIntelScreenView.isMapVisorLit(DARK, DARK))
                .isFalse();
        }

        @Test
        void isMapVisorLitPinsTheThresholdOfEachReading() {
            // Nothing eases these readings today, so a mid value is only reachable if the game changes
            // to fade them. The two thresholds differ - a panel counts as showing from halfway, while
            // the preview has to be all but fully opaque - so each is pinned on its own, keeping a
            // change to either from silently flipping the gate.
            assertThat(VanillaIntelScreenView.isMapVisorLit(0.5f, LIT))
                .isTrue();
            assertThat(VanillaIntelScreenView.isMapVisorLit(0.49f, LIT))
                .isFalse();
            assertThat(VanillaIntelScreenView.isMapVisorLit(LIT, 0.9f))
                .isTrue();
            assertThat(VanillaIntelScreenView.isMapVisorLit(LIT, 0.89f))
                .isFalse();
        }
    }

    @Nested
    class WarnOnceAboutUnreachableIntelPanel {
        @Test
        void warnsWhenTheIntelTabIsOpen() {
            // The case the warning exists for: the tab the panel lives on is up, so the walk had
            // something to find and came back empty anyway. Without this line a game build that
            // reshaped the intel tab would stop every intel-screen overlay in silence.
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.INTEL);

            new VanillaIntelScreenView().warnOnceAboutUnreachableIntelPanel();

            verify(loggerMock)
                .warn(any());
        }

        @Test
        void staysSilentWhenAnotherTabIsShowing() {
            // The ordinary state on every other screen: the tab that is up holds no events panel, so
            // an unreached panel is the expected answer rather than news.
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.MAP);

            new VanillaIntelScreenView().warnOnceAboutUnreachableIntelPanel();

            verify(loggerMock, never())
                .warn(any());
        }

        @Test
        void warnsOnlyOnceWhileTheReachKeepsFailing() {
            // A visor read runs per frame, so a build this reach no longer fits would otherwise
            // write the same line sixty times a second.
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.INTEL);

            var intelScreen = new VanillaIntelScreenView();
            intelScreen.warnOnceAboutUnreachableIntelPanel();
            intelScreen.warnOnceAboutUnreachableIntelPanel();

            verify(loggerMock, times(1))
                .warn(any());
        }
    }

    @Nested
    class WarnOnceAboutUnreadableCoreUi {

        @Test
        void warnsWithTheCauseThatBrokeTheWalk() {
            // The cause is the whole of the diagnosis: the visor reads answer null either way, so
            // without it a reach that failed outright looks exactly like a screen holding no intel
            // panel. It is carried into the line because nothing else in the class records it.
            var unreadableTree = new IllegalStateException("A hop the core UI no longer offers.");

            new VanillaIntelScreenView().warnOnceAboutUnreadableCoreUi(unreadableTree);

            verify(loggerMock)
                .warn(any(), eq(unreadableTree));
        }

        @Test
        void warnsWhateverTabIsShowing() {
            // Deliberately not read against which screen is up, unlike the unreachable-panel
            // warning. The hops that fail here are ones the core UI offers whatever tab it shows,
            // so a reach that cannot take them has stopped every read built on it, not the intel
            // screen's alone.
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.MAP);

            new VanillaIntelScreenView()
                .warnOnceAboutUnreadableCoreUi(new IllegalStateException("A broken reach."));

            verify(loggerMock)
                .warn(any(), any(Throwable.class));
        }

        @Test
        void warnsOnlyOnceWhileTheWalkKeepsFailing() {
            // The walk runs per frame, so a reach that stopped working would otherwise write the
            // same line, stack and all, sixty times a second.
            var intelScreen = new VanillaIntelScreenView();

            intelScreen.warnOnceAboutUnreadableCoreUi(new IllegalStateException("A broken reach."));
            intelScreen.warnOnceAboutUnreadableCoreUi(new IllegalStateException("A broken reach."));

            verify(loggerMock, times(1))
                .warn(any(), any(Throwable.class));
        }

        @Test
        void warnsSeparatelyFromTheUnreachablePanelWarning() {
            // Two failures meaning different things, so neither one-shot may silence the other: a
            // build that broke the reach after an empty walk had already been reported would
            // otherwise say nothing about the thing that actually broke.
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.INTEL);

            var intelScreen = new VanillaIntelScreenView();
            intelScreen.warnOnceAboutUnreachableIntelPanel();
            intelScreen.warnOnceAboutUnreadableCoreUi(new IllegalStateException("A broken reach."));

            verify(loggerMock, times(1))
                .warn(any());
            verify(loggerMock, times(1))
                .warn(any(), any(Throwable.class));
        }
    }
}
