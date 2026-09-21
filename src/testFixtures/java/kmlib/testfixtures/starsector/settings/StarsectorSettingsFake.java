package kmlib.testfixtures.starsector.settings;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.Color;
import java.lang.reflect.Array;
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
 * <p>The class is never instantiated and exposes only static entry points.
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
     * IDs they know and {@code false} for the rest, which is what an install without those mods
     * reports.
     */
    @FunctionalInterface
    public interface EnabledModsSource {
        boolean isEnabled(String modId);
    }

    /**
     * Pluggable adapter for {@link com.fs.starfarer.api.ModSpecAPI#getName()}, for a test whose
     * subject shows a mod's own name rather than the ID it holds. Implementations answer for the
     * mod IDs they know and null for the rest, which is what the game answers for an ID naming no
     * installed mod.
     */
    @FunctionalInterface
    public interface ModNamesSource {
        String nameOf(String modId);
    }

    /** {@link ModNamesSource} for a mod manager that knows no mod by name. */
    public static final ModNamesSource NO_NAMED_MODS = modId -> null;

    /**
     * Pluggable adapter for the element a panel built through {@link SettingsAPI#createCustom}
     * hands back from {@code createUIElement}, for a test whose subject makes a tooltip surface of
     * its own rather than being handed one. That surface never reaches the caller, so the element
     * named here is the only place such an attachment can be observed from outside.
     */
    @FunctionalInterface
    public interface UiElementSource {
        TooltipMakerAPI createElement();
    }

    /** {@link UiElementSource} for a settings object that builds no panels at all. */
    public static final UiElementSource NO_UI_ELEMENTS = () -> null;

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
        installSettings(new SettingsAnswers(
            stringSource, colourSource, null, NO_NAMED_MODS, NO_UI_ELEMENTS));
    }

    /**
     * Installs the proxy with a caller-supplied {@code getString} resolver and panels that build
     * one known element. Use this overload for a subject that makes its own tooltip surface: the
     * panel and the element it comes off are internal to that subject, so the element named here
     * is what an assertion about the attachment is made against.
     */
    public static void installSettings(
            SettingsStringSource stringSource,
            UiElementSource uiElementSource) {
        installSettings(new SettingsAnswers(
            stringSource, DEFAULT_COLOURS, null, NO_NAMED_MODS, uiElementSource));
    }

    /**
     * Installs the proxy carrying a mod manager, which the overloads above deliberately leave off:
     * a settings object whose {@code getModManager} answers null is the state a read taken before
     * the game is fully up meets, and a fixture that always supplied one could not stand for it.
     * Use this overload for a subject that gates on another mod being installed.
     */
    public static void installSettingsWithEnabledMods(EnabledModsSource enabledMods) {
        installSettings(new SettingsAnswers(
            EMPTY_STRINGS, DEFAULT_COLOURS, enabledMods, NO_NAMED_MODS, NO_UI_ELEMENTS));
    }

    /**
     * Installs the proxy carrying a mod manager that knows mods by name as well as by enablement.
     * Use this overload for a subject that shows a mod's own name: with every ID answering nothing,
     * the name a report found and the ID it fell back to are the same string.
     *
     * @param modNames what the mod manager's specs answer for their names
     */
    public static void installSettingsWithModNames(ModNamesSource modNames) {
        installSettings(new SettingsAnswers(
            EMPTY_STRINGS, DEFAULT_COLOURS, modId -> modNames.nameOf(modId) != null, modNames, NO_UI_ELEMENTS));
    }

    public static void clearSettings() {
        Global.setSettings(null);
    }

    private static void installSettings(SettingsAnswers answers) {
        Global.setSettings(settings(answers));
    }

    private static SettingsAPI settings(SettingsAnswers answers) {

        return proxy(SettingsAPI.class, (proxy, method, args) -> {
            // Null unless a caller asked for one, so the no-mod-manager state stays reachable -
            // see installSettingsWithEnabledMods.
            if ("getModManager".equals(method.getName())) {
                return answers.enabledModsSource() == null
                    ? null
                    : modManager(answers.enabledModsSource(), answers.modNamesSource());
            }
            // Misc.<clinit> reads several floats and a colour before any
            // test code runs; returning safe defaults keeps it quiet.
            if ("getFloat".equals(method.getName())) {
                return 1f;
            }
            if ("getColor".equals(method.getName())) {
                return resolveColour(answers.colourSource(), args);
            }
            if ("getString".equals(method.getName())) {
                if (args != null && args.length == 2) {
                    return answers.stringSource().get((String) args[0], (String) args[1]);
                }
                return null;
            }
            if ("createCustom".equals(method.getName())) {
                return customPanel(answers.uiElementSource());
            }
            return resolveDefaultValue(method.getReturnType());
        });
    }

    // A panel handing back the caller's element and defaults for everything else. Its own proxy
    // rather than a mock so the whole settings object stays one kind of stand-in, and so a test
    // naming an element does not also have to describe a panel it never sees.
    private static CustomPanelAPI customPanel(UiElementSource uiElementSource) {

        return proxy(CustomPanelAPI.class, (proxy, method, args) -> {
            if ("createUIElement".equals(method.getName())) {
                return uiElementSource.createElement();
            }
            return resolveDefaultValue(method.getReturnType());
        });
    }

    // A mod manager answering the caller's enablement rule and defaults for everything else, so a
    // subject asking one question of the mod set does not have to be handed a whole one.
    private static ModManagerAPI modManager(EnabledModsSource enabledMods, ModNamesSource modNames) {

        return proxy(ModManagerAPI.class, (proxy, method, args) -> {
            if ("isModEnabled".equals(method.getName()) && args != null && args.length == 1) {
                return enabledMods.isEnabled((String) args[0]);
            }
            if ("getModSpec".equals(method.getName()) && args != null && args.length == 1) {
                return modSpec(modNames, (String) args[0]);
            }
            return resolveDefaultValue(method.getReturnType());
        });
    }

    // The spec for one mod, or null where the source knows no such ID - which is what the game
    // answers for an ID naming no installed mod, and the branch a report's fallback stands on.
    private static ModSpecAPI modSpec(ModNamesSource modNames, String modId) {

        var modName = modNames.nameOf(modId);

        if (modName == null) {
            return null;
        }
        return proxy(ModSpecAPI.class, (proxy, method, args) ->
            "getName".equals(method.getName())
                ? modName
                : resolveDefaultValue(method.getReturnType()));
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

    // A primitive's own zero, read off a one-element array of that type rather than named eight
    // times over. A reference answers null, and so does void: void is a primitive with no array to
    // make, and a call returning void discards whatever comes back anyway.
    private static Object resolveDefaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive() || void.class.equals(returnType)) {
            return null;
        }
        return Array.get(Array.newInstance(returnType, 1), 0);
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] {type},
            handler));
    }

    /**
     * Everything one installed settings object answers with, carried as one value.
     *
     * <p>Each adapter is optional and most tests name one of them, so the alternative is an
     * overload per combination - and the combinations multiply with every adapter added, while the
     * plumbing behind them takes the same list of arguments either way. Held together, the public
     * overloads stay the handful of shapes callers actually ask for and the proxy below takes one
     * parameter however many adapters there come to be.
     *
     * @param stringSource      what {@code getString} answers
     * @param colourSource      what {@code getColor} answers
     * @param enabledModsSource what the mod manager answers, or null for a settings object
     *                          carrying no mod manager at all
     * @param modNamesSource    what that manager's specs answer for their names
     * @param uiElementSource   what a panel's {@code createUIElement} answers
     */
    private record SettingsAnswers(
        SettingsStringSource stringSource,
        SettingsColourSource colourSource,
        EnabledModsSource enabledModsSource,
        ModNamesSource modNamesSource,
        UiElementSource uiElementSource) {
    }
}
