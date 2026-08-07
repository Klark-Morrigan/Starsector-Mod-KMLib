package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.colour.StarsectorUiColour;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.RowGeometry;
import kmlib.starsector.ui.font.LazyFontSpanMeasurer;
import kmlib.starsector.ui.font.TextFace;
import kmlib.starsector.ui.layout.ControlStripLayout;
import kmlib.starsector.ui.layout.TabsControlLayout;
import kmlib.starsector.ui.text.ImageSpan;
import kmlib.starsector.ui.text.LabelRun;
import kmlib.starsector.ui.text.LabelRuns;
import kmlib.starsector.ui.text.StyledSpanMeasurer;
import kmlib.starsector.ui.text.TextSpan;
import kmlib.starsector.ui.widgets.Checkbox;
import kmlib.starsector.ui.widgets.IconLabelRow;
import kmlib.starsector.ui.widgets.LabelledRow;
import kmlib.starsector.ui.widgets.RowSlot;
import kmlib.starsector.ui.widgets.tabs.TabInteractionSources;
import kmlib.starsector.ui.widgets.tabs.TabLookSource;
import kmlib.starsector.ui.widgets.tabs.TabWashSource;
import kmlib.starsector.ui.widgets.tabs.VanillaTabStrip;

import org.lazywizard.lazylib.ui.LazyFont;

import java.util.ArrayList;
import java.util.List;

/**
 * Raw-GL paint for one laid-out {@link Control}: it draws the widget the control's {@link ControlSpec}
 * variant names - the tick box, the radio segments, the toggle, the divider rule, the caption - in the
 * lit state the spec carries, and draws the control's label(s) over it in a body font. The variant names
 * the widget; what the control means stays with whoever built the spec, so this draws a faction toggle or
 * a sort selector the same way without learning either.
 *
 * <p>A label a control authored as several runs is drawn run by run, each in its own colour, so a
 * caller picking a stretch of a label out gets it picked out here. Text that is not authored as spans -
 * a radio's option labels, a trailing caption - is drawn in the vanilla text tone, the one place that
 * default is decided.
 *
 * <p>It composes the per-kind KMLib renderers ({@link CheckboxRenderer}, {@link RadioRowRenderer}, {@link
 * IconRadioListRenderer}, {@link ToggleButton}, {@link DividerRenderer}, {@link VanillaTabStripRenderer})
 * and the shared label paint, so a host renders a whole strip of controls by calling this per control. The
 * GL passthrough is exercised in-engine like the other draw helpers; the caller wraps it in the GL-state
 * save its panel already holds. A small cache mints one GL text buffer per (font, size, text) so a steady
 * body does not leak a buffer per frame.
 */
public final class ControlRenderer {

    // The run count a label drawn from its left edge needs no offset walk for - the one-colour label
    // most controls carry.
    private static final int SINGLE_RUN = 1;

    private ControlRenderer() {
    }

    /**
     * Draws a body control - one with no tabs to be interacting with, so it is drawn as
     * {@link #render(Control, WidgetStyle, float, TabInteractionSources)} with nothing happening to any tab.
     *
     * @param control the laid-out control to draw
     * @param style   the look bundle - accents and body font for every kind
     * @param opacity overall alpha, 0..1
     */
    public static void render(Control control, WidgetStyle style, float opacity) {
        render(control, style, opacity, TabInteractionSources.RESTING);
    }

    /**
     * Draws {@code control} in the lit state its spec carries, styled from {@code style} and faded by
     * {@code opacity}. A tick box / toggle lights its accent when its cell is selected; a radio frames its
     * segments in the accent and washes the active one; a checkbox uses the bright accent for its tick; a
     * tabs row draws the vanilla-styled strip in the style's tab colours and face, lighting the selected tab
     * and painting each tab at whatever point of its hover fade and pulse {@code tabInteractions} reports.
     * Must run with a current GL context, like any immediate-mode GL call.
     *
     * @param control         the laid-out control to draw
     * @param style           the look bundle - accents and body font for every kind, tab colours and face
     *                        for a tabs row
     * @param opacity         overall alpha, 0..1
     * @param tabInteractions what each tab of a tabs row is currently showing; unread by every other kind,
     *                        since only a tabs row has tabs to interact with
     */
    public static void render(
            Control control,
            WidgetStyle style,
            float opacity,
            TabInteractionSources tabInteractions) {

        var paint = new ControlPaint(style, opacity);
        var spec = control.spec();
        if (spec instanceof ControlSpec.Checkbox) {
            drawCheckbox(control, paint);
        } else if (spec instanceof ControlSpec.Toggle) {
            drawToggle(control, paint);
        } else if (spec instanceof ControlSpec.HorizontalRadio
                || spec instanceof ControlSpec.VerticalTable) {
            drawRadio(control, paint);
        } else if (spec instanceof ControlSpec.Label) {
            drawLabelRow(control, paint);
        } else if (spec instanceof ControlSpec.Divider) {
            drawDivider(control, paint);
        } else if (spec instanceof ControlSpec.Tabs) {
            drawTabs(control, paint, tabInteractions);
        }
    }

