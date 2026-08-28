package kmlib.testfixtures.starsector.settings;

/**
 * Answers "is that mod installed?" for the length of one body - the gate every optional-mod
 * integration stands behind, and the three states a running game can put it in.
 *
 * <p>Shared across the KM mod series rather than written out per suite because each such
 * integration opens by settling the same hop down to the mod manager, and a suite spelling it out
 * itself is free to settle a different mod id than the subject asks for and still pass. It also
 * keeps the three states named: a mod that is off, a settings object with no mod manager on it,
 * and no settings at all are different halves of "not up yet", and a boolean argument would
 * flatten them at the call site.
 *
 * <p>Built on {@link StarsectorSettingsFake}'s proxy rather than on a static mock, so a case pays
 * no mock-maker cost and a consuming mod's tests need nothing beyond the fixtures they already
 * compile against. Each scope installs and clears around the body, so no case can leave the
 * settings it installed standing for the next one.
 *
 * <p>Published as a fixture variant with the rest of {@code kmlib.testfixtures}, for the reason
 * that package gives.
 */
public final class ModStateScopes {

    private ModStateScopes() {
    }

    /**
     * Runs body with the mod set readable and one named mod enabled or not, as asked. Every other
     * mod id reports disabled, so a subject reaching for one it was not given behaves as it does
     * without it.
     *
     * @param modId        the mod the body's subject asks about
     * @param isModEnabled what the mod manager reports for it
     * @param body         the case to run inside the scope
     */
    public static void runWithModEnabled(String modId, boolean isModEnabled, Runnable body) {

        runWithSettingsInstalled(
            () -> StarsectorSettingsFake.installSettingsWithEnabledMods(
                askedModId -> isModEnabled && modId.equals(askedModId)),
            body);
    }

    /**
     * Runs body with settings up but no mod manager on them - the half-built state between a game
     * that is up and one that is not, and the one a guard is easiest to leave out of.
     *
     * @param body the case to run inside the scope
     */
    public static void runWithoutModManager(Runnable body) {
        runWithSettingsInstalled(StarsectorSettingsFake::installSettings, body);
    }

    /**
     * Runs body with no settings at all - a read taken before the game has stood them up.
     *
     * @param body the case to run inside the scope
     */
    public static void runWithoutGameSettings(Runnable body) {
        runWithSettingsInstalled(StarsectorSettingsFake::clearSettings, body);
    }

    // Clears afterwards whatever the body did, including throwing: a fixture left installed is
    // read by whichever case runs next, and the failure that follows names that case rather than
    // this one.
    private static void runWithSettingsInstalled(Runnable installSettings, Runnable body) {

        installSettings.run();

        try {
            body.run();

        } finally {
            StarsectorSettingsFake.clearSettings();
        }
    }
}
