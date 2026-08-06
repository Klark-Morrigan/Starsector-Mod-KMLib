package kmlib.starsector.ui.render.gl;

import kmlib.colour.Colours;
import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.font.DrawableStringCache;
import kmlib.starsector.ui.font.TextFace;
import kmlib.starsector.ui.widgets.tabs.HotkeyStyle;
import kmlib.starsector.ui.widgets.tabs.TabLook;
import kmlib.starsector.ui.widgets.tabs.TabLookSource;
import kmlib.starsector.ui.widgets.tabs.TabPalette;
import kmlib.starsector.ui.widgets.tabs.TabShortcutText;
import kmlib.starsector.ui.widgets.tabs.TabStyle;
import kmlib.starsector.ui.widgets.tabs.TabTextRun;
import kmlib.starsector.ui.widgets.tabs.TabWashSource;
import kmlib.starsector.ui.widgets.tabs.VanillaTab;
import kmlib.starsector.ui.widgets.tabs.VanillaTabContent;
import kmlib.starsector.ui.widgets.tabs.VanillaTabStrip;

import org.lazywizard.lazylib.ui.LazyFont;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Raw-GL paint for a {@link VanillaTabStrip}: the sector-map Sector/System tab look - each tab a solid
 * fill (dark at rest, bright when active, travelling toward one shared shade under the pointer) lifted by
 * whatever pulse its wash source reports, a bright underline capping the active tab, hairline dividers,
 * and the tab's text with its bound key lit in the hotkey colour, carrying a hairline beneath it when the
 * style's {@link HotkeyStyle} asks for one - distinct from the underline capping the active tab, which
 * spans the whole tab. The tab geometry lives on the substrate-independent widget; this draws it. The
 * seams between tabs are the chrome every horizontal segmented control shares, so they come from
 * {@link HorizontalSegmentsRenderer} (as a radio row's do); the per-state fill, the wash, the
 * baseline, the underline, and the multi-colour text are this strip's own. Unlike the plain renderers it
 * draws the text itself (off {@link DrawableStringCache}), since a line carrying a lit key inside it is
 * the whole point of the style.
 *
 * <p>Where the key falls is not decided here. A tab lights a letter of its own label where the key has
 * one to land on and spells the key out after it otherwise, and that is
 * {@link kmlib.starsector.ui.widgets.tabs.TabShortcutText}'s call - the same one the layout measured the
 * tab against. This pass walks the runs it produced, colours each by its role, and advances by what it
 * drew, so it cannot draw a tab wider or narrower than the box it was given.
 *
 * <p>Opacity scales every quad and every text colour by one value, so the whole strip fades as a
 * unit. The text is drawn as separate per-run drawables re-coloured per frame (rather than one baked
 * multi-colour run) precisely so they fade with the rest instead of staying opaque.
 */
public final class VanillaTabStripRenderer {

    private static final float BASELINE_THICKNESS = 1f;
    private static final float UNDERLINE_THICKNESS = 2f;

    private VanillaTabStripRenderer() {
    }

    /**
     * Paints the whole strip: each tab's look, the dividers, and each label with its gold shortcut. Each
     * tab's look arrives resolved - already blended however far onto the hovered shade its fade has run -
     * and is lifted by the pulse its wash source reports for it; selection is read separately, so the tab
     * the panel is showing keeps its underline even while it wears the hovered shade. A {@code
     * selectedIndex} outside the row simply underlines none.
     *
     * @param tabs          the laid-out tabs, in row order
     * @param selectedIndex the active tab's index, or a value outside the row
     * @param looks         where each tab's settled look comes from - the hover fade is already blended
     *                      into it here, so this pass only paints it
     * @param washes        where each tab's resolved lift comes from - the interaction is already
     *                      composed into a wash here, so this pass only paints it
     * @param style         the strip's look; its palette's chrome accent (see
     *                      {@link TabPalette#createMapTabPalette}), its hotkey presentation, and its face
     *                      are read here, its band height having been spent laying the tabs out
     * @param opacity       overall alpha, 0..1, applied to every quad and both text colours
     */
    public static void render(
            List<VanillaTab> tabs,
            int selectedIndex,
            TabLookSource looks,
            TabWashSource washes,
            TabStyle style,
            float opacity) {

        var chromeAccent = style.palette().chromeAccent();
        var textFace = style.face();
        for (var index = 0; index < tabs.size(); index++) {

            var tab = tabs.get(index);
            var isSelected = index == selectedIndex;

            // The look as painted: the settled shade the tab has faded to, brightened by whatever pulse is
            // still running on it. A tab with none carries a wash that moves it nowhere, so no branch
            // here decides whether a lift applies.
            var look = looks.resolveLookAt(index).computeWashedLook(washes.resolveWashAt(index));

            renderChrome(tab.bounds(), isSelected, look, chromeAccent, opacity);
            renderTabText(
                tab.bounds(),
                tab.content(),
                look,
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
            chromeAccent,
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

    // The tab's solid fill as its look gives it, a faint baseline grounding the row, and a bright
    // underline capping the active tab. The fill IS the tab's surface: there is no black backdrop
    // underneath, so an unselected tab reads as its solid colour rather than that colour bled over black.
    // Selection is read here rather than off the look, because a hovered selected tab wears the hovered
    // shade and its underline is then the only thing marking it. The inter-tab seams are drawn once by
    // the caller through the shared primitive, not here.
    private static void renderChrome(
            Rectangle bounds,
            boolean isSelected,
            TabLook look,
            Color chromeAccent,
            float opacity) {

        UiFill.renderQuad(bounds, new UiElementPaint(look.fill(), opacity));
        UiFill.renderQuad(
            new Rectangle(
                bounds.x(),
                bounds.y(),
                bounds.width(),
                BASELINE_THICKNESS),
            new UiElementPaint(
                chromeAccent,
                opacity * HorizontalSegmentsRenderer.DIVIDER_ALPHA_MULT));

        if (isSelected) {
            UiFill.renderQuad(
                new Rectangle(
                    bounds.x(),
                    bounds.y(),
                    bounds.width(),
                    UNDERLINE_THICKNESS),
                new UiElementPaint(
                    chromeAccent,
                    opacity));
        }
    }

    // Draws the tab's text - the label with its bound key lit inside it, or spelt out after it - centred
    // as one group inside the tab. The runs and where the key falls among them are TabShortcutText's
    // decision, the same one the layout measured the tab against; this pass only colours each run and
    // advances the cursor by what it drew. Every colour fades by opacity so the text tracks the strip.
    // Skipped silently when any run's font cannot load, so a tab falls back to nothing rather than to a
    // half-drawn line.
    private static void renderTabText(
            Rectangle bounds,
            VanillaTabContent content,
            TabLook look,
            HotkeyStyle hotkeyStyle,
            TextFace textFace,
            float opacity) {

        var runs = resolveDrawnRuns(
            TabShortcutText.resolveRuns(content),
            textFace,
            Colours.scaleAlpha(look.label(), opacity),
            Colours.scaleAlpha(hotkeyStyle.keyColour(), opacity));

        if (runs == null) {
            return;
        }
        var centerY = bounds.computeCenterY();
        var runX = bounds.x() + (bounds.width() - computeRunsWidth(runs)) / 2f;

        for (var run : runs) {
            run.drawable().draw(runX, centerY);

            if (run.isKey() && hotkeyStyle.isKeyUnderlined()) {
                drawKeyUnderline(run.drawable(), runX, centerY, hotkeyStyle, opacity);
            }
            runX += run.drawable().getWidth();
        }
    }

    // Resolves each run to a drawable set to its role's colour and anchored for the left-to-right walk
    // above. Null when any run's font cannot load: the runs are one line of text broken up, so drawing
    // the pieces that did resolve would leave a tab reading as a fragment of its own name.
    private static List<DrawnRun> resolveDrawnRuns(
            List<TabTextRun> runs,
            TextFace textFace,
            Color labelColour,
            Color keyColour) {

        var drawn = new ArrayList<DrawnRun>(runs.size());
        for (var run : runs) {

            var drawable = DrawableStringCache.resolveRun(textFace, run.text());
            if (drawable == null) {
                return null;
            }
            var isKey = run.role() == TabTextRun.Role.KEY;

            drawable.setBaseColor(isKey ? keyColour : labelColour);
            drawable.setAnchor(LazyFont.TextAnchor.CENTER_LEFT);
            drawn.add(new DrawnRun(drawable, isKey));
        }
        return drawn;
    }

    // The whole line's rendered width, so the group centres in the tab as the one string the layout
    // measured rather than as pieces each centred on their own.
    private static float computeRunsWidth(List<DrawnRun> runs) {

        var width = 0f;
        for (var run : runs) {
            width += run.drawable().getWidth();
        }
        return width;
    }

    // The styled emphasis under the key alone. The run is anchored centre-left, so it stands its own
    // height about the draw y; the style places the line against that box, so every renderer drawing this
    // look puts it in the same spot. Faded by the strip's opacity like every other quad here.
    private static void drawKeyUnderline(
            DrawableString key,
            float keyX,
            float centerY,
            HotkeyStyle hotkeyStyle,
            float opacity) {

        var keyBox = new Rectangle(
            keyX,
            centerY - key.getHeight() / 2f,
            key.getWidth(),
            key.getHeight());

        UiFill.renderQuad(
            hotkeyStyle.computeUnderlineBox(keyBox),
            new UiElementPaint(hotkeyStyle.keyColour(), opacity));
    }

    // One resolved run: the drawable to paint and whether it is the bound key, which is all the draw
    // loop above still has to know once the colour has been set.
    private record DrawnRun(
        DrawableString drawable,
        boolean isKey) {
    }
}
