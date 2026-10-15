package kmlib.mods.console.commands;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import kmlib.mods.console.commands.output.CommandOutput;
import kmlib.mods.console.commands.parsing.ParameterSpec;
import kmlib.starsector.SectorWalks;
import kmlib.starsector.entities.Entities;
import kmlib.starsector.markets.LocationMarkets;
import kmlib.starsector.markets.Markets;
import kmlib.starsector.systems.StarSystems;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
public final class ListMapSpoilersCommand
        extends BaseKmlibCommand {

    // No parameters; declaring the spec still makes the parser reject a stray
    // argument as bad syntax rather than silently ignoring it.
    private static final ParameterSpec SPEC =
        ParameterSpec.takingNoArguments("kmlib_list_map_spoilers");

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

        output.showMessage(buildReport(readActiveSector()));

        return CommandResult.SUCCESS;
    }

    /**
     * Builds the spoiler report for {@code sector}: the tree of cut-off systems
     * and those holding an undiscovered owned market. Free of {@code Global} and
     * the console, so the filtering and formatting turn on the sector it is
     * handed and nothing else.
     *
     * @param sector the sector to scan
     * @return the formatted report, or a notice line when nothing qualifies
     */
    static String buildReport(SectorAPI sector) {

        var report = new StringBuilder(
            "Map spoilers - cut-off systems and undiscovered markets:");

        var entries = new ArrayList<String>();

        SectorWalks.walkStarSystems(
            sector,
            system -> describeSpoilerSystem(sector, system).ifPresent(entries::add));

        if (entries.isEmpty()) {
            report.append("\n  (none)");
        }
        for (var entry : entries) {
            report
                .append('\n')
                .append(entry);
        }

        return report.toString();
    }

    // The systems worth spoiling are the ones somebody has settled, so the listing asks the same
    // question the colony commands do rather than restating it as a loop of its own - a place
    // flying the neutral flag is somewhere nobody lives, and nothing about it is a spoiler.
    //
    // Read through the shared reader rather than off the economy directly, so a system's markets
    // are found the one way every other listing finds them - counted like the rest, and answering
    // an unreachable economy as an empty place rather than failing on it.
    private static List<MarketAPI> collectOwnedMarkets(SectorAPI sector, StarSystemAPI system) {

        var ownedMarkets = new ArrayList<MarketAPI>();

        for (var market : LocationMarkets.readMarkets(sector, system)) {

            if (Markets.isSettledColony(market)) {
                ownedMarkets.add(market);
            }
        }

        return ownedMarkets;
    }

    // One system's block of the report: the system, then each owned market with its faction and
    // its visibility flag. Empty for a system not worth spoiling.
    //
    // Only spoiler-worthy systems: cut off (the player cannot normally reach it), or holding a
    // market not found yet. A cut-off system lists even with no owned market, so empty void/story
    // systems still surface; ordinary reachable systems with nothing hidden are skipped.
    private static Optional<String> describeSpoilerSystem(SectorAPI sector, StarSystemAPI system) {

        var ownedMarkets = collectOwnedMarkets(sector, system);
        var isSystemCutOff = system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);

        if (!isSystemCutOff && !hasFlaggedMarkets(ownedMarkets)) {
            return Optional.empty();
        }

        var entry = new StringBuilder(StarSystems.readDisplayName(system));

        if (isSystemCutOff) {
            entry.append("  [cut off]");
        }

        for (var market : ownedMarkets) {

            entry
                .append("\n    ")
                .append(market.getName())
                .append("  (")
                .append(market.getFaction().getDisplayName())
                .append(')')
                .append(readVisibilitySuffix(market));
        }

        return Optional.of(entry.toString());
    }

    private static boolean hasFlaggedMarkets(List<MarketAPI> markets) {

        for (var market : markets) {

            if (!readVisibilitySuffix(market).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    // Flags a market the player has not found yet: one whose entity is still
    // discoverable. Pirate and Luddic Path hidden bases set their entity
    // discoverable, so this covers them too, and - unlike isHidden() or the
    // $core_hiddenBase flag, both of which stay set for the market's life - it
    // stops flagging the moment the entity is discovered.
    private static String readVisibilitySuffix(MarketAPI market) {

        if (!Entities.isDiscoveredByPlayer(market.getPrimaryEntity())) {
            return "  [undiscovered]";
        }
        return "";
    }
}
