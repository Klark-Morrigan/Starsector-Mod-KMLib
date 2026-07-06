package kmlib.starsector.ui.map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.campaign.CampaignUIPersistentData;

import org.apache.log4j.Logger;

/**
 * Reads the live state of the vanilla campaign map (the {@code M} screen) so a mod can gate
 * a custom overlay on it: is the player looking at the sector (hyperspace) map with the
 * Starscape filter off? Custom terrain painting only shows there, so that is the one moment
 * a companion overlay belongs on screen.
 *
 * <p>The active tab is published API ({@code getCurrentCoreTab}); the sub-view and
 * Starscape-filter state live on the game's concrete campaign-UI-data class,
 * {@link CampaignUIPersistentData}, which the API jar does not publish. The port casts to it
 * directly - the game's script classloader denies {@code java.lang.reflect} to mod code, so
 * a reflective read is impossible, while loading core classes is permitted and the class is
 * marked do-not-obfuscate, keeping its name stable across game builds. A UI-data object of
 * any other type resolves to "not the sector map", so an unreadable signal hides the overlay
 * rather than misplacing it.
 */
public final class CampaignMapView {
    private static final Logger LOG = Global.getLogger(CampaignMapView.class);

    // One-shot: getUIData() returning a type other than CampaignUIPersistentData means the
    // cast the whole port relies on cannot land - a genuine anomaly (a wrapping mod, or the
    // class loaded under a different classloader) worth naming once, not per render frame.
    private static boolean hasLoggedUnexpectedUiDataType;

    private CampaignMapView() {
    }

    /**
     * @return whether the sector map is the active core tab, the Sector (not System) sub-view
     *         is showing, and the Starscape filter is off
     */
    public static boolean isSectorMapWithStarscapeOff() {
        var sector = Global.getSector();
        if (sector == null) {
            return false;
        }
        CampaignUIAPI campaignUi = sector.getCampaignUI();
        // getCurrentCoreTab() == MAP is the whole "the map is the active view" signal. Do not
        // also gate on isShowingDialog(): the map is routinely viewed in a dialog-active
        // context (opened from an interaction), where that flag is true the entire time, so
        // gating on it would hide the overlay on the very screen it belongs to.
        if (campaignUi == null || campaignUi.getCurrentCoreTab() != CoreUITabId.MAP) {
            return false;
        }
        var uiData = readConcreteUiData();
        if (uiData == null) {
            return false;
        }
        var filterData = uiData.getMapFilterData();
        if (filterData == null || filterData.starscape) {
            return false;
        }
        var mapLocation = uiData.getCampaignMapLocation();
        // A null location means the map has not recorded a sub-view yet; it opens on the
        // player's current location, so read it as the Sector view until proven a system.
        return mapLocation == null || mapLocation.isHyperspace();
    }

    /**
     * Renders the raw signals behind {@link #isSectorMapWithStarscapeOff()} as one compact
     * line, e.g. {@code "tab=MAP starscape=true mapLocation=hyperspace"}. The gate fails
     * closed by design, so when an overlay is unexpectedly hidden this is the surface a
     * consumer logs to see which signal is blocking it.
     *
     * @return the current view-state signals, or a short reason when they cannot be read
     */
    public static String describeViewState() {
        var sector = Global.getSector();
        if (sector == null) {
            return "no sector";
        }
        CampaignUIAPI campaignUi = sector.getCampaignUI();
        if (campaignUi == null) {
            return "no campaign UI";
        }
        var uiData = readConcreteUiData();
        var filterData = uiData == null ? null : uiData.getMapFilterData();
        var mapLocation = uiData == null ? null : uiData.getCampaignMapLocation();
        return "tab=" + campaignUi.getCurrentCoreTab()
                + " starscape=" + (filterData == null ? "unreadable" : filterData.starscape)
                + " mapLocation=" + describeLocation(mapLocation);
    }

    // The one place the concrete-class coupling lives: null when the game hands back some
    // other UI-data type, which downstream reads as unreadable signals. A non-null value of
    // an unexpected type is named once in the log, since that is the failure mode that
    // silently hides every map-gated overlay and the actual class is what diagnoses it.
    private static CampaignUIPersistentData readConcreteUiData() {
        var uiData = Global.getSector().getUIData();
        if (uiData instanceof CampaignUIPersistentData concreteUiData) {
            return concreteUiData;
        }
        if (uiData != null && !hasLoggedUnexpectedUiDataType) {
            hasLoggedUnexpectedUiDataType = true;
            LOG.warn("Campaign UI data is a " + uiData.getClass().getName()
                    + ", not CampaignUIPersistentData; map-gated overlays stay hidden");
        }
        return null;
    }

    // A null location is a real state (map not opened yet), so it prints as "null" rather
    // than being folded into the unreadable case.
    private static String describeLocation(LocationAPI location) {
        if (location == null) {
            return "null";
        }
        return location.isHyperspace() ? "hyperspace" : location.getId();
    }
}
