package kmlib.reflection;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the reflection seam on plain objects: reading a private field by its type (searching the
 * hierarchy, skipping a null one for a non-null one, returning null when none matches) and invoking
 * a no-argument method. The classloader-restriction bypass itself only bites in-engine, so this
 * proves the mechanism resolves and drives the handles correctly.
 */
class ReflectionTest {

    private static class Holder {
        private final CharSequence text;

        private Holder(CharSequence text) {
            this.text = text;
        }

        // Public because the seam's invokeNoArg resolves via getMethod (public only), matching how
        // the game's own getCore/getCurrentTab/getChildrenCopy are reached.
        public String describe() {
            return "held:" + text;
        }
    }

    @Nested
    class ReadFieldOfType {

        @Test
        void readFieldOfTypeReturnsTheFieldAssignableToTheType() throws Throwable {
            // The text field is declared CharSequence; matching by the assignable String finds it.
            assertThat(Reflection.readFieldOfType(new Holder("value"), CharSequence.class))
                    .isEqualTo("value");
        }

        @Test
        void readFieldOfTypeSkipsANullMatchForNoneWhenAllMatchesAreNull() throws Throwable {
            assertThat(Reflection.readFieldOfType(new Holder(null), CharSequence.class)).isNull();
        }

        @Test
        void readFieldOfTypeReturnsNullWhenNoFieldMatches() throws Throwable {
            assertThat(Reflection.readFieldOfType(new Holder("value"), Runnable.class)).isNull();
        }

        @Test
        void readFieldOfTypeReadsAFieldInheritedFromASuperclass() throws Throwable {
            class Sub extends Holder {
                private Sub() {
                    super("inherited");
                }
            }

            assertThat(Reflection.readFieldOfType(new Sub(), CharSequence.class))
                    .isEqualTo("inherited");
        }
    }

    @Nested
    class InvokeNoArg {

        @Test
        void invokeNoArgCallsTheNamedMethodAndReturnsItsResult() throws Throwable {
            assertThat(Reflection.invokeNoArg(new Holder("x"), "describe")).isEqualTo("held:x");
        }
    }
}
