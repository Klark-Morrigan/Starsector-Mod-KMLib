package kmlib.starsector.ui.compatibility;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import kmlib.animation.TraverseDurations;
import kmlib.animation.TraverseFraction;
import kmlib.starsector.compatibility.CompatibilityFailure;
import kmlib.starsector.strings.KmlibStringKeys;
import kmlib.starsector.ui.colour.StarsectorUiColour;
import kmlib.starsector.ui.coreui.CoreUiOverlayPanels;
import kmlib.starsector.ui.coreui.ModalOverlays;
import kmlib.starsector.ui.coreui.OverlayPresence;
import kmlib.starsector.ui.map.probes.ShownMapTab;

import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.function.Supplier;

/**
 * The compatibility notice as a panel standing on the screen the failure was found on, rather than
 * as a dialog the player meets after leaving it.
 *
 * <p>Why it exists at all is a timing fact. A binding to a renderer breaks inside a map render
 * pass, and the script that shows the dialog is a transient one on the sector, which the campaign
 * engine does not advance while a core screen is up - so a failure found on the map is reported
 * only once the player has left it, by which time the screen it was about is gone. A panel hung in
 * the core UI's own tree is advanced by the screen holding it, so it can be raised on the frame the
 * failure is found.
 *
 * <p><b>Modality is supplied here, because the game does not supply it.</b> A panel added this way
 * is an ordinary child: nothing dims behind it and nothing stops the screen underneath being
 * dispatched to. So the body paints its own backdrop through this plugin's render hook, and this
 * claims every event the panel's own widgets have not already taken - the engine dispatching those
 * widgets first, which is what leaves a blanket claim safe.
 *
 * <p><b>Three ways out, deliberately.</b> The button, the escape key, and the screen going away
 * under it. A notice that could only be dismissed through its own button would trap a player on a
 * screen it had just claimed every event on if that button ever failed to draw.
 */
public final class CompatibilityNoticePanel {

    // What the button reports itself as. Its own object rather than a string, there being one
    // button and nothing to tell it from by name.
    private static final Object CONFIRM_BUTTON_ID = new Object();

    // How far the panel is offset from the corner it hangs off, which is not at all: it is the size
    // of the screen and the core UI it hangs in is too, so the two corners coincide.
    private static final float NO_OFFSET = 0f;

    // The box's width, and the room left around the text inside it. Wide enough for a consuming
    // mod's own sentence to take two lines rather than five.
    private static final float BOX_WIDTH = 660f;
    private static final float BOX_PADDING = 20f;

    // The height the box is built at before its contents are measured. Generous, because an element
    // created too short clips what is drawn into it before there is anything to measure.
    private static final float BOX_BUILD_HEIGHT = 600f;

    // The rule around the box, in the base colour the game frames its own dialogs with. Stroked by
    // a widget of the game's, the game publishing one that strokes and none that fills.
    private static final float BORDER_THICKNESS = 2f;

    // The pace the game's own prompts arrive and leave at, so two dialogs over one screen move
    // together and anything riding this fade rides the same curve it rides theirs.
    private static final TraverseDurations MODAL_DURATIONS = new TraverseDurations(0.3f, 0.2f);

    // The reading handed to whatever stands aside for a modal, held so the same one is taken back.
    private final Supplier<OverlayPresence> screenHold = this::resolveScreenPresence;

    // How far onto the screen the notice is painted. Apart from whether it holds the screen,
    // because the two part company for the length of a fade: input comes back on the press while
    // the paint runs on, and a notice still dissolving must claim nothing or it eats the click
    // after it.
    private final TraverseFraction fadeProgress = new TraverseFraction();

    // Where a map was last found, so the panel comes down with the screen it was raised on.
    private final Supplier<Object> resolveShownMapTab;

    // The screen-sized panel standing in the core UI, or null once it has come down.
    private CustomPanelAPI panel;

    // Where the layout settled the box, which is what the backdrop is painted against.
    private PositionAPI boxPlacement;

    // Whether the notice holds the screen. Moves on the press, never on the fade.
    private boolean isRaised;

    private CompatibilityNoticePanel(Supplier<Object> resolveShownMapTab) {

        this.resolveShownMapTab = resolveShownMapTab;
    }

