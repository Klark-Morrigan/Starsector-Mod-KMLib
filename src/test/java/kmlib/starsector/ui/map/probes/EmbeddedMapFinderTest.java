package kmlib.starsector.ui.map.probes;

import kmlib.testfixtures.starsector.ui.coreui.CoreUiComponentFake;
import kmlib.testfixtures.starsector.ui.map.probes.SectorMapWidgetFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the rule that says which sector map is somebody else's, and the remembering around it.
 *
 * <p>Both are reachable without a running game because both are decided over a tree rather than
 * inside one: the rule is an identity test against the map the player has open, and the memo turns
 * on which tree an answer came out of. The reach that produces that tree is by-name reflection and
 * only exists in a live game, which is why it arrives here as a supplied root.
 *
 * <p>The rule carries what a hover in game space is allowed to answer. Too loose and the map the
 * player is looking at is treated as an intruder in its own screen; too tight and the surface a mod
 * put on the campaign HUD is invisible to everything that would otherwise stand clear of it.
 *
 * <p>The memo's bound is pinned in both directions because both halves are load-bearing and they
 * pull against each other: without the memory a per-frame caller walks the whole core UI every
 * frame, and without the bound a reading taken while a screen was between states stands for as long
 * as the tree root does - the rest of the session, in the campaign's own core UI.
 */
class EmbeddedMapFinderTest {

    private static final Object NO_MAP_TAB_ON_SCREEN = null;

    // A clock that never moves, for the cases that are not about the bound: the memory cannot expire
    // under them, so what they observe is the walk being reused or repeated for its own reason.
    private static final LongSupplier STOPPED_CLOCK = () -> 0L;

    @Nested
    class CollectEmbeddedMapsUnder {

        @Test
        void collectEmbeddedMapsUnderFindsAMapWithWhatItHangsUnder() {
            // The ancestry is the whole reason the walk answers a value rather than a widget: it is
            // in hand only while the walk is running, and it is what names an owner afterwards.
            // Reading it back outside the walk also shows it was copied rather than borrowed - the
            // walk pops its own chain empty on the way back up.
            var mapFake = new SectorMapWidgetFake();
            var panelFake = new CoreUiComponentFake(mapFake);
            var rootFake = new CoreUiComponentFake(panelFake);

            var embeddedMaps = EmbeddedMapFinder.collectEmbeddedMapsUnder(
                rootFake, NO_MAP_TAB_ON_SCREEN);

            assertThat(embeddedMaps)
                .singleElement()
                .satisfies(embeddedMap -> {
                    assertThat(embeddedMap.widget()).isSameAs(mapFake);
                    assertThat(embeddedMap.ancestors()).containsExactly(rootFake, panelFake);
                });
        }

        @Test
        void collectEmbeddedMapsUnderLeavesOutTheMapTabOnScreen() {
            // The one map that is nobody's intruder. Answering it here would name the screen the
            // player opened as a foreign surface to work around.
            var mapTabFake = new SectorMapWidgetFake();

            assertThat(EmbeddedMapFinder.collectEmbeddedMapsUnder(
                    new CoreUiComponentFake(mapTabFake), mapTabFake))
                .isEmpty();
        }

        @Test
        void collectEmbeddedMapsUnderLeavesOutAMapBelowTheMapTabOnScreen() {
            // Pruned at the tab rather than filtered afterwards, so the tab's own subtree - which is
            // most of the tree, the panned map content hanging below it - goes unwalked.
            var mapTabFake = new SectorMapWidgetFake(
                new CoreUiComponentFake(new SectorMapWidgetFake()));

            assertThat(EmbeddedMapFinder.collectEmbeddedMapsUnder(
                    new CoreUiComponentFake(mapTabFake), mapTabFake))
                .isEmpty();
        }

