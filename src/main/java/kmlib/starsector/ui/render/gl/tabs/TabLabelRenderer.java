package kmlib.starsector.ui.render.gl.tabs;

import kmlib.colour.Colours;
import kmlib.math.geometry.PixelGrid;
import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.font.DrawableStringCache;
import kmlib.starsector.ui.font.TextFace;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiFill;
import kmlib.starsector.ui.widgets.tabs.TabShortcutText;
import kmlib.starsector.ui.widgets.tabs.TabTextRun;
import kmlib.starsector.ui.widgets.tabs.VanillaTabContent;
import kmlib.starsector.ui.widgets.tabs.style.HotkeyStyle;
import kmlib.starsector.ui.widgets.tabs.style.TabLook;
import kmlib.starsector.ui.widgets.tabs.style.TabStyle;

import org.lazywizard.lazylib.ui.LazyFont;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Raw-GL paint for what a tab says: its label with the bound key lit inside it, carrying the emphasis its
 * {@link HotkeyStyle} asks for, centred as one group in the box it is handed. A tab's text reads the same
 * whatever surface it stands on - the sector map's seamless strip and the intel screen's raised buttons
 * differ in their chrome and not in how either spells a tab out - so the text is drawn once here and each
 * chrome renderer supplies only the box and the settled look.
 *
 * <p>Where the key falls is not decided here. A tab lights a letter of its own label where the key has one
 * to land on and spells the key out after it otherwise, and that is {@link TabShortcutText}'s call - the
 * same one the layout measured the tab against. This pass walks the runs it produced, colours each by its
 * role, and advances by what it drew, so it cannot draw a tab wider or narrower than the box it was given.
 *
 * <p>Every colour fades by one opacity, so the text tracks whatever chrome is drawn under it. The runs are
 * separate drawables re-coloured per frame rather than one baked multi-colour run precisely so they fade
 * with the rest instead of staying opaque. That is also what lets a haloed style draw the group several
 * times from the one set of drawables, re-colouring them between the ring and the text: a colour set on a
 * single-colour drawable is a live knob, where a baked multi-colour run would hold its tints against
 * every pass.
 *
 * <p>GL passthrough (over {@link UiFill} for the underline and the font cache for the glyphs), exercised
 * in-engine like the other draw helpers.
 */
public final class TabLabelRenderer {

    // The compositing alpha for a quad whose colour already carries the pass's fade in its own channel.
    // Named rather than a bare 1, since the point is that the fade is not being skipped but has already
    // been spent.
    private static final float UNFADED = 1f;

    private TabLabelRenderer() {
    }

    /**
     * Draws the tab's text centred as one group inside {@code bounds}: the label in the look's own label
     * colour, the bound key in the hotkey colour, and a hairline under that key when the style asks for
     * one. A haloed style rings the whole group first, laying it down once per side in the halo's colour,
     * so the emphasis is backed along with the text it marks. Skipped silently when any run's font cannot
     * load, so a tab falls back to nothing rather than to a half-drawn line.
     *
     * <p>Takes the whole row style rather than the three fields it reads from it, so a caller cannot hand
     * this the hotkey convention of one style and the face of another.
     *
     * @param bounds  the box to centre the text in, in UI coordinates
     * @param content the tab's label and its optional bound key
     * @param look    the tab's settled look; only its label colour is read, the fill being the calling
     *                chrome's to paint
     * @param style   the row's look; its hotkey presentation, its face, and its text halo are what this
     *                pass reads
     * @param opacity overall alpha, 0..1, applied to every glyph colour and to the underline
     */
    public static void renderCentredLabel(
            Rectangle bounds,
            VanillaTabContent content,
            TabLook look,
            TabStyle style,
            float opacity) {

        var runs = resolveDrawnRuns(TabShortcutText.resolveRuns(content), style.face());
        if (runs == null) {
            return;
        }
        var hotkeyStyle = style.hotkey();
        var textHalo = style.textHalo();
        var haloOpacity = opacity * textHalo.strength();

        // The ring goes down first, so the text proper covers whatever of it lands under the glyphs. Both
        // roles take the one shade - a ring is the group's silhouette, so a key lit inside it would read
        // as a misplaced second copy of the label rather than as an edge on the first - and the colour is
        // set once for the whole ring rather than per copy, which is what says the four are one thing seen
        // four times.
        setRunColours(runs, textHalo.colour(), textHalo.colour(), haloOpacity);
        for (var haloBox : textHalo.computeHaloBoxes(bounds)) {
            drawRunGroup(haloBox, runs, hotkeyStyle);
        }
        setRunColours(runs, look.label(), hotkeyStyle.keyColour(), opacity);
        drawRunGroup(bounds, runs, hotkeyStyle);
    }

