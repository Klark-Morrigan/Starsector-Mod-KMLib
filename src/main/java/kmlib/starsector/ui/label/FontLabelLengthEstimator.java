package kmlib.starsector.ui.label;

import kmlib.starsector.ui.font.LineWidthMeasurer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link LabelLengthEstimator} that measures one concrete label string with the label
 * font's own glyph metrics (through a {@link LineWidthMeasurer}), so the box fit sizes
 * exactly the block the glyphs will fill - kerning, wide capitals, and all - instead of
 * a guessed rectangle.
 *
 * <p>For each line count the text is wrapped on word boundaries into exactly that many
 * lines, choosing the split whose widest line is narrowest (a balanced block, the
 * shape that reads centred); that widest width is what the fit must clear. Text with
 * fewer words than lines cannot fill them, so that count reports an infinite required
 * length - the fit skips it, and the lower count (which the same words do fill)
 * already covers the shorter block. Widths are measured once at a reference size and
 * scaled: glyph advances are linear in the font size, so one measurement is exact at
 * any line height.
 *
 * <p>One instance per label: the wrap for each line count is computed on first use and
 * kept, since the fit asks the same question at every bisection step of every
 * candidate placement.
 */
public final class FontLabelLengthEstimator implements LabelLengthEstimator {

    // The size widths are measured at before scaling to the asked line height. Any
    // positive value gives the same result (glyph width is linear in size); a round
    // mid-range size keeps the intermediate floats well away from denormals.
    private static final double REFERENCE_FONT_SIZE = 100.0;

    private final LineWidthMeasurer measurer;
    private final String[] words;
    // The resolved wrap per line count; null marks a count the text cannot fill, so
    // the miss is remembered too and never re-derived.
    private final Map<Integer, LineWrap> wrapByLineCount = new HashMap<>();

    public FontLabelLengthEstimator(LineWidthMeasurer measurer, String text) {
        this.measurer = measurer;
        this.words = text.trim().split("\\s+");
    }

    @Override
    public double requiredLengthFor(double lineHeight, int lineCount) {
        var wrap = resolveWrapFor(lineCount);
        return wrap == null ? Double.POSITIVE_INFINITY
                : wrap.widestWidthPerUnitHeight() * lineHeight;
    }

    @Override
    public List<String> wrapIntoLines(int lineCount) {
        var wrap = resolveWrapFor(lineCount);
        return wrap == null ? List.of() : wrap.lines();
    }

    // The memoised wrap for one line count, or null when the text has too few words to
    // fill that many lines. computeIfAbsent cannot cache a null, so the miss is stored
    // explicitly via containsKey.
    private LineWrap resolveWrapFor(int lineCount) {
        if (!wrapByLineCount.containsKey(lineCount)) {
            wrapByLineCount.put(lineCount, computeBalancedWrap(lineCount));
        }
        return wrapByLineCount.get(lineCount);
    }

    // Wraps the words into exactly lineCount lines minimising the widest line's
    // measured width, or null when there are not enough words. Exhaustive over the
    // word-boundary splits: label strings are a handful of words, so trying every
    // split is cheaper than being clever and is exact.
    private LineWrap computeBalancedWrap(int lineCount) {
        if (words.length < lineCount) {
            return null;
        }
        return findBestPartition(0, lineCount);
    }

    // The best split of words[startWord..] into linesLeft lines: tries every length
    // for the first line (leaving at least one word per remaining line) and keeps the
    // split whose widest line is narrowest.
    private LineWrap findBestPartition(int startWord, int linesLeft) {
        if (linesLeft == 1) {
            var line = joinWords(startWord, words.length);
            return new LineWrap(List.of(line), measureWidthPerUnitHeight(line));
        }
        LineWrap best = null;
        for (var endWord = startWord + 1; endWord <= words.length - (linesLeft - 1); endWord++) {
            var line = joinWords(startWord, endWord);
            var lineWidth = measureWidthPerUnitHeight(line);
            var rest = findBestPartition(endWord, linesLeft - 1);
            var candidate = prependLine(line, lineWidth, rest);
            if (best == null || candidate.widestWidthPerUnitHeight()
                    < best.widestWidthPerUnitHeight()) {
                best = candidate;
            }
        }
        return best;
    }

    // One line's width per unit of line height. The joined line is measured whole so
    // inter-word kerning is included, exactly as the glyphs will draw.
    private double measureWidthPerUnitHeight(String line) {
        return measurer.measureLineWidth(line, REFERENCE_FONT_SIZE) / REFERENCE_FONT_SIZE;
    }

    private String joinWords(int startWord, int endWord) {
        return String.join(" ", List.of(words).subList(startWord, endWord));
    }

    // A partial wrap extended by one line in front, its widest width the worse of the
    // new line and the rest.
    private static LineWrap prependLine(String line, double lineWidth, LineWrap rest) {
        var lines = new ArrayList<String>(rest.lines().size() + 1);
        lines.add(line);
        lines.addAll(rest.lines());
        return new LineWrap(List.copyOf(lines),
                Math.max(lineWidth, rest.widestWidthPerUnitHeight()));
    }

    /**
     * One resolved wrap: the lines top-first and the widest line's width per unit of
     * line height - the quantity {@link #requiredLengthFor} scales.
     */
    private record LineWrap(List<String> lines, double widestWidthPerUnitHeight) {
    }
}
