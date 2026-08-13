package kmlib.starsector.ui.input;

/**
 * The {@link HoverArrival} of a row of like elements: whether the pointer has just reached one of them,
 * and it does not matter which was reached before. A lone element holds a {@link HoverArrival}; a row
 * holds one of these.
 *
 * <p>Crossing straight from one element to its neighbour is an arrival like any other - the pointer never
 * left the row, and on a row of abutting elements that is the common way to reach one. An arrival
 * detected only from off the row would answer the first element and then nothing else.
 *
 * <p>A frame steps it one of two ways, and which is the consumer's to decide: it detects an arrival, or -
 * on a frame where the row itself moved - it adopts what is now under the pointer without announcing it.
 * Both keep the latch tracking; only one reports a moment.
 *
 * <p>Singular where {@link HoverFades} is plural, and the difference is real rather than a naming slip.
 * A row runs a fade per element at once - the one rising while the others fall is what makes a row read
 * as one hover travelling across it - so that holds a set. Only one element of a row can be reached at a
 * time, so this holds one latch: which element the pointer is currently on, or none.
 *
 * @param <K> what tells one element of the row from another - an index for a row that keeps its order, an
 *            identity of its own for a set that can be rebuilt under the pointer
 */
public final class KeyedHoverArrival<K> {

    // Which element the pointer was last reported as reaching, or null for none of them. One latch rather
    // than one per key, only one element of a row being under the pointer at a time - so a key crossed to
    // simply replaces the key crossed from.
    private K arrivedKey;

    /**
     * Reports whether the pointer reached an element this frame, and records which one for the next.
     * Called once per frame with whichever key the hit-test found, so the latch tracks the pointer whether
     * or not the consumer acts on the answer.
     *
     * @param hoveredKey the element the pointer is on this frame, or null when it is on none of them
     * @return true on the frame the pointer arrives on any element, including one reached straight from
     *         its neighbour; false while it rests on one and while it is on none
     */
    public boolean detectArrivalAt(K hoveredKey) {

        var isArriving = hoveredKey != null && !hoveredKey.equals(arrivedKey);
        arrivedKey = hoveredKey;
        return isArriving;
    }

    /**
     * Takes the element the pointer is on as already reached, without reporting reaching it - the frame's
     * step for a row that moved under a still pointer rather than a pointer that moved over a still row.
     * Content sliding past a parked cursor was reached by nobody, so an element carried under it has to
     * become the latched one without the moment that would ordinarily go with it.
     *
     * <p>Adopting rather than skipping the frame is the whole of it. A latch left holding the element the
     * pointer was on before the row moved would report an arrival the moment the row settles, which is the
     * same false moment one frame later; a latch that stopped tracking would go deaf to the pointer
     * genuinely moving onto what is now under it.
     *
     * <p>Which movements count as the row moving under the pointer is the consumer's, this end knowing
     * nothing about what it is keyed over - it is told which frames to adopt on rather than working them
     * out.
     *
     * @param hoveredKey the element the pointer is on this frame, or null when it is on none of them
     */
    public void adoptArrivalAt(K hoveredKey) {
        arrivedKey = hoveredKey;
    }

    /**
     * Forgets which element was reached, for a consumer whose row stops showing - so the next session
     * announces an arrival on whatever the pointer is parked over rather than staying silent because that
     * element happens to be the one it was last on.
     */
    public void resetArrival() {
        arrivedKey = null;
    }
}
