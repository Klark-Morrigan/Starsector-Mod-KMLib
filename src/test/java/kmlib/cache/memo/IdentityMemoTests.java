package kmlib.cache.memo;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the identity rule: the memo derives once per key instance and serves that value while the
 * same instance comes back, and an equal but different instance derives afresh. Each test uses its
 * own memo, so the first call always derives.
 */
final class IdentityMemoTests {

    private final IdentityMemo<List<String>, String> memo = new IdentityMemo<>();

    // A stubbed derivation, so a test can count how often the memo reached for one. Generic mocks
    // cannot be created without an unchecked cast, so it is made once here rather than suppressed
    // at every call site.
    @SuppressWarnings("unchecked")
    private static Function<List<String>, String> deriveReturning(String value) {

        Function<List<String>, String> deriveMock = mock(Function.class);

        when(deriveMock.apply(any()))
            .thenReturn(value);

        return deriveMock;
    }

    @Nested
    class ResolveValue {

        // The whole point of the memo: a key that comes back as the same instance is one whose
        // contents have not been replaced, so deriving again would only repeat the work.
        @Test
        void derivesOnceThenServesTheHeldValueForTheSameInstance() {

            // ARRANGE
            var key = new ArrayList<>(List.of("hegemony"));
            var deriveMock = deriveReturning("derived");

            // ACT
            var actualFirst = memo.resolveValue(key, deriveMock);
            var actualSecond = memo.resolveValue(key, deriveMock);

            // ASSERT
            assertThat(actualFirst)
                .isEqualTo("derived");
            assertThat(actualSecond)
                .isSameAs(actualFirst);

            verify(
                deriveMock,
                times(1)).apply(key);
        }

        // Identity, not equality. A replaced object can hold the same contents as the one before
        // it, and a memo comparing by equals would walk both every frame to say so.
        @Test
        void derivesAgainForAnEqualButDifferentInstance() {

            // ARRANGE
            var firstKey = new ArrayList<>(List.of("hegemony"));
            var equalKey = new ArrayList<>(List.of("hegemony"));
            var deriveMock = deriveReturning("derived");

            // ACT
            memo.resolveValue(firstKey, deriveMock);
            memo.resolveValue(equalKey, deriveMock);

            // ASSERT
            verify(
                deriveMock,
                times(2)).apply(any());
        }

        // One entry: going back to an earlier key derives again, since the memo holds only the
        // last one asked after.
        @Test
        void derivesAgainForAKeyItHeldBeforeTheLast() {

            // ARRANGE
            var firstKey = new ArrayList<>(List.of("hegemony"));
            var secondKey = new ArrayList<>(List.of("tritachyon"));
            var deriveMock = deriveReturning("derived");

            // ACT
            memo.resolveValue(firstKey, deriveMock);
            memo.resolveValue(secondKey, deriveMock);
            memo.resolveValue(firstKey, deriveMock);

            // ASSERT
            verify(
                deriveMock,
                times(2)).apply(firstKey);
        }

        // An empty memo holds no key, so a null key has to derive rather than match the null the
        // memo starts with.
        @Test
        void derivesForANullKeyOnTheFirstAsk() {

            // ARRANGE
            var deriveMock = deriveReturning("derived");

            // ACT
            var actualValue = memo.resolveValue(null, deriveMock);

            // ASSERT
            assertThat(actualValue)
                .isEqualTo("derived");
        }
    }
}
