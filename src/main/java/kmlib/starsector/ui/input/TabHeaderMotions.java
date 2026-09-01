package kmlib.starsector.ui.input;

import kmlib.animation.PulseEnvelopes;
import kmlib.animation.TraverseDurations;
import kmlib.starsector.ui.widgets.tabs.TabInteractionSources;

/**
 * Everything a tab row is currently doing: how far each tab has travelled onto the hovered shade, the lift a
 * click is holding on one, the blink a bound key struck on one, and whether the pointer has just reached a
 * tab at all. Held together because they are charged together - one frame's time reaches all of them off one
 * reading of where the pointer is - and read together, two of them composing into the single fraction a tab
 * is painted at.
 *
 * <p>The row's counterpart to what {@link PanelController} holds for the body beneath it. A tab panel's
 * controller then owns neither set: it resolves what the pointer is on, hands that reading to this end and to
 * the body's, and is left holding the fold and the routing of events - which is the whole of what a header
 * plus a body plus a handle adds over a plain panel.
 *
 * <p>Keyed by a tab's index in the row, which is stable for as long as the row is and is all a key has to be.
 * Nothing here is coloured and nothing here reads a cursor: what a fraction lifts a tab toward is the strip's
 * paint, resolved where the palette is, and where the pointer is arrives as a parameter.
 */
final class TabHeaderMotions {

    /**
     * How long a bound key's blink takes to strike and let go, which is the row's one motion that does not
     * run at the pace its host sets. Everything else a panel does in answer to input is a travel - an element
     * moving to where the pointer is holding it, or a lift over the look it has settled on - and travels are
     * paced together so the panel answers at one rhythm. A blink is not a travel: it confirms a key pressed
     * away from the panel, so it has to read as a strike and be gone, and at a travel's pace it reads as one
     * more thing moving at the speed everything else moves at.
     *
     * <p>It lives here, with the motion that runs at it, and is published to consumers under
     * {@link TabPanelController#HOTKEY_BLINK_DURATIONS}.
     */
    static final TraverseDurations HOTKEY_BLINK_DURATIONS = new TraverseDurations(0.05f, 0.2f);

    // How far each tab has travelled onto the hovered shade, keyed by its index in the row. Held with the
    // panel's other transient state rather than on the placement, which is an immutable value the layout
    // computes: a fade is where the row currently stands, not where its parts sit.
    private final HoverFades<Integer> tabHoverFades = new HoverFades<>();

    // The click lift each tab is carrying, keyed the same way the fades are. A separate holder rather than a
    // second reading off the fades because the two motions differ in kind: a hover is a position the pointer
    // holds a tab at, a click is an event that runs its own course after the press that started it.
    private final PulseEnvelopes<Integer> tabClickPulses = new PulseEnvelopes<>();

    // The blink a bound key's press runs on its tab, keyed the same way again. An envelope like the clicks -
    // it is triggered and runs its own course - but read on the look channel with the fades rather than on
    // the lift channel with the clicks, because it carries its tab onto the hovered shade rather than past
    // it. That is what makes a key pressed for the tab already under the pointer show nothing: the blink
    // reaches only where the hover already stands.
    private final PulseEnvelopes<Integer> tabHotkeyBlinks = new PulseEnvelopes<>();

    // When the pointer reaches a tab, so an arrival is answered once rather than every frame the pointer
    // spends there. Keyed by tab index like the fades, and one latch rather than one per tab: only one tab of
    // a row is under the pointer at a time.
    private final KeyedHoverArrival<Integer> tabHoverArrival = new KeyedHoverArrival<>();

    /**
     * What the row's tabs are currently showing, for the render pass to paint them at: how far each has
     * travelled onto the hovered shade, and what momentary lift each carries. The paint pass therefore reads
     * no cursor and holds no timing - it is handed both channels already resolved.
     *
     * @return the row's live interaction channels
     */
    TabInteractionSources resolveTabInteractionSources() {
        return new TabInteractionSources(
            this::resolveHoverFractionAt,
            tabClickPulses::resolvePulseFractionAt);
    }

    /**
     * How far onto the hovered shade a tab currently stands, from either motion that can put it there: the
     * pointer holding it there, or a bound key's blink passing through. The greater of the two rather than
     * their sum, because both aim at the one shade: summed, a blink on a tab already part-way hovered would
     * drive it past a shade neither names.
     *
     * <p>Neither motion is aware of the other - each runs its own course and this reads whichever is further
     * along - so a pointer arriving on a tab mid-blink watches the blink decay until its own fade overtakes
     * it. Continuous, since the greater of two continuous fractions is one, but not a handover: the fade
     * starts from rest rather than from where the blink stood.
     *
     * @param tabIndex the tab being asked about, in row order
     * @return its look-channel fraction, 0 fully off the hovered shade and 1 fully on it
     */
    float resolveHoverFractionAt(int tabIndex) {
        return Math.max(
            tabHoverFades.resolveHoverFractionAt(tabIndex),
            tabHotkeyBlinks.resolvePulseFractionAt(tabIndex));
    }

