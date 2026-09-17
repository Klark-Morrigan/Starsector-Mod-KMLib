package kmlib.opengl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what the build stamped into this jar as it reaches a reader: a version, or no version at
 * all, and never the sentinel or a blank.
 *
 * <p>The value is generated, so nothing in the source tree would fail if the generator stopped
 * producing one - the class would simply carry an empty string, which reads as a version that is
 * present and says nothing. These hold on either binding: the patched leg stamps genir's own
 * version, the stub leg stamps the sentinel the reader answers as {@code null}.
 */
final class FastRenderingBindingTest {

    @Nested
    class ReadBoundVersion {

        @Test
        void answersAVersionOrNullWhicheverBindingTheBuildTook() {

            assertThat(FastRenderingBinding.readBoundVersion())
                .satisfiesAnyOf(
                    version -> assertThat(version).isNull(),
                    version -> assertThat(version).isNotBlank());
        }

        @Test
        void neverAnswersTheSentinelItself() {

            // The stub leg is where this bites: the word stamped in place of a version must come
            // out as no version, or a subject would print it as one.
            assertThat(FastRenderingBinding.readBoundVersion())
                .isNotEqualTo("unknown");
        }
    }
}
