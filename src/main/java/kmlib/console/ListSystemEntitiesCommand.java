package kmlib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import kmlib.console.output.CommandOutput;
import kmlib.console.parsing.Parameter;
import kmlib.console.parsing.ParameterSpec;
import kmlib.starsector.geometry.StarsectorPoints;
import kmlib.starsector.systems.StarSystems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Console command (dev tool): lists the current system's entities as an orbit
 * tree - each entity under the one it orbits, with its distance from that focus
 * and orbital speed. Entities with no orbit are printed after the tree with
 * coordinates, nearest the system center first; fleets are printed last the same
 * way. In-system only. Takes an optional {@code gates} flag to narrow the
 * listing to gates.
 *
 * <p>The {@code gates} flag narrows the tree to gates, but keeps the full chain
 * of bodies they orbit so each gate's place is clear, and the trailing lists to
 * gates only.
 */
public final class ListSystemEntitiesCommand extends KmlibBaseConsoleCommand {
    private static final ListSystemEntitiesSpec SPEC = new ListSystemEntitiesSpec();
    // One full revolution; an orbital period (in days) divides into this to give
    // the entity's angular speed in degrees per day.
    private static final double DEGREES_PER_CIRCLE = 360.0;

    public ListSystemEntitiesCommand() {
    }

    ListSystemEntitiesCommand(CommandOutput output) {
        super(output);
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        var parsed = readInput(context, args)
                .requireCampaign()
                .requireStarSystem()
                .parseArguments(SPEC);
        if (!parsed.isValid()) {
            return parsed.getResult();
        }
        var system = StarSystems.getPlayerStarSystem(Global.getSector());
        output.showMessage(buildReport(system, parsed.get(SPEC.gatesOnly)));
        return CommandResult.SUCCESS;
    }

    /**
     * Builds the entity report for {@code system}. Free of {@code Global} and the
     * console, so the tree, sorting, and gate filtering can be unit tested
     * against a stub system.
     *
     * @param system    the system to scan
     * @param isGatesOnly when true, restrict the output to gates (keeping the
     *                  bodies they orbit so the tree still reads)
     * @return the formatted report
     */
    static String buildReport(StarSystemAPI system, boolean isGatesOnly) {
        var center = system.getCenter();
        var entities = new ArrayList<SectorEntityToken>();
        for (var entity : system.getAllEntities()) {
            if (!(entity instanceof CampaignFleetAPI)) {
                entities.add(entity);
            }
        }

        var childrenByFocus = new LinkedHashMap<SectorEntityToken, List<SectorEntityToken>>();
        for (var entity : entities) {
            var focus = entity.getOrbitFocus();
            if (focus != null) {
                childrenByFocus.computeIfAbsent(focus, key -> new ArrayList<>()).add(entity);
            }
        }
        Set<SectorEntityToken> keep = isGatesOnly ? collectGatesAndAncestors(entities, center) : null;

        var report = new StringBuilder("System entities in ").append(system.getName());
        if (isGatesOnly) {
            report.append(" (gates only)");
        }
        report.append(':');

        var inTree = new HashSet<SectorEntityToken>();
        appendOrbitTree(report, center, childrenByFocus, 0, keep, inTree);

        appendPositionedSection(report, "Unorbited entities (nearest center first)",
                collectUnorbitedEntities(entities, inTree, isGatesOnly), center);
        if (!isGatesOnly) {
            appendPositionedSection(report, "Fleets (nearest center first)",
                    new ArrayList<SectorEntityToken>(system.getFleets()), center);
        }
        return report.toString();
    }

    // Gates plus every body each orbits, up to the center - the nodes to keep in
    // a gates-only tree so each gate's orbit chain still shows.
    private static Set<SectorEntityToken> collectGatesAndAncestors(
            List<SectorEntityToken> entities, SectorEntityToken center) {
        var keep = new HashSet<SectorEntityToken>();
        for (var entity : entities) {
            if (!entity.hasTag(Tags.GATE)) {
                continue;
            }
            for (var node = entity; node != null && keep.add(node);
                    node = node.getOrbitFocus()) {
                // walk up the orbit chain, stopping when a node is already kept
            }
        }
        if (!keep.isEmpty()) {
            keep.add(center);
        }
        return keep;
    }

