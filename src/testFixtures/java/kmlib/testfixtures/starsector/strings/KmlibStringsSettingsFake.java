package kmlib.testfixtures.starsector.strings;

import com.fs.starfarer.api.Global;

import kmlib.starsector.strings.KmlibStringKeys;
import kmlib.testfixtures.starsector.StubbedGlobalLogger;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake.SettingsStringSource;

import org.mockito.MockedStatic;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * KMLib's own strings standing in for the game's, so a suite asserts on the wording a player reads rather than on
 * {@code [REDACTED]}: the console text KMLib's commands print, which a mod's own commands print too where they
 * build on KMLib's. Restated here rather than read off the shipped file, which keeps a unit test off the disk;
 * KMLib's own strings suite holds the two together.
 */
public final class KmlibStringsSettingsFake {

    private static final Map<String, String> STRINGS_BY_KEY = buildStringsByKey();

    /**
     * Answers KMLib's category alone, so a lookup in another category reads as the unknown key it is. Public for
     * a mod's own strings fake to answer KMLib's category through, beside its own.
     */
    public static final SettingsStringSource KMLIB_STRINGS = (category, key) ->
        KmlibStringKeys.CATEGORY.equals(category)
            ? STRINGS_BY_KEY.get(key)
            : null;

    private KmlibStringsSettingsFake() {
    }

    public static void clearSettings() {
        StarsectorSettingsFake.clearSettings();
    }

    public static void installSettings() {
        StarsectorSettingsFake.installSettings(KMLIB_STRINGS);
    }

    /**
     * Opens a static mock of {@link Global} answering loggers as {@link StubbedGlobalLogger} does, and these
     * strings. A static mock answers null for every read it is not told about, the settings included, so a
     * suite mocking {@code Global} would otherwise read every string as the fallback sentinel.
     *
     * @return the open mock, which the caller closes, then clears the settings with {@link #clearSettings()}
     */
    public static MockedStatic<Global> openGlobalAnsweringStrings() {

        installSettings();

        // Read before the mock opens, which would answer the read itself.
        var settings = Global.getSettings();
        var globalMock = StubbedGlobalLogger.openGlobalAnsweringLoggers();

        globalMock
            .when(Global::getSettings)
            .thenReturn(settings);

        return globalMock;
    }

    /**
     * The wording this fake answers with, for the walk that holds it against the shipped strings file.
     *
     * @return each stubbed key's wording
     */
    public static Map<String, String> readStringsByKey() {
        return Map.copyOf(STRINGS_BY_KEY);
    }

