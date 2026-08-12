package kmlib.starsector.ui.input;

import com.fs.starfarer.api.input.InputEventAPI;

import kmlib.animation.PulseEnvelopes;
import kmlib.animation.TraverseDurations;
import kmlib.starsector.ui.controls.BodyHoverSource;
import kmlib.starsector.ui.sound.UiSoundPlayer;
import kmlib.starsector.ui.sound.UiSoundScheme;
import kmlib.starsector.ui.sound.VanillaUiSoundPlayer;
import kmlib.starsector.ui.widgets.scroll.ScrollState;
import kmlib.starsector.ui.widgets.tabs.TabInteractionSources;
import kmlib.starsector.ui.widgets.tabs.TabPanelCollapse;
import kmlib.starsector.ui.widgets.tabs.TabPanelInteractionSources;
import kmlib.starsector.ui.widgets.tabs.TabPanelPlacement;

/**
 * Drives one tab panel's pointer input: it routes a left press on a header tab to that tab's own action,
 * flips the collapse handle on a press of the notch, and delegates everything else - the body control hits,
 * the scrollbar drag, the wheel scroll - to a {@link PanelController} for the body. A tab panel is a panel
 * plus a header plus a collapse handle, so its input is the panel's input plus a header hit-test and a notch
 * hit-test on top; the tab's action is baked into its spec (the host wires it), so this controller stays
 * agnostic to what selecting a tab does.
 *
 * <p>One controller per panel, since it holds that panel's runtime state across frames: the body's scroll
 * and drag state, the collapse animation, the hover fades of the parts that light under the pointer - the
 * header tabs, the body's own controls, and the collapse handle - and the two triggered motions its tabs
 * carry, a click's pulse and a bound key's blink. A host creates it, reads
 * its {@link #getScrollState()} and {@link #getCollapseFraction()} when it lays the panel out, advances the
 * collapse and the input motions each frame it draws, reads {@link #getInteractionSources()} and {@link
 * #getNotchHoverFraction()} to paint with, and feeds it pointer
 * events. That state lives here beside the
 * scroll offset because all of it is the panel's own transient per-session UI state, not the host's; a
 * consumer that lays out a placement and pumps this controller inherits the collapse handle and the live
 * tabs without wiring any of those animations itself. The panel opens
 * expanded by default, or collapsed to its docked rail via {@link #createStartingDocked()}, so a host picks
 * the initial fold at construction rather than driving the animation to reach it.
 *
 * <p>Every animation here is timed and nothing here is coloured, and the same line runs through what the
 * panel sounds: this end knows when a control was pressed and when the pointer reached one, and the look
 * it was handed says what each of those moments sounds like. What a fraction lifts a tab toward is the
 * strip's paint, resolved where the palette is; this end knows only how far each has run.
 */
public final class TabPanelController {
    /**
     * How long a bound key's blink takes to strike and let go, which is the panel's one motion that does
     * not run at the pace its host sets. Everything else the panel does in answer to input is a travel -
     * an element moving to where the pointer is holding it, or a lift over the look it has settled on -
     * and travels are paced together so the panel answers at one rhythm. A blink is not a travel: it
     * confirms a key pressed away from the panel, so it has to read as a strike and be gone, and at a
     * travel's pace it reads as one more thing moving at the speed everything else moves at.
     *
     * <p>Public so a consumer stepping this panel by hand can name the pace rather than measure it.
     */
    public static final TraverseDurations HOTKEY_BLINK_DURATIONS = new TraverseDurations(0.05f, 0.2f);

    // The body's controller, owning the scroll and drag state; this routes everything but a header-tab or
    // notch press to it, so the panel's scroll and drag behaviour is the plain panel's, unchanged.
    private final PanelController bodyController = new PanelController();

    // The collapse animation - how far the body is folded to its docked rail and which way it is heading.
    // Held beside the scroll offset so any tab-panel consumer inherits the handle by pumping this controller.
    private final TabPanelCollapse collapse;

    // How far each header tab has travelled onto the hovered shade, keyed by its index in the row - stable
    // for as long as the row is, which is all a key has to be. Held here with the panel's other transient
    // state rather than on the placement, which is an immutable value the layout computes: a fade is where
    // the panel currently stands, not where its parts sit.
    private final HoverFades<Integer> tabHoverFades = new HoverFades<>();

