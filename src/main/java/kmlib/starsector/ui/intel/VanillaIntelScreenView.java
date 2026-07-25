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
 * API; the visor rectangle reaches the game's concrete intel panel by casting to it - the game's
 * script classloader denies {@code java.lang.reflect} to mod code, while loading and casting to
 * core classes is permitted, and the classes touched are marked do-not-obfuscate, so their names
 * stay stable across game builds.
 *
 * <p>The reach into the concrete panel fails closed: a missing or unexpected link, or a blanked
 * preview, resolves to "no visor", so a caller reading the visor rectangle simply gets {@code null}
 * while there is nothing on the intel screen to draw over.
 */
public final class VanillaIntelScreenView implements IntelScreenView {
    private static final Logger LOG = Global.getLogger(VanillaIntelScreenView.class);

    // The preview widget's opacity is hard-set to 1.0 while it is showing and 0.0 when a
    // large-description item blanks it, so any threshold between the two reads "is the visor lit".
    private static final float VISOR_VISIBLE_MIN_OPACITY = 0.5f;

    // One-shot: a campaign UI of a type other than CampaignState (a wrapping mod) means the cast
    // the panel reach relies on cannot land - a genuine anomaly worth naming once, not on every
    // frame a visor read is attempted.
    private boolean hasLoggedUnexpectedCampaignUiType;

    @Override
    public boolean isIntelTabOpen() {
        CampaignUIAPI campaignUi = readCampaignUi();
        return campaignUi != null && campaignUi.getCurrentCoreTab() == CoreUITabId.INTEL;
    }

    @Override
    public Rectangle getVisorRect() {
        EventsPanel intelPanel = resolveIntelPanel();
        if (intelPanel == null) {
            return null;
        }
        var mapWidget = intelPanel.getMap();
        // A blanked preview (opacity 0, set when a large-description item is selected) has no lit
        // canvas to draw over, so it reports as no visor even though the widget is still laid out.
        if (mapWidget == null || mapWidget.getOpacity() < VISOR_VISIBLE_MIN_OPACITY) {
            return null;
        }
        PositionAPI position = mapWidget.getPosition();
        if (position == null) {
            return null;
        }
        return VanillaPositions.toRectangle(position);
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
            LOG.warn("Campaign UI is a " + campaignUi.getClass().getName()
                    + ", not CampaignState; intel-screen visor reads stay inert");
        }
    }
}
