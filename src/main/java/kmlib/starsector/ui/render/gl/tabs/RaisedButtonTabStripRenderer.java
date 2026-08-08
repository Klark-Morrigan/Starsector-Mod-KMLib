package kmlib.starsector.ui.render.gl.tabs;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.render.gl.UiBoxes;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiFill;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.tabs.RaisedButtonTabStrip;
import kmlib.starsector.ui.widgets.tabs.TabLookSource;
import kmlib.starsector.ui.widgets.tabs.TabWashSource;
import kmlib.starsector.ui.widgets.tabs.VanillaTab;
import kmlib.starsector.ui.widgets.tabs.VanillaTabStrip;
import kmlib.starsector.ui.widgets.tabs.style.TabPalette;
import kmlib.starsector.ui.widgets.tabs.style.TabStyle;

import java.awt.Color;
import java.util.List;

/**
 * Raw-GL paint for a tab row wearing the intel screen's look: each tab a filled button standing clear of
 * its neighbours inside a frame, rather than the seamless run of abutting tabs
 * {@link VanillaTabStripRenderer} draws. It is the chrome half of a pair - the row is laid out once by
 * {@link VanillaTabStrip} and stood into buttons by {@link RaisedButtonTabStrip}, so a button is drawn
 * exactly where the panel hit-tests a tab.
 *
 * <p>It takes the same resolved look and lift the strip does, and reads them the same way: a tab arrives
 * already faded onto its hovered shade and already lifted by whatever pulse is running on it, so both
 * chromes animate identically and neither computes any timing. Selection is not asked for here either -
 * the lit tab arrives on its own shade, exactly as it does on the strip.
 *
 * <p>A button is a constant frame around a changing interior, which is the whole of how it differs from a
 * tab. The two hairlines and the backing beneath them are drawn the same whatever the button is doing, and
 * only the interior quad moves with the look - matching the engine's own buttons, whose outline never
 * shifts and whose fill carries the entire signal. A frame that brightened with the fill would be the one
 * thing on the row moving that the row it was drawn to match holds still.
 *
 * <p>The backing is why a button reads as a button over a lit map: the interior it stands under is the
 * accent's dark step, translucent as the engine keeps it, so without something solid beneath it a button
 * over a bright nebula would read as a tinted patch of map. A button not being shown paints no interior at
 * all, that state arriving as a fill at zero alpha rather than as a case this pass tests for.
 *
 * <p>Opacity scales the backing, the frames, the fill, and the text alike, so a button fades as one piece.
 * GL passthrough (over {@link UiFill} and {@link UiBoxes}), exercised in-engine like the other draw
 * helpers.
 */
public final class RaisedButtonTabStripRenderer {

    // The button outline's weight, matching the hairline the engine's own buttons are framed in. Both
    // frames are drawn at it, so the pair reads as one two-tone edge rather than as a line and a shadow.
    private static final float FRAME_THICKNESS = 1f;

    // How far in the interior sits: clear of both hairlines, so neither is painted over by a lit fill.
    private static final float INTERIOR_INSET = 2f * FRAME_THICKNESS;

    // How solid the backing under a button is. Short of opaque, so the map still shows faintly through a
    // row that would otherwise read as a solid bar laid across the screen, and far enough from clear that
    // a button over the brightest content it can stand on still holds its own shape.
    private static final float BACKING_ALPHA = 0.75f;

    private RaisedButtonTabStripRenderer() {
    }

    /**
     * Paints the whole row: each tab as a filled, framed button standing inside its laid box, with the
     * tab's text and its bound key centred in it under the style's hotkey convention - so a host asking
     * for a plain key gets buttons with no underline, matching the intel screen's own.
     *
     * @param tabs    the laid-out tabs, in row order
     * @param looks   where each tab's settled look comes from - selection and the hover fade are already
     *                blended into it here, so this pass only paints it
     * @param washes  where each tab's resolved lift comes from - the interaction is already composed into
     *                a wash here, so this pass only paints it
     * @param style   the row's look; its palette's chrome accent (see
     *                {@link TabPalette#createRaisedButtonPalette}), its hotkey presentation, and its face
     *                are read here, its band height having been spent laying the tabs out
     * @param opacity overall alpha, 0..1, applied to every quad and every text colour
     */
    public static void render(
            List<VanillaTab> tabs,
            TabLookSource looks,
            TabWashSource washes,
            TabStyle style,
            float opacity) {

        var chromeAccent = style.palette().chromeAccent();

        TabChromeRenderer.paintEachTab(tabs, looks, washes, (tab, look) -> {

            // The button inside the laid tab, its gap to either neighbour taken out of the tab rather than
            // added to the row - the hit box the panel tests stays the whole tab, so the channel between
            // two buttons still answers to whichever one it was laid inside.
            var buttonBox = RaisedButtonTabStrip.computeButtonBox(tab.bounds());

            renderButtonChrome(buttonBox, chromeAccent, opacity);
            UiFill.renderQuad(
                buttonBox.computeInsetBox(INTERIOR_INSET),
                new UiElementPaint(look.fill(), opacity));

            TabLabelRenderer.renderCentredLabel(buttonBox, tab.content(), look, style, opacity);
        });
    }

    // What every button wears whatever its state: a backing solid enough to stand on live content, an
    // outer hairline in the accent's dark step, and an inner one in black. The pair of hairlines is what
    // gives the engine's own buttons their weight against a lit map - a single line reads as a hairline
    // sitting on whatever is behind it, two read as an edge - and neither answers to the look, so a button
    // being shown differs from a resting one in its interior alone.
    private static void renderButtonChrome(Rectangle buttonBox, Color chromeAccent, float opacity) {

        UiFill.renderQuad(buttonBox, new UiElementPaint(Color.BLACK, opacity * BACKING_ALPHA));
        UiBoxes.renderBorder(
            buttonBox,
            new BoxBorder(FRAME_THICKNESS),
            new UiElementPaint(chromeAccent, opacity));
        UiBoxes.renderBorder(
            buttonBox.computeInsetBox(FRAME_THICKNESS),
            new BoxBorder(FRAME_THICKNESS),
            new UiElementPaint(Color.BLACK, opacity));
    }
}
