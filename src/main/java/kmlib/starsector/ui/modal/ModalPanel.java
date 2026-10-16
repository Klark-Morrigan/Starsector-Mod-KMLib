package kmlib.starsector.ui.modal;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import kmlib.animation.TraverseDurations;
import kmlib.starsector.compatibility.GameReachReporter;
import kmlib.starsector.ui.coreui.ModalOverlays;
import kmlib.starsector.ui.coreui.OverlayPresence;

import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * A modal surface over a core screen: a screen-sized panel in the core UI, what it claims while it
 * holds the screen, how far onto the screen it is painted, and when it comes off again.
 *
 * <p>Kept apart from whatever it shows, because the two change for different reasons. What is in the
 * box, and when the player may start or stop, is the caller's. Where the widgets stand, what the frame
 * hooks do, and how long the box lingers after the press belong to the surface.
 *
 * <p><b>Built from the game's own widgets</b> - a panel from {@code SettingsAPI.createCustom} stood in
 * the core UI's own tree by {@link CoreUiOverlayPanels}. Nothing here is painted into another render
 * pass, so the panel reads as part of the game rather than as something drawn over it.
 *
 * <p><b>Modality is supplied here, because the game does not supply it.</b> A panel added this way is
 * an ordinary child: nothing dims behind it and nothing stops the screen underneath being dispatched
 * to. So the body paints its own backdrop, through this plugin's render hook, and this claims every
 * event the panel's own widgets have not already taken - the engine dispatching those widgets before
 * this plugin, which is what leaves a blanket claim safe. Which events that claim takes is
 * {@link ModalPanelEventResponse}'s; this acts on the answer.
 *
 * <p>It is not registered with {@link ModalOverlays}: a caller that wants to be read as a modal hands
 * {@link #resolvePanelPresence()} there itself, since whether it should be is the caller's to decide.
 *
 * <p><b>The fade is written as the panel's opacity</b>, which the engine multiplies into the alpha it
 * hands every widget the panel holds and this plugin's render hook alike. So one write a frame fades
 * the box, its controls and the fills beneath them as one piece, and a panel the engine is itself
 * fading keeps that fade over this one rather than having it overwritten.
 *
 * <p>The panel therefore outlives the press that dismisses it, coming off the screen at the end of the
 * fall. For that length it is on screen and claims nothing.
 *
 * <p>What it cannot answer for itself it reports rather than decides: a press on the way out, and the
 * panel coming off the screen, are both handed back, because what either means for what the panel
 * shows is the caller's to say.
 */
public final class ModalPanel {

    // How far the panel is offset from the corner it hangs off, which is not at all: it is the size of
    // the screen and the core UI it hangs in is too, so the two corners coincide.
    private static final float NO_OFFSET = 0f;

    /** Escape alone closes the panel, which is how the game's own dialogs with a choice in them close. */
    public static final Set<Integer> CLOSED_BY_ESCAPE = Set.of(Keyboard.KEY_ESCAPE);

    /**
     * The pace the game's own prompts arrive and leave at: every confirmation a core screen raises in
     * front of itself is shown at these two durations. A panel moving at this pace moves with them.
     */
    public static final TraverseDurations GAME_PROMPT_DURATIONS = new TraverseDurations(0.3f, 0.2f);

    // The keys whose press asks the panel to close.
    private final Set<Integer> closeKeyCodes;

    // Whether the panel holds the screen, and how far onto it the box is painted.
    private final ModalPanelFade fade;

    // Whether the screen the panel was opened over is still the one showing, asked every frame.
    private final BooleanSupplier isHostScreenShowing;

    // A press on the way out, for the caller to answer for. Reported rather than acted on here: this
    // knows the press happened, and what it costs the caller is not this one's to decide.
    private final Runnable onCloseRequested;

    // The panel is off the screen, at the end of its fall or with the screen it stood over. Reported
    // for the same reason: whatever the caller tied to the panel being up is the caller's to let go.
    private final Runnable onPanelTakenDown;

    // Where a core UI that will not take the panel is filed.
    private final GameReachReporter reporter;

    // The widgets currently in the panel, replaced whole on every change.
    private ModalPanelBody body;

    // The screen-sized panel standing in the core UI, or null while none does. Stood up on the first
    // open and kept through the fall, so a reopen during the fall reuses it rather than standing a
    // second one over the first.
    private CustomPanelAPI panel;

    /**
     * @param reporter            where a core UI that will not take the panel is filed, for the mod
     *                            standing it
     * @param isHostScreenShowing whether the screen the panel was opened over is still showing; a
     *                            false answer takes the panel down at once
     * @param durations           how long the rise and the fall each take, usually
     *                            {@link #GAME_PROMPT_DURATIONS}
     * @param closeKeyCodes       the keys, as {@link Keyboard} codes, whose press is reported as a
     *                            request to close, usually {@link #CLOSED_BY_ESCAPE}
     * @param onCloseRequested    what a press on the way out is reported to
     * @param onPanelTakenDown    what the panel coming off the screen is reported to, whether its fall
     *                            ran out or the screen under it went
     */
    public ModalPanel(
            GameReachReporter reporter,
            BooleanSupplier isHostScreenShowing,
            TraverseDurations durations,
            Set<Integer> closeKeyCodes,
            Runnable onCloseRequested,
            Runnable onPanelTakenDown) {

        this.reporter = Objects.requireNonNull(reporter, "reporter");
        this.isHostScreenShowing = Objects.requireNonNull(isHostScreenShowing, "isHostScreenShowing");
        this.fade = new ModalPanelFade(durations);
        this.closeKeyCodes = Set.copyOf(closeKeyCodes);
        this.onCloseRequested = Objects.requireNonNull(onCloseRequested, "onCloseRequested");
        this.onPanelTakenDown = Objects.requireNonNull(onPanelTakenDown, "onPanelTakenDown");
    }

    /**
     * Hands the screen back. The box stays for the length of its fall and comes off at the end of it.
     */
    public void dismissPanel() {
        fade.dismissPanel();
    }

    /**
     * @return whether the panel holds the screen, which is false from the press that dismisses it
     *         even while the box is still painted
     */
    public boolean isPanelRaised() {
        return fade.isPanelRaised();
    }

    /**
     * Takes the screen, standing the panel up first where there is none to reuse.
     *
     * <p>Painted at nothing on the frame it is stood up, so the first frame is the start of the rise
     * rather than a flash of the whole box before the fade has been stepped once.
     *
     * @return whether the panel is up, false only where the core UI could not be reached to stand one
     *         in - which leaves the screen exactly as it was rather than half-opened
     */
    public boolean raisePanel() {

        if (panel == null && !standPanelUp()) {
            return false;
        }

        fade.raisePanel();
        return true;
    }

    /**
     * @return what this is doing on one frame, for whatever stands aside for it
     */
    public OverlayPresence resolvePanelPresence() {

        // Shaped here rather than by the clock beneath, which is state and knows nothing of screens.
        // What is raised over one is this, so how that is reported is this one's to say.
        return new OverlayPresence(
            fade.isPanelRaised(),
            fade.resolveFadeFraction());
    }

    /**
     * Replaces the widgets in the panel with freshly built ones.
     *
     * <p>Takes a builder rather than a built body because what goes in the box is the caller's to
     * compose and the panel it is composed against is this one's to hold - so neither has to publish
     * what the other keeps.
     *
     * <p>Builds nothing while no panel is standing - before the first raise, or once the panel has
     * come down - since there is nothing to build the widgets against.
     *
     * @param buildBody builds the widgets against the panel they will stand in
     */
    public void showBody(Function<CustomPanelAPI, ? extends ModalPanelBody> buildBody) {

        if (panel == null) {
            return;
        }

        if (body != null) {
            body.removeFromPanel(panel);
        }
        body = buildBody.apply(panel);
    }

    // Where the paint stands, written onto the panel as its opacity - the one write that fades every
    // part of the panel together.
    private void paintFadeOntoPanel() {
        panel.setOpacity(fade.resolveFadeFraction());
    }

    // Stands the screen-sized panel in the core UI, painted at nothing.
    private boolean standPanelUp() {

        var settings = Global.getSettings();
        var newPanel = settings.createCustom(
            settings.getScreenWidth(),
            settings.getScreenHeight(),
            new ModalPanelPlugin());

        var placement = CoreUiOverlayPanels.attachOverlayPanel(newPanel, reporter);
        if (placement == null) {
            return false;
        }

        placement.inTL(NO_OFFSET, NO_OFFSET);
        panel = newPanel;

        paintFadeOntoPanel();
        return true;
    }

    // Takes the panel off the screen and forgets everything built into it, the fade included: what
    // comes down here is not fading, it is gone.
    private void takePanelDown() {

        CoreUiOverlayPanels.detachOverlayPanel(panel);

        panel = null;
        body = null;

        fade.dropFade();
        onPanelTakenDown.run();
    }

    // The panel's own frame hooks: the claim that makes the panel modal, the fade that brings it in
    // and out, and the guard that takes it down with the screen it was opened on. Glue by design -
    // a plugin exists only inside a panel the game built, so every rule it acts on is stated
    // somewhere it can be checked without one.
    private final class ModalPanelPlugin
            implements CustomUIPanelPlugin {

        @Override
        public void positionChanged(PositionAPI position) {
        }

        @Override
        public void renderBelow(float alphaMult) {

            // Under every widget the panel holds, which is where the game draws the interiors of its own
            // custom panels: the dim that stands the screen down and the box's own surface. Nothing else
            // paints them - the game publishes a rectangle component that strokes and none that fills, so
            // the rule around the box is a widget and the two filled areas are not. The alpha handed in
            // already carries the panel's own fade, the panel's opacity being where that is written.
            if (body != null) {
                body.renderFills(alphaMult);
            }
        }

        @Override
        public void render(float alphaMult) {
        }

        @Override
        public void advance(float amount) {

            // A panel this has already let go of, which is what a detach that could not reach the core
            // UI leaves behind: still advanced by whoever holds it, and no longer this one's to paint.
            // Asked first because everything below writes through a panel this no longer has.
            if (panel == null) {
                return;
            }

            // The panel hangs from the core UI, which outlives the screen the panel was opened on -
            // so leaving that screen has to be noticed rather than waited for. Down at once rather than
            // faded: there is no screen left under it to fade against.
            if (!isHostScreenShowing.getAsBoolean()) {

                takePanelDown();
                return;
            }

            fade.advanceFade(amount);
            paintFadeOntoPanel();

            if (fade.isSettledDown()) {
                takePanelDown();
            }
        }

        @Override
        public void processInput(List<InputEventAPI> events) {

            for (var event : events) {

                var response = ModalPanelEventResponse.resolveResponseTo(
                    event,
                    isPanelRaised(),
                    closeKeyCodes);

                if (response == ModalPanelEventResponse.LEAVE_ALONE) {
                    continue;
                }

                event.consume();

                // Nothing after the press is this pass's to answer for: from here on the panel is only
                // fading, and the events under it are the screen's again.
                if (response == ModalPanelEventResponse.CLOSE_PANEL) {

                    onCloseRequested.run();
                    return;
                }
            }
        }

        // Buttons in the body report through the action listener of the element holding them, so a
        // press never needs to reach the panel's own plugin.
        @Override
        public void buttonPressed(Object buttonId) {
        }
    }
}
