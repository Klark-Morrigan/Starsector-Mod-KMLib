package kmlib.starsector.ui.render.gl;

import com.fs.starfarer.api.Global;

import kmlib.starsector.graphics.StarsectorSprites;
import kmlib.starsector.ui.font.LazyFontCache;
import kmlib.starsector.ui.font.LazyFontMeasurer;
import kmlib.starsector.ui.input.UiCursor;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.CursorTooltip;
import kmlib.starsector.ui.widgets.TooltipLayout;
import kmlib.starsector.ui.widgets.TooltipRow;

import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;
import java.util.List;
import java.util.function.Function;

/**
 * Draws a {@link CursorTooltip}'s rows as a free-floating box at the cursor: it resolves the body
 * face, lays the rows out through the widget, and paints the box, each crest, and each row's label and
 * value in one bracketed GL pass. The sanctioned way for a KM UI to show a tooltip on a core screen
 * that offers no panel to hang a vanilla {@code TooltipMakerAPI} on - the same rationale that puts the
 * panel renderers here beside it.
 *
 * <p>The pass reads the live cursor and screen size itself, so a caller supplies only the content rows
 * and the look: the placement that follows the pointer and clamps to the screen is the widget's job,
 * not the caller's. When the body face cannot load the whole box is skipped - a frame of chrome with
 * no text would only mislead. Must run with a current GL context, from an above-UI render pass, since
 * the box composites over the core UI and its tooltips.
 */
public final class CursorTooltipRenderer {
    private CursorTooltipRenderer() {
    }

    /**
     * Lays out {@code rows} at the live cursor and paints them in {@code style}. Skipped silently when
     * the face cannot load; an empty row list draws only the padded box.
     *
     * @param rows  the content rows, top to bottom
     * @param style the body font, size, opacity, and box chrome the whole tooltip draws in
     */
    public static void render(List<TooltipRow> rows, CursorTooltipStyle style) {
        var font = LazyFontCache.loadByFace(style.face().font());
        if (font == null) {
            return;
        }
        var settings = Global.getSettings();
        var layout = CursorTooltip.layOut(
                rows,
                style.face().size(),
                new LazyFontMeasurer(font),
                UiCursor.getUiX(),
                UiCursor.getUiY(),
                settings.getScreenWidth(),
                settings.getScreenHeight());
                
        // The core map and its tooltips draw after this pass, so the box, crests, and text run inside
        // the shared state save that restores the blend and colour the draw touched on the way out.
        GlStateGuard.bracket(() -> drawRows(rows, layout, style));
    }

    // Paints the frame then each row into the laid-out box: the box first, then per row its crest,
    // label, and value at the anchors the widget resolved, so the draw never re-derives the geometry.
    private static void drawRows(
            List<TooltipRow> rows,
            TooltipLayout layout,
            CursorTooltipStyle style) {

        BorderedBoxRenderer.render(
                layout.box(),
                new BoxBorder(style.borderWidth()),
                new UiElementPaint(style.fillColor(), style.opacity()),
                new UiElementPaint(style.borderColor(), style.opacity()));
        for (var index = 0; index < rows.size(); index++) {
            drawRow(
                    rows.get(index),
                    layout.rows().get(index),
                    style);
        }
    }

    // Draws one row's crest, label, trailing marker, and right-aligned value at its resolved anchors. A
    // row with no crest has a null crest box and skips the icon draw; a missing crest asset resolves to
    // null and is skipped the same way, so its label still reads. The marker and the value are drawn
    // unconditionally - an empty string paints nothing.
    private static void drawRow(
            TooltipRow row,
            TooltipLayout.TooltipRowLayout placement,
            CursorTooltipStyle style) {
        if (placement.crestBox() != null) {
            var crest = StarsectorSprites.loadSprite(row.crestSpritePath());
            if (crest != null) {
                UiSprite.renderQuad(
                        crest,
                        placement.crestBox(),
                        style.opacity());
            }
        }

        // Every span shares the row's face and opacity; only the colour differs, so the look is built
        // once here and each call supplies its own colour.
        Function<Color, LabelStyle> createStyleInColour = colour ->
                new LabelStyle(style.face(), colour, style.opacity());

        LabelRenderer.render(
                createStyleInColour.apply(row.textColor()),
                row.text(),
                placement.textX(),
                placement.textY(),
                LazyFont.TextAnchor.TOP_LEFT);

        LabelRenderer.render(
                createStyleInColour.apply(row.markerColor()),
                row.marker(),
                placement.markerX(),
                placement.markerY(),
                LazyFont.TextAnchor.TOP_LEFT);

        LabelRenderer.render(
                createStyleInColour.apply(row.valueColor()),
                row.value(),
                placement.valueX(),
                placement.valueY(),
                LazyFont.TextAnchor.TOP_RIGHT);
    }
}
