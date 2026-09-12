package kmlib.starsector.ui.font;

/**
 * The two line measurements a control strip is snapped by, one per face it letters. A strip is not
 * lettered in a single face: its tabs read in the tab face a look states, and every other control reads
 * in the body face, so a strip snapped through one measurement sizes half its rows against letters they
 * will never be drawn in - the row comes out wider or narrower than its text, and a panel framed to the
 * widest row inherits that error. Pairing the two here is what lets each row be measured in the face it
 * paints in.
 *
 * <p>The pair is built from the faces themselves rather than assembled from two loose measurements
 * ({@link #loadFaceMeasurers}), since the two members are the same type and nothing in a positional
 * hand-off would catch them arriving the wrong way round - which is the very fault this exists to
 * remove. The canonical constructor stays open for a caller supplying its own metrics.
 *
 * @param tabFaceMeasurer  measures a line in the face a tabs row letters in
 * @param bodyFaceMeasurer measures a line in the face every other control letters in
 */
public record StripTextMeasurers(
        LineWidthMeasurer tabFaceMeasurer,
        LineWidthMeasurer bodyFaceMeasurer) {

    /**
     * The pair for a look's two faces, or null when either atlas will not load - a strip snaps its rows
     * to measured text and cannot be laid out without both, so a caller draws nothing that frame rather
     * than laying a strip out against metrics it does not have.
     *
     * <p>The two faces are named by different types, so a caller cannot pass them the wrong way round.
     *
     * @param tabFace  the face a tabs row letters in, as its look states it
     * @param bodyFont the atlas every other control letters in
     * @return the pair bound to those faces, or null when either will not load
     */
    public static StripTextMeasurers loadFaceMeasurers(TextFace tabFace, StarsectorFont bodyFont) {

        var tabAtlas = LazyFontCache.loadByFace(tabFace.font());
        var bodyAtlas = LazyFontCache.loadByFace(bodyFont);

        if (tabAtlas == null || bodyAtlas == null) {
            return null;
        }
        return new StripTextMeasurers(
            new LazyFontMeasurer(tabAtlas),
            new LazyFontMeasurer(bodyAtlas));
    }
}
