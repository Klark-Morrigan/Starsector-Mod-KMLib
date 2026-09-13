package kmlib.mods.console.commands;

import kmlib.mods.console.commands.output.CommandOutput;
import kmlib.mods.console.commands.output.GameLogCommandOutput;
import kmlib.mods.console.commands.parsing.Parameter;
import kmlib.mods.console.commands.parsing.ParameterSpec;
import kmlib.mods.console.commands.parsing.ParsedParameters;
import kmlib.starsector.factions.FactionSourceMods;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Console command (dev tool): lists every faction in the sector with which mod it
 * came from and what it holds. Takes an optional filter keyword narrowing which
 * factions are listed, and any of the options that drop a part of each entry or
 * send the whole listing to the game log rather than the console.
 *
 * <p>The listing itself is {@link FactionListingReport}'s. What is left here is
 * everything that is true of a console command rather than of a listing: reading
 * the words the player typed, rejecting a pair of keywords that contradict each
 * other, and deciding where the answer goes.
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

        var report = new FactionListingReport(
            filter,
            options,
            FactionSourceMods.readSourcesByFactionId())
            .describeFactions(readActiveSector());

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
            return gatherSelected(filtersByFlag, parsed, new ArrayList<>());
        }

        // Which options the player asked for. A set rather than a list, since these
        // compose and no consumer cares which order they were typed in.
        private Set<ListingOption> collectSelectedOptions(ParsedParameters parsed) {
            return gatherSelected(optionsByFlag, parsed, EnumSet.noneOf(ListingOption.class));
        }

        // The keywords of one map the player actually typed, gathered into the
        // collection the caller wants them in. Takes that collection rather than
        // building one, because the two callers differ in nothing else: whether
        // repeats and order matter is the whole difference between a filter set and
        // an option set, and it is the collection that states it.
        private static <T, C extends Collection<T>> C gatherSelected(
                Map<Parameter<Boolean>, T> keywordsByFlag,
                ParsedParameters parsed,
                C selected) {

            for (var entry : keywordsByFlag.entrySet()) {

                if (Boolean.TRUE.equals(parsed.get(entry.getKey()))) {
                    selected.add(entry.getValue());
                }
            }
            return selected;
        }
    }
}