    private static void appendOrbitTree(StringBuilder report, SectorEntityToken node,
            Map<SectorEntityToken, List<SectorEntityToken>> childrenByFocus, int depth,
            Set<SectorEntityToken> keep, Set<SectorEntityToken> inTree) {
        if (node == null || !inTree.add(node)) {
            return;
        }
        // In gates-only mode a node not on a gate's orbit chain has no gate
        // beneath it, so the whole branch is skipped.
        if (keep != null && !keep.contains(node)) {
            return;
        }
        report.append('\n');
        for (var indent = 0; indent < depth; indent++) {
            report.append("  ");
        }
        report.append(formatOrbitNode(node));

        var children = childrenByFocus.get(node);
        if (children != null) {
            children.sort(Comparator.comparingDouble(
                    child -> StarsectorPoints.computeDistanceBetween(child, node)));
            for (var child : children) {
                appendOrbitTree(report, child, childrenByFocus, depth + 1, keep, inTree);
            }
        }
    }

    private static List<SectorEntityToken> collectUnorbitedEntities(
            List<SectorEntityToken> entities, Set<SectorEntityToken> inTree, boolean gatesOnly) {
        var unorbited = new ArrayList<SectorEntityToken>();
        for (var entity : entities) {
            if (inTree.contains(entity) || (gatesOnly && !entity.hasTag(Tags.GATE))) {
                continue;
            }
            unorbited.add(entity);
        }
        return unorbited;
    }

    private static void appendPositionedSection(StringBuilder report, String heading,
            List<SectorEntityToken> entities, SectorEntityToken center) {
        if (entities.isEmpty()) {
            return;
        }
        entities.sort(Comparator.comparingDouble(
                entity -> StarsectorPoints.computeDistanceBetween(entity, center)));
        report.append("\n\n").append(heading).append(':');
        for (var entity : entities) {
            report.append("\n  ").append(describe(entity)).append(String.format(Locale.ROOT,
                    "  (%.0f, %.0f)", entity.getLocation().x, entity.getLocation().y));
        }
    }

    // One tree line: the entity, plus its distance to and speed around its focus
    // (omitted for a focusless root like the central star).
    private static String formatOrbitNode(SectorEntityToken entity) {
        var focus = entity.getOrbitFocus();
        if (focus == null) {
            return describe(entity);
        }
        return describe(entity) + String.format(Locale.ROOT, "  dist=%.0f  speed=%.2f deg/day",
                StarsectorPoints.computeDistanceBetween(entity, focus),
                computeOrbitalSpeedDegPerDay(entity));
    }

    private static String describe(SectorEntityToken entity) {
        var name = entity.getName() != null ? entity.getName() : "(unnamed)";
        return name + " [" + entity.getId() + ']';
    }

    private static double computeOrbitalSpeedDegPerDay(SectorEntityToken entity) {
        var orbit = entity.getOrbit();
        if (orbit == null || orbit.getOrbitalPeriod() <= 0f) {
            return 0.0;
        }
        return DEGREES_PER_CIRCLE / orbit.getOrbitalPeriod();
    }

    /**
     * What {@code kmlib_list_system_entities} accepts: a lone {@code gates} flag
     * that narrows the listing to gates and their orbit chains. A flag rather than
     * a value, so it is given as the bare keyword and absent means the full tree.
     */
    private static final class ListSystemEntitiesSpec extends ParameterSpec {
        private final Parameter<Boolean> gatesOnly = acceptsFlag("gates");

        private ListSystemEntitiesSpec() {
            super("Usage: kmlib_list_system_entities [gates].");
        }
    }
}
