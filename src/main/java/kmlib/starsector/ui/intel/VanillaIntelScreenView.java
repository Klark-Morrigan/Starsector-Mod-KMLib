package kmlib.starsector.ui.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.campaign.comms.v2.EventsPanel;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.coreui.CoreUiTree;
import kmlib.starsector.ui.layout.VanillaPositions;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link IntelScreenView} binding backed by the live campaign UI. The tab-open read is published
 * API; the map visor and its starscape state reach the game's concrete intel panel by walking the
 * live core-UI widget tree - the game's script classloader denies {@code java.lang.reflect} to mod
 * code, so the hops down to the tab that is up are taken by method name through {@link CoreUiTree},
 * and the panel is picked out of that tab's subtree by its own type.
 *
 * <p>The visor's two reads are one walk: the rectangle is derived from the component, so a caller
 * handed either is looking at the same widget under the same conditions. A widget the layout never
 * positioned is reported as no visor rather than as a visor with no box, which is what lets the two
 * answer null in exactly the same cases.
 *
 * <p>Nothing in the reach names a class the obfuscator chose. The intel tab's own class name is
 * single-letter obfuscator output and is reshuffled between game builds, so recognising the tab
 * would break silently on the next one; {@code EventsPanel} is a readable name, which is what a
 * do-not-obfuscate class looks like, and it is the type the visor readings are taken off anyway.
 *
 * <p>The reach into the concrete panel fails closed: a missing or unexpected link, a sibling sub-tab
 * showing instead, or a blanked preview all resolve to "no visor", so a caller reading the visor
 * rectangle simply gets {@code null} while there is nothing on the intel screen to draw over. The
 * starscape read fails the same way to "not in starscape mode", which is what an unreachable panel
 * is leaving the game doing anyway.
 *
 * <p>Failing closed is silent by design, and silent is wrong for one of the ways it happens: being
 * off the intel tab entirely is the ordinary case on every other screen, while a game build whose
 * intel tab no longer yields a panel would stop every intel-screen overlay with nothing in the log.
 * Those two are distinguishable, because the tab-open read is published API, so the second warns
 * once per session.
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

    // How deep below the tab to look for the panel. The intel tab adds it as a direct child, so one
    // level is what the search actually reaches; the couple of spare levels are there for a build
    // that wraps it in a holder, and the bound itself keeps a malformed tree from a runaway walk.
    private static final int MAX_PANEL_SEARCH_DEPTH = 3;

    // One-shot: the intel tab being up while its panel cannot be reached is a genuine anomaly worth
    // naming once, not on every frame a visor read is attempted.
    private boolean hasLoggedUnreachableIntelPanel;

    @Override
    public boolean isIntelTabOpen() {
        CampaignUIAPI campaignUi = readCampaignUi();
        return campaignUi != null
            && campaignUi.getCurrentCoreTab() == CoreUITabId.INTEL;
    }

    @Override
    public Rectangle getMapVisorRect() {
        // Derived from the widget read rather than walking to the panel a second time, so the two
        // cannot disagree about whether there is a visor: a caller handed a rectangle and a caller
        // handed the component are looking at the same widget under the same conditions.
        UIComponentAPI mapWidget = getMapVisorWidget();
        return mapWidget == null ? null : VanillaPositions.toRectangle(mapWidget.getPosition());
    }

    @Override
    public UIComponentAPI getMapVisorWidget() {
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
        // A widget the layout never positioned is reported as no visor at all, not as a visor with
        // no box. It occupies nothing on screen, so there is nothing to draw over or measure
        // against - and answering the two reads the same way is what lets a caller take either.
        PositionAPI position = mapWidget.getPosition();
        return position == null ? null : mapWidget;
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

    // Names a failed reach only when there was something there to reach. An unreachable panel means
    // one of two very different things: no intel tab is up, which is the ordinary state on every
    // other screen and worth nothing, or the intel tab is up and the walk still came back empty,
    // which is a game build this reach no longer fits and would otherwise stop every intel-screen
    // overlay in silence. The tab-open read is published API, so the two are told apart rather than
    // conflated.
    //
    // Kept apart from the walk for the same reason the visor rule is: the walk needs a live widget
    // tree, while which of its failures counts as news is a rule that stands on its own.
    //
    // Both reads are aimed at the same core UI - an interaction dialog's own while such a dialog is
    // up, the campaign's otherwise - so there is no benign way for them to disagree. A tab that
    // reads as open and yields no panel is a build this reach no longer fits, and the message says
    // so plainly.
    void warnOnceAboutUnreachableIntelPanel() {
        if (hasLoggedUnreachableIntelPanel || !isIntelTabOpen()) {
            return;
        }
        hasLoggedUnreachableIntelPanel = true;
        LOG.warn("The intel tab is open but no EventsPanel was found below the core UI's current "
            + "tab; intel-screen visor reads answer 'no visor' while that is so.");
    }

    // Walks the live core UI to the intel screen's events panel: campaign UI -> core -> current tab,
    // then that tab's subtree. Answers null off the intel tab, since the tab that is up then holds
    // no such panel, which is what leaves every caller inert on the other screens.
    private EventsPanel resolveIntelPanel() {
        EventsPanel intelPanel = null;
        try {
            intelPanel = findEventsPanelIn(CoreUiTree.resolveCurrentTab(), MAX_PANEL_SEARCH_DEPTH);
        } catch (Throwable unreadableTree) {
            // A hop that is absent or throws outright leaves the panel unreached, which is the same
            // outcome for a caller as a tab that holds no panel. Swallowed rather than raised: a
            // caller is in the middle of a render pass, and a read that cannot answer must not take
            // down the frame it was meant to refine.
        }
        if (intelPanel == null) {
            warnOnceAboutUnreachableIntelPanel();
        }
        return intelPanel;
    }

    // The first events panel at or below this component, or null when there is none.
    //
    // Breadth-first, and the order is what makes this cheap rather than the depth bound. The intel
    // tab adds the panel after two large sibling panels, so a depth-first walk would descend both of
    // their subtrees to the bound before ever reaching a sibling that sits one level down - hundreds
    // of by-name child reads per call, twice a frame, to find something that was never more than one
    // hop away. Level by level, the panel is found among the tab's own children and no subtree below
    // them is read at all.
    private static EventsPanel findEventsPanelIn(Object root, int maxDepth) {
        List<?> level = root == null ? List.of() : List.of(root);
        for (var depth = 0; depth <= maxDepth && !level.isEmpty(); depth++) {
            for (var component : level) {
                if (component instanceof EventsPanel intelPanel) {
                    return intelPanel;
                }
            }
            // Descend only once the whole level has missed, so finding the panel among the tab's
            // children costs no child read below them.
            var nextLevel = new ArrayList<>();
            for (var component : level) {
                nextLevel.addAll(CoreUiTree.readChildrenOf(component));
            }
            level = nextLevel;
        }
        return null;
    }

    private CampaignUIAPI readCampaignUi() {
        var sector = Global.getSector();
        return sector == null ? null : sector.getCampaignUI();
    }
}
