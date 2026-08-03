package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.colour.StarsectorUiColour;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.RowGeometry;
import kmlib.starsector.ui.font.TextFace;
import kmlib.starsector.ui.input.UiCursor;
import kmlib.starsector.ui.layout.ControlStripLayout;
import kmlib.starsector.ui.widgets.Checkbox;
import kmlib.starsector.ui.widgets.IconLabelRow;
import kmlib.starsector.ui.widgets.LabelledRow;
import kmlib.starsector.ui.widgets.RowSlot;
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
 * <p>It composes the per-kind KMLib renderers ({@link CheckboxRenderer}, {@link RadioRowRenderer}, {@link
 * IconRadioListRenderer}, {@link ToggleButton}, {@link DividerRenderer}, {@link VanillaTabStripRenderer})
 * and the shared label paint, so a host renders a whole strip of controls by calling this per control. The
 * GL passthrough is exercised in-engine like the other draw helpers; the caller wraps it in the GL-state
 * save its panel already holds. A small cache mints one GL text buffer per (font, size, text) so a steady
 * body does not leak a buffer per frame.
 */
public final class ControlRenderer {
    private ControlRenderer() {
    }

    /**
     * Draws {@code control} in the lit state its spec carries, styled from {@code style} and faded by
     * {@code opacity}. A tick box / toggle lights its accent when its cell is selected; a radio frames its
     * segments in the accent and washes the active one; a checkbox uses the bright accent for its tick; a
     * tabs row draws the vanilla-styled strip in the style's tab colours and face, lighting the selected
     * tab and the one under the cursor. Must run with a current GL context, like any immediate-mode GL
     * call.
     *
     * @param control the laid-out control to draw
     * @param style   the look bundle - accents and body font for every kind, tab colours and face for a
     *                tabs row
     * @param opacity overall alpha, 0..1
     */
    public static void render(Control control, WidgetStyle style, float opacity) {
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
            drawTabs(control, paint);
        }
    }

    // A tabs row: the vanilla Sector/System strip, each tab drawn in its snapped segment with the
    // selected tab lit and the tab under the cursor washed. The segments were split to text by the
    // layout; pairing each with its content (rebuilt from the spec through the same helper the layout
    // measured with) yields the tabs the strip renderer paints, so the drawn tab matches the hit box.
    // Hover reads the cursor here so the tab under the pointer lights without an input event.
    private static void drawTabs(Control control, ControlPaint paint) {
        var spec = (ControlSpec.Tabs) control.spec();
        var contents = ControlStripLayout.buildTabContents(spec);
        var tabs = VanillaTabStrip.zipTabs(contents, control.segments());

        var hoveredIndex = VanillaTabStrip.findTabIndexAt(
            tabs,
            UiCursor.getUiX(),
            UiCursor.getUiY());

        // The same value the layout measured the band against, so a strip is drawn in exactly the look
        // it was laid out under.
        VanillaTabStripRenderer.render(
            tabs,
            spec.selectedIndex(),
            hoveredIndex,
            paint.style().tabStyle(),
            paint.opacity());
    }

    // A tick box lit when the spec's cell is selected, then its label to the right at the same gap the
    // layout reserved, so the label sits exactly in the space snapped for it.
    private static void drawCheckbox(Control control, ControlPaint paint) {
        var style = paint.style();
        var spec = (ControlSpec.Checkbox) control.spec();
        var bounds = control.bounds();

        CheckboxRenderer.render(
            bounds,
            spec.isLit(),
            new UiElementPaint(style.accent(), paint.opacity()),
            new UiElementPaint(style.brightAccent(), paint.opacity()));

        var box = Checkbox.computeTickBox(bounds);
        var labelX = box.x()
            + box.width()
            + ControlStripLayout.CHECKBOX_LABEL_GAP;

        drawBodyLabel(
            paint,
            spec.label(),
            labelX,
            bounds.computeCenterY(),
            LazyFont.TextAnchor.CENTER_LEFT);
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
        var accent = paint.style().accent();
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

        var accent = paint.style().accent();
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

            drawBodyLabel(
                paint,
                labelledRow.resolveLabelText(),
                labelX,
                segment.computeCenterY(),
                LazyFont.TextAnchor.CENTER_LEFT);

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
        if (trailingRowSlot instanceof RowSlot.Text text && text.textSpan().hasText()) {
            // Drawn at the body size - the same size the layout reserved the column at.
            drawBodyLabel(
                paint,
                text.textSpan().text(),
                IconLabelRow.computeTrailingAnchorX(segment),
                segment.computeCenterY(),
                LazyFont.TextAnchor.CENTER_RIGHT,
                ControlStripLayout.BODY_FONT_SIZE);
        }
    }

    // A single button washed when the spec's cell is lit, its label centred in it - the lit state is the
    // on/off signal, so the label carries no On/Off word.
    private static void drawToggle(Control control, ControlPaint paint) {
        var accent = paint.style().accent();
        var spec = (ControlSpec.Toggle) control.spec();
        var bounds = control.bounds();

        ToggleButton.render(
            bounds,
            spec.isLit(),
            accent,
            accent,
            paint.opacity());

        drawBodyLabel(
            paint,
            spec.label(),
            bounds.computeCenterX(),
            bounds.computeCenterY(),
            LazyFont.TextAnchor.CENTER);
    }

    // A divider row: a single hairline centred across the row in the accent, parting one run of controls
    // from the next - it heads a section like a caption but carries no text and no hit target.
    private static void drawDivider(Control control, ControlPaint paint) {
        DividerRenderer.render(control.bounds(), paint.style().accent(), paint.opacity());
    }

    // A caption row: only its text, left-aligned at the row's left edge and vertically centred, with no
    // widget chrome - it heads the controls below it and is never clicked.
    private static void drawLabelRow(Control control, ControlPaint paint) {
        var spec = (ControlSpec.Label) control.spec();
        var bounds = control.bounds();
        drawBodyLabel(
            paint,
            spec.text(),
            bounds.x(),
            bounds.computeCenterY(),
            LazyFont.TextAnchor.CENTER_LEFT);
    }

    // Draws one body label at the body font size (the common case), delegating to the explicit-size draw.
    private static void drawBodyLabel(
            ControlPaint paint,
            String text,
            float x,
            float y,
            LazyFont.TextAnchor anchor) {
        drawBodyLabel(
            paint,
            text,
            x,
            y,
            anchor,
            ControlStripLayout.BODY_FONT_SIZE);
    }

    // Draws one body label in the vanilla text colour, faded by opacity, at the given anchor and size,
    // through the shared label primitive so the control text and any other KM UI text share one cache.
    private static void drawBodyLabel(
            ControlPaint paint,
            String text,
            float x,
            float y,
            LazyFont.TextAnchor anchor,
            double fontSize) {
        var labelStyle = new LabelStyle(
            new TextFace(
                paint.style().bodyFont(),
                fontSize),
            StarsectorUiColour.VANILLA_TEXT.resolve(),
            paint.opacity());
        LabelRenderer.render(labelStyle, text, x, y, anchor);
    }

    // The look bundle plus the frame's alpha, threaded together through every draw so a helper takes one
    // paint rather than unpacking the accents, body font, and opacity into loose arguments each time.
    private record ControlPaint(WidgetStyle style, float opacity) {
    }
}
