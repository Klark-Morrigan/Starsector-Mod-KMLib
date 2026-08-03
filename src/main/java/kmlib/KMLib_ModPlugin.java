package kmlib;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

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
        try {
            KmlibLunaSettings.installBindings();
        } catch (RuntimeException exception) {
            LOG.error("Failed to install KMLib LunaLib settings bindings", exception);
        }
        logActiveRenderer();
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
