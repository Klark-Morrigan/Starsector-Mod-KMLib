package kmlib.starsector.ui.suppression;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.layout.VanillaPositions;
import kmlib.starsector.ui.screen.VanillaScreen;

import org.apache.log4j.Logger;

import java.util.function.Supplier;

/**
 * Stops a widget parked off screen from rendering, by writing its own opacity to zero, and puts that
 * opacity back when the widget returns.
 *
 * <p>A widget nobody can see goes on rendering its whole subtree every frame, and everything that
 * subtree does to the rest of the game happens with it. The work alone is reason enough, and where
 * the subtree is something the game answers questions from, a hidden one answers them too: a sector
 * map composited into a panel, say, iterates the sector's terrain and binds a transform of its own,
 * so a frame that also draws a map the player opened carries two transforms and a cursor read has
 * to pick between them. A widget rendering nothing contributes neither.
 *
 * <p>Opacity is the lever because it is a real one rather than a fade. The engine's component render
 * multiplies the incoming alpha by the component's own opacity and returns before drawing its
 * subtree once the product reaches zero, so a zeroed widget renders no subtree at all.
 *
 * <p><b>Parked means the widget's box does not meet the screen at all.</b> Nothing about how it got
 * there is read - not resting positions, not step sizes, not whatever setting sized it - so a widget
 * parked by any arithmetic reads the same way. Partial overlap counts as present, which is what
 * leaves a panel sliding on or off screen alone: those frames were already frames where the player
 * could see part of it.
 *
 * <p><b>The reading is where the widget is, never what shows of it</b>, and the two are different
 * questions here for a reason of this script's own making: the moment it zeroes a widget, any read
 * that sifts out widgets drawn to nothing would stop reporting that one - and what would say when to
 * restore it is exactly what would be gone. So the position is read straight, whatever the widget is
 * drawn at.
 *
 * <p>Which widget may be suppressed at all is a port, asked afresh every frame and free to answer
 * nothing. A widget belongs to whoever built it, so suppressing one is a decision about somebody
 * else's screen that only the caller can take - and answering nothing is how that caller withdraws
 * the decision, which restores the widget on the next frame rather than at the next rebuild.
 *
 * <p>The opacity put back is the one the widget carried when it went off screen rather than a flat
 * value, so nothing here invents a number for a field somebody else owns. It is kept beside the
 * widget it was read from and goes back to that widget alone: a caller answering with a different
 * one - a panel its owner rebuilt - ends the suppression this holds rather than carrying a value
 * across two widgets.
 *
 * <p>Runs while paused, because the states this exists for are mostly paused ones: a dialog, a menu,
 * or an open screen with a parked panel still rendering behind it. It also cannot run in a render
 * pass at all, which is what makes it a script rather than part of one: the pass it suppresses is
 * the last pass it would ever be asked from, so the widget would be stranded invisible for the rest
 * of the session.
 *
 * <p>Never throws. It writes into a widget the game is about to render, and a fault in that write
 * must not take the campaign's frame with it; a failure is recorded once and the widget left as it
 * stands.
 */
public final class OffScreenWidgetSuppressor implements EveryFrameScript {

    private static final Logger LOG = Global.getLogger(OffScreenWidgetSuppressor.class);

    // Drawn to nothing, which is what makes the engine return before rendering the subtree.
    private static final float SUPPRESSED_OPACITY = 0f;

    private final Supplier<UIComponentAPI> findSuppressibleWidget;
    private final Supplier<Rectangle> readScreenBox;

    // One-shot guard: this runs every frame, so a recurring fault would flood the log.
    private boolean hasLoggedSuppressionError;

    // What the suppressed widget was drawn at before it was suppressed, held beside the widget it
    // was read from. Meaningless without that widget, which is why nothing reads it while
    // suppressedWidget is null.
    private float opacityBeforeSuppression;

