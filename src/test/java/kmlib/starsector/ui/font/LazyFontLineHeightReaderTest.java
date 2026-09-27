package kmlib.starsector.ui.font;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lazywizard.lazylib.ui.LazyFont;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins what the live reader answers: the loaded face's own line height, and nothing at all for a face
 * that will not load.
 *
 * <p>The cache and the face are both stood in for: a real face needs a {@code .fnt} off an install and a
 * texture bind, and what is under test is which number of the loaded face reaches the caller rather than
 * LazyLib's parsing of the descriptor.
 */
class LazyFontLineHeightReaderTest {

    // A line height no vanilla atlas states, so a reader answering a number of its own for the face -
    // vanilla's 15 for this basename, say - cannot pass by coincidence.
    private static final float INSTALLED_LINE_HEIGHT = 17f;

    @Nested
    class ReadLineHeight {

        @Test
        void readLineHeightAnswersTheLoadedFacesOwnLineHeight() {
            // The install decides the number, so the reader has to take it off the face it loaded rather
            // than off anything the enum could state.
            var faceMock = mock(LazyFont.class);

            when(faceMock.getBaseHeight())
                .thenReturn(INSTALLED_LINE_HEIGHT);

            try (var cacheMock = mockStatic(LazyFontCache.class)) {

                cacheMock
                    .when(() -> LazyFontCache.loadByFace(StarsectorFont.VANILLA_INSIGNIA_15))
                    .thenReturn(faceMock);

                assertThat(LazyFontLineHeightReader.readLineHeight(StarsectorFont.VANILLA_INSIGNIA_15))
                    .isEqualTo(17d);
            }
        }

        @Test
        void readLineHeightAnswersZeroWhenTheFaceCannotLoad() {
            // A face that will not load paints nothing, so a line in it takes no room: the reader answers
            // no height rather than throwing through the style being composed.
            try (var cacheMock = mockStatic(LazyFontCache.class)) {

                cacheMock
                    .when(() -> LazyFontCache.loadByFace(StarsectorFont.VANILLA_INSIGNIA_42))
                    .thenReturn(null);

                assertThat(LazyFontLineHeightReader.readLineHeight(StarsectorFont.VANILLA_INSIGNIA_42))
                    .isZero();
            }
        }
    }
}