    // How far each body cell has travelled onto its hovered look, keyed by the slot it occupies in the
    // strip. A second set rather than a second key space in the one above, the two rows having nothing to
    // say to each other: a tab and a body control are never hovered together, but nor is either ever asked
    // about in the other's terms, and one map holding both would have to be keyed by something that can
    // spell either.
    private final HoverFades<BodyCellSlot> bodyHoverFades = new HoverFades<>();

    // The click lift each header tab is carrying, keyed the same way the fades are. A separate holder rather
    // than a second reading off the fades because the two motions differ in kind: a hover is a position the
    // pointer holds a tab at, a click is an event that runs its own course after the press that started it.
    private final PulseEnvelopes<Integer> tabClickPulses = new PulseEnvelopes<>();

    // The blink a bound key's press runs on its tab, keyed the same way again. An envelope like the clicks -
    // it is triggered and runs its own course - but read on the look channel with the fades rather than on
    // the lift channel with the clicks, because it carries its tab onto the hovered shade rather than past
    // it. That is what makes a key pressed for the tab already under the pointer show nothing: the blink
    // reaches only where the hover already stands.
    private final PulseEnvelopes<Integer> tabHotkeyBlinks = new PulseEnvelopes<>();

    // How far the collapse handle has travelled onto its lit look. A lone fade rather than a keyed set,
    // there being one handle per panel, and a fraction rather than a flag so the notch lights and dims at
    // the pace the tabs do rather than switching on the frame the pointer arrives.
    private final HoverFade notchHoverFade = new HoverFade();

    // Where this panel's interface sounds go, so a KM tab answers a press the way the engine's own controls
    // do. Held as a seam rather than reached for directly because a sound leaves no trace in the panel's
    // state: every other answer to an input can be read back off a fraction, and this one can only be
    // asserted by recording that it was asked for.
    private final UiSoundPlayer soundPlayer;

    // What each moment this panel's controls answer sounds like - which role, and how loudly. Taken from
    // the host with the rest of the panel's look rather than named here, because how a press sounds is a
    // property of how the panel presents itself: this end owns the moments - it is what detects them - and
    // owns none of the choices.
    // One value for the whole panel, so its notch and its tabs answer alike and a panel is silenced in one
    // place rather than by visiting every control that ever named a sound.
    private final UiSoundScheme soundScheme;

    // When the pointer reaches a header tab, so an arrival is answered once rather than every frame the
    // pointer spends there. Keyed by tab index like the fades, and one latch rather than one per tab: only
    // one tab of a row is under the pointer at a time.
    private final KeyedHoverArrival<Integer> tabHoverArrival = new KeyedHoverArrival<>();

    // The same moment for the collapse handle - a lone one, there being one handle per panel. Held beside
    // the tabs' rather than as a flag of this class's own so the two answer to one rule, and so a reset
    // cannot clear one and leave the other latched.
    private final HoverArrival notchHoverArrival = new HoverArrival();

    /**
     * A controller whose panel opens expanded and answers like a vanilla control - the fold and the scheme
     * a tab panel starts at unless a host asks otherwise.
     */
    public TabPanelController() {
        this(
            new TabPanelCollapse(),
            new VanillaUiSoundPlayer(),
            UiSoundScheme.createVanillaSoundScheme());
    }

    /**
     * A controller whose panel opens expanded, sounds through the given player, and answers by the given
     * scheme - for a host wearing a look of its own, or a test asserting which moments sound.
     *
     * @param soundPlayer where this panel's interface sounds go
     * @param soundScheme what each moment this panel's controls answer sounds like, the audible half of
     *                    the look the host paints the panel from
     */
    public TabPanelController(UiSoundPlayer soundPlayer, UiSoundScheme soundScheme) {
        this(new TabPanelCollapse(), soundPlayer, soundScheme);
    }

