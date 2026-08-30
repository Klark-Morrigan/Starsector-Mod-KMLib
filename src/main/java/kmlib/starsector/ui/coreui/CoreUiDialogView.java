package kmlib.starsector.ui.coreui;

/**
 * Whether a modal dialog is raised over the core UI right now - a confirmation prompt, a picker, or
 * anything else a core screen stands up in front of itself and takes the whole screen for.
 *
 * <p>Exists because the published read does not answer this. {@code CampaignUIAPI#isShowingDialog}
 * reports the conversation dialogs the campaign raises, and a modal a core screen raises over itself
 * is none of those - it is stood up inside the core UI and never reaches the campaign's dialog state
 * at all, so the published read stays false for as long as one is up.
 *
 * <p>What that costs is paid by anything composited after the core UI rather than inside it. Such an
 * overlay is outside the widget tree, so nothing about the dialog reaches it: it draws over the
 * dialog undimmed, and goes on hit-testing the pointer under it while the dialog's own interceptor
 * is swallowing those events for every widget that *is* in the tree. Both halves are settled by
 * standing the overlay down for as long as this answers yes.
 *
 * <p>Recognised by shape rather than by type. Every such modal is added as a direct child of the
 * core UI panel, and they all descend from one base which alone declares the accessor for the
 * backdrop dim a modal paints behind itself - no other component in the game declares that name. So
 * carrying the name identifies a modal. The amount is never read: a modal that dims nothing still
 * intercepts, which makes the accessor's presence the question and its value a different one.
 *
 * <p>By name rather than by cast for the reason the reach below it is: the base is not nameable from
 * mod source in an obfuscated build, and a type test against it would also make this class
 * unexercisable outside a running game, since naming the type is what forces it to load. Reading a
 * name instead leaves the whole rule testable against any object that answers it.
 *
 * <p>The walk tests the core UI's children and never the root itself, which is not an optimisation:
 * the core UI panel descends from that same base and carries the accessor, so a test that included
 * the root would report a modal on every frame the game has a core UI at all.
 *
 * <p>Fails closed on the dialog and open for the caller - whatever cannot be established reads as no
 * modal showing. The answer only ever takes something away from a caller, so an unreadable tree
 * leaves it doing what it did before this question existed rather than going quiet on a screen the
 * player is looking at.
 */
public final class CoreUiDialogView {

    // The accessor a modal alone carries: the backdrop dim it paints behind itself. Part of the
    // base's own contract, so it survives obfuscation the way the hops in the reach below do.
    private static final String GET_BACKGROUND_DIM_AMOUNT_METHOD = "getBackgroundDimAmount";

    private CoreUiDialogView() {
    }

    /**
     * @return whether a modal dialog stands over the core UI this frame, and {@code false} whenever
     *         that cannot be established - there being no campaign yet, no core UI in force, or the
     *         reach into it having failed outright
     */
    public static boolean isModalDialogShowing() {

        try {
            return isModalDialogShowingUnder(CoreUiTree.resolveActiveCoreUi());

        } catch (Throwable cannotReachCoreUi) {

            // The reach raises on a hop that is absent rather than empty, leaving each caller to
            // apply its own policy. This one's is the fail-open answer above.
            return false;
        }
    }

    /**
     * The rule itself, over a core UI its caller has already resolved. Package-private so it can be
     * driven against a stood-up tree with no game running, the live resolution above being the half
     * that needs one.
     *
     * @param coreUi the core UI in force, or null when there is none
     * @return whether any of its children is a modal that is still on screen
     */
    static boolean isModalDialogShowingUnder(Object coreUi) {

        if (coreUi == null) {
            return false;
        }

        for (var child : CoreUiTree.readChildrenOf(coreUi)) {

            // Asked in this order because the cheap half settles most children: the name is answered
            // from a memo, while the fade state costs two hops into whatever carries it.
            if (isModalDialog(child) && CoreUiTree.isComponentShowing(child)) {
                return true;
            }
        }
        return false;
    }

    // Whether this child is a modal at all. A dismissed one stays a child until its fade finishes,
    // so this says what a component is and the showing read beside it says whether it is still up.
    private static boolean isModalDialog(Object component) {

        return component != null
            && CoreUiTree.hasMethodNamed(component, GET_BACKGROUND_DIM_AMOUNT_METHOD);
    }
}
