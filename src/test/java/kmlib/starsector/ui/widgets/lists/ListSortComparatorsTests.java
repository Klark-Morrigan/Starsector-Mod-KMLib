package kmlib.starsector.ui.widgets.lists;

import kmlib.testfixtures.starsector.ui.widgets.lists.Anomaly;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shape a numeric sort vocabulary gets by declaring its metrics: where the mode's own key
 * sits, how a tie falls down the chain and then to the name and the ID, which key the player's
 * direction reaches, and the direction each kind of key runs in by default. The tail is pinned twice
 * over: as the end of that shape, and on its own, which is how a mode ranking by something other
 * than a metric reaches it.
 *
 * <p>Run over an item type declared outside this package, so the cases say what the assembly does
 * for any caller's own rows.
 */
final class ListSortComparatorsTests {

    // The fixture's two numbers, named once so a case can promote either one and read the same
    // accessor back out of the chain.
    private static final ToIntFunction<Anomaly> SEVERITY = Anomaly::severity;
    private static final ToIntFunction<Anomaly> RADIUS = Anomaly::radius;

    // What a mode that ranks by name names instead of a number. Spelled as the accessor type rather
    // than left a bare null at each call, so those cases read as the by-name ranking they set up.
    private static final ToIntFunction<Anomaly> NO_METRIC = null;

    // Severity first, radius behind it - the order this fixture vocabulary breaks ties down.
    private static final List<ToIntFunction<Anomaly>> METRIC_CHAIN = List.of(SEVERITY, RADIUS);

    // A caller's own order that separates nothing, so a case over the tail reads what the tail did
    // rather than what the order in front of it did.
    private static final Comparator<Anomaly> LEVEL_ORDER = (leftAnomaly, rightAnomaly) -> 0;

    @Nested
    class AssembleComparator {

        @Test
        void ranksByThePrimaryMetricHighToLow() {

            var low = new Anomaly("low", 1, 0);
            var high = new Anomaly("high", 9, 0);

            assertThat(listIdsSortedBy(SEVERITY, low, high))
                .containsExactly("high", "low");
        }

        @Test
        void breaksAPrimaryTieDownTheMetricChain() {

            // Level on severity, so the tie falls to radius next in the chain: the wider one leads
            // even though the key being sorted is severity.
            var narrower = new Anomaly("a", 5, 2);
            var wider = new Anomaly("b", 5, 7);

            assertThat(listIdsSortedBy(SEVERITY, narrower, wider))
                .containsExactly("b", "a");
        }

        @Test
        void promotesThePrimaryMetricAheadOfTheMetricChain() {

            // Under radius, the less severe but wider anomaly leads: the primary key is promoted
            // ahead of severity, which sits first in the chain and would otherwise win.
            var severe = new Anomaly("severe", 9, 1);
            var wide = new Anomaly("wide", 1, 8);

            assertThat(listIdsSortedBy(RADIUS, severe, wide))
                .containsExactly("wide", "severe");
        }

        @Test
        void ranksByNameWhenNoMetricIsNamed() {

            var zeta = new Anomaly("Zeta", 0, 0);
            var alpha = new Anomaly("Alpha", 0, 0);

            assertThat(listIdsSortedBy(NO_METRIC, zeta, alpha))
                .containsExactly("Alpha", "Zeta");
        }

        @Test
        void ranksNamesRegardlessOfCase() {

            // A case-sensitive order would put every capital first, so "Beta" would lead "alpha".
            var beta = new Anomaly("Beta", 0, 0);
            var alpha = new Anomaly("alpha", 0, 0);

            assertThat(listIdsSortedBy(NO_METRIC, beta, alpha))
                .containsExactly("alpha", "Beta");
        }

        @Test
        void breaksANameTieDownTheWholeChain() {

            // Two anomalies share a name, so a by-name ranking falls through to the metric chain and
            // the more severe one leads.
            var milder = new Anomaly("milder", "Same", null, 1, 0);
            var harsher = new Anomaly("harsher", "Same", null, 8, 0);

            assertThat(listIdsSortedBy(NO_METRIC, milder, harsher))
                .containsExactly("harsher", "milder");
        }

        @Test
        void breaksAChainLevelPairByName() {

            // Level on every number, so a numeric ranking falls to the name, which reads A-to-Z behind
            // the chain whichever number led it.
            var zeta = new Anomaly("Zeta", 3, 3);
            var alpha = new Anomaly("Alpha", 3, 3);

            assertThat(listIdsSortedBy(SEVERITY, zeta, alpha))
                .containsExactly("Alpha", "Zeta");
        }

        @Test
        void flipsThePrimaryKeyWhenTheDirectionIsTheOpposite() {

            // Ascending reverses the primary metric, so the less severe anomaly leads while the key is
            // still severity - only its direction changed.
            var low = new Anomaly("low", 1, 0);
            var high = new Anomaly("high", 9, 0);

            assertThat(listIdsSortedBy(SEVERITY, SortDirection.ASCENDING, low, high))
                .containsExactly("low", "high");
        }

        @Test
        void keepsTheMetricChainWhenThePrimaryFlips() {

            // With the primary key ascending, a tie on it still breaks down the chain the same way:
            // level on severity, the wider one leads regardless of direction.
            var narrower = new Anomaly("a", 5, 2);
            var wider = new Anomaly("b", 5, 7);

            assertThat(listIdsSortedBy(SEVERITY, SortDirection.ASCENDING, narrower, wider))
                .containsExactly("b", "a");
        }

