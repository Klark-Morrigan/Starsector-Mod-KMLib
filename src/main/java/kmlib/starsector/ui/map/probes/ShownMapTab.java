package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.logging.SessionWarning;
import kmlib.starsector.ui.coreui.CoreUiTree;
import kmlib.starsector.ui.intel.IntelScreenView;
import kmlib.starsector.ui.intel.VanillaIntelScreenView;

import org.apache.log4j.Logger;

/**
 * The map widget on screen, wherever the game is showing one: the {@code M} screen's own core tab,
 * or the intel screen's embedded map preview (its "map visor"). Code that has to reason about the
 * map's widget tree - what the map draws on, what the game lays over it - asks here for the widget
 * to root that reasoning at, instead of assuming the screen it is running on.
 *
 * <p>The two are the same class of widget in two places, which is what lets one answer serve both.
 * A caller therefore gets a map tab or nothing, never some other screen's tab: a rule about how a
 * map tab is laid out cannot be applied to, or complain about, a tab that is not one.
 *
 * <p>The {@code M} screen's tab is recognised by it being a {@link SectorMapAPI}, which is published
 * API and so survives the obfuscator's reshuffling of the tab classes' own names between game
 * builds. That is a test of what the widget <em>is</em> rather than of which core tab the campaign
 * UI reports, and the difference matters: the tab read answers for an interaction dialog's own core
 * UI while such a dialog is up, whereas the walk to the current tab goes through the main one, so
 * pairing the two could root the rule in a tab from a tree the read was never aimed at.
 *
 * <p>The visor is asked second and only when the current tab is not itself a map, because the intel
 * screen hosts its map below a tab that is not one. Its own read fails closed to "no visor" off that
 * screen, so no screen test is needed to keep this inert everywhere else.
 *
 * <p>Finding no map is the ordinary answer on every other screen and is reported quietly. Finding
 * none while the campaign UI says the map screen is up is not, and says so once at WARN: a caller
 * rooted here has to fail open, so a build where the tab stopped answering to this interface would
 * otherwise switch off everything built on it without a word.
 */
public final class ShownMapTab {

    private static final Logger LOG = Global.getLogger(ShownMapTab.class);

    // The live intel screen, since the visor is reached by walking the running game's widget tree.
    // Held here rather than taken per call so a caller in a render pass supplies nothing; the
    // package-private read below is the seam a caller with a view of its own uses instead.
    private static final IntelScreenView INTEL_SCREEN = new VanillaIntelScreenView();

    // Says once per session that this recognition no longer fits, rather than on every frame a
    // caller asks.
    private static final SessionWarning WARNING = new SessionWarning(LOG);

    private ShownMapTab() {
    }

    /**
     * The map widget the player is looking at.
     *
     * @return the {@code M} screen's map tab, the intel screen's lit map visor, or null when
     *         neither is on screen - which is every other screen, and is not a failure
     * @throws RuntimeException when the reach to the current tab is absent or fails outright, so a
     *                          caller applies its own policy to a genuinely broken reach
     */
    public static UIComponentAPI resolveShownMapTab() {
        var mapTab = resolveShownMapTab(CoreUiTree.resolveCurrentTab(), INTEL_SCREEN);
        if (mapTab == null) {
            warnOnceIfTheMapScreenIsUpAnyway();
        }
        return mapTab;
    }

    /**
     * The rule the live read applies, over readings taken elsewhere.
     *
     * @param currentTab  the core tab that is up, as read off the live tree
     * @param intelScreen the intel screen to fall back to
     * @return the map widget among the two, or null when neither is one
     */
    static UIComponentAPI resolveShownMapTab(Object currentTab, IntelScreenView intelScreen) {
        // Both conditions are load-bearing: being a map is what makes it the right root, and being
        // a component is what makes it measurable at all.
        if (currentTab instanceof SectorMapAPI && currentTab instanceof UIComponentAPI mapTab) {
            return mapTab;
        }
        return intelScreen.getMapVisorWidget();
    }

    // Names a map that should have been found and was not. Finding none is the ordinary answer on
    // every screen that shows no map, and worth nothing; finding none while the campaign UI says the
    // map screen is the one up means a build where the tab no longer answers to the interface this
    // recognises it by - which would otherwise leave every rule rooted here inert in silence, since
    // a caller with no map tab has to fail open.
    //
    // The two reads are not aimed at the same core UI, which is the one benign way this can fire:
    // the tab read answers for an interaction dialog's own core UI while such a dialog is up,
    // whereas the walk always goes through the main one. So the message names the tree that was
    // actually searched rather than declaring the recognition broken.
    private static void warnOnceIfTheMapScreenIsUpAnyway() {
        // Tested before anything is read, so a session that has already said this costs a caller in
        // a render pass one field read per frame rather than a walk into the campaign UI.
        if (WARNING.hasWarnedThisSession()) {
            return;
        }
        var sector = Global.getSector();
        var campaignUi = sector == null ? null : sector.getCampaignUI();
        if (campaignUi == null || campaignUi.getCurrentCoreTab() != CoreUITabId.MAP) {
            return;
        }
        WARNING.warnOnce(
            "The map screen is up but the main core UI's current tab is not a SectorMapAPI; "
                + "rules about map-tab layout have no tab to root at while that is so.");
    }
}
