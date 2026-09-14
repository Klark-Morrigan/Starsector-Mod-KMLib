package kmlib.starsector.ui.widgets.tabs;

import kmlib.text.KmlibStrings;

import java.util.ArrayList;
import java.util.List;

/**
 * How a tab says which key it answers to, following the engine's own rule: a key whose letter already
 * occurs in the label lights that letter where it stands, and only a key with nowhere to land is spelt
 * out after the label in brackets. So {@code Sector} bound to {@code S} lights its own first letter,
 * while the same tab bound to {@code F1} reads {@code Sector  [F1]}.
 *
 * <p>A multi-character key is always spelt out. Lighting one letter of a name would say the wrong thing
 * about a key that is not a letter at all, and there is no single glyph in the label to stand for it.
 *
 * <p>The decision is taken once and expressed as the tab's text broken into runs, so the pass that
 * measures a tab and the pass that paints it read the same answer. Measured off the concatenation of
 * those runs and painted off the runs themselves, a tab cannot be sized for one presentation and drawn
 * in the other - which is what a rule that ran twice, once per pass, would eventually do.
 *
 * <p>Substrate-independent and pure, like the rest of this package: text in, text out, with nothing
 * about fonts, colours, or pixels.
 */
public final class TabShortcutText {

    /**
     * The opening delimiter wrapped around a spelt-out key - a square bracket, the way vanilla brackets a
     * hotkey it could not light in place.
     */
    public static final String SHORTCUT_OPEN_DELIMITER = "[";

    /** The closing delimiter, the pair to {@link #SHORTCUT_OPEN_DELIMITER}. */
    public static final String SHORTCUT_CLOSE_DELIMITER = "]";

    // The clearance between the label and a spelt-out key, as text rather than as a pixel gap: it is
    // measured with the rest of the string and drawn with the rest of the runs, so the space a tab is
    // sized for is the space it is drawn with.
    private static final String SHORTCUT_GAP_TEXT = "  ";

    // The length of a key that can stand as one lit letter of the label. Anything longer is spelt out.
    private static final int SINGLE_GLYPH_KEY_LENGTH = 1;

    // What indexOf answers when the key's letter is nowhere in the label.
    private static final int NOT_IN_LABEL = -1;

    private TabShortcutText() {
    }

    /**
     * A tab's text broken into the runs it is painted as, in reading order: the label with its bound key
     * lit where it stands, or the label followed by the key spelt out in brackets, or - for a tab with no
     * binding - the label alone.
     *
     * <p>The lit letter is the label's own, so its case is the label's rather than the key name's: a tab
     * reading {@code Sector} bound to {@code s} still lights a capital S, because what is lit is a letter
     * of the name and not a copy of the key laid over it.
     *
     * @param content the tab's label and its bound key, if it has one
     * @return the tab's runs in reading order, never empty for a tab that has a label
     */
    public static List<TabTextRun> resolveRuns(VanillaTabContent content) {

        var label = content.label();
        var shortcut = content.shortcut();

        if (!KmlibStrings.hasText(shortcut)) {
            return buildLabelOnlyRuns(label);
        }
        var litIndex = resolveLitLetterIndex(label, shortcut);

        return litIndex == NOT_IN_LABEL
            ? buildSpeltOutRuns(label, shortcut)
            : buildLitLetterRuns(label, litIndex);
    }

    /**
     * The text a tab occupies, which is its runs read end to end. What the layout measures a tab against,
     * so a tab lighting a letter of its own name is sized for exactly that and gains none of the width a
     * spelt-out key would have taken.
     *
     * @param content the tab's label and its bound key, if it has one
     * @return the tab's whole text
     */
    public static String composeDisplayText(VanillaTabContent content) {

        var display = new StringBuilder();
        for (var run : resolveRuns(content)) {
            display.append(run.text());
        }
        return display.toString();
    }

    // Where in the label the bound key's letter stands, or NOT_IN_LABEL when it has nowhere to land -
    // either because the key is more than one glyph or because the name simply does not contain it.
    // Matched without regard to case, since a lit letter is the label's own and a key name is quoted in
    // whatever case the binding reports.
    private static int resolveLitLetterIndex(String label, String shortcut) {

        if (shortcut.length() != SINGLE_GLYPH_KEY_LENGTH) {
            return NOT_IN_LABEL;
        }
        return label
            .toLowerCase()
            .indexOf(shortcut.toLowerCase());
    }

    private static List<TabTextRun> buildLabelOnlyRuns(String label) {
        return buildRuns(new TabTextRun(label, TabTextRun.Role.LABEL));
    }

    // The label with one of its letters lit in place. The letter is taken out of the label rather than
    // out of the key, so the run holds the glyph the tab was already showing.
    private static List<TabTextRun> buildLitLetterRuns(String label, int litIndex) {
        return buildRuns(
            new TabTextRun(label.substring(0, litIndex), TabTextRun.Role.LABEL),
            new TabTextRun(label.substring(litIndex, litIndex + SINGLE_GLYPH_KEY_LENGTH),
                TabTextRun.Role.KEY),
            new TabTextRun(label.substring(litIndex + SINGLE_GLYPH_KEY_LENGTH), TabTextRun.Role.LABEL));
    }

    // The label followed by the key in brackets, the delimiters reading as label text either side of it.
    private static List<TabTextRun> buildSpeltOutRuns(String label, String shortcut) {
        return buildRuns(
            new TabTextRun(label, TabTextRun.Role.LABEL),
            new TabTextRun(SHORTCUT_GAP_TEXT, TabTextRun.Role.LABEL),
            new TabTextRun(SHORTCUT_OPEN_DELIMITER, TabTextRun.Role.LABEL),
            new TabTextRun(shortcut, TabTextRun.Role.KEY),
            new TabTextRun(SHORTCUT_CLOSE_DELIMITER, TabTextRun.Role.LABEL));
    }

    // Drops the runs that came out empty - a key lighting the label's first or last letter leaves nothing
    // on that side - so every run handed on is one there is something to draw.
    private static List<TabTextRun> buildRuns(TabTextRun... runs) {

        var kept = new ArrayList<TabTextRun>(runs.length);
        for (var run : runs) {
            if (!run.text().isEmpty()) {
                kept.add(run);
            }
        }
        return List.copyOf(kept);
    }
}
