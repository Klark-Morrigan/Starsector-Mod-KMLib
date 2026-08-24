package kmlib.testfixtures.starsector.settings;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
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
 * <p>Lives in the production source set rather than {@code src/test},
 * with the rest of {@code kmlib.testfixtures}, so consumer mods can
 * reach it through their {@code testCompileOnly} dependency on
 * {@code KMLib.jar} - the jar is consumed flat, with no Gradle variants
 * for a test-fixtures artifact to travel in. The class is never
 * instantiated and exposes only static entry points; players running
 * with KMLib enabled pay nothing for it at runtime.
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
     * Pluggable adapter for {@link SettingsAPI#getColor(String)}, for a test
     * whose subject reads a named engine colour and asserts on what it
     * builds from it. Implementations return the stored value or
     * {@code null} when the key is unknown, in which case the proxy falls
     * back to its default shade - so a test names only the keys it cares
     * about and every other lookup stays answerable.
     */
    @FunctionalInterface
    public interface SettingsColourSource {
        Color get(String key);
    }

    /** {@link SettingsColourSource} that leaves every key to the default shade. */
    public static final SettingsColourSource DEFAULT_COLOURS = key -> null;

    /**
     * Pluggable adapter for {@link com.fs.starfarer.api.ModManagerAPI#isModEnabled(String)}, for a
     * test whose subject gates on another mod being installed. Implementations answer for the mod
     * ids they know and {@code false} for the rest, which is what an install without those mods
     * reports.
     */
    @FunctionalInterface
    public interface EnabledModsSource {
        boolean isEnabled(String modId);
    }

    // What an unnamed colour key answers with. Opaque and unmistakable: a test that did not mean to read
    // a colour sees white rather than a plausible shade it might have asserted against by accident.
    private static final Color DEFAULT_COLOUR = Color.WHITE;

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
        installSettings(stringSource, DEFAULT_COLOURS);
    }

    /**
     * Installs the proxy with caller-supplied {@code getString} and
     * {@code getColor} resolvers. Use this overload when the subject
     * builds a shade out of a named engine colour: with every key
     * answering the same default, a derived colour and the value it was
     * derived from are indistinguishable, and the assertion pins nothing.
     */
    public static void installSettings(
            SettingsStringSource stringSource,
            SettingsColourSource colourSource) {
        installSettings(stringSource, colourSource, null);
    }

    /**
     * Installs the proxy carrying a mod manager, which the overloads above deliberately leave off:
     * a settings object whose {@code getModManager} answers null is the state a read taken before
     * the game is fully up meets, and a fixture that always supplied one could not stand for it.
     * Use this overload for a subject that gates on another mod being installed.
     */
    public static void installSettingsWithEnabledMods(EnabledModsSource enabledMods) {
        installSettings(EMPTY_STRINGS, DEFAULT_COLOURS, enabledMods);
    }

    public static void clearSettings() {
        Global.setSettings(null);
    }

    private static void installSettings(
            SettingsStringSource stringSource,
            SettingsColourSource colourSource,
            EnabledModsSource enabledMods) {
        Global.setSettings(settings(stringSource, colourSource, enabledMods));
    }

    private static SettingsAPI settings(
            SettingsStringSource stringSource,
            SettingsColourSource colourSource,
            EnabledModsSource enabledMods) {

        return proxy(SettingsAPI.class, (proxy, method, args) -> {
            // Null unless a caller asked for one, so the no-mod-manager state stays reachable -
            // see installSettingsWithEnabledMods.
            if ("getModManager".equals(method.getName())) {
                return enabledMods == null ? null : modManager(enabledMods);
            }
            // Misc.<clinit> reads several floats and a colour before any
            // test code runs; returning safe defaults keeps it quiet.
            if ("getFloat".equals(method.getName())) {
                return 1f;
            }
            if ("getColor".equals(method.getName())) {
                return resolveColour(colourSource, args);
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

    // A mod manager answering the caller's enablement rule and defaults for everything else, so a
    // subject asking one question of the mod set does not have to be handed a whole one.
    private static ModManagerAPI modManager(EnabledModsSource enabledMods) {

        return proxy(ModManagerAPI.class, (proxy, method, args) -> {
            if ("isModEnabled".equals(method.getName()) && args != null && args.length == 1) {
                return enabledMods.isEnabled((String) args[0]);
            }
            return defaultValue(method.getReturnType());
        });
    }

    // The colour a getColor call answers with: the caller's, where it named one for that key, and the
    // default shade otherwise - including for the key-less call shapes, since a source keyed by name has
    // nothing to say about those.
    private static Color resolveColour(SettingsColourSource colourSource, Object[] args) {

        if (args == null || args.length != 1 || !(args[0] instanceof String key)) {
            return DEFAULT_COLOUR;
        }
        var named = colourSource.get(key);

        return named == null
            ? DEFAULT_COLOUR
            : named;
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
