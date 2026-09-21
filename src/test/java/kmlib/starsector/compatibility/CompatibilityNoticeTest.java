package kmlib.starsector.compatibility;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.testfixtures.logging.LogAppenderFake;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;
import kmlib.testfixtures.starsector.compatibility.CompatibilitySlotTemplates;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture.createFailure;
import static kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture.createFailureLosing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins when the notice opens the dialog and when it holds off: once per recorded failure, on a
 * frame with a campaign UI up and no dialog on it, and never on a frame that cannot show one.
 *
 * <p>The frames it must hold off on are the cases worth the most, because the game's dialog is
 * refused when asked for behind another: a notice that asked anyway would look right here and reach
 * no player. A dialog up and a campaign UI not yet there are each followed by the frame that can
 * show, so the hold is a wait rather than a loss; a sector not there at all is fixed at
 * construction, and holds for the session.
 *
 * <p>The log line is pinned apart from the dialog, because it is the line a report is written from
 * and the dialog call is a binding to code outside the library: the line has to be there whatever
 * that call did, whether it threw or simply answered that it would not open.
 */
final class CompatibilityNoticeTest {

    private static final String FAST_RENDERING = CompatibilityFailureFixture.FAST_RENDERING_SUBJECT_KEY;

    private static final String NEXERELIN = CompatibilityFailureFixture.NEXERELIN_SUBJECT_KEY;

    // One consumer throughout: what this suite is about is when a failure reaches a dialog, which
    // is the same question whichever mod recorded it.
    private static final CompatibilityConsumer MAP_OVERLAY = CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER;

    private static final float ONE_FRAME = 0.016f;

    private final CompatibilityFailures failures = new CompatibilityFailures();

    private CampaignUIAPI campaignUiMock;
    private SectorAPI sectorMock;
    private CompatibilityNotice notice;

    @BeforeEach
    void setUp() {

        CompatibilitySlotTemplates.installSlotTemplates();

        campaignUiMock = mock(CampaignUIAPI.class);
        sectorMock = mock(SectorAPI.class);

        // A frame that can show by default - a campaign UI with no dialog up, and a dialog call
        // that opens - so each case below takes one signal away and what it observes is that
        // signal. The opening has to be stated: a mock answers false, which is the refusal.
        when(sectorMock.getCampaignUI())
            .thenReturn(campaignUiMock);
        when(campaignUiMock.isShowingDialog())
            .thenReturn(false);
        when(openNoticeDialogOn(campaignUiMock))
            .thenReturn(true);

        notice = new CompatibilityNotice(sectorMock, failures);
    }

    @AfterEach
    void clearSettings() {

        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class Advance {

        @Test
        void showsTheRecordedFailureAsTheModalItComposes() {

            var failure = createFailure();
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> failure);

            notice.advance(ONE_FRAME);

            openNoticeDialogShowing(verify(campaignUiMock), failure.describeForPlayer());
        }

        @Test
        void showsAFailureOnceHoweverManyFramesFollow() {

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailure());

            notice.advance(ONE_FRAME);
            notice.advance(ONE_FRAME);
            notice.advance(ONE_FRAME);

            openNoticeDialogOn(verify(campaignUiMock, times(1)));
        }

        @Test
        void showsNothingWhereNothingWasRecorded() {

            notice.advance(ONE_FRAME);

            openNoticeDialogOn(verify(campaignUiMock, never()));
        }

        @Test
        void readsNothingOfTheCampaignUiWhereNothingWasRecorded() {

            // The healthy path is the one every frame of every session takes, and it must cost one
            // check on the record rather than a walk to the campaign UI.
            notice.advance(ONE_FRAME);

            verify(sectorMock, never())
                .getCampaignUI();
        }

        @Test
        void holdsTheFailureWhileADialogIsUp() {

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailure());
            when(campaignUiMock.isShowingDialog())
                .thenReturn(true);

            notice.advance(ONE_FRAME);

