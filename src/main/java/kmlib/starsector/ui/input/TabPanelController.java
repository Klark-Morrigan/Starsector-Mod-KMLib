package kmlib.starsector.ui.input;

import com.fs.starfarer.api.input.InputEventAPI;

import kmlib.animation.TraverseDurations;
import kmlib.starsector.ui.controls.BodyHoverSource;
import kmlib.starsector.ui.controls.BodyInteractionSources;
import kmlib.starsector.ui.controls.BodyPressSource;
import kmlib.starsector.ui.sound.PointerArrivalTarget;
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
 * routes a left press on the panel's own band button to the button's action, flips the collapse handle on a
 * press of the notch, and delegates everything else - the body control hits, the scrollbar drag, the wheel
 * scroll - to a {@link PanelController} for the body. A tab panel is a panel plus a header plus a collapse
 * handle, so its input is the panel's input plus a header hit-test and a notch hit-test on top; each action
 * is baked into its own spec (the host wires it), so this controller stays agnostic to what selecting a tab
 * or pressing the button does.
 *
 * <p>One controller per panel, since it holds that panel's runtime state across frames: the collapse
 * animation and its handle's fade, and the routing that decides which of the panel's parts a frame's reading
 * belongs to. What each part is doing is held by that part's own end - the row's fades, lifts and blinks on
 * {@link TabHeaderMotions}, and the body's scroll, drag, cell fades, arrivals, press lifts and hover report
 * on the {@link PanelController} beneath, the end a body press actually lands on. Both are charged by this
 * one per-frame pass, off one reading of where the pointer is, which is what keeps the panel answering at a
 * single rhythm.
 *
 * <p>A host creates it, reads its {@link #getScrollState()} and {@link #getCollapseFraction()} when it lays
 * the panel out, advances the collapse and the input motions each frame it draws, reads {@link
 * #getInteractionSources()} and {@link #getNotchHoverFraction()} to paint with, and feeds it pointer events.
 * That state lives here rather than on the host because all of it is the panel's own transient per-session UI
 * state; a consumer that lays out a placement and pumps this controller inherits the collapse handle and the
 * live tabs without wiring any of those animations itself. The panel opens expanded by default, or collapsed
 * to its docked rail via {@link #createStartingDocked()}, so a host picks the initial fold at construction
 * rather than driving the animation to reach it.
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
     * <p>Public so a consumer stepping this panel by hand can name the pace rather than measure it; the
     * value itself lives with the motion that runs at it.
     */
    public static final TraverseDurations HOTKEY_BLINK_DURATIONS = TabHeaderMotions.HOTKEY_BLINK_DURATIONS;

    // The band button's one cell. It is laid out as a one-cell control so it wears the row's look, so the
    // press that fires it names that cell rather than resolving one: a hit-test would answer the same
    // number for the only box there is, and the two readers would then have to agree about it.
    private static final int BAND_BUTTON_CELL = 0;

    // The body's controller, owning the scroll and drag state; this routes everything but a header-tab or
    // notch press to it, so the panel's scroll and drag behaviour is the plain panel's, unchanged. Built
    // with this panel's own player and scheme, the wheel being a moment it answers: a body sounding by the
    // library's default look while its header sounded by the host's would be one panel presenting itself
    // two ways.
    private final PanelController bodyController;

    // The collapse animation - how far the body is folded to its docked rail and which way it is heading.
    // Held beside the scroll offset so any tab-panel consumer inherits the handle by pumping this controller.
    private final TabPanelCollapse collapse;

    // What the header row is doing - its tabs' hover fades, the lift a click holds on one, the blink a bound
    // key strikes on one, and the arrival latch behind their sound. Held as one thing for the reason the
    // body's are held on the controller beneath: they are charged together off one reading of where the
    // pointer is, and two of them compose into the one fraction a tab is painted at.
    private final TabHeaderMotions headerMotions = new TabHeaderMotions();

    // How far the collapse handle has travelled onto its lit look. A lone fade rather than a keyed set,
    // there being one handle per panel, and a fraction rather than a flag so the notch lights and dims at
    // the pace the tabs do rather than switching on the frame the pointer arrives.
    private final HoverFade notchHoverFade = new HoverFade();

    // How far the panel's own band button has travelled onto the hovered shade. A lone fade like the
    // handle's rather than an entry in the row's keyed set, because it is one cell that is not a tab: keyed
    // alongside them it would take an index out of the row's space, and every index there is a tab's.
    private final HoverFade bandButtonHoverFade = new HoverFade();

    // When the pointer reaches the band button - its own latch beside its own fade, for the same reason the
    // handle keeps one.
    private final HoverArrival bandButtonHoverArrival = new HoverArrival();

    // What this panel sounds like in answer to the moments it detects, so a KM tab answers a press the way
    // the engine's own controls do. One value for the whole panel, handed down to the body beneath, so its
    // notch, its tabs and its controls answer alike and a panel is silenced in one place rather than by
    // visiting every control that ever named a sound.
    private final PanelSounds sounds;

    // When the pointer reaches the collapse handle - a lone latch, there being one handle per panel, and the
    // handle's own rather than the row's because the two are gated differently: the tabs go behind the fold
    // and the handle outlives it.
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
     * scheme - for a host wearing a look of its own, or one that wants to observe which moments sound.
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
        this.sounds = new PanelSounds(soundPlayer, soundScheme);

        // The body is handed this panel's own sounds rather than the parts they were built from: a body
        // sounding by the library's default look while its header sounded by the host's would be one panel
        // presenting itself two ways.
        this.bodyController = new PanelController(sounds);
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
            bandButtonHoverFade.getHoverFraction(),
            new BodyInteractionSources(
                getBodyHoverSource(),
                getBodyPressSource()));
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
     * controls, and its collapse handle, the click pulses and hotkey blinks running on its tabs, and the
     * press lifts running on its body cells - by a frame's worth of time, for the host to call each frame it
     * draws, after it has resolved the placement.
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
     *
     * <p>Whoever was being told about the hovered body cell hears the pointer leave here, that being what a
     * panel going away is to it: no further frame resolves a reading, so nothing else would tell it to let
     * go of the cell it was last handed.
     */
    public void resetInputMotions() {

        // Everything the row holds - its fades, its lifts, its blinks, and what it announced - goes in one
        // call to the end that holds all of it.
        headerMotions.resetTabMotions();
        notchHoverFade.resetFade();
        bandButtonHoverFade.resetFade();

        // Forgetting what was announced, so a panel re-opening under a still pointer sounds that part's
        // arrival afresh. It is an arrival to the player - the handle and the button were not there a moment
        // ago - even though the pointer never moved, and the alternative is a control that lights in silence
        // for the one case where the panel came to the cursor rather than the other way about.
        notchHoverArrival.resetArrival();
        bandButtonHoverArrival.resetArrival();

        // Everything the body holds - its fades, its lifts, what it announced, and what it told the host
        // answering its hover - goes in one call to the end that holds all of it.
        bodyController.resetBodyInputMotions();
    }

    /**
     * Handles one pointer event over the tab panel: a left press on the collapse notch flips the fold, a left
     * press on a fully expanded panel's band button fires the panel's own action, and a left press on such a
     * panel's header tab fires that tab's own action and pulses it (each consumed); every other event - body
     * control hits, the scrollbar drag, the wheel - is the body's,
     * delegated to its {@link PanelController}. What claiming means differs by the kind of event: a press
     * or a wheel is consumed, while a move is claimed by {@linkplain PointerParking parking the pointer},
     * so the screen behind hears that the pointer left whatever it had lit. Hover is none of its own
     * business either: the fades are resolved per
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
        if (event.isLMBUpEvent() && headerMotions.releaseHeldClickPulses()) {

            // The press sounds on the release rather than on the down, matching the engine's own tabs: the
            // sound and the wash fading out are one answer to the button coming up. Gated on a hold having
            // actually ended, since every release on the screen reaches here and only the ones that let go
            // of a tab were owed anything - a click on the map behind the panel must not click at the
            // player.
            sounds.soundPress();
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
            sounds.soundPress();
            event.consume();
            return;
        }
        // A left press on the panel's own band button fires the button's action. No two of the panel's parts
        // occupy the same point, so the order among these branches decides nothing about which one answers;
        // it sits here because it reads with the notch above it, both being chrome that acts on the press
        // rather than tabs that hold a lift.
        //
        // Sounded on the way down like the handle, and for the same reason: the button acts at once and has
        // nothing left to report by the release, where a tab holds its lift until the button comes up.
        if (event.isLMBDownEvent() && activateBandButtonAtPoint(placement, event.getX(), event.getY())) {
            sounds.soundPress();
            event.consume();
            return;
        }
        // A left press on a header tab fires that tab's action and pulses it. Only a left press hits a tab -
        // a wheel or an in-progress drag over the header falls through to the body, which simply finds
        // nothing there and claims it, the same as any chrome.
        if (event.isLMBDownEvent() && activateTabAtPoint(placement, event.getX(), event.getY())) {
            event.consume();
            return;
        }
        // Where the player actually put the pointer, read before the body can claim it: claiming a move
        // parks the pointer rather than consuming the event, so the position is no longer the player's to
        // read afterwards and the header test below would be answering about somewhere nobody pointed.
        var pointerX = event.getX();
        var pointerY = event.getY();

        bodyController.handlePointer(event, placement.body());

        // Last, so an in-progress scrollbar drag - which the body owns wherever the pointer has wandered,
        // the tab row included - keeps the event it is following.
        if (!event.isConsumed()
                && placement.drawnHeaderBand().containsPoint(pointerX, pointerY)) {
            PointerParking.claimEvent(event);
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
        headerMotions.startHotkeyBlinkAt(tabIndex);

        // Sounded as well as flashed, and for the same reason the flash exists: a keypress puts nothing
        // under the pointer to explain itself, so it needs both answers the engine gives a press rather
        // than half of one. Immediately rather than on any release, a key having no held moment to end.
        sounds.soundPress();
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
        return headerMotions.resolveTabInteractionSources();
    }

    /**
     * What the body's controls are currently showing, for the render pass to lift them by: asked for a
     * control's place in the drawn strip, it answers that control's own cells. The strip walk binds the
     * position and the widget below passes only the cell it is painting, so the two halves of a slot are
     * never both loose in one call - a crossed pair would light a cell of the wrong control, which is a
     * flicker nobody can reproduce rather than a failure anything reports.
     *
     * <p>Read from the body's own controller, which is where a body's motions are held and where the two
     * halves of a slot are put back together; this end reaches for it rather than binding a second channel
     * over the same cells. The other half of {@link #getInteractionSources()}.
     *
     * @return the panel's live body hover channel
     */
    BodyHoverSource getBodyHoverSource() {
        return bodyController.getBodyHoverSource();
    }

    /**
     * What the body's controls are showing for the presses they answered - a channel beside the hover above
     * rather than folded into it, the two saying different things about one cell: where the pointer is
     * standing, and what it just did there. Read from the body's own controller for the same reason the
     * hover is.
     *
     * @return the panel's live body press channel
     */
    BodyPressSource getBodyPressSource() {
        return bodyController.getBodyPressSource();
    }

    /**
     * How far onto its hovered look the body cell at a given slot currently stands, read from the controller
     * that holds the body's fades.
     *
     * <p>The point the source above is bound over, and the terms the fades are actually keyed in - which is
     * what makes it the reachable end for pinning that a slot's two halves are not crossed.
     *
     * @param slot the body cell being asked about
     * @return its hover fraction, 0 fully at rest and 1 fully on its hovered look
     */
    float resolveBodyHoverFractionAt(BodyCellSlot slot) {
        return bodyController.resolveBodyHoverFractionAt(slot);
    }

    /**
     * How far through its press lift the body cell at a given slot currently stands - the second channel a
     * body cell answers on, beside the hover above it, and read from the same controller.
     *
     * @param slot the body cell being asked about
     * @return its press fraction, 0 with no lift running on it and 1 at a lift's peak
     */
    float resolveBodyPressFractionAt(BodyCellSlot slot) {
        return bodyController.resolveBodyPressFractionAt(slot);
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

        if (pressedTabIndex == ControlHitResolver.NO_CELL_RESOLVED) {
            return false;
        }
        headerMotions.startHeldClickPulseAt(pressedTabIndex);

        // The action answers the switch, not the press, so the tab already being shown fires nothing - a
        // tabs row carries no reselect field and so reads as INERT, which is that rule. Split from the lift
        // because the two report different things, and only this one has a reason to do nothing; the answer
        // it returns is dropped, the lift above having already recorded which tab the player pressed.
        ControlActivation.activateCellIfActionable(placement.tabsHeader(), pressedTabIndex);
        return true;
    }

    /**
     * Fires the panel's band button when a press landed on it, reporting whether it acted. Split from the
     * event above for the reason the tab activation is: what the button does can be checked without an
     * engine input event to raise.
     *
     * <p>No lift and no reselect rule. The button is one cell that is never the lit one, so it fires on
     * every press that reaches it; and it acts at once - whatever it opens is already on screen by the
     * release - so there is nothing for a held lift to report.
     *
     * @param placement the laid-out tab panel the renderer drew this frame
     * @param pointX    the press x in UI coordinates, the coordinates the placement is laid out in
     * @param pointY    the press y in UI coordinates
     * @return whether the press landed on the band button; false leaves the press to the tabs and the body
     */
    boolean activateBandButtonAtPoint(TabPanelPlacement placement, float pointX, float pointY) {

        if (!isBandButtonHoveredAt(placement, pointX, pointY)) {
            return false;
        }
        ControlActivation.activateCellIfActionable(placement.bandButton(), BAND_BUTTON_CELL);
        return true;
    }

    /**
     * Whether the pointer is on the panel's band button - the one hit-test the button answers, read by the
     * fade that lights it and by the press that fires it, so the two cannot come to disagree.
     *
     * <p>Gated on the panel presenting its band at all, exactly as the tabs are: the button stands in their
     * row and is wiped with them by the fold, so a point on a button behind the docked rail is a point on
     * bare screen. That is also why it is not gated the way the handle is - the handle draws past the fold
     * and is what brings a docked panel back, while the button goes behind it.
     *
     * @param placement the laid-out tab panel to test against
     * @param pointX    the point's x in UI coordinates
     * @param pointY    the point's y in UI coordinates
     * @return whether the pointer is on a band button the panel is presenting
     */
    boolean isBandButtonHoveredAt(TabPanelPlacement placement, float pointX, float pointY) {

        return isPresentingTabsOf(placement)
            && placement.containsPointInBandButton(pointX, pointY);
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
                isBandButtonHoveredAt(placement, pointX, pointY),
                resolveHoveredBodyCellAtPoint(placement, pointX, pointY),
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
     * <p>What the body does with the frame - its cells' fades and lifts, the arrival it answers, and the
     * report out to the host that built the control under the pointer - is the body controller's, handed
     * the same reading and the same pair of paces. Nothing about a moving list reaches this end: whether
     * one moved is state that end holds, being the end that moved it.
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

        // Everything the row does with this frame - its fades, the lift a press is holding on a tab, and any
        // blink still running - is charged in one call to the end that holds all of it.
        headerMotions.advanceTabMotionsForFrame(hover.tabIndex(), elapsedSeconds, durations);

        // Everything the body does with this frame - its cells' fades, the lifts running on them, and the
        // report out to whoever built the control under the pointer - is charged in one call to the end
        // that holds all of it. It travels on the same pair of paces as the row above it, which is the whole
        // of what the panel shares between its parts: what a fraction lifts a checkbox toward is that
        // widget's own paint, and a panel answering the pointer at two speeds reads as two panels.
        bodyController.advanceBodyInputMotionsForFrame(hover.bodyCell(), elapsedSeconds, durations);

        // The band button travels at the row's pace like the tabs it stands beside, the reading behind it
        // already gated with theirs.
        bandButtonHoverFade.advanceTowardHover(hover.isBandButtonHovered(), elapsedSeconds, durations);

        // The handle is never gated with the tabs - it draws past the fold and outlives it, being what brings
        // a docked panel back.
        notchHoverFade.advanceTowardHover(hover.isNotchHovered(), elapsedSeconds, durations);
    }

    /**
     * Which header tab a point falls on, given what the panel is currently showing - the one hit-test the
     * panel answers the header with, read by the hover that lights a tab and by the press that fires one. Two
     * readers of one geometry rather than two hit-tests that happen to agree: the tab that lights and the tab
     * a press lands on are the same tab because they are the same answer.
     *
     * <p>It answers geometry and visibility and nothing else. Whether pressing that tab would <em>do</em>
     * anything is the press path's own question, settled after this by {@link
     * ControlActivation#activateCellIfActionable} - which is what lets the lit tab light while firing nothing.
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
            return ControlHitResolver.NO_CELL_RESOLVED;
        }
        // Through the body's own control resolver rather than a hit-test of the header's own: the header is
        // an ordinary laid-out control, and one mapping of a row miss onto "no cell" is one place for the
        // unboxing trap that mapping carries to be got wrong.
        return ControlHitResolver.resolveHitCell(placement.tabsHeader(), pointX, pointY);
    }

    // Which body cell a point falls on, given what the panel is currently drawing - the body's half of the
    // frame's reading. Through the body controller's own walk rather than a hit-test of this class's own,
    // so the cell that lights and the cell a press lands on are the same cell because they are the same
    // answer, and the viewport and box gates that walk carries are inherited rather than restated.
    //
    // A hit is reduced to where it landed, what kind of thing was reached, and where that control wants the
    // reading told - the control it names being the press's business alone: a fade is held against the
    // slot, and a hover that carried the widget on could come to be keyed by it. Both readings off the
    // control are settled here, while the walk still has it, so nothing downstream has to ask a second time.
    private static HoveredBodyCell resolveHoveredBodyCellAtPoint(
            TabPanelPlacement placement,
            float pointX,
            float pointY) {

        var hitCell = ControlHitResolver.resolveHitBodyCell(placement.body(), pointX, pointY);

        return hitCell == null
            ? null
            : new HoveredBodyCell(
                hitCell.slot(),
                hitCell.resolveArrivalTarget(),
                hitCell.resolveHoverReport());
    }

    // Answers the pointer reaching any of the panel's hoverable parts, once per arrival, at the level its
    // own kind of thing is owed. A tab, the band button and the handle answer alike because all three are
    // the panel's own furniture - each moves the player between whole views - while a body cell answers as
    // whatever it is, which the walk that found it settled. No part needed a detection of its own: every one
    // of them is already resolved once a frame for its fade.
    //
    // A moment rather than a position, which is why none is read off a fade: the pointer resting on a part
    // holds its fade at the top for as long as it stays, and a sound taken from that would be a tone rather
    // than a tick.
    private void soundArrivalsAt(TabPanelHover hover) {

        // Every latch stepped before any is read. Each latches what the pointer is on this frame, so a
        // short-circuit would leave the unread ones holding a stale reading - and then stay silent on the
        // frame the pointer did come back to one of them, that stale latch saying it never left.
        var hasReachedTab = headerMotions.detectTabArrivalAt(hover.tabIndex());
        var hasReachedBandButton = bandButtonHoverArrival.detectArrival(hover.isBandButtonHovered());
        var hasReachedNotch = notchHoverArrival.detectArrival(hover.isNotchHovered());
        var hasReachedBodyCell = bodyController.detectBodyCellArrivalAt(hover.resolveBodyCellSlot());

        // At most one of them can have fired: there is one pointer, and no two of the panel's parts occupy
        // the same point. So this picks the cue of whatever was reached rather than composing an answer out
        // of several, and a second arrival in one frame would be a hit-test fault rather than a moment two
        // sounds are owed for.
        if (hasReachedTab || hasReachedBandButton || hasReachedNotch) {
            sounds.soundPointerArrivalAt(PointerArrivalTarget.PANEL_CHROME);
            return;
        }
        if (hasReachedBodyCell) {
            sounds.soundPointerArrivalAt(hover.bodyCell().arrivalTarget());
        }
    }
}
