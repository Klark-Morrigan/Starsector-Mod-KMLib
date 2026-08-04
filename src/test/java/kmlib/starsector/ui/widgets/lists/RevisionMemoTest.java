package kmlib.starsector.ui.widgets.lists;

import com.fs.starfarer.api.campaign.SectorAPI;

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
 * Pins the picker memo: it walks the caller's supplier once for a given sector, scope, and revision
 * and serves that value until one of them changes, so a per-frame body build does not re-resolve it.
 * Run over a list of the {@link Anomaly} fixture, the shape a picker actually memoises, since the
 * memo names nothing about what it holds. Each test uses its own memo instance, so the first call
 * always recomputes.
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

            var sectorMock = mock(SectorAPI.class);
            var supplierMock = supplierReturning(ANOMALIES);

            var first = memo.resolveValue(sectorMock, SCOPE_ID, 7, supplierMock);
            var second = memo.resolveValue(sectorMock, SCOPE_ID, 7, supplierMock);

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
            var sectorMock = mock(SectorAPI.class);
            var supplierMock = supplierReturning(ANOMALIES);

            memo.resolveValue(sectorMock, SCOPE_ID, 0, supplierMock);
            memo.resolveValue(sectorMock, SCOPE_ID, 1, supplierMock);

            verify(
                supplierMock,
                times(2)).get();
        }

        @Test
        void resolveValueRecomputesForADifferentScope() {
            // Two scopes hold different values, so a switch between them must re-resolve rather than
            // serve the scope the memo happens to hold.
            var sectorMock = mock(SectorAPI.class);
            var supplierMock = supplierReturning(ANOMALIES);

            memo.resolveValue(sectorMock, SCOPE_ID, 0, supplierMock);
            memo.resolveValue(sectorMock, "other", 0, supplierMock);

            verify(
                supplierMock,
                times(2)).get();
        }

        @Test
        void resolveValueMemoisesUnderANullSectorLikeAnyOtherKey() {
            // A consumer reading before a save is loaded passes null, so null is a key rather than a
            // rejected argument. The case is worth its own test because the first call's recompute
            // rests entirely on the scope compare: the memo starts holding an empty reference, so
            // null == the held sector from the outset and that half of the key never forces it.
            var supplierMock = supplierReturning(ANOMALIES);

            var first = memo.resolveValue(null, SCOPE_ID, 0, supplierMock);
            var second = memo.resolveValue(null, SCOPE_ID, 0, supplierMock);

            assertThat(first)
                .isEqualTo(ANOMALIES);
            assertThat(second)
                .isEqualTo(ANOMALIES);

            verify(
                supplierMock,
                times(1)).get();
        }

        @Test
        void resolveValueRecomputesWhenASectorLoadsUnderANullSectorMemo() {
            // The other half of the same case: a value resolved before the save loaded must not
            // outlive the load, or the first frame of a fresh sector serves what was memoised
            // against no sector at all.
            var sectorMock = mock(SectorAPI.class);
            var supplierMock = supplierReturning(ANOMALIES);

            memo.resolveValue(null, SCOPE_ID, 0, supplierMock);
            memo.resolveValue(sectorMock, SCOPE_ID, 0, supplierMock);

            verify(
                supplierMock,
                times(2)).get();
        }

        @Test
        void resolveValueRecomputesForADifferentSector() {
            // A save reloaded in the same session is a fresh sector under the same revision, so the
            // memo must recompute against it rather than serve the previous save's value.
            var firstSectorMock = mock(SectorAPI.class);
            var secondSectorMock = mock(SectorAPI.class);
            var supplierMock = supplierReturning(ANOMALIES);

            memo.resolveValue(firstSectorMock, SCOPE_ID, 0, supplierMock);
            memo.resolveValue(secondSectorMock, SCOPE_ID, 0, supplierMock);

            verify(
                supplierMock,
                times(2)).get();
        }
    }
}
