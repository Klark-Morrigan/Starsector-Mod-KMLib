package kmlib.starsector.ui.text;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins {@link ImageSpan}'s contracts: an image run always holds a path to load, it squares off whatever
 * line it is set on, and it carries the tint its asset was authored with - stating none where the
 * colours are in the texture's own pixels. All three matter downstream without being checked there - a
 * null path reports as a failure at the texture lookup inside a draw call rather than at whoever built
 * the run, a width that did not follow the line would reserve room the drawn square then failed to
 * fill, and a tint dropped between the asset read and the draw shows as a glyph in a shade nobody
 * chose.
 */
class ImageSpanTest {

    private static final String CREST_SPRITE_PATH = "graphics/factions/crest_hegemony.png";

    private static final String ICON_SPRITE_PATH = "graphics/warroom/icon_planet.png";

    private static final Color ICON_TINT = new Color(120, 190, 255);

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

        @Test
        void constructorBuildsAWordOfItsOwn() {
            // An image is set among the words rather than inside one, so it always stands the sentence's
            // own space clear of them - a mark butted against the name it prefixes would read as part of
            // the glyphs.
            assertThat(new ImageSpan(CREST_SPRITE_PATH).isJoinedToPreviousRun())
                .isFalse();
        }

        @Test
        void constructorStatesNoTintForAnImageDrawnAsAuthored() {
            // A crest's colours are in its own pixels, so the path alone builds the run and the draw is
            // left to multiply by nothing.
            assertThat(new ImageSpan(CREST_SPRITE_PATH).tintColour())
                .isNull();
        }

        @Test
        void constructorKeepsTheTintItWasGiven() {
            // A map glyph's colour is authored beside its path, so the run carries it to the draw rather
            // than the draw picking a shade of its own.
            assertThat(new ImageSpan(ICON_SPRITE_PATH, ICON_TINT).tintColour())
                .isEqualTo(ICON_TINT);
        }
    }

    @Nested
    class Equals {

        @Test
        void equalsIsTrueForTheSamePathAndTint() {

            assertThat(new ImageSpan(ICON_SPRITE_PATH, ICON_TINT))
                .isEqualTo(new ImageSpan(ICON_SPRITE_PATH, ICON_TINT));
        }

        @Test
        void equalsIsFalseForTheSamePathInAnotherTint() {
            // The tint is half of what an image run states, so two runs of one shared glyph in two
            // colours are two different marks rather than one repeated.
            assertThat(new ImageSpan(ICON_SPRITE_PATH, ICON_TINT))
                .isNotEqualTo(new ImageSpan(ICON_SPRITE_PATH, new Color(255, 90, 60)));
        }

        @Test
        void equalsIsFalseForATintedSpanAgainstAnUntintedOne() {

            assertThat(new ImageSpan(ICON_SPRITE_PATH, ICON_TINT))
                .isNotEqualTo(new ImageSpan(ICON_SPRITE_PATH));
        }

        @Test
        void equalsIsTrueForTheTwoSpellingsOfAnUntintedImage() {
            // Untinted has one value however it was built, so a run assembled from a path alone and one
            // stating no colour are the same mark rather than two that happen to draw alike.
            assertThat(new ImageSpan(CREST_SPRITE_PATH))
                .isEqualTo(new ImageSpan(CREST_SPRITE_PATH, null));
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

        @Test
        void computeWidthIsUnchangedByATint() {
            // A tint is a colour the texture is multiplied by, so it changes what the square shows and
            // never how much room the line reserves for it.
            assertThat(new ImageSpan(ICON_SPRITE_PATH, ICON_TINT).computeWidth(20f, ONE_UNIT_PER_RUN))
                .isEqualTo(20f);
        }
    }

    @Nested
    class HasContent {

        @Test
        void hasContentIsTrueForAnyImageRun() {
            // There is no blank spelling of an image run, so one exists only where a caller had an image
            // to set into the line.
            assertThat(new ImageSpan(CREST_SPRITE_PATH).hasContent())
                .isTrue();
        }
    }

    @Nested
    class PaintRun {

        @Test
        void paintRunHandsItselfToThePaintersImageMethod() {
            // A run that named no kind would compile and draw nothing, leaving a gap on the line the size
            // of the room the measurement charged for it.
            var imageSpan = new ImageSpan(CREST_SPRITE_PATH);
            var labelRunPainterFake = new LabelRunPainterFake();

            imageSpan.paintRun(labelRunPainterFake, 48f);

            assertThat(labelRunPainterFake.getPaintedRun())
                .isSameAs(imageSpan);
            assertThat(labelRunPainterFake.getPaintedRunX())
                .isEqualTo(48f);
        }
    }
}
