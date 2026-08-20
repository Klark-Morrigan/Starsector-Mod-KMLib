package kmlib.extensions;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the one thing that makes a decline worth anything: that it cannot be built without a reason.
 *
 * <p>The requirement is the whole point of the type. Every other way of asking for it - a document,
 * a review, a logging convention on the mod's own side - is one an implementation can quietly stop
 * meeting, and the install where that happens is exactly the one nobody can then diagnose. Refused
 * at construction, it fails in the implementation's own code, at the moment the reason is missing.
 */
final class DeclinedWorkTest {

    @Nested
    class Construction {

        @Test
        void carries_the_reason_it_was_given() {

            var declinedWork = new DeclinedWork("the body under it is not a planet");

            assertThat(declinedWork.reason())
                .isEqualTo("the body under it is not a planet");
        }

        @Test
        void is_not_executed_work() {

            assertThat(new DeclinedWork("nothing to do here").wasExecuted())
                .isFalse();
        }

        @Test
        void refuses_a_decline_that_says_nothing() {

            assertThatThrownBy(() -> new DeclinedWork(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void refuses_a_decline_whose_reason_is_only_whitespace() {
            // Blank passes a null check and reads as a reason at every call site that builds one,
            // so the test for text is the one worth having.
            assertThatThrownBy(() -> new DeclinedWork("   "))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
