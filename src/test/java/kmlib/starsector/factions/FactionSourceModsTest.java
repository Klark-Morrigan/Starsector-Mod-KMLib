package kmlib.starsector.factions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.SettingsAPI;

import kmlib.starsector.settings.modmanager.ModSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the recovery of where a faction came from: that a row's folder is matched to the mod
 * installed in it and carries that mod's own id, that the base game's folder-less rows are named as
 * such, that an unmatched folder still names itself but offers no id, and that the faction id comes
 * out of the faction file rather than off its name.
 *
 * <p>A mod's folder and its id are posed as different strings throughout, because they usually are
 * and because the match is made on one while the answer carries the other - posing them alike would
 * pass whichever of the two the code actually used.
 *
 * <p>Rows are posed the way the game's CSV merger writes them - an absolute install path, in the
 * separators the platform hands the loader, joined to the spreadsheet path - because the parsing of
 * that string is half of what is under test, and a row posed in the shape the parser happens to
 * want would pin nothing.
 */
final class FactionSourceModsTest {

    private static final String FACTIONS_CSV_PATH = "data/world/factions/factions.csv";

    @AfterEach
    void tearDown() {
        Global.setSettings(null);
    }

    @Nested
    class ReadSourcesByFactionId {

        @Test
        void namesTheModInstalledInTheFolderARowWasReadFrom() {

            var data = new GameDataFixture();

            data.enableMod("tahlan", "Tahlan Shipworks", "tahlan_shipworks");
            data.declareFaction(
                "C:\\Games\\Starsector\\starsector-core\\..\\mods\\tahlan",
                "data/world/factions/tahlan_greathouses.faction",
                "tahlan_greathouses");
            data.install();

            assertThat(FactionSourceMods.readSourcesByFactionId())
                .containsExactly(modEntry("tahlan_greathouses", "Tahlan Shipworks", "tahlan_shipworks"));
        }

        @Test
        void namesTheBaseGameForARowCarryingNoFolder() {
            // The base game loads out of the working directory rather than a named folder, and the
            // merger spells that absence out rather than leaving the column empty.
            var data = new GameDataFixture();

            data.declareBaseGameFaction("data/world/factions/hegemony.faction", "hegemony");
            data.install();

            assertThat(FactionSourceMods.readSourcesByFactionId())
                .containsExactly(unidentifiedEntry("hegemony", "vanilla"));
        }

        @Test
        void namesTheFolderItselfWhenNoEnabledModIsInstalledInIt() {
            // A source the mod manager does not account for is still worth placing: the folder is
            // what a player would go and look in.
            var data = new GameDataFixture();

            data.declareFaction(
                "C:\\Games\\Starsector\\starsector-core\\..\\mods\\some_unlisted_mod",
                "data/world/factions/unlisted.faction",
                "unlisted");
            data.install();

            assertThat(FactionSourceMods.readSourcesByFactionId())
                .containsExactly(unidentifiedEntry("unlisted", "some_unlisted_mod"));
        }

        @Test
        void keysAFactionByTheIdItsFileDeclaresRatherThanByTheFileName() {
            // The loader reads the id out of the file, so a listing keyed off the file name would
            // disagree with the sector about which faction a row declared.
            var data = new GameDataFixture();

            data.enableMod("tahlan", "Tahlan Shipworks", "tahlan_shipworks");
            data.declareFaction(
                "C:\\Games\\Starsector\\starsector-core\\..\\mods\\tahlan",
                "data/world/factions/tahlan_cieveFaction.faction",
                "tahlan_cieve");
            data.install();

            assertThat(FactionSourceMods.readSourcesByFactionId())
                .containsExactly(modEntry("tahlan_cieve", "Tahlan Shipworks", "tahlan_shipworks"));
        }

