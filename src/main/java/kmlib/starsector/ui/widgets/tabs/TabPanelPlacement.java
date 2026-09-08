package kmlib.starsector.ui.widgets.tabs;

import kmlib.math.geometry.Rectangle;
import kmlib.math.geometry.Rectangles;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.PanelPlacement;

import java.util.List;

/**
 * One laid-out tab panel: a headerless {@link PanelPlacement} for the {@code body} with a {@code
 * tabsHeader} control standing on top of the body's box. All geometry is in UI coordinates, so
 * the same placement a renderer draws is the one a consumer hit-tests, with no conversion. The {@code
 * tabsHeader} is an ordinary laid-out {@link kmlib.starsector.ui.controls.ControlSpec.Tabs} control (its
 * segments split per tab), so it measures, draws, and hit-tests through the generic control path like any
 * body control - the panel owns only where the header sits.
 *
 * <p>The body's {@link PanelPlacement#box()} frames the body alone: the tab row sits above it rather than
 * inside it, the way a strip of tabs sits on the panel it selects, so nothing of the body reaches behind the
 * row and a bodyless panel is its row and nothing else. The box's width tracks the body, so a tab row wider
 * than the body overhangs the frame rather than widening it. The panel's footprint is therefore the box
 * plus the drawn row (plus the handle) rather than the box alone - {@link #containsPoint} tests it piece
 * by piece, and {@link #computeOuterBound} encloses the same pieces in one rect.
 *
 * <p>The {@code bandButton} is the panel's own chrome standing after the last tab: a button that belongs to
 * the panel rather than to any tab, so pressing it fires its own action and never moves the selection. It is
 * laid out, drawn and hit-tested as a one-cell tabs control, which is what makes it read as part of the row
 * it stands in without being a tab in it - a segment inside the tabs control would shift every index the
 * selection, the lit tab and any bound keys are resolved by. Null when the host asks for none, and consumers
 * reading it must null-check.
 *
 * <p>The {@code drawnHeaderBand} is how much of the row is on screen: the whole row - tabs and the band
 * button together - at rest, and the part the fold has not yet wiped while the body is folding. One rect the
 * draw pass clips the row to and the containment test reads, so the panel cannot claim a strip of screen
 * where its tabs are no longer painted.
 *
 * <p>The {@code notch} is the collapse handle: a rect protruding past the box's right border edge, centred
 * on the frame. It rides that edge as the body collapses, so the render pass draws it and the input pass
 * hit-tests it against the one rect, and the handle tracks the shrinking edge to stay reachable when the
 * panel is docked. It is null for a bodyless panel: with no body to collapse the panel is not collapsible,
 * so it exposes no handle, and the render and input passes both skip it. Consumers reading {@code notch}
 * must null-check it.
 *
 * <p>The {@code border} is the frame the box was laid out around, carried on the placement so a renderer
 * strokes the width the layout actually reserved rather than reading it back from wherever the layout read
 * it. Those two reads agreeing is what keeps the stroke inside the box: a frame stroked wider than the inset
 * the layout spent would overlap the content it was supposed to sit outside. A host that strokes a different
 * set of edges than the layout reserved - one dropping an edge it now sits flush against - still composes
 * its own {@link BoxBorder}, since which edges are open is its decision and can only be made once the box
 * has a resolved position; the width is not.
 *
 * @param tabsHeader      the laid-out tabs control across the header band, its segments split per tab
 * @param bandButton      the panel's own one-cell button standing after the last tab, or null when the host
 *                        asks for none
 * @param drawnHeaderBand the part of that row currently on screen - the whole row at rest, narrowed to the
 *                        box's span while the fold wipes it
 * @param body            the headerless panel placement beneath the row (its box frames the body alone)
 * @param border          the frame the box was laid out around - the width every stroke of it must use
 * @param notch           the collapse-handle rect on the box's right border edge, centred on the frame, or
 *                        null when the panel has no body to collapse and so no handle
 */
