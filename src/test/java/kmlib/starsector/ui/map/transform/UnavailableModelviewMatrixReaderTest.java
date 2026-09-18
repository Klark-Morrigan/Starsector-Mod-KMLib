package kmlib.starsector.ui.map.transform;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one contract the unavailable binding has: it never reports a matrix, first read or any
 * read after, so a caller degraded onto it parks on every frame rather than on the first alone.
 */
final class UnavailableModelviewMatrixReaderTest {

    // More than one, so a reader that answered absent once and then something else could not pass.
    private static final int REPEATED_READ_COUNT = 3;

    @Nested
    class ReadModelviewMatrix {

        @Test
        void reportsNoReadingOnTheFirstAndEveryRepeatedRead() {

            for (var read = 0; read < REPEATED_READ_COUNT; read++) {
                assertThat(UnavailableModelviewMatrixReader.INSTANCE.readModelviewMatrix())
                    .as("read %d", read)
                    .isNull();
            }
        }
    }
}
