package kmlib.starsector.ui.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.testfixtures.logging.LogAppenderFake;
import kmlib.testfixtures.starsector.StubbedGlobalLogger;
import kmlib.testfixtures.starsector.compatibility.GameReachRecordFixture;
import kmlib.testfixtures.starsector.ui.coreui.CoreUiReachFailures;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the two rules the class applies on its own: {@link VanillaIntelScreenView#isIntelTabOpen} -
 * the one method that reads published API - across each way it fails closed,
 * {@link VanillaIntelScreenView#isMapVisorLit}, which decides whether the two widget readings
 * amount to a visor worth drawing over, and
 * {@link VanillaIntelScreenView#warnOnceAboutUnreachableIntelPanel} and
 * {@link VanillaIntelScreenView#warnOnceAboutUnreadableCoreUi}, which decide when a panel the walk
 * did not reach is worth saying so about - and, between them, keep a walk that came back empty
 * apart from one that failed outright, the second being the only one with a cause to print - and
 * {@link VanillaIntelScreenView#readIntelSubtabBrightness}, which keeps a fader that no longer links
 * from ending the frame.
 *
 * <p>{@link VanillaIntelScreenView#getMapVisorWidget}, {@link VanillaIntelScreenView#isMapStarscapeModeOn}
 * and {@link VanillaIntelScreenView#readMapVisorStateBelow} are not unit-tested: each names the
 * obfuscated {@code EventsPanel}, whose dotted member names fail class-load under a verifying JVM (it
 * only loads under the game's non-verifying one), so not even their fail-closed branches can be entered
 * here and the walk itself is exercised in-game. {@link VanillaIntelScreenView#getMapVisorRect} and
 * {@link VanillaIntelScreenView#readMapVisorState} are out of reach with them, being derived from the
 * widget read. Only that widget-fetching is out of reach, which is why the rules applied around it are
 * reachable on their own.
 */
class VanillaIntelScreenViewTests {

    private final GameReachRecordFixture reachRecord = new GameReachRecordFixture();

    private MockedStatic<Global> globalMock;
    private SectorAPI sectorMock;
    private CampaignUIAPI campaignUiMock;

    @BeforeEach
    void setUp() {

        sectorMock = mock(SectorAPI.class);
        campaignUiMock = mock(CampaignUIAPI.class);

        globalMock = StubbedGlobalLogger.openGlobalAnsweringLoggers();
        globalMock
            .when(Global::getSector)
            .thenReturn(sectorMock);

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

            // ARRANGE
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.INTEL);

            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());

            // ACT
            var actualOpen = intelScreen.isIntelTabOpen();

            // ASSERT
            assertThat(actualOpen)
                .isTrue();
        }

        @Test
        void isFalseWhenAnotherTabIsActive() {

            // ARRANGE
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.MAP);

            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());

            // ACT
            var actualOpen = intelScreen.isIntelTabOpen();

            // ASSERT
            assertThat(actualOpen)
                .isFalse();
        }

        @Test
        void isFalseWhenTheSectorIsMissing() {

            // ARRANGE
            globalMock
                .when(Global::getSector)
                .thenReturn(null);

            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());

            // ACT
            var actualOpen = intelScreen.isIntelTabOpen();

            // ASSERT
            assertThat(actualOpen)
                .isFalse();
        }

        @Test
        void isFalseWhenTheCampaignUiIsMissing() {

            // ARRANGE
            when(sectorMock.getCampaignUI())
                .thenReturn(null);

            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());

            // ACT
            var actualOpen = intelScreen.isIntelTabOpen();

            // ASSERT
            assertThat(actualOpen)
                .isFalse();
        }
    }

    @Nested
    class IsMapVisorLit {

        // The game snaps both readings, so these are the values the live widgets actually carry.
        private static final float DARK = 0f;
        private static final float LIT = 1f;

        @Test
        void isTrueWhenTheIntelSubtabShowsAndThePreviewIsLit() {

            // ARRANGE
            var intelSubtabBrightness = LIT;
            var mapWidgetOpacity = LIT;

            // ACT
            var actualLit = VanillaIntelScreenView.isMapVisorLit(
                intelSubtabBrightness,
                mapWidgetOpacity);

            // ASSERT
            assertThat(actualLit)
                .isTrue();
        }

        // The Planets and Factions sub-tabs share the intel tab's container, and switching to one
        // only fades the events panel out - its map widget stays at full opacity behind them. Reading
        // that opacity alone therefore reports a visor that is not on screen, and the sidebar drew
        // over those sub-tabs. The dark panel is what rules them out.
        @Test
        void isFalseWhenASiblingSubtabIsShowing() {

            // ARRANGE
            var intelSubtabBrightness = DARK;
            var mapWidgetOpacity = LIT;

            // ACT
            var actualLit = VanillaIntelScreenView.isMapVisorLit(
                intelSubtabBrightness,
                mapWidgetOpacity);

            // ASSERT
            assertThat(actualLit)
                .isFalse();
        }

        // The Intel sub-tab is showing, but a large-description item has blanked the preview, so
        // there is a lit panel with no lit canvas inside it.
        @Test
        void isFalseWhenThePreviewIsBlanked() {

            // ARRANGE
            var intelSubtabBrightness = LIT;
            var mapWidgetOpacity = DARK;

            // ACT
            var actualLit = VanillaIntelScreenView.isMapVisorLit(
                intelSubtabBrightness,
                mapWidgetOpacity);

            // ASSERT
            assertThat(actualLit)
                .isFalse();
        }

        @Test
        void isFalseWhenNeitherReadingIsLit() {

            // ARRANGE
            var intelSubtabBrightness = DARK;
            var mapWidgetOpacity = DARK;

            // ACT
            var actualLit = VanillaIntelScreenView.isMapVisorLit(
                intelSubtabBrightness,
                mapWidgetOpacity);

            // ASSERT
            assertThat(actualLit)
                .isFalse();
        }

        // Nothing eases these readings today, so a mid value is only reachable if the game changes
        // to fade them. The two thresholds differ - a panel counts as showing from halfway, while
        // the preview has to be all but fully opaque - so each is pinned on its own, keeping a
        // change to either from silently flipping the gate.
        @Test
        void pinsTheThresholdOfEachReading() {

            // ARRANGE
            var halfwayBrightness = 0.5f;
            var belowHalfwayBrightness = 0.49f;
            var nearlyOpaque = 0.9f;
            var belowNearlyOpaque = 0.89f;

            // ACT
            var actualLitAtHalfway = VanillaIntelScreenView.isMapVisorLit(halfwayBrightness, LIT);
            var actualLitBelowHalfway = VanillaIntelScreenView.isMapVisorLit(belowHalfwayBrightness, LIT);
            var actualLitNearlyOpaque = VanillaIntelScreenView.isMapVisorLit(LIT, nearlyOpaque);
            var actualLitBelowNearlyOpaque = VanillaIntelScreenView.isMapVisorLit(LIT, belowNearlyOpaque);

            // ASSERT
            assertThat(actualLitAtHalfway)
                .isTrue();
            assertThat(actualLitBelowHalfway)
                .isFalse();
            assertThat(actualLitNearlyOpaque)
                .isTrue();
            assertThat(actualLitBelowNearlyOpaque)
                .isFalse();
        }
    }

    @Nested
    class WarnOnceAboutUnreachableIntelPanel {

        // The case the warning exists for: the tab the panel lives on is up, so the walk had
        // something to find and came back empty anyway. Without this line a game build that
        // reshaped the intel tab would stop every intel-screen overlay in silence.
        @Test
        void warnsWhenTheIntelTabIsOpen() {

            // ARRANGE
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.INTEL);

            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());

            // ACT
            var capture = LogAppenderFake.captureLogOf(
                VanillaIntelScreenView.class,
                intelScreen::warnOnceAboutUnreachableIntelPanel);

            // ASSERT
            assertThat(capture.getMessages())
                .hasSize(1);
        }

        // The ordinary state on every other screen: the tab that is up holds no events panel, so
        // an unreached panel is the expected answer rather than news.
        @Test
        void staysSilentWhenAnotherTabIsShowing() {

            // ARRANGE
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.MAP);

            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());

            // ACT
            var capture = LogAppenderFake.captureLogOf(
                VanillaIntelScreenView.class,
                intelScreen::warnOnceAboutUnreachableIntelPanel);

            // ASSERT
            assertThat(capture.getMessages())
                .isEmpty();
        }

        // A visor read runs per frame, so a build this reach no longer fits would otherwise
        // write the same line sixty times a second.
        @Test
        void warnsOnlyOnceWhileTheReachKeepsFailing() {

            // ARRANGE
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.INTEL);

            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());

            // ACT
            var capture = LogAppenderFake.captureLogOf(
                VanillaIntelScreenView.class,
                () -> {
                    intelScreen.warnOnceAboutUnreachableIntelPanel();
                    intelScreen.warnOnceAboutUnreachableIntelPanel();
                });

            // ASSERT
            assertThat(capture.getMessages())
                .hasSize(1);
        }

        // An open intel tab always holds the panel, so not finding it is never ordinary.
        @Test
        void filesThePanelMissingWhereTheIntelTabIsOpen() {

            // ARRANGE
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.INTEL);

            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());
            var expectedBrokenDetail = "EventsPanel below the intel tab";

            // ACT
            intelScreen.warnOnceAboutUnreachableIntelPanel();

            // ASSERT
            assertThat(reachRecord.takeReportedFailure().breakage().brokenDetail())
                .isEqualTo(expectedBrokenDetail);
        }

        @Test
        void filesNothingWhereAnotherTabIsShowing() {

            // ARRANGE
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.MAP);

            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());

            // ACT
            intelScreen.warnOnceAboutUnreachableIntelPanel();

            // ASSERT
            assertThat(reachRecord.hasReported())
                .isFalse();
        }
    }

    @Nested
    class WarnOnceAboutUnreadableCoreUi {

        // The cause is the whole of the diagnosis: the visor reads answer null either way, so
        // without it a reach that failed outright looks exactly like a screen holding no intel
        // panel. It is carried into the line because nothing else in the class records it.
        @Test
        void warnsWithTheCauseThatBrokeTheWalk() {

            // ARRANGE
            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());
            var unreadableTree = new IllegalStateException("A hop the core UI no longer offers.");

            // ACT
            var capture = LogAppenderFake.captureLogOf(
                VanillaIntelScreenView.class,
                () -> intelScreen.warnOnceAboutUnreadableCoreUi(unreadableTree));

            // ASSERT
            assertThat(capture.getThrowables())
                .containsExactly(unreadableTree);
        }

        // Deliberately not read against which screen is up, unlike the unreachable-panel
        // warning. The hops that fail here are ones the core UI offers whatever tab it shows,
        // so a reach that cannot take them has stopped every read built on it, not the intel
        // screen's alone.
        @Test
        void warnsWhateverTabIsShowing() {

            // ARRANGE
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.MAP);

            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());

            // ACT
            var capture = LogAppenderFake.captureLogOf(
                VanillaIntelScreenView.class,
                () -> intelScreen.warnOnceAboutUnreadableCoreUi(
                    new IllegalStateException("A broken reach.")));

            // ASSERT
            assertThat(capture.getThrowables())
                .hasSize(1);
        }

        // The walk runs per frame, so a reach that stopped working would otherwise write the
        // same line, stack and all, sixty times a second.
        @Test
        void warnsOnlyOnceWhileTheWalkKeepsFailing() {

            // ARRANGE
            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());

            // ACT
            var capture = LogAppenderFake.captureLogOf(
                VanillaIntelScreenView.class,
                () -> {
                    intelScreen.warnOnceAboutUnreadableCoreUi(new IllegalStateException("A broken reach."));
                    intelScreen.warnOnceAboutUnreadableCoreUi(new IllegalStateException("A broken reach."));
                });

            // ASSERT
            assertThat(capture.getThrowables())
                .hasSize(1);
        }

        // Two failures meaning different things, so neither one-shot may silence the other: a
        // build that broke the reach after an empty walk had already been reported would
        // otherwise say nothing about the thing that actually broke. Two lines, one of them
        // carrying the walk's cause and the other none.
        @Test
        void warnsSeparatelyFromTheUnreachablePanelWarning() {

            // ARRANGE
            when(campaignUiMock.getCurrentCoreTab())
                .thenReturn(CoreUITabId.INTEL);

            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());

            // ACT
            var capture = LogAppenderFake.captureLogOf(
                VanillaIntelScreenView.class,
                () -> {
                    intelScreen.warnOnceAboutUnreachableIntelPanel();
                    intelScreen.warnOnceAboutUnreadableCoreUi(new IllegalStateException("A broken reach."));
                });

            // ASSERT
            assertThat(capture.getMessages())
                .hasSize(2);
            assertThat(capture.getThrowables())
                .hasSize(1);
        }

        @Test
        void filesTheWalkWithTheCauseThatBrokeIt() {

            // ARRANGE
            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());
            var unreadableTree = new IllegalStateException("A hop the core UI no longer offers.");

            // ACT
            intelScreen.warnOnceAboutUnreadableCoreUi(unreadableTree);

            // ASSERT
            assertThat(reachRecord.takeReportedFailure().cause())
                .isSameAs(unreadableTree);
        }
    }

    @Nested
    class ReadIntelSubtabBrightness {

        @Test
        void answersTheFadersBrightness() {

            // ARRANGE
            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());

            // ACT
            var actualBrightness = intelScreen.readIntelSubtabBrightness(() -> 0.75);

            // ASSERT
            assertThat(actualBrightness)
                .isEqualTo(0.75f);
        }

        // Read from render passes with no catch of their own, so a renamed fader has to come
        // back as an unlit visor rather than end the frame.
        @Test
        void answersDarkWhereTheFaderNoLongerLinks() {

            // ARRANGE
            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());

            // ACT
            var actualBrightness = intelScreen.readIntelSubtabBrightness(
                CoreUiReachFailures::throwUnlinkedMember);

            // ASSERT
            assertThat(actualBrightness)
                .isZero();
        }

        @Test
        void warnsOnceWithTheCauseWhileTheFaderKeepsFailingToLink() {

            // ARRANGE
            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());
            var unlinkedFader = new NoSuchMethodError("getFader");

            // ACT
            var capture = LogAppenderFake.captureLogOf(
                VanillaIntelScreenView.class,
                () -> {
                    intelScreen.readIntelSubtabBrightness(() -> {
                        throw unlinkedFader;
                    });
                    intelScreen.readIntelSubtabBrightness(() -> {
                        throw unlinkedFader;
                    });
                });

            // ASSERT
            assertThat(capture.getThrowables())
                .containsExactly(unlinkedFader);
        }

        // A different member from any the walk takes, so a walk already reported broken must
        // not silence news of the fader.
        @Test
        void warnsSeparatelyFromTheWalksWarnings() {

            // ARRANGE
            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());

            // ACT
            var capture = LogAppenderFake.captureLogOf(
                VanillaIntelScreenView.class,
                () -> {
                    intelScreen.warnOnceAboutUnreadableCoreUi(new IllegalStateException("A broken reach."));
                    intelScreen.readIntelSubtabBrightness(CoreUiReachFailures::throwUnlinkedMember);
                });

            // ASSERT
            assertThat(capture.getThrowables())
                .hasSize(2);
        }

        @Test
        void filesTheFaderThatNoLongerLinks() {

            // ARRANGE
            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());
            var expectedBrokenDetail = "EventsPanel.getFader";

            // ACT
            intelScreen.readIntelSubtabBrightness(CoreUiReachFailures::throwUnlinkedMember);

            // ASSERT
            assertThat(reachRecord.takeReportedFailure().breakage().brokenDetail())
                .isEqualTo(expectedBrokenDetail);
        }

        @Test
        void filesNothingForAFaderThatReads() {

            // ARRANGE
            var intelScreen = new VanillaIntelScreenView(reachRecord.getReporter());

            // ACT
            intelScreen.readIntelSubtabBrightness(() -> 0.75);

            // ASSERT
            assertThat(reachRecord.hasReported())
                .isFalse();
        }
    }
}
