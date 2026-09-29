package kmlib.starsector.ui.font.installed;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The header of a bitmap font descriptor ({@code .fnt}), read the way LazyLib reads it: its first three
 * lines joined by a space and split into tokens on {@code =} and on whitespace outside quotes. What a
 * suite holds an installed face to - LazyLib refuses a header of any token count but one, the game's own
 * loader misreads any page count but one, and the line height is the size the atlas draws 1:1 at.
 *
 * @param tokenCount  how many tokens the header splits into
 * @param lineHeight  the {@code lineHeight} the header states
 * @param pageCount   the {@code pages} the header states
 */
record FontDescriptorHeader(
    int tokenCount,
    double lineHeight,
    int pageCount) {

    /** The token count LazyLib requires of a header, refusing to load a face whose header has another. */
    static final int LAZYFONT_HEADER_TOKEN_COUNT = 51;

    // LazyLib's own split, copied character for character: an equals sign, or a run of whitespace with
    // an even number of quotes after it - so a quoted face name keeps its spaces.
    private static final Pattern HEADER_TOKEN_SEPARATOR = Pattern.compile("=|\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

    // LazyLib reads exactly this many lines as the header: info, common and the one page line.
    private static final int HEADER_LINE_COUNT = 3;

    private static final String LINE_HEIGHT_KEY = "lineHeight";
    private static final String PAGE_COUNT_KEY = "pages";

    /**
     * Reads the header off a descriptor's lines.
     *
     * @param descriptorLines every line of a {@code .fnt} file
     * @return its header's token count, line height and page count
     */
    static FontDescriptorHeader readDescriptorHeader(List<String> descriptorLines) {

        if (descriptorLines.size() < HEADER_LINE_COUNT) {
            throw new IllegalArgumentException("The descriptor holds no three-line header");
        }
        // Split with Java's trailing-empty drop, which is LazyLib's dropLastWhile over Kotlin's split.
        var tokens = HEADER_TOKEN_SEPARATOR.split(String.join(" ", descriptorLines.subList(0, HEADER_LINE_COUNT)));

        return new FontDescriptorHeader(
            tokens.length,
            Double.parseDouble(readValueAfterKey(tokens, LINE_HEIGHT_KEY)),
            Integer.parseInt(readValueAfterKey(tokens, PAGE_COUNT_KEY)));
    }

    // The token a key is followed by, the split having parted each key from its value at the equals.
    private static String readValueAfterKey(String[] tokens, String key) {

        for (var index = 0; index < tokens.length - 1; index++) {
            if (tokens[index].equals(key)) {
                return tokens[index + 1];
            }
        }
        throw new IllegalArgumentException("The descriptor states no " + key);
    }
}
