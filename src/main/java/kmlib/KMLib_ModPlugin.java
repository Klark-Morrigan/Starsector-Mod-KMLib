package kmlib;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

import kmlib.mods.nexerelin.NexerelinIntegration;
import kmlib.mods.rat.RandomAssortmentOfThingsIntegration;
import kmlib.opengl.FastRendering;
import kmlib.settings.KmlibLunaSettings;

import org.apache.log4j.Logger;

/**
 * KMLib's entry point, which exists for one reason: applying the library's own log verbosity from
 * its own setting.
 *
 * <p>Everything else here is called into by whichever mod wants it, so the library had no load-time
 * work and no plugin until this. Its logging is the exception - a level has to be applied to the
 * {@code kmlib} logger subtree before anything under it logs, and no consuming mod can do that
 * without retuning the library for every other mod in the same game.
 *
 * <p>A failed binding is logged rather than thrown: the library's verbosity falling back to log4j's
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
            RandomAssortmentOfThingsIntegration::installSystemAccessRoutes);
    }

    // One guarded step per integration, so a mod whose registration throws costs only its own
    // adapters rather than every adapter that had not been reached yet.
    //
    // A failure leaves the library running its own sequences rather than a mod's, which is the
    // behaviour of an install without that mod - a worse colony than the player expected, and a
    // far better outcome than taking down every mod that depends on KMLib.
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
