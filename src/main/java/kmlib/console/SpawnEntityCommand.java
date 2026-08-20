package kmlib.console;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import kmlib.collections.KmlibCollections;
import kmlib.console.output.CommandOutput;
import kmlib.console.parsing.Parameter;
import kmlib.console.parsing.ParameterSpec;
import kmlib.console.validation.CommandContextValidation;
import kmlib.math.geometry.Points;
import kmlib.starsector.entities.EntityNameGenerator;
import kmlib.starsector.entities.EntityOrbits;
import kmlib.starsector.entities.EntitySpawner;
import kmlib.starsector.systems.SectorStarSystems;
import kmlib.starsector.systems.StarSystems;
import kmlib.text.KmlibStrings;

import java.util.Arrays;
import java.util.Random;

import static kmlib.console.parsing.ParameterValues.decimal;
import static kmlib.console.parsing.ParameterValues.nonNegativeDecimal;
import static kmlib.console.parsing.ParameterValues.text;

/**
 * Console command (dev tool): spawns an entity at the player's fleet position in
 * the current system, orbiting a chosen focus. In-system only.
 *
 * <p>Usage: {@code kmlib_spawn <kind> [orbit_focus_id] [speed] [jitter=<frac>]}.
 * The first token names the {@link SpawnableKind} to spawn (an inactive gate, or
 * a working jump point). The focus and speed are each optional and may be given
 * positionally or by name ({@code focus=<id>}, {@code speed=<deg/day>}); a named
 * value drops its slot out of the positional order, so positionals fill the
 * remaining {@code [focus, speed]} slots in turn. A lone bare token therefore
 * reads as the focus id - use {@code speed=} to set the speed while keeping the
 * default focus.
 *
 * <p>The orbit focus defaults to the system {@link StarSystemAPI#getCenter()
 * center}. A binary or trinary system has no single center (its stars orbit a
 * shared, invisible barycenter), so the command refuses to guess and asks for a
 * focus id - typically one of its stars.
 *
 * <p>Speed is in degrees per day (the same unit {@code kmlib_list_system_entities}
 * reports). With no explicit speed it is derived from the orbit radius via
 * {@link EntityOrbits}, matching vanilla's constant-tangential-speed drift. A
 * random jitter then widens whichever speed results - the radius-derived default
 * or an explicit {@code speed=} - by a fraction the named {@code jitter=<frac>}
 * option controls (default {@value EntityOrbits#VANILLA_JITTER_FRACTION}); pass
 * {@code jitter=0} for an exact speed. Jitter is named-only because it is a spread
 * modifier rather than a positional value. A non-positive speed pins the entity
 * in place.
 *
 * <p>Each kind owns its creation path: the gate is spawned inactive (grants no
 * access until activated, so a tester can confirm an inactive gate keeps a
 * cut-off system off a political map), while the jump point also generates its
 * hyperspace entrance and clears the system's cut-off tag so the system becomes
 * reachable. The orbit geometry and focus resolution are shared across kinds; a
 * new kind is added by adding a {@link SpawnableKind} constant.
 */
public final class SpawnEntityCommand extends KmlibBaseConsoleCommand {
    private static final SpawnSpec SPEC = new SpawnSpec();

    public SpawnEntityCommand() {
    }

