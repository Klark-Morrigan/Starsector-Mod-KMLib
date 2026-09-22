package kmlib.starsector.ui.compatibility;

import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import kmlib.starsector.compatibility.CompatibilityFailure;
import kmlib.starsector.compatibility.CompatibilityNoticeLine;
import kmlib.starsector.ui.colour.StarsectorUiColour;
import kmlib.starsector.ui.highlight.Highlight;
import kmlib.starsector.ui.highlight.HighlightedParagraph;
import kmlib.starsector.ui.layout.VanillaPositions;
import kmlib.starsector.ui.render.gl.UiElementPaint;
import kmlib.starsector.ui.render.gl.UiFill;
import kmlib.starsector.ui.screen.VanillaScreen;

import java.awt.Color;

/**
 * What the on-screen compatibility notice is made of: the two filled areas under it, and the
 * heading, diagnosis, rows, closing line and button drawn in it.
 *
 * <p>Drawn from the game's own text widgets rather than handed over as one string, which is what
 * the dialog beside this has to do. That buys the one thing a string cannot carry: the runs the
 * notice names as standing out are tinted - names, versions and the log's own file brought forward,
 * what went wrong and what to do about it warned with - and everything else reads plain. No font the
 * game ships is monospaced, so what separates a row's answer from its label on screen is its colour.
 *
 * <p>Which colour each kind of emphasis takes is decided here and nowhere else. Which runs there
 * are is {@link CompatibilityFailure}'s, so the panel and the dialog show one notice.
 *
 * <p>Runs are handed over in the order the notice gives them, which the engine relies on: it
 * matches each from where the last one ended, so a name brought forward early and appearing again
 * inside a later phrase is tinted once for each rather than twice for the first.
 *
 * <p>Static: it draws into an element and a placement its caller holds, and keeps nothing of either.
 */
final class CompatibilityNoticeBody {

    // How dark the screen behind the notice goes. Enough to stand the map down without hiding it,
    // the notice being about the map the player is looking at.
    private static final float BACKDROP_ALPHA = 0.55f;

    // The box's own surface, opaque enough to read text over whatever the map drew underneath.
    private static final float BOX_ALPHA = 0.9f;

    // Between the paragraphs of the notice, which are separate readings.
    private static final float PARAGRAPH_GAP = 12f;

    // Between rows, which are a list.
    private static final float ROW_GAP = 3f;

    // Above the button, which is not part of the text and should not read as another line of it.
    private static final float BUTTON_GAP = 14f;

    private static final float BUTTON_WIDTH = 90f;
    private static final float BUTTON_HEIGHT = 24f;

    private CompatibilityNoticeBody() {
    }

    /**
     * Draws the notice into {@code box}: the heading, the diagnosis where there is one, one
     * paragraph per row, the closing line, and the button that dismisses it.
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

        addNoticeLine(box, failure.describeHeadingForPlayer(), 0f);

        // The lead of a diagnosis opens a paragraph; the cases under it are a list within it.
        var gapAboveDiagnosisLine = PARAGRAPH_GAP;

        for (var diagnosisLine : failure.describeDiagnosisForPlayer()) {
            addNoticeLine(box, diagnosisLine, gapAboveDiagnosisLine);
            gapAboveDiagnosisLine = ROW_GAP;
        }

        var gapAboveRow = PARAGRAPH_GAP;

        for (var row : failure.describeRowsForPlayer()) {
            addNoticeLine(box, row, gapAboveRow);
            gapAboveRow = ROW_GAP;
        }

        addNoticeLine(box, failure.describeClosingForPlayer(), PARAGRAPH_GAP);

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
    static void renderFills(PositionAPI boxPlacement, float boxPadding, float alphaMult) {

        UiFill.renderQuad(
            VanillaScreen.resolveScreenBox(),
            new UiElementPaint(StarsectorUiColour.BLACK.resolve(), BACKDROP_ALPHA * alphaMult));

        if (boxPlacement != null) {

            // Grown by the padding the text is inset by, so the fill reaches the frame around it
            // rather than stopping where the words do and leaving a ring of the map showing.
            UiFill.renderQuad(
                VanillaPositions.toRectangle(boxPlacement).computeInsetBox(-boxPadding),
                new UiElementPaint(StarsectorUiColour.BLACK.resolve(), BOX_ALPHA * alphaMult));
        }
    }

    // One line of the notice as a paragraph, its named runs tinted by kind.
    private static void addNoticeLine(TooltipMakerAPI box, CompatibilityNoticeLine line, float gapAbove) {

        var runs = line.emphasisedRuns();
        var highlights = new Highlight[runs.size()];

        for (var i = 0; i < highlights.length; i++) {
            var run = runs.get(i);

            highlights[i] = new Highlight(run.runText(), resolveEmphasisColour(run.emphasis()));
        }

        new HighlightedParagraph(line.lineText(), highlights).addTo(box, gapAbove);
    }

    // What each kind of emphasis is tinted. The one place a colour is put to a kind.
    private static Color resolveEmphasisColour(CompatibilityNoticeLine.Emphasis emphasis) {

        return switch (emphasis) {
            case WARNING -> StarsectorUiColour.VANILLA_HIGHLIGHT_RED.resolve();
            case REASSURANCE -> StarsectorUiColour.VANILLA_HIGHLIGHT_GREEN.resolve();
            case HIGHLIGHT -> StarsectorUiColour.VANILLA_HIGHLIGHT_GOLD.resolve();
        };
    }
}
