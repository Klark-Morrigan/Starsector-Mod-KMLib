package kmlib.starsector.ui.render.gl.tabs;

import kmlib.starsector.ui.render.gl.UiBoxes;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiFill;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.tabs.RaisedButtonTabStrip;
import kmlib.starsector.ui.widgets.tabs.TabLookSource;
import kmlib.starsector.ui.widgets.tabs.TabWashSource;
import kmlib.starsector.ui.widgets.tabs.VanillaTab;
import kmlib.starsector.ui.widgets.tabs.VanillaTabStrip;
import kmlib.starsector.ui.widgets.tabs.style.TabStyle;

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
 * <p>The frame takes the look's own label colour rather than the palette's chrome accent, which is what
 * makes a lit button read as lit all over: the look's three shades are one colour at the engine's three
 * glow amounts, so a frame drawn from it brightens with the fill and the text under the pointer instead of
 * standing at one tone while they move. It also keeps this pass selection-blind - "brighter frame when
 * shown" falls out of the look it was handed rather than out of a state it would have to be told.
 *
 * <p>Opacity scales the fill, the frame, and the text alike, so a button fades as one piece. GL
 * passthrough (over {@link UiFill} and {@link UiBoxes}), exercised in-engine like the other draw helpers.
 */
public final class RaisedButtonTabStripRenderer {

    // The button outline's weight, matching the hairline the engine's own buttons are framed in.
    private static final float FRAME_THICKNESS = 1f;

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
     * @param style   the row's look; its hotkey presentation and its face are read here, its band height
     *                having been spent laying the tabs out
     * @param opacity overall alpha, 0..1, applied to every quad and every text colour
     */
    public static void render(
            List<VanillaTab> tabs,
            TabLookSource looks,
            TabWashSource washes,
            TabStyle style,
            float opacity) {

        var textFace = style.face();
        for (var index = 0; index < tabs.size(); index++) {

            var tab = tabs.get(index);

            // The look as painted: the settled shade the tab has faded to, brightened by whatever pulse is
            // still running on it. A tab with none carries a wash that moves it nowhere, so no branch here
            // decides whether a lift applies.
            var look = looks.resolveLookAt(index).computeWashedLook(washes.resolveWashAt(index));

            // The button inside the laid tab, its gap to either neighbour taken out of the tab rather than
            // added to the row - the hit box the panel tests stays the whole tab, so the channel between
            // two buttons still answers to whichever one it was laid inside.
            var buttonBox = RaisedButtonTabStrip.computeButtonBox(tab.bounds());

            UiFill.renderQuad(buttonBox, new UiElementPaint(look.fill(), opacity));
            UiBoxes.renderBorder(
                buttonBox,
                new BoxBorder(FRAME_THICKNESS),
                new UiElementPaint(look.label(), opacity));

            TabLabelRenderer.renderCentredLabel(
                buttonBox,
                tab.content(),
                look,
                style.hotkey(),
                textFace,
                opacity);
        }
    }
}
