package kmlib.starsector.ui.render.gl.tooltip;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.font.LazyFontCache;
import kmlib.starsector.ui.font.LazyFontSpanMeasurer;
import kmlib.starsector.ui.input.UiCursor;
import kmlib.starsector.ui.render.gl.GlStateGuard;
import kmlib.starsector.ui.render.gl.LabelRenderer;
import kmlib.starsector.ui.render.gl.LabelStyle;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiFill;
import kmlib.starsector.ui.render.gl.UiSprite;
import kmlib.starsector.ui.render.gl.panel.BorderedBoxRenderer;
import kmlib.starsector.ui.screen.VanillaScreen;
import kmlib.starsector.ui.text.ImageSpan;
import kmlib.starsector.ui.text.LabelRunPainter;
import kmlib.starsector.ui.text.RedactedSpan;
import kmlib.starsector.ui.text.TextSpan;
import kmlib.starsector.ui.text.TextStyle;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.LabelledRow;
import kmlib.starsector.ui.widgets.RowSlot;
import kmlib.starsector.ui.widgets.tooltip.CursorTooltip;
import kmlib.starsector.ui.widgets.tooltip.TooltipHeightFit;
import kmlib.starsector.ui.widgets.tooltip.TooltipLayout;
import kmlib.starsector.ui.widgets.tooltip.TooltipRow;
import kmlib.starsector.ui.widgets.tooltip.TooltipSection;

import org.lazywizard.lazylib.ui.LazyFont;

import java.util.List;

/**
 * Draws a {@link CursorTooltip}'s blocks as a free-floating box at the cursor: it lays them out through
 * the widget, resolves each row's look from the kind of line it is, and paints the box, each crest, each
 * row's label - run by run, words, inline images and withheld names alike - its value, and the
 * {@linkplain TooltipLeaderLineRenderer rule} led between the two, in one bracketed GL pass.
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
 *
 * <p>Beside the draw it answers how tall a box would stand and how much room there is for one, neither
 * of which spends a frame or touches GL. A caller with more to say than the screen holds settles that
 * against these two before it hands the content over, since the box itself only clamps - drawn, an
 * over-tall box runs past both edges and says nothing about what it lost. A caller that would rather
 * keep every line than choose between them hands the box over to {@link #renderFitted} instead, which
 * gives up size on the way in.
 */
public final class CursorTooltipRenderer {

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
        var layout = CursorTooltip.layOut(
            sections,
            style.typography(),
            LazyFontSpanMeasurer::measureSpanWidth,
            UiCursor.getUiX(),
            UiCursor.getUiY(),
            resolveScreenBound());

        // Painted off the flat run of lines the layout anchored, in the same order: how those lines were
        // grouped was spent settling the spacing, and the draw has nothing left to do with it.
        var rows = TooltipSection.readRowsInOrder(sections);

