package kmlib.starsector.ui.render.gl.tooltip;

import com.fs.starfarer.api.Global;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.font.LazyFontCache;
import kmlib.starsector.ui.font.LazyFontSpanMeasurer;
import kmlib.starsector.ui.input.UiCursor;
import kmlib.starsector.ui.render.gl.GlStateGuard;
import kmlib.starsector.ui.render.gl.LabelRenderer;
import kmlib.starsector.ui.render.gl.LabelStyle;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiSprite;
import kmlib.starsector.ui.render.gl.panel.BorderedBoxRenderer;
import kmlib.starsector.ui.text.ImageSpan;
import kmlib.starsector.ui.text.LabelRun;
import kmlib.starsector.ui.text.TextSpan;
import kmlib.starsector.ui.text.TextStyle;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.LabelledRow;
import kmlib.starsector.ui.widgets.RowSlot;
import kmlib.starsector.ui.widgets.tooltip.CursorTooltip;
import kmlib.starsector.ui.widgets.tooltip.TooltipLayout;
import kmlib.starsector.ui.widgets.tooltip.TooltipRow;
import kmlib.starsector.ui.widgets.tooltip.TooltipSection;

import org.lazywizard.lazylib.ui.LazyFont;

import java.util.List;

/**
 * Draws a {@link CursorTooltip}'s blocks as a free-floating box at the cursor: it lays them out through
 * the widget, resolves each row's look from the kind of line it is, and paints the box, each crest, and
 * each row's label - run by run, words and inline images alike - and its value in one bracketed GL pass.
 * The sanctioned way for a KM UI to show a tooltip
 * on a core screen that offers no panel to hang a vanilla {@code TooltipMakerAPI} on - the same rationale
 * that puts the panel renderers here beside it.
 *
 * <p>The pass reads the live cursor and screen size itself, so a caller supplies only the content and
 * the look: the placement that follows the pointer and clamps to the screen is the widget's job,
 * not the caller's. When the body face cannot load the whole box is skipped - a frame of chrome with
 * no text would only mislead - while any other face failing costs only the lines drawn in it, since the
 * body still reads. Must run with a current GL context, from an above-UI render pass, since the box
 * composites over the core UI and its tooltips.
 */
public final class CursorTooltipRenderer {

    // The screen's own lower-left corner, which is where UI coordinates start: the box clamps inside the
    // whole screen rather than inside a region of it, so the bound handed over is the screen itself.
    private static final float SCREEN_ORIGIN = 0f;

    private CursorTooltipRenderer() {
    }

    /**
     * Lays out {@code sections} at the live cursor and paints them in {@code style}. Skipped silently
     * when the body face cannot load; an empty list draws only the padded box.
     *
     * @param sections the content blocks, top to bottom
     * @param style    the typography, opacity, and box chrome the whole tooltip draws in
     */
    public static void render(List<TooltipSection> sections, CursorTooltipStyle style) {
        // The body face carries all but a handful of a tooltip's lines, so a box that cannot load it has
        // effectively nothing to say and is dropped whole rather than framed empty. Every other face is
        // left to degrade per line: the measurement charges it no width and the draw skips it.
        if (LazyFontCache.loadByFace(style.typography().paragraphStyle().face().font()) == null) {
            return;
        }
        var settings = Global.getSettings();
        var layout = CursorTooltip.layOut(
            sections,
            style.typography(),
            LazyFontSpanMeasurer::measureSpanWidth,
            UiCursor.getUiX(),
            UiCursor.getUiY(),
            new Rectangle(
                SCREEN_ORIGIN,
                SCREEN_ORIGIN,
                settings.getScreenWidth(),
                settings.getScreenHeight()));

        // Painted off the flat run of lines the layout anchored, in the same order: how those lines were
        // grouped was spent settling the spacing, and the draw has nothing left to do with it.
        var rows = TooltipSection.readRowsInOrder(sections);

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
            new UiElementPaint(style.fillColour(), style.opacity()),
            new UiElementPaint(style.borderColour(), style.opacity()));

