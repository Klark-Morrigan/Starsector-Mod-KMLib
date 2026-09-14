package kmlib.profiling;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what one level admits of another: each admits itself and everything coarser than it, and
 * off admits nothing at all - including a section spelled at the same end of the scale, which is
 * what stops "no capture" reading as "the coarsest capture".
 */
final class ProfileLevelTest {

    @Nested
    class CanAdmitLevel {

        @Test
        void admitsASectionRegisteredAtTheSameLevel() {

            assertThat(ProfileLevel.COARSE.canAdmitLevel(ProfileLevel.COARSE))
                .isTrue();
        }

        @Test
        void admitsACoarserSectionThanTheCaptureIsKeeping() {
            // A capture reading per-item work still reports the frame the items ran in, which is
            // what the per-item numbers are read against.
            assertThat(ProfileLevel.FINE.canAdmitLevel(ProfileLevel.COARSE))
                .isTrue();
        }

        @Test
        void refusesAFinerSectionThanTheCaptureIsKeeping() {

            assertThat(ProfileLevel.COARSE.canAdmitLevel(ProfileLevel.FINE))
                .isFalse();
        }

        @Test
        void refusesEverySectionWhileOff() {
            // Off means no capture rather than the coarsest one, so it cannot admit a section by
            // sitting at the bottom of the same ordering.
            assertThat(ProfileLevel.OFF.canAdmitLevel(ProfileLevel.OFF))
                .isFalse();
            assertThat(ProfileLevel.OFF.canAdmitLevel(ProfileLevel.COARSE))
                .isFalse();
        }
    }
}
