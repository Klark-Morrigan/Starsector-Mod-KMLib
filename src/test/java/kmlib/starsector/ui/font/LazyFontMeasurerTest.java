package kmlib.starsector.ui.font;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lazywizard.lazylib.ui.LazyFont;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins what this measurer reads off a loaded face: the width of a line, and how far below a line's top
 * edge the middle of its lower-case letters falls.
 *
 * <p>The band metric is the one worth fixing hardest, because nothing downstream can catch it being
 * wrong. It decides where a mark set among a line's words lands, and a misread glyph box or a dropped
 * scale puts that mark through the very words it was meant to sit between - which no measurement of the
 * box, and no test of the layout that placed the run, would notice.
 *
 * <p>The face is mocked rather than loaded: a real one needs a {@code .fnt} off an install and a texture
 * bind, neither of which exists under the test JVM, and what is under test here is the arithmetic over a
 * glyph box rather than LazyLib's own parsing of one.
 */
class LazyFontMeasurerTest {

    // A face whose atlas was rasterised at 15, so a request at 30 comes out at double and a request at 15
    // at 1:1 - which is what says whether the scale was applied at all.
    private static final float ATLAS_BASE_HEIGHT = 15f;
    private static final double NATIVE_FONT_SIZE = 15d;
    private static final double DOUBLED_FONT_SIZE = 30d;

    // The lower-case glyph box the mocked face reports: its top sits 4 atlas units below the line's top
    // edge, and it stands 6 tall. Both odd enough that a drop worked out from either one alone, or from
    // the line height instead, comes out at a number this one cannot be confused with.
    private static final int LOWERCASE_GLYPH_TOP_OFFSET = 4;
    private static final int LOWERCASE_GLYPH_HEIGHT = 6;

    private static final float TOLERANCE = 0.001f;

    // A face reporting the glyph box above for its lower-case band, and the atlas height every request is
    // scaled against. Built per case so no case can be steered by another's stubbing.
    private static LazyFont createFaceMock() {

        var lowercaseGlyphMock = mock(LazyFont.LazyChar.class);

        when(lowercaseGlyphMock.getYOffset())
            .thenReturn(LOWERCASE_GLYPH_TOP_OFFSET);
        when(lowercaseGlyphMock.getHeight())
            .thenReturn(LOWERCASE_GLYPH_HEIGHT);

        var faceMock = mock(LazyFont.class);

        when(faceMock.getBaseHeight())
            .thenReturn(ATLAS_BASE_HEIGHT);
        when(faceMock.getChar('x'))
            .thenReturn(lowercaseGlyphMock);

        return faceMock;
    }

    @Nested
    class MeasureLineWidth {

        @Test
        void measureLineWidthAnswersWhatTheFaceMeasuresTheLineAt() {
            // The port's whole contribution is asking the loaded face, so what is pinned is that the size
            // asked for reaches it rather than a size of this class's own.
            var faceMock = mock(LazyFont.class);

            when(faceMock.calcWidth("Hegemony", (float) DOUBLED_FONT_SIZE))
                .thenReturn(96f);

            assertThat(new LazyFontMeasurer(faceMock).measureLineWidth("Hegemony", DOUBLED_FONT_SIZE))
                .isCloseTo(96d, within((double) TOLERANCE));
        }
    }

    @Nested
    class MeasureLowercaseBandCentreDrop {

        @Test
        void measureLowercaseBandCentreDropFallsAtTheMiddleOfTheGlyphBox() {
            // At the atlas's own height nothing is scaled, so the drop is the glyph's top offset plus
            // half its height: 4 + 3.
            var drop = new LazyFontMeasurer(createFaceMock())
                .measureLowercaseBandCentreDrop(NATIVE_FONT_SIZE);

            assertThat(drop)
                .isCloseTo(7d, within((double) TOLERANCE));
        }

        @Test
        void measureLowercaseBandCentreDropScalesWithTheSizeTheLineDrawsAt() {
            // Drawn at twice the atlas height every glyph unit is worth two, so the same box centres at
            // 14 rather than 7 - a drop left unscaled would put the rule near the cap height of a line
            // drawn large, which is exactly the case a box mixing faces produces.
            var drop = new LazyFontMeasurer(createFaceMock())
                .measureLowercaseBandCentreDrop(DOUBLED_FONT_SIZE);

            assertThat(drop)
                .isCloseTo(14d, within((double) TOLERANCE));
        }
    }
}
