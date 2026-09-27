package kmlib.starsector.ui.map.presence;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.campaign.CampaignUIPersistentData;

import kmlib.logging.SessionWarning;
import kmlib.starsector.ui.coreui.CampaignScreenView;

import org.apache.log4j.Logger;

/**
 * Reads the live state of the vanilla campaign map (the {@code M} screen) so a mod can gate on
 * it: is the player looking at the sector (hyperspace) map, and is that map drawing the
 * stylised Starscape look or the ordinary schematic? The two halves are asked separately
 * because they gate different things - whether a companion overlay belongs on screen at all,
 * and which of two draw paths the game will honour while it is.
 *
 * <p>The Starscape state read here is the campaign's persisted filter, which is the object the
 * core-UI map tab binds to: that tab is built without filter params and so falls back to the
 * persisted one, standalone and dialog-hosted alike. The reads therefore agree with what that
 * host actually draws, rather than merely approximating it. Maps built with their own filter -
 * every panel-embedded preview, and anything opened through {@code MapParams} - are a different
 * object and are not what this answers for; they are also not the map tab, so the tab read
 * excludes them. The exception worth knowing is a map tab opened *with* such params, where the
 * persisted filter and the one that tab draws from can differ.
 *
 * <p>The active tab comes from {@link CampaignScreenView#resolveShownCoreTab()} - the published
 * read, corrected for an interaction dialog that goes on handing out the core UI of a screen the
 * player has closed, which would otherwise leave this reporting the map long after they left it.
 * The sub-view and Starscape-filter state live on the game's concrete campaign-UI-data class,
 * {@link CampaignUIPersistentData}, which the API jar does not publish. The port casts to it
 * directly - the game's script classloader denies {@code java.lang.reflect} to mod code, so
 * a reflective read is impossible, while loading core classes is permitted and the class is
 * marked do-not-obfuscate, keeping its name stable across game builds. A UI-data object of
 * any other type resolves to "not the sector map", and so does one of that type whose members a
 * game build has changed, so an unreadable signal hides the overlay rather than misplacing it.
 */
public final class CampaignMapView {

    private static final Logger LOG = Global.getLogger(CampaignMapView.class);

    // Once per session: the cast the whole port relies on cannot land, or lands on a class that
    // no longer carries what is read off it - a genuine anomaly (a wrapping mod, the class loaded
    // under a different classloader, or a game build that changed it) worth naming once, not per
    // render frame. One warning for both, being the same news: the map's state cannot be read.
    private static final SessionWarning UNREADABLE_UI_DATA_WARNING = new SessionWarning(LOG);

    private CampaignMapView() {
    }

    /**
     * @return whether the sector map is the active core tab and the Sector (not System) sub-view
     *         is showing, whichever way the Starscape filter is set
     */
    public static boolean isSectorMapShowing() {
        return resolveSectorMapState().isShowing();
    }

    /**
     * @return whether the sector map is showing with the Starscape filter on, the state in which
     *         the game paints the stylised Starscape look in place of the ordinary map
     */
    public static boolean isSectorMapInStarscapeMode() {
        return resolveSectorMapState() == SectorMapState.SHOWING_IN_STARSCAPE_MODE;
    }

    /**
     * @return whether the sector map is showing with the Starscape filter off
     */
    public static boolean isSectorMapWithStarscapeOff() {
        return resolveSectorMapState() == SectorMapState.SHOWING_WITH_STARSCAPE_OFF;
    }