    // Shared construction taking the collapse seed, so the expanded default and the docked start differ only
    // in that seed and neither construction path learns a second one.
    private TabPanelController(
            TabPanelCollapse collapse,
            UiSoundPlayer soundPlayer,
            UiSoundScheme soundScheme) {

        this.collapse = collapse;
        this.soundPlayer = soundPlayer;
        this.soundScheme = soundScheme;
    }

    /**
     * A controller whose panel opens collapsed to its docked rail rather than expanded, for a host that
     * wants the body out of the way until the player expands it. The handle then animates it open exactly as
     * an expanded panel animates shut.
     *
     * @return a controller seeded at the docked end
     */
    public static TabPanelController createStartingDocked() {
        return createStartingDocked(
            new VanillaUiSoundPlayer(),
            UiSoundScheme.createVanillaSoundScheme());
    }

    /**
     * A controller seeded at the docked end that sounds through the given player and by the given scheme.
     *
     * @param soundPlayer where this panel's interface sounds go
     * @param soundScheme what each moment this panel's controls answer sounds like
     * @return a controller seeded at the docked end
     */
    public static TabPanelController createStartingDocked(
            UiSoundPlayer soundPlayer,
            UiSoundScheme soundScheme) {

        return new TabPanelController(TabPanelCollapse.createDocked(), soundPlayer, soundScheme);
    }

    /**
     * @return the body's scroll position, for the layout to read (the requested offset) and settle
     *         (clamp to the overflow) each frame
     */
    public ScrollState getScrollState() {
        return bodyController.getScrollState();
    }

    /**
     * @return how far the body is collapsed toward its docked rail, eased for the render pass to lay the
     *         panel out at and to orient the notch chevron; 0 fully expanded, 1 fully docked
     */
    public float getCollapseFraction() {
        return collapse.getCollapseFraction();
    }

    /**
     * @return how far the collapse handle has faded onto its lit look, for the render pass to light the
     *         handle by; 0 fully at rest, 1 fully lit
     */
    public float getNotchHoverFraction() {
        return notchHoverFade.getHoverFraction();
    }

    /**
     * @return true only when the panel is fully expanded and idle - not docked, docking, or undocking -
     *         which is a fact about the fold alone. What a caller gating input on the tabs wants is
     *         {@link #isPresentingTabsOf}, since a panel with no body to fold is presenting its tabs
     *         whatever this says
     */
    public boolean isFullyExpanded() {
        return collapse.isFullyExpanded();
    }

    /**
     * Whether the panel is offering the tabs of this placement to the player at all - the one question
     * every gate on tab input asks, whether it is a press, a fade, or a bound key.
     *
     * <p>A panel with a body answers by its fold: docked, docking, or undocking, its header is behind the
     * rail or on its way there, so a tab still laid out under the pointer is not one the player can see,
     * let alone aim at. A panel with no body answers yes always. It has nothing to fold and no handle to
     * unfold it, so the fold left standing in the controller by some other tab says nothing about it - and
     * acting on that fold would leave a row drawn in full whose tabs refuse every press, light under no
     * pointer, and ignore their own keys, with nothing on screen to explain why or any way to undo it.
     *
     * @param placement the laid-out tab panel being drawn and hit-tested this frame
     * @return whether this placement's tabs are live
     */
    public boolean isPresentingTabsOf(TabPanelPlacement placement) {
        return !placement.hasBody() || isFullyExpanded();
    }

    /**
     * What the pointer is doing to the whole panel, for the render pass to paint it at - the header's
     * channels and the body's together. One value rather than an accessor per half, because both are read
     * off the same frame's resolution against the same placement: a consumer taking them one at a time could
     * pair a fresh reading with a stale one, and the panel would draw a row and a strip that disagree about
     * where the pointer is.
     *
     * @return the panel's live interaction channels
     */
    public TabPanelInteractionSources getInteractionSources() {
        return new TabPanelInteractionSources(
            getTabInteractionSources(),
            getBodyHoverSource());
    }

    /**
     * Steps the collapse animation toward its current direction's end by a frame's worth of time, for the
     * host to call each frame it draws so the fold accelerates and settles under the eased curve. The
     * duration is the host's to supply, so it can expose the pace as a setting; a settled panel is left
     * unchanged, so an unconditional per-frame call rests once the animation ends.
     *
     * @param elapsedSeconds  real time since the last frame the host drew
     * @param durationSeconds how long a full collapse or expand should take; zero or less snaps instantly
     */
    public void advanceCollapse(float elapsedSeconds, float durationSeconds) {
        collapse.advanceByElapsedTime(elapsedSeconds, durationSeconds);
    }

