package kmlib.settings;

import com.fs.starfarer.api.Global;

import lunalib.backend.ui.settings.LunaSettingsLoader;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.lazywizard.lazylib.JSONUtils;

import java.io.IOException;

/**
 * The live binding of {@link LunaSettingsStore}: LunaLib's own settings loader, whose per-mod store
 * is a LazyLib JSON object that the readers read from and {@code save()} rewrites whole.
 *
 * <p>The store is absent until LunaLib has loaded the mod's settings, which happens the first time
 * any setting of that mod is read. Before then every write here is a no-op, which is why the port's
 * operations answer whether they landed rather than assuming they did.
 *
 * <p>A failure is logged and reported, never thrown. What is at stake in a settings write is one
 * value's persistence, and a JSON or disk failure taking down the UI pass that made the write would
 * cost the player the whole screen to save one field.
 */
public final class LunaLibSettingsStore implements LunaSettingsStore {

    private static final Logger LOG = Global.getLogger(LunaLibSettingsStore.class);

    @Override
    public boolean hasStoreFor(String modId) {

        return findStoreFor(modId) != null;
    }

    @Override
    public boolean hasValue(String modId, String fieldId) {

        var store = findStoreFor(modId);

        return store != null && store.has(fieldId);
    }

    @Override
    public boolean putValue(String modId, String fieldId, Object value) {

        var store = findStoreFor(modId);
        if (store == null) {
            return false;
        }
        try {
            store.put(fieldId, value);
            return true;

        } catch (JSONException putFailed) {
            LOG.error("Failed to write LunaLib setting '"
                + fieldId
                + "' for mod '"
                + modId
                + "'",
                putFailed);
            return false;
        }
    }

    @Override
    public boolean removeValue(String modId, String fieldId) {

        var store = findStoreFor(modId);
        if (store == null || !store.has(fieldId)) {
            return false;
        }
        store.remove(fieldId);
        return true;
    }

    @Override
    public boolean saveStore(String modId) {

        var store = findStoreFor(modId);
        if (store == null) {
            return false;
        }
        try {
            store.save();
            return true;

        } catch (JSONException | IOException saveFailed) {
            LOG.error("Failed to save LunaLib settings for mod '" + modId + "'", saveFailed);
            return false;
        }
    }

    @Override
    public void announceSettingsChanged(String modId) {

        LunaSettings.reportSettingsChanged(modId);
    }

    // The mod's backing store as LunaLib holds it right now, looked up per operation rather than
    // kept: LunaLib replaces the object when it reloads a mod's settings, and a reference held over
    // that would be written into by nobody's readers.
    private static JSONUtils.CommonDataJSONObject findStoreFor(String modId) {

        return LunaSettingsLoader
            .getSettings()
            .get(modId);
    }
}
