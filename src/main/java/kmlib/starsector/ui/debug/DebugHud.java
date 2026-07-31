package kmlib.starsector.ui.debug;

import com.fs.starfarer.api.Global;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.font.LazyFontCache;
import kmlib.starsector.ui.font.LazyFontMeasurer;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.font.StarsectorFont;
import kmlib.starsector.ui.font.TextFace;
import kmlib.starsector.ui.render.gl.GlStateGuard;
import kmlib.starsector.ui.render.gl.LabelRenderer;
import kmlib.starsector.ui.render.gl.LabelStyle;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiFill;

import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A four-corner on-screen debug readout: any code pushes a keyed value to a corner, and one render
 * pass a frame draws them all and clears, so the next frame starts empty. This is the shared
 * "print a value onto the screen while chasing a bug" surface - a caller pushes what it wants seen
 * rather than log-diving, and the values land in a fixed spot per corner frame to frame.
 *
 * <p>Immediate mode by design: pushing accumulates for the current frame only, and {@link #render}
 * both draws and clears, so a value shown one frame and not pushed the next simply stops showing.
 * The push-then-render ordering is the caller's to honour - pushes must land before the frame's
 * render, which is why the render is wired last, into the pass composited over everything.
 *
 * <p>Single-threaded: pushed and rendered on the game's render thread, so the accumulator needs no
 * synchronisation.
 */
public final class DebugHud {

    // The body face both the key and value draw in, at the two sizes the layout sets, and the alpha
    // they draw at - readable over the map without fully hiding what is behind them.
    private static final StarsectorFont FONT = StarsectorFont.VANILLA_INSIGNIA_15;
    private static final float OPACITY = 0.9f;

    // A half-opaque black plate behind each line, padded a little past the glyphs, so bright debug
    // text stays legible over a busy map instead of vanishing into same-coloured terrain beneath it.
    private static final UiElementPaint BACKDROP_PAINT = new UiElementPaint(
        Color.BLACK,
        0.5f);

    private static final float BACKDROP_PADDING = 3f;

    // One shared readout the whole run pushes to, since a debug print has no owner to hang an
    // instance off and every caller wants the same on-screen surface.
    private static final DebugHud INSTANCE = new DebugHud();

    private final Map<DebugQuadrant, List<DebugHudEntry>> entriesByQuadrant =
        new EnumMap<>(DebugQuadrant.class);

    private DebugHud() {
    }

    /**
     * @return the shared readout every caller pushes to and the one render pass draws
     */
    public static DebugHud getInstance() {
        return INSTANCE;
    }

    /**
     * Adds one keyed value to a corner for this frame, beneath anything already pushed there.
     *
     * @param quadrant the corner to stack it into
     * @param key      the label, drawn small above the body
     * @param body     the value, drawn beneath the key
     */
    public void push(DebugQuadrant quadrant, String key, String body) {
        entriesByQuadrant
            .computeIfAbsent(quadrant, unused -> new ArrayList<>())
            .add(new DebugHudEntry(key, body));
    }

    /**
     * Draws every pushed entry pinned to its screen corner and clears them, so the frame's readout
     * does not carry into the next. Must run with a current GL context, in a pass composited over
     * what it annotates.
     *
     * @param edgePadding how far in from each screen edge a corner's block sits
     */
    public void renderAtCorners(float edgePadding) {
        var settings = Global.getSettings();
        var screenWidth = settings.getScreenWidth();
        var screenHeight = settings.getScreenHeight();
        drawAndClear(quadrant -> DebugHudLayout.layOutAtCorner(
            quadrant,
            entriesByQuadrant.get(quadrant),
            screenWidth,
            screenHeight,
            edgePadding));
    }

    /**
     * Draws every pushed entry fanned out around the cursor and clears them, so a reading sits
     * beside the pointer it annotates. Must run with a current GL context.
     *
     * @param cursorX the cursor x in UI coordinates
     * @param cursorY the cursor y in UI coordinates
     */
    public void renderAroundCursor(float cursorX, float cursorY) {
        drawAndClear(quadrant -> DebugHudLayout.layOutAroundCursor(
            quadrant,
            entriesByQuadrant.get(quadrant),
            cursorX,
            cursorY));
    }

    /**
     * Drops every pushed entry without drawing, so a stream switched off mid-frame leaves nothing
     * behind.
     */
    public void clear() {
        entriesByQuadrant.clear();
    }

    // Draws each non-empty corner's lines through the given layout, inside one GL state bracket, then
    // clears - the shared body of both render modes, which differ only in how a corner is laid out.
    // The face is resolved once so each line's backdrop can be sized to its glyphs; a face that fails
    // to load leaves the backdrop off (measurer null) and the text draw skips itself the same way.
    private void drawAndClear(Function<DebugQuadrant, List<DebugHudLine>> layout) {
        var face = LazyFontCache.loadByFace(FONT);
        var measurer = face == null ? null : new LazyFontMeasurer(face);
        GlStateGuard.bracket(() -> {
            for (var quadrant : DebugQuadrant.values()) {
                var entries = entriesByQuadrant.get(quadrant);
                if (entries == null || entries.isEmpty()) {
                    continue;
                }
                for (var line : layout.apply(quadrant)) {
                    drawLine(line, measurer);
                }
            }
        });
        clear();
    }

    // Draws one line's backdrop plate then its text. The plate goes first so the glyphs land on top
    // of it rather than behind it.
    private static void drawLine(DebugHudLine line, LineWidthMeasurer measurer) {
        drawBackdrop(line, measurer);
        // A left-half corner aligns its right edge to the anchor; a right-half corner its left. Only
        // the top row is used since the layout stacks by the line's top.
        var anchor = line.isRightAligned()
            ? LazyFont.TextAnchor.TOP_RIGHT
            : LazyFont.TextAnchor.TOP_LEFT;
        LabelRenderer.render(
            new LabelStyle(
                new TextFace(FONT, line.fontSize()),
                line.colour(),
                OPACITY),
            line.text(),
            line.x(),
            line.y(),
            anchor);
    }

    // Fills the half-opaque black plate behind one line, sized to the measured glyph run and padded a
    // little on every side. The line's y is its top and it stacks downward, so the plate drops a font
    // height below it; a right-aligned line's x is its right edge, so the plate extends left of it.
    // Skipped when the face could not be measured, leaving the text to draw over the map unbacked.
    private static void drawBackdrop(DebugHudLine line, LineWidthMeasurer measurer) {
        if (measurer == null) {
            return;
        }
        var width = (float) measurer.measureLineWidth(line.text(), line.fontSize());
        var height = (float) line.fontSize();
        var left = line.isRightAligned() ? line.x() - width : line.x();
        var quadBounds = new Rectangle(
            left - BACKDROP_PADDING,
            line.y() - height - BACKDROP_PADDING,
            width + 2f * BACKDROP_PADDING,
            height + 2f * BACKDROP_PADDING);

        UiFill.renderQuad(quadBounds, BACKDROP_PAINT);
    }
}
