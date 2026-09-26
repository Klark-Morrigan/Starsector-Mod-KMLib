package kmlib;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.mods.nexerelin.NexerelinIntegration;
import kmlib.mods.nexerelin.NexerelinPresence;
import kmlib.mods.rat.RandomAssortmentOfThingsIntegration;
import kmlib.mods.rat.RandomAssortmentOfThingsPresence;
import kmlib.opengl.FastRendering;
import kmlib.settings.KmlibLunaSettings;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.starsector.compatibility.CompatibilityNotice;
import kmlib.starsector.compatibility.ModIntegration;
import kmlib.starsector.scripts.SectorScripts;
import kmlib.starsector.startup.WiringSteps;
import kmlib.starsector.strings.KmlibStringKeys;

import org.apache.log4j.Logger;

/**
 * KMLib's entry point, for the little the library has to do on its own behalf rather than on a
 * consuming mod's: applying its own log verbosity from its own setting, standing up the adapters
 * for whichever optional mods this install has, and telling the player when a binding it holds to
 * third-party code has stopped holding.
 *
 * <p>Everything else here is called into by whichever mod wants it. Those are the exceptions
 * because no consuming mod can do them for it: a level has to be applied to the {@code kmlib}
 * logger subtree before anything under it logs, and a mod applying one would retune the library for
 * every other mod in the same game; and a binding the library holds is the library's to report, not
 * something one mod among several should be answering for.
 *
 * <p>A failed step is logged rather than thrown, and where the step was an integration with another
 * mod it is also reported to the player. Throwing would take down every mod that depends on KMLib
 * over one registration, which is wildly out of proportion; logging alone would leave a player who
 * enabled a mod to discover by playing that the mod did not integrate. So the library is a consumer
 * of its own compatibility channel here, filing under its own ID the way a consuming mod files
 * under its.
 */
public class KMLib_ModPlugin extends BaseModPlugin {

    private static final Logger LOG = Global.getLogger(KMLib_ModPlugin.class);

    // The guard every start-up step below runs behind. Logging as this entry point rather than as
    // the guard's own package is what keeps a failed step under the kmlib subtree, which is the
    // switch a reader chasing the library's output turns up.
    private static final WiringSteps WIRING_STEPS = new WiringSteps(LOG);

    // Which of the library's own features each integration serves, as the half of a latch key the
    // mod ID does not cover. One per integration, so the library losing two things to two third
    // parties is two reports rather than the first one and silence.
    private static final String LUNALIB_SETTINGS_FEATURE_KEY = "lunalib-settings";
    private static final String NEXERELIN_ROUTINES_FEATURE_KEY = "nexerelin-routines";
    private static final String RAT_ACCESS_ROUTES_FEATURE_KEY = "system-access-routes";

    @Override
    public void onApplicationLoad() {

        WIRING_STEPS.runGuardedStep(
            KmlibLunaSettings::installBindings,
            "Failed to install KMLib LunaLib settings bindings",
            KMLib_ModPlugin::describeLunaLibIntegration);
        logActiveRenderer();
        installOptionalModIntegrations();
    }

    @Override
    public void onGameLoad(boolean newGame) {

        super.onGameLoad(newGame);
        installCompatibilityNotice(Global.getSector());
    }

    // Puts the reporter on the loaded sector, so what the record holds reaches the player. Per load
    // rather than once at application load: the notice is transient - it opens a dialog on a
    // campaign UI, which is the loaded sector's - and a transient script does not survive a load.
    // The record it drains is the process's, so a failure recorded before this sector existed is
    // waiting for the first notice that runs.
    //
    // Guarded without an integration named, alone among the steps here. This step binds to nothing
    // a player could act on, and it is the reporter: a failure to install it has nowhere to be
    // reported to, since what would carry the report is the thing that did not install.
    static void installCompatibilityNotice(SectorAPI sector) {

        WIRING_STEPS.runGuardedStep(
            () -> SectorScripts.installTransientScript(
                sector,
                () -> new CompatibilityNotice(sector, CompatibilityFailures.SESSION_RECORD)),
            "Failed to install the KMLib compatibility notice");
    }

