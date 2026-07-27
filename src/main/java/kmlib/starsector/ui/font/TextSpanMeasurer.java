package kmlib.starsector.ui.font;

/**
 * Measures how wide one span of text renders in a given {@link TextFace} - the atlas and the size
 * together - so a layout whose lines do not all share one face can size each line in the face it will
 * actually be drawn in. The counterpart to {@link LineWidthMeasurer}, which binds one face for a whole
 * layout and so takes only the size: that is the right port for a single-face control, and the wrong one
 * as soon as a heading and a body line sit in the same box, because two atlases differ in width at the
 * same size and a line measured on the wrong one overflows or under-fills the box it sized.
 *
 * <p>A font-agnostic port for the same reason: layout code depends on this narrow measurement rather
 * than on a font loader, so it neither knows how a face is resolved nor what happens when one will not
 * load. {@link LazyFontSpanMeasurer} is the LazyLib-backed adapter.
 */
@FunctionalInterface
public interface TextSpanMeasurer {

    /**
     * The rendered width of {@code span} drawn in {@code face}, in the same units the face measures in.
     * The span is the text as it will be drawn, casing and all, since that is what the glyphs are looked
     * up for.
     *
     * @param face the atlas and size the span draws in
     * @param span the text to measure, already a single line
     * @return the span's rendered width
     */
    double measureSpanWidth(TextFace face, String span);
}