            openNoticeDialogOn(verify(campaignUiMock, never()));
        }

        @Test
        void showsTheHeldFailureOnTheFrameTheDialogIsDown() {

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailure());
            when(campaignUiMock.isShowingDialog())
                .thenReturn(true, false);

            notice.advance(ONE_FRAME);
            notice.advance(ONE_FRAME);

            openNoticeDialogOn(verify(campaignUiMock, times(1)));
        }

        @Test
        void holdsTheFailureWhileTheSectorHasNoCampaignUi() {

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailure());
            when(sectorMock.getCampaignUI())
                .thenReturn(null, campaignUiMock);

            notice.advance(ONE_FRAME);
            notice.advance(ONE_FRAME);

            openNoticeDialogOn(verify(campaignUiMock, times(1)));
        }

        @Test
        void showsNothingAndThrowsNothingWhereThereIsNoSector() {

            var noticeWithoutSector = new CompatibilityNotice(null, failures);
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailure());

            assertThatCode(() -> noticeWithoutSector.advance(ONE_FRAME))
                .doesNotThrowAnyException();
            openNoticeDialogOn(verify(campaignUiMock, never()));
        }

        @Test
        void showsTwoFailuresOnTwoFramesRatherThanOne() {

            // The game refuses a dialog asked for behind another, so the second of two taken
            // together is shown on the next frame that can show it, not stacked on the first.
            var firstFailure = createFailureLosing("The first feature stopped working.");
            var secondFailure = createFailureLosing("The second feature stopped working.");
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> firstFailure);
            failures.recordOnce(NEXERELIN, MAP_OVERLAY, recordedAs -> secondFailure);

            notice.advance(ONE_FRAME);
            openNoticeDialogOn(verify(campaignUiMock, times(1)));

            notice.advance(ONE_FRAME);
            InOrder shownInOrder = inOrder(campaignUiMock);
            openNoticeDialogShowing(shownInOrder.verify(campaignUiMock), firstFailure.describeForPlayer());
            openNoticeDialogShowing(shownInOrder.verify(campaignUiMock), secondFailure.describeForPlayer());
        }

        @Test
        void takesTheFailureOffTheRecordOnceShown() {

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailure());

            notice.advance(ONE_FRAME);

            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void logsTheFailuresLogLineWhenItShowsIt() {

            var failure = CompatibilityFailureFixture.createFailure();
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> failure);

            var appenderFake = LogAppenderFake.captureLogOf(
                CompatibilityNotice.class,
                () -> notice.advance(ONE_FRAME));

            assertThat(appenderFake.getMessages())
                .containsExactly(failure.describeForLog());
        }

        @Test
        void logsTheFailuresLogLineWhereTheDialogCallThrows() {

            var failure = CompatibilityFailureFixture.createFailure();
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> failure);
            when(openNoticeDialogOn(campaignUiMock))
                .thenThrow(new IllegalStateException("no screen panel"));

            var appenderFake = LogAppenderFake.captureLogOf(
                CompatibilityNotice.class,
                () -> notice.advance(ONE_FRAME));

            assertThat(appenderFake.getMessages())
                .startsWith(failure.describeForLog());
        }

        @Test
        void logsTheRefusalWhereTheGameWouldNotOpenTheDialog() {

            // The dialog answers whether it opened, where the message dialog before it answered
            // nothing and was dropped in silence. The failure's own line is in the log either way;
            // this is what says no player saw it.
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailure());
            when(openNoticeDialogOn(campaignUiMock))
                .thenReturn(false);

            var appenderFake = LogAppenderFake.captureLogOf(
                CompatibilityNotice.class,
                () -> notice.advance(ONE_FRAME));

            assertThat(appenderFake.getMessages())
                .anyMatch(message -> message.startsWith("The game would not open a compatibility notice"));
        }

        @Test
        void survivesADialogCallThatThrowsAndDoesNotRetryIt() {

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailure());
            when(openNoticeDialogOn(campaignUiMock))
                .thenThrow(new IllegalStateException("no screen panel"));

            assertThatCode(() -> notice.advance(ONE_FRAME))
                .doesNotThrowAnyException();
            notice.advance(ONE_FRAME);

            openNoticeDialogOn(verify(campaignUiMock, times(1)));
        }

        @Test
        void logsADialogFaultOnceHoweverManyFailuresItStrikes() {

            failures.recordOnce(
                FAST_RENDERING,
                MAP_OVERLAY,
                recordedAs -> createFailureLosing("The first feature stopped working."));
            failures.recordOnce(
                NEXERELIN,
                MAP_OVERLAY,
                recordedAs -> createFailureLosing("The second feature stopped working."));
            when(openNoticeDialogOn(campaignUiMock))
                .thenThrow(new IllegalStateException("no screen panel"));

            var appenderFake = LogAppenderFake.captureLogOf(
                CompatibilityNotice.class,
                () -> {
                    notice.advance(ONE_FRAME);
                    notice.advance(ONE_FRAME);
                });

            // Two failure lines and one fault line: the fault is a standing state of the dialog
            // call, not news on every failure it strikes.
            assertThat(appenderFake.getMessages())
                .hasSize(3)
                .filteredOn(message -> message.startsWith("Could not show a compatibility notice"))
                .hasSize(1);
        }
    }

    @Nested
    class RunWhilePaused {

        @Test
        void isTrueBecauseTheFramesThatCanShowAreMostlyPausedOnes() {

            assertThat(notice.runWhilePaused())
                .isTrue();
        }
    }

    @Nested
    class IsDone {

        @Test
        void isFalseForTheWholeSession() {

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailure());
            notice.advance(ONE_FRAME);

            assertThat(notice.isDone())
                .isFalse();
        }
    }

    // The dialog call as a case stubs or verifies it, whatever it was asked to show. Seven
    // arguments, of which only the text is this suite's business: the panel's size and its one
    // button are the notice's own, and a case restating them would be pinning them twice.
    private static boolean openNoticeDialogOn(CampaignUIAPI campaignUi) {

        return campaignUi.showConfirmDialog(
            anyString(), anyString(), any(), anyFloat(), anyFloat(), any(), any());
    }

    // The same call, narrowed to the one modal a case means.
    private static boolean openNoticeDialogShowing(CampaignUIAPI campaignUi, String modalText) {

        return campaignUi.showConfirmDialog(
            eq(modalText), anyString(), any(), anyFloat(), anyFloat(), any(), any());
    }
}
