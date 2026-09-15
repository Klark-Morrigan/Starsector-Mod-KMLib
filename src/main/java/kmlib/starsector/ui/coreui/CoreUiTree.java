package kmlib.starsector.ui.coreui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The reach into the live core-UI widget tree: the hops from the campaign UI down to the tab that
 * is up, one component's children once there, and the question of whether a shape carries a given
 * hop at all.
 *
 * <p>None of it is published API. The core's own accessors are reached by name through
 * {@link ReflectedMembers}, over the {@link ReflectionBypass} that routes around the game's
 * mod-classloader reflection ban. Names
 * rather than casts because the tab classes carry illegal member names an obfuscated build leaves
 * unwritable in Java source. This package is the only place in the library that reflects at all -
 * here for the hops taken by name, and in {@link CoreUiMethods} for the members that have to be
 * recognised by their shape - so what the ban exists to contain stays contained to it.
 *
 * <p>Names no tab and no screen. Every hop it takes is one the core UI offers whatever tab is up,
 * which is why it sits in a package of its own rather than beside any one screen's probes: a probe
 * for a second screen would otherwise have to reach through the first screen's package to get at
 * the same handful of method names.
 *
 * <p>Deliberately policy-free about what a failure <em>means</em>. A hop is offered both ways -
 * raising, or answering null - and which of the two a caller takes is the caller's decision: a probe
 * that suppresses an overlay wants a failed read to count one way, a probe that draws one wants the
 * opposite, and offering only one of them would force both to live with it. The one judgement made
 * is that an object not answering a name is a different shape rather than a broken one: most
 * components are leaves with no children to offer, and most dialogs host no core UI, so reading
 * either absence as a failure would abort a walk at its first leaf and take down every read
 * attempted while a scripted dialog is up.
 */
public final class CoreUiTree {

    // The core UI's own accessors, driven by name. All are part of its contract, so they survive
    // obfuscation.
    private static final String GET_CORE_METHOD = "getCore";
    private static final String GET_CORE_UI_METHOD = "getCoreUI";
    private static final String GET_CURRENT_TAB_METHOD = "getCurrentTab";
    private static final String GET_CHILDREN_METHOD = "getChildrenCopy";

    // A component's fade state, which is what says whether it is still on screen. Both names are
    // the game's own contract - every component carries a fader, and the fader's own read - so they
    // survive obfuscation like the hops above.
    private static final String GET_FADER_METHOD = "getFader";
    private static final String IS_FADED_OUT_METHOD = "isFadedOut";

    // A hop is resolved against the argument types it is handed - none, for the reads this class
    // takes itself. Shared rather than left to the varargs call so a tree walk does not allocate a
    // fresh empty array at each hop; the reach allocates per invoke regardless, so this trims that
    // cost rather than avoiding it.
    private static final Object[] NO_ARGS = new Object[0];

    // Which shapes carry which names, so a walk pays the by-name resolution once per question
    // instead of once per node on every frame. Held beside the walk that asks rather than inside
    // the reach, since the presence question is answered without building a match at all and so has
    // nothing there to be memoised against - and held once here rather than per caller, since every
    // walk asks about the same handful of names over the same tree. Whether a class carries a name
    // is fixed for the run, so the answer belongs to the shape rather than to the moment, and the
    // map is bounded by the classes met times the few names this library asks about.
    private static final Map<MethodNameQuery, Boolean> SHAPES_CARRYING_NAME = new ConcurrentHashMap<>();

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
     * <p>Answered from a memo, since a class carries a name or does not for the whole run. A walk
     * asking per node per frame therefore resolves each shape once rather than each time it meets
     * one.
     *
     * @param instance   the object whose class to look in
     * @param methodName the method name to look for
     * @return whether the class declares or inherits any method of that name
     */
    public static boolean hasMethodNamed(Object instance, String methodName) {

        return SHAPES_CARRYING_NAME.computeIfAbsent(
            new MethodNameQuery(instance.getClass(), methodName),
            query -> ReflectedMembers.hasMethodNamed(query.shape(), query.methodName()));
    }

