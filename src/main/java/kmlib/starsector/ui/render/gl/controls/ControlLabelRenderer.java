package kmlib.starsector.ui.render.gl.controls;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.colour.StarsectorUiColour;
import kmlib.starsector.ui.font.LazyFontSpanMeasurer;
import kmlib.starsector.ui.layout.ControlStripLayout;
import kmlib.starsector.ui.render.gl.LabelRenderer;
import kmlib.starsector.ui.render.gl.LabelStyle;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiFill;
import kmlib.starsector.ui.render.gl.UiSprite;
import kmlib.starsector.ui.text.ImageSpan;
import kmlib.starsector.ui.text.LabelRun;
import kmlib.starsector.ui.text.LabelRunPainter;
import kmlib.starsector.ui.text.LabelRuns;
import kmlib.starsector.ui.text.RedactedSpan;
import kmlib.starsector.ui.text.StyledSpanMeasurer;
import kmlib.starsector.ui.text.TextSpan;

import org.lazywizard.lazylib.ui.LazyFont;

import java.util.List;

/**
 * Draws the text a control shows, at the body face the strip laid the control out in: a label authored as
 * several runs painted run by run, each in its own colour, with an image or a withheld name set among the
 * words; and plain text - a radio's option names, a trailing caption - drawn in the vanilla text tone,
 * the one place that default is decided.
 *
 * <p>Held apart from the chrome that surrounds it because the two answer to different things. What a
 * widget looks like follows the control's kind, while how its words are set follows the face and the row,
 * and every kind of control shares the latter: the same measurement, the same anchors, the same run walk
 * behind a tick box's caption and a table row's name. Kept in one class with the chrome, that shared half
 * reads as though each widget had its own way of lettering itself.
 *
 * <p>Measured through the binding the strip snapped the control to, so what is drawn fits the room that
 * was reserved for it. The GL passthrough is run only in-engine like the other draw helpers; the caller
 * wraps it in the GL-state save its panel already holds.
 */
final class ControlLabelRenderer {

    // The run count a label drawn from its left edge needs no offset walk for - the one-colour label
    // most controls carry.
    private static final int SINGLE_RUN = 1;

    private ControlLabelRenderer() {
    }

    /**
     * Draws one plain label in the vanilla text colour - the tone a control's text reads in unless its own
     * content named another, which is what a label authored as spans does. The one place that decision is
     * made, so the segment labels, the trailing caption, and any other unspanned text agree.
     *
     * @param paint  the look and alpha the control draws with
     * @param text   the words to set
     * @param x      the anchor's x, in UI units
     * @param y      the anchor's y, in UI units
     * @param anchor which corner or edge of the text the anchor names
     */
    static void drawBodyLabel(
            ControlPaint paint,
            String text,
            float x,
            float y,
            LazyFont.TextAnchor anchor) {

        drawBodySpan(
            paint,
            new TextSpan(text, StarsectorUiColour.VANILLA_TEXT.resolve()),
            x,
            y,
            anchor);
    }