    private static Map<String, String> buildStringsByKey() {

        var stringsByKey = new LinkedHashMap<String, String>();

        stringsByKey.put(KmlibStringKeys.CONSOLE_COLONY_ALREADY_OWNED, "%s is already owned by %s.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_COLONY_FOUNDED, "Founded a colony on %s for %s.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_COLONY_TRANSFERRED, "Transferred %s to %s.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_ENTITIES_FLEETS, "Fleets (nearest center first)");
        stringsByKey.put(KmlibStringKeys.CONSOLE_ENTITIES_HEADING, "System entities in %s:");
        stringsByKey.put(KmlibStringKeys.CONSOLE_ENTITIES_HEADING_GATES_ONLY, "System entities in %s (gates only):");
        stringsByKey.put(KmlibStringKeys.CONSOLE_ENTITIES_ORBIT, "dist=%.0f  speed=%.2f deg/day");
        stringsByKey.put(KmlibStringKeys.CONSOLE_ENTITIES_UNNAMED, "(unnamed)");
        stringsByKey.put(KmlibStringKeys.CONSOLE_ENTITIES_UNORBITED, "Unorbited entities (nearest center first)");
        stringsByKey.put(KmlibStringKeys.CONSOLE_EXPECTED_ONE_OF, "one of %s");
        stringsByKey.put(KmlibStringKeys.CONSOLE_FACTIONS_ATTITUDE_SELF, "(self)");
        stringsByKey.put(KmlibStringKeys.CONSOLE_FACTIONS_HEADER, "Factions and their holdings");
        stringsByKey.put(KmlibStringKeys.CONSOLE_FACTIONS_HEADER_WITHOUT_HOLDINGS, "Factions");
        stringsByKey.put(KmlibStringKeys.CONSOLE_FACTIONS_HEADING, "%s:");
        stringsByKey.put(KmlibStringKeys.CONSOLE_FACTIONS_HEADING_FILTERED, "%s (%s):");
        stringsByKey.put(KmlibStringKeys.CONSOLE_FACTIONS_HOLDINGS, "holdings: %d");
        stringsByKey.put(KmlibStringKeys.CONSOLE_FACTIONS_HOLDINGS_SYSTEMS, "systems: %s");
        stringsByKey.put(KmlibStringKeys.CONSOLE_FACTIONS_HOLDINGS_VISIBILITY, "(%d hidden, %d discoverable)");
        stringsByKey.put(KmlibStringKeys.CONSOLE_FACTIONS_HYPERSPACE, "(hyperspace)");
        stringsByKey.put(KmlibStringKeys.CONSOLE_FACTIONS_SOURCE, "from: %s");
        stringsByKey.put(KmlibStringKeys.CONSOLE_FACTIONS_TERRITORIAL, "[territorial]");
        stringsByKey.put(KmlibStringKeys.CONSOLE_FACTIONS_UNATTRIBUTED, "(unattributed)");
        stringsByKey.put(KmlibStringKeys.CONSOLE_FACTIONS_WRITTEN_TO_LOG, "Faction listing written to the game log.");
        stringsByKey.put(
            KmlibStringKeys.CONSOLE_GATE_ACTIVATED,
            "Activated gate '%s' in %s; it lights up on its next advance.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_GATE_NOT_FOUND, "No gate with ID '%s' in %s.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_LISTING_NONE, "(none)");
        stringsByKey.put(
            KmlibStringKeys.CONSOLE_PARAMETER_FLAG_GIVEN_VALUE,
            "'%s' is a flag; give it on its own, without '='.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_PARAMETER_INVALID_VALUE, "Invalid %s '%s'. Expected %s.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_PARAMETER_MISSING, "Missing required parameter '%s'.");
        stringsByKey.put(
            KmlibStringKeys.CONSOLE_PARAMETER_STANDALONE_COMBINED,
            "'%s' cannot be combined with other arguments. %s");
        stringsByKey.put(KmlibStringKeys.CONSOLE_PARAMETER_TOO_MANY, "Too many arguments. %s");
        stringsByKey.put(KmlibStringKeys.CONSOLE_PARAMETER_UNKNOWN, "Unknown parameter '%s'. Use %s.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_PARAMETER_USAGE, "Usage: %s.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_PROFILING_EXPECTED_TOP, "a whole number of rows above zero");
        stringsByKey.put(
            KmlibStringKeys.CONSOLE_PROFILING_TIMINGS_LOGGED,
            "%s timings written to the game log (starsector-core/starsector.log).");
        stringsByKey.put(KmlibStringKeys.CONSOLE_PROFILING_TIMINGS_RESET, "%s timings reset.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_REQUIRES_CAMPAIGN, "This command can only run in a campaign.");
        stringsByKey.put(
            KmlibStringKeys.CONSOLE_REQUIRES_STAR_SYSTEM,
            "This command must be run inside a star system.");
        stringsByKey.put(
            KmlibStringKeys.CONSOLE_SPAWN_AMBIGUOUS_FOCUS,
            "This system has %d stars (%s). Re-run with a focus ID - one of those star IDs, or any entity ID in the"
                + " system.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_SPAWN_EXPECTED_JITTER, "a non-negative fraction (e.g. 0.25)");
        stringsByKey.put(KmlibStringKeys.CONSOLE_SPAWN_EXPECTED_SPEED, "a number in degrees per day");
        stringsByKey.put(KmlibStringKeys.CONSOLE_SPAWN_FOCUS_NOT_FOUND, "No entity with ID '%s' in %s.");
        stringsByKey.put(
            KmlibStringKeys.CONSOLE_SPAWN_GATE_ADDED,
            "Added an inactive gate (ID %s) at your fleet position. Activate it with the gate-activation command"
                + " and this ID.");
        stringsByKey.put(
            KmlibStringKeys.CONSOLE_SPAWN_JUMP_POINT_ADDED,
            "Spawned %s at your fleet position in %s, generated its hyperspace entrance, and cleared the cut-off"
                + " tag.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_SPAWN_UNKNOWN_KIND, "Unknown entity type '%s'. Supported: %s.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_SPOILERS_CUT_OFF, "[cut off]");
        stringsByKey.put(
            KmlibStringKeys.CONSOLE_SPOILERS_HEADING,
            "Map spoilers - cut-off systems and undiscovered markets:");
        stringsByKey.put(KmlibStringKeys.CONSOLE_SPOILERS_UNDISCOVERED, "[undiscovered]");
        stringsByKey.put(KmlibStringKeys.CONSOLE_TARGET_ENTITY_NOT_FOUND, "No entity with ID '%s' in the sector.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_TARGET_ENTITY_WITHOUT_MARKET, "Entity '%s' has no market.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_TARGET_FACTION_NOT_FOUND, "No faction with ID '%s'.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_TARGET_MARKET_INELIGIBLE, "The market on '%s' is not %s.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_TARGET_NO_ELIGIBLE_MARKET, "Nothing in %s is %s.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_TARGET_NO_SECTOR_FOR_FACTIONS, "No sector to read factions from.");
        stringsByKey.put(KmlibStringKeys.CONSOLE_TARGET_NO_SECTOR_FOR_MARKETS, "No sector to search.");
        stringsByKey.put(
            KmlibStringKeys.CONSOLE_TARGET_NO_STAR_SYSTEM,
            "Not in a star system - name an entity ID to point at a place directly.");
        stringsByKey.put(
            KmlibStringKeys.CONSOLE_TARGET_REQUIREMENT_COLONISABLE_BODY,
            "a body ready for colonisation");
        stringsByKey.put(KmlibStringKeys.CONSOLE_TARGET_REQUIREMENT_EXISTING_COLONY, "an existing colony");

        return stringsByKey;
    }
}