    /**
     * Takes a no-arg hop and answers null rather than raising when it cannot be taken, for the reads
     * where a shape not carrying the name is an ordinary answer.
     *
     * <p>The forgiving counterpart to {@link #invokeNoArg}, and the one most walks want: a widget
     * tree is mostly leaves, so a walk that treated an absent name as a failure would abort at the
     * first ordinary component it met. What is *not* decided here is what the null means - a caller
     * reading a leaf and a caller reading a broken reach both get one, and each is left to say which
     * it was expecting.
     *
     * <p>The name is asked for before the hop is taken, so the common case costs a memo lookup
     * rather than a thrown exception on a path that runs per node per frame. The swallow behind it
     * covers the other way a hop fails - resolving and then throwing - which is a different thing
     * and equally not this class's to interpret.
     *
     * @param instance   the object to call on
     * @param methodName the no-arg method to resolve
     * @return whatever the method returned, or null when the shape carries no such name or the call
     *         itself failed
     */
    public static Object readHopIfOffered(Object instance, String methodName) {

        if (!hasMethodNamed(instance, methodName)) {
            return null;
        }

        try {
            return invokeNoArg(instance, methodName);

        } catch (Throwable cannotReadHop) {
            return null;
        }
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
     * undeclared and not necessarily as a {@link RuntimeException}: the target's own throw comes
     * back wrapped in a checked exception, and the bypass rethrows what it caught as it was thrown
     * rather than replacing it. A caller guarding this has to catch {@link Throwable}, the way the
     * reads in this class do.
     *
     * @param instance   the object to call on
     * @param methodName the method to resolve
     * @param arguments  the arguments to pass, which also select the overload
     * @return whatever the method returned, or null for a void one
     */
    public static Object invokeWithArgs(Object instance, String methodName, Object... arguments) {

        return ReflectedMembers.invokeByName(instance, methodName, arguments);
    }

    /**
     * Whether a component is still drawn where its parent put it, so a caller holding one the game
     * handed out earlier can tell a live widget from one the screen it belonged to has taken down.
     *
     * <p>Read off the component's own fader, which is the signal the game's containers act on: a
     * panel that has finished fading out is dropped from its parent's children on the next advance.
     * A reference outlives that, and nothing about the reference itself says so.
     *
     * <p>Fails open, unlike the reads it sits beside: a shape carrying no fader, or one whose fader
     * cannot be read, counts as showing. The answer only ever takes something away from a caller -
     * a tree it would otherwise walk, a screen it would otherwise act on - so an unreadable signal
     * leaves it doing what it did before rather than going quiet on a screen the player is looking
     * at.
     *
     * @param component the component to test, or null when the caller has none
     * @return whether it is still on screen; false when there is no component at all
     */
    public static boolean isComponentShowing(Object component) {

        if (component == null) {
            return false;
        }

        var fader = readHopIfOffered(component, GET_FADER_METHOD);
        if (fader == null) {
            return true;
        }

        // Anything but a plain "yes, faded out" is read as showing, so an unreadable fade state
        // fails open along with an absent fader.
        return !Boolean.TRUE.equals(readHopIfOffered(fader, IS_FADED_OUT_METHOD));
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
        return dialog == null
            ? null
            : readHopIfOffered(dialog, GET_CORE_UI_METHOD);
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
        return core == null
            ? null
            : invokeNoArg(core, GET_CURRENT_TAB_METHOD);
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
    // The dialog's core counts only while it is still on screen, which is not the same as the
    // dialog having one. Closing a core screen opened from a dialog fades that core out and drops
    // it from the dialog's children, but the dialog goes on handing the panel out, and unlike the
    // campaign's core it never closes the tab it was showing - so a walk that took it on presence
    // alone would keep finding the closed screen's widgets, lit, for the rest of the visit. Once it
    // is down the screens are the campaign's again.
    private static Object resolveActiveCore(CampaignUIAPI campaignUi) {

        var dialogCore = readCoreUiOf(campaignUi.getCurrentInteractionDialog());

        return isComponentShowing(dialogCore)
            ? dialogCore
            : invokeNoArg(campaignUi, GET_CORE_METHOD);
    }

    // The one thing that keys the memo: a shape and a name are what decide the answer together, and
    // neither alone identifies the question being asked.
    private record MethodNameQuery(
        Class<?> shape,
        String methodName) {
    }
}
