package kmlib.mods.console.commands;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.mods.console.commands.output.CommandOutput;
import kmlib.mods.console.commands.output.GameLogCommandOutput;
import kmlib.mods.console.commands.parsing.Parameter;
import kmlib.mods.console.commands.parsing.ParameterSpec;
import kmlib.mods.console.commands.parsing.ParsedParameters;
import kmlib.starsector.factions.FactionFlags;
import kmlib.starsector.factions.FactionSourceMods;
import kmlib.starsector.factions.StarsectorPlayerFactionResolver;
import kmlib.starsector.factions.relation.StarsectorPlayerRelations;
import kmlib.starsector.factions.relation.StarsectorRelationFormatter;
import kmlib.starsector.markets.MarketVisibility;
import kmlib.starsector.markets.colonies.Colony;
import kmlib.starsector.markets.colonies.SectorColonies;
import kmlib.starsector.settings.modmanager.ModSource;
import kmlib.text.KmlibStrings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Console command (dev tool): lists every faction in the sector with which mod
 * it came from and what it holds - how many places, how many of those are
 * concealed, how many the player has still to find, and which systems they sit
 * in. Prints to the console, two lines per faction. Takes an optional filter
 * keyword narrowing which factions are listed, and any of the options that drop
 * a part of the listing or send it to the log instead.
 *
 * <p>Every faction is listed unfiltered, {@code neutral} and {@code derelict} and
 * the internal placeholders included, because the holdings counts are exactly
 * what tells a reader which of those are inert. Suppressing them would hide the
 * answer the command exists to give.
 *
 * <p>Concealment and discovery are reported side by side rather than nested,
 * because neither is a subset of the other. {@code isHidden()} is a listing flag
 * that stays set for a market's whole life - the Galatia Academy is permanently
 * hidden, and a pirate base stays hidden long after it is raided - while
 * discoverability is player awareness and clears the moment the player arrives.
 * An undiscovered pirate base counts in both columns; a discovered one counts only as
 * hidden.
 *
 * <p>Counting is per place, not per market: several markets can share one body -
 * a mod attaching its own market beside vanilla's on the same planet - and
 * counting both would report two holdings where the player sees one. That rule is
 * not restated here. It belongs to the sector's colony read, along with what
 * counts as a colony at all and whether one the economy never registered is
 * present, and is inherited by reading through it rather than walking the economy
 * afresh.
 *
 * <p>Which mod a faction came from answers the question the id alone raises on a
 * heavily modded install - a listing of eighty factions is mostly a list of
 * unfamiliar ids, and the folder they were declared in is what makes it
 * navigable. It is read once per run rather than per faction, and is the one
 * clause that is dropped outright when it cannot be read: an unattributed mark
 * against every faction would say only that the read failed, which the clause's
 * absence says more quietly.
 *
 * <p>Shaping is the command's own: grouping the colonies by owner, counting them,
 * naming their systems and labelling hyperspace are this listing's questions
 * rather than the sector's, so they live here rather than in a domain type built
 * to one output's shape.
 *
 * <p>Soft Console Commands dependency: this class touches
 * {@code org.lazywizard.console.*}, but it is loaded only when Console Commands
 * instantiates it from KMLib's commands.csv. A game without Console Commands
 * never loads it, so KMLib runs fine without that mod - no hard dependency is
 * declared.
 */
public final class ListFactionsCommand extends BaseKmlibCommand {

    // Derived from the keywords themselves rather than written out, so adding one
    // cannot leave this line offering the player a set that no longer matches what
    // the command accepts.
    //
    // Its initialiser runs both enums', which is safe only because a constant there
    // captures a method reference and never a static field of this class - one read
    // from an enum constant's initialiser would still be null at that point.
    private static final String USAGE = describeUsage();

    // Two headers rather than one with a clause cut off it: a listing that reports no
    // holdings must not open by saying it does.
    private static final String HEADER = "Factions and their holdings";
    private static final String HEADER_WITHOUT_HOLDINGS = "Factions";
    private static final String HOLDINGS_INDENT = "\n    ";

    // A colony outside every star system has no id to name it by. Vanilla builds
    // none, but mods put markets in hyperspace, so the clause says where they are
    // rather than dropping them and under-reporting the systems a faction is in.
    private static final String HYPERSPACE_LABEL = "(hyperspace)";

    // What a faction's segments are joined by, and what separates the two names a
    // faction has where it carries a distinct long one.
    private static final String NAME_SEPARATOR = " / ";
    private static final String NO_FACTIONS_LINE = "\n  (none)";
    private static final String SEGMENT_SEPARATOR = " - ";
    private static final String SELF_RELATIONSHIP = "(self)";
    private static final String SOURCE_PREFIX = "from: ";
    private static final String TERRITORIAL_MARK = " [territorial]";

