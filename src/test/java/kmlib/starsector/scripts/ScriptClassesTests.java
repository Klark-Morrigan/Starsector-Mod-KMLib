package kmlib.starsector.scripts;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Builds real classes by name, so each case runs the lookup the game runs rather than a stand-in for it.
 * Every failure is checked for the class name in its message, that being what points an author at the row
 * to fix.
 */
final class ScriptClassesTests {

    @Nested
    class InstantiateScript {

        @Test
        void buildsTheNamedClassAsTheRequestedType() {

            var script = ScriptClasses.instantiateScript(BuildableScript.class.getName(), Runnable.class);

            assertThat(script)
                .isInstanceOf(BuildableScript.class);
        }

        @Test
        void resolvesThroughThisLibrarysLoaderForAJdkType() {

            // Object has no defining loader of its own to resolve the name through.
            var script = ScriptClasses.instantiateScript(BuildableScript.class.getName(), Object.class);

            assertThat(script)
                .isInstanceOf(BuildableScript.class);
        }

        @Test
        void throwsNamingTheClassWhenItIsMissing() {

            var missing = "kmlib.does.not.Exist";

            assertThatThrownBy(() -> ScriptClasses.instantiateScript(missing, Object.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(missing);
        }

        @Test
        void throwsNamingTheClassWhenItIsNotTheRequestedType() {

            var name = BuildableScript.class.getName();

            assertThatThrownBy(() -> ScriptClasses.instantiateScript(name, Comparable.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(name)
                .hasMessageContaining(Comparable.class.getName());
        }

        @Test
        void refusesTheWrongTypeBeforeRunningItsConstructor() {

            var name = ThrowingScript.class.getName();

            // The throwing script's constructor would surface as IllegalStateException had it run.
            assertThatThrownBy(() -> ScriptClasses.instantiateScript(name, Comparable.class))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void throwsNamingTheClassWhenItHasNoNoArgConstructor() {

            var name = ArgumentTakingScript.class.getName();

            assertThatThrownBy(() -> ScriptClasses.instantiateScript(name, Runnable.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(name)
                .hasMessageContaining("public no-arg constructor");
        }

        @Test
        void throwsNamingTheClassWhenTheClassIsNotPublic() {

            var name = HiddenScript.class.getName();

            assertThatThrownBy(() -> ScriptClasses.instantiateScript(name, Runnable.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(name);
        }

        @Test
        void wrapsAConstructorFailureNamingTheClass() {

            var name = ThrowingScript.class.getName();

            assertThatThrownBy(() -> ScriptClasses.instantiateScript(name, Runnable.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(name)
                .hasRootCauseMessage(ThrowingScript.FAILURE_MESSAGE);
        }
    }

    // Public with a public no-arg constructor: the one shape a data-named class has to take.
    public static final class BuildableScript implements Runnable {

        @Override
        public void run() {
        }
    }

    // Public, but only buildable with an argument no data file can supply.
    public static final class ArgumentTakingScript implements Runnable {

        public ArgumentTakingScript(String required) {
        }

        @Override
        public void run() {
        }
    }

    // A public constructor on a class the public lookup cannot see.
    static final class HiddenScript implements Runnable {

        public HiddenScript() {
        }

        @Override
        public void run() {
        }
    }

    public static final class ThrowingScript implements Runnable {

        static final String FAILURE_MESSAGE = "constructor failed";

        public ThrowingScript() {

            throw new UnsupportedOperationException(FAILURE_MESSAGE);
        }

        @Override
        public void run() {
        }
    }
}
