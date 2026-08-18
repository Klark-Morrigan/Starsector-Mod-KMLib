package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.ui.SectorMapAPI;

import kmlib.testfixtures.starsector.ui.coreui.CoreUiComponentFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

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
 */
class EmbeddedMapFinderTest {

    private static final Object NO_MAP_TAB_ON_SCREEN = null;

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
            var finder = new EmbeddedMapFinder(() -> null, () -> NO_MAP_TAB_ON_SCREEN);

            assertThat(finder.findEmbeddedMaps())
                .isEmpty();
        }

        @Test
        void findEmbeddedMapsWalksOneTreeOnce() {
            // What makes this askable from a render pass. A widget a mod built once stays where it
            // was put, so a second walk of the same tree would pay a full descent for the answer
            // already in hand.
            var rootFake = new WalkCountingComponentFake(new SectorMapWidgetFake());
            var finder = new EmbeddedMapFinder(() -> rootFake, () -> NO_MAP_TAB_ON_SCREEN);

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
            var finder = new EmbeddedMapFinder(treeRootFakes::next, () -> NO_MAP_TAB_ON_SCREEN);

            finder.findEmbeddedMaps();

            assertThat(finder.findEmbeddedMaps())
                .extracting(EmbeddedMap::widget)
                .containsExactly(secondMapFake);
        }

        @Test
        void findEmbeddedMapsWalksAgainWhileNothingIsFound() {
            // Nothing orders a mod's widget building against ours, so an empty first walk can mean
            // "not built yet" rather than "not there". Remembered, it would answer for the session.
            var rootFake = new WalkCountingComponentFake();
            var finder = new EmbeddedMapFinder(() -> rootFake, () -> NO_MAP_TAB_ON_SCREEN);

            finder.findEmbeddedMaps();
            finder.findEmbeddedMaps();

            assertThat(rootFake.countChildrenReads())
                .isEqualTo(2);
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
                () -> NO_MAP_TAB_ON_SCREEN);

            assertThat(finder.findEmbeddedMaps())
                .isEmpty();
        }
    }

    // A sector map that is also a parent in the tree, which is what an embedded map is: a widget
    // composed into somebody's panel with content of its own below it. Public because a by-name
    // invoke resolves a public method and then calls it, which the nested type's own visibility
    // affects and the enclosing class's does not.
    //
    // Local rather than a shipped fixture because nothing outside this test builds a tree with a map
    // in it - the rule this fixture exercises answers a value, and a consuming mod's tests stand up
    // that value rather than the tree it was read from.
    public static final class SectorMapWidgetFake implements SectorMapAPI {
        private final List<Object> children;

        public SectorMapWidgetFake(Object... children) {
            this.children = List.of(children);
        }

        public List<Object> getChildrenCopy() {
            return children;
        }

        // The published half of a map is two entity lookups a tree walk never takes. They throw
        // rather than answering null, so a walk that strayed into one fails here instead of passing
        // on a fixture that cannot stand for what it asked.
        @Override
        public SectorEntityToken getConstellationLabelEntity(Constellation constellation) {
            throw new UnsupportedOperationException("A fixture for tree walks holds no entities.");
        }

        @Override
        public SectorEntityToken getIntelIconEntity(IntelInfoPlugin intel) {
            throw new UnsupportedOperationException("A fixture for tree walks holds no entities.");
        }
    }

    // A parent that counts how often its children were asked for, which is how a walk is observed
    // from outside: a memo that answered without walking never reaches the root's children.
    public static final class WalkCountingComponentFake {
        private final List<Object> children;

        private int childrenReadCount;

        public WalkCountingComponentFake(Object... children) {
            this.children = List.of(children);
        }

        public int countChildrenReads() {
            return childrenReadCount;
        }

        public List<Object> getChildrenCopy() {
            childrenReadCount++;
            return children;
        }
    }
}
