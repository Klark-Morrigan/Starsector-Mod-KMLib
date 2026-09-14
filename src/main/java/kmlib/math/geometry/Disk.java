package kmlib.math.geometry;

/**
 * A disk about a centre, carried together with the segment count it is approximated
 * at - the three facts a disk clip cannot be performed without.
 *
 * <p>The segment count belongs with the centre and radius rather than beside them,
 * because two clips of the same disk only agree if they approximate it identically:
 * a keep-out and the reach bound it was cut from land on the same chords when they
 * share a count, and leave a seam when they do not. Passing the three as one value
 * means a caller cannot vary one of them between two calls it meant to keep aligned.
 *
 * <p>The centre is taken as read rather than copied, matching how {x, y} points are
 * passed throughout this package; a caller that mutates the array it handed over
 * changes the disk with it. Note that this makes equality identity-based on the
 * centre array, as for any record holding an array.
 *
 * @param centre   the disk's centre as {x, y}
 * @param radius   distance from the centre to the approximating polygon's vertices;
 *                 a radius below {@link Limits#MIN_EDGE_LENGTH} encloses nothing, and
 *                 is legal so a caller can pass a computed radius without pre-checking
 * @param segments sides of the regular polygon approximating the disk; higher is
 *                 smoother, lower has fewer vertices
 */
public record Disk(
    double[] centre,
    double radius,
    int segments) {

    /**
     * @throws IllegalArgumentException when the centre is not an {x, y} pair, or when
     *                                  {@code segments} cannot enclose an area
     */
    public Disk {

        if (centre == null || centre.length < 2) {
            throw new IllegalArgumentException(
                "centre must be an {x, y} pair: "
                    + (centre == null ? "null" : "length "
                    + centre.length));
        }

        // Rejected at construction rather than at each clip, so a disk that exists is
        // always one the clips can walk the edges of.
        if (segments < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            throw new IllegalArgumentException(
                "segments must be at least " + Limits.MIN_VERTICES_TO_ENCLOSE_AREA
                    + " to approximate a disk: "
                    + segments);
        }
    }

    /**
     * @return the centre's x coordinate
     */
    public double centreX() {
        return centre[0];
    }

    /**
     * @return the centre's y coordinate
     */
    public double centreY() {
        return centre[1];
    }
}
