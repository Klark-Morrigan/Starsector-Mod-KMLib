package kmlib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.console.factions.FactionTargetResolver;
import kmlib.console.factions.ResolvedFactionTarget;
import kmlib.console.factions.UnresolvedFactionTarget;
import kmlib.console.markets.MarketTargetRequirement;
import kmlib.console.markets.MarketTargetResolver;
import kmlib.console.markets.ResolvedMarketTarget;
import kmlib.console.markets.UnresolvedMarketTarget;
import kmlib.console.output.CommandOutput;
import kmlib.console.parsing.Parameter;
import kmlib.console.parsing.ParameterSpec;
import kmlib.starsector.factions.StarsectorPlayerFactionResolver;
import kmlib.starsector.markets.MarketColoniser;

import static kmlib.console.parsing.ParameterValues.text;

/**
 * Console command (cheat): founds a colony on a body that so far carries only survey data. Takes
 * an optional entity id and an optional owning faction id, so a bare invocation colonises the
 * nearest such body for the player.
 *
 * <p>Named, a body can be colonised from anywhere - another system, or hyperspace - since an
 * entity id points at one place in the whole sector. Only the bare invocation needs the player to
 * be in a star system, "nearest" having nowhere to measure from otherwise. That is one condition
 * of the search rather than of the command, so it is stated where the search is and this command
 * guards only on being in a campaign.
 *
 * <p>Bypasses every prerequisite the survey panel's own button enforces - no survey, no outpost
 * cost, no need to be in orbit - which is what makes it a cheat rather than a shortcut. What it
 * produces is otherwise the game's own baseline colony, and on an install running a mod with a
 * colonisation of its own, that mod's.
 *
 * <p>A shell over three collaborators, and deliberately holds no mechanics of its own: which
 * place was meant is {@code MarketTargetResolver}'s, which faction was meant is
 * {@code FactionTargetResolver}'s, and what founding a colony consists of is
 * {@link MarketColoniser}'s. That split is what lets the same founding be reached from a mod's
 * own code rather than only from a player typing at the console.
 *
 * <p>Both refusals are settled before anything is mutated, so a run naming an unknown faction
 * leaves the body untouched rather than half-colonised. The target is resolved first only
 * because a run with two mistakes in it has to report one of them, and the place is the argument
 * a player is likelier to have got wrong.
 *
 * <p>Soft Console Commands dependency: this class touches {@code org.lazywizard.console.*}, but
 * it is loaded only when Console Commands instantiates it from KMLib's commands.csv. A game
 * without Console Commands never loads it, so KMLib runs fine without that mod - no hard
 * dependency is declared.
 */
public final class ColoniseCommand extends KmlibBaseConsoleCommand {

    private static final ColoniseSpec SPEC = new ColoniseSpec();

    public ColoniseCommand() {
    }

    ColoniseCommand(CommandOutput output) {
        super(output);
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context) {

        // A campaign is the only context guard: whether being in a star system is needed depends
        // on which argument shape was typed, which is not known until the parse, and is the
        // search's own condition rather than this command's.
        var parsed = readInput(context, args)
            .requireCampaign()
            .parseArguments(SPEC);

        if (!parsed.isValid()) {
            return parsed.getResult();
        }

        var sector = Global.getSector();

        var target = MarketTargetResolver.resolveTargetMarket(
            sector,
            parsed.get(SPEC.entityId),
            MarketTargetRequirement.COLONISABLE_BODY);

        if (target instanceof UnresolvedMarketTarget unresolvedTarget) {

            output.showMessage(unresolvedTarget.failureMessage());
            return CommandResult.ERROR;
        }

        var owner = FactionTargetResolver.resolveOwningFaction(sector, parsed.get(SPEC.factionId));

        if (owner instanceof UnresolvedFactionTarget unresolvedOwner) {

            output.showMessage(unresolvedOwner.failureMessage());
            return CommandResult.ERROR;
        }

        var market = ((ResolvedMarketTarget) target).market();
        var faction = ((ResolvedFactionTarget) owner).faction();

        MarketColoniser.establishColony(sector, market, faction.getId());

        output.showMessage(describeFoundedColony(market, faction));
        return CommandResult.SUCCESS;
    }

    // What the player is told about the colony that now exists. Built from the market only after
    // the founding, never before it: survey data goes by whatever name its placeholder happened
    // to hold, and the colony takes the body's - or, where a mod founded it, whatever name that
    // mod gave the place.
    private static String describeFoundedColony(MarketAPI market, FactionAPI faction) {

        return "Founded a colony on "
            + market.getName()
            + " for "
            // Through the resolver rather than getDisplayName(), because the player faction
            // reports a placeholder until it has an identity of its own - "Independent" before
            // the first colony, and the literal "player" on a stock Nexerelin setup.
            + StarsectorPlayerFactionResolver.resolveDisplayName(faction, faction.getId())
            + '.';
    }

    /**
     * What {@code kmlib_colonise} accepts: the body to colonise and the faction to colonise it
     * for, both optional and in that order.
     *
     * <p>Neither carries a default of its own. What an omitted argument means is a question about
     * the sector rather than about the command line - the nearest colonisable body, the player's
     * own faction - so each is left unsupplied here and answered by the resolver that knows how
     * to look it up.
     */
    private static final class ColoniseSpec extends ParameterSpec {

        private final Parameter<String> entityId =
            acceptsPositional("entity_id", "<entity-id>", text());
        private final Parameter<String> factionId =
            acceptsPositional("faction_id", "<faction-id>", text());

        private ColoniseSpec() {
            super("Usage: kmlib_colonise [entity-id] [faction-id].");
        }
    }
}
