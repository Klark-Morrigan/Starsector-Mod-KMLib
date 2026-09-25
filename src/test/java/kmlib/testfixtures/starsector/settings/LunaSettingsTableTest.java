package kmlib.testfixtures.starsector.settings;

import kmlib.testfixtures.starsector.settings.LunaSettingsTable.FieldBehaviour;
import kmlib.testfixtures.starsector.settings.LunaSettingsTable.FieldRow;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins every reading of a settings table against a file laid out the way LunaLib reads it: rows told
 * apart by type, sections bound by file order, and the cells a caller asks for by meaning rather than by
 * column.
 */
final class LunaSettingsTableTest {

    private static final String HEADER_LINE =
        "fieldID,,,,fieldName,,fieldType,defaultValue,secondaryValue,,,fieldDescription,,,minValue,maxValue,tab";

    // One of each row type the readings tell apart: a caption, a prose row, a Radio and a slider, with a
    // spacer between two of them that carries no prefixed ID.
    private static final String ROWS = """
        kmu_caption,,,,Caption,,Header,Caption,,,,,,,,,General
        kmu_note,,,,,,Text,"Prose, with a comma",,,,,,,,,General
        ,,,,,,,,,,,,,,,,
        kmu_palette,,,,Palette,,Radio,Gold,"Gold, Silver",,,Which palette,,,,,Visuals
        kmu_width,,,,Width,,Int,4,,,,How wide,,,1,8,Visuals
        """;

    private static LunaSettingsTable createTable(Path directory, String rows) throws IOException {

        var settingsCsv = directory.resolve("LunaSettings.csv");

        Files.writeString(settingsCsv, HEADER_LINE + "\n" + rows, StandardCharsets.UTF_8);

        return new LunaSettingsTable(settingsCsv, "kmu_");
    }

    @Nested
    class FindHeaderRowsWhoseCaptionColumnsDisagree {

        @Test
        void aCaptionWhoseNameAndDefaultDifferIsFound(@TempDir Path directory) throws IOException {

            // A value row's name and default are different things by design, so only captions are asked.
            var table = createTable(directory, """
                kmu_agreed,,,,Colours,,Header,Colours,,,,,,,,,General
                kmu_drifted,,,,Colors,,Header,Colours,,,,,,,,,General
                kmu_width,,,,Width,,Int,4,,,,How wide,,,1,8,General
                """);

            assertThat(table.findHeaderRowsWhoseCaptionColumnsDisagree())
                .containsExactly("kmu_drifted");
        }
    }

    @Nested
    class FindRowsStrandedFromTheirSection {

        @Test
        void aValueRowOffItsCaptionsTabOrAheadOfEveryCaptionIsFound(@TempDir Path directory) throws IOException {

            // Prose ahead of every caption owns nothing and belongs to no section, so it is never stranded.
            var table = createTable(directory, """
                kmu_intro,,,,,,Text,Hello,,,,,,,,,General
                kmu_early,,,,Early,,Boolean,true,,,,,,,,,General
                kmu_general,,,,General,,Header,General,,,,,,,,,General
                kmu_placed,,,,Placed,,Boolean,true,,,,,,,,,General
                kmu_visuals,,,,Visuals,,Header,Visuals,,,,,,,,,Visuals
                kmu_moved,,,,Moved,,Boolean,true,,,,,,,,,General
                """);

            assertThat(table.findRowsStrandedFromTheirSection())
                .containsExactly("kmu_early", "kmu_moved");
        }
    }

    @Nested
    class FindTabsDeclaredInMoreThanOneRun {

        @Test
        void aTabInterruptedByAnotherIsFound(@TempDir Path directory) throws IOException {

            var table = createTable(directory, """
                kmu_first,,,,First,,Boolean,true,,,,,,,,,General
                kmu_second,,,,Second,,Boolean,true,,,,,,,,,Visuals
                kmu_third,,,,Third,,Boolean,true,,,,,,,,,General
                """);

            assertThat(table.findTabsDeclaredInMoreThanOneRun())
                .containsExactly("General");
        }

