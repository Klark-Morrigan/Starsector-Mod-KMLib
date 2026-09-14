package kmlib.starsector.ui.widgets.tooltip;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the one lookup a box's line spacing answers: a tier held apart on purpose gets its own gap, the
 * tiers nested under it inherit it, and whatever stands above the shallowest statement keeps the base
 * one. Those two fallbacks are what let a box state only the runs it wants tightened - a tier that
 * resolved to zero, or to whichever gap was stated last wherever it sat, would respace lines nobody said
 * anything about.
 */
class TooltipLineGapsTest {

    // The spacing a box holds its lines at, and the tighter ones it holds two runs at. Stated as three
    // distinct values so a lookup returning the wrong one cannot pass by coincidence.
    private static final float BASE_GAP = 4f;
    private static final float FACTOR_GAP = 3f;
    private static final float TIER_GAP = 1f;
    private static final float TOLERANCE = 0.001f;

    // How far under the box's own voice the line above a gap stands. The negative is not a level any row
    // produces - a row floors its own at zero - but the lookup is public and is handed whatever a caller
    // holds.
    private static final int ABOVE_THE_BOXS_VOICE = -1;
    private static final int IN_THE_BOXS_VOICE = 0;
    private static final int ONE_STEP_UNDER = 1;
    private static final int TWO_STEPS_UNDER = 2;
    private static final int THREE_STEPS_UNDER = 3;
    private static final int FOUR_STEPS_UNDER = 4;

    // Deeper than any box states a tier for, and deeper than any listing seen so far - the lookup is
    // walked up from the level it is handed, so a depth well past the stated ones is what says the walk
    // ends at a statement rather than at some bound near it.
    private static final int FAR_DEEPER_THAN_ANY_STATED = 12;

    // The share of its room a compressed box keeps, and where the three gaps above land once it is
    // spent. Stated as literals so the case asserts the arithmetic rather than repeating it.
    private static final float HALF_THE_ROOM = 0.5f;
    private static final float HALVED_BASE_GAP = 2f;
    private static final float HALVED_FACTOR_GAP = 1.5f;
    private static final float HALVED_TIER_GAP = 0.5f;

    private static TooltipLineGaps buildFlatGaps() {
        return TooltipLineGaps.createGaps(BASE_GAP);
    }

    // Two tiers held apart from the rest, which is the state every box that varies its spacing is in:
    // the runs it lists most of are tightened, and everything above them keeps the base gap.
    private static TooltipLineGaps buildTieredGaps() {
        return buildFlatGaps()
            .gappedAtLevel(TWO_STEPS_UNDER, FACTOR_GAP)
            .gappedAtLevel(THREE_STEPS_UNDER, TIER_GAP);
    }

    @Nested
    class CreateGaps {

        @Test
        void createGapsHoldsEveryLineAtTheBaseGap() {

            var gaps = buildFlatGaps();

            assertThat(gaps.baseGap())
                .isCloseTo(BASE_GAP, within(TOLERANCE));
            assertThat(gaps.gapsBySubordinationLevel())
                .isEmpty();
        }

        @Test
        void createGapsLeavesEveryTierResolvingTheSameGap() {
            // The baseline a box gets by saying nothing: a stack reads at one spacing however deep it
            // goes, so a box that lists nothing deep never has to name a tier.
            var gaps = buildFlatGaps();

            assertThat(gaps.resolveGapAfter(IN_THE_BOXS_VOICE))
                .isCloseTo(BASE_GAP, within(TOLERANCE));
            assertThat(gaps.resolveGapAfter(THREE_STEPS_UNDER))
                .isCloseTo(BASE_GAP, within(TOLERANCE));
        }
    }

    @Nested
    class Constructor {

        @Test
        void constructorCopiesTheStatedTiersAwayFromTheCaller() {
            // A caller building its tiers in a map it goes on to reuse must not be able to respace a box
            // already built from it, which is the whole of what the copy is for.
            var statedGaps = new HashMap<Integer, Float>();
            statedGaps.put(TWO_STEPS_UNDER, FACTOR_GAP);

            var gaps = new TooltipLineGaps(BASE_GAP, statedGaps);

            statedGaps.put(TWO_STEPS_UNDER, TIER_GAP);

            assertThat(gaps.resolveGapAfter(TWO_STEPS_UNDER))
                .isCloseTo(FACTOR_GAP, within(TOLERANCE));
        }
    }

    @Nested
    class GappedAtLevel {