    /**
     * Steps every motion the panel makes in answer to input - the hover fades of its header tabs, its body
     * controls, and its collapse handle, and the click pulses and hotkey blinks running on its tabs - by a
     * frame's worth of time, for the host to call each frame it draws, after it has resolved the placement.
     * One call rather than one per motion, so the panel's parts cannot be advanced against different
     * placements or charged different slices of the same frame.
     *
     * <p>What is under the pointer is resolved against the very placement being drawn rather than latched
     * from the last pointer event. That is what keeps a fade honest when the panel moves under a still
     * cursor: a scroll, a fold, or a relayout otherwise leaves an element lit that the pointer is no longer
     * over.
     *
     * <p>A panel not presenting its tabs (see {@link #isPresentingTabsOf}) hovers none of them, whatever is
     * laid out under the pointer. The handle is not gated that way - it draws past the frame and outlives
     * the fold, being what brings a docked panel back.
     *
     * @param placement      the laid-out tab panel this frame is drawing
     * @param elapsedSeconds real time since the last frame the host drew
     * @param durations      how long a traverse takes each way - onto a hovered look or up to a click's
     *                       peak, and back off either; a non-positive one snaps that way. One pair for every
     *                       travel the panel makes, since two paces written beside each other is how one
     *                       panel ends up with two rhythms. The hotkey blink is not among them: it is a
     *                       strike rather than a travel and keeps {@link #HOTKEY_BLINK_DURATIONS}
     */
    public void advanceInputMotions(
            TabPanelPlacement placement,
            float elapsedSeconds,
            TraverseDurations durations) {

        // One cursor read spent on every hit-test, so each of the panel's parts answers the same pointer.
        advanceInputMotionsAtPoint(
            placement,
            UiCursor.getUiX(),
            UiCursor.getUiY(),
            elapsedSeconds,
            durations);
    }

    /**
     * Ends any in-progress body scrollbar drag, for the host to call when the panel stops showing so a
     * drag left dangling cannot hijack the next session.
     */
    public void cancelDrag() {
        bodyController.cancelDrag();
    }

    /**
     * Drops every input motion the panel holds, for the host to call when the panel stops showing. A fade
     * left part-way up, or a pulse left part-way through its cycle, would otherwise be the first thing the
     * next session paints and then wind down, showing the player the tail of an interaction they never saw
     * begin - the same reason a host drops its frame clock there.
     */
    public void resetInputMotions() {
        tabHoverFades.resetFades();
        bodyHoverFades.resetFades();
        notchHoverFade.resetFade();
        tabClickPulses.resetPulses();
        tabHotkeyBlinks.resetPulses();

        // Forgetting what was announced, so a panel re-opening under a still pointer sounds that tab's - or
        // that handle's - arrival afresh. It is an arrival to the player - the row was not there a moment
        // ago - even though the pointer never moved, and the alternative is a control that lights in silence
        // for the one case where the panel came to the cursor rather than the other way about.
        tabHoverArrival.resetArrival();
        notchHoverArrival.resetArrival();
    }

