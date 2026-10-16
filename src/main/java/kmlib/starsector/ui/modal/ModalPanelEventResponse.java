package kmlib.starsector.ui.modal;

import com.fs.starfarer.api.input.InputEventAPI;

import java.util.Set;

/**
 * What a {@link ModalPanel} does with one input event, and the rule that decides it.
 *
 * <p>Kept out of the panel's plugin so the rule can be checked without a running game. A plugin
 * exists only inside a panel the game built, over a core UI that exists only while the game runs.
 * This rule is a function of one event, and needs neither.
 *
 * <p><b>A raised panel claims every event it is handed that nothing has already taken.</b> The
 * engine hands a custom panel's events to its children first and to its plugin afterwards, so
 * anything the panel's own controls acted on arrives here already consumed - which makes the
 * consumed skip below the whole of what protects them, and leaves nothing to except by position.
 *
 * <p>The panel's own box is not excepted by measuring it, because the widget underneath would read
 * that exception too: a screen answers a pointer it hears, and inside the box it would hear one. A
 * widget under the dim would then raise its own tooltip while the same widget out in the open
 * answers nothing.
 *
 * <p><b>A dismissed panel claims nothing at all</b>, though its box is on screen for the length of
 * its fade. The press that dismisses it is the moment the screen is the player's again, and a claim
 * held for the fall would eat the click that follows.
 */
enum ModalPanelEventResponse {

    /** The event is the panel's own widgets' to handle, or somebody else's already. */
    LEAVE_ALONE,

    /** The event is consumed, so nothing under the panel is dispatched to. */
    CLAIM,

    /** The event is consumed and the panel comes down with it. */
    CLOSE_PANEL;

    /**
     * Decides what becomes of {@code event}.
     *
     * <p>Whether the panel is up is asked before the event is touched at all, because a panel that
     * has let go answers for every event without reading one - and six of {@link InputEventAPI}'s
     * accessors throw once something has consumed the event, so the consumed skip has to stand ahead
     * of the key readings below it either way.
     *
     * @param event         the event the panel was handed
     * @param isPanelRaised whether the panel is still up, as against on screen but dismissed
     * @param closeKeyCodes the keys whose press asks the panel to close, as {@code Keyboard} codes
     * @return what the panel does with it
     */
    static ModalPanelEventResponse resolveResponseTo(
            InputEventAPI event,
            boolean isPanelRaised,
            Set<Integer> closeKeyCodes) {

        if (!isPanelRaised || event.isConsumed()) {
            return LEAVE_ALONE;
        }

        if (event.isKeyDownEvent() && closeKeyCodes.contains(event.getEventValue())) {
            return CLOSE_PANEL;
        }

        return CLAIM;
    }
}
