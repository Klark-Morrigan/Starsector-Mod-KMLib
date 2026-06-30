package kmlib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import kmlib.console.output.CommandOutput;
import kmlib.console.parsing.ParameterSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Console command that lists, as a tree, the star systems worth spoiling: those
 * cut off from hyperspace, or holding a hidden or undiscovered faction-owned
 * (non-neutral) market. For each, it prints the system, then its owned entities
 * and factions, flagging the cut-off system and each hidden/undiscovered market.
 * Ordinary, fully-visible systems are omitted - this surfaces only what the
 * player would not normally see.
 *
 * <p>Soft Console Commands dependency: this class touches
 * {@code org.lazywizard.console.*}, but it is loaded only when Console Commands
 * instantiates it from KMLib's commands.csv. A game without Console Commands
 * never loads it, so KMLib runs fine without that mod - no hard dependency is
 * declared.
 */
public final class ListMapSpoilersCommand extends KmlibBaseConsoleCommand {
    private static final String NEUTRAL_FACTION_ID = "neutral";
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
     * Builds the spoiler report for {@code sector}: the tree of cut-off and
     * hidden/undiscovered owned systems. Free of {@code Global} and the console,
     * so the filtering and formatting can be unit tested against a stub sector.
     *
     * @param sector the sector to scan
     * @return the formatted report, or a notice line when nothing qualifies
     */
    static String buildReport(SectorAPI sector) {
        var report = new StringBuilder(
                "Map spoilers - cut-off systems and hidden/undiscovered markets:");
        var systemCount = 0;
        for (var system : sector.getStarSystems()) {
            var ownedMarkets = collectOwnedMarkets(sector, system);

            var isSystemCutOff = system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
            var isSystemOrdinary = !isSystemCutOff && !hasHiddenMarkets(ownedMarkets);

            // Only spoiler-worthy systems: cut off, or holding a market the
            // player would not normally see. Ordinary visible systems are skipped.
            if (ownedMarkets.isEmpty() || isSystemOrdinary) {
                continue;
            }
            systemCount++;
            report.append('\n').append(system.getName());
            if (isSystemCutOff) {
                report.append("  [cut off]");
            }
            for (var market : ownedMarkets) {
                report.append("\n    ").append(market.getName())
                        .append("  (").append(market.getFaction().getDisplayName()).append(')')
                        .append(getVisibilitySuffix(market));
            }
        }
        if (systemCount == 0) {
            report.append("\n  (none)");
        }
        return report.toString();
    }

    private static boolean hasHiddenMarkets(List<MarketAPI> markets) {
        for (var market : markets) {
            if (!getVisibilitySuffix(market).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static List<MarketAPI> collectOwnedMarkets(SectorAPI sector, StarSystemAPI system) {
        var ownedMarkets = new ArrayList<MarketAPI>();
        for (var market : sector.getEconomy().getMarkets(system)) {
            var faction = market.getFaction();
            if (market.isPlanetConditionMarketOnly() || faction == null
                    || NEUTRAL_FACTION_ID.equals(faction.getId())) {
                continue;
            }
            ownedMarkets.add(market);
        }
        return ownedMarkets;
    }

    // Flags a market the player would not normally see on the map: a hidden
    // market, or one whose entity has not been discovered.
    private static String getVisibilitySuffix(MarketAPI market) {
        if (market.isHidden()) {
            return "  [hidden]";
        }
        var entity = market.getPrimaryEntity();
        if (entity != null && entity.isDiscoverable()) {
            return "  [undiscovered]";
        }
        return "";
    }
}