        @Test
        void gappedAtLevelHoldsThatTiersLinesAtItsOwnGap() {
            assertThat(buildFlatGaps()
                    .gappedAtLevel(TWO_STEPS_UNDER, FACTOR_GAP)
                    .resolveGapAfter(TWO_STEPS_UNDER))
                .isCloseTo(FACTOR_GAP, within(TOLERANCE));
        }

        @Test
        void gappedAtLevelLeavesEveryOtherTierAsItWas() {
            // Each statement tightens one run and nothing else - a tier stated after another must not
            // carry its gap up or down the stack.
            var gaps = buildTieredGaps();

            assertThat(gaps.baseGap())
                .isCloseTo(BASE_GAP, within(TOLERANCE));
            assertThat(gaps.resolveGapAfter(ONE_STEP_UNDER))
                .isCloseTo(BASE_GAP, within(TOLERANCE));
            assertThat(gaps.resolveGapAfter(TWO_STEPS_UNDER))
                .isCloseTo(FACTOR_GAP, within(TOLERANCE));
        }

        @Test
        void gappedAtLevelLeavesTheGapsItWasBuiltFromUntouched() {
            // The refinements are chained off one another, so a box holding an earlier spacing has to
            // keep answering as it did before the later one was layered on it.
            var flatGaps = buildFlatGaps();
            flatGaps.gappedAtLevel(TWO_STEPS_UNDER, FACTOR_GAP);

            assertThat(flatGaps.resolveGapAfter(TWO_STEPS_UNDER))
                .isCloseTo(BASE_GAP, within(TOLERANCE));
        }

        @Test
        void gappedAtLevelTakesTheLaterStatementOfATierStatedTwice() {
            // Layering a stated gap over a built-in one is naming a tier that was already named on
            // purpose, so the later statement wins rather than the pair being an error.
            assertThat(buildFlatGaps()
                    .gappedAtLevel(TWO_STEPS_UNDER, TIER_GAP)
                    .gappedAtLevel(TWO_STEPS_UNDER, FACTOR_GAP)
                    .resolveGapAfter(TWO_STEPS_UNDER))
                .isCloseTo(FACTOR_GAP, within(TOLERANCE));
        }

        @Test
        void gappedAtLevelReadsATierAboveTheBoxsVoiceAsTheBoxsOwn() {
            // Stated at a level no row can carry, the tier still has to be one the lookup reaches -
            // otherwise the statement lands in an entry nothing ever resolves.
            assertThat(buildFlatGaps()
                    .gappedAtLevel(ABOVE_THE_BOXS_VOICE, FACTOR_GAP)
                    .resolveGapAfter(IN_THE_BOXS_VOICE))
                .isCloseTo(FACTOR_GAP, within(TOLERANCE));
        }
    }

    @Nested
    class TightenedBy {

        @Test
        void tightenedByKeepsThatShareOfEveryGapItHolds() {
            // What a compressed box spends between its lines: the base gap and each stated tier alike
            // come down, or the tiers a box never named would be the only room it kept at full width.
            var tightenedGaps = buildTieredGaps().tightenedBy(HALF_THE_ROOM);

            assertThat(tightenedGaps.resolveGapAfter(IN_THE_BOXS_VOICE))
                .isCloseTo(HALVED_BASE_GAP, within(TOLERANCE));
            assertThat(tightenedGaps.resolveGapAfter(TWO_STEPS_UNDER))
                .isCloseTo(HALVED_FACTOR_GAP, within(TOLERANCE));
            assertThat(tightenedGaps.resolveGapAfter(THREE_STEPS_UNDER))
                .isCloseTo(HALVED_TIER_GAP, within(TOLERANCE));
        }

        @Test
        void tightenedByHoldsTheTiersInTheOrderTheyWereStatedIn() {
            // One share across the tiers rather than a share per tier: the tiers are a shape - a run a
            // box holds tighter than the rest is saying that run belongs together - and a compression
            // that flattened them would undo the statement at exactly the depth it was made.
            var tightenedGaps = buildTieredGaps().tightenedBy(HALF_THE_ROOM);

            assertThat(tightenedGaps.resolveGapAfter(THREE_STEPS_UNDER))
                .isLessThan(tightenedGaps.resolveGapAfter(TWO_STEPS_UNDER));
            assertThat(tightenedGaps.resolveGapAfter(TWO_STEPS_UNDER))
                .isLessThan(tightenedGaps.resolveGapAfter(IN_THE_BOXS_VOICE));
        }

        @Test
        void tightenedByLeavesTheGapsItWasBuiltFromUntouched() {
            // A caller solving for how far it has to compress measures candidate after candidate off the
            // one spacing, so a tightening that reached back into it would compound with every probe.
            var tieredGaps = buildTieredGaps();
            tieredGaps.tightenedBy(HALF_THE_ROOM);

            assertThat(tieredGaps.resolveGapAfter(IN_THE_BOXS_VOICE))
                .isCloseTo(BASE_GAP, within(TOLERANCE));
        }
    }