    // A faction the sector holds that no row in the game's data declares - one a mod built at
    // runtime rather than from a file. Named rather than left blank, so a reader can tell it apart
    // from a faction whose clause was simply not printed.
    private static final String UNATTRIBUTED_SOURCE = "(unattributed)";

    // What the console is told when the report went to the log instead, so a run that printed
    // nothing where the player is looking does not read as a run that did nothing.
    private static final String WROTE_TO_LOG_NOTICE =
        "Faction listing written to the game log.";

    private static final ListFactionsSpec SPEC = new ListFactionsSpec();

    public ListFactionsCommand() {
    }

    ListFactionsCommand(CommandOutput output) {
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

        // The keywords are alternatives, not composable predicates: no_markets
        // contradicts the other three, and a pair such as "hidden discoverable"
        // has no single honest answer for the systems clause. The parser accepts
        // each flag independently and cannot express that, so the mutual
        // exclusion is checked here and reported against the usage line.
        var selectedFilters = SPEC.collectSelectedFilters(parsed);

        if (selectedFilters.size() > 1) {

            output.showMessage("Give at most one filter. " + USAGE);
            return CommandResult.BAD_SYNTAX;
        }
        var filter = selectedFilters.isEmpty()
            ? FactionListingFilter.ALL
            : selectedFilters.get(0);

        var options = SPEC.collectSelectedOptions(parsed);

        var report = buildReport(
            readActiveSector(),
            filter,
            options,
            FactionSourceMods.readSourcesByFactionId());

        // The log keeps what the console scrolls away, which is what a listing this
        // long is usually wanted for; the console still hears that it happened.
        if (options.contains(ListingOption.WRITE_TO_LOG)) {

            GameLogCommandOutput.INSTANCE.showMessage(report);
            output.showMessage(WROTE_TO_LOG_NOTICE);

        } else {
            output.showMessage(report);
        }
        return CommandResult.SUCCESS;
    }

