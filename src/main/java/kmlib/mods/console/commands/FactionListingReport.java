package kmlib.mods.console.commands;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.starsector.factions.FactionFlags;
import kmlib.starsector.factions.StarsectorPlayerFactionResolver;
import kmlib.starsector.factions.relation.StarsectorPlayerRelations;
import kmlib.starsector.factions.relation.StarsectorRelationFormatter;
import kmlib.starsector.markets.colonies.Colony;
import kmlib.starsector.markets.colonies.SectorColonies;
import kmlib.starsector.settings.modmanager.ModSource;
import kmlib.text.KmlibStrings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The faction listing itself: every faction the filter admits, written as the ID it is addressed
 * by, the names it is known by, where it stands with the player, which mod declared it, and what it
 * holds.
 *
 * <p>Its own class rather than the command's back half because the two answer to different things.
 * The command answers to Console Commands - what the player typed, what it means, where the answer
 * goes - while this answers only to the sector it is handed. Held apart, the whole listing is
 * reachable without a console, a context or a running game.
 *
 * <p>What a run decides once is held as state rather than threaded through every line: the filter,
 * what was left off, and the sources are fixed for the whole listing, and passing them down five
 * arguments at a time made every helper's signature a record of where its inputs came from rather
 * than of what it does.
 *
 * <p>Every faction is listed unfiltered, {@code neutral} and {@code derelict} and the internal
 * placeholders included, because the holdings counts are exactly what tells a reader which of those
 * are inert. Suppressing them would hide the answer the listing exists to give.
 *
 * <p>Concealment and discovery are reported side by side rather than nested, because neither is a
 * subset of the other. {@code isHidden()} is a listing flag that stays set for a market's whole
 * life - the Galatia Academy is permanently hidden, and a pirate base stays hidden long after it is
 * raided - while discoverability is player awareness and clears the moment the player arrives. An
 * undiscovered pirate base counts in both columns; a discovered one counts only as hidden.
 *
 * <p>Counting is per place, not per market: several markets can share one body - a mod attaching
 * its own market beside vanilla's on the same planet - and counting both would report two holdings
 * where the player sees one. That rule is not restated here. It belongs to the sector's colony
 * read, along with what counts as a colony at all and whether one the economy never registered is
 * present, and is inherited by reading through it rather than walking the economy afresh.
 *
 * <p>Which mod a faction came from answers the question the ID alone raises on a heavily modded
 * install - a listing of eighty factions is mostly a list of unfamiliar IDs, and the mod they were
 * declared by is what makes it navigable. It is the one clause dropped outright when it cannot be
 * read: an unattributed mark against every faction would say only that the read failed, which the
 * clause's absence says more quietly.
 *
 * <p>Shaping is this listing's own: grouping the colonies by owner, counting them, naming their
 * systems and labelling hyperspace are its questions rather than the sector's, so they live here
 * rather than in a domain type built to one output's shape.
 */
final class FactionListingReport {

    // Two headers rather than one with a clause cut off it: a listing that reports no holdings must
    // not open by saying it does.
    private static final String HEADER = "Factions and their holdings";
    private static final String HEADER_WITHOUT_HOLDINGS = "Factions";
    private static final String HOLDINGS_INDENT = "\n    ";

    // What a faction's segments are joined by, and what separates the two names a faction has where
    // it carries a distinct long one.
    private static final String NAME_SEPARATOR = " / ";
    private static final String NO_FACTIONS_LINE = "\n  (none)";
    private static final String SEGMENT_SEPARATOR = " - ";
    private static final String SELF_ATTITUDE = "(self)";
    private static final String SOURCE_PREFIX = "from: ";
    private static final String TERRITORIAL_MARK = " [territorial]";

    // A faction the sector holds that no row in the game's data declares - one a mod built at
    // runtime rather than from a file. Named rather than left blank, so a reader can tell it apart
    // from a faction whose clause was simply not printed.
    private static final String UNATTRIBUTED_SOURCE = "(unattributed)";