    // A tabs row: the vanilla Sector/System strip, each tab drawn in its snapped segment at whatever point
    // of its hover fade and pulse the caller reports. The segments were split to text by the layout;
    // pairing each with its content (rebuilt from the spec through the same helper the layout measured
    // with) yields the tabs the strip renderer paints, so the drawn tab matches the hit box.
    private static void drawTabs(
            Control control,
            ControlPaint paint,
            TabInteractionSources tabInteractions) {

        var spec = (ControlSpec.Tabs) control.spec();
        var contents = TabsControlLayout.buildTabContents(spec);
        var tabs = VanillaTabStrip.zipTabs(contents, control.segments());

        // The same value the layout measured the band against, so a strip is drawn in exactly the look
        // it was laid out under.
        var tabStyle = paint.style().tabStyle();

        // Both channels arrive as bare fractions and are bound to the palette here, the one place holding
        // both. Neither is resolved from scratch: the cursor is not read (the panel's own state says which
        // tab is hovered, tested against the placement it was drawn at) and no timing is held (a click is an
        // event, and its decay belongs with whatever saw it).
        VanillaTabStripRenderer.render(
            tabs,
            TabLookSource.createHoverFadedLookSource(
                tabStyle.palette(),
                spec.selectedIndex(),
                tabInteractions.hoverSource()),
            TabWashSource.createClickPulsedWashSource(
                tabStyle.palette(),
                tabInteractions.pulseSource()),
            tabStyle,
            paint.opacity());
    }

    // A tick box lit when the spec's cell is selected, then its label at the anchor the widget places
    // it at - the same anchor the row was sized around, so the label sits exactly in the space snapped
    // for it.
    private static void drawCheckbox(Control control, ControlPaint paint) {
        var style = paint.style();
        var spec = (ControlSpec.Checkbox) control.spec();
        var bounds = control.bounds();

        CheckboxRenderer.render(
            bounds,
            spec.isLit(),
            new UiElementPaint(style.accentColours().base(), paint.opacity()),
            new UiElementPaint(style.accentColours().bright(), paint.opacity()));

        drawBodyLabelRuns(
            paint,
            spec.labelRuns(),
            Checkbox.computeLabelAnchorX(bounds),
            bounds.computeCenterY());
    }

    // A radio group: the segments framed and the active one washed, then its labels. A table lays each
    // row in columns - what it leads with, its name past that, its trailing slot flush right; a
    // uniform-cell list and a horizontal radio centre each name in its segment, the radio appending its
    // trailing caption. A vertical stack re-derives its grid from the footprint; a horizontal radio draws
    // its chrome over the laid segments, the same rects the labels below centre in, so the wash and
    // dividers cannot part from the labels whether even or snapped.
    private static void drawRadio(Control control, ControlPaint paint) {
        var spec = control.spec();
        if (spec instanceof ControlSpec.VerticalTable table
                && table.rowGeometry() == RowGeometry.COLUMNS) {
            drawColumnTable(control, table, paint);
            return;
        }
        var accent = paint.style().accentColours().base();
        var bounds = control.bounds();
        var labels = spec.labels();
        var segments = control.segments();
        var selectedIndex = ((ControlSpec.Interactive) spec).selectedIndex();

        // Frame and wash both stroke the accent, the plain radio's single chrome tone.
        var colours = new RadioColours(accent, accent);
        if (spec instanceof ControlSpec.VerticalTable table) {
            RadioRowRenderer.renderVerticalGrid(
                bounds,
                labels.size(),
                selectedIndex,
                table.columnCount(),
                colours,
                paint.opacity());
        } else {
            RadioRowRenderer.renderHorizontalRow(
                bounds,
                segments,
                selectedIndex,
                colours,
                paint.opacity());
        }
        for (var index = 0; index < segments.size() && index < labels.size(); index++) {
            var segment = segments.get(index);
            drawBodyLabel(
                paint,
                labels.get(index),
                segment.computeCenterX(),
                segment.computeCenterY(),
                LazyFont.TextAnchor.CENTER);
        }
        if (spec instanceof ControlSpec.HorizontalRadio radio && radio.hasTrailingCaption()) {

            var trailingX = bounds.x()
                + bounds.width()
                + ControlStripLayout.TRAILING_LABEL_GAP;

            drawBodyLabel(
                paint,
                radio.trailingLabel(),
                trailingX,
                bounds.computeCenterY(),
                LazyFont.TextAnchor.CENTER_LEFT);
        }
    }

