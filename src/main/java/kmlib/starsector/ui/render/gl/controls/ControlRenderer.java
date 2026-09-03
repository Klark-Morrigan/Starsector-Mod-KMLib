package kmlib.starsector.ui.render.gl.controls;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.colour.StarsectorUiColour;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlInteractionSources;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.RowGeometry;
import kmlib.starsector.ui.layout.ControlStripLayout;
import kmlib.starsector.ui.layout.TabsControlLayout;
import kmlib.starsector.ui.render.gl.TriangleRenderer;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.style.WidgetStyle;
import kmlib.starsector.ui.render.gl.tabs.TabChromeRenderer;
import kmlib.starsector.ui.text.LabelRun;
import kmlib.starsector.ui.widgets.Checkbox;
import kmlib.starsector.ui.widgets.IconLabelRow;
import kmlib.starsector.ui.widgets.LabelledRow;
import kmlib.starsector.ui.widgets.RowSlot;
import kmlib.starsector.ui.widgets.RowSlotPainter;
import kmlib.starsector.ui.widgets.tabs.TabInteractionSources;
import kmlib.starsector.ui.widgets.tabs.TabLightSource;
import kmlib.starsector.ui.widgets.tabs.TabLookSource;
import kmlib.starsector.ui.widgets.tabs.TabPaintSources;
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
 * <p>What the pointer is doing to the control arrives as fractions rather than as a hovered index, and is
 * bound to the look's hovered wash and press light here - the one place holding both a motion's progress and
 * the shade it travels toward. Each widget below is handed finished paint per cell, so no renderer reads a
 * cursor, holds a timing, or learns that a fade exists; what a cell's lift is made of stays the widget's own,
 * and only its pace is the panel's.
 *
 * <p>Every word a control shows is set by {@link ControlLabelRenderer}, whichever kind of control it
 * belongs to: how a label's runs are measured and anchored follows the face and the row rather than the
 * widget, so this pass decides only where each label goes and what chrome stands behind it.
 *
 * <p>It composes the per-kind KMLib renderers ({@link CheckboxRenderer}, {@link RadioRowRenderer}, {@link
 * IconRadioListRenderer}, {@link ToggleButton}, {@link DividerRenderer}, and whichever
 * {@link TabChromeRenderer} the style names)
 * and the shared label paint, so a host renders a whole strip of controls by calling this per control. The
 * GL passthrough is run only in-engine like the other draw helpers; the caller wraps it in the GL-state
 * save its panel already holds. A small cache mints one GL text buffer per (font, size, text) so a steady
 * body does not leak a buffer per frame.
 */
public final class ControlRenderer {

    private ControlRenderer() {
    }

    /**
     * Draws a body control at whatever point of its hover fades and press lifts its cells stand - one with no
     * tabs to be interacting with, so it is drawn as {@link #render(Control, WidgetStyle, float,
     * TabInteractionSources, ControlInteractionSources)} with nothing happening to any tab. A consumer drawing
     * a strip without an animator behind it passes {@link ControlInteractionSources#RESTING}, which says so
     * where it is called rather than through an overload that says it by omission.
     *
     * @param control      the laid-out control to draw
     * @param style        the look bundle - accents, the cell treatments, and body font for every kind
     * @param opacity      overall alpha, 0..1
     * @param interactions how far onto its hovered look, and how far through its press lift, each of this
     *                     control's cells stands
     */
    public static void render(
            Control control,
            WidgetStyle style,
            float opacity,
            ControlInteractionSources interactions) {

        render(control, style, opacity, TabInteractionSources.RESTING, interactions);
    }

