package kmlib.starsector.ui.highlight;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HighlightTest {

    @Nested
    class Constructor {
        @Test
        void exposesTextAndColor() {
            var highlight = new Highlight("the Hegemony's", Color.RED);

            assertThat(highlight.getText()).isEqualTo("the Hegemony's");
            assertThat(highlight.getColor()).isEqualTo(Color.RED);
        }

        @Test
        void rejectsNullText() {
            assertThatThrownBy(() -> new Highlight(null, Color.WHITE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("text");
        }

        @Test
        void rejectsNullColor() {
            assertThatThrownBy(() -> new Highlight("token", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("color");
        }
    }

    @Nested
    class Of {
        @Test
        void staticFactoryBuildsTheSameInstance() {
            var built = Highlight.of("token", Color.WHITE);

            assertThat(built.getText()).isEqualTo("token");
            assertThat(built.getColor()).isEqualTo(Color.WHITE);
        }
    }
}
