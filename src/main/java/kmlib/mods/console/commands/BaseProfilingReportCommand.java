package kmlib.mods.console.commands;

import kmlib.mods.console.commands.output.CommandOutput;
import kmlib.mods.console.commands.output.ConsoleCommandOutput;
import kmlib.mods.console.commands.output.GameLogCommandOutput;
import kmlib.mods.console.commands.parsing.Parameter;
import kmlib.mods.console.commands.parsing.ParameterSpec;
import kmlib.mods.console.commands.parsing.ParameterValues;
import kmlib.mods.console.commands.parsing.ParsedParameters;
import kmlib.profiling.ActiveProfiler;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.Profiler;
import kmlib.profiling.report.ProfileReportRequest;
import kmlib.profiling.report.TimingReport;
import kmlib.starsector.SectorWalkCounters;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Base for a console command that writes the accumulated performance timings to the game log, or
 * clears them when invoked with {@code reset}.
 *
 * <p>A subclass names only what is its own: the word the console knows the command by, the label
 * its notices carry, and which beat a frame is counted by. Profiling knows that a row was opened
 * some number of times; which section a frame opens once is the decision of whoever opened it.
 *
 * <p>Every reading goes to the log and only a notice to the overlay. A capture is a table wider
 * than the console can lay out, so what the overlay would show is a wrapped and unreadable copy of
 * what the log holds properly aligned - and the log is the file a player already attaches. The
 * overlay says where the reading landed rather than being left silent, since a command that
 * visibly did nothing reads as one that failed.
 *
 * <p>The profiler is resolved per invocation rather than held, because a level knob can rebind
 * it: a command holding the profiler it was built with would report the capture the player
 * switched away from.
 */
public abstract class BaseProfilingReportCommand
        extends BaseKmlibCommand {

    private static final String TIMINGS_LOGGED_SUFFIX =
        " timings written to the game log (starsector-core/starsector.log).";
    private static final String TIMINGS_RESET_SUFFIX = " timings reset.";

    private final ProfileSection frameBeat;
    private final CommandOutput log;
    private final Supplier<Profiler> resolveBoundProfiler;
    private final ProfilingSpec spec;
    private final String timingsLoggedNotice;
    private final String timingsResetNotice;

    /**
     * Binds the command to the active profiler, the live console and the game log.
     *
     * @param commandName the word the console knows the command by, as its usage line spells it
     * @param noticeLabel what the overlay notices name the timings after
     * @param frameBeat   the section opened once a frame, which {@code perframe} divides by
     */
    protected BaseProfilingReportCommand(String commandName, String noticeLabel, ProfileSection frameBeat) {

        this(
            commandName,
            noticeLabel,
            frameBeat,
            ActiveProfiler::resolveProfiler,
            ConsoleCommandOutput.INSTANCE,
            GameLogCommandOutput.INSTANCE);
    }

    /**
     * Binds the command to the given profiler lookup and sinks.
     *
     * @param commandName          the word the console knows the command by, as its usage line spells it
     * @param noticeLabel          what the overlay notices name the timings after
     * @param frameBeat            the section opened once a frame, which {@code perframe} divides by
     * @param resolveBoundProfiler the profiler to act on, asked for again at every invocation
     * @param output               where the notices and any parse problem go
     * @param log                  where the reading goes
     */
    protected BaseProfilingReportCommand(
            String commandName,
            String noticeLabel,
            ProfileSection frameBeat,
            Supplier<Profiler> resolveBoundProfiler,
            CommandOutput output,
            CommandOutput log) {

        super(output);

        Objects.requireNonNull(noticeLabel, "noticeLabel");

        this.frameBeat = Objects.requireNonNull(frameBeat, "frameBeat");
        this.log = Objects.requireNonNull(log, "log");
        this.resolveBoundProfiler = Objects.requireNonNull(resolveBoundProfiler, "resolveBoundProfiler");
        this.spec = new ProfilingSpec(commandName);
        this.timingsLoggedNotice = noticeLabel + TIMINGS_LOGGED_SUFFIX;
        this.timingsResetNotice = noticeLabel + TIMINGS_RESET_SUFFIX;
    }

    @Override
    public final CommandResult runCommand(String args, CommandContext context) {

        // No context guard: a capture is worth reading wherever the console opens.
        var parsed = readInput(context, args).parseArguments(spec);

        if (!parsed.isValid()) {
            return parsed.getResult();
        }

        var profiler = resolveBoundProfiler.get();

        if (parsed.get(spec.reset)) {

            profiler.reset();
            output.showMessage(timingsResetNotice);
            return CommandResult.SUCCESS;
        }

        log.showMessage(TimingReport.format(profiler.snapshot(), buildRequest(parsed)));
        output.showMessage(timingsLoggedNotice);

        return CommandResult.SUCCESS;
    }

    // The reading, narrowed by whatever else was asked for. Each narrowing states
    // its own "everything" default, so an invocation that named only a view asks
    // for the whole capture without the command spelling that out.
    private ProfileReportRequest buildRequest(ParsedParameters parsed) {

        var request = parsed.get(spec.view)
            .limitToNamespace(parsed.get(spec.namespace))
            .limitToTopRows(parsed.get(spec.top));

        return parsed.get(spec.perFrame)
            ? request.divideByFramesOf(frameBeat)
            : request;
    }

    /**
     * What the command accepts: which reading of the capture to write, how to narrow it, whether
     * to divide it by the frames it was measured over, and a {@code reset} that clears the timings
     * instead of reporting them.
     *
     * <p>The reading is a positional, being the one thing an invocation is mostly about; the
     * narrowings are named, since two numbers given positionally could not be told apart; and the
     * per-frame division and the reset are bare keywords, absence being the answer for both. The
     * reset stands alone, because it writes no reading for the others to shape.
     */
    private static final class ProfilingSpec
            extends ParameterSpec {

        private static final String EXPECTED_TOP = "a whole number of rows above zero";

        private final Parameter<ProfileReportRequest> view =
            acceptsKeyword("view", buildRequestsByView())
                .defaultsTo(ProfileReportRequest.showTree());

        private final Parameter<String> namespace =
            acceptsNamed("namespace", "<prefix>", ParameterValues.text())
                .defaultsTo(ProfileReportRequest.EVERY_NAMESPACE);

        private final Parameter<Integer> top =
            acceptsNamed("top", "<count>", ParameterValues.positiveWholeNumber(EXPECTED_TOP))
                .defaultsTo(ProfileReportRequest.EVERY_ROW);

        private final Parameter<Boolean> perFrame = acceptsFlag("perframe");
        private final Parameter<Boolean> reset = acceptsFlag("reset").markStandalone();

        private ProfilingSpec(String commandName) {
            super(commandName);
        }

        // The word each reading is asked for by, in the order the usage line offers them. The
        // walks reading sorts on the sector-walk counter, the one traversal every pass shares, so
        // it is the reading worth a word of its own.
        private static Map<String, ProfileReportRequest> buildRequestsByView() {

            var requestsByView = new LinkedHashMap<String, ProfileReportRequest>();

            requestsByView.put("tree", ProfileReportRequest.showTree());
            requestsByView.put("flat", ProfileReportRequest.showRowsBySelfTime());
            requestsByView.put("walks", ProfileReportRequest.showRowsCounting(SectorWalkCounters.SECTOR_WALKS));

            return requestsByView;
        }
    }
}
