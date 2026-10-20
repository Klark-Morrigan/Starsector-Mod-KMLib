package kmlib.mods.console.commands.parsing;

import kmlib.testfixtures.mods.console.commands.output.CommandOutputFake;
import kmlib.testfixtures.starsector.strings.KmlibStringsSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandResult;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins {@link ParameterSpec#parse} (and the {@link ParameterParser} engine
 * behind it) across the spec-driven behaviour: named and positional values reach
 * their typed keys; a named value drops its positional slot so bare tokens fill
 * the rest in declared order; a name-only parameter never claims a slot; an
 * unknown name, a value its parser rejects (named or positional), a missing
 * required parameter, and surplus positionals are each reported as bad syntax;
 * an unsupplied parameter falls back to its default while
 * {@link ParsedParameters#isSupplied} still reports it absent; a null argument
 * string parses as nothing supplied; a flag reads true only when its bare keyword
 * is present, never claims a positional slot, and is rejected when given a value;
 * a keyword parameter reads the value its word selects; and a standalone
 * parameter is refused alongside anything else. Also pins the usage line the spec
 * composes from its declarations, and the command name it refuses. Feedback is
 * read back through a recording {@link CommandOutputFake}. Cases live under a
 * {@link Nested} group named for the method under test.
 */
final class ParameterParserTests {

    private static final String USAGE = "Usage: spawn <kind> [<id>] [<deg/day>] [jitter=<frac>].";

    private CommandOutputFake outputFake;

    @BeforeEach
    void setUp() {
        outputFake = new CommandOutputFake();
    }

    // A two-positional, one-name-only spec mirroring the spawn command's shape,
    // its keys exposed so a test can read parsed values back by key.
    private static final class SampleSpec extends ParameterSpec {

        private final Parameter<String> focus =
            acceptsPositional("focus", "<id>", ParameterValues.text())
                .defaultsTo("");

        private final Parameter<Float> speed = acceptsPositional(
            "speed",
            "<deg/day>",
            ParameterValues.decimal("a number in degrees per day"));

        private final Parameter<Float> jitter =
            acceptsNamed(
                "jitter",
                "<frac>",
                ParameterValues.nonNegativeDecimal("a non-negative fraction (e.g. 0.25)"))
            .defaultsTo(0.25f);

        private SampleSpec() {
            super("spawn", "<kind>");
        }
    }

    @BeforeEach
    void installStrings() {
        KmlibStringsSettingsFake.installSettings();
    }

    @AfterEach
    void clearStrings() {
        KmlibStringsSettingsFake.clearSettings();
    }

    @Nested
    class Parse {

        private final SampleSpec spec = new SampleSpec();

        @Test
        void readsNamedValuesIntoTheirTypedKeys() {

            var parsed = spec.parse(
                new String[] {"focus=beta", "speed=5", "jitter=0.5"}, outputFake);

            assertThat(parsed.isValid())
                .isTrue();
            assertThat(parsed.get(spec.focus))
                .isEqualTo("beta");
            assertThat(parsed.get(spec.speed))
                .isEqualTo(5f);
            assertThat(parsed.get(spec.jitter))
                .isEqualTo(0.5f);
        }

        @Test
        void fillsPositionalSlotsInDeclaredOrder() {

            var parsed = spec.parse(new String[] {"beta", "5"}, outputFake);

            assertThat(parsed.isValid())
                .isTrue();

            // First bare token is the focus, the second the speed.
            assertThat(parsed.get(spec.focus))
                .isEqualTo("beta");
            assertThat(parsed.get(spec.speed))
                .isEqualTo(5f);
        }

        @Test
        void aNamedValueDropsItsPositionalSlot() {

            var parsed = spec.parse(new String[] {"focus=beta", "5"}, outputFake);

            assertThat(parsed.isValid())
                .isTrue();

            // Focus is taken by name, so the lone bare token fills the next open
            // slot - speed - rather than re-filling focus.
            assertThat(parsed.get(spec.focus))
                .isEqualTo("beta");
            assertThat(parsed.get(spec.speed))
                .isEqualTo(5f);
        }

        @Test
        void matchesParameterNamesCaseInsensitively() {

            var parsed = spec.parse(new String[] {"FOCUS=beta"}, outputFake);

            assertThat(parsed.isValid())
                .isTrue();
            assertThat(parsed.get(spec.focus))
                .isEqualTo("beta");
        }

        @Test
        void returnsTheDefaultForAnUnsuppliedParameter() {

            var parsed = spec.parse(new String[0], outputFake);

            assertThat(parsed.isValid())
                .isTrue();
            assertThat(parsed.get(spec.jitter))
                .isEqualTo(0.25f);
            assertThat(parsed.get(spec.speed))
                .isNull();

            // A defaulted value is not the same as one the player typed.
            assertThat(parsed.isSupplied(spec.jitter))
                .isFalse();
        }

        @Test
        void reportsAParameterAsSuppliedOnlyWhenTyped() {

            var parsed = spec.parse(new String[] {"jitter=0.5"}, outputFake);

            assertThat(parsed.isSupplied(spec.jitter))
                .isTrue();
        }

        @Test
        void rejectsAnUnknownNamedParameter() {

            var parsed = spec.parse(new String[] {"colour=red"}, outputFake);

            assertThat(parsed.isValid())
                .isFalse();
            assertThat(parsed.getResult())
                .isEqualTo(CommandResult.BAD_SYNTAX);

            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Unknown parameter 'colour'")
                    && message.contains("focus=<id>")
                    && message.contains("speed=<deg/day>")
                    && message.contains("jitter=<frac>"));
        }

        @Test
        void rejectsAValueItsParserRefusesAndFramesTheClause() {

            var parsed = spec.parse(new String[] {"speed=fast"}, outputFake);

            assertThat(parsed.isValid())
                .isFalse();

            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.equals(
                    "Invalid speed 'fast'. Expected a number in degrees per day."));
        }

        @Test
        void rejectsAPositionalValueItsParserRefuses() {
            // The first bare token fills focus; the second fills speed, whose
            // parser rejects it - the same framing as a named bad value, reached
            // through the positional fill rather than a name=value token.
            var parsed = spec.parse(new String[] {"beta", "fast"}, outputFake);

            assertThat(parsed.isValid())
                .isFalse();
            assertThat(parsed.getResult())
                .isEqualTo(CommandResult.BAD_SYNTAX);

            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.equals(
                    "Invalid speed 'fast'. Expected a number in degrees per day."));
        }

        @Test
        void parsesANullArgumentStringAsNothingSupplied() {
            // Console Commands hands a bare invocation an empty string, but the
            // whole-string entry point tolerates null too: it yields no tokens, so
            // every parameter falls back to its default.
            var parsed = spec.parse((String) null, outputFake);

            assertThat(parsed.isValid())
                .isTrue();
            assertThat(parsed.get(spec.focus))
                .isEqualTo("");
            assertThat(parsed.isSupplied(spec.speed))
                .isFalse();
        }

        @Test
        void rejectsANegativeValueWithTheSameClauseAsANonNumericOne() {

            var parsed = spec.parse(new String[] {"jitter=-0.5"}, outputFake);

            assertThat(parsed.isValid())
                .isFalse();

            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.equals(
                    "Invalid jitter '-0.5'. Expected a non-negative fraction (e.g. 0.25)."));
        }

        @Test
        void rejectsMorePositionalsThanOpenSlotsWithTheUsageLine() {

            var parsed = spec.parse(new String[] {"beta", "5", "extra"}, outputFake);

            assertThat(parsed.isValid())
                .isFalse();

            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Too many arguments")
                    && message.contains(USAGE));
        }

        @Test
        void doesNotLetANameOnlyParameterTakeAPositionalSlot() {
            // focus and speed are the only two slots; a third bare token has no
            // slot because jitter is name-only.
            var parsed = spec.parse(new String[] {"beta", "5", "0.5"}, outputFake);

            assertThat(parsed.isValid())
                .isFalse();
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
        void rejectsAMissingRequiredParameter() {

            var parsed = spec.parse(new String[0], outputFake);

            assertThat(parsed.isValid())
                .isFalse();
            assertThat(parsed.getResult())
                .isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Missing required parameter 'target'"));
        }

        @Test
        void acceptsARequiredParameterOnceSupplied() {

            var parsed = spec.parse(new String[] {"target=beta"}, outputFake);

            assertThat(parsed.isValid())
                .isTrue();
            assertThat(parsed.get(spec.target))
                .isEqualTo("beta");
        }
    }

    private static final class RequiredSpec extends ParameterSpec {

        private final Parameter<String> target =
            acceptsNamed("target", "<id>", ParameterValues.text()).markRequired();

        private RequiredSpec() {
            super("sample");
        }
    }

    @Nested
    class ParseFlag {

        private final FlagSpec spec = new FlagSpec();

        @Test
        void setsAFlagPresentAsABareKeyword() {

            var parsed = spec.parse(new String[] {"verbose"}, outputFake);

            assertThat(parsed.isValid())
                .isTrue();
            assertThat(parsed.get(spec.verbose))
                .isTrue();
            assertThat(parsed.isSupplied(spec.verbose))
                .isTrue();
        }

        @Test
        void leavesAnAbsentFlagFalseAndUnsupplied() {

            var parsed = spec.parse(new String[0], outputFake);

            assertThat(parsed.isValid())
                .isTrue();
            assertThat(parsed.get(spec.verbose))
                .isFalse();
            assertThat(parsed.isSupplied(spec.verbose))
                .isFalse();
        }

        @Test
        void aFlagDoesNotClaimAPositionalSlot() {

            var parsed = spec.parse(new String[] {"alpha", "verbose"}, outputFake);

            assertThat(parsed.isValid())
                .isTrue();

            // The flag is matched by name first, so the lone bare value still
            // fills the single positional slot rather than being crowded out.
            assertThat(parsed.get(spec.name))
                .isEqualTo("alpha");
            assertThat(parsed.get(spec.verbose))
                .isTrue();
        }

        @Test
        void rejectsAFlagGivenAValue() {

            var parsed = spec.parse(new String[] {"verbose=1"}, outputFake);

            assertThat(parsed.isValid())
                .isFalse();
            assertThat(parsed.getResult())
                .isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("'verbose' is a flag"));
        }

        @Test
        void listsAFlagAsABareKeywordWhenNamingAnUnknownParameter() {

            var parsed = spec.parse(new String[] {"colour=red"}, outputFake);

            assertThat(parsed.isValid())
                .isFalse();

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
            super("sample");
        }
    }

    @Nested
    class ParseKeyword {

        private final KeywordSpec spec = new KeywordSpec();

        @Test
        void readsTheValueItsKeywordSelectsWhateverItsCase() {

            var parsed = spec.parse(new String[] {"FLAT"}, outputFake);

            assertThat(parsed.isValid())
                .isTrue();
            assertThat(parsed.get(spec.view))
                .isEqualTo(2);
        }

        @Test
        void answersAStrayWordWithTheKeywordsItAccepts() {

            var parsed = spec.parse(new String[] {"bogus"}, outputFake);

            assertThat(parsed.isValid())
                .isFalse();
            assertThat(outputFake.getMessages())
                .containsExactly("Invalid view 'bogus'. Expected one of tree, flat.");
        }

        @Test
        void reportsASecondKeywordAsSurplus() {
            // One parameter holds one value, so naming two of its words is the surplus the
            // usage line answers - which is what keeps alternatives from being combined.
            var parsed = spec.parse(new String[] {"tree", "flat"}, outputFake);

            assertThat(parsed.isValid())
                .isFalse();
            assertThat(outputFake.getMessages())
                .containsExactly("Too many arguments. Usage: sample [tree|flat].");
        }
    }

    // A single keyword parameter, its words listed in an order unlike the alphabet's, so a
    // message that lost the declared order would show it.
    private static final class KeywordSpec extends ParameterSpec {

        private final Parameter<Integer> view = acceptsKeyword("view", createValuesByView());

        private KeywordSpec() {
            super("sample");
        }

        private static Map<String, Integer> createValuesByView() {

            var valuesByView = new LinkedHashMap<String, Integer>();

            valuesByView.put("tree", 1);
            valuesByView.put("flat", 2);

            return valuesByView;
        }
    }

    @Nested
    class ParseStandalone {

        private final StandaloneSpec spec = new StandaloneSpec();

        @Test
        void acceptsAStandaloneParameterOnItsOwn() {
            // Given alone it is the whole invocation, so the required target is not asked for.
            var parsed = spec.parse(new String[] {"reset"}, outputFake);

            assertThat(parsed.isValid())
                .isTrue();
            assertThat(parsed.get(spec.reset))
                .isTrue();
            assertThat(parsed.isSupplied(spec.target))
                .isFalse();
        }

        @Test
        void rejectsAStandaloneParameterAlongsideAnother() {

            var parsed = spec.parse(new String[] {"target=beta", "reset"}, outputFake);

            assertThat(parsed.isValid())
                .isFalse();
            assertThat(parsed.getResult())
                .isEqualTo(CommandResult.BAD_SYNTAX);

            assertThat(outputFake.getMessages())
                .containsExactly("'reset' cannot be combined with other arguments."
                    + " Usage: sample target=<id>, or sample reset.");
        }

        @Test
        void stillRequiresTheMainFormsParametersWithoutIt() {

            var parsed = spec.parse(new String[0], outputFake);

            assertThat(parsed.isValid())
                .isFalse();
            assertThat(outputFake.getMessages())
                .containsExactly("Missing required parameter 'target'.");
        }
    }

    // A required parameter beside a standalone flag, so the flag given alone is shown to stand
    // in for the whole invocation rather than to skip one check by accident.
    private static final class StandaloneSpec extends ParameterSpec {

        private final Parameter<String> target =
            acceptsNamed("target", "<id>", ParameterValues.text()).markRequired();

        private final Parameter<Boolean> reset = acceptsFlag("reset").markStandalone();

        private StandaloneSpec() {
            super("sample");
        }
    }

    @Nested
    class DescribeUsage {

        @Test
        void bracketsEachOptionalParameterAfterTheCommandAndItsLeadingWord() {

            assertThat(new SampleSpec().describeUsage())
                .isEqualTo(USAGE);
        }

        @Test
        void leavesARequiredParameterUnbracketed() {

            assertThat(new RequiredSpec().describeUsage())
                .isEqualTo("Usage: sample target=<id>.");
        }

        @Test
        void namesAFlagByItsKeyword() {

            assertThat(new FlagSpec().describeUsage())
                .isEqualTo("Usage: sample [<id>] [verbose].");
        }

        @Test
        void offersAStandaloneParameterAsAFormOfItsOwn() {

            assertThat(new StandaloneSpec().describeUsage())
                .isEqualTo("Usage: sample target=<id>, or sample reset.");
        }
    }

    @Nested
    class TakingNoArguments {

        private final ParameterSpec spec =
            ParameterSpec.takingNoArguments("sample");

        @Test
        void acceptsAnEmptyArgumentList() {

            var parsed = spec.parse(new String[0], outputFake);

            assertThat(parsed.isValid())
                .isTrue();
            assertThat(outputFake.getMessages())
                .isEmpty();
        }

        @Test
        void rejectsAnyArgumentWithTheUsageLine() {

            var parsed = spec.parse(new String[] {"stray"}, outputFake);

            assertThat(parsed.isValid())
                .isFalse();
            assertThat(parsed.getResult())
                .isEqualTo(CommandResult.BAD_SYNTAX);

            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Too many arguments")
                    && message.contains("Usage: sample."));
        }

        @Test
        void refusesAUsageLineGivenInPlaceOfTheCommandName() {
            // The argument once took the whole usage line, and a caller still passing one would
            // otherwise print "Usage: Usage: sample.." to the player.
            assertThatThrownBy(() -> ParameterSpec.takingNoArguments("Usage: sample."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A command name is one word, not 'Usage: sample.'");
        }
    }
}