        @Test
        void collectEmbeddedMapsUnderFindsEveryMapWhileNoMapTabIsOnScreen() {
            // Game space, which is where a minimap on the campaign HUD is the only map drawn. With
            // no tab to exclude, every map in the tree is somebody's embedded one.
            var minimapFake = new SectorMapWidgetFake();
            var secondMapFake = new SectorMapWidgetFake();

            var embeddedMaps = EmbeddedMapFinder.collectEmbeddedMapsUnder(
                new CoreUiComponentFake(minimapFake, secondMapFake), NO_MAP_TAB_ON_SCREEN);

            assertThat(embeddedMaps)
                .extracting(EmbeddedMap::widget)
                .containsExactly(minimapFake, secondMapFake);
        }

        @Test
        void collectEmbeddedMapsUnderFindsNothingInATreeHoldingNoMap() {
            // A vanilla install with no screen open, which is what most of these walks look at.
            assertThat(EmbeddedMapFinder.collectEmbeddedMapsUnder(
                    new CoreUiComponentFake(new CoreUiComponentFake()), NO_MAP_TAB_ON_SCREEN))
                .isEmpty();
        }

        @Test
        void collectEmbeddedMapsUnderStopsDescendingPastTheSearchDepth() {
            // The runaway guard. A pathological tree - or one whose parent and child answer as each
            // other's children - would otherwise walk until the stack gave out, in the middle of a
            // frame.
            Object componentFake = new SectorMapWidgetFake();
            for (var depth = 0; depth <= ProbeLimits.MAX_SEARCH_DEPTH; depth++) {
                componentFake = new CoreUiComponentFake(componentFake);
            }

            assertThat(EmbeddedMapFinder.collectEmbeddedMapsUnder(
                    componentFake, NO_MAP_TAB_ON_SCREEN))
                .isEmpty();
        }
    }

    @Nested
    class FindEmbeddedMaps {

        @Test
        void findEmbeddedMapsAnswersNothingBeforeThereIsATree() {
            // No campaign UI is stood up yet. Nothing to walk is not a failure and says nothing
            // about what a tree will hold once there is one.
            var finder = new EmbeddedMapFinder(
                () -> null, () -> NO_MAP_TAB_ON_SCREEN, STOPPED_CLOCK);

            assertThat(finder.findEmbeddedMaps())
                .isEmpty();
        }

        @Test
        void findEmbeddedMapsWalksOneTreeOnce() {
            // What makes this askable from a render pass. A widget a mod built once stays where it
            // was put, so a second walk of the same tree would pay a full descent for the answer
            // already in hand.
            var rootFake = new CoreUiComponentFake(new SectorMapWidgetFake());
            var finder = new EmbeddedMapFinder(
                () -> rootFake, () -> NO_MAP_TAB_ON_SCREEN, STOPPED_CLOCK);

            finder.findEmbeddedMaps();
            finder.findEmbeddedMaps();

            assertThat(rootFake.countChildrenReads())
                .isEqualTo(1);
        }

        @Test
        void findEmbeddedMapsWalksATreeItHasNotSeen() {
            // The core UI in force changes when an interaction dialog stands up its own, so an
            // answer held past that would describe a tree nobody is being shown.
            var firstMapFake = new SectorMapWidgetFake();
            var secondMapFake = new SectorMapWidgetFake();
            var treeRootFakes = List.<Object>of(
                    new CoreUiComponentFake(firstMapFake),
                    new CoreUiComponentFake(secondMapFake))
                .iterator();
            var finder = new EmbeddedMapFinder(
                treeRootFakes::next, () -> NO_MAP_TAB_ON_SCREEN, STOPPED_CLOCK);

            finder.findEmbeddedMaps();

            assertThat(finder.findEmbeddedMaps())
                .extracting(EmbeddedMap::widget)
                .containsExactly(secondMapFake);
        }

        @Test
        void findEmbeddedMapsWalksAgainWhileNothingIsFound() {
            // Nothing orders a mod's widget building against ours, so an empty first walk can mean
            // "not built yet" rather than "not there". Remembered, it would answer for the session.
            var rootFake = new CoreUiComponentFake();
            var finder = new EmbeddedMapFinder(
                () -> rootFake, () -> NO_MAP_TAB_ON_SCREEN, STOPPED_CLOCK);

            finder.findEmbeddedMaps();
            finder.findEmbeddedMaps();

            assertThat(rootFake.countChildrenReads())
                .isEqualTo(2);
        }