    // A table of rows laid in columns: what the row leads with at its left, the name past it, and its
    // trailing slot flush at the right edge. The list chrome and the leading images are the widget's; the
    // name and the trailing slot draw here at the same anchors the widget reserves, so a row leading with
    // nothing reads as a plain name and a row trailing with nothing shows only its name.
    private static void drawColumnTable(
            Control control,
            ControlSpec.VerticalTable spec,
            ControlPaint paint) {

        var accent = paint.style().accentColours().base();
        var bounds = control.bounds();
        var labelledRows = spec.labelledRows();

        IconRadioListRenderer.render(
            bounds,
            resolveLeadingRowSlots(labelledRows),
            spec.selectedIndex(),
            spec.columnCount(),
            new RadioColours(accent, accent),
            paint.opacity());

        var segments = control.segments();

        for (var index = 0; index < segments.size() && index < labelledRows.size(); index++) {
            var segment = segments.get(index);
            var labelledRow = labelledRows.get(index);
            // The label starts past the leading slot when the row fills one, or at the row's left inset
            // when it does not - the same filled-slot rule the layout sized the row with.
            var labelX = IconLabelRow.computeLabelAnchorX(
                segment,
                labelledRow.leadingRowSlot().isFilled());

            drawBodyLabelRuns(
                paint,
                labelledRow.labelRuns(),
                labelX,
                segment.computeCenterY());

            drawTrailingRowSlot(
                labelledRow.trailingRowSlot(),
                segment,
                paint);
        }
    }

    // Each row's leading slot in row order, for the list widget that draws whatever image sits in one.
    // The widget is handed the slots rather than the rows because the leading column is all it paints -
    // the label and the trailing column are drawn here, against the anchors it reserves.
    private static List<RowSlot> resolveLeadingRowSlots(List<LabelledRow> labelledRows) {
        var leadingRowSlots = new ArrayList<RowSlot>(labelledRows.size());
        for (var labelledRow : labelledRows) {
            leadingRowSlots.add(labelledRow.leadingRowSlot());
        }
        return leadingRowSlots;
    }

    // The row's trailing slot: a filled direction triangle for a marker the body font has no glyph for
    // (a sort selector's ascending/descending), or the right-aligned run of text a ranked row shows as
    // its value. Both right-align to the same inset the layout reserved, so a triangle column and a value
    // column occupy the same right-hand strip. A row trailing with nothing draws nothing here.
    private static void drawTrailingRowSlot(
            RowSlot trailingRowSlot,
            Rectangle segment,
            ControlPaint paint) {

        if (trailingRowSlot instanceof RowSlot.Triangle triangle) {
            // In the row's body text tone so it reads as a quiet annotation like the value it replaces.
            var trianglePaint = new UiElementPaint(
                StarsectorUiColour.VANILLA_TEXT.resolve(),
                paint.opacity());
            TriangleRenderer.render(
                IconLabelRow.computeDirectionTriangleBox(segment),
                triangle.triangleDirection(),
                trianglePaint);
            return;
        }
        if (trailingRowSlot instanceof RowSlot.Text text && text.textSpan().hasContent()) {
            // Drawn at the body size - the same size the layout reserved the column at - and in the run's
            // own colour, so a value the row picked out reads as picked out here too.
            drawBodySpan(
                paint,
                text.textSpan(),
                IconLabelRow.computeTrailingAnchorX(segment),
                segment.computeCenterY(),
                LazyFont.TextAnchor.CENTER_RIGHT);
        }
    }

    // A single button washed when the spec's cell is lit, its label centred in it - the lit state is the
    // on/off signal, so the label carries no On/Off word.
    private static void drawToggle(Control control, ControlPaint paint) {
        var accent = paint.style().accentColours().base();
        var spec = (ControlSpec.Toggle) control.spec();
        var bounds = control.bounds();

        ToggleButton.render(
            bounds,
            spec.isLit(),
            accent,
            accent,
            paint.opacity());

        drawCentredBodyLabelRuns(
            paint,
            spec.labelRuns(),
            bounds.computeCenterX(),
            bounds.computeCenterY());
    }

    // A divider row: a single hairline centred across the row in the accent, parting one run of controls
    // from the next - it heads a section like a caption but carries no text and no hit target.
    private static void drawDivider(Control control, ControlPaint paint) {
        DividerRenderer.render(control.bounds(), paint.style().accentColours().base(), paint.opacity());
    }

    // A caption row: only its text, left-aligned at the row's left edge and vertically centred, with no
    // widget chrome - it heads the controls below it and is never clicked.
    private static void drawLabelRow(Control control, ControlPaint paint) {
        var spec = (ControlSpec.Label) control.spec();
        var bounds = control.bounds();
        drawBodyLabelRuns(
            paint,
            spec.labelRuns(),
            bounds.x(),
            bounds.computeCenterY());
    }

