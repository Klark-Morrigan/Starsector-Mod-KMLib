package kmlib.starsector.ui.render.gl.panel;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins where the handle's hover range is settled. Two separate pieces of the lighting read that fraction -
 * the wash over the face and the chevron's own shade - and neither confines it, so an overshooting value
 * reaching them would light one further than the other. Confining it on the way in is what keeps them
 * agreeing about where "fully lit" is.
 */
final class NotchStateTest {

    private static final float TOLERANCE = 0.0001f;

    // A collapse fraction part-way through the fold, so a state passing it through unchanged is visible as
    // that value rather than as an end it might have been clamped to.
    private static final float PART_FOLDED = 0.4f;

    @Nested
    class Constructor {

        @Test
        void notchStateKeepsAHoverFractionAlreadyWithinItsRange() {
            assertThat(new NotchState(PART_FOLDED, 0.25f).hoverFraction())
                .isCloseTo(0.25f, within(TOLERANCE));
        }

        @Test
        void notchStateSettlesAnOvershootingHoverFractionAtTheLitEnd() {
            assertThat(new NotchState(PART_FOLDED, 2f).hoverFraction())
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void notchStateSettlesAnUndershootingHoverFractionAtTheRestingEnd() {
            assertThat(new NotchState(PART_FOLDED, -1f).hoverFraction())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void notchStatePassesTheCollapseFractionThroughUntouched() {
            // The collapse fraction is confined by the one computation that reads it, whose limit is a
            // statement about the glyph rather than about the number - so it must arrive here unaltered.
            assertThat(new NotchState(PART_FOLDED, 0f).collapseFraction())
                .isCloseTo(PART_FOLDED, within(TOLERANCE));
        }
    }
}
