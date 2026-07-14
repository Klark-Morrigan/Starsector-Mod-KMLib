package kmlib.starsector.ui.render.gl;

import com.fs.starfarer.api.util.Misc;

import kmlib.color.Colors;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.font.LazyFontCache;
import kmlib.starsector.ui.input.UiCursor;
import kmlib.starsector.ui.layout.ControlStripLayout;
import kmlib.starsector.ui.widgets.Checkbox;
import kmlib.starsector.ui.widgets.IconLabelRow;
import kmlib.starsector.ui.widgets.VanillaTabStrip;
import kmlib.text.KmlibStrings;

import org.lazywizard.lazylib.ui.LazyFont;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Raw-GL paint for one laid-out {@link Control}: it draws the widget the control's {@link
 * kmlib.starsector.ui.controls.ControlKind} names - the tick box, the radio segments, the toggle, the
 * divider rule, the caption - in the lit state the control's spec carries, and draws the control's
 * label(s) over it in a body font. The kind names the widget; what the control means stays with whoever
 * built the spec, so this draws a faction toggle or a sort selector the same way without learning either.
 *
 * <p>It composes the per-kind KMLib renderers ({@link CheckboxRenderer}, {@link RadioRowRenderer}, {@link
 * IconRadioListRenderer}, {@link ToggleButton}, {@link DividerRenderer}, {@link VanillaTabStripRenderer})
 * and the shared label paint, so a host renders a whole strip of controls by calling this per control. The
 * GL passthrough is exercised in-engine like the other draw helpers; the caller wraps it in the GL-state
 * save its panel already holds. A small cache mints one GL text buffer per (font, size, text) so a steady
 * body does not leak a buffer per frame.
 */
public final class ControlRenderer {
    // Cached across the run: body labels are a handful of static strings, so one GL text buffer per
    // distinct (font, size, text) serves the whole run rather than leaking a buffer per frame. The base
    // colour is re-set before each draw, so one buffer serves every frame at any opacity.
    private static final Map<String, DrawableString> BODY_TEXT_CACHE = new HashMap<>();

    private ControlRenderer() {
    }

    /**
     * Draws {@code control} in the lit state its spec carries, styled from {@code style} and faded by
     * {@code opacity}. A tick box / toggle lights its accent when its cell is selected; a radio frames its
     * segments in the accent and washes the active one; a checkbox uses the bright accent for its tick; a
     * tabs row draws the vanilla-styled strip in the style's tab colours and font, lighting the selected
     * tab and the one under the cursor. Must run with a current GL context, like any immediate-mode GL
     * call.
     *
     * @param control the laid-out control to draw
     * @param style   the look bundle - accents and body font for every kind, tab colours and font for a
     *                tabs row
     * @param opacity overall alpha, 0..1
     */
    public static void render(Control control, WidgetStyle style, float opacity) {
        var accent = style.accent();
        var brightAccent = style.brightAccent();
        var bodyFont = style.bodyFont();
        switch (control.spec().kind()) {
            case CHECKBOX -> drawCheckbox(control, accent, brightAccent, bodyFont, opacity);
            case RADIO -> drawRadio(control, accent, bodyFont, opacity);
            case TOGGLE -> drawToggle(control, accent, bodyFont, opacity);
            case LABEL -> drawLabelRow(control, bodyFont, opacity);
            case DIVIDER -> drawDivider(control, accent, opacity);
            case TABS -> drawTabs(control, style, opacity);
        }
    }

