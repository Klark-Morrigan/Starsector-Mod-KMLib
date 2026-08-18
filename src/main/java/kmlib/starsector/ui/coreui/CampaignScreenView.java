package kmlib.starsector.ui.coreui;

import com.fs.starfarer.api.Global;

/**
 * Whether the campaign is showing the world itself with none of its screens over it - the state
 * this library calls <em>game space</em>: the player flying the sector, no core screen open and no
 * interaction dialog up.
 *
 * <p>The question exists because a hook can be reached in either state without being told which.
 * Anything drawn or read from such a hook has to decide whether the frame belongs to a screen the
 * player opened or to the world itself, and the two want opposite answers often enough that
 * guessing from what else is on screen is not available.
 *
 * <p>Two signals, both published API. A core tab is what the game reports while any core screen is
 * up, and it comes back null once the player leaves them, so its absence is the whole "no screen"
 * half. The dialog read is the second half rather than a refinement of it: a scripted dialog hosts
 * no core UI of its own, so it raises no tab while covering the world completely.
 *
 * <p>The pause menu is deliberately not read. It is raised over whatever was on screen without
 * taking that screen down, so it is neither a core screen nor a dialog and a frame under it is
 * still game space by the reading here. Whether that matters is the caller's: a caller that must
 * stand aside for the menu reads {@code CampaignUIAPI#isShowingMenu} for itself, and folding it in
 * here would hand every caller a menu rule it did not ask for and could not switch off.
 *
 * <p>Fails closed, so a campaign that is not stood up yet reports no game space. The reads are only
 * ever used to widen what a caller does, and a widening taken on an unreadable game would act on a
 * screen nobody can see.
 */
public final class CampaignScreenView {

    private CampaignScreenView() {
    }

    /**
     * @return whether the player is looking at the campaign world with no core screen open and no
     *         interaction dialog up; false while either is showing, and false before there is a
     *         campaign to read
     */
    public static boolean isShowingGameSpace() {
        var sector = Global.getSector();
        if (sector == null) {
            return false;
        }
        var campaignUi = sector.getCampaignUI();
        if (campaignUi == null) {
            return false;
        }
        return campaignUi.getCurrentCoreTab() == null && !campaignUi.isShowingDialog();
    }
}
