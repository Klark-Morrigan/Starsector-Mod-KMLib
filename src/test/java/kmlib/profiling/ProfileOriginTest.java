package kmlib.profiling;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Pins that an origin is an identity rather than a string: one label resolves to one instance
 * however often it is asked for, which is what lets a root opened every frame find its group by
 * comparing references. Pins the reserved origin beside it, since what a span nobody attributed
 * lands under has to be one fixed thing rather than whatever a caller happens to spell.
 */
final class ProfileOriginTest {

    private static final String ORIGIN_LABEL = "test.profileOrigin.MN-6220 - Marat";

    @Nested
    class RegisterOrigin {

        @Test
        void returnsTheSameOriginForTheSameLabel() {

            var origin = ProfileOrigin.registerOrigin(ORIGIN_LABEL);

            assertThat(ProfileOrigin.registerOrigin(ORIGIN_LABEL))
                .isSameAs(origin);
        }

        @Test
        void returnsDistinctOriginsForDistinctLabels() {

            assertThat(ProfileOrigin.registerOrigin("test.profileOrigin.first"))
                .isNotSameAs(ProfileOrigin.registerOrigin("test.profileOrigin.second"));
        }

        @Test
        void keepsTheLabelItWasRegisteredUnder() {

            assertThat(ProfileOrigin.registerOrigin(ORIGIN_LABEL).getLabel())
                .isEqualTo(ORIGIN_LABEL);
        }

        @Test
        void refusesAnUnlabelledOrigin() {

            assertThatNullPointerException()
                .isThrownBy(() -> ProfileOrigin.registerOrigin(null));
        }

        @Test
        void resolvesTheReservedOriginFromItsOwnLabel() {
            // A caller naming its game "unscoped" would otherwise open a second group spelled the
            // same as the one unattributed spans land in, and a reader could not tell them apart.
            assertThat(ProfileOrigin.registerOrigin(ProfileOrigin.UNSCOPED.getLabel()))
                .isSameAs(ProfileOrigin.UNSCOPED);
        }
    }
}
