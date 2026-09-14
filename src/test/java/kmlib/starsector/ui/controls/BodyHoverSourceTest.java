package kmlib.starsector.ui.controls;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins what a body strip drawn without an animator behind it reports. A resting source is what every
 * consumer of a panel that holds no live state takes, and a curried seam has one way to get it wrong that a
 * plain constant does not: answering nothing at all for a position, which is a null a paint pass walks into
 * rather than a control drawn at rest.
 */
final class BodyHoverSourceTest {

    private static final float TOLERANCE = 0.0001f;

    // A position and a cell well down a strip, so the resting answer is shown to hold for any place rather
    // than only for the first one a walk asks about.
    private static final int LATE_CONTROL_INDEX = 7;
    private static final int LATE_CELL = 3;

    @Nested
    class CreateRestingHoverSource {

        @Test
        void createRestingHoverSourceHoversNoCellOfAnyControl() {

            assertThat(BodyHoverSource.createRestingHoverSource()
                    .resolveControlHoverSourceAt(LATE_CONTROL_INDEX)
                    .resolveHoverFractionAt(LATE_CELL))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void createRestingHoverSourceAnswersEveryControlWithASourceOfItsOwn() {
            // A position answered with nothing is a null a paint pass reads through, so the resting source
            // has to hand one back for a strip position no control stands at as readily as for one that has.
            assertThat(BodyHoverSource.createRestingHoverSource()
                    .resolveControlHoverSourceAt(LATE_CONTROL_INDEX))
                .isNotNull();
        }
    }
}
