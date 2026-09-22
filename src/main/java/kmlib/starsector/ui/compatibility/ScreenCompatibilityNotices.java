package kmlib.starsector.ui.compatibility;

import com.fs.starfarer.api.Global;

import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.starsector.ui.map.probes.ShownMapTab;

import org.apache.log4j.Logger;

import java.util.function.Supplier;

/**
 * Reporting a compatibility failure on the screen it was found on, where that screen can hold a
 * notice, and leaving it for the campaign's own dialog where it cannot.
 *
 * <p>The two reporters drain one record and neither repeats the other. Whichever shows a failure
 * takes it, so the dialog never re-opens a notice the player already dismissed on the map, and a
 * failure this declines to show is still sitting on the record when the player returns to the
 * campaign.
 *
 * <p>Called from the game thread only. A binding can also fail on a deferred renderer's own thread,
 * and standing a panel in the game's widget tree from there would be a second bug rather than a
 * report - such a failure is recorded and left for the dialog, which is the reporter that runs
 * where it is safe to open one.
 */
public final class ScreenCompatibilityNotices {

    private static final Logger LOG = Global.getLogger(ScreenCompatibilityNotices.class);

    // The notice on screen, so a second failure found on the same screen does not stand a panel
    // over the one the player is reading. Held for the session like the record it drains, there
    // being one screen and one player in front of it.
    private static CompatibilityNoticePanel raisedNotice;

    private ScreenCompatibilityNotices() {
    }

    /**
     * Shows the oldest unreported failure on the screen in force, where there is one to show it on.
     *
     * <p>The panel is stood up before the failure is taken, so a raise that cannot find a screen
     * leaves the record exactly as it was. Once the failure is taken it is logged before it is
     * drawn, which is the promise the notice's own pointer at the log makes.
     *
     * @param failureRecord the record to report from
     * @return whether a notice was stood up, which is false whenever the caller should leave the
     *         failure for the campaign's dialog
     */
    public static boolean showPendingFailureOnScreen(CompatibilityFailures failureRecord) {

        return showPendingFailureOnScreen(failureRecord, ShownMapTab::resolveShownMapTab);
    }

    /**
     * The reporting rule, over a reach for the screen its caller supplies. Package-private so the
     * decision either side of the raise can be driven without walking a live widget tree - that
     * walk reports a screen it cannot find, and a caller exercising this rule is not asking it to.
     *
     * @param failureRecord      the record to report from
     * @param resolveShownMapTab the reach for the screen to stand a notice on
     * @return whether a notice was stood up
     */
    static boolean showPendingFailureOnScreen(
            CompatibilityFailures failureRecord,
            Supplier<Object> resolveShownMapTab) {

        // The healthy path, and the one a per-frame caller pays: one empty check on the record.
        if (!failureRecord.hasUnreported() || isNoticeAlreadyRaised()) {
            return false;
        }

        var notice = CompatibilityNoticePanel.raiseNoticeOnScreen(resolveShownMapTab);
        if (notice == null) {
            return false;
        }

        var failure = failureRecord.takeNextUnreported();
        if (failure == null) {

            // Another reporter took it between the check above and here. Nothing to show, so the
            // panel comes straight back off rather than standing empty over the screen.
            notice.dismissNotice();
            return false;
        }

        LOG.error(failure.describeForLog(), failure.cause());
        notice.showFailure(failure);
        raisedNotice = notice;

        return true;
    }

    // Whether a notice is still on screen. The panel takes itself down on its own - a button, the
    // escape key, or the screen going - so what is held here is only believed until it is asked.
    private static boolean isNoticeAlreadyRaised() {

        return raisedNotice != null && raisedNotice.isNoticeRaised();
    }
}
