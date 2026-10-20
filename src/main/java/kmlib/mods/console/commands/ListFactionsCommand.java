package kmlib.mods.console.commands;

import com.fs.starfarer.api.Global;

import kmlib.mods.console.ConsoleStrings;
import kmlib.mods.console.commands.output.CommandOutput;
import kmlib.mods.console.commands.output.GameLogCommandOutput;
import kmlib.mods.console.commands.parsing.Parameter;
import kmlib.mods.console.commands.parsing.ParameterSpec;
import kmlib.mods.console.commands.parsing.ParsedParameters;
import kmlib.starsector.factions.FactionSourceMods;
import kmlib.starsector.strings.KmlibStringKeys;

import java.util.EnumSet;
import java.util.LinkedHashMap;
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
 * the words the player typed and deciding where the answer goes.
 *
 * <p>Soft Console Commands dependency: this class touches
 * {@code org.lazywizard.console.*}, but it is loaded only when Console Commands
 * instantiates it from KMLib's commands.csv. A game without Console Commands
 * never loads it, so KMLib runs fine without that mod - no hard dependency is
 * declared.
 */
public final class ListFactionsCommand
        extends BaseKmlibCommand {

    private static final String COMMAND_NAME = "kmlib_list_factions";

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

        var options = SPEC.collectSelectedOptions(parsed);

        var report = new FactionListingReport(
            parsed.get(SPEC.filter),
            options,
            FactionSourceMods.readSourcesByFactionId(Global.getSettings()))
            .describeFactions(readActiveSector());

        // The log keeps what the console scrolls away, which is what a listing this
        // long is usually wanted for; the console still hears that it happened, so a run
        // that printed nothing where the player is looking does not read as one that did nothing.
        if (options.contains(ListingOption.WRITE_TO_LOG)) {

            GameLogCommandOutput.INSTANCE.showMessage(report);
            output.showMessage(ConsoleStrings.get(
                KmlibStringKeys.CATEGORY,
                KmlibStringKeys.CONSOLE_FACTIONS_WRITTEN_TO_LOG));

        } else {
            output.showMessage(report);
        }

        return CommandResult.SUCCESS;
    }

    /**
     * What {@code kmlib_list_factions} accepts: at most one filter keyword, and any of the option
     * keywords alongside it. None means the unfiltered listing with nothing left off.
     *
     * <p>The filter is one positional parameter whose value is the filter its keyword names, so a
     * second filter is a surplus argument the parser refuses. The keywords are alternatives rather
     * than predicates to combine: no_markets contradicts the other three, and a pair such as
     * "hidden discoverable" has no single honest answer for the systems clause. The options are
     * flags, since they compose.
     *
     * <p>Both are built by walking the two enums, so the keyword lists stay the enums' alone, and a
     * keyword added to either arrives with its parameter rather than waiting for a line here.
     */
    private static final class ListFactionsSpec extends ParameterSpec {

        private final Parameter<FactionListingFilter> filter =
            acceptsKeyword("filter", buildFiltersByKeyword())
                .defaultsTo(FactionListingFilter.ALL);

        private final Map<Parameter<Boolean>, ListingOption> optionsByFlag =
            new LinkedHashMap<>();

        private ListFactionsSpec() {

            super(COMMAND_NAME);

            for (var option : ListingOption.values()) {
                optionsByFlag.put(acceptsFlag(option.getKeyword()), option);
            }
        }

        // Which options the player asked for. A set, since these compose and no
        // consumer cares which order they were typed in.
        private Set<ListingOption> collectSelectedOptions(ParsedParameters parsed) {

            var selected = EnumSet.noneOf(ListingOption.class);

            for (var entry : optionsByFlag.entrySet()) {

                if (Boolean.TRUE.equals(parsed.get(entry.getKey()))) {
                    selected.add(entry.getValue());
                }
            }

            return selected;
        }

        // Enum order, which is also the order the usage line names them. ALL carries
        // no keyword, being the absence of one.
        private static Map<String, FactionListingFilter> buildFiltersByKeyword() {

            var filtersByKeyword = new LinkedHashMap<String, FactionListingFilter>();

            for (var listingFilter : FactionListingFilter.values()) {

                if (listingFilter.getKeyword() != null) {
                    filtersByKeyword.put(listingFilter.getKeyword(), listingFilter);
                }
            }

            return filtersByKeyword;
        }
    }
}
