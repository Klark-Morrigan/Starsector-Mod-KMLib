package kmlib.starsector.ui.font;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lazywizard.lazylib.ui.LazyFont;
import org.mockito.MockedStatic;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins how the live reader tells a drawn character from a fallen-back one, over a face standing in for
 * LazyLib's: every character it holds answers its own glyph, anything else the fallback, and the
 * typographic apostrophe LazyLib redraws as the straight one answers that glyph.
 *
 * <p>The face and the cache are stood in for, a real face needing a {@code .fnt} off an install and a
 * texture bind.
 */
class LazyFontGlyphCoverageReaderTest {

    // "Hegemony" (U+9738 U+4E3B), the faction's name on a localised install - the text its map labels
    // draw there.
    private static final String LOCALISED_NAME = "霸主";

    // A typographic apostrophe (U+2019), which LazyLib draws as the straight one.
    private static final char TYPOGRAPHIC_APOSTROPHE = '’';

    // A character outside the basic plane, which LazyLib's single-unit lookup cannot ask for.
    // "Grinning face" (U+1F600).
    private static final String SUPPLEMENTARY_CHARACTER = "😀";

    // The printable ASCII range the stand-in face holds.
    private static final char FIRST_HELD_CHARACTER = ' ';
    private static final char LAST_HELD_CHARACTER = '~';

    private MockedStatic<LazyFontCache> cacheMock;

    @BeforeEach
    void mockTheCache() {
        cacheMock = mockStatic(LazyFontCache.class);
    }

    @AfterEach
    void closeTheCache() {
        cacheMock.close();
    }

    @Nested
    class CoversText {

        @Test
        void coversTextIsTrueForTextEveryCharacterOfWhichTheAtlasHolds() {

            answerWith(createAsciiFaceMock(true));

            assertThat(LazyFontGlyphCoverageReader.coversText(StarsectorFont.VANILLA_INSIGNIA_42, "Hegemony"))
                .isTrue();
        }

        @Test
        void coversTextIsFalseWhenACharacterFallsBack() {
            // The vanilla atlas under a localised name: every character comes back as the question mark,
            // which is the row of them KMU's map labels drew.
            answerWith(createAsciiFaceMock(true));

            assertThat(LazyFontGlyphCoverageReader.coversText(StarsectorFont.VANILLA_INSIGNIA_42, LOCALISED_NAME))
                .isFalse();
        }

        @Test
        void coversTextCountsAPresentQuestionMarkAsDrawn() {
            // The question mark is the fallback's own glyph, so a reader comparing against the fallback
            // alone would call the one character it certainly draws a gap.
            answerWith(createAsciiFaceMock(true));

            assertThat(LazyFontGlyphCoverageReader.coversText(StarsectorFont.VANILLA_INSIGNIA_15, "Who?"))
                .isTrue();
        }

        @Test
        void coversTextCountsAQuestionMarkAsAGapWhereTheAtlasFallsBackToTheSpace() {
            // An atlas without a question mark falls back to its space, so the question mark itself is
            // then one of the characters it cannot draw.
            answerWith(createAsciiFaceMock(false));

            assertThat(LazyFontGlyphCoverageReader.coversText(StarsectorFont.VANILLA_INSIGNIA_15, "Who?"))
                .isFalse();
        }

        @Test
        void coversTextCountsATypographicApostropheLazyLibRedrawsAsDrawn() {
            // LazyLib answers the typographic apostrophe with the straight one's glyph - a different ID
            // from the character asked for, and still no fallback.
            answerWith(createAsciiFaceMock(true));

            assertThat(LazyFontGlyphCoverageReader.coversText(
                    StarsectorFont.VANILLA_INSIGNIA_15,
                    "Ludd" + TYPOGRAPHIC_APOSTROPHE + "s"))
                .isTrue();
        }

        @Test
        void coversTextLeavesWhitespaceUnasked() {
            // A face draws whitespace as space whether its atlas holds a glyph or not, so a tab in a name
            // is no reason to leave the preferred face.
            answerWith(createAsciiFaceMock(true));

            assertThat(LazyFontGlyphCoverageReader.coversText(StarsectorFont.VANILLA_INSIGNIA_15, "Tri\tTachyon"))
                .isTrue();
        }

        @Test
        void coversTextIsFalseForACharacterOutsideTheBasicPlane() {

            answerWith(createAsciiFaceMock(true));

            assertThat(LazyFontGlyphCoverageReader.coversText(
                    StarsectorFont.VANILLA_INSIGNIA_15,
                    SUPPLEMENTARY_CHARACTER))
                .isFalse();
        }

        @Test
        void coversTextIsFalseWhenTheFaceCannotLoad() {
            // A face that will not load draws nothing, so it covers nothing either.
            cacheMock
                .when(() -> LazyFontCache.loadByFace(StarsectorFont.VANILLA_INSIGNIA_42))
                .thenReturn(null);

            assertThat(LazyFontGlyphCoverageReader.coversText(StarsectorFont.VANILLA_INSIGNIA_42, "Hegemony"))
                .isFalse();
        }
    }

    private void answerWith(LazyFont faceMock) {
        cacheMock
            .when(() -> LazyFontCache.loadByFace(any()))
            .thenReturn(faceMock);
    }

    // A face holding printable ASCII and nothing past it, answering the typographic apostrophe with the
    // straight one as LazyLib does. Without a question mark it falls back to its space, as LazyLib does for
    // such an atlas; with one, to the question mark.
    private static LazyFont createAsciiFaceMock(boolean isQuestionMarkHeld) {

        var glyphByCharacter = new HashMap<Character, LazyFont.LazyChar>();
        for (var character = FIRST_HELD_CHARACTER; character <= LAST_HELD_CHARACTER; character++) {
            if (character != '?' || isQuestionMarkHeld) {
                glyphByCharacter.put(character, createGlyphMock(character));
            }
        }
        glyphByCharacter.put(TYPOGRAPHIC_APOSTROPHE, glyphByCharacter.get('\''));

        var fallbackGlyph = glyphByCharacter.get(isQuestionMarkHeld ? '?' : ' ');
        var faceMock = mock(LazyFont.class);

        when(faceMock.getChar(anyChar()))
            .thenAnswer(call -> glyphByCharacter.getOrDefault(call.<Character>getArgument(0), fallbackGlyph));

        return faceMock;
    }

    private static LazyFont.LazyChar createGlyphMock(char character) {

        var glyphMock = mock(LazyFont.LazyChar.class);

        when(glyphMock.getId())
            .thenReturn((int) character);

        return glyphMock;
    }
}
