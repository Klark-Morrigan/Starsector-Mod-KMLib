package kmlib.profiling;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Pins that a section declaring a loop is one identity like any other registered value, that it
 * reports on the same row a plain open of its name would, and that its steps are values a caller
 * holds rather than names spelled inside the loop - which is what makes marking one an index into
 * a slot rather than a lookup.
 */
final class PhasedSectionTest {

    private static final String PLAN_PHASE_NAME = "plan";
    private static final String TRACE_PHASE_NAME = "trace";

    @Nested
    class RegisterPhasedSection {

        @Test
        void returnsTheSamePhasedSectionForTheSameName() {

            var section = PhasedSection.registerPhasedSection(
                "test.phasedSection.repeated", PLAN_PHASE_NAME);

            assertThat(PhasedSection.registerPhasedSection(
                    "test.phasedSection.repeated", PLAN_PHASE_NAME))
                .isSameAs(section);
        }

        @Test
        void reportsOnTheRowAPlainOpenOfThatNameLandsOn() {
            // One name is one row whichever way it was opened, so a section that grew a loop does
            // not split the history of what it costs across two rows spelled alike.
            var section =
                PhasedSection.registerPhasedSection("test.phasedSection.shared", PLAN_PHASE_NAME);

            assertThat(section.getSection())
                .isSameAs(ProfileSection.registerSection("test.phasedSection.shared"));
        }

        @Test
        void keepsItsPhasesInTheOrderATurnPaysThem() {
            // The order is the report's: a reader follows a turn through its steps by reading
            // along the line, so the declaration order is what the slots are numbered by.
            var section = PhasedSection.registerPhasedSection(
                "test.phasedSection.ordered", PLAN_PHASE_NAME, TRACE_PHASE_NAME);

            assertThat(section.getPhases())
                .extracting(ProfilePhase::getName)
                .containsExactly("plan", "trace");
        }

        @Test
        void refusesAnUnnamedSection() {

            assertThatNullPointerException()
                .isThrownBy(() -> PhasedSection.registerPhasedSection(null, PLAN_PHASE_NAME));
        }

        @Test
        void declaresTheRowOnTheTermsItWasHanded() {
            // The loop's terms are the row's: a threshold stated for the pass reaches the section a
            // plain open of the same name lands on, so the two cannot disagree about it.
            var section = PhasedSection.registerPhasedSection(
                "test.phasedSection.logged",
                SectionTerms.DEFAULT.withCallLogThreshold(CallLogThreshold.LOGGING_EVERY_CALL),
                PLAN_PHASE_NAME);

            assertThat(section.getSection().getCallLogThreshold())
                .isSameAs(CallLogThreshold.LOGGING_EVERY_CALL);
        }
    }

    @Nested
    class ResolvePhase {

        @Test
        void handsBackThePhaseTheSectionDeclaredUnderThatName() {

            var section = PhasedSection.registerPhasedSection(
                "test.phasedSection.resolving", PLAN_PHASE_NAME, TRACE_PHASE_NAME);

            assertThat(section.resolvePhase(TRACE_PHASE_NAME))
                .isSameAs(section.getPhases().get(1));
        }

        @Test
        void numbersEachPhaseByTheSlotItAddsTo() {
            // What makes marking a step affordable inside a per-item loop: the slot is settled
            // when the section is declared, so a turn adds into an array rather than looking a
            // name up.
            var section = PhasedSection.registerPhasedSection(
                "test.phasedSection.slotted", PLAN_PHASE_NAME, TRACE_PHASE_NAME);

            assertThat(section.resolvePhase(TRACE_PHASE_NAME).getSlotIndex())
                .isEqualTo(1);
        }

        @Test
        void keepsEachPhaseAnsweringForTheSectionItWasDeclaredOn() {
            // What lets a scope tell a step of its own loop from one that numbers a slot meaning
            // something else entirely.
            var section = PhasedSection.registerPhasedSection(
                "test.phasedSection.owned", PLAN_PHASE_NAME);

            assertThat(section.resolvePhase(PLAN_PHASE_NAME).getSection())
                .isSameAs(section);
        }

        @Test
        void refusesAPhaseTheSectionNeverDeclared() {
            // A misspelling in the constant beside the section, raised where it is written rather
            // than measured through: a step nothing marks would read as one costing nothing.
            var section = PhasedSection.registerPhasedSection(
                "test.phasedSection.undeclared", PLAN_PHASE_NAME);

            assertThatIllegalArgumentException()
                .isThrownBy(() -> section.resolvePhase(TRACE_PHASE_NAME));
        }
    }
}
