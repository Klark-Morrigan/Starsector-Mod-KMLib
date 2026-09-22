package kmlib.starsector.ui.compatibility;

import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import kmlib.starsector.compatibility.CompatibilityFailure;
import kmlib.starsector.ui.colour.StarsectorUiColour;
import kmlib.starsector.ui.highlight.Highlight;
import kmlib.starsector.ui.highlight.HighlightedParagraph;
import kmlib.starsector.ui.layout.VanillaPositions;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiFill;
import kmlib.starsector.ui.screen.VanillaScreen;

/**
 * What the on-screen compatibility notice is made of: the two filled areas under it, and the
 * heading, rows and button drawn in it.
 *
 * <p>Drawn from the game's own text widgets rather than handed over as one string, which is what
 * the dialog beside this has to do. That buys the one thing a string cannot carry: each row's value
 * is tinted while its label is left alone, so both versions and the mod that lost something are
 * found by eye rather than read for. No font the game ships is monospaced, so the spacing in a row's
 * wording reads as a column only approximately either way - what tells the values apart here is
 * their colour.
 *
 * <p>The rows themselves are not composed here. Which rows a notice carries and what order they
 * come in is {@link CompatibilityFailure#describeRowsForPlayer()}'s, so the panel and the dialog
 * show the same notice; this decides only how one is painted.
 *
 * <p>Static: it draws into a element and a placement its caller holds, and keeps nothing of either.
 */
final class CompatibilityNoticeBody {

    // How dark the screen behind the notice goes. Enough to stand the map down without hiding it,
    // the notice being about the map the player is looking at.
    private static final float BACKDROP_ALPHA = 0.55f;

    // The box's own surface, opaque enough to read text over whatever the map drew underneath.
    private static final float BOX_ALPHA = 0.9f;

    // Between the heading and the first row, which are two readings rather than one run.
    private static final float HEADING_GAP = 10f;

    // Between rows, which are a list.
    private static final float ROW_GAP = 3f;

    // Above the button, which is not part of the list and should not read as another row.
    private static final float BUTTON_GAP = 14f;

    private static final float BUTTON_WIDTH = 90f;
    private static final float BUTTON_HEIGHT = 24f;

    private CompatibilityNoticeBody() {
    }

    /**
     * Draws the notice into {@code box}: the heading, one paragraph per row, and the button that
     * dismisses it.
     *
     * @param box             the element the notice is drawn in, sized by its caller
     * @param failure         what the notice is about
     * @param confirmButtonId what the button reports itself as when pressed
     * @param confirmText     the button's label, out of the library's own strings
     */
    static void fillNoticeBody(
            TooltipMakerAPI box,
            CompatibilityFailure failure,
            Object confirmButtonId,
            String confirmText) {

        var highlightColour = StarsectorUiColour.VANILLA_HIGHLIGHT_GOLD.resolve();

        // The third party is the one word in the heading a player is looking for, the rest of it
        // being the same two sentences under every notice.
        new HighlightedParagraph(
                failure.describeHeadingForPlayer(),
                new Highlight(failure.subject().name(), highlightColour))
            .addTo(box, 0f);

        var gapAboveRow = HEADING_GAP;

        for (var row : failure.describeRowsForPlayer()) {

            // Tinted on the value alone. The label says what the row is and every notice carries
            // the same ones; what differs between two notices is what fills them.
            new HighlightedParagraph(
                    row.fillRow(),
                    new Highlight(row.rowValue(), highlightColour))
                .addTo(box, gapAboveRow);

            gapAboveRow = ROW_GAP;
        }

        box.addButton(confirmText, confirmButtonId, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_GAP);
    }

    /**
     * Paints the two filled areas the game does not: the screen behind the notice, and the box the
     * notice is drawn on.
     *
     * <p>Under every widget, which is where the game draws the interiors of its own panels. The
     * game publishes a rectangle component that strokes and none that fills, so the surface a
     * notice is read against has to be painted rather than added.
     *
     * @param boxPlacement where the layout settled the box, read rather than recomputed so a
     *                     resized window moves the fill with it
     * @param alphaMult    the fade the panel is being drawn at
     */
    static void renderFills(PositionAPI boxPlacement, float alphaMult) {

        UiFill.renderQuad(
            VanillaScreen.resolveScreenBox(),
            new UiElementPaint(StarsectorUiColour.BLACK.resolve(), BACKDROP_ALPHA * alphaMult));

        if (boxPlacement != null) {

            UiFill.renderQuad(
                VanillaPositions.toRectangle(boxPlacement),
                new UiElementPaint(StarsectorUiColour.BLACK.resolve(), BOX_ALPHA * alphaMult));
        }
    }
}
