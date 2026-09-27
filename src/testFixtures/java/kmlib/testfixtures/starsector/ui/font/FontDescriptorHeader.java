package kmlib.testfixtures.starsector.ui.font;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The header of a bitmap font descriptor ({@code .fnt}), read the way LazyLib reads it: its first three
 * lines joined by a space and split into tokens on {@code =} and on whitespace outside quotes. What a
 * suite holds an installed face to - LazyLib refuses a header of any token count but one, the game's own
 * loader misreads any page count but one, and the line height is the size the atlas draws 1:1 at.
 *
 * <p>Read byte for byte as Latin-1 rather than as UTF-8. A localised atlas names its face in the
 * localisation's own script, and a byte never splits a token here: no byte of a multi-byte UTF-8
 * sequence is an equals sign, a quote or whitespace, so the token count comes out as LazyLib's does
 * whatever encoding the file is in, and a file in some other encoding still reads.
 *
 * @param tokenCount  how many tokens the header splits into
 * @param lineHeight  the {@code lineHeight} the header states
 * @param pageCount   the {@code pages} the header states
 */
public record FontDescriptorHeader(
    int tokenCount,
    double lineHeight,
    int pageCount) {

    /** The token count LazyLib requires of a header, refusing to load a face whose header has another. */
    public static final int LAZYFONT_HEADER_TOKEN_COUNT = 51;

    // LazyLib's own split, copied character for character: an equals sign, or a run of whitespace with
    // an even number of quotes after it - so a quoted face name keeps its spaces.
    private static final Pattern HEADER_TOKEN_SEPARATOR = Pattern.compile("=|\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

    // LazyLib reads exactly this many lines as the header: info, common and the one page line.
    private static final int HEADER_LINE_COUNT = 3;

    private static final String LINE_HEIGHT_KEY = "lineHeight";
    private static final String PAGE_COUNT_KEY = "pages";

    /**
     * Reads the header of the descriptor at {@code descriptorFile}.
     *
     * @param descriptorFile a {@code .fnt} file
     * @return its header's token count, line height and page count
     */
    public static FontDescriptorHeader readDescriptorHeader(Path descriptorFile) {

        List<String> lines;
        try {
            lines = Files.readAllLines(descriptorFile, StandardCharsets.ISO_8859_1);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read " + descriptorFile, exception);
        }
        if (lines.size() < HEADER_LINE_COUNT) {
            throw new IllegalArgumentException(descriptorFile + " holds no three-line header");
        }
        // Split with Java's trailing-empty drop, which is LazyLib's dropLastWhile over Kotlin's split.
        var tokens = HEADER_TOKEN_SEPARATOR.split(String.join(" ", lines.subList(0, HEADER_LINE_COUNT)));

        return new FontDescriptorHeader(
            tokens.length,
            Double.parseDouble(readValueAfterKey(tokens, LINE_HEIGHT_KEY, descriptorFile)),
            Integer.parseInt(readValueAfterKey(tokens, PAGE_COUNT_KEY, descriptorFile)));
    }

    // The token a key is followed by, the split having parted each key from its value at the equals.
    private static String readValueAfterKey(String[] tokens, String key, Path descriptorFile) {

        for (var index = 0; index < tokens.length - 1; index++) {
            if (tokens[index].equals(key)) {
                return tokens[index + 1];
            }
        }
        throw new IllegalArgumentException(descriptorFile + " states no " + key);
    }
}
