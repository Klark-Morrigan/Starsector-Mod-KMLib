package kmlib.starsector.ui.font;

import kmlib.testfixtures.logging.LogAppenderFake;
import kmlib.testfixtures.starsector.ui.font.FaceLineHeightReaderFake;
import kmlib.testfixtures.starsector.ui.font.GlyphCoverageReaderFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins which face a text settles on - the one it asks for where the atlas holds what it draws, the next
 * cut down where it does not, held to the kinds of text it is made of and to nothing else - and that a
 * settled answer is held rather than read again.
 */
final class SettledFaceMemoTests {

    // "Hegemony" (U+9738 U+4E3B), a faction name as a localised install draws it.
    private static final String LOCALISED_NAME = "霸主";

    // A caller's kinds of text: names in the localised script, and its own Latin words.
    private enum TextKind {
        NAMES,
        WORDS
    }

    private static final Set<TextKind> NAMES_ONLY = Set.of(TextKind.NAMES);
    private static final Set<TextKind> WORDS_ONLY = Set.of(TextKind.WORDS);

    private static final Map<TextKind, List<String>> LOCALISED_TEXTS = Map.of(
        TextKind.NAMES, List.of("Tri-Tachyon", LOCALISED_NAME),
        TextKind.WORDS, List.of("Political map"));

    // A localised install: every atlas holds Latin text, and the middle insignia cut the localised script
    // besides - so the largest cut, asked for a localised name, has one step to take.
    private static FaceResolver createLocalisedInstallResolver() {

        return new FaceResolver(
            FaceLineHeightReaderFake.createVanillaLineHeights(),
            GlyphCoverageReaderFake.createLatinOnlyCoverage().coveringEveryCharacter(StarsectorFont.VANILLA_INSIGNIA_25),
            StarsectorFont.VANILLA_INSIGNIA_15);
    }

    private static SettledFaceMemo<TextKind> createLocalisedMemo(Function<TextKind, List<String>> reader) {
        return new SettledFaceMemo<>(SettledFaceMemoTests::createLocalisedInstallResolver, reader);
    }

    private static SettledFaceMemo<TextKind> createLocalisedMemo() {
        return createLocalisedMemo(LOCALISED_TEXTS::get);
    }

    @Nested
    class SettleFace {

        @Test
        void keepsTheRequestedFaceWhereItsAtlasHoldsEveryProbedText() {

            assertThat(createLocalisedMemo().settleFace(StarsectorFont.VANILLA_INSIGNIA_42, WORDS_ONLY))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_42);
        }

        @Test
        void stepsDownTheFamilyWhereTheRequestedAtlasLacksAProbedText() {
            // The map-label case: the largest cut holds no localised glyph, the next one down does.
            assertThat(createLocalisedMemo().settleFace(StarsectorFont.VANILLA_INSIGNIA_42, NAMES_ONLY))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_25);
        }

        @Test
        void holdsTheFaceToTheKindsItsTextIsMadeOfAndNoOthers() {
            // The localised name is among the names, which this text never draws.
            var memo = createLocalisedMemo();

            memo.settleFace(StarsectorFont.VANILLA_INSIGNIA_42, NAMES_ONLY);

            assertThat(memo.settleFace(StarsectorFont.VANILLA_INSIGNIA_42, WORDS_ONLY))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_42);
        }

        @Test
        void readsEachKindOnceHoweverManyTextsAreHeldToIt() {
            // Reading a kind walks every text of it, which is the cost holding the faces saves.
            var nameReads = new int[1];
            var memo = createLocalisedMemo(kind -> {
                if (kind == TextKind.NAMES) {
                    nameReads[0]++;
                }
                return LOCALISED_TEXTS.get(kind);
            });

            memo.settleFace(StarsectorFont.VANILLA_INSIGNIA_42, NAMES_ONLY);
            memo.settleFace(StarsectorFont.VANILLA_INSIGNIA_15, NAMES_ONLY);
            memo.settleFace(StarsectorFont.VANILLA_INSIGNIA_42, Set.of(TextKind.NAMES));

            assertThat(nameReads[0])
                .isEqualTo(1);
        }

        @Test
        void saysOnceWhichFaceATextMovedTo() {
            // The first thing a report of text drawn wrongly is read against.
            var memo = createLocalisedMemo();

            var logFake = LogAppenderFake.captureLogOf(SettledFaceMemo.class, () -> {
                memo.settleFace(StarsectorFont.VANILLA_INSIGNIA_42, NAMES_ONLY);
                memo.settleFace(StarsectorFont.VANILLA_INSIGNIA_42, NAMES_ONLY);
            });

            assertThat(logFake.getMessages())
                .containsExactly("Text asking for insignia42LTaa against [NAMES] draws in graphics/fonts/insignia25LTaa.fnt");
        }
    }

    @Nested
    class CreateUnsettled {

        @Test
        void keepsEveryTextInTheFaceItAsksFor() {
            // No text to hold a face to, such as no game loaded: nothing is read and nothing moves.
            SettledFaceMemo<TextKind> memo = SettledFaceMemo.createUnsettled();

            assertThat(memo.settleFace(StarsectorFont.VANILLA_INSIGNIA_42, NAMES_ONLY))
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_42);
        }
    }

    @Nested
    class DiscardFaces {

        @Test
        void readsTheTextAgainOnTheNextSettling() {
            // A released holder keeps nothing, so a text settled afterwards is settled from what is
            // read then.
            var nameReads = new int[1];
            var memo = createLocalisedMemo(kind -> {
                nameReads[0]++;
                return LOCALISED_TEXTS.get(kind);
            });

            memo.settleFace(StarsectorFont.VANILLA_INSIGNIA_42, NAMES_ONLY);
            memo.discardFaces();
            memo.settleFace(StarsectorFont.VANILLA_INSIGNIA_42, NAMES_ONLY);

            assertThat(nameReads[0])
                .isEqualTo(2);
        }
    }
}
