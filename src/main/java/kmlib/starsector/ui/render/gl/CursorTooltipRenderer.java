package kmlib.starsector.ui.render.gl;

import com.fs.starfarer.api.Global;

import kmlib.starsector.graphics.StarsectorSprites;
import kmlib.starsector.ui.font.LazyFontCache;
import kmlib.starsector.ui.font.LazyFontSpanMeasurer;
import kmlib.starsector.ui.input.UiCursor;
import kmlib.starsector.ui.text.TextSpan;
import kmlib.starsector.ui.text.TextStyle;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.CursorTooltip;
import kmlib.starsector.ui.widgets.TooltipLayout;
import kmlib.starsector.ui.widgets.TooltipRow;

import org.lazywizard.lazylib.ui.LazyFont;

import java.util.List;

/**
 * Draws a {@link CursorTooltip}'s rows as a free-floating box at the cursor: it lays the rows out through
 * the widget, resolves each row's look from the kind of line it is, and paints the box, each crest, and
 * each row's label and value in one bracketed GL pass. The sanctioned way for a KM UI to show a tooltip
 * on a core screen that offers no panel to hang a vanilla {@code TooltipMakerAPI} on - the same rationale
 * that puts the panel renderers here beside it.
 *
 * <p>The pass reads the live cursor and screen size itself, so a caller supplies only the content rows
 * and the look: the placement that follows the pointer and clamps to the screen is the widget's job,
 * not the caller's. When the body face cannot load the whole box is skipped - a frame of chrome with
 * no text would only mislead - while any other face failing costs only the lines drawn in it, since the
 * body still reads. Must run with a current GL context, from an above-UI render pass, since the box
 * composites over the core UI and its tooltips.
 */
public final class CursorTooltipRenderer {
    private CursorTooltipRenderer() {
    }

    /**
     * Lays out {@code rows} at the live cursor and paints them in {@code style}. Skipped silently when
     * the body face cannot load; an empty row list draws only the padded box.
     *
     * @param rows  the content rows, top to bottom
     * @param style the typography, opacity, and box chrome the whole tooltip draws in
     */
    public static void render(List<TooltipRow> rows, CursorTooltipStyle style) {
        // The body face carries all but a handful of a tooltip's lines, so a box that cannot load it has
        // effectively nothing to say and is dropped whole rather than framed empty. Every other face is
        // left to degrade per line: the measurement charges it no width and the draw skips it.
        if (LazyFontCache.loadByFace(style.typography().paragraphStyle().face().font()) == null) {
            return;
        }
        var settings = Global.getSettings();
        var layout = CursorTooltip.layOut(
                rows,
                style.typography(),
                LazyFontSpanMeasurer::measureSpanWidth,
                UiCursor.getUiX(),
                UiCursor.getUiY(),
                settings.getScreenWidth(),
                settings.getScreenHeight());

        // The core map and its tooltips draw after this pass, so the box, crests, and text run inside
        // the shared state save that restores the blend and colour the draw touched on the way out.
        GlStateGuard.bracket(() -> drawRows(rows, layout, style));
    }

    // Paints the frame then each row into the laid-out box: the box first, then per row its crest,
    // label, and value at the anchors the widget resolved, so the draw never re-derives the geometry.
    private static void drawRows(
            List<TooltipRow> rows,
            TooltipLayout layout,
            CursorTooltipStyle style) {

        BorderedBoxRenderer.render(
                layout.box(),
                new BoxBorder(style.borderWidth()),
                new UiElementPaint(style.fillColor(), style.opacity()),
                new UiElementPaint(style.borderColor(), style.opacity()));
                
        for (var index = 0; index < rows.size(); index++) {
            drawRow(
                    rows.get(index),
                    layout.rows().get(index),
                    style);
        }
    }

    // Draws one row's crest, its label's runs, and its right-aligned value at their resolved anchors. A
    // row with no crest has a null crest box and skips the icon draw; a missing crest asset resolves to
    // null and is skipped the same way, so its label still reads. Every run and the value are drawn
    // unconditionally - a blank span paints nothing.
    private static void drawRow(
            TooltipRow row,
            TooltipLayout.TooltipRowLayout placement,
            CursorTooltipStyle style) {

        if (placement.crestBox() != null) {
            var crest = StarsectorSprites.loadSprite(row.crestSpritePath());
            if (crest != null) {
                UiSprite.renderQuad(
                        crest,
                        placement.crestBox(),
                        style.opacity());
            }
        }

        // The kind of line the row is decides the face, size, and casing every span of it shares -
        // resolved once here, the same lookup the layout made when it measured them.
        var rowPaint = new RowPaint(
                style.typography().resolveStyleFor(row.lineStyle()),
                style.opacity());

        // Anchored as the layout pinned them, not by each style's own alignment: the columns are the
        // layout's decision, so a style's default anchor has no say in a box that resolved its own. The
        // runs walk in step with the anchors the same rows produced, so run and anchor cannot slip.
        var labelTextSpans = row.labelTextSpans();
        for (var index = 0; index < labelTextSpans.size(); index++) {
            rowPaint.drawSpan(
                    labelTextSpans.get(index),
                    placement.labelRunXs().get(index),
                    placement.labelY(),
                    LazyFont.TextAnchor.TOP_LEFT);
        }

        rowPaint.drawSpan(
                row.valueTextSpan(),
                placement.valueX(),
                placement.valueY(),
                LazyFont.TextAnchor.TOP_RIGHT);
    }

    /**
     * One row's resolved look bound to the box's opacity - the two things every span of that row draws
     * with, so a span states only its own colour and placement. Bound once because the spans of a row
     * must not each restate the look they share: a second label run drawn on a face the first was not
     * would neither line up nor read as part of the same line.
     *
     * <p>Each span is drawn on its own, through the whole-line paint that caches one glyph run per
     * (face, text) pair and applies the box's fade as it draws. Batching a row's runs into one
     * {@code DrawableString} through {@code append(text, colour)} would bake each run's colour absolute
     * at append time, after which the base colour the fade is applied through recolours only the
     * segment before the first tinted run - so the box's opacity would stop applying to exactly the
     * runs a caller picked out.
     *
     * @param textStyle the face, size, and casing the row's spans draw in
     * @param opacity   the alpha the whole box and its text fade by
     */
    private record RowPaint(TextStyle textStyle, float opacity) {

        // Draws one span at an anchor in its own colour. The span's own colour wins over the style's
        // default, since a label run or a value picked out in the row model must not be flattened to
        // one colour by the look.
        private void drawSpan(
                TextSpan textSpan,
                float x,
                float y,
                LazyFont.TextAnchor anchor) {

            LabelRenderer.render(
                    new LabelStyle(textStyle.face(), textSpan.colour(), opacity),
                    // Painted from the look's own display form, the same one the layout measured, or a
                    // shouted line would be drawn wider than the box sized to hold it.
                    textStyle.resolveDisplayText(textSpan.text()),
                    x,
                    y,
                    anchor);
        }
    }
}
