package kmlib.testfixtures.starsector.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A settings cell as LunaLib draws it: the text with its brackets removed, and the bracketed runs it then
 * asks the game to highlight.
 *
 * <p>LunaLib takes the text between the first {@code [} and the first {@code ]}, removes that pair, and
 * repeats, at most a hundred times per cell. Copied here so a check sees the same text and runs the
 * settings screen does.
 *
 * @param drawnText the cell's text with every bracket pair removed
 * @param runTexts  the bracketed runs, in reading order
 */
public record LunaSettingsHighlights(
    String drawnText,
    List<String> runTexts) {

    // LunaLib stops extracting after this many pairs in one cell.
    private static final int MAXIMUM_BRACKET_PAIRS = 100;

    /**
     * Holds the runs unmodifiable, however they were built.
     *
     * @param drawnText see the record
     * @param runTexts  see the record
     */
    public LunaSettingsHighlights {

        Objects.requireNonNull(drawnText, "drawnText");
        runTexts = List.copyOf(runTexts);
    }

    /**
     * Reads a cell the way LunaLib does before drawing it.
     *
     * @param cellText the cell as the settings table holds it
     * @return what LunaLib draws and highlights
     */
    public static LunaSettingsHighlights parseHighlights(String cellText) {

        var drawnText = cellText;
        var runTexts = new ArrayList<String>();

        for (var pairCount = 0; pairCount < MAXIMUM_BRACKET_PAIRS && drawnText.contains("["); pairCount++) {

            var openIndex = drawnText.indexOf('[');
            var closeIndex = drawnText.indexOf(']');

            // LunaLib reads between the first of each, so a closing bracket ahead of the opening one would
            // throw inside the settings screen.
            if (closeIndex < openIndex) {
                throw new AssertionError("LunaLib cannot read a [ with no ] after it: " + cellText);
            }

            runTexts.add(drawnText.substring(openIndex + 1, closeIndex));

            drawnText = drawnText
                .replaceFirst("\\[", "")
                .replaceFirst("]", "");
        }
        return new LunaSettingsHighlights(drawnText, runTexts);
    }
}
