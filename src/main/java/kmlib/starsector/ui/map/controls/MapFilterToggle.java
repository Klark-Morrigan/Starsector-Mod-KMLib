package kmlib.starsector.ui.map.controls;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import kmlib.logging.SessionWarning;
import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.tooltip.Tooltips;
import kmlib.text.KmlibStrings;

import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.util.function.Consumer;

/**
 * A toggle standing on a map's filter row: how one is put there, and the handle whoever put it there
 * drives it by.
 *
 * <p>Its size is measured off the row rather than stated. The {@code M} screen's strip lays its
 * buttons at one size and the intel visor's band at a smaller one, so a control appended at a size
 * of its own would be right on one screen and wrong on the other - and would go on being wrong the
 * day the game changes either. Measuring it makes one path serve both screens with no branch on
 * which is up, and the measurement is taken from the row's neighbours rather than from the row's own
 * ideas: the height a row is laid at is the height of a button on it, and the width of the button
 * already standing at the end is the width the next one has to match to look like its sibling.
 *
 * <p>Whether there is room is asked before anything is built, because the row does not clip what it
 * holds. A button appended past the end of it is not refused and does not error - it renders off the
 * end of the strip, over whatever is there. So the width still free is measured, and an append that
 * would not fit is declined outright.
 *
 * <p>That guard is not there for the game's own rows, both of which have room to spare. It is there
 * for the row another mod has already appended to: first come, and the honest answer to arriving
 * second is to leave the row as it was found rather than to draw over somebody's control.
 *
 * <p>Every refusal here is the same refusal the build itself makes - no toggle, one line in the log,
 * and a caller with one case to handle. A control appended to another party's widget must not be
 * able to take anything down with it, and that includes being unable to take down the row it could
 * not get onto.
 */
public final class MapFilterToggle {

    private static final Logger LOG = Global.getLogger(MapFilterToggle.class);

    // What the game leaves between two buttons on either of its rows. Stated rather than measured
    // off the gap between neighbours: it is one number the row lays every one of its children by,
    // where a measured gap would be the same number arrived at by subtraction and undefined for the
    // row holding a single button.
    private static final float BUTTON_GAP = 3f;

    // Whether the game is asked to spell a bound key out after the button's words. Left false
    // because it writes nothing either way on the kind of button a filter row holds - and were that
    // to change, it would write a second announcement beside the one made here.
    private static final boolean IS_SHORTCUT_SPELLED_OUT = false;

    // LWJGL's "no key", which is what a keycode field holds once the player has cleared its binding.
    // A control handed this must end up with no key rather than with a key nothing can press.
    private static final int NO_SHORTCUT_KEYCODE = 0;

    // Says once per session that a row could not be measured or had no room, rather than on every
    // frame a caller reattaches. One holder for the refusals made here, those being one piece of news
    // to whoever asked - there is no button - so the first of them to happen is the one worth the
    // line. The build below keeps its own, a row it cannot recognise being news of a different kind.
    private static final SessionWarning WARNING = new SessionWarning(LOG);

    // The tooltip's own holder. A control that ended up without its hover is news of a different
    // kind from a row that would take no control at all: there is still a working box on the row,
    // and the player is short a sentence rather than a switch.
    private static final SessionWarning TOOLTIP_WARNING = new SessionWarning(LOG);

    // The key announcement's own holder, for the same reason: a control answering a key it does not
    // say is still a working control, and is news of a different kind from one that never went up.
    private static final SessionWarning SHORTCUT_WARNING = new SessionWarning(LOG);

    private final MapFilterRow row;

    private final ButtonAPI button;

    private MapFilterToggle(MapFilterRow row, ButtonAPI button) {
        this.row = row;
        this.button = button;
    }

    /**
     * Stands a toggle at the end of a row, at the size that row lays its own buttons at.
     *
     * @param row       the row to append to
     * @param label     the words on the button
     * @param onToggled what to run when it is clicked, which is called after the button has already
     *                  flipped its own state - so a caller reads that state back off the handle
     *                  returned here rather than tracking it
     * @return the handle to drive it by, or null when the row cannot be measured, has no room left,
     *         or is not a shape a button can be built on - each of which is logged once and none of
     *         which is an error, the row being somebody else's
     */
    public static MapFilterToggle appendToRow(MapFilterRow row, String label, Runnable onToggled) {

        var buttonSize = measureButtonFor(row);
        if (buttonSize == null) {
            return null;
        }

        var appendedButton = VanillaToggleFactory.appendToggle(row, label, buttonSize, onToggled);

        return adoptAppendedButton(row, appendedButton);
    }

