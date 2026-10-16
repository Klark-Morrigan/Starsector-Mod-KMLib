package kmlib.starsector.ui.compatibility;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import kmlib.starsector.compatibility.GameReachReporter;
import kmlib.starsector.compatibility.ReportedFailure;
import kmlib.starsector.strings.KmlibStringKeys;
import kmlib.starsector.ui.colour.StarsectorUiColour;
import kmlib.starsector.ui.coreui.ModalOverlays;
import kmlib.starsector.ui.coreui.OverlayPresence;
import kmlib.starsector.ui.map.probes.ShownMapTab;
import kmlib.starsector.ui.modal.ModalPanel;
import kmlib.starsector.ui.modal.ModalPanelBody;

import org.lwjgl.input.Keyboard;

import java.util.Set;
import java.util.function.BooleanSupplier;
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
 * <p>The surface, its modality and its fade are {@link ModalPanel}'s. What this adds is the notice
 * itself, and a hold on the screen through {@link ModalOverlays} for as long as the panel is up, so
 * whatever stands aside for the game's own modals stands aside for this one too.
 *
 * <p><b>Three ways out, deliberately.</b> The button, a key, and the screen going away under it. A
 * notice that could only be dismissed through its own button would trap a player on a screen it had
 * just claimed every event on if that button ever failed to draw.
 */
public final class CompatibilityNoticePanel {

    // What the button reports itself as. Its own object rather than a string, there being one
    // button and nothing to tell it from by name.
    private static final Object CONFIRM_BUTTON_ID = new Object();

    // Which keys take a one-button notice off the screen. The game binds both its keyboard confirm
    // and its keyboard cancel to the single option, so enter and escape both dismiss; space is the
    // third a player reaches for without being told.
    private static final Set<Integer> CLOSED_BY_ANY_ANSWER = Set.of(
        Keyboard.KEY_ESCAPE,
        Keyboard.KEY_RETURN,
        Keyboard.KEY_SPACE);

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

    // The surface the notice stands on. Files nothing where it cannot stand: a notice that is not
    // raised is left on the record for the campaign's dialog, so no report is lost by it.
    private final ModalPanel modalPanel;

    // The reading handed to whatever stands aside for a modal, held so the same one is taken back.
    private final Supplier<OverlayPresence> screenHold;

    private CompatibilityNoticePanel(BooleanSupplier isMapTabShowing) {

        this.modalPanel = new ModalPanel(
            GameReachReporter.UNREPORTED,
            isMapTabShowing,
            ModalPanel.GAME_PROMPT_DURATIONS,
            CLOSED_BY_ANY_ANSWER,
            this::dismissNotice,
            this::releaseScreen);

        this.screenHold = modalPanel::resolvePanelPresence;
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

        return raiseNoticeOnScreen(ShownMapTab::isMapTabShowing);
    }

    /**
     * Hands the screen back, leaving the notice to fade from wherever it stands.
     *
     * <p>The panel outlives the press for the length of that fall, and claims nothing for it.
     * Standing aside is what lets whatever thinned itself under the notice come back on the same
     * curve rather than snapping.
     */
    public void dismissNotice() {

        modalPanel.dismissPanel();
    }

    /**
     * @return whether the notice is still on screen, fading out included, which is false from the
     *         frame it comes down
     */
    public boolean isNoticeRaised() {

        return modalPanel.resolvePanelPresence().isShowing();
    }

    /**
     * Draws {@code failure} into the notice already on screen.
     *
     * @param failure what the notice is about
     */
    public void showFailure(ReportedFailure failure) {

        modalPanel.showBody(noticePanel -> new NoticeBody(noticePanel, failure, this::dismissNotice));
    }

    /**
     * The raise itself, over a read of the screen its caller supplies. Package-private so the rule
     * can be driven without a running game, the live read being the half that needs one.
     *
     * @param isMapTabShowing whether a map is on screen: what says there is a screen to stand on at
     *                        all, and what the panel then watches to know when that screen has gone.
     *                        It fails closed, and reports a map screen it cannot find itself, so a
     *                        raise that answers nothing has already said why
     * @return the panel, or null where none could be stood up
     */
    static CompatibilityNoticePanel raiseNoticeOnScreen(BooleanSupplier isMapTabShowing) {

        if (!isMapTabShowing.getAsBoolean()) {
            return null;
        }

        var notice = new CompatibilityNoticePanel(isMapTabShowing);
        if (!notice.modalPanel.raisePanel()) {
            return null;
        }

        // Said the moment the panel is up rather than once it is filled: input has to stand down
        // from the first frame, and what fills the box a frame later changes nothing about that.
        ModalOverlays.holdScreen(notice.screenHold);

        return notice;
    }

    // The panel is off the screen, so nothing is left for anything to stand aside for.
    private void releaseScreen() {

        ModalOverlays.releaseScreen(screenHold);
    }

    // The notice's widgets: the text and button in one element, centred on the screen and measured
    // rather than assumed, because what a consuming mod says it lost is its own sentence and can run
    // to any length.
    private static final class NoticeBody
            implements ModalPanelBody {

        // The element holding the text and the button, held so it can be taken off again.
        private final TooltipMakerAPI box;

        // Where the layout settled the box, which is what the backdrop is painted against.
        private final PositionAPI boxPlacement;

        NoticeBody(CustomPanelAPI noticePanel, ReportedFailure failure, Runnable onConfirmPressed) {

            var settings = Global.getSettings();

            box = noticePanel.createUIElement(BOX_WIDTH - BOX_PADDING * 2, BOX_BUILD_HEIGHT, false);

            // The button reports to the element holding it, the panel's own plugin hearing nothing of
            // it. Set before the button is added so the button is built with somewhere to report.
            box.setActionListenerDelegate((buttonId, data) -> onConfirmPressed.run());

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
                (settings.getScreenWidth() - BOX_WIDTH) / 2 + BOX_PADDING,
                (settings.getScreenHeight() - boxHeight) / 2 + BOX_PADDING);

            // The fill is painted against the box rather than the text in it, so the placement carries
            // the padding the text is inset by.
            boxPlacement.setSize(BOX_WIDTH - BOX_PADDING * 2, boxHeight - BOX_PADDING * 2);
        }

        @Override
        public void removeFromPanel(CustomPanelAPI noticePanel) {

            noticePanel.removeComponent(box);
        }

        @Override
        public void renderFills(float alphaMult) {

            CompatibilityNoticeBody.renderFills(boxPlacement, BOX_PADDING, alphaMult);
        }
    }
}
