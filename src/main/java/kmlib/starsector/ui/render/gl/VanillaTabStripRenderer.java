package kmlib.starsector.ui.render.gl;

import kmlib.color.Colors;
import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.color.StarsectorUiColor;
import kmlib.starsector.ui.font.DrawableStringCache;
import kmlib.starsector.ui.font.TextFace;
import kmlib.starsector.ui.widgets.tabs.HotkeyStyle;
import kmlib.starsector.ui.widgets.tabs.TabStyle;
import kmlib.starsector.ui.widgets.tabs.VanillaTab;
import kmlib.starsector.ui.widgets.tabs.VanillaTabColors;
import kmlib.starsector.ui.widgets.tabs.VanillaTabContent;
import kmlib.starsector.ui.widgets.tabs.VanillaTabStrip;
import kmlib.text.KmlibStrings;

import org.lazywizard.lazylib.ui.LazyFont;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Raw-GL paint for a {@link VanillaTabStrip}: the sector-map Sector/System tab look - each tab a solid
 * state fill (a dark fill at rest, a bright fill when active), the hovered tab washed a little toward
 * white, a bright underline capping the active tab, hairline dividers, and each label drawn beside its
 * shortcut - the key alone in accent gold, its delimiters in the label colour, the way vanilla
 * highlights only the key, and a hairline under the key itself when the style's {@link HotkeyStyle} asks
 * for one - distinct from the underline capping the active tab, which spans the whole tab.
 * The tab geometry lives on the substrate-independent widget; this draws it. The seams between tabs are
 * the chrome every horizontal segmented control shares, so they come from
 * {@link HorizontalSegmentsRenderer} (as a radio row's do); the per-state fill, the hover wash, the
 * baseline, the underline, and the multi-colour label are this strip's own. Unlike the plain renderers it
 * draws the label text itself (off {@link DrawableStringCache}), since the
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
     * @param style         the strip's look; its palette (see {@link VanillaTabColors#mapTabs}), its
     *                      hotkey presentation, and its face are read here, its band height having been
     *                      spent laying the tabs out
     * @param opacity       overall alpha, 0..1, applied to every quad and both text colours
     */
    public static void render(
            List<VanillaTab> tabs,
            int selectedIndex,
            int hoveredIndex,
            TabStyle style,
            float opacity) {

        var colors = style.colors();
        var textFace = style.face();
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
                    style.hotkey(),
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
                ? Colors.blendRgbTowards(
                        baseFill,
                        StarsectorUiColor.WHITE.resolve(),
                        HOVER_WHITE_WASH)
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
    // inside the tab. The shortcut paints as three segments so only the key carries the hotkey colour
    // and the style's emphasis while its delimiters share the label colour, matching how vanilla
    // highlights the key alone inside its brackets. Every colour fades by opacity so the text tracks the
    // strip. Skipped silently when the font cannot load.
    private static void renderTabText(
            Rectangle bounds,
            VanillaTabContent content,
            boolean isSelected,
            boolean isHovered,
            VanillaTabColors colors,
            HotkeyStyle hotkeyStyle,
            TextFace textFace,
            float opacity) {

        var label = DrawableStringCache.resolveRun(textFace, content.label());
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

        // The delimiters take the label's state colour; only the key takes the styled hotkey colour.
        var shortcut = KmlibStrings.hasText(content.shortcut())
                ? resolveShortcutSegments(
                        textFace,
                        content.shortcut(),
                        fadedLabelColor,
                        Colors.scaleAlpha(hotkeyStyle.keyColor(), opacity))
                : null;

        var labelWidth = label.getWidth();
        var gap = shortcut == null ? 0f : SHORTCUT_GAP;
        var shortcutWidth = shortcut == null ? 0f : shortcut.computeTotalWidth();
        var startX = bounds.x() + (bounds.width() - (labelWidth + gap + shortcutWidth)) / 2f;
        var centerY = bounds.computeCenterY();

        label.setAnchor(LazyFont.TextAnchor.CENTER_LEFT);
        label.draw(startX, centerY);

        if (shortcut != null) {
            drawShortcut(
                    shortcut,
                    startX + labelWidth + gap,
                    centerY,
                    hotkeyStyle,
                    opacity);
        }
    }

    // Resolves the shortcut's three drawables - open delimiter, key, close delimiter - each set to
    // its colour so the key alone lights gold while its delimiters read as label text. The delimiters
    // come from VanillaTabStrip, the same source the measured display string delimits with, so the
    // paint pass and the layout snap cannot drift. Null when any segment's font cannot load, so the
    // tab falls back to a bare label rather than a half-drawn shortcut.
    private static ShortcutSegments resolveShortcutSegments(
            TextFace textFace,
            String shortcut,
            Color delimiterColor,
            Color keyColor) {

        var open = DrawableStringCache.resolveRun(textFace, VanillaTabStrip.SHORTCUT_OPEN_DELIMITER);
        var key = DrawableStringCache.resolveRun(textFace, shortcut);
        var close = DrawableStringCache.resolveRun(textFace, VanillaTabStrip.SHORTCUT_CLOSE_DELIMITER);

        if (open == null || key == null || close == null) {
            return null;
        }

        open.setBaseColor(delimiterColor);
        key.setBaseColor(keyColor);
        close.setBaseColor(delimiterColor);

        for (var segment : List.of(open, key, close)) {
            segment.setAnchor(LazyFont.TextAnchor.CENTER_LEFT);
        }
        return new ShortcutSegments(open, key, close);
    }

    // Draws the delimited shortcut left to right from startX, each piece advancing the cursor by its
    // own width so the three read as one continuous "[K]" run despite carrying two colours, and draws
    // the styled emphasis under the key alone - the piece vanilla underlines, which is why the segments
    // are named rather than walked as a list.
    private static void drawShortcut(
            ShortcutSegments segments,
            float startX,
            float centerY,
            HotkeyStyle hotkeyStyle,
            float opacity) {

        var key = segments.key();
        var keyX = startX + segments.open().getWidth();

        segments.open().draw(startX, centerY);
        key.draw(keyX, centerY);
        segments.close().draw(keyX + key.getWidth(), centerY);

        if (!hotkeyStyle.isKeyUnderlined()) {
            return;
        }
        // The key's drawn box: the run is anchored centre-left, so it stands its own height about the
        // draw y. The style places the underline against that box, so every renderer drawing this look
        // puts it in the same spot. Faded by the strip's opacity like every other quad here.
        var keyBox = new Rectangle(
                keyX,
                centerY - key.getHeight() / 2f,
                key.getWidth(),
                key.getHeight());

        UiFill.renderQuad(
                hotkeyStyle.computeUnderlineBox(keyBox),
                new UiElementPaint(hotkeyStyle.keyColor(), opacity));
    }

    // The shortcut's three drawn pieces, named rather than positional so the paint pass can single the
    // key out - it alone takes the hotkey colour and carries the underline, while its delimiters are
    // label text.
    private record ShortcutSegments(
            DrawableString open,
            DrawableString key,
            DrawableString close) {

        // The run's full rendered width, so the shortcut centres against the label as if it were the one
        // delimited string the layout measured.
        private float computeTotalWidth() {
            return open.getWidth() + key.getWidth() + close.getWidth();
        }
    }
}