    // The widget this has written to, or null while it has written to none. Identity is the whole of
    // what it is compared by: two widgets are the same one only by being the same object, and a
    // core-UI widget's equals is the obfuscated class's business.
    private UIComponentAPI suppressedWidget;

    /**
     * @param findSuppressibleWidget the widget this may switch off, or null for none - asked afresh
     *                               every frame, since which widget is suppressible is the caller's
     *                               decision and one it may withdraw
     * @param readScreenBox          the screen the widget's box is compared against, in the UI units
     *                               that box is laid out in; {@link VanillaScreen#resolveScreenBox}
     *                               answers it for a running game
     */
    public OffScreenWidgetSuppressor(
            Supplier<UIComponentAPI> findSuppressibleWidget,
            Supplier<Rectangle> readScreenBox) {

        this.findSuppressibleWidget = findSuppressibleWidget;
        this.readScreenBox = readScreenBox;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        // Guarded so an uncaught fault - a widget that refuses the write, a port that throws - is
        // recorded rather than thrown into the campaign's frame, and does not stop the script from
        // trying again on the next one.
        try {
            applySuppression();
        } catch (RuntimeException exception) {
            if (!hasLoggedSuppressionError) {
                hasLoggedSuppressionError = true;
                LOG.error("Could not suppress or restore an off-screen widget; it is left as it "
                    + "stands for the rest of this session.", exception);
            }
        }
    }

    private void applySuppression() {

        var widget = findSuppressibleWidget.get();

        // Any change of widget ends the suppression being held, the widget going away entirely
        // included: the opacity kept is that widget's own, so it goes back to that widget and to
        // nothing else. A widget its owner has since rebuilt is written to for nothing, which costs
        // one field on an object nobody draws.
        if (widget != suppressedWidget) {
            restoreSuppressedWidget();
        }
        if (widget == null) {
            return;
        }
        if (isWidgetOnScreen(widget)) {
            restoreSuppressedWidget();
            return;
        }
        suppressWidget(widget);
    }

    // Whether any part of the widget stands on the screen. A widget the layout never placed answers
    // yes, which is this script failing open: nothing can say a widget is parked without a box to
    // say it of, and leaving it drawn is the state the caller had before this ran.
    private boolean isWidgetOnScreen(UIComponentAPI widget) {

        var position = widget.getPosition();
        if (position == null) {
            return true;
        }
        return VanillaPositions.toRectangle(position).overlapsBox(readScreenBox.get());
    }

    // Writes the widget down to nothing, and remembers what it was drawn at the first time. The
    // write repeats for as long as the widget stays parked, so an opacity its owner sets in the
    // meantime is answered on the next frame rather than leaving a hidden widget rendering again;
    // only the reading is not repeated, a second one landing on this script's own zero.
    private void suppressWidget(UIComponentAPI widget) {

        if (suppressedWidget != widget) {
            opacityBeforeSuppression = widget.getOpacity();
            suppressedWidget = widget;

            LOG.debug("Off-screen widget suppression: " + describeWidget(widget)
                + " is parked off screen, so its opacity is held at zero and it renders nothing");
        }
        widget.setOpacity(SUPPRESSED_OPACITY);
    }

    // Hands the widget back the opacity it had, and forgets it. A no-op while nothing is suppressed,
    // which is most frames.
    private void restoreSuppressedWidget() {

        if (suppressedWidget == null) {
            return;
        }
        suppressedWidget.setOpacity(opacityBeforeSuppression);

        LOG.debug("Off-screen widget suppression: " + describeWidget(suppressedWidget)
            + " is back, so its opacity returns to " + opacityBeforeSuppression);

        suppressedWidget = null;
    }

    // Names the widget by its class, which is what a reader can match against a tree trace. There is
    // no id on a core-UI widget to name it by.
    private static String describeWidget(UIComponentAPI widget) {
        return widget.getClass().getSimpleName();
    }
}
