package kmlib.testfixtures.starsector.ui.font;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The glyph IDs a font atlas declares, held as inclusive ranges. A localised atlas declares several
 * thousand IDs, most of them in contiguous runs through the CJK block, so a handful of ranges stands for
 * what would otherwise be a set of thousands, read once per face per install.
 *
 * @param idRanges the ranges, ascending, neither overlapping nor touching one another
 */
public record GlyphIdRanges(
    List<GlyphIdRange> idRanges) {

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
