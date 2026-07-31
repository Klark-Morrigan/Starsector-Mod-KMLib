package kmlib.starsector.ui.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.campaign.CampaignState;
import com.fs.starfarer.campaign.comms.F;
import com.fs.starfarer.campaign.comms.v2.EventsPanel;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.layout.VanillaPositions;

import org.apache.log4j.Logger;

/**
 * {@link IntelScreenView} binding backed by the live campaign UI. The tab-open read is published
 * API; the map visor rectangle and its starscape state reach the game's concrete intel panel by
 * casting to it - the game's script classloader denies {@code java.lang.reflect} to mod code, while
 * loading and casting to core classes is permitted, and the classes touched are marked
 * do-not-obfuscate, so their names stay stable across game builds.
 *
 * <p>The reach into the concrete panel fails closed: a missing or unexpected link, a sibling sub-tab
 * showing instead, or a blanked preview all resolve to "no visor", so a caller reading the visor
 * rectangle simply gets {@code null} while there is nothing on the intel screen to draw over. The
 * starscape read fails the same way to "not in starscape mode", which is what an unreachable panel
 * is leaving the game doing anyway.
 */
public final class VanillaIntelScreenView implements IntelScreenView {
    private static final Logger LOG = Global.getLogger(VanillaIntelScreenView.class);

    // The preview widget's opacity is hard-set to 1.0 while it is showing and 0.0 when a
    // large-description item blanks it, so any threshold between the two reads "is the preview lit".
    private static final float MAP_WIDGET_VISIBLE_MIN_OPACITY = 0.9f;

    // The sub-tab switch forces the panel faders fully in and out rather than easing them, so the
    // events panel's brightness is only ever dark or lit and any threshold between the two reads
    // "is the intel sub-tab the one showing".
    private static final float INTEL_SUBTAB_SHOWING_MIN_BRIGHTNESS = 0.5f;

    // One-shot: a campaign UI of a type other than CampaignState (a wrapping mod) means the cast
    // the panel reach relies on cannot land - a genuine anomaly worth naming once, not on every
    // frame a visor read is attempted.
    private boolean hasLoggedUnexpectedCampaignUiType;

    @Override
    public boolean isIntelTabOpen() {
        CampaignUIAPI campaignUi = readCampaignUi();
        return campaignUi != null
            && campaignUi.getCurrentCoreTab() == CoreUITabId.INTEL;
    }

    @Override
    public Rectangle getMapVisorRect() {
        EventsPanel intelPanel = resolveIntelPanel();
        if (intelPanel == null) {
            return null;
        }
        var mapWidget = intelPanel.getMap();
        if (mapWidget == null) {
            return null;
        }
        if (!isMapVisorLit(intelPanel.getFader().getBrightness(), mapWidget.getOpacity())) {
            return null;
        }
        PositionAPI position = mapWidget.getPosition();
        if (position == null) {
            return null;
        }
        return VanillaPositions.toRectangle(position);
    }

    @Override
    public boolean isMapStarscapeModeOn() {
        EventsPanel intelPanel = resolveIntelPanel();
        if (intelPanel == null) {
            return false;
        }
        // Two steps in: the panel's map member is the framed holder widget, and the map inside it is
        // what owns the filter state. Its own starscape read is used rather than the raw filter flag,
        // so this says exactly what the game says - the filter alone is not starscape mode, which
        // also needs the map to be showing hyperspace.
        var mapWidget = intelPanel.getMap();
        var map = mapWidget == null ? null : mapWidget.getMap();
        return map != null && map.isStarscapeMode();
    }

    // Whether the two live signals add up to a lit map visor. Kept apart from the walk that fetches
    // them because they answer different questions: the walk is about reaching the game's widgets,
    // this is the rule about what their numbers mean, stated in the plain floats it actually needs.
    //
    // Both readings are load-bearing and neither implies the other. The intel core tab hosts three
    // sub-tabs - Intel, Planets, Factions - sharing one container, and switching between them only
    // fades the events panel out; it is never torn down and its map widget keeps full opacity behind
    // whichever sub-tab is up, so panel brightness is the only signal that says which sub-tab shows.
    // Conversely the Intel sub-tab can be showing with its preview blanked, which zeroes the map
    // widget's opacity while the panel stays lit. Either alone leaves nothing to draw over.
    static boolean isMapVisorLit(float intelSubtabBrightness, float mapWidgetOpacity) {
        return intelSubtabBrightness >= INTEL_SUBTAB_SHOWING_MIN_BRIGHTNESS
            && mapWidgetOpacity >= MAP_WIDGET_VISIBLE_MIN_OPACITY;
    }

    private CampaignUIAPI readCampaignUi() {
        var sector = Global.getSector();
        return sector == null ? null : sector.getCampaignUI();
    }

    // Walks the live core UI to the intel panel: campaign UI -> core -> current tab. The current
    // tab is the intel container F only while the intel tab is showing, so an "off the intel tab"
    // state and a wrapping-mod anomaly both resolve to null here and the caller stays inert.
    private EventsPanel resolveIntelPanel() {
        CampaignUIAPI campaignUi = readCampaignUi();
        if (!(campaignUi instanceof CampaignState campaignState)) {
            warnOnceAboutUnexpectedCampaignUi(campaignUi);
            return null;
        }
        var core = campaignState.getCore();
        if (core == null) {
            return null;
        }
        if (!(core.getCurrentTab() instanceof F intelContainer)) {
            return null;
        }
        return intelContainer.getEventsPanel();
    }

    // A null campaign UI is an ordinary "no campaign yet" state, not the anomaly this names: the
    // warning is for a non-null UI of an unexpected type, the case that silently makes every visor
    // read do nothing and whose actual class is what diagnoses it.
    private void warnOnceAboutUnexpectedCampaignUi(CampaignUIAPI campaignUi) {
        if (campaignUi != null && !hasLoggedUnexpectedCampaignUiType) {
            hasLoggedUnexpectedCampaignUiType = true;
            LOG.warn("Campaign UI is a "
                + campaignUi.getClass().getName()
                + ", not CampaignState; intel-screen visor reads stay inert");
        }
    }
}
