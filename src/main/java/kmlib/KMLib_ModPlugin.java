package kmlib;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.mods.nexerelin.NexerelinIntegration;
import kmlib.mods.rat.RandomAssortmentOfThingsIntegration;
import kmlib.opengl.FastRendering;
import kmlib.settings.KmlibLunaSettings;
import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.starsector.compatibility.CompatibilityNotice;
import kmlib.starsector.scripts.SectorScripts;

import org.apache.log4j.Logger;

/**
 * KMLib's entry point, for the little the library has to do on its own behalf rather than on a
 * consuming mod's: applying its own log verbosity from its own setting, and telling the player when
 * a binding it holds to third-party code has stopped holding.
 *
 * <p>Everything else here is called into by whichever mod wants it. Those two are the exceptions
 * because no consuming mod can do either for it: a level has to be applied to the {@code kmlib}
 * logger subtree before anything under it logs, and a mod applying one would retune the library for
 * every other mod in the same game; and a binding the library holds is the library's to report, not
 * something one mod among several should be answering for.
 *
 * <p>A failed step is logged rather than thrown: the library's verbosity falling back to log4j's
 * default is a diagnostic inconvenience, and taking down every mod that depends on KMLib over it
 * would be wildly out of proportion.
 */
public class KMLib_ModPlugin extends BaseModPlugin {

    private static final Logger LOG = Global.getLogger(KMLib_ModPlugin.class);

    @Override
    public void onApplicationLoad() {

        installLunaLibSettingsBindings();
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
    static void installCompatibilityNotice(SectorAPI sector) {

        installGuarded(
            "compatibility notice",
            () -> SectorScripts.installTransientScript(
                sector,
                () -> new CompatibilityNotice(sector, CompatibilityFailures.SESSION_RECORD)));
    }

    private static void installLunaLibSettingsBindings() {

        try {
            KmlibLunaSettings.installBindings();

        } catch (RuntimeException exception) {
            LOG.error("Failed to install KMLib LunaLib settings bindings", exception);
        }
    }

    // Puts the adapters for whichever optional mods this install has in front of the operations
    // that may defer to them. It happens here because which mods are present is a fact about the
    // install, and an operation asking that question for itself would be naming a mod it has no
    // business knowing about - so the composing is done once, where a mod's entry point already is.
    private static void installOptionalModIntegrations() {

        installGuarded(
            "Nexerelin routines",
            NexerelinIntegration::installRoutines);

        installGuarded(
            "Random Assortment of Things system access routes",
            RandomAssortmentOfThingsIntegration::installModdedSystemAccessRoutes);
    }

    // One guarded step per thing installed, so a step that throws costs only what it was installing
    // rather than every step that had not been reached yet.
    //
    // For an integration, a failure leaves the library running its own sequences rather than a
    // mod's, which is the behaviour of an install without that mod - a worse colony than the player
    // expected, and a far better outcome than taking down every mod that depends on KMLib.
    private static void installGuarded(String integrationDescription, Runnable installation) {

        try {
            installation.run();

        } catch (RuntimeException exception) {
            LOG.error("Failed to install KMLib " + integrationDescription, exception);
        }
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
