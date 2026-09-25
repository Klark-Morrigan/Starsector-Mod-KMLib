package kmlib.testfixtures.starsector.settings;

import kmlib.testfixtures.starsector.settings.LunaSettingsTable.FieldBehaviour;

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
 * Pins the readings that split a settings row into what it does and what it says: its behaviour, the
 * text the screen draws, and the tab it is placed on.
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
