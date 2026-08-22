package kmlib.starsector.ui.screen;

import com.fs.starfarer.api.Global;

import kmlib.math.geometry.Rectangle;

/**
 * How big the game's window is, in both of the spaces a UI pass works in - the whole screen as a
 * box, and each axis on its own for arithmetic that wants a single number.
 *
 * <p>Two spaces, because a UI pass straddles them and they are not interchangeable. UI units are
 * what the layout runs in: a widget reports its position in them, the campaign's ortho projection
 * spans them, and anything comparing a widget against the screen compares here. Screen pixels are
 * what the framebuffer is addressed in: the mouse reports them and a scissor clip is stated in
 * them. The two coincide only at a pixel scale of 1, so a number taken from the wrong one is right
 * on the developer's monitor and wrong on a scaled display. How they relate is set out in
 * {@code docs/dev/rendering-environment.md}.
 *
 * <p>A pixel length is reachable only as part of a {@link ScreenAxis}, never on its own, because on
 * its own it is not an answer to anything: every caller wanting one is converting between the
 * spaces and needs the UI partner in the same breath. Handing the pair over bound is what leaves no
 * arrangement of the two floats for a caller to get wrong. A UI length is offered loose beside
 * that, since a widget's own box is measured against it with no conversion in sight.
 *
 * <p>The box is cornered at the origin with its extent in UI units, the same frame of reference a
 * laid-out widget reports its position in, so a widget's box and this compare with no conversion
 * between them.
 *
 * <p>Read afresh at every ask rather than resolved once. The player can resize the window or change
 * the UI scale mid-session, and a value kept from before either would describe a screen that is no
 * longer there.
 *
 * <p>A vanilla-tier home rather than the neutral layout package: every number here comes from the
 * running game, so this cannot sit where the layout maths it feeds runs without one.
 */
public final class VanillaScreen {

    // The screen box hangs off the origin: UI coordinates start at the bottom-left of the window,
    // so the screen is the one box in this space whose corner is known without measuring it.
    private static final float SCREEN_ORIGIN = 0f;

    private VanillaScreen() {
    }

    /**
     * @return the whole screen in UI units, cornered at the origin
     */
    public static Rectangle resolveScreenBox() {

        return new Rectangle(
            SCREEN_ORIGIN,
            SCREEN_ORIGIN,
            resolveUiWidth(),
            resolveUiHeight());
    }

    /**
     * @return the screen's width in UI units - what a widget's own x is measured against
     */
    public static float resolveUiWidth() {
        return Global.getSettings().getScreenWidth();
    }

    /**
     * @return the screen's height in UI units - what a widget's own y is measured against
     */
    public static float resolveUiHeight() {
        return Global.getSettings().getScreenHeight();
    }

    /**
     * @return the horizontal axis in both spaces at once, for converting an x between them
     */
    public static ScreenAxis resolveXAxis() {

        var settings = Global.getSettings();
        return new ScreenAxis(settings.getScreenWidth(), settings.getScreenWidthPixels());
    }

    /**
     * @return the vertical axis in both spaces at once, for converting a y between them
     */
    public static ScreenAxis resolveYAxis() {

        var settings = Global.getSettings();
        return new ScreenAxis(settings.getScreenHeight(), settings.getScreenHeightPixels());
    }
}
