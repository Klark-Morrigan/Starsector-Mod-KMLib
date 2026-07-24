package kmlib.starsector.ui.render.gl;

import kmlib.color.Colors;
import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.color.StarsectorUiColor;
import kmlib.starsector.ui.font.LazyFontCache;
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
 * Raw-GL paint for a {@link VanillaTabStrip}: the sector-map Sector/System tab look - a black strip
 * with the active tab lit by a player-colour wash and underline, the hovered tab washed fainter,
 * hairline dividers, and each label drawn beside its shortcut - the key alone in accent gold, its
 * delimiters in the label colour, the way vanilla highlights only the key. The tab
 * geometry lives on the substrate-independent widget; this draws it. The wash on the selected tab and
 * the seams between tabs are the chrome every horizontal segmented control shares, so they come from
 * {@link HorizontalSegmentsRenderer} (as a radio row's do); the black backdrop, the hover wash, the
 * baseline, the underline, and the multi-colour label are this strip's own. Unlike the plain renderers it
 * draws the label text itself (through {@link LazyFontCache}), since the
 * label-with-a-gold-key-shortcut is the whole point of the style.
 *
 * <p>Opacity scales every quad and every text colour by one value, so the whole strip fades as a
 * unit. The shortcut is drawn as separate delimiter and key drawables re-coloured per frame (rather
 * than one baked multi-colour run) precisely so they fade with the rest instead of staying opaque.
 */
public final class VanillaTabStripRenderer {
    // How much of the strip opacity the hover wash carries, so a hovered tab lights fainter than the
    // selected one (whose wash strength is the shared HorizontalSegmentsRenderer.SELECTED_WASH_ALPHA_MULT).
    private static final float HOVER_FILL_ALPHA_MULT = 0.15f;

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
     * Paints the whole strip: each tab's backdrop and accents, the dividers, and each label with
     * its gold shortcut. The selected and hovered tabs light up; a {@code selectedIndex} or
     * {@code hoveredIndex} outside the row simply lights none.
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

    // Black backdrop, an accent wash on the selected (or fainter, hovered) tab, a faint baseline
    // grounding the row, and a bright underline capping the active tab. The inter-tab seams are drawn
    // once by the caller through the shared primitive, not here.
    private static void renderChrome(
            Rectangle bounds,
            boolean isSelected,
            boolean isHovered,
            VanillaTabColors colors,
            float opacity) {

        UiFill.renderQuad(
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height(),
                colors.backdrop(),
                opacity);

        if (isSelected) {
            HorizontalSegmentsRenderer.renderSelectedWash(
                    bounds,
                    colors.accent(),
                    opacity);
        } else if (isHovered) {
            UiFill.renderQuad(
                    bounds.x(),
                    bounds.y(),
                    bounds.width(),
                    bounds.height(),
                    colors.accent(),
                    opacity * HOVER_FILL_ALPHA_MULT);
        }

        UiFill.renderQuad(
                bounds.x(),
                bounds.y(),
                bounds.width(),
                BASELINE_THICKNESS,
                colors.accent(),
                opacity * HorizontalSegmentsRenderer.DIVIDER_ALPHA_MULT);

        if (isSelected) {
            UiFill.renderQuad(
                    bounds.x(),
                    bounds.y(),
                    bounds.width(),
                    UNDERLINE_THICKNESS,
                    colors.accent(),
                    opacity);
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
                ? colors.labelSelected()
                : isHovered
                        ? colors.labelHovered()
                        : colors.labelDefault();

        var fadedLabelColor = Colors.scaleAlpha(labelColor, opacity);
        label.setBaseColor(fadedLabelColor);

        // The delimiters take the label's state colour; only the key takes the gold accent.
        var shortcut = KmlibStrings.hasText(content.shortcut())
                ? resolveShortcutSegments(
                        textFace,
                        content.shortcut(),
                        fadedLabelColor,
                        Colors.scaleAlpha(colors.shortcut(),
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
    // read as a single continuous "(K)" run despite carrying two colours.
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
        var key = textFace.basename() + "|" + textFace.size() + "|" + text;
        var cached = TEXT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        var font = LazyFontCache.loadByBasename(textFace.basename());
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

    /**
     * A text face - the font basename and size a run of the strip's text draws in - bundled so the
     * paint pass threads one value from the caller down through its label and shortcut draws rather
     * than the basename-and-size pair at every hop.
     *
     * @param basename the {@code graphics/fonts} basename the text draws in
     * @param size     the size the text draws at
     */
    public record TextFace(String basename, double size) {
    }
}