public record TabPanelPlacement(
    Control tabsHeader,
    Control bandButton,
    Rectangle drawnHeaderBand,
    PanelPlacement body,
    BoxBorder border,
    Rectangle notch) {

    /**
     * Whether the point lands anywhere on the laid-out panel. The footprint is the drawn tab row
     * <em>plus</em> the body's box <em>plus</em> the notch, because all three are drawn outside one
     * another: the row stands above the box (and is the whole panel when there is no body), and the handle
     * is drawn past the box's right border edge and is all that remains on screen once the body is docked.
     * A box test alone would report a point on either as being off the panel entirely, and a panel that
     * does not own the screen it paints on neither blocks what is behind it nor suppresses what reads the
     * cursor over it.
     *
     * @param pointX the point's x in UI coordinates, the coordinates the placement is laid out in
     * @param pointY the point's y in UI coordinates
     * @return whether the point is on the drawn tab row, the body, or the collapse handle
     */
    public boolean containsPoint(float pointX, float pointY) {
        return Rectangles.findIndexContaining(listDrawnParts(), pointX, pointY) != Rectangles.NONE;
    }

    /**
     * The smallest rectangle enclosing everything the panel paints - the drawn tab row, the body's box
     * and the collapse handle, and the row alone when there is no body. The panel has no single box of its
     * own, since the three sit outside one another, so a pass that needs one region for the whole panel
     * rather than a test per piece composes it here: from the same pieces {@link #containsPoint} tests, so
     * what the panel draws, what it claims from the pointer, and what it bounds cannot drift apart.
     *
     * <p>The bound is a superset of the footprint, not the footprint itself: the corner beside a tab row
     * narrower than its body is inside the bound and outside every piece. That suits a clip or a
     * backdrop, which may cover screen the panel does not paint on; it does not suit a hit test, which
     * must not claim that corner - {@link #containsPoint} stays the answer there.
     *
     * @return the enclosing bound of the drawn row, the body and the handle, in UI coordinates
     */
    public Rectangle computeOuterBound() {

        var parts = listDrawnParts();

        // Seeded with the row rather than with an empty rect at the origin, which would drag the bound back
        // to a corner of the screen the panel is nowhere near.
        var bound = parts.get(0);
        for (var index = 1; index < parts.size(); index++) {
            bound = bound.unionWith(parts.get(index));
        }
        return bound;
    }

    /**
     * Whether the panel has a body under its tab row at all. A tab whose body is empty lays out no box and
     * no handle, so it is its row alone: the passes that frame, fold, and fill the body have nothing to act
     * on, and each asks this rather than reading the absence off the handle it happens to leave behind.
     *
     * @return whether there is a body beneath the tab row
     */
    public boolean hasBody() {
        return !body.bodyControls().isEmpty();
    }

    /**
     * Whether the point lands on the panel's band button. A panel asked for none absorbs the null the same
     * way the handle's test does: with no button to be over, no point is over it.
     *
     * <p>Geometry alone, like every other containment test here. Whether the row is being presented at all
     * is the panel's live fold, which this value knows nothing about - so a caller routing a press asks that
     * question beside this one, exactly as it does for a tab.
     *
     * @param pointX the point's x in UI coordinates
     * @param pointY the point's y in UI coordinates
     * @return whether the point is on the band button
     */
    public boolean containsPointInBandButton(float pointX, float pointY) {
        return bandButton != null && bandButton.bounds().containsPoint(pointX, pointY);
    }

    /**
     * Whether the point lands on the collapse handle. A bodyless panel has nothing to fold and so no
     * handle, which is the null the test absorbs: with no rect to be over, no point is over it.
     *
     * @param pointX the point's x in UI coordinates
     * @param pointY the point's y in UI coordinates
     * @return whether the point is on the collapse handle
     */
    public boolean containsPointInNotch(float pointX, float pointY) {
        return notch != null && notch.containsPoint(pointX, pointY);
    }

    // The rects the panel puts on screen, as one list: the drawn row, the body's box, and the handle when
    // there is one. Every question about where the panel is - is the pointer on it, what does it span -
    // is answered off this list, so a piece can only be added or dropped for all of them at once. The row
    // is always in it; a panel that painted nothing at all would not be laid out.
    //
    // A bodyless panel lists its row alone. The empty box such a panel carries stands for the absence of a
    // body rather than for a place, and is anchored at the panel's outer edge while the row starts inside
    // the border - so a list that took it in would claim, and bound, a border's width of screen to the
    // left of everything the panel paints.
    private List<Rectangle> listDrawnParts() {

        if (!hasBody()) {
            return List.of(drawnHeaderBand);
        }
        return notch == null
            ? List.of(drawnHeaderBand, body.box())
            : List.of(drawnHeaderBand, body.box(), notch);
    }
}