        @Test
        void oneUnbrokenRunPerTabPasses(@TempDir Path directory) throws IOException {

            assertThat(createTable(directory, ROWS).findTabsDeclaredInMoreThanOneRun())
                .isEmpty();
        }
    }

    @Nested
    class FindTextRowsWhoseWordsAreNotDrawn {

        @Test
        void proseWithANameOrWithoutWordsIsFound(@TempDir Path directory) throws IOException {

            var table = createTable(directory, """
                kmu_drawn,,,,,,Text,Drawn words,,,,,,,,,General
                kmu_named,,,,Invisible,,Text,Drawn words,,,,,,,,,General
                kmu_blank,,,,,,Text,,,,,,,,,,General
                """);

            assertThat(table.findTextRowsWhoseWordsAreNotDrawn())
                .containsExactly("kmu_named", "kmu_blank");
        }
    }

    @Nested
    class ReadBehavioursByFieldId {

        @Test
        void eachRowsBehaviourIsReadInFileOrder(@TempDir Path directory) throws IOException {

            var behaviours = createTable(directory, ROWS).readBehavioursByFieldId();

            assertThat(behaviours)
                .containsExactly(
                    Map.entry("kmu_caption", new FieldBehaviour("Header", Optional.empty(), "", "", "")),
                    Map.entry("kmu_note", new FieldBehaviour("Text", Optional.empty(), "", "", "")),
                    Map.entry("kmu_palette", new FieldBehaviour("Radio", Optional.of("Gold"), "Gold, Silver", "", "")),
                    Map.entry("kmu_width", new FieldBehaviour("Int", Optional.of("4"), "", "1", "8")));
        }

        @Test
        void aFieldDeclaredTwiceIsRefused(@TempDir Path directory) throws IOException {

            var table = createTable(directory, """
                kmu_width,,,,Width,,Int,4,,,,How wide,,,1,8,Visuals
                kmu_width,,,,Width,,Int,5,,,,How wide,,,1,8,Visuals
                """);

            assertThatThrownBy(table::readBehavioursByFieldId)
                .isInstanceOf(AssertionError.class)
                .hasMessage("Field kmu_width is declared by more than one row");
        }
    }

    @Nested
    class ReadDeclaredFieldIds {

        @Test
        void everyPrefixedRowIsReadAndTheFurnitureIsNot(@TempDir Path directory) throws IOException {

            // The header line and the spacer carry no prefixed ID.
            assertThat(createTable(directory, ROWS).readDeclaredFieldIds())
                .containsExactly("kmu_caption", "kmu_note", "kmu_palette", "kmu_width");
        }
    }

    @Nested
    class ReadDeclaredTabs {

        @Test
        void everyRowsTabIsReadInFileOrder(@TempDir Path directory) throws IOException {

            assertThat(createTable(directory, ROWS).readDeclaredTabs())
                .containsExactly("General", "General", "Visuals", "Visuals");
        }
    }

    @Nested
    class ReadDefaultValue {

        @Test
        void theDefaultCellIsRead(@TempDir Path directory) throws IOException {

            assertThat(createTable(directory, ROWS).readDefaultValue("kmu_palette", "Radio"))
                .isEqualTo("Gold");
        }

