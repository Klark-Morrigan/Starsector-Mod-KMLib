package kmlib.starsector.ui.font;

/**
 * A text face - the atlas a run of text draws in and the size it draws at. The two always travel
 * together (a glyph run is a face at a size), so bundling them lets a look bundle or a paint pass carry
 * the face as one value rather than threading the font-and-size pair through every style record, hop,
 * and cache key. The colour and opacity a run fades by are not part of the face - they vary per draw
 * over one cached glyph run - so they stay separate.
 *
 * @param font the atlas the text draws in
 * @param size the size the text draws at, which need not be the atlas's native size
 */
public record TextFace(
    StarsectorFont font,
    double size) {

    /**
     * Builds {@code font} at the one size its installed atlas draws 1:1 at - what a caller with no size
     * of its own wants, a bitmap atlas being crisp at exactly that size and resampled at any other.
     *
     * <p>The size is read rather than named, because it is the install's: the same basename carries a
     * different atlas on a localised install, and a size written down for vanilla's would draw that
     * atlas scaled.
     *
     * @param font        the atlas the text draws in
     * @param lineHeights where the installed atlas's line height is read from
     * @return the face at its native size
     */
    public static TextFace createNativeFace(StarsectorFont font, FaceLineHeightReader lineHeights) {
        return new TextFace(font, lineHeights.readLineHeight(font));
    }
}
