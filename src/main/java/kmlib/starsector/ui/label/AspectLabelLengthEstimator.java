package kmlib.starsector.ui.label;

import java.util.List;

/**
 * A {@link LabelLengthEstimator} with no real text behind it: it treats a label as a
 * rectangle a fixed multiple longer than it is tall, so the box fit can still size a
 * band where no font or text is available. A consumer's debug overlay can then still
 * show a plausible label footprint for such a region; no text draws there regardless.
 *
 * <p>A one-line label at a given line height is {@code aspect} times as long as it is
 * tall; splitting it across more lines gives each line about that fraction of the
 * characters, so the longest line - the length the box must clear - shrinks by the
 * line count.
 *
 * @param aspect a one-line label's length as a multiple of its line height
 */
public record AspectLabelLengthEstimator(double aspect) implements LabelLengthEstimator {

    @Override
    public double requiredLengthFor(double lineHeight, int lineCount) {
        return aspect * lineHeight / lineCount;
    }

    // A stand-in has no text to wrap, so no label is minted from it - only the sized
    // band shows, for a consumer that draws one.
    @Override
    public List<String> wrapIntoLines(int lineCount) {
        return List.of();
    }
}
