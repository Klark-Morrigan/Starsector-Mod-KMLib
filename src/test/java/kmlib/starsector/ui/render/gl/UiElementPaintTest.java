package kmlib.starsector.ui.render.gl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the {@link UiElementPaint#isHidden} contract: an element shows nothing when it
 * has no colour or a non-positive alpha, and shows something only when both a colour and
 * a positive alpha are present.
 */
final class UiElementPaintTest {

    @Nested
    class IsHidden {
        @Test
        void reportsHiddenWhenTheColourIsNull() {
            assertThat(new UiElementPaint(null, 1f).isHidden()).isTrue();
        }

        @Test
        void reportsHiddenWhenTheAlphaIsZero() {
            assertThat(new UiElementPaint(Color.RED, 0f).isHidden()).isTrue();
        }

        @Test
        void reportsHiddenWhenTheAlphaIsNegative() {
            assertThat(new UiElementPaint(Color.RED, -0.5f).isHidden()).isTrue();
        }

        @Test
        void reportsVisibleWhenAColourAndPositiveAlphaArePresent() {
            assertThat(new UiElementPaint(Color.RED, 0.25f).isHidden()).isFalse();
        }
    }
}
