package kmlib.testfixtures.starsector.settings;

import kmlib.settings.LabeledChoice;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins each link of the walk from a field ID to its fallback over a source tree written the way the
 * conventions expect, and the failure of each link over one written some other way - the walk's point
 * being that a getter it cannot follow fails loudly rather than dropping out.
 */
final class LunaSettingsSourceTextTest {

    // The field IDs held in one class and read in another, so every link that crosses a file is walked.
    private static final String FIELDS_SOURCE = """
        final class Fields {

            static final String WIDTH_FIELD = "kmu_width";
            static final String SHOW_FIELD = "kmu_show";
            // kmu_mentioned in a comment is prose, not a read.
        }
        """;

    private static final String READER_SOURCE = """
        final class Reader {

            private static final int DEFAULT_WIDTH = 4;
            private static final boolean DEFAULT_SHOW = true;
            private static final Palette DEFAULT_PALETTE = Palette.SILVER;
            private static final Palette MISSING_PALETTE = Palette.BRONZE;

            int readWidth() { return settings.readInt(WIDTH_FIELD, DEFAULT_WIDTH); }
            boolean readShow() { return settings.readBoolean(SHOW_FIELD, DEFAULT_SHOW); }
        }
        """;

    private enum Palette implements LabeledChoice {

        GOLD("Gold"),
        SILVER("Silver");

        private final String label;

        Palette(String label) {
            this.label = label;
        }

        @Override
        public String getLabel() {
            return label;
        }
    }

    private static LunaSettingsSourceText createSourceText(Path sourceRoot, Map<String, String> sourcesByPath)
            throws IOException {

        for (var pathAndSource : sourcesByPath.entrySet()) {

            var source = sourceRoot.resolve(pathAndSource.getKey());

            Files.createDirectories(source.getParent());
            Files.writeString(source, pathAndSource.getValue(), StandardCharsets.UTF_8);
        }
        return new LunaSettingsSourceText(sourceRoot, "kmu_");
    }

    private static LunaSettingsSourceText createConventionalSourceText(Path sourceRoot) throws IOException {

        return createSourceText(
            sourceRoot,
            Map.of("kmu/Fields.java", FIELDS_SOURCE, "kmu/Reader.java", READER_SOURCE));
    }

    @Nested
    class FindBooleanFallbackConstant {

        @Test
        void theFallbackPassedBesideTheFieldIsFound(@TempDir Path sourceRoot) throws IOException {

            assertThat(createConventionalSourceText(sourceRoot).findBooleanFallbackConstant("kmu_show"))
                .isEqualTo("DEFAULT_SHOW");
        }

        @Test
        void aRowFetchedThroughANumericReadIsNotFollowed(@TempDir Path sourceRoot) throws IOException {

            var sourceText = createConventionalSourceText(sourceRoot);

            assertThatThrownBy(() -> sourceText.findBooleanFallbackConstant("kmu_width"))
                .isInstanceOf(AssertionError.class)
                .hasMessageStartingWith("Expected exactly one match for the fallback passed beside WIDTH_FIELD")
                .hasMessageEndingWith(" but found 0: []");
        }

        @Test
        void aFieldIdHeldByTwoConstantsIsRefused(@TempDir Path sourceRoot) throws IOException {

            var sourceText = createSourceText(sourceRoot, Map.of("kmu/Twice.java", """
                final class Twice {
                    static final String FIRST_FIELD = "kmu_twice";
                    static final String SECOND_FIELD = "kmu_twice";
                }
                """));

            assertThatThrownBy(() -> sourceText.findBooleanFallbackConstant("kmu_twice"))
                .isInstanceOf(AssertionError.class)
                .hasMessageStartingWith("Expected exactly one match for the constant holding field id kmu_twice")
                .hasMessageEndingWith(" but found 2: [FIRST_FIELD, SECOND_FIELD]");
        }
    }

    @Nested
    class FindNumericFallbackConstant {

        @ParameterizedTest(name = "{0}")
        @CsvSource({"readDouble", "readFloat", "readInt"})
        void everyTypedNumericReadIsFollowed(String readMethodName, @TempDir Path sourceRoot) throws IOException {

            var sourceText = createSourceText(sourceRoot, Map.of("kmu/Numeric.java", """
                final class Numeric {
                    static final String SCALE_FIELD = "kmu_scale";
                    double readScale() { return settings.%s(SCALE_FIELD, DEFAULT_SCALE); }
                }
                """.formatted(readMethodName)));

            assertThat(sourceText.findNumericFallbackConstant("kmu_scale"))
                .isEqualTo("DEFAULT_SCALE");
        }