    /**
     * Draws a label's runs from {@code leftX} rightwards, each at the offset the runs measured out to - so
     * a control calling part of its label out, showing a mark beside its words, or blocking a name out
     * reads that way here. Anchored run by run rather than laid as one string: a batched draw would
     * flatten the runs to a single colour, which is the whole of what a second run buys a caller.
     *
     * @param paint     the look and alpha the control draws with
     * @param labelRuns the label's runs in reading order
     * @param leftX     the label's left edge, in UI units
     * @param centreY   the middle line of the row the label is centred on
     */
    static void drawBodyLabelRuns(
            ControlPaint paint,
            List<LabelRun> labelRuns,
            float leftX,
            float centreY) {

        // Each run hands itself to the painter rather than being tested here, so what a run is stays the
        // run's own statement and this pass holds only what each kind looks like on a control row. One
        // painter per label, since what it binds - the control's paint and the row's middle line - is
        // settled for the whole label.
        var labelRunPainter = new BodyLabelRunPainter(paint, centreY);

        // A label of one run starts at leftX whatever it measures, so the walk that exists to find the
        // next run's anchor is skipped - a list of rows is redrawn every frame, and each row would
        // otherwise be measured a second time to learn an offset that is always zero.
        if (labelRuns.size() == SINGLE_RUN) {
            labelRuns
                .get(0)
                .paintRun(labelRunPainter, leftX);
            return;
        }

        // The runs and the offsets they measured out to are walked together here rather than measured in
        // one method and drawn in another: they are aligned by index, so a boundary between them is a
        // boundary a mismatched pair could cross - which is the parallel-list hazard the row model was
        // built to retire, and it is no more welcome inside a draw than it was inside the content.
        var runOffsets = measureBodyLabelRuns(paint, labelRuns);

        for (var index = 0; index < labelRuns.size(); index++) {
            labelRuns
                .get(index)
                .paintRun(labelRunPainter, leftX + runOffsets.runOffsetXs().get(index));
        }
    }

    /**
     * Draws one span at the body size in its own colour, faded by opacity, through the shared label
     * primitive so the control text and any other KM UI text share one cache.
     *
     * @param paint    the look and alpha the control draws with
     * @param textSpan the run to set, in the colour it states
     * @param x        the anchor's x, in UI units
     * @param y        the anchor's y, in UI units
     * @param anchor   which corner or edge of the text the anchor names
     */
    static void drawBodySpan(
            ControlPaint paint,
            TextSpan textSpan,
            float x,
            float y,
            LazyFont.TextAnchor anchor) {

        var labelStyle = new LabelStyle(
            paint.resolveBodyFace(),
            textSpan.colour(),
            paint.opacity());

        LabelRenderer.render(labelStyle, textSpan.text(), x, y, anchor);
    }

    /**
     * The same runs set about {@code centreX}: the label's whole measured span is centred as one, so the
     * runs stay one sentence rather than each centring on its own. Only the width is needed to find where
     * the label starts; where each run sits within it is the draw's own walk.
     *
     * @param paint     the look and alpha the control draws with
     * @param labelRuns the label's runs in reading order
     * @param centreX   the middle of the space the label is centred in, in UI units
     * @param centreY   the middle line of the row the label is centred on
     */
    static void drawCentredBodyLabelRuns(
            ControlPaint paint,
            List<LabelRun> labelRuns,
            float centreX,
            float centreY) {

        var runsWidth = measureBodyLabelRuns(paint, labelRuns).runsWidth();

        drawBodyLabelRuns(
            paint,
            labelRuns,
            centreX - runsWidth / 2f,
            centreY);
    }

    /**
     * The same runs set to finish at {@code rightX}, for a value laid into a column reserved from its
     * right edge. Sibling to {@link #drawCentredBodyLabelRuns}, differing only in which edge the
     * measured width is taken off - and, through {@link LabelRuns#paintRunsEndingAt}, measured once for
     * both the edge and the anchors rather than walked a second time to draw.
     *
     * @param paint     the look and alpha the control draws with
     * @param labelRuns the label's runs in reading order
     * @param rightX    the right edge the runs finish at, in UI units
     * @param centreY   the middle line of the row the label is centred on
     */
    static void drawBodyLabelRunsEndingAt(
            ControlPaint paint,
            List<LabelRun> labelRuns,
            float rightX,
            float centreY) {

        LabelRuns.paintRunsEndingAt(
            labelRuns,
            rightX,
            ControlStripLayout.CONTROL_ROW_HEIGHT,
            bindBodySpanMeasurer(paint),
            new BodyLabelRunPainter(paint, centreY));
    }