    // The three integrations as the channel states one: the third party it is with, and the library
    // as the mod that loses something by it. Composed when a step has already failed rather than
    // held as constants, so the wording read out of strings.json - and the mod manager read behind
    // the installed version - stay off the load path of every install where the step worked.
    //
    // Open to the suite because a transposition here is invisible from everywhere else: a report
    // filed under the wrong mod's ID, or carrying the wrong feature's sentence, composes and reads
    // as plausibly as the right one and reaches a player naming a mod that was working.
    static ModIntegration describeLunaLibIntegration() {

        return new ModIntegration(
            KmlibLunaSettings.LUNALIB_MOD_ID,
            KmlibLunaSettings.LUNALIB_MOD_NAME,
            new CompatibilityConsumer(
                KmlibMod.MOD_ID,
                LUNALIB_SETTINGS_FEATURE_KEY,
                KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_LOST_LUNALIB_SETTINGS),
                KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_UNAFFECTED_LUNALIB_SETTINGS)));
    }

    static ModIntegration describeNexerelinIntegration() {

        return new ModIntegration(
            NexerelinPresence.MOD_ID,
            NexerelinPresence.MOD_NAME,
            new CompatibilityConsumer(
                KmlibMod.MOD_ID,
                NEXERELIN_ROUTINES_FEATURE_KEY,
                KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_LOST_NEXERELIN_ROUTINES),
                KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_UNAFFECTED_NEXERELIN_ROUTINES)));
    }

    static ModIntegration describeRandomAssortmentOfThingsIntegration() {

        return new ModIntegration(
            RandomAssortmentOfThingsPresence.MOD_ID,
            RandomAssortmentOfThingsPresence.MOD_NAME,
            new CompatibilityConsumer(
                KmlibMod.MOD_ID,
                RAT_ACCESS_ROUTES_FEATURE_KEY,
                KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_LOST_RAT_ACCESS_ROUTES),
                KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_UNAFFECTED_RAT_ACCESS_ROUTES)));
    }

    // Puts the adapters for whichever optional mods this install has in front of the operations
    // that may defer to them. It happens here because which mods are present is a fact about the
    // install, and an operation asking that question for itself would be naming a mod it has no
    // business knowing about - so the composing is done once, where a mod's entry point already is.
    //
    // One guarded step per integration, so a step that throws costs only what it was installing
    // rather than every step that had not been reached yet. For an integration, that leaves the
    // library running its own sequences rather than a mod's, which is the behaviour of an install
    // without that mod - a worse colony than the player expected, and the reason they are told.
    //
    // Each describer is handed to the installation as well as to its guard. An adapter reaches its
    // mod's types only when first called, so a mod that changed underneath it usually fails there
    // rather than here - and one describer for both is what makes the two one report.
    private static void installOptionalModIntegrations() {

        WIRING_STEPS.runGuardedStep(
            () -> NexerelinIntegration.installRoutines(KMLib_ModPlugin::describeNexerelinIntegration),
            "Failed to install KMLib Nexerelin routines",
            KMLib_ModPlugin::describeNexerelinIntegration);

        WIRING_STEPS.runGuardedStep(
            () -> RandomAssortmentOfThingsIntegration.installModdedSystemAccessRoutes(
                KMLib_ModPlugin::describeRandomAssortmentOfThingsIntegration),
            "Failed to install KMLib Random Assortment of Things system access routes",
            KMLib_ModPlugin::describeRandomAssortmentOfThingsIntegration);
    }

    // Which GL implementation every KM draw call reaches, stated once at load. It changes what a
    // GL hint does and what a state read answers, so it is the standing condition any rendering
    // report is read under - and a report that does not say which stack produced it cannot be
    // compared with one from the other.
    //
    // Logged here rather than left to the one binding that already reports it: that line is a
    // side effect of the map transform being read for a hover, so it appears only in a session
    // that hovered the map, and it names the reader it picked rather than the renderer underneath.
    private static void logActiveRenderer() {
        LOG.info("Active GL renderer resolved; fastRendering="
            + FastRendering.isFastRenderingActive());
    }
}