    /**
     * Handles one pointer event over the tab panel: a left press on the collapse notch flips the fold and a
     * left press on a fully expanded panel's header tab fires that tab's own action and pulses it (each
     * consumed); every other event - body control hits, the scrollbar drag, the wheel - is the body's,
     * delegated to its {@link PanelController}. Hover is none of its business: the fades are resolved per
     * frame against the drawn placement by {@link #advanceInputMotions}, so nothing here has to be latched
     * for the render pass.
     *
     * <p>Whatever is left over the drawn tab row is swallowed there, exactly as the body swallows what lands
     * on its own chrome. The row is drawn outside the body's box, so without this the surface behind the
     * panel would go on reading a pointer the player has parked on the tabs - and a tab row with no body
     * under it would block nothing at all.
     *
     * @param event     the pointer event
     * @param placement the laid-out tab panel the renderer drew this frame
     */
    public void handlePointer(InputEventAPI event, TabPanelPlacement placement) {

        // A left release lets go of whatever press the tabs are holding, wherever the pointer has got to by
        // then: a press begun on a tab and released over a neighbour, over the map, or off the panel
        // entirely still ends that tab's lift, because the act it reported was the press and the press is
        // over. First and unconditional, so no branch below can swallow the release before the tabs hear
        // it; neither consumed nor returned on, a release being a report rather than a claim, and every
        // branch below tests for a press so it falls through them untouched.
        if (event.isLMBUpEvent() && tabClickPulses.releaseHeldPulses()) {

            // The press sounds on the release rather than on the down, matching the engine's own tabs: the
            // sound and the wash fading out are one answer to the button coming up. Gated on a hold having
            // actually ended, since every release on the screen reaches here and only the ones that let go
            // of a tab were owed anything - a click on the map behind the panel must not click at the
            // player.
            soundPlayer.playCueIfPresent(soundScheme.pressCue());
        }

        // A left press on the notch flips the body between expanded and docked. Tested before the header and
        // body because the notch sits outside the box (and stays reachable when docked), so it can never
        // collide with a tab or a body control for the same press.
        if (event.isLMBDownEvent()
                && placement.containsPointInNotch(event.getX(), event.getY())) {
            collapse.toggleCollapse();

            // Answered here on the way down rather than on the release the tabs wait for, because the
            // sound follows the moment the control acts and the handle acts immediately: the fold is
            // already moving. A tab's lift is held until the button comes up, so its press is not over
            // until then; the handle holds nothing and has nothing left to report by the release.
            soundPlayer.playCueIfPresent(soundScheme.pressCue());
            event.consume();
            return;
        }
        // A left press on a header tab fires that tab's action and pulses it. Only a left press hits a tab -
        // a wheel or an in-progress drag over the header falls through to the body, which simply finds
        // nothing there and consumes it, the same as any chrome.
        if (event.isLMBDownEvent() && activateTabAtPoint(placement, event.getX(), event.getY())) {
            event.consume();
            return;
        }
        bodyController.handlePointer(event, placement.body());

        // Last, so an in-progress scrollbar drag - which the body owns wherever the pointer has wandered,
        // the tab row included - keeps the event it is following.
        if (!event.isConsumed()
                && placement.drawnHeaderBand().containsPoint(event.getX(), event.getY())) {
            event.consume();
        }
    }

    /**
     * Blinks one header tab onto the hovered shade and back, for whatever routed a bound key's press to that
     * tab to call as it selects it. The key is the consumer's - which keycodes reach which tabs is its own
     * business - so all that arrives here is which tab was reached; the blink then runs its course without a
     * second call.
     *
     * <p>It confirms the press rather than the switch, so a key pressed for the tab the panel is already
     * showing still blinks. That is the opposite of the click pulse, which follows the action: a press has an
     * inert tab under it to explain why nothing happened, and a keypress has nothing on screen at all.
     *
     * <p>The blink shows nothing on a tab the pointer already holds fully on the hovered shade, the two
     * sharing one channel and composing by the greater of them - a tab already there has nowhere to travel.
     * On a tab only part-way onto it, the blink carries it the rest of the way and back.
     *
     * <p>It runs at {@link #HOTKEY_BLINK_DURATIONS} rather than at whatever pace the host is stepping the
     * panel's travels by, so the strike is over about as fast as the eye can catch it however leisurely the
     * rest of the panel moves.
     *
     * @param tabIndex the tab the pressed key is bound to, in row order
     */
    public void startHotkeyBlinkAt(int tabIndex) {
        tabHotkeyBlinks.startPulseAt(tabIndex);

        // Sounded as well as flashed, and for the same reason the flash exists: a keypress puts nothing
        // under the pointer to explain itself, so it needs both answers the engine gives a press rather
        // than half of one. Immediately rather than on any release, a key having no held moment to end.
        soundPlayer.playCueIfPresent(soundScheme.pressCue());
    }