    // Draws a label's runs from leftX rightwards, each at the offset the runs measured out to - text in
    // its own colour, an image hung square on the row - so a control calling part of its label out, or
    // showing a mark beside its words, reads that way here. Anchored run by run rather than laid as one
    // string: a batched draw would flatten the runs to a single colour, which is the whole of what a
    // second run buys a caller.
    private static void drawBodyLabelRuns(
            ControlPaint paint,
            List<LabelRun> labelRuns,
            float leftX,
            float centreY) {

        // A label of one run starts at leftX whatever it measures, so the walk that exists to find the
        // next run's anchor is skipped - a list of rows is redrawn every frame, and each row would
        // otherwise be measured a second time to learn an offset that is always zero.
        if (labelRuns.size() == SINGLE_RUN) {
            drawBodyLabelRun(paint, labelRuns.get(0), leftX, centreY);
            return;
        }

        // The runs and the offsets they measured out to are walked together here rather than measured in
        // one method and drawn in another: they are aligned by index, so a boundary between them is a
        // boundary a mismatched pair could cross - which is the parallel-list hazard the row model was
        // built to retire, and it is no more welcome inside a draw than it was inside the content.
        var runOffsets = measureBodyLabelRuns(paint, labelRuns);

        for (var index = 0; index < labelRuns.size(); index++) {
            drawBodyLabelRun(
                paint,
                labelRuns.get(index),
                leftX + runOffsets.runOffsetXs().get(index),
                centreY);
        }
    }

    // Draws one run of a label at its resolved anchor, centred on the row's own middle line: a stretch of
    // text in its own colour, or a small image squared off the control row so it stands level with the
    // words beside it. The branch is over what the run is, so which control the label belongs to never
    // enters into it.
    private static void drawBodyLabelRun(
            ControlPaint paint,
            LabelRun labelRun,
            float runX,
            float centreY) {

        if (labelRun instanceof TextSpan textSpan) {
            drawBodySpan(
                paint,
                textSpan,
                runX,
                centreY,
                LazyFont.TextAnchor.CENTER_LEFT);
            return;
        }
        if (labelRun instanceof ImageSpan imageSpan) {
            UiSprite.renderImage(
                imageSpan.spritePath(),
                computeLabelImageBox(runX, centreY),
                paint.opacity());
        }
    }

    // The square an image run hangs in: from its own anchor, the control row's height on each side, set
    // about the row's middle line so it is centred exactly as the text beside it is. The image run
    // reports that same height to the layout, so the room reserved and the square painted agree.
    private static Rectangle computeLabelImageBox(float runX, float centreY) {
        return new Rectangle(
            runX,
            centreY - ControlStripLayout.CONTROL_ROW_HEIGHT / 2f,
            ControlStripLayout.CONTROL_ROW_HEIGHT,
            ControlStripLayout.CONTROL_ROW_HEIGHT);
    }

    // The same runs set about centreX: the label's whole measured span is centred as one, so the runs
    // stay one sentence rather than each centring on its own. Only the width is needed to find where
    // the label starts; where each run sits within it is the draw's own walk.
    private static void drawCentredBodyLabelRuns(
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

    // The body-line measurement bound to the face this pass paints in, so a run charges its own width
    // without the walk above learning which face it will be drawn in.
    private static StyledSpanMeasurer bindBodySpanMeasurer(ControlPaint paint) {
        var bodyFace = paint.resolveBodyFace();
        return textSpan -> LazyFontSpanMeasurer.measureSpanWidth(bodyFace, textSpan.text());
    }

    // Draws one plain label in the vanilla text colour - the tone a control's text reads in unless its
    // own content named another, which is what a label authored as spans does. The one place that
    // decision is made, so the segment labels, the trailing caption, and any other unspanned text agree.
    private static void drawBodyLabel(
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

    // Draws one span at the body size in its own colour, faded by opacity, through the shared label
    // primitive so the control text and any other KM UI text share one cache.
    private static void drawBodySpan(
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

    // The look bundle plus the frame's alpha, threaded together through every draw so a helper takes one
    // paint rather than unpacking the accents, body font, and opacity into loose arguments each time.
    private record ControlPaint(WidgetStyle style, float opacity) {

        // The face every control's text is measured and drawn at. Asked of the paint rather than built
        // where it is wanted, so the measurement and the draw cannot end up naming a different pair -
        // text measured on one face and painted on another sizes a row it then overflows.
        private TextFace resolveBodyFace() {
            return new TextFace(
                style.bodyFont(),
                ControlStripLayout.BODY_FONT_SIZE);
        }
    }
}