    @Nested
    class ResolveGapAfter {

        @Test
        void resolveGapAfterReturnsTheGapStatedForThatTier() {
            assertThat(buildTieredGaps().resolveGapAfter(THREE_STEPS_UNDER))
                .isCloseTo(TIER_GAP, within(TOLERANCE));
        }

        @Test
        void resolveGapAfterReturnsTheBaseGapForATierNothingWasStatedFor() {
            // The tiers above a tightened run are the box's own voice and the lines just under it, which
            // nobody names - they have to keep the spacing they had before any run was tightened.
            var gaps = buildTieredGaps();

            assertThat(gaps.resolveGapAfter(IN_THE_BOXS_VOICE))
                .isCloseTo(BASE_GAP, within(TOLERANCE));
            assertThat(gaps.resolveGapAfter(ONE_STEP_UNDER))
                .isCloseTo(BASE_GAP, within(TOLERANCE));
        }

        @Test
        void resolveGapAfterTreatsALevelAboveTheBoxsVoiceAsSpeakingInIt() {
            // A row floors its own level at zero, but the lookup is public and is handed whatever a
            // caller has, so a negative resolves what a line speaking for the box resolves.
            assertThat(buildFlatGaps()
                    .gappedAtLevel(IN_THE_BOXS_VOICE, FACTOR_GAP)
                    .resolveGapAfter(ABOVE_THE_BOXS_VOICE))
                .isCloseTo(FACTOR_GAP, within(TOLERANCE));
        }

        @Test
        void resolveGapAfterCarriesTheDeepestStatedTierDownToEveryTierUnderIt() {
            // A listing is as deep as its subject matter, so a box resolves tiers past the last one it
            // named. Those are part of the deepest named run's account and read with it - given the base
            // gap instead, the innermost lines of a box would be the airiest thing in it, which is the
            // reverse of what tightening the run above them asked for.
            var gaps = buildTieredGaps();

            assertThat(gaps.resolveGapAfter(FOUR_STEPS_UNDER))
                .isCloseTo(TIER_GAP, within(TOLERANCE));
            assertThat(gaps.resolveGapAfter(FAR_DEEPER_THAN_ANY_STATED))
                .isCloseTo(TIER_GAP, within(TOLERANCE));
        }

        @Test
        void resolveGapAfterCarriesAStatedTierOnlyDownwards() {
            // The inheritance runs one way: a tier nested under a statement is part of that run, while
            // the lines a run hangs from are not - so a box tightening something deep never pulls the
            // lines above it together as a side effect.
            var gaps = buildFlatGaps().gappedAtLevel(THREE_STEPS_UNDER, TIER_GAP);

            assertThat(gaps.resolveGapAfter(TWO_STEPS_UNDER))
                .isCloseTo(BASE_GAP, within(TOLERANCE));
            assertThat(gaps.resolveGapAfter(FOUR_STEPS_UNDER))
                .isCloseTo(TIER_GAP, within(TOLERANCE));
        }

        @Test
        void resolveGapAfterTakesTheNearestStatedTierAboveALevelRatherThanTheDeepestOne() {
            // Which statement a level inherits, where a box states more than one: the nearest above it,
            // so each stated tier governs the run it opens and hands over at the next statement rather
            // than at whichever one happens to be deepest.
            var gaps = buildTieredGaps();

            assertThat(gaps.resolveGapAfter(TWO_STEPS_UNDER))
                .isCloseTo(FACTOR_GAP, within(TOLERANCE));
            assertThat(gaps.resolveGapAfter(THREE_STEPS_UNDER))
                .isCloseTo(TIER_GAP, within(TOLERANCE));
        }

        @Test
        void resolveGapAfterAnswersTheSameForGapsBuiltDirectlyFromAMap() {
            // The canonical constructor is public, so tiers can arrive as a map rather than through the
            // refinement - both have to reach the same lookup.
            var gaps = new TooltipLineGaps(BASE_GAP, Map.of(TWO_STEPS_UNDER, FACTOR_GAP));

            assertThat(gaps.resolveGapAfter(TWO_STEPS_UNDER))
                .isCloseTo(FACTOR_GAP, within(TOLERANCE));
            assertThat(gaps.resolveGapAfter(ONE_STEP_UNDER))
                .isCloseTo(BASE_GAP, within(TOLERANCE));
        }
    }
}
