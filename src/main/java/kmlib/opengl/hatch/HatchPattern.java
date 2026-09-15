package kmlib.opengl.hatch;

/**
 * The family of parallel lines a region is cut with: how far apart they sit, which way they run,
 * and how near two of one line's crossings must be to count as the same stroke.
 *
 * <p>One value rather than three parameters because the three are decided together and travel
 * together - a caller reads them from wherever it keeps its settings, hands them down through
 * whatever builds its geometry, and they reach {@link Hatching} unchanged. Threaded as loose
 * doubles, two of them are the same type and adjacent, so a transposed pair is a hatch that
 * silently comes out at the wrong density with nothing to fail on.
 *
 * <p>It is also the whole of what a cut depends on, which is what lets a caller hold "the pattern"
 * apart from how the resulting segments are stroked: the pattern shapes geometry and is worth
 * caching against, while the stroke is a per-frame choice that changes no vertex.
 *
 * @param spacing               the perpendicular distance between adjacent lines, in the hatched
 *                              geometry's own units; a non-positive value describes no family and
 *                              so hatches nothing
 * @param angleRadians          the direction the lines run in; a line is undirected, so a half
 *                              turn describes the same family
 * @param joinToleranceFraction how far apart two of one line's crossings may sit and still count
 *                              as the same stroke, as a fraction of the spacing; zero merges only
 *                              crossings that coincide exactly
 */
public record HatchPattern(
    double spacing,
    double angleRadians,
    double joinToleranceFraction) {
}
