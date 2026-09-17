package kmlib.opengl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins that the build actually stamped something into this jar.
 *
 * <p>The value is generated, so nothing in the source tree would fail if the generator stopped
 * producing one - the class would simply carry an empty string, and the mismatch report that reads
 * it would name the third party and then trail off mid-sentence on a path that runs only once
 * something is already broken. This is the assertion that holds on either binding: the patched leg
 * stamps genir's own version, the stub leg stamps the sentinel, and both are text.
 */
class FastRenderingBindingTest {

    @Nested
    class ReadBoundVersion {

        @Test
        void reportsTextWhicheverBindingTheBuildTook() {
            assertThat(FastRenderingBinding.readBoundVersion()).isNotBlank();
        }
    }
}