    /**
     * Hangs a hover tooltip on the button, the way the game's own row hangs one on most of the
     * buttons already standing on it.
     *
     * <p>Asked of the toggle rather than of the button, because the button is not handed out: the
     * wrapper exists so that writing into another party's row stays this package's business, and a
     * caller reaching the widget to decorate it would be reaching past that.
     *
     * <p>Above the button, which is where the row's own tooltips sit - the row runs along the
     * bottom of both screens it appears on, so anywhere else is off the edge of the display.
     *
     * <p>A hover the control ends up without costs the hover and nothing else, which takes two
     * things rather than one. The engine's own attachment quietly does nothing when the target is
     * not the widget kind it expects, so a reworked row costs the words silently; and the surface
     * the attachment is made through is built here, so a substrate that refuses to build one is
     * caught rather than thrown at the caller - who has a working box already standing on a row,
     * and no way to take it off again if this were allowed to read as a failed attachment.
     *
     * @param width fixed tooltip width in pixels
     * @param body  painter invoked on every hover with the tooltip element to fill
     */
    public void attachTooltip(float width, Consumer<TooltipMakerAPI> body) {

        try {
            Tooltips.attachWithOwnSurface(
                button,
                TooltipMakerAPI.TooltipLocation.ABOVE,
                width,
                body);

        } catch (RuntimeException failure) {

            TOOLTIP_WARNING.warnOnce(
                "The control appended to the map's filter row could not be given a hover tooltip; "
                    + "it is left standing without one.",
                failure);
        }
    }

    /**
     * Gives the button a key that clicks it.
     *
     * <p>Asked of the toggle rather than of the button for the reason the hover is: the widget is
     * not handed out, so what can be done to it is what this exposes.
     *
     * <p>Nothing has to be arranged for the key to be live in the right places and dead everywhere
     * else. A button answers its key while it is on screen and its row is taking input, so a key
     * bound here reaches exactly the screens the control stands on - there is no listener to
     * register, none to take away, and no key at all on a screen the control never reached.
     *
     * <p>The key is written into the button's words as well as bound, because a key nothing shows
     * is a key nobody presses. The row does that for the buttons it keys itself and cannot do it
     * for one keyed this way, so it is done here and in the row's own format - which is what keeps
     * the control reading as one of the row's rather than as the only button on the strip with a
     * secret.
     *
     * @param keycode the LWJGL code to bind, 0 leaving the button with no key at all - which is
     *                what a keycode field holds after the player clears its binding, so binding it
     *                literally would give the control a key that cannot be pressed and no way to
     *                tell that from a real one
     */
    public void bindShortcut(int keycode) {

        if (keycode <= NO_SHORTCUT_KEYCODE) {
            return;
        }

        button.setShortcut(keycode, IS_SHORTCUT_SPELLED_OUT);

        announceShortcutInLabel(keycode);
    }

    /**
     * Whether the button is currently ticked.
     *
     * @return the button's own state, which it flips itself on being clicked
     */
    public boolean isChecked() {

        return button.isChecked();
    }

    /**
     * Whether this toggle is still standing on the row that is on screen.
     *
     * <p>The one question a holder has to keep asking. The map widget builds its row inside its own
     * constructor, so every open of the screen produces a new row while this goes on holding a
     * button attached to the old one - which draws nowhere, reports nothing, and looks from here
     * exactly like a button that is still where it was put.
     *
     * @param shownRow the row currently on screen, or null when no map is showing one
     * @return whether this toggle stands on that row
     */
    public boolean isStillAttachedTo(MapFilterRow shownRow) {

        return row.isSameRowAs(shownRow);
    }