    private final FactionListingFilter filter;
    private final Set<ListingOption> options;
    private final Map<String, ModSource> sourcesByFactionId;

    /**
     * @param filter             which factions to list, and which of their holdings the systems
     *                           clause names
     * @param options            what the player asked left off; the routing option belongs to the
     *                           command and is not read here
     * @param sourcesByFactionId what mod declared each faction; empty for a run with nothing to
     *                           report, which drops the clause rather than marking every faction
     *                           unattributed
     */
    FactionListingReport(
            FactionListingFilter filter,
            Set<ListingOption> options,
            Map<String, ModSource> sourcesByFactionId) {

        this.filter = filter;
        this.options = options;
        this.sourcesByFactionId = sourcesByFactionId;
    }

    /**
     * Writes the listing for one sector. Free of {@code Global} and the console, so the grouping,
     * the counts, the filtering and the formatting turn on what it is handed and nothing else.
     *
     * @param sector the sector whose factions and colonies to read
     * @return the formatted report, or a notice line when no faction matches
     */
    String describeFactions(SectorAPI sector) {

        // One walk of the sector answers every faction's row, so the read happens here rather than
        // once per faction.
        var coloniesByFactionId = groupColoniesByFactionId(SectorColonies.readColonies(sector));
        var playerFactionId = readPlayerFactionId(sector);
        var isHoldingsShown = !options.contains(ListingOption.OMIT_HOLDINGS);

        var report = new StringBuilder(isHoldingsShown
            ? HEADER
            : HEADER_WITHOUT_HOLDINGS);

        var keyword = filter.getKeyword();

        if (keyword != null) {
            report.append(" (").append(keyword).append(')');
        }
        report.append(':');

        var listedCount = 0;

        for (var faction : collectFactionsSortedById(sector)) {

            var holdings = new FactionHoldings(
                coloniesByFactionId.getOrDefault(faction.getId(), List.of()));

            if (!filter.shouldList(holdings)) {
                continue;
            }
            listedCount++;

            appendFactionLine(report, faction, playerFactionId);

            // Left off, the listing is one line per faction, which is what makes a sector's worth
            // of them scannable side by side.
            if (isHoldingsShown) {
                appendHoldingsLine(report, holdings);
            }
        }
        if (listedCount == 0) {
            report.append(NO_FACTIONS_LINE);
        }
        return report.toString();
    }

    // How a faction is named and where it stands: its ID and names carrying the territorial mark,
    // its attitude to the player, and the mod it came from.
    //
    // Assembled as segments and joined rather than appended in place, because two of the three are
    // droppable - by a keyword, and by there being nothing to say - and appending would leave a
    // separator hanging off whichever end went.
    private void appendFactionLine(
            StringBuilder report,
            FactionAPI faction,
            String playerFactionId) {

        var segments = new ArrayList<String>();

        segments.add(describeIdentity(faction));

        if (!options.contains(ListingOption.OMIT_ATTITUDE)) {

            var attitude = describeAttitude(faction, playerFactionId);

            if (attitude != null) {
                segments.add(attitude);
            }
        }

        // Nothing read means nothing to say about any faction, so the clause goes rather than
        // standing on every line as a mark the reader cannot act on.
        if (!sourcesByFactionId.isEmpty()) {
            segments.add(SOURCE_PREFIX + describeSource(sourcesByFactionId.get(faction.getId())));
        }

        report
            .append('\n')
            .append(String.join(SEGMENT_SEPARATOR, segments));
    }

