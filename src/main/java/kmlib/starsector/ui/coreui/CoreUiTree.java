package kmlib.starsector.ui.coreui;

import com.fs.starfarer.api.Global;

import org.magiclib.ReflectionUtils;

import java.util.List;

/**
 * The reach into the live core-UI widget tree: the hops from the campaign UI down to the tab that
 * is up, and one component's children once there.
 *
 * <p>None of it is published API. The core's own accessors are reached by name through MagicLib's
 * {@link ReflectionUtils}, the ecosystem's proven bypass of the game's script-classloader
 * reflection ban - it drives {@code java.lang.reflect} through method handles, so no reflect type
 * is named in mod code. Names rather than casts because the tab classes carry illegal member names
 * an obfuscated build leaves unwritable in Java source.
 *
 * <p>Names no tab and no screen. Every hop it takes is one the core UI offers whatever tab is up,
 * which is why it sits in a package of its own rather than beside any one screen's probes: a probe
 * for a second screen would otherwise have to reach through the first screen's package to get at
 * the same three method names.
 *
 * <p>Deliberately policy-free. Every hop either answers or throws, and what a failed read *means*
 * is the caller's to decide: a probe that suppresses an overlay wants a failure to read one way, a
 * probe that draws one wants the opposite, and baking either here would force both to live with
 * one. The only judgement made is that a component exposing no children is a leaf rather than a
 * failure, since most components are leaves and that would otherwise abort a walk at its first one.
 */
public final class CoreUiTree {

    // The core UI's own accessors, driven by name. All are part of its contract, so they survive
    // obfuscation.
    private static final String GET_CORE_METHOD = "getCore";
    private static final String GET_CURRENT_TAB_METHOD = "getCurrentTab";
    private static final String GET_CHILDREN_METHOD = "getChildrenCopy";

    // ReflectionUtils.invoke resolves a public method (declared=false) matching these argument
    // types - none, for the no-arg reads here.
    private static final Object[] NO_ARGS = new Object[0];
    private static final boolean PUBLIC_METHOD = false;

    private CoreUiTree() {
    }

    /**
     * A component's children, or none when it exposes no {@code getChildrenCopy}.
     *
     * <p>Most components are leaves with no such method, so an unreadable children list ends the
     * walk down that branch rather than being reported as a failure.
     *
     * @param component the component to descend into
     * @return its children, or an empty list when it is a leaf
     */
    public static List<?> readChildrenOf(Object component) {
        try {
            if (invokeNoArg(component, GET_CHILDREN_METHOD) instanceof List<?> children) {
                return children;
            }
        } catch (Throwable notAParent) {
            // No getChildrenCopy: a leaf. Descend no further down this branch.
        }
        return List.of();
    }

    /**
     * Invokes a public no-arg method by name, so a caller can take a hop this class does not name.
     *
     * @param instance   the object to call on
     * @param methodName the public no-arg method to resolve
     * @return whatever the method returned
     * @throws RuntimeException when the method is absent or the call fails, which a caller either
     *                          expects (a leaf that exposes no such method) or treats as its own
     *                          kind of read failure
     */
    public static Object invokeNoArg(Object instance, String methodName) {
        // invoke is an instance method on the ReflectionUtils singleton; only set/get are static.
        return ReflectionUtils.INSTANCE.invoke(methodName, instance, NO_ARGS, PUBLIC_METHOD);
    }

    /**
     * Walks campaign UI -> core -> current tab.
     *
     * @return the tab currently up, or null when there is no campaign UI yet or a hop answered
     *         null; the reason is not distinguished because no caller can act on it differently
     * @throws RuntimeException when a hop is absent or fails outright, so a caller applies its own
     *                          policy to a genuinely broken reach rather than to an empty screen
     */
    public static Object resolveCurrentTab() {
        var sector = Global.getSector();
        if (sector == null || sector.getCampaignUI() == null) {
            return null;
        }
        var core = invokeNoArg(sector.getCampaignUI(), GET_CORE_METHOD);
        return core == null ? null : invokeNoArg(core, GET_CURRENT_TAB_METHOD);
    }
}
