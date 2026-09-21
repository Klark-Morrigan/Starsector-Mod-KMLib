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
     * Pluggable adapter for {@link com.fs.starfarer.api.ModSpecAPI#getVersion()}, for a test whose
     * subject states a mismatch between the version a mod declares and something else.
     * Implementations answer for the mod IDs they know and null for the rest, which is what a spec
     * declaring no version answers.
     */
    @FunctionalInterface
    public interface ModVersionsSource {
        String versionOf(String modId);
    }

    /** {@link ModVersionsSource} for specs that state no version at all. */
    public static final ModVersionsSource NO_MOD_VERSIONS = modId -> null;

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
     * Installs the proxy with a caller-supplied {@code getString} resolver. Use this when tests
     * assert on localised labels that the resolver is expected to surface.
     */
    public static void installSettings(SettingsStringSource stringSource) {
        buildSettings().answerStrings(stringSource).installSettings();
    }

    /**
     * Opens a settings object to be described adapter by adapter, for everything the two shapes
     * above do not cover.
     *
     * <p>A builder rather than an overload per combination. The adapters are independent and most
     * tests name one, so overloads multiply with every adapter added - and two of them differed
     * only in which functional interface the second argument was, which no lambda at a call site
     * can tell apart. Named adapters cannot be transposed and cannot be ambiguous.
     *
     * @return a settings object answering the defaults, to be narrowed and then installed
     */
    public static SettingsBuilder buildSettings() {
        return new SettingsBuilder();
    }

    public static void clearSettings() {
        Global.setSettings(null);
    }

    private static SettingsAPI settings(SettingsBuilder answers) {

        return proxy(SettingsAPI.class, (proxy, method, args) -> {
            // Null unless a caller named one of the mod adapters, so the no-mod-manager state
            // stays reachable - see SettingsBuilder.
            if ("getModManager".equals(method.getName())) {
                return answers.hasModManager()
                    ? modManager(answers)
                    : null;
            }
            // Misc.<clinit> reads several floats and a colour before any
            // test code runs; returning safe defaults keeps it quiet.
            if ("getFloat".equals(method.getName())) {
                return 1f;
            }
            if ("getColor".equals(method.getName())) {
                return resolveColour(answers.colourSource, args);
            }
            if ("getString".equals(method.getName())) {
                if (args != null && args.length == 2) {
                    return answers.stringSource.get((String) args[0], (String) args[1]);
                }
                return null;
            }
            if ("createCustom".equals(method.getName())) {
                return customPanel(answers.uiElementSource);
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
    private static ModManagerAPI modManager(SettingsBuilder answers) {

        return proxy(ModManagerAPI.class, (proxy, method, args) -> {
            if ("isModEnabled".equals(method.getName()) && args != null && args.length == 1) {
                return answers.resolveEnabledMods().isEnabled((String) args[0]);
            }
            if ("getModSpec".equals(method.getName()) && args != null && args.length == 1) {
                return modSpec(answers, (String) args[0]);
            }
            return resolveDefaultValue(method.getReturnType());
        });
    }

    // The spec for one mod, or null where the source knows no such ID - which is what the game
    // answers for an ID naming no installed mod, and the branch a report's fallback stands on.
    // A spec the source names but states no version for answers null for it, the state a mod
    // declaring none leaves a reader in.
    private static ModSpecAPI modSpec(SettingsBuilder answers, String modId) {

        var modName = answers.resolveModNames().nameOf(modId);

        if (modName == null) {
            return null;
        }
        return proxy(ModSpecAPI.class, (proxy, method, args) -> {
            if ("getName".equals(method.getName())) {
                return modName;
            }
            if ("getVersion".equals(method.getName())) {
                return answers.resolveModVersions().versionOf(modId);
            }
            return resolveDefaultValue(method.getReturnType());
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
     * One settings object described adapter by adapter, each named where it is supplied.
     *
     * <p>Every adapter is optional and most tests name one, so an entry point per combination
     * multiplies with each adapter added while the plumbing behind them takes the same list either
     * way. Naming each at the call site also settles what an overload pair could not: two adapters
     * are told apart by the name they are passed under rather than by their functional interface,
     * which a lambda does not carry.
     *
     * <p>An adapter left unnamed answers the default beside its interface, and a mod manager is
     * present only where one of the three mod adapters was named - a settings object whose
     * {@code getModManager} answers null is the state a read taken before the game is fully up
     * meets, and a fixture that always supplied one could not stand for it.
     */
    public static final class SettingsBuilder {

        private SettingsStringSource stringSource = EMPTY_STRINGS;

        private SettingsColourSource colourSource = DEFAULT_COLOURS;

        // Null rather than a default: absent is what decides whether there is a mod manager at
        // all, which no source value can say.
        private EnabledModsSource enabledModsSource;
        private ModNamesSource modNamesSource;
        private ModVersionsSource modVersionsSource;

        private UiElementSource uiElementSource = NO_UI_ELEMENTS;

        private SettingsBuilder() {
        }

        /**
         * @param colourSource what {@code getColor} answers. Name it where the subject builds a
         *                     shade out of a named engine colour: with every key answering the same
         *                     default, a derived colour and the value it came from are the same
         *                     colour and the assertion pins nothing
         * @return this builder
         */
        public SettingsBuilder answerColours(SettingsColourSource colourSource) {

            this.colourSource = colourSource;
            return this;
        }

        /**
         * @param enabledModsSource what the mod manager reports for a mod ID. Name it where the
         *                          subject gates on another mod being installed
         * @return this builder
         */
        public SettingsBuilder answerEnabledMods(EnabledModsSource enabledModsSource) {

            this.enabledModsSource = enabledModsSource;
            return this;
        }

        /**
         * @param modNamesSource what the mod manager's specs answer for their names, and which IDs
         *                       it lists a spec for at all. Name it where the subject shows a mod's
         *                       own name: with every ID answering nothing, the name a report found
         *                       and the ID it fell back to are the same string
         * @return this builder
         */
        public SettingsBuilder answerModNames(ModNamesSource modNamesSource) {

            this.modNamesSource = modNamesSource;
            return this;
        }

        /**
         * @param modVersionsSource what those specs answer for their versions. Name it where the
         *                          subject reports which release of a mod is installed: with every
         *                          ID answering nothing, a version that was read and one that could
         *                          not be are the same absence
         * @return this builder
         */
        public SettingsBuilder answerModVersions(ModVersionsSource modVersionsSource) {

            this.modVersionsSource = modVersionsSource;
            return this;
        }

        /**
         * @param stringSource what {@code getString} answers
         * @return this builder
         */
        public SettingsBuilder answerStrings(SettingsStringSource stringSource) {

            this.stringSource = stringSource;
            return this;
        }

        /**
         * @param uiElementSource what a panel's {@code createUIElement} answers. Name it where the
         *                        subject makes its own tooltip surface: that surface never reaches
         *                        the caller, so the element named here is the only place the
         *                        attachment can be observed from outside
         * @return this builder
         */
        public SettingsBuilder answerUiElements(UiElementSource uiElementSource) {

            this.uiElementSource = uiElementSource;
            return this;
        }

        /** Installs what this describes into {@link Global}, in place of whatever was there. */
        public void installSettings() {

            Global.setSettings(settings(this));
        }

        // Whether the settings object carries a mod manager at all. Any of the three mod adapters
        // implies one: a caller naming what specs answer has plainly not asked for the state where
        // there is nothing to ask.
        private boolean hasModManager() {

            return enabledModsSource != null || modNamesSource != null || modVersionsSource != null;
        }

        // Enablement where only the specs were named: a mod the manager lists is a mod it has. The
        // two readings are separable - a spec can exist for a disabled mod - but a caller that
        // cared about the difference would have named the rule.
        private EnabledModsSource resolveEnabledMods() {

            return enabledModsSource == null
                ? modId -> resolveModNames().nameOf(modId) != null
                : enabledModsSource;
        }

        private ModNamesSource resolveModNames() {

            return modNamesSource == null ? NO_NAMED_MODS : modNamesSource;
        }

        private ModVersionsSource resolveModVersions() {

            return modVersionsSource == null ? NO_MOD_VERSIONS : modVersionsSource;
        }
    }
}
