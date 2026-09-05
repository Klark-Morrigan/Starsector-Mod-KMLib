package kmlib.starsector.ui.coreui;

import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

/**
 * Standing a panel of one's own in the core UI's own widget tree, and taking it off again.
 *
 * <p>Exists because the game publishes no way to raise a panel over a core screen. Every published
 * route to a custom panel hangs off an interaction dialog, and a screen the player opened from the
 * campaign has none - standing one up to obtain the route would close the screen the panel was
 * wanted on. What is left is the tree itself: the core UI is a panel like any other, so a component
 * added to it is drawn and hit-tested by the game beside the screen's own widgets rather than
 * composited after them.
 *
 * <p>That is the whole of what this buys, and it is worth being plain about what it does not. The
 * game dims nothing behind such a panel and stops dispatching to nothing under it, because the
 * dimming and the interception belong to the modal base {@link CoreUiDialogView} recognises and
 * nothing here descends from it. A caller wanting either supplies its own, and a caller wanting to
 * be seen as a modal by anything reading that base cannot be.
 *
 * <p>Reached through the published panel interface rather than by name. The core UI's own class is
 * obfuscated, but it is a panel, and adding and removing children is what the interface is for - so
 * a game build that renames the class leaves this working, and one that stops answering the
 * interface leaves it refusing rather than throwing.
 *
 * <p><b>The parent is resolved at each call and never held.</b> A core UI is rebuilt as the player
 * moves between screens, and a panel added back to the root a previous call resolved is added to a
 * tree nothing is drawing - which reports success and shows the player nothing. Re-resolving also
 * settles removal on its own: whatever discarded the old root discarded the panel with it, so a
 * removal that no longer finds its parent has nothing left to do.
 *
 * <p>Fails closed, which is the opposite of the reads in this package and deliberate: those take
 * something away from a caller that was working before them, while this is the caller's whole
 * effect. A no reads as "the panel is not up", which is a state every caller already has to handle.
 */
public final class CoreUiOverlayPanels {

    private CoreUiOverlayPanels() {
    }

    /**
     * Stands {@code panel} in the core UI in force and raises it above the screen's own widgets.
     *
     * @param panel the panel to stand up, typically from {@code SettingsAPI.createCustom}
     * @return where the layout will place it, for the caller to position - or null when there is no
     *         core UI, when it is not a panel that takes children, or when the reach into it failed,
     *         all of which leave the screen exactly as it was
     */
    public static PositionAPI attachOverlayPanel(UIComponentAPI panel) {

        try {
            return attachOverlayPanelTo(CoreUiTree.resolveActiveCoreUi(), panel);

        } catch (Throwable cannotReachCoreUi) {

            // The reach raises on a hop that is absent rather than empty. Nothing was added, so the
            // caller's own answer - no panel is up - is already the truthful one.
            return null;
        }
    }

    /**
     * Takes {@code panel} back off the core UI in force.
     *
     * <p>Quiet about every way it can find nothing to remove: a panel that was never added, one
     * whose root has since been rebuilt, and a core UI that cannot be reached are one outcome for
     * the caller, which is that the panel is not on screen.
     *
     * @param panel the panel to take off
     */
    public static void detachOverlayPanel(UIComponentAPI panel) {

        try {
            detachOverlayPanelFrom(CoreUiTree.resolveActiveCoreUi(), panel);

        } catch (Throwable cannotReachCoreUi) {
            // Nothing to undo and nothing to report: see above.
        }
    }

    /**
     * The attaching rule itself, over a core UI its caller has already resolved. Package-private so it
     * can be driven against a stood-up tree with no game running, the live resolution above being the
     * half that needs one.
     *
     * @param coreUi the core UI in force, or null when there is none
     * @param panel  the panel to stand up, or null when the caller built none
     * @return where the layout will place it, or null when there was nothing to add it to
     */
    static PositionAPI attachOverlayPanelTo(Object coreUi, UIComponentAPI panel) {

        if (panel == null || !(coreUi instanceof UIPanelAPI coreUiPanel)) {
            return null;
        }

        var placement = coreUiPanel.addComponent(panel);

        // Above the screen's own widgets rather than wherever the child list happened to put it. A
        // panel raised over a screen is only over it while it is drawn last, and the screen's widgets
        // were added first.
        coreUiPanel.bringComponentToTop(panel);

        return placement;
    }

    /**
     * The removing rule, over a core UI its caller has already resolved. Package-private for the reason
     * the attaching rule beside it is.
     *
     * @param coreUi the core UI in force, or null when there is none
     * @param panel  the panel to take off, or null when the caller holds none
     */
    static void detachOverlayPanelFrom(Object coreUi, UIComponentAPI panel) {

        if (panel != null && coreUi instanceof UIPanelAPI coreUiPanel) {
            coreUiPanel.removeComponent(panel);
        }
    }
}
