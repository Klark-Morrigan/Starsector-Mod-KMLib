package kmlib.starsector.ui.map.probes;

import kmlib.testfixtures.starsector.ui.coreui.CoreUiComponentFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the walk two probes now share, which neither of them can pin for itself: each drove it from a
 * live tree, so what was covered was the read at one node and never the descent between them.
 *
 * <p>Four of these are the descent's own contract - the root answers before its children, an answer
 * deep in the tree is still reached, the earliest branch wins when two answer, and the depth bound
 * stops a tree that would otherwise walk until the stack gave out. The last is the contract a caller
 * builds a diagnostic on: the read is tried on every component reached, not only until one answers,
 * so a read that counts what it saw sees all of it.
 */
class SubtreeSearchTest {

    private static final Object ANSWER = new Object();
    private static final Object OTHER_ANSWER = new Object();
    private static final Object NO_ROOT = null;

    @Nested
    class FindFirstUnder {

        @Test
        void findFirstUnderAnswersAValueOnTheRootItself() {
            // Root-first, so a container that answers is found before whatever it holds. A walk that
            // descended first would answer about the innermost widget of a surface rather than the
            // surface.
            var rootFake = new CoreUiComponentFake();

            assertThat(SubtreeSearch.findFirstUnder(rootFake, readAnswerOn(rootFake)))
                .isSameAs(ANSWER);
        }

        @Test
        void findFirstUnderAnswersAValueDeepInTheSubtree() {
            // The ordinary case for both callers: what they look for is several containers below the
            // root, on a chain whose shape is one build's layout rather than anything to rely on.
            var targetFake = new CoreUiComponentFake();
            var rootFake = new CoreUiComponentFake(new CoreUiComponentFake(targetFake));

            assertThat(SubtreeSearch.findFirstUnder(rootFake, readAnswerOn(targetFake)))
                .isSameAs(ANSWER);
        }

        @Test
        void findFirstUnderAnswersTheEarliestBranchWhenMoreThanOneComponentAnswers() {
            // "First" has to mean something stable, or a caller looking for one widget among several
            // gets a different one as the tree is rebuilt.
            var earlierFake = new CoreUiComponentFake();
            var laterFake = new CoreUiComponentFake();
            var rootFake = new CoreUiComponentFake(earlierFake, laterFake);

            assertThat(SubtreeSearch.<Object>findFirstUnder(
                    rootFake,
                    component -> {
                        if (component == earlierFake) {
                            return ANSWER;
                        }
                        return component == laterFake ? OTHER_ANSWER : null;
                    }))
                .isSameAs(ANSWER);
        }

        @Test
        void findFirstUnderAnswersNothingWhenNoComponentDoes() {
            // The resting state on every screen that is not showing what a caller is looking for.
            assertThat(SubtreeSearch.<Object>findFirstUnder(
                    new CoreUiComponentFake(new CoreUiComponentFake()), component -> null))
                .isNull();
        }

        @Test
        void findFirstUnderAnswersNothingWithNoRoot() {
            assertThat(SubtreeSearch.<Object>findFirstUnder(NO_ROOT, component -> ANSWER))
                .isNull();
        }

        @Test
        void findFirstUnderStopsDescendingPastTheSearchDepth() {
            // The runaway guard, and the reason the bound is the walk's rather than each caller's: a
            // pathological tree - or one whose parent and child answer as each other's children -
            // would otherwise walk until the stack gave out, in the middle of a frame.
            Object targetFake = new CoreUiComponentFake();
            Object componentFake = targetFake;

            for (var depth = 0; depth <= ProbeLimits.MAX_SEARCH_DEPTH; depth++) {
                componentFake = new CoreUiComponentFake(componentFake);
            }

            assertThat(SubtreeSearch.findFirstUnder(componentFake, readAnswerOn(targetFake)))
                .isNull();
        }

        @Test
        void findFirstUnderTriesEveryComponentItReaches() {
            // What lets a caller hang a tally or a record of what it saw on the read itself, rather
            // than the walk having to offer a second callback for it. Three components, three tries.
            var readCount = new AtomicInteger();

            SubtreeSearch.findFirstUnder(
                new CoreUiComponentFake(new CoreUiComponentFake(), new CoreUiComponentFake()),
                component -> {
                    readCount.incrementAndGet();
                    return null;
                });

            assertThat(readCount)
                .hasValue(3);
        }
    }

    // A read that answers for one component and nothing for the rest, which is the shape both
    // callers have: most of a widget tree does not carry what they are looking for.
    private static Function<Object, Object> readAnswerOn(Object targetFake) {
        return component -> component == targetFake ? ANSWER : null;
    }
}
