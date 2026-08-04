package kmlib.starsector.ui.text;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins {@link ImageSpan}'s two contracts: an image run always holds a path to load, and it squares off
 * whatever line it is set on. Both matter downstream without being checked there - a null path reports
 * as a failure at the texture lookup inside a draw call rather than at whoever built the run, and a
 * width that did not follow the line would reserve room the drawn square then failed to fill.
 */
class ImageSpanTest {

    private static final String CREST_SPRITE_PATH = "graphics/factions/crest_hegemony.png";

    // Measures every run as one unit wide, so a width taken from the glyph measurement rather than from
    // the line would be visible in the assertion below.
    private static final StyledSpanMeasurer ONE_UNIT_PER_RUN = textSpan -> 1d;

    @Nested
    class Constructor {

        @Test
        void constructorKeepsThePathItWasGiven() {
            assertThat(new ImageSpan(CREST_SPRITE_PATH).spritePath())
                .isEqualTo(CREST_SPRITE_PATH);
        }

        @Test
        void constructorRejectsANullPath() {
            assertThatThrownBy(() -> new ImageSpan(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("spritePath");
        }
    }

    @Nested
    class ComputeWidth {

        @Test
        void computeWidthSquaresTheImageOffItsLine() {
            // The square follows the line rather than a fixed step, so a crest set among body text and
            // one set among a heading each come out the size of the words beside them.
            assertThat(new ImageSpan(CREST_SPRITE_PATH).computeWidth(20f, ONE_UNIT_PER_RUN))
                .isEqualTo(20f);
        }

        @Test
        void computeWidthFollowsATallerLine() {
            assertThat(new ImageSpan(CREST_SPRITE_PATH).computeWidth(32f, ONE_UNIT_PER_RUN))
                .isEqualTo(32f);
        }
    }

    @Nested
    class HasContent {

        @Test
        void hasContentIsTrueForAnyImageRun() {
            // There is no blank spelling of an image run, so one exists only where a caller had an image
            // to set into the line.
            assertThat(new ImageSpan(CREST_SPRITE_PATH).hasContent()).isTrue();
        }
    }
}
