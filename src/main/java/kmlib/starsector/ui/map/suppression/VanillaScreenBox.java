package kmlib.starsector.ui.map.suppression;

import com.fs.starfarer.api.Global;

import kmlib.math.geometry.Rectangle;

/**
 * The screen itself as a box, in the UI units a widget's own position is laid out in - what a rule
 * about whether a widget stands anywhere the player could see it compares against.
 *
 * <p>Origin at the lower left and extent from the game's settings, which is the same frame of
 * reference a laid-out widget reports its position in, so a box and this compare with no
 * conversion between them.
 *
 * <p>Read afresh at every ask rather than resolved once. The player can resize the window or change
 * the UI scale mid-session, and a box kept from before either would describe a screen that is no
 * longer there - which, for a rule that switches a widget off, would keep switching it off after the
 * screen grew to include it.
 *
 * <p>Here rather than in the neutral layout package because it reaches the running game for its
 * numbers. It is the live half of this package's screen port and nothing else asks it yet; a second
 * package needing the same read is the point at which it moves somewhere both can see.
 */
public final class VanillaScreenBox {

    private VanillaScreenBox() {
    }

    /**
     * @return the whole screen in UI units, cornered at the origin
     */
    public static Rectangle resolveScreenBox() {

        var settings = Global.getSettings();
        return new Rectangle(0f, 0f, settings.getScreenWidth(), settings.getScreenHeight());
    }
}
