package kmlib.opengl;

/**
 * How a hatch reports the several places one of its lines crosses a tessellated region: what
 * counts as one emitted primitive when a line enters and leaves the region's triangles many
 * times over.
 *
 * <p>An axis of the geometry rather than of the drawing. The choice is made where the run is
 * baked and decides the shape of the returned run, so a draw pass strokes the result without
 * being able to tell which was picked - and changing it costs a rebuild, not a frame.
 *
 * <p>Named as a value the caller passes rather than left implicit in the clip, so the walk that
 * finds the crossings and the rule that turns them into segments stay separable: the walk is
 * written once, and a joining only chooses what collects its output.
 */
public enum HatchJoining {

    /**
     * One segment per triangle crossed, so a line spanning several triangles comes back as a
     * chain of shorter segments touching end to end. The clip's own unit: each triangle is
     * walked once and answers only for itself, which is the least the walk can do and the
     * reference any other joining is measured against.
     */
    PER_TRIANGLE(false),

    /**
     * One segment per unbroken stretch of a line, so a line spanning several triangles comes back
     * whole and breaks only where it genuinely leaves the region. Crossings that abut - or that
     * fall within the join tolerance of abutting - are merged before anything is emitted.
     *
     * <p>What this buys is not fewer primitives but a stroke GL can rasterise as one: a wide
     * aliased line is stair-stepped along its major axis, and every primitive restarts that phase
     * from its own sub-pixel endpoint, so a chain of touching segments duplicates or drops a
     * pixel column at each join.
     */
    COALESCED(true);

    private final boolean isMerging;

    HatchJoining(boolean isMerging) {
        this.isMerging = isMerging;
    }

    /**
     * Whether this joining ever puts two of a line's crossings into one segment.
     *
     * <p>Answered here rather than by a caller testing for a particular constant, because it is
     * the same fact that decides whether the join tolerance is read at all and whether a
     * {@link HatchJoinTally} carries measurements or structural zeroes. A caller matching on the
     * constant would restate that fact, and a joining added beside these would leave the restated
     * copy wrong rather than incomplete.
     *
     * @return {@code true} when the joining merges crossings, so it reads a tolerance and its
     *         tally reports what it found
     */
    public boolean isMerging() {
        return isMerging;
    }
}