    /**
     * What the header's tabs are currently showing, for the render pass to paint them at: how far each has
     * travelled onto the hovered shade, and what momentary lift each carries. The paint pass therefore reads
     * no cursor and holds no timing - it is handed both channels already resolved.
     *
     * <p>One half of {@link #getInteractionSources()}, which is what a consumer drawing the panel takes; this
     * is reachable on its own for what pins the half.
     *
     * @return the panel's live tab interaction channels
     */
    TabInteractionSources getTabInteractionSources() {
        return new TabInteractionSources(
            this::resolveHoverFractionAt,
            tabClickPulses::resolvePulseFractionAt);
    }

    /**
     * What the body's controls are currently showing, for the render pass to lift them by: asked for a
     * control's place in the drawn strip, it answers that control's own cells. The strip walk binds the
     * position and the widget below passes only the cell it is painting, so the two halves of a slot are
     * never both loose in one call - a crossed pair would light a cell of the wrong control, which is a
     * flicker nobody can reproduce rather than a failure anything reports.
     *
     * <p>The seam a paint pass takes, over the point read below: a control is drawn cell by cell, so what it
     * needs is something to ask, not a fraction fetched per cell by a caller that would have to spell the
     * slot out itself. The other half of {@link #getInteractionSources()}.
     *
     * @return the panel's live body hover channel
     */
    BodyHoverSource getBodyHoverSource() {
        return controlIndex -> cell -> resolveBodyHoverFractionAt(new BodyCellSlot(controlIndex, cell));
    }

    /**
     * How far onto its hovered look the body cell at a given slot currently stands. A bare fraction, so this
     * end holds no colour: what the lift is made of - a blend, a wash, a brightened frame - is the widget's
     * own paint, resolved where its style is.
     *
     * <p>The point read the source above is bound over, and the terms the fades are actually keyed in - which
     * is what makes it the reachable end for pinning that a slot's two halves are not crossed.
     *
     * @param slot the body cell being asked about
     * @return its hover fraction, 0 fully at rest and 1 fully on its hovered look
     */
    float resolveBodyHoverFractionAt(BodyCellSlot slot) {
        return bodyHoverFades.resolveHoverFractionAt(slot);
    }

    /**
     * Fires the header tab a press landed on and starts that tab's click pulse, reporting whether it acted.
     * Split from the event above so the pairing this seam exists for - the tab that fires is the tab that
     * pulses - can be checked without an engine input event to raise.
     *
     * <p>The lift follows the press and the action follows the switch, so the two part on the tab the panel
     * is already showing: it lifts like any other, and fires nothing. A press is an act the player made
     * whether or not anything came of it, and a tab that answered it with nothing at all would read as a
     * panel that missed the click rather than as one with nothing to do.
     *
     * <p>A panel not presenting its tabs offers none to press, on the same rule its hover fades answer to -
     * and through the same resolver, so the two cannot come to disagree about which tabs are live.
     *
     * @param placement the laid-out tab panel the renderer drew this frame
     * @param pointX    the press x in UI coordinates, the coordinates the placement is laid out in
     * @param pointY    the press y in UI coordinates
     * @return whether the press landed on a tab; false leaves the press to the body, and through it to
     *         whatever lies behind the panel
     */
    boolean activateTabAtPoint(TabPanelPlacement placement, float pointX, float pointY) {

        // What the pointer is on, taken from the one resolver the fades read too. The raw tab rather than an
        // actionable one: what lifts is what the player pressed, and the lit tab fires nothing yet still has
        // to answer, or pressing it reads as a panel that missed the click.
        var pressedTabIndex = resolveTabIndexAtPoint(placement, pointX, pointY);

        if (pressedTabIndex == PanelController.NO_CELL_RESOLVED) {
            return false;
        }
        // Held rather than self-timed: the lift reports a press the player is still making, so it waits at
        // its peak until the release above rather than timing its own fall.
        tabClickPulses.startHeldPulseAt(pressedTabIndex);

        // The action answers the switch, not the press, so the tab already being shown fires nothing - a
        // tabs row carries no reselect field and so reads as INERT, which is that rule. Split from the lift
        // because the two report different things, and only this one has a reason to do nothing; the answer
        // it returns is dropped, the lift above having already recorded which tab the player pressed.
        PanelController.activateCellIfActionable(placement.tabsHeader(), pressedTabIndex);
        return true;
    }