    /**
     * Stands an empty notice on the screen in force, for {@link #showFailure} to fill.
     *
     * <p>Empty, and filled in a second step, so that a failure is taken off the record only once
     * there is something on screen to show it in. A caller whose raise answers nothing still holds
     * its failure and can report it the other way.
     *
     * @return the panel standing on screen, or null where no screen was found to stand it on -
     *         which leaves the screen exactly as it was
     */
    public static CompatibilityNoticePanel raiseNoticeOnScreen() {

        return raiseNoticeOnScreen(ShownMapTab::resolveShownMapTab);
    }

    /**
     * Draws {@code failure} into the notice already on screen.
     *
     * @param failure what the notice is about
     */
    public void showFailure(CompatibilityFailure failure) {

        if (panel == null) {
            return;
        }
        var settings = Global.getSettings();

        drawNoticeInto(panel, failure, settings.getScreenWidth(), settings.getScreenHeight());
    }

    /**
     * @return whether the notice is still on screen, which is false from the frame it comes down
     */
    public boolean isNoticeRaised() {

        return panel != null;
    }

    /**
     * Hands the screen back, leaving the notice to fade from wherever it stands.
     *
     * <p>The panel outlives the press for the length of that fall, and claims nothing for it.
     * Standing aside is what lets whatever thinned itself under the notice come back on the same
     * curve rather than snapping.
     */
    public void dismissNotice() {

        isRaised = false;
    }

    /**
     * The raise itself, over a reach for the screen its caller supplies. Package-private so the
     * rule can be driven against a stood-up tree, the live reach being the half that needs a game.
     *
     * @param resolveShownMapTab the reach for the map tab on screen: what says there is a screen to
     *                           stand on at all, and what the panel then watches to know when that
     *                           screen has gone. It reports a map screen it cannot find itself, so
     *                           a raise that answers nothing has already said why
     * @return the panel, or null where none could be stood up
     */
    static CompatibilityNoticePanel raiseNoticeOnScreen(Supplier<Object> resolveShownMapTab) {

        var notice = new CompatibilityNoticePanel(resolveShownMapTab);

        if (!notice.isScreenShowing()) {
            return null;
        }

        return notice.standPanelUp() ? notice : null;
    }

    // Stands the screen-sized panel in the core UI. Answers false without leaving anything behind
    // where the core UI could not be reached.
    private boolean standPanelUp() {

        var settings = Global.getSettings();
        var newPanel = settings.createCustom(
            settings.getScreenWidth(),
            settings.getScreenHeight(),
            new NoticePanelPlugin());

        var placement = CoreUiOverlayPanels.attachOverlayPanel(newPanel);
        if (placement == null) {
            return false;
        }
        placement.inTL(NO_OFFSET, NO_OFFSET);
        panel = newPanel;
        isRaised = true;

        // Painted at nothing on the frame it is stood up, so the first frame is the start of the
        // rise rather than a flash of the whole box before the fade has been stepped once.
        paintFadeOntoPanel();

        // Said the moment the panel is up rather than once it is filled: input has to stand down
        // from the first frame, and what fills the box a frame later changes nothing about that.
        ModalOverlays.holdScreen(screenHold);

        return true;
    }

    // Takes the panel off the screen and forgets everything built into it, the fade included: what
    // comes down here is not fading, it is gone.
    private void takePanelDown() {

        ModalOverlays.releaseScreen(screenHold);
        CoreUiOverlayPanels.detachOverlayPanel(panel);

        panel = null;
        boxPlacement = null;
        fadeProgress.dropToRest();
    }

    // Where the paint stands, written onto the panel as its opacity - the one write that fades
    // every part of the notice together, the engine multiplying it into the alpha it hands each
    // widget and this plugin's render hook alike.
    private void paintFadeOntoPanel() {

        panel.setOpacity(fadeProgress.getEasedValue());
    }

    // What this is doing on one frame, for whatever stands aside for a modal.
    private OverlayPresence resolveScreenPresence() {

        return panel == null
            ? OverlayPresence.NONE
            : new OverlayPresence(isRaised, fadeProgress.getEasedValue());
    }

    // Fails closed, the opposite way round from the reads it goes through: a reach that raises
    // means the widget tree cannot be walked at all, and a notice nobody can place should not be
    // stood on a screen it cannot see.
    private boolean isScreenShowing() {

        try {
            return resolveShownMapTab.get() != null;

        } catch (RuntimeException cannotReachScreen) {
            return false;
        }
    }

