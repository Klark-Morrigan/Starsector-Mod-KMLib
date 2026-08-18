package kmlib.starsector.ui.coreui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;

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
 * one. The one judgement made is that an object not answering a name is a different shape rather
 * than a broken one: most components are leaves with no children to offer, and most dialogs host no
 * core UI, so reading either absence as a failure would abort a walk at its first leaf and take
 * down every read attempted while a scripted dialog is up.
 */
public final class CoreUiTree {

    // The core UI's own accessors, driven by name. All are part of its contract, so they survive
    // obfuscation.
    private static final String GET_CORE_METHOD = "getCore";
    private static final String GET_CORE_UI_METHOD = "getCoreUI";
    private static final String GET_CURRENT_TAB_METHOD = "getCurrentTab";
    private static final String GET_CHILDREN_METHOD = "getChildrenCopy";

    // ReflectionUtils.invoke resolves a method matching the argument types it is handed - none, for
    // the reads this class takes itself. Passed as an explicit shared array rather than left to the
    // varargs call, which would allocate a fresh empty one at each hop of every tree walk, and those
    // run per frame.
    private static final Object[] NO_ARGS = new Object[0];

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
        // A component offering no such name is a leaf. Descend no further down this branch.
        return readHopIfOffered(component, GET_CHILDREN_METHOD) instanceof List<?> children
            ? children
            : List.of();
    }

    /**
     * Whether the object's class carries a method of this name at all, so a caller can tell a shape
     * that does not offer a hop from one whose hop failed.
     *
     * <p>Worth asking rather than inferring from a failed call, on both counts a walk cares about.
     * The reach reports "no such name" and "more than one match" as the same type, so a caller
     * catching that type cannot tell which it got. And a name that does not resolve costs a thrown
     * exception where this costs a lookup, which is the difference between the two on every leaf a
     * per-frame walk meets - and most of a widget tree is leaves.
     *
     * <p>Answers the name only, not the argument shape. A caller that goes on to invoke with
     * arguments can still find that nothing takes them.
     *
     * @param instance   the object whose class to look in
     * @param methodName the method name to look for
     * @return whether the class declares or inherits any method of that name
     */
    public static boolean hasMethodNamed(Object instance, String methodName) {
        return !ReflectionUtils
            .getMethodsMatching(instance, methodName)
            .isEmpty();
    }

    /**
     * Invokes a no-arg method by name, so a caller can take a hop this class does not name.
     *
     * <p>Fails exactly as {@link #invokeWithArgs} does, being the same call with nothing to pass -
     * see there for what comes back out, which a caller either expects (a leaf that exposes no such
     * method) or treats as its own kind of read failure.
     *
     * @param instance   the object to call on
     * @param methodName the no-arg method to resolve
     * @return whatever the method returned
     */
    public static Object invokeNoArg(Object instance, String methodName) {
        return invokeWithArgs(instance, methodName, NO_ARGS);
    }

    /**
     * Invokes a method by name with arguments, for the hops that take them.
     *
     * <p>Separate from {@link #invokeNoArg} rather than replacing it, because the two say different
     * things at a call site: the no-arg name asserts the hop takes nothing, where this one would
     * read as an argument list that happened to come out empty.
     *
     * <p>The parameter types the method is resolved against come from the arguments' own classes,
     * with a boxed primitive unwrapping to the primitive - so a {@code Float} handed in here
     * resolves a {@code (float)} parameter, which is the shape the core UI's draw and input entry
     * points take. Matching from there is assignment compatibility rather than identity: a
     * parameter declared as a supertype or an interface of the argument resolves, as do the
     * widening and boxing conversions a direct call would make, and a null argument matches any
     * parameter that is not primitive. Resolution reaches the target's private methods as well as
     * its public ones, so a name is answered by more shapes than a direct call could reach.
     *
     * <p>That leniency is what lets more than one method match a single name and argument shape,
     * and an ambiguous name fails rather than one of the matches being picked. So a caller naming
     * a hop is asserting the name is unique on the target, not merely present.
     *
     * <p>Every failure - no method of that name and shape, more than one, or the call itself
     * throwing - comes back out, leaving the caller to decide what a failed hop means. It arrives
     * undeclared and not necessarily as a {@link RuntimeException}: the bypass is Kotlin, which
     * lets the checked exception wrapping the target's own throw escape unannounced. A caller
     * guarding this has to catch {@link Throwable}, the way the reads in this class do.
     *
     * @param instance   the object to call on
     * @param methodName the method to resolve
     * @param arguments  the arguments to pass, which also select the overload
     * @return whatever the method returned, or null for a void one
     */
    public static Object invokeWithArgs(Object instance, String methodName, Object... arguments) {
        // The target stays declared as Object deliberately: a Class-typed argument in that position
        // selects the overload that invokes a static method on that class rather than one on the
        // object, and the two differ only in the static type at the call site.
        return ReflectionUtils.invoke(instance, methodName, arguments);
    }

    /**
     * A dialog's own core UI, or null when there is no dialog or it hosts none.
     *
     * <p>Answering null for a dialog that exposes no such accessor is the same judgement made about
     * a component with no children: the absence names a different shape, not a failed read. The
     * game builds one dialog class that hosts a core UI and any number of scripted ones that do
     * not, so a caller falling through to the campaign's own core is reading the screen correctly
     * rather than papering over a broken hop.
     *
     * @param dialog the interaction dialog to look inside, or null when none is up
     * @return the core UI it hosts, or null
     */
    public static Object readCoreUiOf(Object dialog) {
        // A dialog offering no such name is a scripted one, hosting no core UI of its own.
        return dialog == null ? null : readHopIfOffered(dialog, GET_CORE_UI_METHOD);
    }

    /**
     * Walks campaign UI -> the core UI that is up -> current tab.
     *
     * @return the tab currently up, or null when there is no campaign UI yet or a hop answered
     *         null; the reason is not distinguished because no caller can act on it differently
     * @throws RuntimeException when a hop is absent or fails outright, so a caller applies its own
     *                          policy to a genuinely broken reach rather than to an empty screen
     */
    public static Object resolveCurrentTab() {

        var core = resolveActiveCoreUi();
        return core == null ? null : invokeNoArg(core, GET_CURRENT_TAB_METHOD);
    }

    /**
     * The core UI the screens are being drawn from, above any one tab.
     *
     * <p>Published beside the tab read for the walks that must not start at a tab: a tab is one
     * screen's subtree, while things drawn over the campaign - the HUD and whatever it raises -
     * hang elsewhere under this. A walk rooted at the tab cannot see them at all, so it would
     * report their absence rather than their contents.
     *
     * @return the core UI in force, or null when there is no campaign UI yet or the hop answered
     *         null
     * @throws RuntimeException when a hop is absent or fails outright, so a caller applies its own
     *                          policy to a genuinely broken reach rather than to an empty screen
     */
    public static Object resolveActiveCoreUi() {

        var sector = Global.getSector();

        if (sector == null || sector.getCampaignUI() == null) {
            return null;
        }
        return resolveActiveCore(sector.getCampaignUI());
    }

    // The core UI the screens are actually being drawn from. An interaction dialog stands up its
    // own, and every core screen opened while one is up - map, intel, refit - is hosted by that one
    // rather than the campaign's, which goes on holding whatever tab it was left on. Walking the
    // campaign's core regardless therefore searches the wrong tree for as long as a dialog is up,
    // which is a whole docked visit rather than a moment.
    //
    // Same precedence the published tab read applies, so the two cannot disagree about which screen
    // is up: a caller that gates on the tab id and then walks to its widgets is answered about one
    // core UI, not two.
    private static Object resolveActiveCore(CampaignUIAPI campaignUi) {

        var dialogCore = readCoreUiOf(campaignUi.getCurrentInteractionDialog());
        return dialogCore != null
            ? dialogCore
            : invokeNoArg(campaignUi, GET_CORE_METHOD);
    }

    // A hop where not answering the name is a shape rather than a failure, so the absence comes
    // back as null. Both such reads differ in what they do with that null, not in how they read it,
    // which is why the swallow is stated here once instead of at each of them.
    //
    // The name is asked for before the hop is taken, so the common case - a leaf carrying no such
    // name - costs a lookup rather than a thrown exception, on a path that runs per node per frame.
    // The catch stays behind it for the hop that resolves and then fails, which is a different thing
    // and equally not this class's to interpret.
    private static Object readHopIfOffered(Object instance, String methodName) {

        if (!hasMethodNamed(instance, methodName)) {
            return null;
        }
        
        try {
            return invokeNoArg(instance, methodName);
            
        } catch (Throwable cannotReadHop) {
            return null;
        }
    }
}