    /**
     * Steps the input motions for a pointer at a given point, hit-testing the panel's hoverable parts
     * against the placement being drawn. Split from the cursor read above for the same reason {@link
     * UiCursor} keeps its scaling separable from its LWJGL read: this is where each part is paired with the
     * hit-test that decides it - a pairing crossed over would light the handle for a tab - and the split is
     * what lets that pairing be checked without a display to point at.
     *
     * @param placement      the laid-out tab panel this frame is drawing
     * @param pointX         the pointer's x in UI coordinates, the coordinates the placement is laid out in
     * @param pointY         the pointer's y in UI coordinates
     * @param elapsedSeconds real time since the last frame the host drew
     * @param durations      how long a traverse takes each way; a non-positive one snaps that way
     */
    void advanceInputMotionsAtPoint(
            TabPanelPlacement placement,
            float pointX,
            float pointY,
            float elapsedSeconds,
            TraverseDurations durations) {

        // The fold is settled inside each resolver rather than here, so the hover and the press cannot ask
        // it differently; what reaches the advance below is only where the pointer is. The body asks it of
        // the box it is drawn inside and the header of its own gate, the two folding by different means -
        // the box narrows, the row is clipped - and neither reader choosing for itself which applies.
        advanceInputMotionsForFrame(
            new TabPanelHover(
                resolveTabIndexAtPoint(placement, pointX, pointY),
                resolveBodyCellSlotAtPoint(placement, pointX, pointY),
                placement.containsPointInNotch(pointX, pointY)),
            elapsedSeconds,
            durations);
    }

    /**
     * Steps every motion the panel holds by one frame, told what the hit-tests above found the pointer on.
     * Named for the frame rather than for the hover because only some of what it steps answers to a pointer:
     * the fades do, and the pulses do not - a click is an event already seen, and its cycle runs on wherever
     * the pointer went afterwards. They travel together so one frame's time is charged to every motion the
     * panel makes, off one pair of paces.
     *
     * <p>Whether the panel is presenting its tabs at all is settled before this, by the caller that holds
     * the placement: a panel presenting none is a pointer on none, and stating it twice would be two places
     * to keep in step. What arrives here is only where the pointer is.
     *
     * @param hover          what the pointer is on this frame, over both of the panel's hoverable parts
     * @param elapsedSeconds real time since the last frame the host drew
     * @param durations      how long a traverse takes each way; a non-positive one snaps that way
     */
    void advanceInputMotionsForFrame(
            TabPanelHover hover,
            float elapsedSeconds,
            TraverseDurations durations) {

        soundArrivalsAt(hover);

        // The handle is never gated with the tabs - it draws past the frame and outlives the fold, being
        // what brings a docked panel back.
        tabHoverFades.advanceTowardHoveredKey(hover.tabIndex(), elapsedSeconds, durations);

        // The body's cells travel on the same pair of paces as the row above them, which is the whole of
        // what the panel shares between its parts: what a fraction lifts a checkbox toward is that widget's
        // own paint, and a panel answering the pointer at two speeds reads as two panels.
        bodyHoverFades.advanceTowardHoveredKey(hover.bodyCellSlot(), elapsedSeconds, durations);

        notchHoverFade.advanceTowardHover(hover.isNotchHovered(), elapsedSeconds, durations);
        tabClickPulses.advanceByElapsedTime(elapsedSeconds, durations);

        // Ungated, like the clicks and unlike the fades: a blink is an event already seen, so its cycle runs
        // out wherever the panel goes afterwards rather than being cut short by a fold it did not ask for.
        // Paced by the strike rather than by the panel's travels - the one motion here that answers to its
        // own clock, since it confirms something that happened away from the panel and has to be gone by
        // the time the player looks for it.
        tabHotkeyBlinks.advanceByElapsedTime(elapsedSeconds, HOTKEY_BLINK_DURATIONS);
    }

