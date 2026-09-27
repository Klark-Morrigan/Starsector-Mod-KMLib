package kmlib.logging;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what setting a level through {@link KmLogging} does to log4j: a named level reaches the
 * whole subtree through inheritance, whitespace is tolerated, null and unrecognised names fall back
 * to the library default, and loggers outside the subtree are left alone.
 *
 * <p>Uses the real log4j {@link Logger} hierarchy (no mock), since that inheritance is exactly what
 * the helper relies on; each case uses a distinct logger-root name because log4j loggers are
 * process-global.
 */
final class KmLoggingTest {

    @Nested
    class ApplyLevel {

        @Test
        void namedLevelIsInheritedByDescendantLoggers() {

            var descendant = Logger.getLogger("kmlibtest_named.child.grandchild");

            KmLogging.applyLevel("kmlibtest_named", "DEBUG");

            assertThat(descendant.getEffectiveLevel())
                .isEqualTo(Level.DEBUG);
        }

        @Test
        void surroundingWhitespaceOnTheNameIsTolerated() {

            var descendant = Logger.getLogger("kmlibtest_pad.child");

            KmLogging.applyLevel("kmlibtest_pad", "  ERROR  ");

            assertThat(descendant.getEffectiveLevel())
                .isEqualTo(Level.ERROR);
        }

        @Test
        void nullNameFallsBackToTheLibraryDefault() {
            // The root's own level rather than a descendant's effective one, which a root left unset
            // would take from whatever the JVM's root logger happens to stand at.
            var root = Logger.getLogger("kmlibtest_null");

            KmLogging.applyLevel("kmlibtest_null", null);

            assertThat(root.getLevel())
                .isEqualTo(Level.WARN);
        }

        @Test
        void unrecognisedNameFallsBackToTheLibraryDefault() {
            // The root's own level rather than a descendant's effective one, which a root left unset
            // would take from whatever the JVM's root logger happens to stand at.
            var root = Logger.getLogger("kmlibtest_bad");

            KmLogging.applyLevel("kmlibtest_bad", "nonsense");

            assertThat(root.getLevel())
                .isEqualTo(Level.WARN);
        }

        @Test
        void loggersOutsideTheSubtreeAreNotAffected() {

            var sibling = Logger.getLogger("kmlibtest_sibling_outside");
            var siblingBefore = sibling.getEffectiveLevel();

            KmLogging.applyLevel("kmlibtest_subtree", "OFF");

            assertThat(sibling.getEffectiveLevel())
                .isEqualTo(siblingBefore);
        }
    }

    @Nested
    class DefaultLevel {

        @Test
        void libraryDefaultLevelIsWarn() {
            // Pins the shared fallback every binding applies where no level is set, so mods do not
            // restate a default of their own.
            assertThat(KmLogging.DEFAULT_LEVEL)
                .isEqualTo(Level.WARN);
        }
    }
}
