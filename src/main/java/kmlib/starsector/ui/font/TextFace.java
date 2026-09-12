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
}
