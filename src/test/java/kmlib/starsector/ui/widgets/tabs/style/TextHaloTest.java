package kmlib.starsector.ui.widgets.tabs.style;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link TextHalo}: where the ring's copies land, and that no ring is a different answer from a ring
 * of zero radius. The placement is what a renderer hands its whole run group over to, so a set of boxes
 * shifted wrongly rings the text off-centre or leaves a side of it bare; and the two ways of saying "no
 * halo" part on cost rather than on pixels - a zero radius still lays every run down four more times.
 */
final class TextHaloTest {

    // A text box's drawn bounds: standing numbers with nothing round about them, so a shifted box cannot
    // agree with its source by coincidence.
    private static final Rectangle TEXT_BOX = new Rectangle(100f, 50f, 64f, 17f);

    private static final Color HALO_COLOUR = Color.BLACK;
    private static final float RADIUS = 3f;

    private static final TextHalo HALO_AT_RADIUS = new TextHalo(HALO_COLOUR, true, RADIUS, 0.5f);

    @Nested
    class ComputeHaloBoxes {

        @Test
        void computeHaloBoxesRingsTheTextBoxOnAllFourSidesAtTheStyledRadius() {
            // Four copies, each the text's box moved one radius to its own side. A ring rather than an
            // offset copy is the whole point of the value: a side left out is a side of the glyphs left
            // reading against whatever is behind them.
            assertThat(HALO_AT_RADIUS.computeHaloBoxes(TEXT_BOX))
                .containsExactlyInAnyOrder(
                    new Rectangle(97f, 50f, 64f, 17f),
                    new Rectangle(103f, 50f, 64f, 17f),
                    new Rectangle(100f, 47f, 64f, 17f),
                    new Rectangle(100f, 53f, 64f, 17f));
        }

        @Test
        void computeHaloBoxesKeepsTheTextBoxsOwnExtentInEveryCopy() {
            // Each copy is the same text at the same size, so it must centre in a box of the same shape:
            // a copy's box that grew or shrank would slide it by half the difference as well.
            assertThat(HALO_AT_RADIUS.computeHaloBoxes(TEXT_BOX))
                .allSatisfy(haloBox -> {
                    assertThat(haloBox.width())
                        .isEqualTo(TEXT_BOX.width());
                    assertThat(haloBox.height())
                        .isEqualTo(TEXT_BOX.height());
                });
        }

        @Test
        void computeHaloBoxesLaysDownNoCopyAtAllForAnUndrawnHalo() {
            // The claim that matters is that nothing is drawn, not that the radius is zero: a ring at zero
            // would land invisibly under its own text and still cost four passes over every run, so an
            // empty answer is what lets a renderer walk this list without testing the flag itself.
            assertThat(TextHalo.NONE.computeHaloBoxes(TEXT_BOX))
                .isEmpty();
        }
    }

    @Nested
    class None {

        @Test
        void noneDrawsNoRingAtAll() {
            assertThat(TextHalo.NONE.isHaloDrawn())
                .isFalse();
        }
    }

    @Nested
    class CreateBlackHairline {

        @Test
        void createBlackHairlineRingsTheTextInSolidBlack() {
            // A black ring is what gives a hard-edged pixel face an edge to sit against live content, and it
            // is solid because the face is: single-pixel strokes backed by a partial shade read as a grey
            // smudge where the vanilla text beside them reads as strokes on black.
            var halo = TextHalo.createBlackHairline();

            assertThat(halo.isHaloDrawn())
                .isTrue();
            assertThat(halo.colour())
                .isEqualTo(Color.BLACK);
            assertThat(halo.radius())
                .isGreaterThan(0f);
            assertThat(halo.strength())
                .isEqualTo(1f);
        }
    }
}
