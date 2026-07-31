package kmlib.starsector.ui.render.gl;

import kmlib.starsector.ui.text.TextAlignment;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.lazywizard.lazylib.ui.LazyFont;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class TextAnchorsTest {

    @Nested
    class ResolveAnchor {
        // The anchor each alignment must land on, restated rather than derived from the value under
        // test: a mapping built by name would agree with any drift in the names it matched, which is
        // the failure the hand-written switch exists to catch.
        @ParameterizedTest
        @EnumSource(TextAlignment.class)
        void resolveAnchorNamesTheLazyFontAnchorForTheSamePosition(TextAlignment alignment) {
            var expected = switch (alignment) {
            case TOP_LEFT -> LazyFont.TextAnchor.TOP_LEFT;
            case TOP_CENTER -> LazyFont.TextAnchor.TOP_CENTER;
            case TOP_RIGHT -> LazyFont.TextAnchor.TOP_RIGHT;
            case CENTER_LEFT -> LazyFont.TextAnchor.CENTER_LEFT;
            case CENTER -> LazyFont.TextAnchor.CENTER;
            case CENTER_RIGHT -> LazyFont.TextAnchor.CENTER_RIGHT;
            case BOTTOM_LEFT -> LazyFont.TextAnchor.BOTTOM_LEFT;
            case BOTTOM_CENTER -> LazyFont.TextAnchor.BOTTOM_CENTER;
            case BOTTOM_RIGHT -> LazyFont.TextAnchor.BOTTOM_RIGHT;
            };

            assertThat(TextAnchors.resolveAnchor(alignment)).isEqualTo(expected);
        }

        @Test
        void resolveAnchorIsDistinctForEveryAlignment() {
            // Nine positions to nine anchors: two alignments sharing one anchor - the copy-paste a
            // hand-written switch invites - would pin text of one alignment where the other belongs.
            var anchors = Arrays.stream(TextAlignment.values())
                .map(TextAnchors::resolveAnchor)
                .toList();

            assertThat(anchors).doesNotHaveDuplicates();
        }
    }
}