    // Builds the box, measures what went into it, and centres it on the screen. Measured rather
    // than assumed because what a consuming mod says it lost is its own sentence and can run to
    // any length.
    private void drawNoticeInto(
            CustomPanelAPI noticePanel,
            CompatibilityFailure failure,
            float screenWidth,
            float screenHeight) {

        var box = noticePanel.createUIElement(BOX_WIDTH - BOX_PADDING * 2, BOX_BUILD_HEIGHT, false);

        CompatibilityNoticeBody.fillNoticeBody(
            box,
            failure,
            CONFIRM_BUTTON_ID,
            KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_NOTICE_CONFIRM_BUTTON));

        var boxHeight = box.getHeightSoFar() + BOX_PADDING * 2;

        // The rule around the box, laid outside the text by the padding on every side. Added
        // without advancing the layout, so it frames what was measured rather than adding to it.
        var border = box.createRect(StarsectorUiColour.VANILLA_PLAYER_BASE.resolve(), BORDER_THICKNESS);
        box.addCustomDoNotSetPosition(border)
            .getPosition()
            .inTL(-BOX_PADDING, -BOX_PADDING)
            .setSize(BOX_WIDTH, boxHeight);

        boxPlacement = noticePanel.addUIElement(box);
        boxPlacement.inTL(
            (screenWidth - BOX_WIDTH) / 2 + BOX_PADDING,
            (screenHeight - boxHeight) / 2 + BOX_PADDING);

        // The fill is painted against the box rather than the text in it, so the placement carries
        // the padding the text is inset by.
        boxPlacement.setSize(BOX_WIDTH - BOX_PADDING * 2, boxHeight - BOX_PADDING * 2);
    }

    // The panel's own frame hooks. Glue by design - a plugin exists only inside a panel the game
    // built, so every rule it acts on is stated somewhere it can be checked without one.
    private final class NoticePanelPlugin implements CustomUIPanelPlugin {

        @Override
        public void positionChanged(PositionAPI position) {
        }

        @Override
        public void renderBelow(float alphaMult) {

            CompatibilityNoticeBody.renderFills(boxPlacement, BOX_PADDING, alphaMult);
        }

        @Override
        public void render(float alphaMult) {
        }

        @Override
        public void advance(float amount) {

            // A panel this has already let go of, which is what a detach that could not reach the
            // core UI leaves behind: still advanced by whoever holds it, and no longer ours.
            if (panel == null) {
                return;
            }

            // The panel hangs from the core UI, which outlives the screen the notice was raised on,
            // so leaving that screen has to be noticed rather than waited for. Down at once rather
            // than faded: there is no screen left under it to fade against.
            if (!isScreenShowing()) {
                takePanelDown();
                return;
            }

            fadeProgress.advanceTowardEnd(isRaised, amount, MODAL_DURATIONS);
            paintFadeOntoPanel();

            // Off the screen once the fall has run out, a notice still fading being one the player
            // is watching leave.
            if (!isRaised && fadeProgress.hasSettledAtRest()) {
                takePanelDown();
            }
        }

        @Override
        public void processInput(List<InputEventAPI> events) {

            // Nothing is claimed once the screen has been handed back: the notice is only fading
            // from here, and the events under it are the screen's again.
            if (panel == null || !isRaised) {
                return;
            }

            for (var event : events) {

                if (event.isConsumed()) {
                    continue;
                }

                // Read before it is claimed: a consumed event refuses its value, and the read is
                // the one thing this needs from it.
                var isDismissRequested = event.isKeyDownEvent() && isDismissKey(event.getEventValue());

                // Claimed whether or not it dismisses: the screen underneath is stood down for as
                // long as the notice is up, which is what makes it read as a modal rather than as
                // something drawn over a map still taking clicks.
                event.consume();

                if (isDismissRequested) {
                    dismissNotice();
                    return;
                }
            }
        }

        @Override
        public void buttonPressed(Object buttonId) {

            if (buttonId == CONFIRM_BUTTON_ID) {
                dismissNotice();
            }
        }

        // Which keys take a one-button notice off the screen. The game's own binds both its
        // keyboard confirm and its keyboard cancel to the single option, so enter and escape both
        // dismiss; space is the third a player reaches for without being told.
        private boolean isDismissKey(int keyCode) {

            return keyCode == Keyboard.KEY_ESCAPE
                || keyCode == Keyboard.KEY_RETURN
                || keyCode == Keyboard.KEY_SPACE;
        }
    }
}