    // A tabs row: the vanilla Sector/System strip, each tab drawn in its snapped segment with the
    // selected tab lit and the tab under the cursor washed. The segments were split to text by the
    // layout; pairing each with its content (rebuilt from the spec through the same helper the layout
    // measured with) yields the tabs the strip renderer paints, so the drawn tab matches the hit box.
    // Hover reads the cursor here so the tab under the pointer lights without an input event.
    private static void drawTabs(Control control, WidgetStyle style, float opacity) {
        var spec = control.spec();
        var contents = ControlStripLayout.buildTabContents(spec);
        var tabs = VanillaTabStrip.zipTabs(contents, control.segments());
        var hoveredIndex = VanillaTabStrip.findTabIndexAt(tabs, UiCursor.getUiX(), UiCursor.getUiY());
        var tabStyle = style.tabStyle();
        VanillaTabStripRenderer.render(tabs, spec.selectedIndex(), hoveredIndex, tabStyle.colors(),
                tabStyle.font(), tabStyle.fontSize(), opacity);
    }

    // A tick box lit when the spec's cell is selected, then its label to the right at the same gap the
    // layout reserved, so the label sits exactly in the space snapped for it.
    private static void drawCheckbox(Control control, Color accent, Color brightAccent, String bodyFont,
            float opacity) {
        var spec = control.spec();
        var bounds = control.bounds();
        CheckboxRenderer.render(bounds, isLit(spec), accent, brightAccent, opacity);
        var box = Checkbox.computeTickBox(bounds);
        var labelX = box.x() + box.width() + ControlStripLayout.CHECKBOX_LABEL_GAP;
        drawBodyLabel(bodyFont, spec.labels().get(0), labelX, bounds.computeCenterY(),
                LazyFont.TextAnchor.CENTER_LEFT, opacity);
    }

    // A radio group: the segments framed and the active one washed, then its labels. An icon-list radio
    // (non-empty icon paths) draws the crests and left-anchors its names past them; a plain radio centres
    // each name in its segment and appends its trailing caption. The segments flow the way the spec's
    // alignment sets, so the wash and dividers follow the same flow the layout split the row into.
    private static void drawRadio(Control control, Color accent, String bodyFont, float opacity) {
        if (!control.spec().iconPaths().isEmpty()) {
            drawIconRadio(control, accent, bodyFont, opacity);
            return;
        }
        var spec = control.spec();
        var bounds = control.bounds();
        var labels = spec.labels();
        RadioRowRenderer.render(bounds, labels.size(), spec.selectedIndex(), spec.alignment(),
                spec.columnCount(), accent, accent, opacity);
        var segments = control.segments();
        for (var index = 0; index < segments.size() && index < labels.size(); index++) {
            var segment = segments.get(index);
            drawBodyLabel(bodyFont, labels.get(index), segment.computeCenterX(), segment.computeCenterY(),
                    LazyFont.TextAnchor.CENTER, opacity);
        }
        if (KmlibStrings.hasText(spec.trailingLabel())) {
            var trailingX = bounds.x() + bounds.width() + ControlStripLayout.TRAILING_LABEL_GAP;
            drawBodyLabel(bodyFont, spec.trailingLabel(), trailingX, bounds.computeCenterY(),
                    LazyFont.TextAnchor.CENTER_LEFT, opacity);
        }
    }

    // An icon-radio table: a crest at the row's left, the name past it, and an optional ranking value
    // flush at the right edge. The list chrome and the icons are the widget's; the name and value draw
    // here at the same anchors the widget reserves, so an icon-less option reads as a plain name and a
    // value-less option shows only its name.
    private static void drawIconRadio(Control control, Color accent, String bodyFont, float opacity) {
        var spec = control.spec();
        var bounds = control.bounds();
        IconRadioListRenderer.render(bounds, spec.iconPaths(), spec.selectedIndex(),
                spec.columnCount(), accent, accent, opacity);
        var segments = control.segments();
        var labels = spec.labels();
        for (var index = 0; index < segments.size() && index < labels.size(); index++) {
            var segment = segments.get(index);
            // The label starts past the icon when the option carries one, or at the row's left inset when
            // it does not - the same has-icon rule the layout sized the row with.
            var labelX = IconLabelRow.computeLabelAnchorX(segment, spec.hasIconAt(index));
            drawBodyLabel(bodyFont, labels.get(index), labelX, segment.computeCenterY(),
                    LazyFont.TextAnchor.CENTER_LEFT, opacity);
            // The value right-aligns to the row's trailing inset the layout sized past the name. Drawn
            // only when the option carries one, so a value-less row is unchanged.
            var trailing = spec.trailingLabelAt(index);
            if (KmlibStrings.hasText(trailing)) {
                // Drawn at the spec's trailing size - the body size for a body-size value, a reduced size
                // for a compact direction letter - the same size the layout reserved the column at.
                drawBodyLabel(bodyFont, trailing, IconLabelRow.computeTrailingAnchorX(segment),
                        segment.computeCenterY(), LazyFont.TextAnchor.CENTER_RIGHT, opacity,
                        ControlStripLayout.BODY_FONT_SIZE * spec.trailingScale());
            }
        }
    }

