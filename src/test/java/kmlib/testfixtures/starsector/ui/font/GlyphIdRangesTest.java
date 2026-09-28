package kmlib.testfixtures.starsector.ui.font;

import kmlib.testfixtures.starsector.ui.font.GlyphIdRanges.GlyphIdRange;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class GlyphIdRangesTest {

    // A run of three, a lone ID past it: the two shapes a range takes.
    private static GlyphIdRanges createRunAndLoneId() {
        return GlyphIdRanges.createFromIds(IntStream.of(32, 33, 34, 40));
    }

    @Nested
    class CreateFromIds {

        @Test
        void createFromIdsFoldsConsecutiveIdsIntoRangesWhateverOrderTheyArriveIn() {

            var ranges = GlyphIdRanges.createFromIds(IntStream.of(34, 32, 33, 33, 40, 50, 51));

            assertThat(ranges.idRanges())
                .containsExactly(new GlyphIdRange(32, 34), new GlyphIdRange(40, 40), new GlyphIdRange(50, 51));
        }

        @Test
        void createFromIdsHoldsNoRangeForNoIds() {

            assertThat(GlyphIdRanges.createFromIds(IntStream.empty()).idRanges())
                .isEmpty();
        }
    }

    @Nested
    class ContainsId {

        @Test
        void containsIdIsTrueAtARangesEndsAndInside() {

            var ranges = createRunAndLoneId();

            assertThat(List.of(ranges.containsId(32), ranges.containsId(33), ranges.containsId(34)))
                .containsOnly(true);
        }

        @Test
        void containsIdIsTrueForALoneId() {

            assertThat(createRunAndLoneId().containsId(40))
                .isTrue();
        }

        @Test
        void containsIdIsFalseBetweenRanges() {

            assertThat(createRunAndLoneId().containsId(35))
                .isFalse();
        }
    }
}
