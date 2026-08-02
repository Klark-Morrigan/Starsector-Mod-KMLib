package kmlib;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

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
    }
}