    SpawnEntityCommand(CommandOutput output) {
        super(output);
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        // The kind (a leading token) is inspected between the context guards and
        // the parameter parse, so this command drives the two collaborators
        // directly rather than through CommandInput.
        var command = new CommandContextValidation(context, output)
            .requireCampaign()
            .requireStarSystem(readActiveSector())
            .validateAndPrintFeedback();
        if (!command.isValid()) {
            return command.getResult();
        }
        // The first word selects the kind; the rest configure focus and speed. A
        // bare invocation names no word at all, which is no kind - reported as the
        // unknown type it is, so the message lists what the command does accept.
        var words = KmlibStrings.splitIntoWords(args);
        var kindArg = words.isEmpty() ? "" : words.get(0);
        var kind = SpawnableKind.fromArg(kindArg);
        if (kind == null) {
            output.showMessage("Unknown entity type '"
                + kindArg
                + "'. Supported: "
                + SpawnableKind.listSupportedArgs()
                + '.');
            return CommandResult.ERROR;
        }
        // The spec prints why on a malformed argument; surface that as bad syntax.
        // A kind was named, so the words past it are the spec's to parse.
        var parameterTokens = words
            .subList(1, words.size())
            .toArray(new String[0]);
        var parsed = SPEC.parse(parameterTokens, output);
        if (!parsed.isValid()) {
            return parsed.getResult();
        }

        var system = SectorStarSystems.getPlayerStarSystem(readActiveSector());
        // resolveOrbitFocus returns null (having printed why) when the focus is
        // ambiguous in a multi-star system or the supplied id matches no entity,
        // so we surface that as bad syntax too.
        var focus = resolveOrbitFocus(system, parsed.get(SPEC.focus));
        if (focus == null) {
            return CommandResult.BAD_SYNTAX;
        }
        var fleetLocation = readActiveSector().getPlayerFleet().getLocation();
        var focusLocation = focus.getLocation();
        var distance = (float) Points.computeDistance(focusLocation, fleetLocation);
        var angle = (float) Points.computeAngleDegrees(focusLocation, fleetLocation);

        // An explicit speed sets the base rate; otherwise derive a vanilla-paced
        // rate from the orbit radius. The jitter then widens whichever base by the
        // requested (or default) spread - it applies in either case.
        var baseSpeedDegPerDay = parsed.get(SPEC.speed) != null
            ? parsed.get(SPEC.speed)
            : EntityOrbits.deriveBaseSpeedDegPerDay(distance);
        var speedDegPerDay = EntityOrbits.applyJitter(
            baseSpeedDegPerDay,
            parsed.get(SPEC.jitter),
            new Random());

        var entity = kind.spawnOrbiting(focus, distance, speedDegPerDay, angle);

        output.showMessage(kind.describeSpawn(entity, system));
        return CommandResult.SUCCESS;
    }

    /**
     * Resolves the entity the spawn should orbit, or null when the request is
     * ambiguous or unresolvable - printing the reason to the console so the
     * caller can just return {@code BAD_SYNTAX}.
     *
     * <p>An explicit {@code orbit_focus_id} always wins and is looked up among
     * the system's entities. With no id, a single-star system has an unambiguous
     * {@link StarSystemAPI#getCenter() center} to orbit; a system with two or
     * more stars does not, so the caller must name a focus and the error lists
     * the star ids as the likely candidates.
     */
    private SectorEntityToken resolveOrbitFocus(StarSystemAPI system, String focusArg) {
        if (KmlibStrings.hasText(focusArg)) {
            var focusId = focusArg.trim();
            var focus = system.getEntityById(focusId);
            if (focus == null) {
                output.showMessage("No entity with id '"
                    + focusId
                    + "' in "
                    + StarSystems.readDisplayName(system)
                    + ".");
            }
            return focus;
        }
        var stars = StarSystems.getStars(system);
        if (stars.size() >= 2) {
            output.showMessage("This system has "
                + stars.size()
                + " stars ("
                + KmlibCollections.join(stars, ", ", PlanetAPI::getId)
                + "). Re-run with an orbit_focus_id -"
                + " one of those star ids, or any entity id in the system.");
            return null;
        }
        return system.getCenter();
    }