        @Test
        void flipsTheNameKeyWhenTheDirectionIsDescending() {

            // A by-name ranking's natural order is ascending, so descending reverses it to Z-to-A.
            var zeta = new Anomaly("Zeta", 0, 0);
            var alpha = new Anomaly("Alpha", 0, 0);

            assertThat(listIdsSortedBy(NO_METRIC, SortDirection.DESCENDING, zeta, alpha))
                .containsExactly("Zeta", "Alpha");
        }

        @Test
        void fallsBackToAStableByIdOrderWhenEveryKeyIsLevel() {

            // Same name and identical numbers, so every visible key is level; the by-ID key gives a
            // total order so the pair holds a fixed position rather than reshuffling frame to frame.
            var second = new Anomaly("bbb", "Same", null, 3, 3);
            var first = new Anomaly("aaa", "Same", null, 3, 3);

            assertThat(listIdsSortedBy(SEVERITY, second, first))
                .containsExactly("aaa", "bbb");
        }

        @Test
        void sortsAnUnlabelledItemWithTheBlanks() {

            // An item no name resolved for sorts as though its label were empty rather than throwing
            // when the ranking reaches the name.
            var named = new Anomaly("named", "Alpha", null, 0, 0);
            var unlabelled = new Anomaly("unlabelled", null, null, 0, 0);

            assertThat(listIdsSortedBy(NO_METRIC, named, unlabelled))
                .containsExactly("unlabelled", "named");
        }
    }

    @Nested
    class AppendNameAndIdTail {

        @Test
        void breaksAPairTheCallersOwnOrderLeftLevelByName() {

            // The caller's order separates nothing, so what arranges the pair is the tail alone: the
            // label, A-to-Z.
            var zeta = new Anomaly("Zeta", 9, 9);
            var alpha = new Anomaly("Alpha", 1, 1);

            assertThat(listIdsWithTailBehind(LEVEL_ORDER, zeta, alpha))
                .containsExactly("Alpha", "Zeta");
        }

        @Test
        void breaksAPairTheNameLeavesLevelById() {

            // Level under the caller's order and sharing a label, so only the by-ID key behind the
            // name separates them - which is what keeps such a pair from reshuffling frame to frame.
            var second = new Anomaly("bbb", "Same", null, 0, 0);
            var first = new Anomaly("aaa", "Same", null, 0, 0);

            assertThat(listIdsWithTailBehind(LEVEL_ORDER, second, first))
                .containsExactly("aaa", "bbb");
        }

        @Test
        void leavesTheCallersOwnOrderLeading() {

            // The caller's order decides the pair, so the tail never reaches the name - the anomaly
            // that would lead A-to-Z comes second.
            var zeta = new Anomaly("Zeta", 9, 0);
            var alpha = new Anomaly("Alpha", 1, 0);

            assertThat(listIdsWithTailBehind(
                    Comparator.comparingInt((Anomaly anomaly) -> -anomaly.severity()),
                    zeta,
                    alpha))
                .containsExactly("Zeta", "Alpha");
        }
    }

    @Nested
    class ResolveDefaultDirection {

        @Test
        void isDescendingForAMetric() {

            // A number leads with the bigger item, so its natural order runs high-to-low.
            assertThat(ListSortComparators.resolveDefaultDirection(SEVERITY))
                .isEqualTo(SortDirection.DESCENDING);
        }

        @Test
        void isAscendingWhenNoMetricIsNamed() {

            // A name reads A-to-Z, so its natural order runs ascending.
            assertThat(ListSortComparators.resolveDefaultDirection(NO_METRIC))
                .isEqualTo(SortDirection.ASCENDING);
        }
    }

    // Assembles the comparator for a mode promoting this metric and reads the items' IDs off it, in
    // the primary key's own natural direction, so an assertion reads the default arrangement without
    // spelling out the direction. The flip cases use the direction overload.
    private static List<String> listIdsSortedBy(ToIntFunction<Anomaly> primaryMetric, Anomaly... anomalies) {

        return listIdsSortedBy(
            primaryMetric,
            ListSortComparators.resolveDefaultDirection(primaryMetric),
            anomalies);
    }

    // The same in a named direction, which is what the flip cases ask for.
    private static List<String> listIdsSortedBy(
            ToIntFunction<Anomaly> primaryMetric,
            SortDirection direction,
            Anomaly... anomalies) {

        return listIdsInOrder(
            ListSortComparators.assembleComparator(primaryMetric, METRIC_CHAIN, direction),
            anomalies);
    }

    // Puts the tail behind a caller's own order and reads the items' IDs off the result, which is how
    // a mode ranking by something other than a metric reaches the tail.
    private static List<String> listIdsWithTailBehind(Comparator<Anomaly> callersOrder, Anomaly... anomalies) {
        return listIdsInOrder(ListSortComparators.appendNameAndIdTail(callersOrder), anomalies);
    }

    // The IDs the anomalies land in under the order, so an assertion reads the arrangement alone.
    private static List<String> listIdsInOrder(Comparator<Anomaly> order, Anomaly... anomalies) {

        var sorted = new ArrayList<>(List.of(anomalies));

        sorted.sort(order);

        var ids = new ArrayList<String>(sorted.size());

        for (var anomaly : sorted) {
            ids.add(anomaly.itemId());
        }

        return ids;
    }
}
