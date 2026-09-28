package kmlib.testfixtures.starsector.ui.font;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.IntStream;

/**
 * The glyph IDs a font atlas declares, held as inclusive ranges. A localised atlas declares several
 * thousand IDs, most of them in contiguous runs through the CJK block, so ranges are what let a failure
 * name the glyphs a face lacks as a line a reader can take in - {@code 956,8364} rather than a dump of
 * thousands of IDs around them.
 *
 * @param idRanges the ranges, ascending, neither overlapping nor touching one another
 */
public record GlyphIdRanges(
    List<GlyphIdRange> idRanges) {

    // The separators of the spelling a failure names ranges in: between ranges, and between a range's
    // two ends.
    private static final String RANGE_SEPARATOR = ",";
    private static final String BOUND_SEPARATOR = "-";

    /**
     * Copies the ranges, which a caller builds through {@link #createFromIds}.
     */
    public GlyphIdRanges {
        idRanges = List.copyOf(idRanges);
    }

    /**
     * Collects IDs, in any order and with repeats, into ranges.
     *
     * @param ids the IDs an atlas declares
     * @return those IDs as ranges
     */
    public static GlyphIdRanges createFromIds(IntStream ids) {

        var sortedIds = ids.sorted().distinct().toArray();
        var idRanges = new ArrayList<GlyphIdRange>();

        var index = 0;
        while (index < sortedIds.length) {
            var firstId = sortedIds[index];
            var lastId = firstId;

            // Extends the run for as long as each next ID follows on from the last.
            while (index + 1 < sortedIds.length && sortedIds[index + 1] == lastId + 1) {
                index++;
                lastId = sortedIds[index];
            }
            idRanges.add(new GlyphIdRange(firstId, lastId));
            index++;
        }
        return new GlyphIdRanges(idRanges);
    }

    /**
     * @param id a glyph ID, which for a descriptor is the character's code point
     * @return whether the atlas declares it
     */
    public boolean containsId(int id) {
        return idRanges.stream()
            .anyMatch(idRange -> idRange.firstId() <= id && id <= idRange.lastId());
    }

    /**
     * @return how many IDs the ranges hold - the atlas's declared glyph count
     */
    public int countIds() {
        return idRanges.stream()
            .mapToInt(idRange -> idRange.lastId() - idRange.firstId() + 1)
            .sum();
    }

    /**
     * @param other another atlas's IDs
     * @return every ID {@code other} declares that these do not, as ranges
     */
    public GlyphIdRanges listIdsMissingFrom(GlyphIdRanges other) {
        return createFromIds(other.idRanges.stream()
            .flatMapToInt(idRange -> IntStream.rangeClosed(idRange.firstId(), idRange.lastId()))
            .filter(id -> !containsId(id)));
    }

    /**
     * @return these ranges as a failure names them: joined by commas, a run spelt by its two ends and a
     *         lone ID as itself
     */
    public String formatRanges() {

        var spelling = new StringJoiner(RANGE_SEPARATOR);

        for (var idRange : idRanges) {
            spelling.add(idRange.firstId() == idRange.lastId()
                ? Integer.toString(idRange.firstId())
                : idRange.firstId() + BOUND_SEPARATOR + idRange.lastId());
        }
        return spelling.toString();
    }

    /**
     * One run of consecutive glyph IDs.
     *
     * @param firstId the run's first ID
     * @param lastId  the run's last ID, inclusive
     */
    public record GlyphIdRange(
        int firstId,
        int lastId) {
    }
}