        @Test
        void namesTheFolderWhileTheSettingsCarryNoModManager() {
            // The half-built state between a game that is up and one that is not: the folder is
            // still there to report, so the read answers rather than giving up on every row.
            var data = new GameDataFixture();

            data.declareFaction(
                "C:\\Games\\Starsector\\starsector-core\\..\\mods\\tahlan",
                "data/world/factions/tahlan_greathouses.faction",
                "tahlan_greathouses");
            data.installWithoutModManager();

            assertThat(FactionSourceMods.readSourcesByFactionId())
                .containsExactly(unidentifiedEntry("tahlan_greathouses", "tahlan"));
        }

        @Test
        void namesTheBaseGameForARowCarryingNoSourceColumnAtAll() {
            // Not a shape the merger writes - it stamps the column onto every row it emits - but
            // the answer is the same one a folder-less row gets rather than a blank or a throw, and
            // which of those it is decides what a reader sees if the engine ever stops stamping it.
            var data = new GameDataFixture();

            data.declareFactionWithoutSourceColumn(
                "data/world/factions/hegemony.faction",
                "hegemony");
            data.install();

            assertThat(FactionSourceMods.readSourcesByFactionId())
                .containsExactly(unidentifiedEntry("hegemony", "vanilla"));
        }

        @Test
        void skipsARowNamingNoFactionFile() {
            // A trailing blank line in a mod's spreadsheet is an ordinary thing to ship, and it
            // declares nothing.
            var data = new GameDataFixture();

            data.declareNothing("C:\\Games\\Starsector\\starsector-core\\..\\mods\\tahlan");
            data.install();

            assertThat(FactionSourceMods.readSourcesByFactionId())
                .isEmpty();
        }

        @Test
        void skipsAFactionFileThatWillNotOpen() {
            // The loader reads the same file the same way, so such a row became no faction either
            // - there is nothing in the sector for it to have named.
            var data = new GameDataFixture();

            data.declareUnreadableFaction(
                "C:\\Games\\Starsector\\starsector-core\\..\\mods\\tahlan",
                "data/world/factions/broken.faction");
            data.install();

            assertThat(FactionSourceMods.readSourcesByFactionId())
                .isEmpty();
        }

        @Test
        void reportsNothingBeforeTheGameSettingsAreUp() {
            // A read taken outside a running game, which has no data to consult and must answer
            // rather than throw.
            assertThat(FactionSourceMods.readSourcesByFactionId())
                .isEmpty();
        }

        @Test
        void reportsNothingWhenTheSpreadsheetWillNotOpen() {
            // The listing this feeds is worth less without it and is not worth taking down over
            // it.
            new GameDataFixture().installWithUnreadableSpreadsheet();

            assertThat(FactionSourceMods.readSourcesByFactionId())
                .isEmpty();
        }
    }

    // A mod source: named, and carrying the id another command would take.
    private static Map.Entry<String, ModSource> modEntry(
            String factionId,
            String modName,
            String modId) {

        return Map.entry(factionId, new ModSource(modName, modId));
    }

    // A source that is not an installed mod, and so has no id to carry. Map.entry
    // rejects a null value, so the pair is built rather than named.
    private static Map.Entry<String, ModSource> unidentifiedEntry(
            String factionId,
            String sourceName) {

        return Map.entry(factionId, new ModSource(sourceName, null));
    }

    /**
     * The three reads the source map is built from, posed together: the merged factions
     * spreadsheet, the faction files its rows name, and the enabled mods.
     *
     * <p>Each faction is declared once, which fills the spreadsheet and the file behind it in one
     * go - the two agreeing is a precondition of the game running at all, and a case free to set
     * them against each other would pose an install that cannot exist.
     */
    private static final class GameDataFixture {

        private final List<JSONObject> declarationRows = new ArrayList<>();
        private final List<ModSpecAPI> enabledMods = new ArrayList<>();
        private final Map<String, String> factionIdsByFilePath = new LinkedHashMap<>();

        private void declareBaseGameFaction(String factionFilePath, String factionId) {
            declareFaction(null, factionFilePath, factionId);
        }

        private void declareFaction(
                String sourceFolderPath,
                String factionFilePath,
                String factionId) {

            factionIdsByFilePath.put(factionFilePath, factionId);
            declarationRows.add(buildRow(sourceFolderPath, factionFilePath));
        }