    // The body-line measurement bound to the face this pass paints in, so a run charges its own width
    // without the walk above learning which face it will be drawn in.
    private static StyledSpanMeasurer bindBodySpanMeasurer(ControlPaint paint) {

        var bodyFace = paint.resolveBodyFace();

        return textSpan -> LazyFontSpanMeasurer.measureSpanWidth(bodyFace, textSpan.text());
    }

    // The foot of the band a control's label stands in, the row being centred on its middle line. Held
    // here because more than one element is set against that band - anything hung on the row rather than
    // written along it - and two readings of where the band starts would paint them at two heights.
    private static float computeLabelBottomY(float centreY) {

        return centreY - ControlStripLayout.CONTROL_ROW_HEIGHT / 2f;
    }

    // The square an image run hangs in: from its own anchor, the control row's height on each side, set
    // about the row's middle line so it is centred exactly as the text beside it is. The image run
    // reports that same height to the layout, so the room reserved and the square painted agree.
    private static Rectangle computeLabelImageBox(float runX, float centreY) {

        return new Rectangle(
            runX,
            computeLabelBottomY(centreY),
            ControlStripLayout.CONTROL_ROW_HEIGHT,
            ControlStripLayout.CONTROL_ROW_HEIGHT);
    }

    // Where a label's runs measure out to at the body face, through the same one-sentence rule the strip
    // layout snapped the control to - so what is drawn fits the room that was reserved for it. The row
    // height goes along for the same reason it does there: it is what an image run sizes itself off.
    private static LabelRuns.LabelRunOffsets measureBodyLabelRuns(
            ControlPaint paint,
            List<LabelRun> labelRuns) {

        return LabelRuns.measureRunOffsets(
            labelRuns,
            ControlStripLayout.CONTROL_ROW_HEIGHT,
            bindBodySpanMeasurer(paint));
    }

    /**
     * What draws one label's runs: each kind of run set against the control row's middle line, in the
     * paint the pass is drawing with. Bound to the label rather than to the run, since the paint and the
     * row's line are the label's for all of its runs and only the anchor moves between them.
     *
     * @param paint   the look, opacity, and cell paints the control draws with
     * @param centreY the middle line of the row the label is centred on
     */
    private record BodyLabelRunPainter(
        ControlPaint paint,
        float centreY) implements LabelRunPainter {

        @Override
        public void paintImageSpan(ImageSpan imageSpan, float runX) {

            UiSprite.renderImage(
                imageSpan.spritePath(),
                computeLabelImageBox(runX, centreY),
                paint.opacity(),
                imageSpan.tintColour());
        }

        // Filled in the run's own fill colour and faded by the frame's alpha, like every other mark on
        // the row - a redaction is a name drawn as blocks rather than chrome of the control, so it reads
        // in the colour the label it stands in was written in, short the step a solid block is taken down
        // by.
        //
        // At the standard strength rather than one of the control strip's own: a strip is set in the same
        // faces at the same sizes as the boxes beside it, so the balance measured there holds here, and a
        // knob for it would be a second answer to a question nothing here asks differently.
        @Override
        public void paintRedactedSpan(RedactedSpan redactedSpan, float runX) {

            // Measured through the body binding the strip laid the control out by, so the blocks fill
            // exactly the stretch the row reserved for the withheld name. Filled as one run, since the
            // blocks are one redaction rather than several marks that happen to share a colour.
            UiFill.renderQuads(
                redactedSpan.layOutWordBars(
                    runX,
                    computeLabelBottomY(centreY),
                    ControlStripLayout.CONTROL_ROW_HEIGHT,
                    bindBodySpanMeasurer(paint)),
                new UiElementPaint(
                    redactedSpan.resolveBlockFillColour(
                        RedactedSpan.TEXT_WEIGHT_DARKENING_STRENGTH),
                    paint.opacity()));
        }

        @Override
        public void paintTextSpan(TextSpan textSpan, float runX) {

            drawBodySpan(
                paint,
                textSpan,
                runX,
                centreY,
                LazyFont.TextAnchor.CENTER_LEFT);
        }
    }
}
