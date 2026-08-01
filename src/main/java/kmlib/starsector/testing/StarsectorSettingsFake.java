package kmlib.starsector.testing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import java.awt.Color;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Single source of truth for the no-op {@link SettingsAPI} proxy that
 * tests across the KM mod series install into {@link Global} before
 * touching {@link com.fs.starfarer.api.util.Misc}. Misc's static
 * initialiser calls {@link Global#getSettings()} and trips an NPE the
 * moment Mockito's inline mock-maker triggers class loading, so any
 * test that wants to stub Misc must install a proxy first.
 *
 * <p>Lives in the production source set rather than {@code src/test}
 * so consumer mods can reach it through their {@code testCompileOnly}
 * dependency on {@code KMLib.jar}. The class is never instantiated and
 * exposes only static entry points; players running with KMLib enabled
 * pay nothing for it at runtime.
 *
 * <p>String lookups go through a {@link SettingsStringSource} so each
 * mod can plug in its own localisation map without KMLib having to
 * know every category that exists in the wider ecosystem.
 */
public final class StarsectorSettingsFake {
    private StarsectorSettingsFake() {
    }

    /**
     * Pluggable adapter for {@link SettingsAPI#getString(String, String)}.
     * Implementations should return the stored value or {@code null}
     * when the (category, key) pair is unknown - the proxy passes the
     * {@code null} straight back to the caller.
     */
    @FunctionalInterface
    public interface SettingsStringSource {
        String get(String category, String key);
    }

    /** {@link SettingsStringSource} that always reports an unknown key. */
    public static final SettingsStringSource EMPTY_STRINGS = (category, key) -> null;

    /**
     * Installs a no-op {@link SettingsAPI} proxy whose {@code getString}
     * always returns {@code null}. Sufficient for tests that only need
     * Misc to load without inspecting localised text.
     */
    public static void installSettings() {
        installSettings(EMPTY_STRINGS);
    }

    /**
     * Installs the proxy with a caller-supplied {@code getString}
     * resolver. Use the overload when tests assert on localised labels
     * that the resolver is expected to surface.
     */
    public static void installSettings(SettingsStringSource stringSource) {
        Global.setSettings(settings(stringSource));
    }

    public static void clearSettings() {
        Global.setSettings(null);
    }

    private static SettingsAPI settings(SettingsStringSource stringSource) {
        return proxy(SettingsAPI.class, (proxy, method, args) -> {
            // Misc.<clinit> reads several floats and a colour before any
            // test code runs; returning safe defaults keeps it quiet.
            if ("getFloat".equals(method.getName())) {
                return 1f;
            }
            if ("getColor".equals(method.getName())) {
                return Color.WHITE;
            }
            if ("getString".equals(method.getName())) {
                if (args != null && args.length == 2) {
                    return stringSource.get((String) args[0], (String) args[1]);
                }
                return null;
            }
            return defaultValue(method.getReturnType());
        });
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (char.class.equals(returnType)) {
            return '\0';
        }
        if (byte.class.equals(returnType)) {
            return (byte) 0;
        }
        if (short.class.equals(returnType)) {
            return (short) 0;
        }
        if (int.class.equals(returnType)) {
            return 0;
        }
        if (long.class.equals(returnType)) {
            return 0L;
        }
        if (float.class.equals(returnType)) {
            return 0f;
        }
        if (double.class.equals(returnType)) {
            return 0d;
        }
        return null;
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] {type},
            handler));
    }
}
