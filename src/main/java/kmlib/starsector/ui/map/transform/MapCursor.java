package kmlib.starsector.ui.map.transform;

import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector2f;

/**
 * Answers "what world point is the cursor over?" from inside the campaign map's render pass.
 *
 * <p>The three steps between a cursor and a world point - is the cursor even on the window,
 * snapshot the pass's transform, invert it - each have their own way of coming back with no
 * usable answer, and any of them means the same thing to a caller: it does not know where the
 * cursor is, so it must not act as if it does. Folding them into one call is what keeps that
 * from being re-derived, and re-derived slightly differently, by every overlay that hovers.
 *
 * <p>Reading the cursor rather than consuming input events is deliberate: the map keeps hovering
 * entities and drawing their tooltips exactly as it did, because nothing here takes an event away
 * from it. An overlay built on this is a passive second reader of a cursor the game is still free
 * to interpret its own way.
 *
 * <p>What the world point <em>means</em> - which of an overlay's shapes covers it, if any - is the
 * overlay's own question, and nothing here knows anything about one.
 */
public final class MapCursor {

    private MapCursor() {
    }

    /**
     * The world point under the cursor, for a caller inside the map's render pass.
     *
     * <p>Must be called from inside that pass, for {@link CampaignMapTransform#captureFromMapPass}'s
     * reason: the modelview describes the map only while the pass runs, so calling this anywhere
     * else resolves a point against whatever unrelated transform is in force.
     *
     * @param factor                the scale the same render pass applies per vertex, needed to
     *                              undo the map's zoom
     * @param modelviewMatrixReader the source of the modelview in force, from
     *                              {@link ModelviewMatrixReaders#selectForActiveRenderer}
     * @return the world point under the cursor, or {@code null} when there is no trustworthy
     *         answer - the cursor has left the window, the transform read back cannot be the
     *         map's, or the snapshot will not invert
     */
    public static Vector2f resolveWorldPointDuringMapPass(
            float factor,
            ModelviewMatrixReader modelviewMatrixReader) {

        // The cursor keeps reporting its last position after leaving the window, so without this a
        // caller would go on resolving a point it has not been over since.
        if (!Mouse.isInsideWindow()) {
            return null;
        }
        var transform = CampaignMapTransform.captureFromMapPass(
            factor,
            modelviewMatrixReader);
            
        if (transform == null) {
            return null;
        }
        return transform.unprojectToWorld(Mouse.getX(), Mouse.getY());
    }
}
