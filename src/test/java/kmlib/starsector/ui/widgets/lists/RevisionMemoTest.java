package kmlib.starsector.ui.widgets.lists;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the picker memo: it walks the caller's supplier once for a given scope and revision and
 * serves that value until one of them changes, so a per-frame body build does not re-resolve it, and
 * a discard leaves nothing to serve. Run over a list of the {@link Anomaly} fixture, the shape a
 * picker actually memoises, since the memo names nothing about what it holds. Each test uses its own
 * memo instance, so the first call always recomputes.
 */
final class RevisionMemoTest {

    // The value the stubbed supplier returns; the memo forwards it verbatim, so its contents only
    // stand in.
    private static final List<Anomaly> ANOMALIES = List.of(new Anomaly("Storm", 9, 8));

    // The scope every single-scope test resolves under; a switch between scopes is its own case.
    private static final String SCOPE_ID = "anomalies";

    private final RevisionMemo<List<Anomaly>> memo = new RevisionMemo<>();

    // A stubbed resolve of the value, so a test can count how often the memo actually reached for
    // one. Generic mocks cannot be created without an unchecked cast, so it is made once here rather
    // than suppressed at every call site.
    @SuppressWarnings("unchecked")
    private static Supplier<List<Anomaly>> supplierReturning(List<Anomaly> value) {
        Supplier<List<Anomaly>> supplierMock = mock(Supplier.class);
        when(supplierMock.get()).thenReturn(value);
        return supplierMock;
    }

    @Nested
    class ResolveValue {

        @Test
        void resolveValueWalksTheSupplierOnceThenServesTheMemoForTheSameInputs() {

            var supplierMock = supplierReturning(ANOMALIES);

            var first = memo.resolveValue(SCOPE_ID, 7, supplierMock);
            var second = memo.resolveValue(SCOPE_ID, 7, supplierMock);

            assertThat(first)
                .isEqualTo(ANOMALIES);
            assertThat(second)
                .isEqualTo(ANOMALIES);

            // One walk feeds both calls: the second reads the memo rather than re-resolving.
            verify(
                supplierMock,
                times(1)).get();
        }

        @Test
        void resolveValueRecomputesWhenTheRevisionMoves() {
            // A moved revision is the caller saying something the value depends on changed, so the
            // memo is stale and must re-resolve.
            var supplierMock = supplierReturning(ANOMALIES);

            memo.resolveValue(SCOPE_ID, 0, supplierMock);
            memo.resolveValue(SCOPE_ID, 1, supplierMock);

            verify(
                supplierMock,
                times(2)).get();
        }

        @Test
        void resolveValueRecomputesForADifferentScope() {
            // Two scopes hold different values, so a switch between them must re-resolve rather than
            // serve the scope the memo happens to hold.
            var supplierMock = supplierReturning(ANOMALIES);

            memo.resolveValue(SCOPE_ID, 0, supplierMock);
            memo.resolveValue("other", 0, supplierMock);

            verify(
                supplierMock,
                times(2)).get();
        }
    }

    @Nested
    class DiscardValue {

        @Test
        void discardValueWalksTheSupplierAgainUnderTheKeyItHadServed() {
            // What a released holder's discard has to leave behind: nothing. The scope and the
            // revision are exactly as they were - a holder going away moves neither - so the walk
            // has to be forced by the discard or not at all.
            var supplierMock = supplierReturning(ANOMALIES);

            memo.resolveValue(SCOPE_ID, 0, supplierMock);
            memo.discardValue();
            memo.resolveValue(SCOPE_ID, 0, supplierMock);

            verify(
                supplierMock,
                times(2)).get();
        }

        @Test
        void discardValueServesTheFreshWalkAfterwards() {
            // The memo stays usable after a discard rather than being spent: a consumer that
            // discards and then resolves reads the new walk's value, not an empty answer.
            var supplierMock = supplierReturning(ANOMALIES);

            memo.resolveValue(SCOPE_ID, 0, supplierMock);
            memo.discardValue();

            assertThat(memo.resolveValue(SCOPE_ID, 0, supplierMock))
                .isEqualTo(ANOMALIES);
        }
    }
}
