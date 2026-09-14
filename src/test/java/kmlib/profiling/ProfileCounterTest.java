package kmlib.profiling;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Pins that a counter is an identity rather than a string: one name resolves to one instance
 * however often it is asked for, which is what lets a scope find the tally to add to by comparing
 * references inside the loops it counts.
 */
final class ProfileCounterTest {

    private static final String COUNTER_NAME = "test.profileCounter.systems";

    @Nested
    class RegisterCounter {

        @Test
        void returnsTheSameCounterForTheSameName() {

            var counter = ProfileCounter.registerCounter(COUNTER_NAME);

            assertThat(ProfileCounter.registerCounter(COUNTER_NAME))
                .isSameAs(counter);
        }

        @Test
        void returnsDistinctCountersForDistinctNames() {

            assertThat(ProfileCounter.registerCounter("test.profileCounter.markets"))
                .isNotSameAs(ProfileCounter.registerCounter("test.profileCounter.walks"));
        }

        @Test
        void keepsTheNameItWasRegisteredUnder() {

            assertThat(ProfileCounter.registerCounter(COUNTER_NAME).getName())
                .isEqualTo(COUNTER_NAME);
        }

        @Test
        void refusesAnUnnamedCounter() {

            assertThatNullPointerException()
                .isThrownBy(() -> ProfileCounter.registerCounter(null));
        }
    }
}