        // A row the merger never stamped its source column onto.
        private void declareFactionWithoutSourceColumn(String factionFilePath, String factionId) {

            factionIdsByFilePath.put(factionFilePath, factionId);

            var row = new JSONObject();

            try {
                row.put("faction", factionFilePath);

            } catch (JSONException putFailed) {
                // The put call declares it for keys and values this never poses.
                throw new IllegalStateException(putFailed);
            }
            declarationRows.add(row);
        }

        // A row whose file column is blank, as a spreadsheet's trailing empty line arrives.
        private void declareNothing(String sourceFolderPath) {
            declarationRows.add(buildRow(sourceFolderPath, ""));
        }

        // A row naming a file no read will answer for, which is what a malformed .faction is.
        private void declareUnreadableFaction(String sourceFolderPath, String factionFilePath) {
            declarationRows.add(buildRow(sourceFolderPath, factionFilePath));
        }

        // The folder and the id are separate on purpose: they are rarely the same string, and a
        // fixture conflating them could not pose the folder-to-mod match this is built to pin.
        private void enableMod(String modFolderName, String modName, String modId) {

            var modSpecMock = mock(ModSpecAPI.class);

            when(modSpecMock.getDirName())
                .thenReturn(modFolderName);
            when(modSpecMock.getId())
                .thenReturn(modId);
            when(modSpecMock.getName())
                .thenReturn(modName);

            enabledMods.add(modSpecMock);
        }

        private void install() {
            installSettings(buildSettings(true), true);
        }

        private void installWithoutModManager() {
            installSettings(buildSettings(true), false);
        }

        private void installWithUnreadableSpreadsheet() {
            installSettings(buildSettings(false), true);
        }

        // The row the merger writes: the file the row names, plus the folder it was read from
        // joined to the spreadsheet's own path. A folder the source has none of arrives stringified
        // rather than absent, which is how the base game's rows are told apart.
        private JSONObject buildRow(String sourceFolderPath, String factionFilePath) {

            var row = new JSONObject();

            try {
                row.put("faction", factionFilePath);
                row.put("fs_rowSource", sourceFolderPath + "/" + FACTIONS_CSV_PATH);

            } catch (JSONException putFailed) {
                // The put calls declare it for keys and values this never poses.
                throw new IllegalStateException(putFailed);
            }
            return row;
        }

        private SettingsAPI buildSettings(boolean isSpreadsheetReadable) {

            var settingsMock = mock(SettingsAPI.class);

            try {
                if (isSpreadsheetReadable) {
                    when(settingsMock.getMergedSpreadsheetData(anyString(), anyString()))
                        .thenAnswer(call -> new JSONArray(declarationRows));
                } else {
                    when(settingsMock.getMergedSpreadsheetData(anyString(), anyString()))
                        .thenThrow(new IOException("the spreadsheet will not open"));
                }
                when(settingsMock.getMergedJSON(anyString()))
                    .thenAnswer(call -> readFactionFile(call.getArgument(0)));

            } catch (Exception stubbingFailed) {
                // The stubbed calls declare checked kinds; none is thrown while stubbing them.
                throw new IllegalStateException(stubbingFailed);
            }
            return settingsMock;
        }

        private void installSettings(SettingsAPI settings, boolean carriesModManager) {

            if (carriesModManager) {

                var modManagerMock = mock(ModManagerAPI.class);

                when(modManagerMock.getEnabledModsCopy())
                    .thenReturn(enabledMods);
                when(settings.getModManager())
                    .thenReturn(modManagerMock);
            }
            Global.setSettings(settings);
        }

        private JSONObject readFactionFile(String factionFilePath)
                throws IOException, JSONException {

            var factionId = factionIdsByFilePath.get(factionFilePath);

            if (factionId == null) {
                throw new IOException("the faction file will not open");
            }
            var factionFile = new JSONObject();

            factionFile.put("id", factionId);

            return factionFile;
        }
    }
}
