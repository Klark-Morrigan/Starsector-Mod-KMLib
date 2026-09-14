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
        void carriesTheReasonItWasGiven() {

            var declinedWork = new DeclinedWork("the body under it is not a planet");

            assertThat(declinedWork.reason())
                .isEqualTo("the body under it is not a planet");
        }

        @Test
        void isNotExecutedWork() {

            assertThat(new DeclinedWork("nothing to do here").wasExecuted())
                .isFalse();
        }

        @Test
        void refusesADeclineThatSaysNothing() {

            assertThatThrownBy(() -> new DeclinedWork(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void refusesADeclineWhoseReasonIsOnlyWhitespace() {
            // Blank passes a null check and reads as a reason at every call site that builds one,
            // so the test for text is the one worth having.
            assertThatThrownBy(() -> new DeclinedWork("   "))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
