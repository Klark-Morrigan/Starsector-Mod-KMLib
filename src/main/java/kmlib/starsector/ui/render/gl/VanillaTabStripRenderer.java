package kmlib.starsector.ui.render.gl;

import kmlib.color.Colors;
import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.color.StarsectorUiColor;
import kmlib.starsector.ui.font.LazyFontCache;
import kmlib.starsector.ui.font.TextFace;
import kmlib.starsector.ui.widgets.tabs.VanillaTab;
import kmlib.starsector.ui.widgets.tabs.VanillaTabContent;
import kmlib.starsector.ui.widgets.tabs.VanillaTabStrip;
import kmlib.text.KmlibStrings;

import org.lazywizard.lazylib.ui.LazyFont;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Raw-GL paint for a {@link VanillaTabStrip}: the sector-map Sector/System tab look - each tab a solid
 * state fill (a dark fill at rest, a bright fill when active), the hovered tab washed a little toward
 * white, a bright underline capping the active tab, hairline dividers, and each label drawn beside its
 * shortcut - the key alone in accent gold, its delimiters in the label colour, the way vanilla
 * highlights only the key. The tab geometry lives on the substrate-independent widget; this draws it.
 * The seams between tabs are the chrome every horizontal segmented control shares, so they come from
 * {@link HorizontalSegmentsRenderer} (as a radio row's do); the per-state fill, the hover wash, the
 * baseline, the underline, and the multi-colour label are this strip's own. Unlike the plain renderers it
 * draws the label text itself (through {@link LazyFontCache}), since the
 * label-with-a-gold-key-shortcut is the whole point of the style.
 *
 * <p>Opacity scales every quad and every text colour by one value, so the whole strip fades as a
 * unit. The shortcut is drawn as separate delimiter and key drawables re-coloured per frame (rather
 * than one baked multi-colour run) precisely so they fade with the rest instead of staying opaque.
 */
public final class VanillaTabStripRenderer {
    // How far a hovered tab's fill washes toward white - a small constant lift, so a hovered tab reads
    // brighter than its resting state the way the vanilla map tabs do. Tunable against the real tabs
    // in-game. The click and hotkey pulses (added later) wash the same fill by a larger, animated
    // amount, so they layer on this same mechanism.
    private static final float HOVER_WHITE_WASH = 0.15f;

    private static final float BASELINE_THICKNESS = 1f;
    private static final float UNDERLINE_THICKNESS = 2f;
    // Pixel gap drawn between the label and its delimited shortcut, matching the two-space gap the
    // layout measured with.
    private static final float SHORTCUT_GAP = 6f;

    // The strip's labels are a handful of static strings, so one GL text buffer per distinct
    // (font, size, text) serves the whole run rather than leaking a buffer per frame. Shared
    // across instances since the strings and font rarely differ between callers.
    private static final Map<String, DrawableString> TEXT_CACHE = new HashMap<>();

    private VanillaTabStripRenderer() {
    }

    /**
     * Paints the whole strip: each tab's state fill and accents, the dividers, and each label with
     * its gold shortcut. The selected tab draws its bright fill and underline, a hovered tab washes
     * toward white; a {@code selectedIndex} or {@code hoveredIndex} outside the row simply lights none.
     *
     * @param tabs          the laid-out tabs, in row order
     * @param selectedIndex the active tab's index, or a value outside the row
     * @param hoveredIndex  the hovered tab's index, or a value outside the row
     * @param colors        the palette to paint with (see {@link VanillaTabColors#mapTabs})
     * @param textFace      the font basename and size the labels draw in
     * @param opacity       overall alpha, 0..1, applied to every quad and both text colours
     */
    public static void render(
            List<VanillaTab> tabs,
            int selectedIndex,
            int hoveredIndex,
            VanillaTabColors colors,
            TextFace textFace,
            float opacity) {
        for (var index = 0; index < tabs.size(); index++) {
            var tab = tabs.get(index);
            var isSelected = index == selectedIndex;
            var isHovered = index == hoveredIndex;
            renderChrome(tab.bounds(), isSelected, isHovered, colors, opacity);
            renderTabText(
                    tab.bounds(),
                    tab.content(),
                    isSelected,
                    isHovered,
                    colors,
                    textFace,
                    opacity);
        }
        // The seams between tabs, ruled once over the laid boxes through the shared segmented-row
        // primitive so this strip and a radio row divide their segments the same way. Drawn after the
        // per-tab chrome (a divider must sit over the backdrops it parts) and clear of the centred
        // labels, so the single pass reads identically to a per-tab rule.
        HorizontalSegmentsRenderer.renderSeamDividers(
                collectBounds(tabs),
                colors.accent(),
                opacity);
    }

    // The laid tab boxes, in row order, for the shared seam-divider pass.
    private static List<Rectangle> collectBounds(List<VanillaTab> tabs) {
        var bounds = new ArrayList<Rectangle>(tabs.size());
        for (var tab : tabs) {
            bounds.add(tab.bounds());
        }
        return bounds;
    }

    // The tab's solid state fill - the selected tab's bright fill or a resting tab's dark fill,
    // washed toward white while hovered - a faint baseline grounding the row, and a bright underline
    // capping the active tab. The fill IS the tab's surface: there is no black backdrop underneath, so
    // an unselected tab reads as its solid colour rather than that colour bled over black. The
    // inter-tab seams are drawn once by the caller through the shared primitive, not here.
    private static void renderChrome(
            Rectangle bounds,
            boolean isSelected,
            boolean isHovered,
            VanillaTabColors colors,
            float opacity) {

        var baseFill = isSelected ? colors.fillSelected() : colors.fillDefault();
        // A hovered tab lifts by washing its fill a small amount toward white; a resting tab draws its
        // bare state fill. The pulse states (click, hotkey) added later wash this same fill further.
        var fill = isHovered
                ? Colors.blendRgbTowards(baseFill, StarsectorUiColor.WHITE.resolve(), HOVER_WHITE_WASH)
                : baseFill;
        UiFill.renderQuad(bounds, new UiElementPaint(fill, opacity));

        UiFill.renderQuad(
                new Rectangle(
                        bounds.x(),
                        bounds.y(),
                        bounds.width(),
                        BASELINE_THICKNESS),
                new UiElementPaint(
                        colors.accent(),
                        opacity * HorizontalSegmentsRenderer.DIVIDER_ALPHA_MULT));

        if (isSelected) {
            UiFill.renderQuad(
                    new Rectangle(
                            bounds.x(),
                            bounds.y(),
                            bounds.width(),
                            UNDERLINE_THICKNESS),
                    new UiElementPaint(
                            colors.accent(),
                            opacity));
        }
    }

    // Draws the label in its state colour and, when present, the shortcut - centred as one group
    // inside the tab. The shortcut paints as three segments so only the key carries the accent gold
    // while its delimiters share the label colour, matching how vanilla highlights the key alone
    // inside its parentheses. Every colour fades by opacity so the text tracks the strip. Skipped
    // silently when the font cannot load.
    private static void renderTabText(
            Rectangle bounds,
            VanillaTabContent content,
            boolean isSelected,
            boolean isHovered,
            VanillaTabColors colors,
            TextFace textFace,
            float opacity) {

        var label = resolveText(textFace, content.label());
        if (label == null) {
            return;
        }
        var labelColor = isSelected
                ? colors.tabSelected()
                : isHovered
                        ? colors.tabHovered()
                        : colors.tabDefault();

        var fadedLabelColor = Colors.scaleAlpha(labelColor, opacity);
        label.setBaseColor(fadedLabelColor);

        // The delimiters take the label's state colour; only the key takes the gold accent.
        var shortcut = KmlibStrings.hasText(content.shortcut())
                ? resolveShortcutSegments(
                        textFace,
                        content.shortcut(),
                        fadedLabelColor,
                        Colors.scaleAlpha(colors.hotkey(),
                        opacity))
                : List.<DrawableString>of();

        var labelWidth = label.getWidth();
        var gap = shortcut.isEmpty() ? 0f : SHORTCUT_GAP;
        var shortcutWidth = sumSegmentWidths(shortcut);
        var startX = bounds.x() + (bounds.width() - (labelWidth + gap + shortcutWidth)) / 2f;
        var centerY = bounds.computeCenterY();

        label.setAnchor(LazyFont.TextAnchor.CENTER_LEFT);
        label.draw(startX, centerY);

        drawSegmentsInOrder(
                shortcut,
                startX + labelWidth + gap,
                centerY);
    }

    // Resolves the shortcut's three drawables - open delimiter, key, close delimiter - each set to
    // its colour so the key alone lights gold while its delimiters read as label text. The delimiters
    // come from VanillaTabStrip, the same source the measured display string delimits with, so the
    // paint pass and the layout snap cannot drift. Empty when any segment's font cannot load, so the
    // tab falls back to a bare label rather than a half-drawn shortcut.
    private static List<DrawableString> resolveShortcutSegments(
            TextFace textFace,
            String shortcut,
            Color delimiterColor,
            Color keyColor) {

        var open = resolveText(textFace, VanillaTabStrip.SHORTCUT_OPEN_DELIMITER);
        var key = resolveText(textFace, shortcut);
        var close = resolveText(textFace, VanillaTabStrip.SHORTCUT_CLOSE_DELIMITER);

        if (open == null || key == null || close == null) {
            return List.of();
        }

        open.setBaseColor(delimiterColor);
        key.setBaseColor(keyColor);
        close.setBaseColor(delimiterColor);

        var segments = List.of(open, key, close);
        for (var segment : segments) {
            segment.setAnchor(LazyFont.TextAnchor.CENTER_LEFT);
        }
        return segments;
    }

    // The summed rendered width of the shortcut segments, so the group centres against the label as
    // if it were the one delimited string the layout measured.
    private static float sumSegmentWidths(List<DrawableString> segments) {
        var total = 0f;
        for (var segment : segments) {
            total += segment.getWidth();
        }
        return total;
    }

    // Draws the segments left to right from startX, advancing the cursor by each one's width so they
    // read as a single continuous "[K]" run despite carrying two colours.
    private static void drawSegmentsInOrder(
            List<DrawableString> segments,
            float startX,
            float centerY) {

        var cursorX = startX;
        for (var segment : segments) {
            segment.draw(cursorX, centerY);
            cursorX += segment.getWidth();
        }
    }

    // Mints a drawable once per (font, size, text) and reuses it; the base colour is re-set before
    // each draw, so one buffer serves every frame. Null when the font face cannot load, in which
    // case the tab draws its chrome without text.
    private static DrawableString resolveText(TextFace textFace, String text) {
        var key = textFace.font().name() + "|" + textFace.size() + "|" + text;
        var cached = TEXT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        var font = LazyFontCache.loadByFace(textFace.font());
        if (font == null) {
            return null;
        }
        // The base colour is a throwaway - render re-sets it per frame before drawing - so a
        // null-safe palette literal serves; the live label/shortcut colours arrive at draw time.
        var drawable = font.createText(
                text,
                StarsectorUiColor.WHITE.resolve(),
                (float) textFace.size());

        TEXT_CACHE.put(key, drawable);
        return drawable;
    }
}