    /**
     * Ticks or unticks the button without reporting a click.
     *
     * <p>What a caller seeds a freshly appended toggle from, so the control opens showing the state
     * the player left it in rather than the state a new button happens to start in.
     *
     * @param isChecked whether the button should show as ticked
     */
    public void setChecked(boolean isChecked) {

        button.setChecked(isChecked);
    }

    // Says which key the button answers to, in its own words.
    //
    // The game says it for the buttons it keys itself and cannot say it for one keyed by bare code:
    // that call writes the key into the words of one kind of button only, and a filter row is
    // furnished with a different kind. So the words are written here instead - and not through the
    // button, whose published text accessors answer for that same one kind and quietly do nothing
    // for this one, but through the label underneath it.
    private void announceShortcutInLabel(int keycode) {

        var keyName = Keyboard.getKeyName(keycode);

        // A code outside the key table. Nothing to announce, and a bracket around nothing would read
        // as a control whose key had gone missing rather than one whose key cannot be written down.
        if (!KmlibStrings.hasText(keyName)) {
            return;
        }

        var label = VanillaButtonLabel.resolveLabelOf(button);

        // Already said where it happened. The key is bound either way, so what is lost is the
        // telling rather than the control.
        if (label == null) {
            return;
        }

        try {
            label.announceShortcut(keyName);

        } catch (RuntimeException failure) {

            // Saying it reads the palette a key is drawn in, which is one more thing that can be
            // unavailable. The binding is already made, so this costs the words and not the key.
            SHORTCUT_WARNING.warnOnce(
                "The key bound to the control on the map's filter row could not be written into its "
                    + "words; it is left answering a key it does not announce.",
                failure);
        }
    }

    // What is left of the row to the right of everything already standing on it. Measured from the
    // row's own left edge rather than from the origin, both boxes being in one UI space but the row
    // not starting at zero in it.
    private static float computeFreeWidthOf(Rectangle rowBox, Rectangle lastButtonBox) {

        var usedWidth = lastButtonBox.x() + lastButtonBox.width() - rowBox.x();

        return rowBox.width() - usedWidth;
    }

    // How big a control on this row has to be, or nothing where the row will not take one. The size
    // and the room are answered together because they are one decision - whether this row is fit for
    // a control at all - and because the three ways of failing it are one piece of news to whoever
    // asked, which is why they share a warning as well as an answer.
    private static ButtonSize measureButtonFor(MapFilterRow row) {

        var rowBox = row.readBox();
        var lastButtonBox = row.readLastButtonBox();

        // A row the layout never placed, or one holding nothing to take a width from. Both leave
        // nothing to match, and a button laid at a guess would be the one thing on the row that did
        // not look like its neighbours.
        if (rowBox == null || lastButtonBox == null) {
            WARNING.warnOnce(
                "The map's filter row could not be measured, so no control is appended to it.");
            return null;
        }

        var buttonSize = new ButtonSize(lastButtonBox.width(), rowBox.height());

        if (buttonSize.width() <= 0f || buttonSize.height() <= 0f) {
            WARNING.warnOnce(
                "The map's filter row measures to nothing, so no control is appended to it.");
            return null;
        }

        if (computeFreeWidthOf(rowBox, lastButtonBox) < BUTTON_GAP + buttonSize.width()) {
            WARNING.warnOnce(
                "The map's filter row has no room left for another control; it is left as it was "
                    + "found.");
            return null;
        }

        return buttonSize;
    }

    // The button as something whose ticked state can be read and written. The game's own filter
    // buttons publish that through the modding interface, so it is asked of them through it rather
    // than through a second reach by name.
    //
    // Asked after the append rather than before because the button does not exist before: what a row
    // building some other kind of button costs is one control standing inert on it, which is the
    // whole of the price for a question that cannot be put any earlier.
    private static MapFilterToggle adoptAppendedButton(MapFilterRow row, Object appendedButton) {

        if (appendedButton instanceof ButtonAPI button) {
            return new MapFilterToggle(row, button);
        }

        // Null is the build's own refusal, already logged where it happened. Anything else is a
        // button of a kind this cannot drive, which is news of its own.
        if (appendedButton != null) {
            WARNING.warnOnce(
                "The map's filter row builds buttons whose state cannot be read, so the control "
                    + "appended to it cannot be driven.");
        }

        return null;
    }
}