    /**
     * Draws {@code control} in the lit state its spec carries, styled from {@code style} and faded by
     * {@code opacity}. A tick box / toggle lights its accent when its cell is selected; a radio frames its
     * segments in the accent and washes the active one; a checkbox uses the bright accent for its tick; a
     * tabs row draws in the tab chrome the style names, in its tab colours and face, lighting the selected
     * tab and painting each tab at whatever point of its hover fade and pulse {@code tabInteractions}
     * reports. Every cell a body control hits by washes over that lit state at whatever point of its own
     * hover fade {@code interactions} reports, so what the pointer is on lights whether or not pressing it
     * would do anything, and lights again over that wash for as long as a press it answered is still
     * falling. Must run with a current GL context, like any immediate-mode GL call.
     *
     * @param control         the laid-out control to draw
     * @param style           the look bundle - accents and body font for every kind, tab colours and face
     *                        for a tabs row
     * @param opacity         overall alpha, 0..1
     * @param tabInteractions what each tab of a tabs row is currently showing; unread by every other kind,
     *                        since only a tabs row has tabs to interact with
     * @param interactions    how far onto its hovered look, and how far through its press lift, each of this
     *                        control's cells stands; unread by a tabs row, which answers the pointer through
     *                        its own palette, and by a caption or a divider, which are not hit targets at all
     */
    public static void render(
            Control control,
            WidgetStyle style,
            float opacity,
            TabInteractionSources tabInteractions,
            ControlInteractionSources interactions) {

        // Both channels are bound to the look's own treatments once here, the one place holding a motion's
        // progress and the shade it travels toward, so each widget below is handed finished paint rather
        // than a fraction and a colour to combine for itself.
        var paint = new ControlPaint(
            style,
            opacity,
            new CellPaintSources(
                CellHoverWashSource.createHoverFadedWashSource(
                    style.controlHoverWash(),
                    interactions.hovers(),
                    opacity),
                CellPressLightSource.createPressLitLightSource(
                    style.controlPressLight(),
                    interactions.presses(),
                    opacity)));

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

    // A tabs row, each tab drawn in its snapped segment at whatever point of its hover fade and pulse the
    // caller reports. The segments were split to text by the layout; pairing each with its content (rebuilt
    // from the spec through the same helper the layout measured with) yields the tabs the chrome renderer
    // paints, so the drawn tab matches the hit box.
    //
    // Which chrome it wears is the style's call, so a host matches the tab convention of whatever screen it
    // sits on. The choice is resolved to a painter through the chrome's own seam rather than branched on
    // here: this pass branches over what a control IS, and a second branch over how one of them looks would
    // grow a case every time a chrome is added to a control it does not otherwise know about.
    private static void drawTabs(
            Control control,
            ControlPaint paint,
            TabInteractionSources tabInteractions) {

        var spec = (ControlSpec.Tabs) control.spec();
        var contents = TabsControlLayout.buildTabContents(spec);
        var tabs = VanillaTabStrip.zipTabs(contents, control.segments());

        // The same value the layout measured the band against, so a row is drawn in exactly the look
        // it was laid out under.
        var tabStyle = paint.style().tabStyle();

        // Both channels arrive as bare fractions and are bound to the palette here, the one place holding
        // both. Neither is resolved from scratch: the cursor is not read (the panel's own state says which
        // tab is hovered, tested against the placement it was drawn at) and no timing is held (a click is an
        // event, and its decay belongs with whatever saw it).
        var looks = TabLookSource.createHoverFadedLookSource(
            tabStyle.palette(),
            spec.selectedIndex(),
            tabInteractions.hoverSource());

        var washes = TabWashSource.createClickPulsedWashSource(
            tabStyle.palette(),
            tabInteractions.pulseSource());

        // The hover fraction is spent twice, on the two halves of one rule: a palette answering the pointer
        // with a shade blends the look and lights nothing, and one answering with a glow leaves the look
        // alone and lights the finished tab. Which of the two happens is the palette's, so both are wired
        // here whatever chrome the row wears.
        var lights = TabLightSource.createHoverLitSource(
            tabStyle.palette(),
            tabInteractions.hoverSource());

        TabChromeRenderer.resolveRendererFor(tabStyle.chrome())
            .renderTabs(
                tabs,
                new TabPaintSources(looks, washes, lights),
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
            paint.cellPaints(),
            new UiElementPaint(style.accentColours().base(), paint.opacity()),
            new UiElementPaint(style.accentColours().bright(), paint.opacity()));

        ControlLabelRenderer.drawBodyLabelRuns(
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
                paint.cellPaints(),
                paint.opacity());
        } else {
            RadioRowRenderer.renderHorizontalRow(
                bounds,
                segments,
                selectedIndex,
                colours,
                paint.cellPaints(),
                paint.opacity());
        }
        for (var index = 0; index < segments.size() && index < labels.size(); index++) {
            var segment = segments.get(index);
            ControlLabelRenderer.drawBodyLabel(
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

            ControlLabelRenderer.drawBodyLabel(
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
            paint.cellPaints(),
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

            ControlLabelRenderer.drawBodyLabelRuns(
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
    // (a sort selector's ascending/descending), or the right-aligned value a ranked row shows - as one
    // run, or as several where the value is picked out in shades of its own. All right-align to the same
    // inset the layout reserved, so a triangle column and a value column occupy the same right-hand
    // strip. A row trailing with nothing draws nothing here.
    //
    // One painter per row, since what it binds - the row's own segment and the control's paint - is
    // settled for the whole row.
    private static void drawTrailingRowSlot(
            RowSlot trailingRowSlot,
            Rectangle segment,
            ControlPaint paint) {

        trailingRowSlot.paintSlot(new TrailingSlotPainter(segment, paint));
    }

    /**
     * What each kind of slot looks like in a control row's trailing column, bound to the one row.
     *
     * <p>Three of the six draw nothing, and say so: a control row trails no crest and no tick box, and
     * an unfilled column is the column the rows around it reserved. Stated rather than omitted, so a
     * kind added to the set breaks this surface rather than joining the ones it silently skips.
     *
     * @param segment the row's own box, which every anchor here is taken from
     * @param paint   the look and alpha the control draws with
     */
    private record TrailingSlotPainter(
        Rectangle segment,
        ControlPaint paint) implements RowSlotPainter {

        @Override
        public void paintEmptySlot() {
        }

        @Override
        public void paintImageSlot(RowSlot.Image imageSlot) {
        }

        // Finishing at the same inset a one-run value finishes at, so a row whose value is picked out in
        // two shades lines its column up with every plainer row above it. Each run is drawn on its own
        // for the reason a label's runs are: batched into one string, a run's colour would be flattened
        // to the first one's, which is the whole of what a second run buys a caller.
        @Override
        public void paintTextRunsSlot(RowSlot.TextRuns textRunsSlot) {

            ControlLabelRenderer.drawBodyLabelRunsEndingAt(
                paint,
                List.<LabelRun>copyOf(textRunsSlot.textSpans()),
                IconLabelRow.computeTrailingAnchorX(segment),
                segment.computeCenterY());
        }

        // Drawn at the body size - the same size the layout reserved the column at - and in the run's own
        // colour, so a value the row picked out reads as picked out here too. A run that came out blank
        // draws nothing rather than anchoring an empty string.
        @Override
        public void paintTextSlot(RowSlot.Text textSlot) {

            if (!textSlot.textSpan().hasContent()) {
                return;
            }
            ControlLabelRenderer.drawBodySpan(
                paint,
                textSlot.textSpan(),
                IconLabelRow.computeTrailingAnchorX(segment),
                segment.computeCenterY(),
                LazyFont.TextAnchor.CENTER_RIGHT);
        }

        @Override
        public void paintTickSlot(RowSlot.Tick tickSlot) {
        }

        // In the row's body text tone so it reads as a quiet annotation like the value it replaces.
        @Override
        public void paintTriangleSlot(RowSlot.Triangle triangleSlot) {

            var trianglePaint = new UiElementPaint(
                StarsectorUiColour.VANILLA_TEXT.resolve(),
                paint.opacity());

            TriangleRenderer.render(
                IconLabelRow.computeDirectionTriangleBox(segment),
                triangleSlot.triangleDirection(),
                trianglePaint);
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
            paint.cellPaints(),
            paint.opacity());

        ControlLabelRenderer.drawCentredBodyLabelRuns(
            paint,
            spec.labelRuns(),
            bounds.computeCenterX(),
            bounds.computeCenterY());
    }

    // A divider row: a single hairline centred across the row in the accent, parting one run of controls
    // from the next - it heads a section like a caption but carries no text and no hit target.
    private static void drawDivider(Control control, ControlPaint paint) {

        DividerRenderer.render(
            control.bounds(),
            paint.style().accentColours().base(),
            paint.opacity());
    }

    // A caption row: only its text, left-aligned at the row's left edge and vertically centred, with no
    // widget chrome - it heads the controls below it and is never clicked.
    private static void drawLabelRow(Control control, ControlPaint paint) {

        var spec = (ControlSpec.Label) control.spec();
        var bounds = control.bounds();

        ControlLabelRenderer.drawBodyLabelRuns(
            paint,
            spec.labelRuns(),
            bounds.x(),
            bounds.computeCenterY());
    }

}
