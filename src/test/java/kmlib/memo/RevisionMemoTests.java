package kmlib.memo;

import kmlib.testfixtures.starsector.ui.widgets.lists.Anomaly;

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
final class RevisionMemoTests {

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

        // One walk feeds both calls: the second reads the memo rather than re-resolving.
        @Test
        void walksTheSupplierOnceThenServesTheMemoForTheSameInputs() {

            // ARRANGE
            var supplierMock = supplierReturning(ANOMALIES);

            // ACT
            var actualFirst = memo.resolveValue(SCOPE_ID, 7, supplierMock);
            var actualSecond = memo.resolveValue(SCOPE_ID, 7, supplierMock);

            // ASSERT
            assertThat(actualFirst)
                .isEqualTo(ANOMALIES);
            assertThat(actualSecond)
                .isEqualTo(ANOMALIES);

            verify(
                supplierMock,
                times(1)).get();
        }

        // A moved revision is the caller saying something the value depends on changed, so the
        // memo is stale and must re-resolve.
        @Test
        void recomputesWhenTheRevisionMoves() {

            // ARRANGE
            var supplierMock = supplierReturning(ANOMALIES);

            // ACT
            memo.resolveValue(SCOPE_ID, 0, supplierMock);
            memo.resolveValue(SCOPE_ID, 1, supplierMock);

            // ASSERT
            verify(
                supplierMock,
                times(2)).get();
        }

        // Two scopes hold different values, so a switch between them must re-resolve rather than
        // serve the scope the memo happens to hold.
        @Test
        void recomputesForADifferentScope() {

            // ARRANGE
            var supplierMock = supplierReturning(ANOMALIES);

            // ACT
            memo.resolveValue(SCOPE_ID, 0, supplierMock);
            memo.resolveValue("other", 0, supplierMock);

            // ASSERT
            verify(
                supplierMock,
                times(2)).get();
        }
    }

    @Nested
    class DiscardValue {

        // What a released holder's discard has to leave behind: nothing. The scope and the
        // revision are exactly as they were - a holder going away moves neither - so the walk
        // has to be forced by the discard or not at all.
        @Test
        void walksTheSupplierAgainUnderTheKeyItHadServed() {

            // ARRANGE
            var supplierMock = supplierReturning(ANOMALIES);

            memo.resolveValue(SCOPE_ID, 0, supplierMock);

            // ACT
            memo.discardValue();
            memo.resolveValue(SCOPE_ID, 0, supplierMock);

            // ASSERT
            verify(
                supplierMock,
                times(2)).get();
        }

        // The memo stays usable after a discard rather than being spent: a consumer that
        // discards and then resolves reads the new walk's value, not an empty answer.
        @Test
        void servesTheFreshWalkAfterwards() {

            // ARRANGE
            var supplierMock = supplierReturning(ANOMALIES);

            memo.resolveValue(SCOPE_ID, 0, supplierMock);
            memo.discardValue();

            // ACT
            var actualValue = memo.resolveValue(SCOPE_ID, 0, supplierMock);

            // ASSERT
            assertThat(actualValue)
                .isEqualTo(ANOMALIES);
        }
    }
}
