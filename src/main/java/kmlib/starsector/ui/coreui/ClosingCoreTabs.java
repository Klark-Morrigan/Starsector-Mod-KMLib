package kmlib.starsector.ui.coreui;

import java.util.ArrayList;
import java.util.List;

/**
 * The tabs the core UI is still drawing after it stopped naming them: screens the player has closed
 * or switched away from, part way through fading out.
 *
 * <p>The core UI forgets a closing tab before it stops drawing it. Closing a tab starts the fade-out
 * of the wrapper the tab sits in and clears the current tab in the same call. The wrapper stays a
 * child of the core UI, drawing the tab, until its fader reads faded out. So for the length of that
 * fade, every read taken through the current tab says the screen is gone while it is still on screen.
 * A question about what is being painted has to count these tabs. A question about input does not,
 * since a closing tab takes none.
 *
 * <p>A wrapper is recognised by the {@code getPanel} hop it offers, reached by name as every hop into
 * the core UI is. Its class name is obfuscator output and changes between game builds.
 *
 * <p>Policy-free about a broken reach, as {@link CoreUiTree} is: the hop to the core UI raises, and the
 * caller decides what that means.
 */
public final class ClosingCoreTabs {

    // The tab wrapper's hop to the tab it holds. The game's own contract on the wrapper, so it
    // survives obfuscation like the core UI's accessors.
    private static final String GET_PANEL_METHOD = "getPanel";

    private ClosingCoreTabs() {
    }

    /**
     * @return the tabs of the core UI in force that are still fading out, never its current tab; empty
     *         when none is, and before there is a campaign UI
     * @throws RuntimeException when a hop down to the core UI or its current tab is absent or fails
     *                          outright
     */
    public static List<Object> readClosingTabs() {

        var core = CoreUiTree.resolveActiveCoreUi();
        if (core == null) {
            return List.of();
        }

        return readClosingTabsAmong(
            CoreUiTree.readChildrenOf(core),
            CoreUiTree.readCurrentTabOf(core));
    }

    /**
     * The rule the live read applies, over readings taken elsewhere.
     *
     * @param coreChildren the core UI's children
     * @param currentTab   the core UI's current tab, or null when it names none
     * @return the tab of every child that holds one, is still showing and is not the current tab
     */
    static List<Object> readClosingTabsAmong(List<?> coreChildren, Object currentTab) {

        var closingTabs = new ArrayList<>();

        for (var child : coreChildren) {

            var tab = CoreUiTree.readHopIfOffered(child, GET_PANEL_METHOD);

            // The current tab's own wrapper holds it too, and is the one tab here that is not closing.
            if (tab != null
                    && tab != currentTab
                    && CoreUiTree.isComponentShowing(child)) {

                closingTabs.add(tab);
            }
        }

        return closingTabs;
    }
}
