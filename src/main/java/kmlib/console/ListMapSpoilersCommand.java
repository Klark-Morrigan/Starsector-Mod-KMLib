package kmlib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import kmlib.console.output.CommandOutput;
import kmlib.console.parsing.ParameterSpec;
import kmlib.starsector.markets.Markets;
import kmlib.starsector.systems.StarSystems;

import java.util.ArrayList;
import java.util.List;

/**
 * Console command that lists, as a tree, the star systems worth spoiling: those
 * cut off from hyperspace, or holding a faction-owned (non-neutral) market the
 * player has not found yet - an undiscovered entity, which includes concealed
 * pirate/pather bases. For each, it prints the system, then its owned entities
 * and factions, flagging the cut-off system and each undiscovered market.
 * Ordinary, fully-visible systems are omitted - this surfaces only what the
 * player would not normally see.
 *
 * <p>Entity discoverability, not any market flag, drives what is surfaced.
 * {@code MarketAPI.isHidden()} and the {@code $core_hiddenBase} memory flag are
 * both economy details that stay set for the market's whole life - on the
 * Galatia Academy, and on a pirate base long after the player has found it - so
 * neither tracks "not found yet". A market is surfaced only while its entity is
 * still discoverable, which clears the moment the player discovers it.
 *
 * <p>Soft Console Commands dependency: this class touches
 * {@code org.lazywizard.console.*}, but it is loaded only when Console Commands
 * instantiates it from KMLib's commands.csv. A game without Console Commands
 * never loads it, so KMLib runs fine without that mod - no hard dependency is
 * declared.
 */
public final class ListMapSpoilersCommand extends KmlibBaseConsoleCommand {
    // No parameters; declaring the spec still makes the parser reject a stray
    // argument as bad syntax rather than silently ignoring it.
    private static final ParameterSpec SPEC =
        ParameterSpec.takingNoArguments("Usage: kmlib_list_map_spoilers.");

    public ListMapSpoilersCommand() {
    }

    ListMapSpoilersCommand(CommandOutput output) {
        super(output);
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        var parsed = readInput(context, args)
            .requireCampaign()
            .parseArguments(SPEC);
        if (!parsed.isValid()) {
            return parsed.getResult();
        }
        output.showMessage(buildReport(Global.getSector()));
        return CommandResult.SUCCESS;
    }

    /**
     * Builds the spoiler report for {@code sector}: the tree of cut-off systems
     * and those holding an undiscovered owned market. Free of {@code Global} and
     * the console, so the filtering and formatting can be unit tested against a
     * stub sector.
     *
     * @param sector the sector to scan
     * @return the formatted report, or a notice line when nothing qualifies
     */
    static String buildReport(SectorAPI sector) {
        var report = new StringBuilder(
            "Map spoilers - cut-off systems and undiscovered markets:");
        var systemCount = 0;
        for (var system : sector.getStarSystems()) {
            var ownedMarkets = collectOwnedMarkets(sector, system);

            var isSystemCutOff = system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);

            // Only spoiler-worthy systems: cut off (the player cannot normally
            // reach it), or holding a market not found yet. A cut-off system
            // lists even with no owned market, so empty void/story systems still
            // surface; ordinary reachable systems with nothing hidden are skipped.
            if (!isSystemCutOff && !hasFlaggedMarkets(ownedMarkets)) {
                continue;
            }
            systemCount++;
            report.append('\n').append(StarSystems.readDisplayName(system));
            if (isSystemCutOff) {
                report.append("  [cut off]");
            }
            for (var market : ownedMarkets) {
                report
                    .append("\n    ")
                    .append(market.getName())
                    .append("  (")
                    .append(market.getFaction().getDisplayName())
                    .append(')')
                    .append(getVisibilitySuffix(market));
            }
        }
        if (systemCount == 0) {
            report.append("\n  (none)");
        }
        return report.toString();
    }

    private static boolean hasFlaggedMarkets(List<MarketAPI> markets) {
        for (var market : markets) {
            if (!getVisibilitySuffix(market).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // The systems worth spoiling are the ones somebody has settled, so the listing asks the same
    // question the colony commands do rather than restating it as a loop of its own - a place
    // flying the neutral flag is somewhere nobody lives, and nothing about it is a spoiler.
    private static List<MarketAPI> collectOwnedMarkets(SectorAPI sector, StarSystemAPI system) {
        var ownedMarkets = new ArrayList<MarketAPI>();
        for (var market : sector.getEconomy().getMarkets(system)) {
            if (Markets.isSettledColony(market)) {
                ownedMarkets.add(market);
            }
        }
        return ownedMarkets;
    }

    // Flags a market the player has not found yet: one whose entity is still
    // discoverable. Pirate and Luddic Path hidden bases set their entity
    // discoverable, so this covers them too, and - unlike isHidden() or the
    // $core_hiddenBase flag, both of which stay set for the market's life - it
    // stops flagging the moment the entity is discovered.
    private static String getVisibilitySuffix(MarketAPI market) {
        var entity = market.getPrimaryEntity();
        if (entity != null && entity.isDiscoverable()) {
            return "  [undiscovered]";
        }
        return "";
    }
}
