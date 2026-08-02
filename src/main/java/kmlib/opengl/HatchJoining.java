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
    PER_TRIANGLE
}
