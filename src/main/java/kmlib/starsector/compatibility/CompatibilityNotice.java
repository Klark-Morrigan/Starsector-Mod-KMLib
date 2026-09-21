package kmlib.starsector.compatibility;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import org.apache.log4j.Logger;

import java.util.Objects;

/**
 * Tells the player, once per failed subject, that a binding to third-party code has stopped holding
 * - as the game's own message dialog, from a frame that can open one.
 *
 * <p>A failure is recorded where it happens, which is a render pass or a load step: neither is a
 * place a dialog can be opened from, and at load there may be no campaign UI to open it on at all.
 * So the record and the report are two steps, and this is the second. It drains
 * {@link CompatibilityFailures} and shows what it took, holding each failure until a frame with a
 * campaign UI up and no other dialog on it. Transient rather than persisted: what it reports is a
 * fact about the jars installed this session, not about the save.
 *
 * <p>One modal per failure, one per frame. Each failure composes its own heading and versions, so
 * two joined into one dialog would read as one subject with two names. And the game's message
 * dialog is dropped, silently, when asked for while any dialog is up - which is what the gate on
 * the dialog check exists for, and why a second failure waits a frame rather than being shown on
 * the same one as the first: shown then, it would be the one dropped.
 *
 * <p>The log block is written before the dialog is opened, and whatever the dialog does. It is what
 * a report to the third party's author is written from, and it is also what the modal's own "see
 * the log" sentence promises is there - so it must not depend on a UI call that is itself a binding
 * to code outside this library.
 *
 * <p>Runs while paused, because the frames it needs are mostly paused ones: a failure found with the
 * map open is found under a pause, and the dialog it waits behind holds one.
 *
 * <p>Never throws. It runs every frame on the campaign's own thread, and a dialog call that faults
 * must not take the frame with it.
 */
public final class CompatibilityNotice implements EveryFrameScript {

    private static final Logger LOG = Global.getLogger(CompatibilityNotice.class);

    private final CompatibilityFailures failures;
    private final SectorAPI sector;

    // One-shot guard on the dialog call faulting: a fault that repeats would otherwise be logged on
    // every failure shown after it.
    private boolean hasLoggedDialogError;

    /**
     * @param sector   the sector whose campaign UI the dialog is opened on, read afresh every frame
     *                 because at load there may be none yet; {@code null} shows nothing
     * @param failures the session's record this drains
     */
    public CompatibilityNotice(SectorAPI sector, CompatibilityFailures failures) {

        this.sector = sector;
        this.failures = Objects.requireNonNull(
            failures,
            "A notice with no record to drain would run every frame and report nothing.");
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {

        // The healthy path is one empty check and a return, which is the registry's cheap read.
        if (!failures.hasUnreported()) {
            return;
        }

        var campaignUi = resolveCampaignUiReadyForDialog();
        if (campaignUi == null) {
            return;
        }
        // Taken only once a frame can show it, so a failure is never held outside the record: a
        // frame that cannot open a dialog leaves it where it was for the next one that can.
        showFailure(campaignUi, failures.takeNextUnreported());
    }

    // The campaign UI where a dialog can be opened this frame, or null where it cannot: no sector,
    // no UI on it yet, or a dialog already up - behind which the game would drop this one unshown.
    private CampaignUIAPI resolveCampaignUiReadyForDialog() {

        if (sector == null) {
            return null;
        }
        var campaignUi = sector.getCampaignUI();

        if (campaignUi == null || campaignUi.isShowingDialog()) {
            return null;
        }
        return campaignUi;
    }

    // Logged first and shown second, so what a report is written from exists whatever the
    // dialog call does: a faulting call loses the modal alone. The failure has already left the
    // record, which is what keeps a faulting dialog from being retried on it every frame.
    private void showFailure(CampaignUIAPI campaignUi, CompatibilityFailure failure) {

        LOG.error(failure.describeForLog(), failure.cause());

        try {
            campaignUi.showMessageDialog(failure.describeForPlayer());

        } catch (RuntimeException dialogFailure) {
            reportDialogFailureOnce(dialogFailure);
        }
    }

    private void reportDialogFailureOnce(RuntimeException dialogFailure) {

        if (hasLoggedDialogError) {
            return;
        }
        hasLoggedDialogError = true;
        LOG.error(
            "Could not show a compatibility notice to the player; "
                + "the failure it was for is the line above this one.",
            dialogFailure);
    }
}