    // Resolves each run to a drawable anchored for the left-to-right walk below. Colourless: a drawable is
    // re-coloured per pass rather than per resolve, so a haloed style spends one resolve on all of its
    // passes. Null when any run's font cannot load: the runs are one line of text broken up, so drawing
    // the pieces that did resolve would leave a tab reading as a fragment of its own name.
    private static List<DrawnRun> resolveDrawnRuns(List<TabTextRun> runs, TextFace textFace) {

        var drawn = new ArrayList<DrawnRun>(runs.size());
        for (var run : runs) {

            var drawable = DrawableStringCache.resolveRun(textFace, run.text());
            if (drawable == null) {
                return null;
            }
            drawable.setAnchor(LazyFont.TextAnchor.CENTER_LEFT);
            drawn.add(new DrawnRun(drawable, run.role() == TabTextRun.Role.KEY));
        }
        return drawn;
    }

    // Sets each run to its role's colour, faded to the pass it is about to be drawn at. Apart from the
    // draw below because a colour survives any number of draws, and the ring lays the same group down
    // four times in one shade: setting it per copy would state four times over a thing that is true once.
    private static void setRunColours(
            List<DrawnRun> runs,
            Color labelColour,
            Color keyColour,
            float opacity) {

        for (var run : runs) {
            run.drawable().setBaseColor(
                Colours.scaleAlpha(run.isKey() ? keyColour : labelColour, opacity));
        }
    }

    // One laying-down of the whole group inside the given box, in whatever colours the runs currently
    // carry. The group centres in whichever box it is handed, so a ring copy moves every piece of it - the
    // runs and the emphasis under one of them - by taking a shifted box rather than by shifting each draw
    // site. It takes no colour of its own: the runs were set for this pass, and the underline reads its
    // shade off the key it marks, so the line cannot be drawn in a colour the glyph above it is not.
    private static void drawRunGroup(Rectangle bounds, List<DrawnRun> runs, HotkeyStyle hotkeyStyle) {

        var centerY = bounds.computeCenterY();
        var runX = bounds.x() + (bounds.width() - computeRunsWidth(runs)) / 2f;

        for (var run : runs) {

            // Snapped to whole pixels before it is drawn: centring a run of one width inside a box of
            // another puts it half a pixel out as often as not, and a glyph resampled across two columns
            // comes out both softer and dimmer than the colour it was set in. Snapped per run rather than
            // once for the group, so the rounding cannot pile up along a line.
            var drawnX = PixelGrid.computeSnappedEdge(runX);
            var drawnCentreY = PixelGrid.computeSnappedCentre(centerY, run.drawable().getHeight());

            run.drawable().draw(drawnX, drawnCentreY);

            if (run.isKey() && hotkeyStyle.isKeyUnderlined()) {
                drawKeyUnderline(run.drawable(), drawnX, drawnCentreY, hotkeyStyle);
            }
            runX += run.drawable().getWidth();
        }
    }

    // The whole line's rendered width, so the group centres in the box as the one string the layout
    // measured rather than as pieces each centred on their own.
    private static float computeRunsWidth(List<DrawnRun> runs) {

        var width = 0f;
        for (var run : runs) {
            width += run.drawable().getWidth();
        }
        return width;
    }

    // The styled emphasis under the key alone. The run is anchored centre-left, so it stands its own
    // height about the draw y; the style places the line against that box, so every renderer drawing this
    // look puts it in the same spot.
    //
    // The line takes the key's own drawn colour rather than the style's: it marks that glyph, so it
    // belongs to whichever copy of the group is being laid down - a ring copy drawing it in the styled
    // gold would leave gold hairlines standing out around the one under the key. That colour has the
    // pass's opacity already folded into its alpha, which is why the quad is painted at full compositing
    // alpha rather than faded a second time.
    private static void drawKeyUnderline(
            DrawableString key,
            float keyX,
            float centerY,
            HotkeyStyle hotkeyStyle) {

        var keyBox = new Rectangle(
            keyX,
            centerY - key.getHeight() / 2f,
            key.getWidth(),
            key.getHeight());

        UiFill.renderQuad(
            hotkeyStyle.computeUnderlineBox(keyBox),
            new UiElementPaint(key.getBaseColor(), UNFADED));
    }

    // One resolved run: the drawable to paint and whether it is the bound key, which is all the draw
    // loop above still has to know once the colour has been set.
    private record DrawnRun(
        DrawableString drawable,
        boolean isKey) {
    }
}