    // A single button washed when the spec's cell is lit, its label centred in it - the lit state is the
    // on/off signal, so the label carries no On/Off word.
    private static void drawToggle(Control control, Color accent, String bodyFont, float opacity) {
        var spec = control.spec();
        var bounds = control.bounds();
        ToggleButton.render(bounds, isLit(spec), accent, accent, opacity);
        drawBodyLabel(bodyFont, spec.labels().get(0), bounds.computeCenterX(), bounds.computeCenterY(),
                LazyFont.TextAnchor.CENTER, opacity);
    }

    // A divider row: a single hairline centred across the row in the accent, parting one run of controls
    // from the next - it heads a section like a caption but carries no text and no hit target.
    private static void drawDivider(Control control, Color accent, float opacity) {
        DividerRenderer.render(control.bounds(), accent, opacity);
    }

    // A caption row: only its text, left-aligned at the row's left edge and vertically centred, with no
    // widget chrome - it heads the controls below it and is never clicked.
    private static void drawLabelRow(Control control, String bodyFont, float opacity) {
        var bounds = control.bounds();
        drawBodyLabel(bodyFont, control.spec().labels().get(0), bounds.x(), bounds.computeCenterY(),
                LazyFont.TextAnchor.CENTER_LEFT, opacity);
    }

    // A single-cell control (checkbox, toggle) is lit when its one cell (index 0) is the selected one;
    // NO_SELECTION means off.
    private static boolean isLit(ControlSpec spec) {
        return spec.selectedIndex() != ControlSpec.NO_SELECTION;
    }

    // Draws one body label at the body font size (the common case), delegating to the explicit-size draw.
    private static void drawBodyLabel(String bodyFont, String text, float x, float y,
            LazyFont.TextAnchor anchor, float opacity) {
        drawBodyLabel(bodyFont, text, x, y, anchor, opacity, ControlStripLayout.BODY_FONT_SIZE);
    }

    // Draws one body label in the vanilla text colour, faded by opacity, at the given anchor and size.
    // Skipped silently when the font cannot load, in which case the control draws its chrome without text.
    private static void drawBodyLabel(String bodyFont, String text, float x, float y,
            LazyFont.TextAnchor anchor, float opacity, double fontSize) {
        var drawable = resolveBodyText(bodyFont, text, fontSize);
        if (drawable == null) {
            return;
        }
        drawable.setAnchor(anchor);
        drawable.setBaseColor(Colors.scaleAlpha(Misc.getTextColor(), opacity));
        drawable.draw(x, y);
    }

    // Mints a body drawable once per (font, size, text) and reuses it; the base colour is re-set before
    // each draw, so one buffer serves every frame. Null when the font face cannot load, in which case the
    // control draws without that text.
    private static DrawableString resolveBodyText(String bodyFont, String text, double fontSize) {
        var key = bodyFont + "|" + fontSize + "|" + text;
        var cached = BODY_TEXT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        var font = LazyFontCache.loadByBasename(bodyFont);
        if (font == null) {
            return null;
        }
        var drawable = font.createText(text, Misc.getTextColor(), (float) fontSize);
        BODY_TEXT_CACHE.put(key, drawable);
        return drawable;
    }
}