    /**
     * Builds the faction listing for {@code sector} under {@code filter}. Free of
     * {@code Global} and the console, so the grouping, the counts, the filtering
     * and the formatting turn on what it is handed and nothing else.
     *
     * @param sector             the sector whose factions and colonies to read
     * @param filter             which factions to list, and which of their holdings
     *                           the systems clause names
     * @param options            what the player asked left off; the routing option is
     *                           the caller's business and is ignored here
     * @param sourcesByFactionId what mod declared each faction; empty for a run with
     *                           nothing to report, which drops the clause rather than
     *                           marking every faction unattributed
     * @return the formatted report, or a notice line when no faction matches
     */
    static String buildReport(
            SectorAPI sector,
            FactionListingFilter filter,
            Set<ListingOption> options,
            Map<String, ModSource> sourcesByFactionId) {
        // One walk of the sector answers every faction's row, so the read happens
        // here rather than once per faction.
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

            appendFactionLine(report, faction, playerFactionId, options, sourcesByFactionId);

            // Left off, the listing is one line per faction, which is what makes a
            // sector's worth of them scannable side by side.
            if (isHoldingsShown) {
                appendHoldingsLine(report, holdings, filter);
            }
        }
        if (listedCount == 0) {
            report.append(NO_FACTIONS_LINE);
        }
        return report.toString();
    }

    // How a faction is named and where it stands: its id and names carrying the
    // territorial mark, its attitude to the player, and the mod it came from.
    //
    // Assembled as segments and joined rather than appended in place, because two of
    // the three are droppable - by a keyword, and by there being nothing to say - and
    // appending would leave a separator hanging off whichever end went.
    private static void appendFactionLine(
            StringBuilder report,
            FactionAPI faction,
            String playerFactionId,
            Set<ListingOption> options,
            Map<String, ModSource> sourcesByFactionId) {

        var segments = new ArrayList<String>();

        segments.add(describeIdentity(faction));

        if (!options.contains(ListingOption.OMIT_ATTITUDE)) {

            var attitude = describeAttitude(faction, playerFactionId);

            if (attitude != null) {
                segments.add(attitude);
            }
        }

        // Nothing read means nothing to say about any faction, so the clause goes
        // rather than standing on every line as a mark the reader cannot act on.
        if (!sourcesByFactionId.isEmpty()) {
            segments.add(SOURCE_PREFIX + describeSource(sourcesByFactionId.get(faction.getId())));
        }

        report
            .append('\n')
            .append(String.join(SEGMENT_SEPARATOR, segments));
    }

    // Where the faction stands with the player, or null where there is no standing to
    // report. The player's own faction has none with itself worth printing; the engine
    // still answers for one, which would read as a finding.
    private static String describeAttitude(FactionAPI faction, String playerFactionId) {

        if (Objects.equals(faction.getId(), playerFactionId)) {
            return SELF_RELATIONSHIP;
        }
        return StarsectorPlayerRelations.readPlayerRelation(faction)
            .map(StarsectorRelationFormatter::formatRelation)
            .orElse(null);
    }

    // Which faction this is: the id it is addressed by, the names it is known by, and
    // whether it treats the space around its holdings as its own.
    private static String describeIdentity(FactionAPI faction) {

        // Through the resolver rather than getDisplayName(), because a placeholder
        // name reads as a real one: the player faction reports "Independent" before
        // its first colony and the literal "player" on a stock Nexerelin setup.
        // Falling back to the id says no more than is known, at the cost of repeating
        // it on a faction whose display name is itself one of the placeholders.
        var shortName = StarsectorPlayerFactionResolver.resolveDisplayName(faction, faction.getId());
        var longName = faction.getDisplayNameLong();

        var identity = new StringBuilder()
            .append('[')
            .append(faction.getId())
            .append("] ");

        // Both names only where the long one says something the short one does not.
        // Most factions declare no long name or the same one twice, and "Hegemony /
        // Hegemony" on eighty lines costs more width than the pair is worth.
        if (KmlibStrings.hasText(longName) && !longName.equals(shortName)) {
            identity.append(longName).append(NAME_SEPARATOR);
        }
        identity.append(shortName);

        if (FactionFlags.isTerritorial(faction)) {
            identity.append(TERRITORIAL_MARK);
        }
        return identity.toString();
    }

    // Where a faction came from, with the mod id beside the name for a source that has
    // one - the name is what a reader recognises, the id what another command takes.
    private static String describeSource(ModSource source) {

        if (source == null) {
            return UNATTRIBUTED_SOURCE;
        }
        if (!source.hasModId()) {
            return source.sourceName();
        }
        return source.sourceName() + " [" + source.modId() + "]";
    }

    // What the faction holds: the count of places, how many are concealed, how
    // many are undiscovered, and the systems they sit in.
    private static void appendHoldingsLine(
            StringBuilder report,
            FactionHoldings holdings,
            FactionListingFilter filter) {

        var placeCount = holdings.countPlaces();

        report
            .append(HOLDINGS_INDENT)
            .append("holdings: ")
            .append(placeCount);

        if (placeCount == 0) {
            // Nothing held leaves the visibility counts and the systems clause
            // with nothing to say, and a row of noughts reads as a finding rather
            // than as the absence it is.
            return;
        }

        // The counts are of everything the faction holds even under a filter: the
        // keyword decides who is listed, not what is reported about them.
        report
            .append(" (")
            .append(holdings.countHiddenPlaces())
            .append(" hidden, ")
            .append(holdings.countDiscoverablePlaces())
            .append(" discoverable)");

        // The clause, unlike the counts, does narrow - under "hidden" it names the
        // systems holding a hidden place, which is what makes the filter useful
        // for finding where the matches actually are.
        var systemLabels = holdings.collectSystemLabels(filter.getHoldingSelection());

        if (!systemLabels.isEmpty()) {

            report
                .append("  systems: ")
                .append(String.join(", ", systemLabels));
        }
    }

    // Every faction the sector knows, ordered by id. The sector's own order is
    // load order, which differs between installs and makes two runs hard to
    // compare; a listing of thirty-odd factions is also read by looking one up.
    private static List<FactionAPI> collectFactionsSortedById(SectorAPI sector) {

        var factions = new ArrayList<FactionAPI>(sector.getAllFactions());
        factions.sort(Comparator.comparing(FactionAPI::getId));
        return factions;
    }

    // The filter keywords, pipe-separated, as the usage line offers them. Walked off
    // the filters so the keyword list exists in exactly one place: the enum. ALL
    // carries none, being the absence of a keyword rather than one of them.
    private static String describeFilterKeywords() {

        var keywords = new ArrayList<String>();

        for (var filter : FactionListingFilter.values()) {

            if (filter.getKeyword() != null) {
                keywords.add(filter.getKeyword());
            }
        }
        return String.join("|", keywords);
    }

    // The option keywords, each in its own brackets, as the usage line offers them.
    // Bracketed apart rather than pipe-separated because they compose: a run may name
    // all three, which a single alternation would read as a choice between them.
    private static String describeOptionKeywords() {

        var keywords = new ArrayList<String>();

        for (var option : ListingOption.values()) {
            keywords.add("[" + option.getKeyword() + "]");
        }
        return String.join(" ", keywords);
    }

    // The usage line, composed from both keyword lists rather than written out.
    private static String describeUsage() {

        return "Usage: kmlib_list_factions ["
            + describeFilterKeywords()
            + "] "
            + describeOptionKeywords()
            + ".";
    }

    // The colonies keyed by the id of the faction holding them. Every colony the
    // sector read yields is owned - that is the rule the read selects on - so the
    // owner is always there to key by.
    private static Map<String, List<Colony>> groupColoniesByFactionId(List<Colony> colonies) {

        var coloniesByFactionId = new LinkedHashMap<String, List<Colony>>();

        for (var colony : colonies) {

            coloniesByFactionId
                .computeIfAbsent(colony.market().getFaction().getId(), id -> new ArrayList<>())
                .add(colony);
        }
        return coloniesByFactionId;
    }

    // Whether the player has yet to find this colony's body. The negation of the
    // discovery read rather than a read of the entity's own flag, so a colony with
    // no entity at all - nothing left to find - is not reported as findable.
    private static boolean isStillDiscoverable(Colony colony) {
        return !MarketVisibility.isDiscoveredByPlayer(colony.market());
    }

    private static String readPlayerFactionId(SectorAPI sector) {

        var playerFaction = sector.getPlayerFaction();
        return playerFaction == null
            ? null
            : playerFaction.getId();
    }

    /**
     * What a run leaves off, and where it writes. Unlike the filters these compose:
     * each is answered independently, and a run may name any, all or none of them.
     *
     * <p>Together in one enum because they are one thing to the player - the words
     * typed after the filter - and are walked as one to build the usage line. Each is
     * read by whichever part of the run it belongs to: the two that shape the report
     * by the report, and the routing one by the command that decides where to put it.
     */
    enum ListingOption {

        /** {@code no_holdings}: the holdings line goes, leaving one line per faction. */
        OMIT_HOLDINGS("no_holdings"),

        /** {@code no_attitude}: the faction's standing with the player goes. */
        OMIT_ATTITUDE("no_attitude"),

        /** {@code to_log}: the report goes to the game log rather than the console. */
        WRITE_TO_LOG("to_log");

        private final String keyword;

        ListingOption(String keyword) {
            this.keyword = keyword;
        }

        /** @return the bare keyword that selects this option */
        private String getKeyword() {
            return keyword;
        }
    }

    /**
     * Which factions a run lists, and which of a listed faction's holdings its
     * systems clause names. {@link #ALL} is the unfiltered run, the remaining four
     * the keywords the command accepts.
     *
     * <p>Each carries the holdings it selects, so the inclusion test and the
     * systems clause agree by construction: a faction listed for holding a hidden
     * place is one whose clause names the systems those places are in.
     */
    enum FactionListingFilter {

        /** No keyword given: every faction, with every holding named. */
        ALL(null, colony -> true),

        /** {@code markets}: a faction holding at least one place of any kind. */
        HOLDS_ANYTHING("markets", colony -> true),

        /** {@code hidden}: a faction holding at least one concealed place. */
        HOLDS_HIDDEN("hidden", Colony::isHidden),

        /** {@code discoverable}: a faction holding at least one undiscovered place. */
        HOLDS_DISCOVERABLE("discoverable", ListFactionsCommand::isStillDiscoverable),

        /** {@code no_markets}: a faction holding nothing at all. */
        HOLDS_NOTHING("no_markets", colony -> true);

        private final String keyword;
        private final Predicate<Colony> holdingSelection;

        FactionListingFilter(String keyword, Predicate<Colony> holdingSelection) {
            this.keyword = keyword;
            this.holdingSelection = holdingSelection;
        }

        /**
         * @return the bare keyword that selects this filter, or null for the
         *         unfiltered run, which has none to name in the header
         */
        private String getKeyword() {
            return keyword;
        }

        private Predicate<Colony> getHoldingSelection() {
            return holdingSelection;
        }

        /**
         * Whether a faction holding {@code holdings} belongs in the listing.
         *
         * <p>Counts the holdings this filter selects and answers on that count, so
         * no caller has to know that "which holdings count" and "how many is enough"
         * are two halves of one rule.
         *
         * <p>Exhaustive rather than defaulted on purpose: a filter added to the enum
         * has to state its own rule here, where a default arm would silently give it
         * "at least one" and read as deliberate.
         *
         * @param holdings the places the faction holds
         * @return true when the faction belongs in the listing
         */
        private boolean shouldList(FactionHoldings holdings) {

            var matchingCount = holdings.countPlacesMatching(holdingSelection);

            return switch (this) {
                case ALL -> true;
                case HOLDS_NOTHING -> matchingCount == 0;
                case HOLDS_ANYTHING, HOLDS_HIDDEN, HOLDS_DISCOVERABLE -> matchingCount > 0;
            };
        }
    }

    /**
     * The places one faction holds, with the reads the listing poses of them.
     *
     * <p>The same set answers every question one faction's row raises - whether it
     * is listed at all, how many places, how many concealed, how many undiscovered, and
     * in which systems - so it is named once here rather than each question
     * re-walking the sector.
     */
    private static final class FactionHoldings {

        private final List<Colony> colonies;

        private FactionHoldings(List<Colony> colonies) {
            this.colonies = colonies;
        }

        // The distinct systems the selected places sit in, sorted by id, with
        // hyperspace named once and last - as the sector read itself puts it after
        // the systems, since it is not one and an ordinary id is what a reader
        // should meet first.
        private List<String> collectSystemLabels(Predicate<Colony> selection) {

            var systemIds = new TreeSet<String>();
            var isHyperspaceHeld = false;

            for (var colony : colonies) {

                if (!selection.test(colony)) {
                    continue;
                }

                var system = colony.market().getStarSystem();

                if (system == null) {
                    isHyperspaceHeld = true;
                } else {
                    systemIds.add(system.getId());
                }
            }

            var labels = new ArrayList<String>(systemIds);

            if (isHyperspaceHeld) {
                labels.add(HYPERSPACE_LABEL);
            }
            return labels;
        }

        private int countDiscoverablePlaces() {
            return countPlacesMatching(ListFactionsCommand::isStillDiscoverable);
        }

        private int countHiddenPlaces() {
            return countPlacesMatching(Colony::isHidden);
        }

        private int countPlaces() {
            return colonies.size();
        }

        private int countPlacesMatching(Predicate<Colony> selection) {

            var count = 0;
            for (var colony : colonies) {
                if (selection.test(colony)) {
                    count++;
                }
            }
            return count;
        }
    }

    /**
     * What {@code kmlib_list_factions} accepts: one bare keyword per filter and per
     * option, in the order the usage line names them. Flags rather than value
     * parameters, so each is given as the bare word and none means the unfiltered
     * listing with nothing left off.
     *
     * <p>Declared as flag-to-keyword maps built by walking the two enums themselves,
     * so reading the parse result back gives the filters and options directly and the
     * keyword lists stay the enums' alone - the flags, the usage line and what each
     * keyword selects cannot drift apart, and a keyword added to either enum arrives
     * with its flag rather than waiting for a line here.
     */
    private static final class ListFactionsSpec extends ParameterSpec {

        private final Map<Parameter<Boolean>, FactionListingFilter> filtersByFlag =
            new LinkedHashMap<>();

        private final Map<Parameter<Boolean>, ListingOption> optionsByFlag =
            new LinkedHashMap<>();

        private ListFactionsSpec() {

            super(USAGE);

            // Enum order, which is also the order the usage line names them, both
            // being read off the same values().
            for (var filter : FactionListingFilter.values()) {

                var keyword = filter.getKeyword();

                if (keyword != null) {
                    filtersByFlag.put(acceptsFlag(keyword), filter);
                }
            }

            for (var option : ListingOption.values()) {
                optionsByFlag.put(acceptsFlag(option.getKeyword()), option);
            }
        }

        // Which filters the player asked for, in declaration order. Returns all of
        // them rather than one, so the command can tell "none given" from "more
        // than one given" - the latter being the mistake it reports.
        private List<FactionListingFilter> collectSelectedFilters(ParsedParameters parsed) {

            var selected = new ArrayList<FactionListingFilter>();

            for (var entry : filtersByFlag.entrySet()) {

                if (Boolean.TRUE.equals(parsed.get(entry.getKey()))) {
                    selected.add(entry.getValue());
                }
            }
            return selected;
        }

        // Which options the player asked for. A set rather than a list, since these
        // compose and no consumer cares which order they were typed in.
        private Set<ListingOption> collectSelectedOptions(ParsedParameters parsed) {

            var selected = EnumSet.noneOf(ListingOption.class);

            for (var entry : optionsByFlag.entrySet()) {

                if (Boolean.TRUE.equals(parsed.get(entry.getKey()))) {
                    selected.add(entry.getValue());
                }
            }
            return selected;
        }
    }
}
