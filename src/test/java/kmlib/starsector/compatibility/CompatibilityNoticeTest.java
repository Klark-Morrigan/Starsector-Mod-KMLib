package kmlib.starsector.compatibility;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
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
 * <p>The frames it must hold off on are the cases worth the most, because the game's message dialog
 * is dropped silently when asked for behind another: a notice that asked anyway would look right
 * here and reach no player. So a dialog up, a campaign UI not yet there and a sector not there at
 * all are each a case of their own, and each is followed by the frame that can show - so the hold
 * is a wait rather than a loss.
 */
final class CompatibilityNoticeTest {

    private static final String FAST_RENDERING = "fast-rendering";

    private static final String NEXERELIN = "nexerelin";

    private static final String LOST_FEATURE = "Overlays will not respond to the cursor this session.";

    private static final float ONE_FRAME = 0.016f;

    private final CompatibilityFailures failures = new CompatibilityFailures();

    private CampaignUIAPI campaignUiMock;
    private SectorAPI sectorMock;
    private CompatibilityNotice notice;

    @BeforeEach
    void setUp() {

        // Templates that expose their slots rather than the shipped wording, so what each failure
        // composes carries its own broken detail and two shown are told apart by it.
        var templatesByKey = Map.of(
            "compatibility_notice_title", "title[%s]",
            "compatibility_notice_built_against", "built[%s|%s|%s]",
            "compatibility_notice_built_against_unreadable", "unreadable[%s|%s]",
            "compatibility_notice_consequence", "consequence[%s]",
            "compatibility_notice_version_unknown", "?");
        StarsectorSettingsFake.installSettings((category, key) -> templatesByKey.get(key));

        campaignUiMock = mock(CampaignUIAPI.class);
        sectorMock = mock(SectorAPI.class);

        // A frame that can show by default - a campaign UI with no dialog up - so each case below
        // takes one signal away and what it observes is that signal.
        when(sectorMock.getCampaignUI())
            .thenReturn(campaignUiMock);
        when(campaignUiMock.isShowingDialog())
            .thenReturn(false);

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

            var failure = createFailure(LOST_FEATURE);
            failures.recordOnce(FAST_RENDERING, () -> failure);

            notice.advance(ONE_FRAME);

            verify(campaignUiMock)
                .showMessageDialog(failure.describeForPlayer());
        }

        @Test
        void showsAFailureOnceHoweverManyFramesFollow() {

            failures.recordOnce(FAST_RENDERING, () -> createFailure(LOST_FEATURE));

            notice.advance(ONE_FRAME);
            notice.advance(ONE_FRAME);
            notice.advance(ONE_FRAME);

            verify(campaignUiMock, times(1))
                .showMessageDialog(anyString());
        }

        @Test
        void showsNothingWhereNothingWasRecorded() {

            notice.advance(ONE_FRAME);

            verify(campaignUiMock, never())
                .showMessageDialog(anyString());
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

            failures.recordOnce(FAST_RENDERING, () -> createFailure(LOST_FEATURE));
            when(campaignUiMock.isShowingDialog())
                .thenReturn(true);

            notice.advance(ONE_FRAME);

            verify(campaignUiMock, never())
                .showMessageDialog(anyString());
        }

        @Test
        void showsTheHeldFailureOnTheFrameTheDialogIsDown() {

            failures.recordOnce(FAST_RENDERING, () -> createFailure(LOST_FEATURE));
            when(campaignUiMock.isShowingDialog())
                .thenReturn(true, false);

            notice.advance(ONE_FRAME);
            notice.advance(ONE_FRAME);

            verify(campaignUiMock, times(1))
                .showMessageDialog(anyString());
        }

        @Test
        void holdsTheFailureWhileTheSectorHasNoCampaignUi() {

            failures.recordOnce(FAST_RENDERING, () -> createFailure(LOST_FEATURE));
            when(sectorMock.getCampaignUI())
                .thenReturn(null, campaignUiMock);

            notice.advance(ONE_FRAME);
            notice.advance(ONE_FRAME);

            verify(campaignUiMock, times(1))
                .showMessageDialog(anyString());
        }

        @Test
        void showsNothingAndThrowsNothingWhereThereIsNoSector() {

            var noticeWithoutSector = new CompatibilityNotice(null, failures);
            failures.recordOnce(FAST_RENDERING, () -> createFailure(LOST_FEATURE));

            assertThatCode(() -> noticeWithoutSector.advance(ONE_FRAME))
                .doesNotThrowAnyException();
            verify(campaignUiMock, never())
                .showMessageDialog(anyString());
        }

        @Test
        void showsTwoFailuresOnTwoFramesRatherThanOne() {
            // The game drops a message dialog asked for behind another, so the second of two taken
            // together is shown on the next frame that can show it, not stacked on the first.
            var firstFailure = createFailure("first");
            var secondFailure = createFailure("second");
            failures.recordOnce(FAST_RENDERING, () -> firstFailure);
            failures.recordOnce(NEXERELIN, () -> secondFailure);

            notice.advance(ONE_FRAME);
            verify(campaignUiMock, times(1))
                .showMessageDialog(anyString());

            notice.advance(ONE_FRAME);
            InOrder shownInOrder = inOrder(campaignUiMock);
            shownInOrder.verify(campaignUiMock)
                .showMessageDialog(firstFailure.describeForPlayer());
            shownInOrder.verify(campaignUiMock)
                .showMessageDialog(secondFailure.describeForPlayer());
        }

        @Test
        void takesTheFailureOffTheRecordOnceShown() {

            failures.recordOnce(FAST_RENDERING, () -> createFailure(LOST_FEATURE));

            notice.advance(ONE_FRAME);

            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void survivesADialogCallThatThrowsAndDoesNotRetryIt() {

            failures.recordOnce(FAST_RENDERING, () -> createFailure(LOST_FEATURE));
            doThrow(new IllegalStateException("no screen panel"))
                .when(campaignUiMock)
                .showMessageDialog(anyString());

            assertThatCode(() -> notice.advance(ONE_FRAME))
                .doesNotThrowAnyException();
            notice.advance(ONE_FRAME);

            verify(campaignUiMock, times(1))
                .showMessageDialog(anyString());
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

            failures.recordOnce(FAST_RENDERING, () -> createFailure(LOST_FEATURE));
            notice.advance(ONE_FRAME);

            assertThat(notice.isDone())
                .isFalse();
        }
    }

    // The lost feature is what tells one recorded failure from another in the modal it composes;
    // everything else is one representative subject with neither version read.
    private static CompatibilityFailure createFailure(String lostFeature) {

        return new CompatibilityFailure(
            new CompatibilitySubject("Fast Rendering", null, null),
            lostFeature,
            "GLCommand is absent",
            null);
    }
}