        for (var index = 0; index < rows.size(); index++) {
            drawRow(
                rows.get(index),
                layout.rows().get(index),
                style);
        }
    }

    // Draws one row's flanks and its label's runs at their resolved anchors. Only a table row has flanks
    // at all - a centred row is its label alone - so the label draw is what the two kinds share and the
    // one branch here is over what the row holds rather than over how it is laid.
    private static void drawRow(
            TooltipRow row,
            TooltipLayout.TooltipRowLayout placement,
            CursorTooltipStyle style) {

        // What kind of line the row is and how far it stands under the box's voice together decide the
        // face, size, and casing every span of it shares - resolved once here, from both facts, because
        // this is exactly the lookup the layout made when it measured the row. Resolved from the kind
        // alone, a demoted line would be painted at its kind's full size inside a line the box measured
        // at the shrunk one, and every label past the first run would overlap the words before it.
        var rowPaint = new RowPaint(
            style.typography().resolveStyleFor(row.lineStyle(), row.subordinationLevel()),
            style.opacity());

        if (row instanceof TooltipRow.TableRow tableRow) {
            drawFlankingRowSlots(tableRow.labelledRow(), placement, rowPaint);
        }

        // Anchored as the layout pinned them, not by each style's own alignment: the columns are the
        // layout's decision, so a style's default anchor has no say in a box that resolved its own. The
        // runs walk in step with the anchors the same rows produced, so run and anchor cannot slip. Every
        // run is drawn unconditionally - a blank span paints nothing.
        var labelRuns = row.labelRuns();
        for (var index = 0; index < labelRuns.size(); index++) {
            drawLabelRun(
                labelRuns.get(index),
                placement.labelRunXs().get(index),
                placement,
                rowPaint);
        }
    }

    // Draws one run of a label at the anchor the layout resolved for it: a stretch of text in its own
    // colour, or a small image hung square on the line. The branch is over what the run is rather than
    // over which kind of line it sits on, so a crest set among a centred line's words and one set among a
    // table row's are painted by the same step.
    private static void drawLabelRun(
            LabelRun labelRun,
            float runX,
            TooltipLayout.TooltipRowLayout placement,
            RowPaint rowPaint) {

        if (labelRun instanceof TextSpan textSpan) {
            rowPaint.drawSpan(
                textSpan,
                runX,
                placement.rowTopY(),
                LazyFont.TextAnchor.TOP_LEFT);
            return;
        }
        if (labelRun instanceof ImageSpan imageSpan) {
            UiSprite.renderImage(
                imageSpan.spritePath(),
                computeImageBox(runX, placement),
                rowPaint.opacity());
        }
    }

    // Draws what a table row carries either side of its label: an image in the leading column and a run
    // of text in the trailing one. A slot the row left unfilled has nothing to paint, and a slot holding
    // a kind this pass has not been taught draws nothing - the layout reserved the column from that
    // slot's own width either way, so the unpainted kind costs the box its room rather than overlapping
    // the label.
    private static void drawFlankingRowSlots(
            LabelledRow labelledRow,
            TooltipLayout.TooltipRowLayout placement,
            RowPaint rowPaint) {

        // What the leading column holds decides what is drawn in it: a slot holding something other than
        // an image draws nothing here rather than resolving to a texture lookup never meant for it.
        // Faded through the paint the row's text draws with, so a crest and the label beside it cannot
        // end up compositing at two different alphas.
        if (labelledRow.leadingRowSlot() instanceof RowSlot.Image crestRowSlot) {
            UiSprite.renderImage(
                crestRowSlot.spritePath(),
                computeImageBox(placement.leadingRowSlotX(), placement),
                rowPaint.opacity());
        }
        if (labelledRow.trailingRowSlot() instanceof RowSlot.Text valueRowSlot) {
            rowPaint.drawSpan(
                valueRowSlot.textSpan(),
                placement.trailingRowSlotX(),
                placement.rowTopY(),
                LazyFont.TextAnchor.TOP_RIGHT);
        }
        if (labelledRow.trailingRowSlot() instanceof RowSlot.TextRuns valueRowSlot) {
            drawTrailingTextRuns(valueRowSlot, placement, rowPaint);
        }
    }

    // Draws a value made of several runs inside the column reserved for it: the runs read left to right
    // from the column's own left edge, which is its right anchor less the width the runs come to.
    //
    // The offsets come from the slot itself, through the same walk the layout charged the column its
    // width by - so the room reserved and the runs painted into it are one measurement read twice rather
    // than two that could disagree. Each run is drawn on its own for the reason the label's runs are:
    // batched into one string, a run's colour would be baked absolute and the box's fade would stop
    // applying to it.
    private static void drawTrailingTextRuns(
            RowSlot.TextRuns valueRowSlot,
            TooltipLayout.TooltipRowLayout placement,
            RowPaint rowPaint) {

        var runOffsets = valueRowSlot.measureRunOffsets(
            placement.lineHeight(),
            rowPaint::measureSpanWidth);

        var runsLeftX = placement.trailingRowSlotX() - runOffsets.runsWidth();
        var textSpans = valueRowSlot.textSpans();

        for (var index = 0; index < textSpans.size(); index++) {
            rowPaint.drawSpan(
                textSpans.get(index),
                runsLeftX + runOffsets.runOffsetXs().get(index),
                placement.rowTopY(),
                LazyFont.TextAnchor.TOP_LEFT);
        }
    }

    // The square any small image hangs in on this row: from the given left edge, the row's own line on
    // each side. Shared by the crest in the leading column and by an image run set among the words,
    // because both report exactly that width to the layout - so the room reserved and the square painted
    // are the same size wherever the image sits, and it stands level with the text beside it whatever
    // face that text draws in.
    private static Rectangle computeImageBox(
            float imageX,
            TooltipLayout.TooltipRowLayout placement) {

        return new Rectangle(
            imageX,
            placement.rowTopY() - placement.lineHeight(),
            placement.lineHeight(),
            placement.lineHeight());
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

        // The width one of this row's spans draws at, measured on exactly the face and casing it will be
        // painted in - the same binding the layout measured the row through, so a value laid out inside a
        // column cannot come out wider than the column the box reserved for it.
        private double measureSpanWidth(TextSpan textSpan) {
            return LazyFontSpanMeasurer.measureSpanWidth(
                textStyle.face(),
                textStyle.resolveDisplayText(textSpan.text()));
        }
    }
}
