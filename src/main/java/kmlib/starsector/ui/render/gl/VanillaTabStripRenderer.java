package kmlib.starsector.ui.render.gl;

import kmlib.color.Colors;
import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.color.StarsectorUiColor;
import kmlib.starsector.ui.font.LazyFontCache;
import kmlib.starsector.ui.widgets.VanillaTab;
import kmlib.starsector.ui.widgets.VanillaTabContent;
import kmlib.starsector.ui.widgets.VanillaTabStrip;
import kmlib.text.KmlibStrings;

import org.lazywizard.lazylib.ui.LazyFont;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Raw-GL paint for a {@link VanillaTabStrip}: the sector-map Sector/System tab look - a black strip
 * with the active tab lit by a player-colour wash and underline, the hovered tab washed fainter,
 * hairline dividers, and each label drawn with its bracketed shortcut in accent gold. The tab
 * geometry lives on the substrate-independent widget; this draws it. Unlike the plain renderers it
 * draws the label text itself (through {@link LazyFontCache}), since the two-colour
 * label-plus-gold-shortcut is the whole point of the style.
 *
 * <p>Opacity scales every quad and both text colours by one value, so the whole strip fades as a
 * unit. The gold shortcut is a second drawable re-coloured per frame (rather than a baked
 * multi-colour run) precisely so it fades with the rest instead of staying opaque.
 */
public final class VanillaTabStripRenderer {
    // How much of the strip opacity each accent touch carries, so a lit tab reads as a highlight
    // over the black rather than a second opaque block.
    private static final float SELECTED_FILL_ALPHA_MULT = 0.30f;
    private static final float HOVER_FILL_ALPHA_MULT = 0.15f;
    private static final float DIVIDER_ALPHA_MULT = 0.40f;

    private static final float BASELINE_THICKNESS = 1f;
    private static final float UNDERLINE_THICKNESS = 2f;
    // Pixel gap drawn between the label and its bracketed shortcut, matching the two-space gap the
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
     * @param fontBasename  the {@code graphics/fonts} basename the labels draw in
     * @param fontSize      the label font size
     * @param opacity       overall alpha, 0..1, applied to every quad and both text colours
     */
    public static void render(List<VanillaTab> tabs, int selectedIndex, int hoveredIndex,
            VanillaTabColors colors, String fontBasename, double fontSize, float opacity) {
        for (var index = 0; index < tabs.size(); index++) {
            var tab = tabs.get(index);
            var isSelected = index == selectedIndex;
            var isHovered = index == hoveredIndex;
            renderChrome(tab.bounds(), index, isSelected, isHovered, colors, opacity);
            renderTabText(tab.bounds(), tab.content(), isSelected, isHovered, colors, fontBasename,
                    fontSize, opacity);
        }
    }

    // Black backdrop, an accent wash on the selected (or fainter, hovered) tab, a faint baseline
    // and left divider grounding the row, and a bright underline capping the active tab.
    private static void renderChrome(Rectangle bounds, int index,
            boolean isSelected, boolean isHovered, VanillaTabColors colors, float opacity) {
        UiFill.renderQuad(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                colors.backdrop(), opacity);
        if (isSelected) {
            UiFill.renderQuad(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    colors.accent(), opacity * SELECTED_FILL_ALPHA_MULT);
        } else if (isHovered) {
            UiFill.renderQuad(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    colors.accent(), opacity * HOVER_FILL_ALPHA_MULT);
        }
        UiFill.renderQuad(bounds.x(), bounds.y(), bounds.width(), BASELINE_THICKNESS,
                colors.accent(), opacity * DIVIDER_ALPHA_MULT);
        if (index > 0) {
            UiFill.renderQuad(bounds.x(), bounds.y(), BASELINE_THICKNESS, bounds.height(),
                    colors.accent(), opacity * DIVIDER_ALPHA_MULT);
        }
        if (isSelected) {
            UiFill.renderQuad(bounds.x(), bounds.y(), bounds.width(), UNDERLINE_THICKNESS,
                    colors.accent(), opacity);
        }
    }

    // Draws the label in its state colour and, when present, the bracketed shortcut in gold,
    // centred as one group inside the tab. Both colours fade by opacity so the text tracks the
    // strip. Skipped silently when the font cannot load.
    private static void renderTabText(Rectangle bounds,
            VanillaTabContent content, boolean isSelected, boolean isHovered,
            VanillaTabColors colors, String fontBasename, double fontSize, float opacity) {
        var label = resolveText(fontBasename, fontSize, content.label());
        if (label == null) {
            return;
        }
        var labelColor = isSelected ? colors.labelSelected()
                : isHovered ? colors.labelHovered() : colors.labelDefault();
        label.setBaseColor(Colors.scaleAlpha(labelColor, opacity));
        var shortcut = KmlibStrings.hasText(content.shortcut())
                ? resolveText(fontBasename, fontSize, VanillaTabStrip.bracketShortcut(content.shortcut()))
                : null;
        var labelWidth = label.getWidth();
        var gap = shortcut != null ? SHORTCUT_GAP : 0f;
        var shortcutWidth = shortcut != null ? shortcut.getWidth() : 0f;
        var startX = bounds.x() + (bounds.width() - (labelWidth + gap + shortcutWidth)) / 2f;
        var centerY = bounds.computeCenterY();
        label.setAnchor(LazyFont.TextAnchor.CENTER_LEFT);
        label.draw(startX, centerY);
        if (shortcut != null) {
            shortcut.setBaseColor(Colors.scaleAlpha(colors.shortcut(), opacity));
            shortcut.setAnchor(LazyFont.TextAnchor.CENTER_LEFT);
            shortcut.draw(startX + labelWidth + gap, centerY);
        }
    }

    // Mints a drawable once per (font, size, text) and reuses it; the base colour is re-set before
    // each draw, so one buffer serves every frame. Null when the font face cannot load, in which
    // case the tab draws its chrome without text.
    private static DrawableString resolveText(String fontBasename, double fontSize, String text) {
        var key = fontBasename + "|" + fontSize + "|" + text;
        var cached = TEXT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        var font = LazyFontCache.loadByBasename(fontBasename);
        if (font == null) {
            return null;
        }
        // The base colour is a throwaway - render re-sets it per frame before drawing - so a
        // null-safe palette literal serves; the live label/shortcut colours arrive at draw time.
        var drawable = font.createText(text, StarsectorUiColor.WHITE.resolve(), (float) fontSize);
        TEXT_CACHE.put(key, drawable);
        return drawable;
    }
}