    /**
     * Renders the raw signals behind the reads above, and the state they classify to, as one
     * compact line, e.g.
     * {@code "tab=MAP starscape=true mapLocation=hyperspace state=SHOWING_IN_STARSCAPE_MODE"}.
     * Every read fails closed by design, so when an overlay is unexpectedly hidden this is the
     * surface a consumer logs to see what is blocking it.
     *
     * <p>The signals are re-read here rather than taken from the classified state, because the
     * two say different things: the state is what the reads act on, while the raw signals name
     * the tab and location the classification threw away - which is what turns "not showing"
     * into a reason. Printing both is also what makes them checkable against each other, so a
     * line reading {@code starscape=false} beside a state of {@code NOT_SHOWING} says plainly
     * that the sub-view is what closed the gate and not the filter.
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

        var uiSignals = readMapUiSignals();
        var isStarscapeOn = uiSignals == null ? null : uiSignals.isStarscapeOn();
        var mapLocation = uiSignals == null ? null : uiSignals.mapLocation();

        return "tab="
            + CampaignScreenView.resolveShownCoreTab()
            + " starscape="
            + (isStarscapeOn == null ? "unreadable" : isStarscapeOn)
            + " mapLocation="
            + describeLocation(mapLocation)
            + " state="
            + resolveSectorMapState();
    }

    /**
     * Classifies the three signals the reads above decide from - active tab, map sub-view, and the
     * Starscape filter - in one pass, so those reads differ only in which state they accept and
     * cannot disagree about whether the map is showing. Resolving the signals once per read is what
     * would let the tab and sub-view halves drift as the reads are edited.
     *
     * <p>Published alongside the reads it backs, for a caller that has to hold the answer rather
     * than ask a question of it - one enumerated value carries the whole of the map's state, where
     * keeping the same answer as booleans would take three of them and admit combinations no map is
     * ever in.
     *
     * @return the state the live signals classify to, or {@link SectorMapState#NOT_SHOWING} when
     *         any of them cannot be read
     */
    public static SectorMapState resolveSectorMapState() {

        var sector = Global.getSector();
        if (sector == null) {
            return SectorMapState.NOT_SHOWING;
        }

        // The shown tab being MAP is the whole "the map is the active view" signal. Do not also
        // gate on isShowingDialog(): the map is routinely viewed in a dialog-active context
        // (opened from an interaction), where that flag is true the entire time, so gating on it
        // would hide the overlay on the very screen it belongs to. The corrected read is what
        // makes that safe - the raw one goes on naming the map after the player closes it there.
        // It fails closed to no tab, so a campaign UI that is not up yet needs no test of its own.
        if (CampaignScreenView.resolveShownCoreTab() != CoreUITabId.MAP) {
            return SectorMapState.NOT_SHOWING;
        }

        var uiSignals = readMapUiSignals();
        if (uiSignals == null) {
            return SectorMapState.NOT_SHOWING;
        }
        var mapLocation = uiSignals.mapLocation();

        // A null location means the map has not recorded a sub-view yet; it opens on the
        // player's current location, so read it as the Sector view until proven a system.
        var isSectorSubViewShowing = mapLocation == null || mapLocation.isHyperspace();
        if (!isSectorSubViewShowing) {
            return SectorMapState.NOT_SHOWING;
        }

        if (uiSignals.isStarscapeOn() == null) {
            return SectorMapState.SHOWING_WITH_UNREADABLE_FILTER;
        }

        return uiSignals.isStarscapeOn()
            ? SectorMapState.SHOWING_IN_STARSCAPE_MODE
            : SectorMapState.SHOWING_WITH_STARSCAPE_OFF;
    }

    // The one place the concrete-class coupling lives: null when the game hands back some
    // other UI-data type, or a build whose class no longer carries what is read off it -
    // either of which downstream reads as unreadable signals. Named once in the log, since
    // that is the failure mode that silently hides every map-gated overlay, and the actual
    // class or missing member is what diagnoses it.
    //
    // The LinkageError is caught here rather than left to the callers, because they are render
    // passes and per-frame reads with no catch of their own: a member this build dropped would
    // otherwise end the frame rather than hide the overlay.
    private static MapUiSignals readMapUiSignals() {

        try {
            var uiData = Global.getSector().getUIData();

            if (uiData instanceof CampaignUIPersistentData concreteUiData) {
                var filterData = concreteUiData.getMapFilterData();

                return new MapUiSignals(
                    concreteUiData.getCampaignMapLocation(),
                    filterData == null ? null : filterData.starscape);
            }
            if (uiData != null) {
                UNREADABLE_UI_DATA_WARNING.warnOnce("Campaign UI data is a "
                    + uiData.getClass().getName()
                    + ", not CampaignUIPersistentData; map-gated overlays stay hidden");
            }
            return null;

        } catch (LinkageError linkageError) {

            UNREADABLE_UI_DATA_WARNING.warnOnce(
                "CampaignUIPersistentData no longer carries what the map's state is read from; "
                    + "map-gated overlays stay hidden",
                linkageError);
            return null;
        }
    }

    // A null location is a real state (map not opened yet), so it prints as "null" rather
    // than being folded into the unreadable case.
    private static String describeLocation(LocationAPI location) {

        if (location == null) {
            return "null";
        }
        return location.isHyperspace() ? "hyperspace" : location.getId();
    }

    // The two signals the concrete UI data carries, taken off it together so nothing past the one
    // read holds that class. A null filter state is the object answering none; a null location is
    // a map not opened yet.
    private record MapUiSignals(
        LocationAPI mapLocation,
        Boolean isStarscapeOn) {
    }
}
