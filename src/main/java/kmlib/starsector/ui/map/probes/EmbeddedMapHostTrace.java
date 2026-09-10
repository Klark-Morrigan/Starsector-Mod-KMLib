package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.Global;

import kmlib.logging.SessionWarning;
import kmlib.starsector.ui.coreui.CoreUiTree;

import org.apache.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Names whoever put a sector map somewhere other than the two screens that are meant to hold one.
 *
 * <p>A map widget renders terrain, and terrain rendering is a hook any mod's map can drive. When
 * one does, the pass reaching that hook is indistinguishable from the real map's while it runs - so
 * the question "who is drawing this" cannot be answered from inside it. A stack does not answer it
 * either: a mod builds its widget once and the engine renders it every frame afterwards, so by the
 * time the hook fires every frame between it and the game loop belongs to the engine.
 *
 * <p>What does answer it is the tree. The widget the mod built is still in it, and so is whatever it
 * was built into, so the map's own surroundings name the owner where a stack cannot. Vanilla's own
 * classes are filtered out of that report because they are the part that is never the answer -
 * every host is made of them, and one mod-owned name among them is the whole finding.
 *
 * <p>Finding the maps is {@link EmbeddedMapFinder}'s, and this is the wording of what it found.
 * The split is the ordinary one between a reading and a report on it, and it matters here because
 * the two are wanted at different moments: a rule about an embedded map has to act on the widget
 * every frame, where a line about who owns it is worth saying once.
 *
 * <p>That walk is taken afresh rather than read back from the finder's memo, since the point of the
 * line is what the tree holds as it stands - a remembered answer describes a tree that was, which
 * is the one thing a diagnostic must not quietly do.
 *
 * <p>Panel plugins are reported beside the widgets because that is where a mod's own class most
 * often is. A custom panel is a vanilla component holding a mod-supplied plugin, so a tree walk
 * that read only component classes would cross the mod's own object and report the engine's
 * wrapper around it.
 *
 * <p>Describes rather than logs, on this package's usual terms: a logger named here answers to no
 * mod's verbosity, and how often to repeat a walk this expensive is the caller's to decide.
 */
public final class EmbeddedMapHostTrace {

    private static final Logger LOG = Global.getLogger(EmbeddedMapHostTrace.class);

    // Says once per session that this stopped working, since a caller handed null cannot tell an
    // empty tree from a reach that broke.
    private static final SessionWarning WARNING = MapProbeWarnings.createSharedWarning(LOG);

    // Package prefixes that can never be the answer. The engine's own classes make up every host
    // that could hold a map, so reporting them would bury the one name that identifies an owner.
    private static final List<String> ENGINE_PACKAGE_PREFIXES = List.of(
        "com.fs.starfarer.",
        "com.fs.state.",
        "java.",
        "javax.");

    // How a tooltip host is recognised. Matched on the name because the class is not published and
    // naming it would bind this to one game build; a host that stops matching costs the narrower
    // report below and not the walk, which still names the map's own parentage.
    private static final String TOOLTIP_CLASS_MARKER = "Tooltip";

    // The hop a component offers when it is a custom panel wrapping someone else's plugin.
    private static final String GET_PLUGIN_METHOD = "getPlugin";

    private EmbeddedMapHostTrace() {
    }

    /**
     * Every sector map in the live tree bar the one the player has open, each with the chain of
     * widgets it hangs under and the mod-owned classes found around it.
     *
     * <p>Costs a full walk of the core UI and builds a string, so a caller in a render pass should
     * ask only while it intends to report the answer.
     *
     * @return a one-line description for a log, or null when there is no tree to walk or the reach
     *         into it failed - neither of which the caller can act on differently
     */
    public static String describeEmbeddedMapHosts() {
        try {

            var embeddedMaps = EmbeddedMapFinder.collectLiveEmbeddedMaps();
            if (embeddedMaps == null) {
                return null;
            }
            // An empty list is worth saying rather than suppressing: it separates "walked the tree
            // and no map is in it" from a walk that never ran, and the first of those means the map
            // hangs somewhere that root does not reach.
            return "mapHosts=" + ProbeDescriptions.describeUpToCap(
                embeddedMaps,
                EmbeddedMapHostTrace::describeMapHost);

        } catch (Throwable failure) {
            // Swallowed rather than raised: this is a diagnostic, and one that cannot read the tree
            // must not take down the render pass its caller is in the middle of.
            warnOnce(failure);
            return null;
        }
    }

    /**
     * Whether a class name belongs to something a mod supplied, which is the only kind of name that
     * can answer who owns a widget.
     *
     * @param className the fully qualified name to judge
     * @return whether it sits outside every engine and platform package
     */
    static boolean isModOwnedClass(String className) {

        if (className == null) {
            return false;
        }
        for (var enginePrefix : ENGINE_PACKAGE_PREFIXES) {
            if (className.startsWith(enginePrefix)) {
                return false;
            }
        }
        return true;
    }