        @Test
        void aRowFetchedThroughABooleanReadIsNotFollowed(@TempDir Path sourceRoot) throws IOException {

            var sourceText = createConventionalSourceText(sourceRoot);

            assertThatThrownBy(() -> sourceText.findNumericFallbackConstant("kmu_show"))
                .isInstanceOf(AssertionError.class)
                .hasMessageStartingWith("Expected exactly one match for the fallback passed beside SHOW_FIELD");
        }
    }

    @Nested
    class ReadDeclaredFlag {

        @Test
        void theStateTheConstantIsDeclaredAsIsRead(@TempDir Path sourceRoot) throws IOException {

            assertThat(createConventionalSourceText(sourceRoot).readDeclaredFlag("DEFAULT_SHOW"))
                .isTrue();
        }

        @Test
        void aConstantDeclaredAsSomethingElseIsRefused(@TempDir Path sourceRoot) throws IOException {

            var sourceText = createConventionalSourceText(sourceRoot);

            assertThatThrownBy(() -> sourceText.readDeclaredFlag("DEFAULT_WIDTH"))
                .isInstanceOf(AssertionError.class)
                .hasMessageStartingWith("Expected exactly one match for a declaration of DEFAULT_WIDTH");
        }
    }

    @Nested
    class ReadDeclaredNumber {

        @ParameterizedTest(name = "{0} reads as {1}")
        @CsvSource({"4, 4.0", "2.5, 2.5", "2.5f, 2.5", "0.75D, 0.75", "-3, -3.0"})
        void theNumberIsReadWithoutItsSuffix(String declaredLiteral, double expectedNumber, @TempDir Path sourceRoot)
                throws IOException {

            var sourceText = createSourceText(sourceRoot, Map.of("kmu/Declared.java", """
                final class Declared {
                    private static final double DEFAULT_SCALE = %s;
                }
                """.formatted(declaredLiteral)));

            assertThat(sourceText.readDeclaredNumber("DEFAULT_SCALE"))
                .isEqualTo(expectedNumber);
        }

        @Test
        void aConstantTheSourcesDoNotDeclareIsRefused(@TempDir Path sourceRoot) throws IOException {

            var sourceText = createConventionalSourceText(sourceRoot);

            assertThatThrownBy(() -> sourceText.readDeclaredNumber("DEFAULT_ABSENT"))
                .isInstanceOf(AssertionError.class)
                .hasMessageStartingWith("Expected exactly one match for a declaration of DEFAULT_ABSENT");
        }
    }

    @Nested
    class ReadFallbackLabel {

        @Test
        void theLabelOfTheDeclaredChoiceIsRead(@TempDir Path sourceRoot) throws IOException {

            assertThat(createConventionalSourceText(sourceRoot).readFallbackLabel("DEFAULT_PALETTE", Palette.values()))
                .isEqualTo("Silver");
        }

        @Test
        void aChoiceTheEnumDoesNotOfferIsRefused(@TempDir Path sourceRoot) throws IOException {

            var sourceText = createConventionalSourceText(sourceRoot);

            assertThatThrownBy(() -> sourceText.readFallbackLabel("MISSING_PALETTE", Palette.values()))
                .isInstanceOf(AssertionError.class)
                .hasMessage("MISSING_PALETTE is declared as BRONZE, which is no option of the enum this row's table "
                    + "names");
        }

        @Test
        void aConstantDeclaredTwiceIsRefused(@TempDir Path sourceRoot) throws IOException {

            var sourceText = createSourceText(sourceRoot, Map.of(
                "kmu/First.java", "final class First { static final Palette DEFAULT_PALETTE = Palette.GOLD; }",
                "kmu/Second.java", "final class Second { static final Palette DEFAULT_PALETTE = Palette.SILVER; }"));

            assertThatThrownBy(() -> sourceText.readFallbackLabel("DEFAULT_PALETTE", Palette.values()))
                .isInstanceOf(AssertionError.class)
                .hasMessageStartingWith("Expected exactly one match for declarations of DEFAULT_PALETTE");
        }
    }

    @Nested
    class ReadFieldIdLiteralsInMainSources {

        @Test
        void everyQuotedFieldIdInAJavaSourceIsRead(@TempDir Path sourceRoot) throws IOException {

            // A comment's mention and a non-Java file's are not reads of the field.
            var sourceText = createSourceText(sourceRoot, Map.of(
                "kmu/Fields.java", FIELDS_SOURCE,
                "kmu/notes.txt", "\"kmu_noted\""));

            assertThat(sourceText.readFieldIdLiteralsInMainSources())
                .containsExactlyInAnyOrder("kmu_width", "kmu_show");
        }

        @Test
        void anAbsentSourceTreeNamesItself(@TempDir Path directory) {

            var sourceText = new LunaSettingsSourceText(directory.resolve("absent"), "kmu_");

            assertThatThrownBy(sourceText::readFieldIdLiteralsInMainSources)
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("Could not walk ")
                .hasMessageContaining("absent");
        }
    }
}