    /**
     * What {@code kmlib_spawn} accepts after its kind: the orbit focus and speed,
     * each positional or named, and a name-only jitter. Jitter stands apart as a
     * spread modifier on the final speed rather than a value of its own, so it is
     * never positional. Focus defaults to the system center and speed to the
     * radius derivation when omitted.
     */
    private static final class SpawnSpec extends ParameterSpec {
        private final Parameter<String> focus =
            acceptsPositional("focus", "<id>", text()).defaultsTo("");
        private final Parameter<Float> speed =
            acceptsPositional("speed", "<deg/day>", decimal("a number in degrees per day"));
        private final Parameter<Float> jitter =
            acceptsNamed(
                    "jitter",
                    "<frac>",
                    nonNegativeDecimal("a non-negative fraction (e.g. 0.25)"))
                .defaultsTo(EntityOrbits.VANILLA_JITTER_FRACTION);

        private SpawnSpec() {
            super("Usage: kmlib_spawn <kind> [orbit_focus_id] [speed] [jitter=<frac>].");
        }
    }

    /**
     * The entity kinds this command can spawn. Each owns its creation path (a
     * custom inactive gate, or a working jump point with hyperspace wiring) and
     * the player-facing report for it; the shared orbit geometry and focus
     * resolution are supplied by the command. Add a constant to support a new
     * kind.
     */
    private enum SpawnableKind {
        GATE("gate") {
            @Override
            SectorEntityToken spawnOrbiting(
                    SectorEntityToken focus,
                    float distance,
                    float speedDegPerDay,
                    float startAngleDegrees) {
                // The gate is spawned inactive: it grants no access until the
                // gate-activation command brings it online.
                return EntitySpawner.spawnOrbitingCustomEntity(
                    focus,
                    Entities.INACTIVE_GATE,
                    Factions.NEUTRAL,
                    distance,
                    speedDegPerDay,
                    startAngleDegrees);
            }

            @Override
            String describeSpawn(SectorEntityToken spawned, StarSystemAPI system) {
                return "Added an inactive gate (id "
                    + spawned.getId()
                    + ") at your fleet position."
                    + " Activate it with the gate-activation command and this id.";
            }
        },
        JUMP_POINT("jump_point") {
            @Override
            SectorEntityToken spawnOrbiting(
                    SectorEntityToken focus,
                    float distance,
                    float speedDegPerDay,
                    float startAngleDegrees) {
                // The name is derived from the focus and orbit radius so the point
                // reads like a charted body; spawnOrbitingJumpPoint also generates
                // the hyperspace entrance and clears the system's cut-off tag.
                var name = EntityNameGenerator.generateJumpPointName(focus, distance);
                return EntitySpawner.spawnOrbitingJumpPoint(
                    focus, name,
                    distance,
                    speedDegPerDay,
                    startAngleDegrees);
            }

            @Override
            String describeSpawn(SectorEntityToken spawned, StarSystemAPI system) {
                return "Spawned "
                    + spawned.getName()
                    + " at your fleet position in "
                    + StarSystems.readDisplayName(system)
                    + ", generated its hyperspace entrance,"
                    + " and cleared the cut-off tag.";
            }
        };

        private final String arg;

        SpawnableKind(String arg) {
            this.arg = arg;
        }

        // Spawns this kind orbiting focus on the shared orbit geometry the
        // command supplies, and returns the spawned entity.
        abstract SectorEntityToken spawnOrbiting(
                SectorEntityToken focus,
                float distance,
                float speedDegPerDay,
                float startAngleDegrees);

        // The player-facing report for a completed spawn, read back from the
        // spawned entity (and its system) so the message reflects what was added.
        abstract String describeSpawn(SectorEntityToken spawned, StarSystemAPI system);

        // Resolves the kind named by a command argument, case-insensitively, or
        // null when no kind matches.
        private static SpawnableKind fromArg(String arg) {
            for (var kind : values()) {
                if (kind.arg.equalsIgnoreCase(arg)) {
                    return kind;
                }
            }
            return null;
        }

        // Comma-separated argument keywords, for telling the player which kinds
        // are accepted when their argument did not match one.
        private static String listSupportedArgs() {
            return KmlibCollections.join(
                Arrays.asList(values()),
                ", ",
                kind -> kind.arg);
        }
    }
}