    /**
     * One found map: what it hangs under, and every mod-owned name among its surroundings.
     *
     * <p>The wording of a finding rather than a reading of the screen, which is what makes it
     * answerable over a tree that was handed in - the ancestry arrives on the finding, and the
     * subtree below the host is reached the same way any walk reaches one.
     *
     * @param embeddedMap a map the walk found, with the chain it hangs under
     * @return the map, its host, that chain and the mod-owned classes around it, as one item of a
     *         diagnostic line
     */
    static String describeMapHost(EmbeddedMap embeddedMap) {

        var map = embeddedMap.widget();
        var ancestors = embeddedMap.ancestors();

        // The host to search around it: the tooltip it sits in where there is one, since that is
        // the whole of what a mod built, and its immediate parent otherwise - which at least bounds
        // the search to the map's own siblings rather than the entire screen.
        var host = resolveNearestTooltipAncestor(ancestors);
        if (host == null) {
            host = ancestors.isEmpty() ? map : ancestors.get(ancestors.size() - 1);
        }
        var modOwnedClasses = new LinkedHashSet<String>();

        collectModOwnedClassesIn(host, 0, modOwnedClasses);
        collectModOwnedClassesOf(ancestors, modOwnedClasses);

        // Box and opacity on both, because where a host is and whether it is drawn at all is the
        // reading that explains one nobody can see. A widget renders its whole subtree at zero
        // opacity exactly as it does at one, so a host can drive this pass every frame while
        // showing the player nothing - and a host that does have a visible box is one the player
        // can go and hover deliberately to find out what it is.
        return "[map=" + ProbeDescriptions.describeComponent(map)
            + " host=" + ProbeDescriptions.describeComponent(host)
            + " under=" + describeAncestors(ancestors)
            + " modOwned=" + modOwnedClasses
            + "]";
    }

    // The innermost ancestor that looks like a tooltip, or null when the map hangs under none.
    private static Object resolveNearestTooltipAncestor(List<Object> ancestors) {

        for (var index = ancestors.size() - 1; index >= 0; index--) {

            if (ancestors.get(index).getClass().getName().contains(TOOLTIP_CLASS_MARKER)) {
                return ancestors.get(index);
            }
        }
        return null;
    }

    // Every mod-owned class in this subtree, counting both the components themselves and the
    // plugins they carry.
    private static void collectModOwnedClassesIn(
            Object component,
            int depth,
            Set<String> modOwnedClasses) {

        if (component == null
                || depth > ProbeLimits.MAX_SEARCH_DEPTH
                || modOwnedClasses.size() >= ProbeLimits.MAX_REPORTED_ITEMS) {
            return;
        }
        addIfModOwned(component.getClass().getName(), modOwnedClasses);
        addIfModOwned(readPluginClassNameOf(component), modOwnedClasses);

        for (var child : CoreUiTree.readChildrenOf(component)) {
            collectModOwnedClassesIn(child, depth + 1, modOwnedClasses);
        }
    }

    private static void collectModOwnedClassesOf(
            List<Object> components,
            Set<String> modOwnedClasses) {

        for (var component : components) {
            addIfModOwned(component.getClass().getName(), modOwnedClasses);
            addIfModOwned(readPluginClassNameOf(component), modOwnedClasses);
        }
    }

    private static void addIfModOwned(String className, Set<String> modOwnedClasses) {
        if (isModOwnedClass(className) && modOwnedClasses.size() < ProbeLimits.MAX_REPORTED_ITEMS) {
            modOwnedClasses.add(className);
        }
    }

    // The plugin a custom panel carries, or null for the many components that are not one. Not
    // offering the hop is the ordinary case here rather than a broken reach, so it answers null the
    // way an absent child list does.
    private static String readPluginClassNameOf(Object component) {
        try {
            var plugin = CoreUiTree.invokeNoArg(component, GET_PLUGIN_METHOD);
            return plugin == null ? null : plugin.getClass().getName();

        } catch (Throwable offersNoSuchName) {
            return null;
        }
    }

    // Outermost first, so the line reads down from the screen to the map. Class names alone here,
    // where the map and its host carry boxes: the chain is for the shape of the tree, and a box per
    // level would bury that in coordinates nobody is going to compare.
    private static List<String> describeAncestors(List<Object> ancestors) {
        return ProbeDescriptions.describeUpToCap(
            ancestors, ancestor -> ancestor.getClass().getName());
    }

    // Warns on this library's own logger rather than the caller's, since a reach that broke is the
    // library's news to report.
    private static void warnOnce(Throwable failure) {
        WARNING.warnOnce("Could not walk the core UI tree by reflection; embedded map hosts will "
            + "go undescribed this session.", failure);
    }
}