    /**
     * Whether the pointer has just come onto a tab, latched so an arrival is answered once rather than every
     * frame the pointer spends there. What that moment is owed is the caller's to decide; this only says that
     * it happened.
     *
     * @param tabIndex the tab the pointer is on this frame, or null when it is on none
     * @return whether this frame is the one the pointer arrived on
     */
    boolean detectTabArrivalAt(Integer tabIndex) {
        return tabHoverArrival.detectArrivalAt(tabIndex);
    }

    /**
     * Starts the held lift of a pressed tab. Held rather than self-timed: the lift reports a press the player
     * is still making, so it waits at its peak until {@link #releaseHeldClickPulses()} rather than timing its
     * own fall.
     *
     * @param tabIndex the pressed tab, in row order
     */
    void startHeldClickPulseAt(int tabIndex) {
        tabClickPulses.startHeldPulseAt(tabIndex);
    }

    /**
     * Lets go of whatever press the row is holding, wherever the pointer has got to by then - a press begun on
     * a tab and released over a neighbour, over the map, or off the panel entirely still ends that tab's lift,
     * because the act it reported was the press and the press is over.
     *
     * @return whether a hold actually ended, which is what tells a caller a release was owed anything at all
     */
    boolean releaseHeldClickPulses() {
        return tabClickPulses.releaseHeldPulses();
    }

    /**
     * Blinks one tab onto the hovered shade and back, for whatever routed a bound key's press to that tab.
     * The blink then runs its course without a second call, at {@link #HOTKEY_BLINK_DURATIONS} rather than at
     * whatever pace the host is stepping the panel's travels by.
     *
     * @param tabIndex the tab the pressed key is bound to, in row order
     */
    void startHotkeyBlinkAt(int tabIndex) {
        tabHotkeyBlinks.startPulseAt(tabIndex);
    }

    /**
     * Steps every motion the row holds by one frame, told which tab the pointer is on. Named for the frame
     * rather than for the hover because only some of what it steps answers to a pointer: the fades do, and the
     * pulses do not - a click is an event already seen, and its cycle runs on wherever the pointer went
     * afterwards.
     *
     * <p>Whether the panel is presenting its tabs at all is settled before this, by the caller that holds the
     * placement: a panel presenting none is a pointer on none, and stating it twice would be two places to
     * keep in step.
     *
     * @param hoveredTabIndex the tab the pointer is on this frame, or null when it is on none
     * @param elapsedSeconds  real time since the last frame the host drew
     * @param durations       how long a traverse takes each way; a non-positive one snaps that way
     */
    void advanceTabMotionsForFrame(
            Integer hoveredTabIndex,
            float elapsedSeconds,
            TraverseDurations durations) {

        tabHoverFades.advanceTowardHoveredKey(hoveredTabIndex, elapsedSeconds, durations);
        tabClickPulses.advanceByElapsedTime(elapsedSeconds, durations);

        // Ungated, like the clicks and unlike the fades: a blink is an event already seen, so its cycle runs
        // out wherever the panel goes afterwards rather than being cut short by a fold it did not ask for.
        // Paced by the strike rather than by the panel's travels - the one motion here that answers to its
        // own clock, since it confirms something that happened away from the panel and has to be gone by the
        // time the player looks for it.
        tabHotkeyBlinks.advanceByElapsedTime(elapsedSeconds, HOTKEY_BLINK_DURATIONS);
    }

    /**
     * Drops every motion the row holds, for a panel that stops showing. A fade left part-way up, or a pulse
     * left part-way through its cycle, would otherwise be the first thing the next session paints and then
     * wind down, showing the player the tail of an interaction they never saw begin.
     *
     * <p>The arrival latch goes with them, so a panel re-opening under a still pointer sounds that tab's
     * arrival afresh. It is an arrival to the player - the row was not there a moment ago - even though the
     * pointer never moved.
     */
    void resetTabMotions() {
        tabHoverFades.resetFades();
        tabClickPulses.resetPulses();
        tabHotkeyBlinks.resetPulses();
        tabHoverArrival.resetArrival();
    }
}