    // What the faction holds: the count of places, how many are concealed, how many are
    // undiscovered, and the systems they sit in.
    private void appendHoldingsLine(StringBuilder report, FactionHoldings holdings) {

        var placeCount = holdings.countPlaces();

        report
            .append(HOLDINGS_INDENT)
            .append("holdings: ")
            .append(placeCount);

        if (placeCount == 0) {
            // Nothing held leaves the visibility counts and the systems clause with nothing to say,
            // and a row of noughts reads as a finding rather than as the absence it is.
            return;
        }

        // The counts are of everything the faction holds even under a filter: the keyword decides
        // who is listed, not what is reported about them.
        report
            .append(" (")
            .append(holdings.countHiddenPlaces())
            .append(" hidden, ")
            .append(holdings.countDiscoverablePlaces())
            .append(" discoverable)");

        // The clause, unlike the counts, does narrow - under "hidden" it names the systems holding
        // a hidden place, which is what makes the filter useful for finding where the matches
        // actually are.
        var systemLabels = holdings.collectSystemLabels(filter.getHoldingSelection());

        if (!systemLabels.isEmpty()) {

            report
                .append("  systems: ")
                .append(String.join(", ", systemLabels));
        }
    }

    // Where a faction came from, with the mod ID beside the name for a source that has one - the
    // name is what a reader recognises, the ID what another command takes.
    private String describeSource(ModSource source) {

        if (source == null) {
            return UNATTRIBUTED_SOURCE;
        }
        if (!source.hasModId()) {
            return source.sourceName();
        }
        return source.sourceName() + " [" + source.modId() + "]";
    }

    // Where the faction stands with the player, or null where there is no standing to report. The
    // player's own faction has none with itself worth printing; the engine still answers for one,
    // which would read as a finding.
    private static String describeAttitude(FactionAPI faction, String playerFactionId) {

        if (Objects.equals(faction.getId(), playerFactionId)) {
            return SELF_ATTITUDE;
        }
        return StarsectorPlayerRelations.readPlayerRelation(faction)
            .map(StarsectorRelationFormatter::formatRelation)
            .orElse(null);
    }

    // Which faction this is: the ID it is addressed by, the names it is known by, and whether it
    // treats the space around its holdings as its own.
    private static String describeIdentity(FactionAPI faction) {

        // Through the resolver rather than getDisplayName(), because a placeholder name reads as a
        // real one: the player faction reports "Independent" before its first colony and the
        // literal "player" on a stock Nexerelin setup. Falling back to the ID says no more than is
        // known, at the cost of repeating it on a faction whose display name is itself one of the
        // placeholders.
        var shortName = StarsectorPlayerFactionResolver.resolveDisplayName(faction, faction.getId());
        var longName = faction.getDisplayNameLong();

        var identity = new StringBuilder()
            .append('[')
            .append(faction.getId())
            .append("] ");

        // Both names only where the long one says something the short one does not. Most factions
        // declare no long name or the same one twice, and "Hegemony / Hegemony" on eighty lines
        // costs more width than the pair is worth.
        if (KmlibStrings.hasText(longName) && !longName.equals(shortName)) {
            identity.append(longName).append(NAME_SEPARATOR);
        }
        identity.append(shortName);

        if (FactionFlags.isTerritorial(faction)) {
            identity.append(TERRITORIAL_MARK);
        }
        return identity.toString();
    }

    // Every faction the sector knows, ordered by id. The sector's own order is load order, which
    // differs between installs and makes two runs hard to compare; a listing of thirty-odd factions
    // is also read by looking one up.
    private static List<FactionAPI> collectFactionsSortedById(SectorAPI sector) {

        var factions = new ArrayList<FactionAPI>(sector.getAllFactions());
        factions.sort(Comparator.comparing(FactionAPI::getId));
        return factions;
    }

    // The colonies keyed by the ID of the faction holding them. Every colony the sector read yields
    // is owned - that is the rule the read selects on - so the owner is always there to key by.
    private static Map<String, List<Colony>> groupColoniesByFactionId(List<Colony> colonies) {

        var coloniesByFactionId = new LinkedHashMap<String, List<Colony>>();

        for (var colony : colonies) {

            coloniesByFactionId
                .computeIfAbsent(colony.market().getFaction().getId(), id -> new ArrayList<>())
                .add(colony);
        }
        return coloniesByFactionId;
    }

    private static String readPlayerFactionId(SectorAPI sector) {

        var playerFaction = sector.getPlayerFaction();
        return playerFaction == null
            ? null
            : playerFaction.getId();
    }
}
