package kmlib.console.parsing;

import kmlib.testfixtures.console.output.CommandOutputFake;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link ParameterSpec#parse} (and the {@link ParameterParser} engine
 * behind it) across the spec-driven behaviour: named and positional values reach
 * their typed keys; a named value drops its positional slot so bare tokens fill
 * the rest in declared order; a name-only parameter never claims a slot; an
 * unknown name, a value its parser rejects (named or positional), a missing
 * required parameter, and surplus positionals are each reported as bad syntax
 * (with the usage line omitted when the spec declares none); an unsupplied
 * parameter falls back to its default while {@link ParsedParameters#isSupplied}
 * still reports it absent; a null argument string parses as nothing supplied; and
 * a flag reads true only when its bare keyword is present, never claims a
 * positional slot, and is rejected when given a value. Feedback is read back
 * through a recording {@link CommandOutputFake}. Cases live under a
 * {@link Nested} group named for the method under test.
 */
final class ParameterParserTest {

    private static final String USAGE = "Usage: spawn <kind> [focus] [speed] [jitter=<frac>].";

    private CommandOutputFake outputFake;

    @BeforeEach
    void setUp() {
        outputFake = new CommandOutputFake();
    }

    // A two-positional, one-name-only spec mirroring the spawn command's shape,
    // its keys exposed so a test can read parsed values back by key.
    private static final class SampleSpec extends ParameterSpec {
        private final Parameter<String> focus =
            acceptsPositional("focus", "<id>", ParameterValues.text()).defaultsTo("");
        private final Parameter<Float> speed = acceptsPositional("speed", "<deg/day>",
            ParameterValues.decimal("a number in degrees per day"));
        private final Parameter<Float> jitter = acceptsNamed("jitter", "<frac>",
            ParameterValues.nonNegativeDecimal("a non-negative fraction (e.g. 0.25)"))
            .defaultsTo(0.25f);

        private SampleSpec() {
            super(USAGE);
        }
    }

    @Nested
    class Parse {
        private final SampleSpec spec = new SampleSpec();

        @Test
        void reads_named_values_into_their_typed_keys() {
            var parsed = spec.parse(
                new String[] {"focus=beta", "speed=5", "jitter=0.5"}, outputFake);

            assertThat(parsed.isValid()).isTrue();
            assertThat(parsed.get(spec.focus)).isEqualTo("beta");
            assertThat(parsed.get(spec.speed)).isEqualTo(5f);
            assertThat(parsed.get(spec.jitter)).isEqualTo(0.5f);
        }

        @Test
        void fills_positional_slots_in_declared_order() {
            var parsed = spec.parse(new String[] {"beta", "5"}, outputFake);

            assertThat(parsed.isValid()).isTrue();
            // First bare token is the focus, the second the speed.
            assertThat(parsed.get(spec.focus)).isEqualTo("beta");
            assertThat(parsed.get(spec.speed)).isEqualTo(5f);
        }

        @Test
        void a_named_value_drops_its_positional_slot() {
            var parsed = spec.parse(new String[] {"focus=beta", "5"}, outputFake);

            assertThat(parsed.isValid()).isTrue();
            // Focus is taken by name, so the lone bare token fills the next open
            // slot - speed - rather than re-filling focus.
            assertThat(parsed.get(spec.focus)).isEqualTo("beta");
            assertThat(parsed.get(spec.speed)).isEqualTo(5f);
        }

        @Test
        void matches_parameter_names_case_insensitively() {
            var parsed = spec.parse(new String[] {"FOCUS=beta"}, outputFake);

            assertThat(parsed.isValid()).isTrue();
            assertThat(parsed.get(spec.focus)).isEqualTo("beta");
        }

        @Test
        void returns_the_default_for_an_unsupplied_parameter() {
            var parsed = spec.parse(new String[0], outputFake);

            assertThat(parsed.isValid()).isTrue();
            assertThat(parsed.get(spec.jitter)).isEqualTo(0.25f);
            assertThat(parsed.get(spec.speed)).isNull();
            // A defaulted value is not the same as one the player typed.
            assertThat(parsed.isSupplied(spec.jitter)).isFalse();
        }

        @Test
        void reports_a_parameter_as_supplied_only_when_typed() {
            var parsed = spec.parse(new String[] {"jitter=0.5"}, outputFake);

            assertThat(parsed.isSupplied(spec.jitter)).isTrue();
        }

        @Test
        void rejects_an_unknown_named_parameter() {
            var parsed = spec.parse(new String[] {"colour=red"}, outputFake);

            assertThat(parsed.isValid()).isFalse();
            assertThat(parsed.getResult()).isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Unknown parameter 'colour'")
                        && message.contains("focus=<id>")
                        && message.contains("speed=<deg/day>")
                        && message.contains("jitter=<frac>"));
        }

        @Test
        void rejects_a_value_its_parser_refuses_and_frames_the_clause() {
            var parsed = spec.parse(new String[] {"speed=fast"}, outputFake);

            assertThat(parsed.isValid()).isFalse();
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.equals(
                    "Invalid speed 'fast'. Expected a number in degrees per day."));
        }

        @Test
        void rejects_a_positional_value_its_parser_refuses() {
            // The first bare token fills focus; the second fills speed, whose
            // parser rejects it - the same framing as a named bad value, reached
            // through the positional fill rather than a name=value token.
            var parsed = spec.parse(new String[] {"beta", "fast"}, outputFake);

            assertThat(parsed.isValid()).isFalse();
            assertThat(parsed.getResult()).isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.equals(
                    "Invalid speed 'fast'. Expected a number in degrees per day."));
        }

        @Test
        void parses_a_null_argument_string_as_nothing_supplied() {
            // Console Commands hands a bare invocation an empty string, but the
            // whole-string entry point tolerates null too: it yields no tokens, so
            // every parameter falls back to its default.
            var parsed = spec.parse((String) null, outputFake);

            assertThat(parsed.isValid()).isTrue();
            assertThat(parsed.get(spec.focus)).isEqualTo("");
            assertThat(parsed.isSupplied(spec.speed)).isFalse();
        }

        @Test
        void omits_the_usage_line_when_the_spec_declares_none() {
            // A spec may carry no usage line; the surplus-argument message then
            // stands on its own rather than trailing an empty suffix.
            var parsed = new NoUsageSpec().parse(new String[] {"extra"}, outputFake);

            assertThat(parsed.isValid()).isFalse();
            assertThat(outputFake.getMessages()).contains("Too many arguments.");
        }

        @Test
        void rejects_a_negative_value_with_the_same_clause_as_a_non_numeric_one() {
            var parsed = spec.parse(new String[] {"jitter=-0.5"}, outputFake);

            assertThat(parsed.isValid()).isFalse();
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.equals(
                    "Invalid jitter '-0.5'. Expected a non-negative fraction (e.g. 0.25)."));
        }

        @Test
        void rejects_more_positionals_than_open_slots_with_the_usage_line() {
            var parsed = spec.parse(new String[] {"beta", "5", "extra"}, outputFake);

            assertThat(parsed.isValid()).isFalse();
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Too many arguments")
                        && message.contains(USAGE));
        }

        @Test
        void does_not_let_a_name_only_parameter_take_a_positional_slot() {
            // focus and speed are the only two slots; a third bare token has no
            // slot because jitter is name-only.
            var parsed = spec.parse(new String[] {"beta", "5", "0.5"}, outputFake);

            assertThat(parsed.isValid()).isFalse();
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Too many arguments"));
        }
    }

    @Nested
    class ParseRequired {
        // A spec with a single required parameter, to pin the required check apart
        // from the optional sample above.
        private final RequiredSpec spec = new RequiredSpec();

        @Test
        void rejects_a_missing_required_parameter() {
            var parsed = spec.parse(new String[0], outputFake);

            assertThat(parsed.isValid()).isFalse();
            assertThat(parsed.getResult()).isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Missing required parameter 'target'"));
        }

        @Test
        void accepts_a_required_parameter_once_supplied() {
            var parsed = spec.parse(new String[] {"target=beta"}, outputFake);

            assertThat(parsed.isValid()).isTrue();
            assertThat(parsed.get(spec.target)).isEqualTo("beta");
        }
    }

    private static final class RequiredSpec extends ParameterSpec {
        private final Parameter<String> target =
            acceptsNamed("target", "<id>", ParameterValues.text()).markRequired();

        private RequiredSpec() {
            super("Usage: sample target=<id>.");
        }
    }

    // A spec with no usage line, to pin that the surplus-argument message omits
    // the trailing usage suffix rather than appending an empty one.
    private static final class NoUsageSpec extends ParameterSpec {
        private NoUsageSpec() {
            super(null);
        }
    }

    @Nested
    class ParseFlag {
        private final FlagSpec spec = new FlagSpec();

        @Test
        void sets_a_flag_present_as_a_bare_keyword() {
            var parsed = spec.parse(new String[] {"verbose"}, outputFake);

            assertThat(parsed.isValid()).isTrue();
            assertThat(parsed.get(spec.verbose)).isTrue();
            assertThat(parsed.isSupplied(spec.verbose)).isTrue();
        }

        @Test
        void leaves_an_absent_flag_false_and_unsupplied() {
            var parsed = spec.parse(new String[0], outputFake);

            assertThat(parsed.isValid()).isTrue();
            assertThat(parsed.get(spec.verbose)).isFalse();
            assertThat(parsed.isSupplied(spec.verbose)).isFalse();
        }

        @Test
        void a_flag_does_not_claim_a_positional_slot() {
            var parsed = spec.parse(new String[] {"alpha", "verbose"}, outputFake);

            assertThat(parsed.isValid()).isTrue();
            // The flag is matched by name first, so the lone bare value still
            // fills the single positional slot rather than being crowded out.
            assertThat(parsed.get(spec.name)).isEqualTo("alpha");
            assertThat(parsed.get(spec.verbose)).isTrue();
        }

        @Test
        void rejects_a_flag_given_a_value() {
            var parsed = spec.parse(new String[] {"verbose=1"}, outputFake);

            assertThat(parsed.isValid()).isFalse();
            assertThat(parsed.getResult()).isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("'verbose' is a flag"));
        }

        @Test
        void lists_a_flag_as_a_bare_keyword_when_naming_an_unknown_parameter() {
            var parsed = spec.parse(new String[] {"colour=red"}, outputFake);

            assertThat(parsed.isValid()).isFalse();
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Unknown parameter 'colour'")
                        && message.contains("name=<id>")
                        // The flag is offered as a bare keyword, not name=hint.
                        && message.contains("verbose")
                        && !message.contains("verbose="));
        }
    }

    // A spec pairing one positional with one flag, to pin flag presence and that
    // a flag stays out of the positional order.
    private static final class FlagSpec extends ParameterSpec {
        private final Parameter<String> name =
            acceptsPositional("name", "<id>", ParameterValues.text()).defaultsTo("");
        private final Parameter<Boolean> verbose = acceptsFlag("verbose");

        private FlagSpec() {
            super("Usage: sample [name] [verbose].");
        }
    }

    @Nested
    class TakingNoArguments {
        private final ParameterSpec spec =
            ParameterSpec.takingNoArguments("Usage: sample.");

        @Test
        void accepts_an_empty_argument_list() {
            var parsed = spec.parse(new String[0], outputFake);

            assertThat(parsed.isValid()).isTrue();
            assertThat(outputFake.getMessages()).isEmpty();
        }

        @Test
        void rejects_any_argument_with_the_usage_line() {
            var parsed = spec.parse(new String[] {"stray"}, outputFake);

            assertThat(parsed.isValid()).isFalse();
            assertThat(parsed.getResult()).isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Too many arguments")
                        && message.contains("Usage: sample."));
        }
    }
}
