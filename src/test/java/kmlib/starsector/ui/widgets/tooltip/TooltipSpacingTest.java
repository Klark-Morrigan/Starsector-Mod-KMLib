package kmlib.starsector.ui.widgets.tooltip;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the room a box spends: the baseline it gets by asking for nothing, and that each refinement
 * reaches one of the three measurements and leaves the other two where they were. The three are the
 * same unit and sit side by side, so a refinement writing its value into a neighbour is a box laid out
 * wrongly rather than a compile error - which is what these cases stand in for.
 */
class TooltipSpacingTest {

    // The baseline, restated here rather than read off the class under test, so a change to how far
    // apart anything stands has to be made deliberately in both places. It is today's spacing exactly:
    // the 4 line gap, that gap plus half a 15pt line between blocks, and half of that inside one.
    private static final float DEFAULT_LINE_GAP = 4f;
    private static final float DEFAULT_SECTION_BREAK = 11.5f;
    private static final float DEFAULT_GROUP_BREAK = 5.75f;

    // What a box asks for instead. Each differs from its own baseline, so a refinement that dropped its
    // argument could not pass, and all three differ from each other, so one writing into a neighbour
    // could not either.
    private static final float WIDER_SECTION_BREAK = 24f;
    private static final float WIDER_GROUP_BREAK = 9f;
    private static final float TIGHTER_LINE_GAP = 1f;
    private static final float TOLERANCE = 0.001f;

    // How far under the box's own voice the line above a gap stands.
    private static final int IN_THE_BOXS_VOICE = 0;
    private static final int TWO_STEPS_UNDER = 2;

    // The gaps a box states when it holds one run of its lines tighter than the rest, which is the only
    // state in which the tier of the line above a gap changes anything. Held as a constant so what the
    // refinement is handed is also what it is checked against.
    private static final TooltipLineGaps TIGHTENED_GAPS = TooltipLineGaps
        .createGaps(DEFAULT_LINE_GAP)
        .gappedAtLevel(TWO_STEPS_UNDER, TIGHTER_LINE_GAP);

    private static TooltipSpacing buildStandardSpacing() {
        return TooltipSpacing.createSpacing();
    }

    private static TooltipSpacing buildTightenedSpacing() {
        return buildStandardSpacing().stackedAt(TIGHTENED_GAPS);
    }

    @Nested
    class CreateSpacing {

        @Test
        void createSpacingTakesTheStandardRoomBetweenLinesBlocksAndGroups() {

            var spacing = buildStandardSpacing();

            assertThat(spacing.resolveLineGapAfter(IN_THE_BOXS_VOICE))
                .isCloseTo(DEFAULT_LINE_GAP, within(TOLERANCE));
            assertThat(spacing.sectionBreak())
                .isCloseTo(DEFAULT_SECTION_BREAK, within(TOLERANCE));
            assertThat(spacing.groupBreak())
                .isCloseTo(DEFAULT_GROUP_BREAK, within(TOLERANCE));
        }

        @Test
        void createSpacingStacksEveryTierOfLinesAtTheSameGap() {
            // A box that never names a tier is spaced as one did before there was anything to name, so a
            // listing that goes deep does not quietly tighten with depth.
            assertThat(buildStandardSpacing().resolveLineGapAfter(TWO_STEPS_UNDER))
                .isCloseTo(DEFAULT_LINE_GAP, within(TOLERANCE));
        }

        @Test
        void createSpacingLeavesABlockPartingWiderThanAGroupParting() {
            // The two are held apart so a run inside a block never reads as a block of its own, which
            // only holds while the baseline keeps them in that order.
            assertThat(buildStandardSpacing().groupBreak())
                .isLessThan(buildStandardSpacing().sectionBreak());
        }

        @Test
        void createSpacingPartsBlocksWiderThanItStacksLines() {
            // The parting is what tells a reader one block ended, so it has to stand clear of the gap
            // two lines of the same block already sit at.
            assertThat(buildStandardSpacing().sectionBreak())
                .isGreaterThan(buildStandardSpacing().resolveLineGapAfter(IN_THE_BOXS_VOICE));
        }
    }

    @Nested
    class StackedAt {

        @Test
        void stackedAtSetsHowFarApartTheLinesOfABlockStand() {
            assertThat(buildTightenedSpacing().lineGaps())
                .isEqualTo(TIGHTENED_GAPS);
        }

        @Test
        void stackedAtChangesNothingElse() {
            assertThat(buildTightenedSpacing())
                .usingRecursiveComparison()
                .ignoringFields("lineGaps")
                .isEqualTo(buildStandardSpacing());
        }
    }

    @Nested
    class PartedBy {

        @Test
        void partedBySetsHowFarApartTheBlocksStand() {
            assertThat(buildStandardSpacing().partedBy(WIDER_SECTION_BREAK).sectionBreak())
                .isCloseTo(WIDER_SECTION_BREAK, within(TOLERANCE));
        }

        @Test
        void partedByChangesNothingElse() {
            // The block parting and the group parting are bare floats of the same unit standing side by
            // side, so a refinement writing into the wrong one compiles - this is what would catch it.
            assertThat(buildStandardSpacing().partedBy(WIDER_SECTION_BREAK))
                .usingRecursiveComparison()
                .ignoringFields("sectionBreak")
                .isEqualTo(buildStandardSpacing());
        }
    }

    @Nested
    class GroupedBy {

        @Test
        void groupedBySetsHowFarApartTheNestedBlocksStand() {
            assertThat(buildStandardSpacing().groupedBy(WIDER_GROUP_BREAK).groupBreak())
                .isCloseTo(WIDER_GROUP_BREAK, within(TOLERANCE));
        }

        @Test
        void groupedByChangesNothingElse() {
            assertThat(buildStandardSpacing().groupedBy(WIDER_GROUP_BREAK))
                .usingRecursiveComparison()
                .ignoringFields("groupBreak")
                .isEqualTo(buildStandardSpacing());
        }
    }

    @Nested
    class ResolveLineGapAfter {

        @Test
        void resolveLineGapAfterAnswersFromTheTierOfTheLineAboveTheGap() {
            // The one lookup a surface makes per line. Which tier it reads is the whole of the rule, so
            // a tightened run must not reach the lines standing above it.
            var spacing = buildTightenedSpacing();

            assertThat(spacing.resolveLineGapAfter(TWO_STEPS_UNDER))
                .isCloseTo(TIGHTER_LINE_GAP, within(TOLERANCE));
            assertThat(spacing.resolveLineGapAfter(IN_THE_BOXS_VOICE))
                .isCloseTo(DEFAULT_LINE_GAP, within(TOLERANCE));
        }
    }
}