        // The core map and its tooltips draw after this pass, so the box, crests, and text run inside
        // the shared state save that restores the blend and colour the draw touched on the way out.
        GlStateGuard.bracket(() -> drawRows(rows, layout, style));
    }

    /**
     * Lays out {@code sections} at the live cursor and paints them in {@code style}, compressed first
     * where they stand taller than the screen: the deepest line the blocks hold keeps its size while the
     * tiers above it draw smaller and closer together, until the box fits. A box that already fits is
     * drawn exactly as {@link #render} would draw it.
     *
     * <p>For a box whose content is the player's to ask for rather than the caller's to choose - one
     * that lists whatever a subject happens to hold, and so has no size it can be authored to. What the
     * compression can buy is bounded, so a box deep enough still overflows; a caller that must not
     * overflow weighs its content itself ({@link #measureBoxHeight} against
     * {@link #resolveHeightBudget}) and gives some of it up before drawing.
     *
     * @param sections the content blocks, top to bottom
     * @param style    the typography, opacity, and box chrome the whole tooltip draws in
     */
    public static void renderFitted(List<TooltipSection> sections, CursorTooltipStyle style) {

        render(
            sections,
            style.restyledAs(TooltipHeightFit.fitToHeight(
                sections,
                style.typography(),
                resolveHeightBudget())));
    }

    /**
     * Answers how tall {@code sections} would stand in {@code style}, the box's own chrome included,
     * without drawing anything or reading the cursor.
     *
     * <p>Read against {@link #resolveHeightBudget()} by a caller that has to settle what it can afford to
     * show before it shows it: a box is sized by its content and then clamped on screen, so content
     * taller than the screen is drawn with its ends past the edges and nothing on screen says what was
     * lost. The two answers together are what let such a caller respond in its own terms - by cutting
     * content, by restyling it smaller, or by accepting the overflow - rather than finding out from a
     * screenshot.
     *
     * @param sections the content blocks, top to bottom
     * @param style    the typography, opacity, and box chrome the tooltip would draw in
     * @return the height the box would occupy, in UI units
     */
    public static float measureBoxHeight(List<TooltipSection> sections, CursorTooltipStyle style) {
        return CursorTooltip.measureBoxHeight(sections, style.typography());
    }

    /**
     * @return the tallest a box can stand and still be drawn whole, in UI units - the height of the very
     *         bound {@link #render} clamps the box inside, read live, since the player can resize the
     *         window or rescale the UI between one frame and the next
     */
    public static float resolveHeightBudget() {
        return resolveScreenBound().height();
    }

    // The region a box is kept inside: the whole screen rather than a region of it, so the screen's own
    // box goes over as it stands. Read through one call so the bound a box is clamped to and the budget
    // its height is weighed against cannot end up describing two different screens.
    private static Rectangle resolveScreenBound() {
        return VanillaScreen.resolveScreenBox();
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

        // Ruled along the stretch the layout measured, not one worked out from the anchors beside it: the
        // label's end and the value's start were settled when the row was placed, and a rule re-derived
        // here could only drift from the two columns it exists to join. Attempted on every row, since a
        // row with no rule to lead carries a stretch that draws nothing - the same reading a blank span
        // gets, so no branch here has to learn which rows have values.
        TooltipLeaderLineRenderer.render(
            placement.leaderLine(),
            placement.rowTopY(),
            rowPaint.textStyle().face(),
            style.leaderLineStyle(),
            rowPaint.opacity());

        // Anchored as the layout pinned them, not by each style's own alignment: the columns are the
        // layout's decision, so a style's default anchor has no say in a box that resolved its own. The
        // runs walk in step with the anchors the same rows produced, so run and anchor cannot slip. Every
        // run is drawn unconditionally - a blank span paints nothing.
        //
        // Each run hands itself to the painter rather than being tested here, so what a run is stays the
        // run's own statement and this pass holds only what each kind looks like on a tooltip line. One
        // painter per row, since what it binds - the row's placement and its resolved paint - is settled
        // for the whole row.
        var labelRunPainter = new RowLabelRunPainter(
            placement,
            rowPaint,
            style.redactionDarkeningStrength());
        var labelRuns = row.labelRuns();

        for (var index = 0; index < labelRuns.size(); index++) {
            labelRuns
                .get(index)
                .paintRun(labelRunPainter, placement.labelRunXs().get(index));
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
        // end up compositing at two different alphas, and multiplied by whatever tint the slot states -
        // honoured here as well as in the list widget, since a slot whose tint one of the two surfaces
        // silently ignored would be worse than one that carried none.
        if (labelledRow.leadingRowSlot() instanceof RowSlot.Image crestRowSlot) {
            UiSprite.renderImage(
                crestRowSlot.spritePath(),
                computeImageBox(placement.leadingRowSlotX(), placement),
                rowPaint.opacity(),
                crestRowSlot.tintColour());
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
            computeLineBottomY(placement),
            placement.lineHeight(),
            placement.lineHeight());
    }

    // The foot of the band a row's line stands in, the layout having anchored the row by its top. Held
    // here because more than one element is set against that band - anything hung on the line rather than
    // written along it - and two readings of where a line's foot is would paint them at two heights.
    private static float computeLineBottomY(TooltipLayout.TooltipRowLayout placement) {

        return placement.rowTopY() - placement.lineHeight();
    }

    /**
     * What draws one row's label runs: each kind of run set against the row's own line, in the paint the
     * row resolved. Bound to the row rather than to the run, since the placement and the look are the
     * row's for all of its runs and only the anchor moves between them.
     *
     * @param placement                  where the layout pinned this row and its columns
     * @param rowPaint                   the face, casing, and opacity the row's runs draw in
     * @param redactionDarkeningStrength how far a withheld name's blocks are sunk from the colour of the
     *                                   line they stand in, the box's own setting for it
     */
    private record RowLabelRunPainter(
        TooltipLayout.TooltipRowLayout placement,
        RowPaint rowPaint,
        float redactionDarkeningStrength) implements LabelRunPainter {

        @Override
        public void paintImageSpan(ImageSpan imageSpan, float runX) {

            UiSprite.renderImage(
                imageSpan.spritePath(),
                computeImageBox(runX, placement),
                rowPaint.opacity(),
                imageSpan.tintColour());
        }

        // Filled in the run's own fill colour and faded by the box's opacity, like every other mark on
        // the line - a redaction is a name drawn as blocks rather than a chrome element of the box, so
        // it reads in the colour the line it stands on was written in, short the step the run itself
        // takes off a solid block.
        @Override
        public void paintRedactedSpan(RedactedSpan redactedSpan, float runX) {

            // Measured through the row's own binding, the one the layout charged the line by, so the
            // blocks fill exactly the stretch the box reserved for the withheld name. Filled as one run,
            // since the blocks are one redaction rather than several marks that happen to share a colour.
            UiFill.renderQuads(
                redactedSpan.layOutWordBars(
                    runX,
                    computeLineBottomY(placement),
                    placement.lineHeight(),
                    rowPaint::measureSpanWidth),
                new UiElementPaint(
                    redactedSpan.resolveBlockFillColour(redactionDarkeningStrength),
                    rowPaint.opacity()));
        }

        @Override
        public void paintTextSpan(TextSpan textSpan, float runX) {

            rowPaint.drawSpan(
                textSpan,
                runX,
                placement.rowTopY(),
                LazyFont.TextAnchor.TOP_LEFT);
        }
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