        @Test
        void findEmbeddedMapsWalksTheSameTreeAgainOnceTheMemoryElapses() {
            // The root outlives changes within the tree, so an answer keyed on it alone would stand
            // for as long as the campaign's own core UI does - which is the session. A screen the
            // player closed is still in the tree while it fades and is no longer the map on screen,
            // so a walk taken across those frames counts it as somebody else's map; this is what
            // corrects that reading rather than living with it for the rest of the run.
            var rootFake = new CoreUiComponentFake(new SectorMapWidgetFake());
            var clockFake = new SteppedClockFake();
            var finder = new EmbeddedMapFinder(
                () -> rootFake, () -> NO_MAP_TAB_ON_SCREEN, clockFake::readNanos);

            finder.findEmbeddedMaps();
            clockFake.advanceBy(EmbeddedMapFinder.MEMO_LIFETIME_NANOS);
            finder.findEmbeddedMaps();

            assertThat(rootFake.countChildrenReads())
                .isEqualTo(2);
        }

        @Test
        void findEmbeddedMapsReusesTheWalkWithinTheMemory() {
            // The other half of the same bound, and the reason it is a bound rather than no memo at
            // all: a caller in a render pass asks per frame, and the walk descends the whole core UI
            // by name.
            var rootFake = new CoreUiComponentFake(new SectorMapWidgetFake());
            var clockFake = new SteppedClockFake();
            var finder = new EmbeddedMapFinder(
                () -> rootFake, () -> NO_MAP_TAB_ON_SCREEN, clockFake::readNanos);

            finder.findEmbeddedMaps();
            clockFake.advanceBy(EmbeddedMapFinder.MEMO_LIFETIME_NANOS - 1);
            finder.findEmbeddedMaps();

            assertThat(rootFake.countChildrenReads())
                .isEqualTo(1);
        }

        @Test
        void findEmbeddedMapsAnswersTheScreenAsItStandsAfterTheMemoryElapses() {
            // What the re-walk is for, in the shape the fault took: a map counted as embedded while
            // the screen it belongs to was between states stops being counted once the reads settle,
            // rather than standing as the answer for as long as the root does.
            var mapTabFake = new SectorMapWidgetFake();
            var minimapFake = new SectorMapWidgetFake();
            var rootFake = new CoreUiComponentFake(mapTabFake, minimapFake);
            var clockFake = new SteppedClockFake();
            var shownMapTabFakes = Arrays.asList(NO_MAP_TAB_ON_SCREEN, mapTabFake).iterator();
            var finder = new EmbeddedMapFinder(
                () -> rootFake, shownMapTabFakes::next, clockFake::readNanos);

            finder.findEmbeddedMaps();
            clockFake.advanceBy(EmbeddedMapFinder.MEMO_LIFETIME_NANOS);

            assertThat(finder.findEmbeddedMaps())
                .extracting(EmbeddedMap::widget)
                .containsExactly(minimapFake);
        }

        @Test
        void findEmbeddedMapsAnswersNothingWhenTheTreeCannotBeRead() {
            // The reach is by-name reflection into classes no game build is obliged to keep. A
            // caller is in the middle of a frame, so a broken reach costs the answer and not the
            // frame.
            var finder = new EmbeddedMapFinder(
                () -> {
                    throw new IllegalStateException("A reach that no longer resolves.");
                },
                () -> NO_MAP_TAB_ON_SCREEN,
                STOPPED_CLOCK);

            assertThat(finder.findEmbeddedMaps())
                .isEmpty();
        }
    }

    // An elapsed clock a test moves itself, so the memo's interval is exercised without one. Stepped
    // rather than scripted per reading, the finder reading the clock a differing number of times
    // depending on which branch it takes - which is behaviour a test of the interval must not pin.
    private static final class SteppedClockFake {

        private long nanos;

        private void advanceBy(long elapsedNanos) {
            nanos += elapsedNanos;
        }

        private long readNanos() {
            return nanos;
        }
    }
}
