package kmlib.starsector.ui.coreui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;

/**
 * Which screen the campaign is showing: the core screen the player has open, and whether they are
 * looking at the world itself with none over it - the state this library calls <em>game space</em>,
 * meaning the player flying the sector, no core screen open and no interaction dialog up.
 *
 * <p>The question exists because a hook can be reached in either state without being told which.
 * Anything drawn or read from such a hook has to decide whether the frame belongs to a screen the
 * player opened or to the world itself, and the two want opposite answers often enough that
 * guessing from what else is on screen is not available.
 *
 * <p>The core tab is published API, with one correction. While an interaction dialog is up, the tab
 * the game reports is read off the core UI that dialog stands up, and that core is never closed the
 * way the campaign's own is: shutting a screen opened from a dialog only fades the panel out, so it
 * goes on naming the tab it last showed for the rest of the visit. Read raw, a player who opens the
 * intel screen while docked and closes it again leaves every screen-gated overlay believing the
 * intel screen is still up. So a reported tab stands only while the core UI it came from is still
 * on screen. Nothing corrects the campaign's own core, which does close its tab, and a dialog
 * hosting no core UI at all leaves the reading alone.
 *
 * <p>Game space is that corrected tab plus one more signal, rather than a refinement of it: a
 * scripted dialog hosts no core UI, so it raises no tab while covering the world completely.
 *
 * <p>The pause menu is deliberately not read. It is raised over whatever was on screen without
 * taking that screen down, so it is neither a core screen nor a dialog and a frame under it is
 * still game space by the reading here. Whether that matters is the caller's: a caller that must
 * stand aside for the menu reads {@code CampaignUIAPI#isShowingMenu} for itself, and folding it in
 * here would hand every caller a menu rule it did not ask for and could not switch off.
 *
 * <p>Fails closed, so a campaign that is not stood up yet reports no screen and no game space. The
 * reads are only ever used to widen what a caller does, and a widening taken on an unreadable game
 * would act on a screen nobody can see.
 */
public final class CampaignScreenView {

    private CampaignScreenView() {
    }

    /**
     * The core screen the player has open.
     *
     * @return the core tab on screen, or null when none is - including while a dialog is still
     *         handing out the core UI of a screen the player has closed, and before there is a
     *         campaign to read
     */
    public static CoreUITabId resolveShownCoreTab() {
        
        var campaignUi = readCampaignUi();
        if (campaignUi == null) {
            return null;
        }

        var reportedTab = campaignUi.getCurrentCoreTab();
        if (reportedTab == null) {
            return null;
        }

        // A dialog that hosts a core UI is where the reported tab was read from, so the reading is
        // only as current as that core. A dialog hosting none is left alone: the tab then comes
        // from the campaign's own core, which closes its tab when the player leaves a screen.
        var dialogCore = CoreUiTree.readCoreUiOf(campaignUi.getCurrentInteractionDialog());
        if (dialogCore != null && !CoreUiTree.isComponentShowing(dialogCore)) {
            return null;
        }

        return reportedTab;
    }

    /**
     * @return whether the player is looking at the campaign world with no core screen open and no
     *         interaction dialog up; false while either is showing, and false before there is a
     *         campaign to read
     */
    public static boolean isShowingGameSpace() {

        var campaignUi = readCampaignUi();
        if (campaignUi == null) {
            return false;
        }

        return resolveShownCoreTab() == null
            && !campaignUi.isShowingDialog();
    }

    private static CampaignUIAPI readCampaignUi() {

        var sector = Global.getSector();
        return sector == null
            ? null
            : sector.getCampaignUI();
    }
}
