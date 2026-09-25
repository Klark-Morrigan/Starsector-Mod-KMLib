package kmlib.persistence;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one lookup every keyed option set resolves a stored choice through: a known key finds its
 * option, and nothing stored or a key nothing answers to - left by an older build or another mod -
 * reads as the caller's fallback rather than failing.
 */
final class PersistedChoicesTest {

    @Nested
    class FromKey {

        @Test
        void fromKeyResolvesAKnownKeyToItsOption() {

            assertThat(PersistedChoices.fromKey(Speed.values(), "fast", Speed.SLOW))
                .isEqualTo(Speed.FAST);
        }

        @Test
        void fromKeyFallsBackWhenNothingIsStored() {

            assertThat(PersistedChoices.fromKey(Speed.values(), null, Speed.SLOW))
                .isEqualTo(Speed.SLOW);
        }

        @Test
        void fromKeyFallsBackWhenNoOptionAnswersToTheKey() {
            // A key an older build or another mod wrote names nothing here, so it reads as the
            // choice never having been made.
            assertThat(PersistedChoices.fromKey(Speed.values(), "ludicrous", Speed.SLOW))
                .isEqualTo(Speed.SLOW);
        }

        @Test
        void fromKeyMatchesTheKeyRatherThanTheOptionsName() {
            // The key is the option's own and frozen, and the name is free to change, so a stored
            // string spelling a name must not resolve.
            assertThat(PersistedChoices.fromKey(Speed.values(), "FAST", Speed.SLOW))
                .isEqualTo(Speed.SLOW);
        }

        @Test
        void fromKeyResolvesAmongOptionsACallerAssembled() {
            // The iterable form, for a vocabulary a consumer states rather than an enum declares.
            var options = List.of(Speed.SLOW, Speed.FAST);

            assertThat(PersistedChoices.fromKey(options, "slow", Speed.FAST))
                .isEqualTo(Speed.SLOW);
        }
    }

    // A keyed option set whose keys differ from the constants' names, so a lookup matching the name
    // instead of the key is told apart.
    private enum Speed implements PersistedChoice {

        SLOW("slow"),
        FAST("fast");

        private final String persistenceKey;

        Speed(String persistenceKey) {
            this.persistenceKey = persistenceKey;
        }

        @Override
        public String persistenceKey() {
            return persistenceKey;
        }
    }
}