    /**
     * Which header tab a point falls on, given what the panel is currently showing - the one hit-test the
     * panel answers the header with, read by the hover that lights a tab and by the press that fires one. Two
     * readers of one geometry rather than two hit-tests that happen to agree: the tab that lights and the tab
     * a press lands on are the same tab because they are the same answer.
     *
     * <p>It answers geometry and visibility and nothing else. Whether pressing that tab would <em>do</em>
     * anything is the press path's own question, settled after this by {@link
     * PanelController#activateCellIfActionable} - which is what lets the lit tab light while firing nothing.
     * A resolver that folded the two together would take that shade away, the sidebar's look having the
     * resting and the selected tab converge on one hovered shade with the underline left to mark the
     * selection.
     *
     * <p>The fold is part of the visibility it answers, so neither reader tests it for itself. Folding is a
     * paint-time clip over a header that stays laid out at the panel's full width: the tabs a docked panel
     * wipes off the screen keep their hit boxes exactly where they were, so a point on one of them is a point
     * on bare screen - it must not fire that tab and swallow the click, nor light a tab the player cannot
     * see. See {@link #isPresentingTabsOf} for what a bodyless panel answers.
     *
     * <p>The header never scrolls, so it hit-tests unclipped - unlike a body control in the flex list, whose
     * rows answer to their viewport.
     *
     * @param placement the laid-out tab panel to test against
     * @param pointX    the point's x in UI coordinates, the coordinates the placement is laid out in
     * @param pointY    the point's y in UI coordinates
     * @return the tab's index, or null when the point is on no tab the panel is presenting
     */
    Integer resolveTabIndexAtPoint(
            TabPanelPlacement placement,
            float pointX,
            float pointY) {

        // A panel presenting no tabs is a pointer on no tab, whatever is laid out under it.
        if (!isPresentingTabsOf(placement)) {
            return PanelController.NO_CELL_RESOLVED;
        }
        // Through the body's own control resolver rather than a hit-test of the header's own: the header is
        // an ordinary laid-out control, and one mapping of a row miss onto "no cell" is one place for the
        // unboxing trap that mapping carries to be got wrong.
        return PanelController.resolveHitCell(placement.tabsHeader(), pointX, pointY);
    }

    // Which body cell a point falls on, given what the panel is currently drawing - the body's half of the
    // frame's reading. Through the body controller's own walk rather than a hit-test of this class's own,
    // so the cell that lights and the cell a press lands on are the same cell because they are the same
    // answer, and the viewport and box gates that walk carries are inherited rather than restated.
    //
    // A hit is reduced to where it landed, the control it names being the press's business alone: a fade is
    // held against the slot, and a hover that also carried the widget could come to be keyed by it.
    private static BodyCellSlot resolveBodyCellSlotAtPoint(
            TabPanelPlacement placement,
            float pointX,
            float pointY) {

        var hitCell = PanelController.resolveHitBodyCell(placement.body(), pointX, pointY);

        return hitCell == null
            ? null
            : hitCell.slot();
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
    private float resolveHoverFractionAt(int tabIndex) {
        return Math.max(
            tabHoverFades.resolveHoverFractionAt(tabIndex),
            tabHotkeyBlinks.resolvePulseFractionAt(tabIndex));
    }

    // Answers the pointer reaching either of the panel's two hoverable parts, once per arrival. Both parts
    // sound alike because both are controls the player aims at, and the handle needed no new detection -
    // its hover was already being found for its fade.
    //
    // A moment rather than a position, which is why neither is read off a fade: the pointer resting on a
    // part holds its fade at the top for as long as it stays, and a sound taken from that would be a tone
    // rather than a tick.
    private void soundArrivalsAt(TabPanelHover hover) {

        // Both stepped before either is read. Each latches what the pointer is on this frame, so a
        // short-circuit would leave the unread one holding a stale reading - and then stay silent on the
        // frame the pointer did come back to it, that stale latch saying it never left.
        var hasReachedTab = tabHoverArrival.detectArrivalAt(hover.tabIndex());
        var hasReachedNotch = notchHoverArrival.detectArrival(hover.isNotchHovered());

        if (hasReachedTab || hasReachedNotch) {
            soundPlayer.playCueIfPresent(soundScheme.pointerArrivalCue());
        }
    }
}
