package kmlib.math.geometry;

/**
 * A stretch of a {@link RingPath}, as the two arc lengths it opens and closes at.
 *
 * <p>The two ends are not interchangeable - one is where the stretch begins and the
 * other is where it ends, and a walk between them only runs forward - but as a pair of
 * bare distances they are, so a caller reading them back the wrong way round lays every
 * layout backwards and compiles cleanly. Named once here so that cannot be written, and
 * so what a path hands back says what it is rather than needing the prose beside it.
 *
 * <p>The end may sit past the perimeter. A stretch straddling the path's own start is
 * one stretch that happens to cross the origin, and stated as a start after its end it
 * would be neither walkable nor measurable; carried on into the next lap it is both, and
 * it is the form {@link RingPath#collectPointsBetween} already walks.
 *
 * @param startArcLength where the stretch opens, measured along the path from its start
 * @param endArcLength   where it closes, measured from that same start; past the
 *                       perimeter for a stretch that crosses it
 */
public record RingStretch(
    double startArcLength,
    double endArcLength) {

    /**
     * @return how far along the path the stretch reaches
     */
    public double computeLength() {
        return endArcLength - startArcLength;
    }
}
