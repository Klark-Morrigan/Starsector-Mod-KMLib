package kmlib.cache.memo;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the stamp's comparison by content: equal only when every revision is, in the same order and
 * of the same length, including where the revisions fold to one int under the {@code Objects.hash}
 * polynomial.
 */
final class RevisionStampTests {

    @Nested
    class Of {

        // A holder keeps the stamp it built under, so a caller reusing its array must not move it.
        @Test
        void keepsTheRevisionsItWasGivenWhenTheArrayIsWrittenAfterwards() {

            // ARRANGE
            var revisions = new int[] {1, 2};
            var expectedText = "[1, 2]";

            // ACT
            var actualStamp = RevisionStamp.of(revisions);

            revisions[0] = 9;

            // ASSERT
            assertThat(actualStamp)
                .hasToString(expectedText);
        }
    }

    @Nested
    class Append {

        // The joined stamp is the head's revisions, then the tail's.
        @Test
        void joinsTheTailAfterTheHead() {

            // ARRANGE
            var head = RevisionStamp.of(1, 2);
            var tail = RevisionStamp.of(3);
            var expectedText = "[1, 2, 3]";

            // ACT
            var actualStamp = head.append(tail);

            // ASSERT
            assertThat(actualStamp)
                .hasToString(expectedText);
        }

        // A source sampling nothing answers the empty stamp, which must leave the holder's own
        // revisions as they were.
        @Test
        void leavesTheHeadEqualToItselfForTheEmptyTail() {

            // ARRANGE
            var head = RevisionStamp.of(1, 2);

            // ACT
            var actualStamp = head.append(RevisionStamp.EMPTY);

            // ASSERT
            assertThat(actualStamp)
                .isEqualTo(head);
        }
    }

    @Nested
    class Equals {

        // Two readings under the same revisions are the same reading.
        @Test
        void isTrueForTheSameRevisionsInTheSameOrder() {

            // ARRANGE
            var stamp = RevisionStamp.of(4, 7);
            var other = RevisionStamp.of(4, 7);

            // ACT
            var isEqual = stamp.equals(other);

            // ASSERT
            assertThat(isEqual)
                .isTrue();
        }

        // The case a fold loses: 31 * (31 + 0) + 31 and 31 * (31 + 1) + 0 are both 992.
        @Test
        void isFalseForRevisionsFoldingToOneHash() {

            // ARRANGE
            var stamp = RevisionStamp.of(0, 31);
            var other = RevisionStamp.of(1, 0);

            // ACT
            var isEqual = stamp.equals(other);

            // ASSERT
            assertThat(isEqual)
                .isFalse();
        }

        // Each position is one source's revision, so two sources swapping values is a change.
        @Test
        void isFalseForTheSameRevisionsInAnotherOrder() {

            // ARRANGE
            var stamp = RevisionStamp.of(1, 2);
            var other = RevisionStamp.of(2, 1);

            // ACT
            var isEqual = stamp.equals(other);

            // ASSERT
            assertThat(isEqual)
                .isFalse();
        }

        // A source added or dropped is a change, even where the extra revision is zero.
        @Test
        void isFalseForAnotherLength() {

            // ARRANGE
            var stamp = RevisionStamp.of(0);
            var other = RevisionStamp.of(0, 0);

            // ACT
            var isEqual = stamp.equals(other);

            // ASSERT
            assertThat(isEqual)
                .isFalse();
        }
    }
}