        @Test
        void aRowReadAsAnotherTypeIsRefused(@TempDir Path directory) throws IOException {

            var table = createTable(directory, ROWS);

            assertThatThrownBy(() -> table.readDefaultValue("kmu_palette", "Int"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Field kmu_palette in ")
                .hasMessageEndingWith(" is declared Radio but was read as Int");
        }

        @Test
        void aRowTheFileDoesNotDeclareIsRefused(@TempDir Path directory) throws IOException {

            var table = createTable(directory, ROWS);

            assertThatThrownBy(() -> table.readDefaultValue("kmu_absent", "Int"))
                .isInstanceOf(AssertionError.class)
                .hasMessageStartingWith("Expected exactly one row for field kmu_absent in ")
                .hasMessageEndingWith(" but found 0");
        }
    }

    @Nested
    class ReadDisplayedTexts {

        @Test
        void onlyTheCellsTheScreenDrawsAreRead(@TempDir Path directory) throws IOException {

            // A caption's name column is inert, a prose row's is empty, and a slider's bounds and a stored
            // default are values rather than words.
            assertThat(createTable(directory, ROWS).readDisplayedTexts())
                .containsExactly(
                    "General", "Caption",
                    "General", "Prose, with a comma",
                    "Visuals", "Palette", "Which palette", "Gold, Silver",
                    "Visuals", "Width", "How wide");
        }
    }

    @Nested
    class ReadFieldIdsAndTypes {

        @Test
        void everyRowIsReadAsItsIdAndType(@TempDir Path directory) throws IOException {

            assertThat(createTable(directory, ROWS).readFieldIdsAndTypes())
                .containsExactly(
                    new FieldRow("kmu_caption", "Header"),
                    new FieldRow("kmu_note", "Text"),
                    new FieldRow("kmu_palette", "Radio"),
                    new FieldRow("kmu_width", "Int"));
        }
    }

    @Nested
    class ReadMaxValue {

        @Test
        void theSlidersHighEndIsRead(@TempDir Path directory) throws IOException {

            assertThat(createTable(directory, ROWS).readMaxValue("kmu_width", "Int"))
                .isEqualTo("8");
        }
    }

    @Nested
    class ReadMinValue {

        @Test
        void theSlidersLowEndIsRead(@TempDir Path directory) throws IOException {

            assertThat(createTable(directory, ROWS).readMinValue("kmu_width", "Int"))
                .isEqualTo("1");
        }
    }

    @Nested
    class ReadOptions {

        @Test
        void optionsAreTrimmedAndEmptiesDropped(@TempDir Path directory) throws IOException {

            var table = createTable(directory, """
                kmu_spaced,,,,Spaced,,Radio,Gold,"Gold ,  Silver,",,,Which,,,,,General
                """);

            assertThat(table.readOptions("kmu_spaced"))
                .containsExactly("Gold", "Silver");
        }

        @Test
        void aRowThatIsNoRadioIsRefused(@TempDir Path directory) throws IOException {

            var table = createTable(directory, ROWS);

            assertThatThrownBy(() -> table.readOptions("kmu_width"))
                .isInstanceOf(AssertionError.class)
                .hasMessageEndingWith(" is declared Int but was read as Radio");
        }
    }

    @Nested
    class ReadRadioFieldIds {

        @Test
        void onlyRadioRowsAreRead(@TempDir Path directory) throws IOException {

            assertThat(createTable(directory, ROWS).readRadioFieldIds())
                .containsExactly("kmu_palette");
        }
    }

    @Nested
    class ReadTabsByFieldId {

        @Test
        void eachRowsTabIsReadInFileOrder(@TempDir Path directory) throws IOException {

            assertThat(createTable(directory, ROWS).readTabsByFieldId())
                .containsExactly(
                    Map.entry("kmu_caption", "General"),
                    Map.entry("kmu_note", "General"),
                    Map.entry("kmu_palette", "Visuals"),
                    Map.entry("kmu_width", "Visuals"));
        }
    }

    @Nested
    class ReadValueFieldIds {

        @Test
        void captionsAndProseAreLeftOut(@TempDir Path directory) throws IOException {

            assertThat(createTable(directory, ROWS).readValueFieldIds())
                .containsExactly("kmu_palette", "kmu_width");
        }
    }

    @Nested
    class DescribeDifferencesFrom {

        private static final FieldBehaviour RADIO =
            new FieldBehaviour("Radio", Optional.of("Gold"), "Gold, Silver", "", "");

        @Test
        void anIdenticalRowDiffersInNothing() {

            assertThat(RADIO.describeDifferencesFrom(RADIO))
                .isEmpty();
        }

        @Test
        void eachDifferingColumnIsNamedAsTheHeaderNamesIt() {

            var translated = new FieldBehaviour("Int", Optional.of("Or"), "Or, Argent", "1", "8");

            assertThat(translated.describeDifferencesFrom(RADIO))
                .containsExactly(
                    "fieldType: \"Int\" against \"Radio\"",
                    "defaultValue: \"Or\" against \"Gold\"",
                    "secondaryValue: \"Or, Argent\" against \"Gold, Silver\"",
                    "minValue: \"1\" against \"\"",
                    "maxValue: \"8\" against \"\"");
        }
    }
}
