package kmlib.starsector.scripts;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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

            // ACT
            var actualScript = ScriptClasses.instantiateScript(BuildableScript.class.getName(), Runnable.class);

            // ASSERT
            assertThat(actualScript)
                .isInstanceOf(BuildableScript.class);
        }

        // Object has no defining loader of its own to resolve the name through.
        @Test
        void resolvesThroughThisLibrarysLoaderForAJdkType() {

            // ACT
            var actualScript = ScriptClasses.instantiateScript(BuildableScript.class.getName(), Object.class);

            // ASSERT
            assertThat(actualScript)
                .isInstanceOf(BuildableScript.class);
        }

        @Test
        void throwsNamingTheClassWhenItIsMissing() {

            // ARRANGE
            var missingName = "kmlib.does.not.Exist";

            // ACT
            var actualFailure = catchThrowable(() -> ScriptClasses.instantiateScript(missingName, Object.class));

            // ASSERT
            assertThat(actualFailure)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(missingName);
        }

        @Test
        void throwsNamingTheClassWhenItIsNotTheRequestedType() {

            // ARRANGE
            var name = BuildableScript.class.getName();

            // ACT
            var actualFailure = catchThrowable(() -> ScriptClasses.instantiateScript(name, Comparable.class));

            // ASSERT
            assertThat(actualFailure)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(name)
                .hasMessageContaining(Comparable.class.getName());
        }

        // The throwing script's constructor would surface as IllegalStateException had it run.
        @Test
        void refusesTheWrongTypeBeforeRunningItsConstructor() {

            // ARRANGE
            var name = ThrowingScript.class.getName();

            // ACT
            var actualFailure = catchThrowable(() -> ScriptClasses.instantiateScript(name, Comparable.class));

            // ASSERT
            assertThat(actualFailure)
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void throwsNamingTheClassWhenItHasNoNoArgConstructor() {

            // ARRANGE
            var name = ArgumentTakingScript.class.getName();

            // ACT
            var actualFailure = catchThrowable(() -> ScriptClasses.instantiateScript(name, Runnable.class));

            // ASSERT
            assertThat(actualFailure)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(name)
                .hasMessageContaining("public no-arg constructor");
        }

        @Test
        void throwsNamingTheClassWhenTheClassIsNotPublic() {

            // ARRANGE
            var name = HiddenScript.class.getName();

            // ACT
            var actualFailure = catchThrowable(() -> ScriptClasses.instantiateScript(name, Runnable.class));

            // ASSERT
            assertThat(actualFailure)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(name);
        }

        @Test
        void wrapsAConstructorFailureNamingTheClass() {

            // ARRANGE
            var name = ThrowingScript.class.getName();

            // ACT
            var actualFailure = catchThrowable(() -> ScriptClasses.instantiateScript(name, Runnable.class));

            // ASSERT
            assertThat(actualFailure)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(name)
                .hasRootCauseMessage(ThrowingScript.FAILURE_MESSAGE);
        }

        @Test
        void throwsNamingTheClassWhenItsStaticInitialiserThrows() {

            // ARRANGE
            var name = InitialiserFailingScript.class.getName();

            // ACT
            var actualFailure = catchThrowable(() -> ScriptClasses.instantiateScript(name, Runnable.class));

            // ASSERT
            assertThat(actualFailure)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(name);
        }

        // A JVM error, which the constructor path otherwise lets through unwrapped.
        @Test
        void wrapsAMissingTypeInTheConstructorNamingTheClass() {

            // ARRANGE
            var name = DependencyMissingScript.class.getName();

            // ACT
            var actualFailure = catchThrowable(() -> ScriptClasses.instantiateScript(name, Runnable.class));

            // ASSERT
            assertThat(actualFailure)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(name)
                .hasCauseInstanceOf(NoClassDefFoundError.class);
        }
    }

    @Nested
    class RequireScriptClass {

        @Test
        void acceptsAPublicClassOfTheTypeWithAPublicNoArgConstructor() {

            // ARRANGE
            var name = BuildableScript.class.getName();

            // ACT
            var actualFailure = catchThrowable(() -> ScriptClasses.requireScriptClass(name, Runnable.class));

            // ASSERT
            assertThat(actualFailure)
                .isNull();
        }

        // The check never builds what it checks, so a constructor that would throw is not run.
        @Test
        void acceptsAClassWithoutRunningItsConstructor() {

            // ARRANGE
            var name = ThrowingScript.class.getName();

            // ACT
            var actualFailure = catchThrowable(() -> ScriptClasses.requireScriptClass(name, Runnable.class));

            // ASSERT
            assertThat(actualFailure)
                .isNull();
        }

        @Test
        void throwsNamingTheClassWhenItIsMissing() {

            // ARRANGE
            var missingName = "kmlib.does.not.Exist";

            // ACT
            var actualFailure = catchThrowable(() -> ScriptClasses.requireScriptClass(missingName, Object.class));

            // ASSERT
            assertThat(actualFailure)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(missingName);
        }

        @Test
        void throwsNamingTheClassWhenItIsNotTheRequestedType() {

            // ARRANGE
            var name = BuildableScript.class.getName();

            // ACT
            var actualFailure = catchThrowable(() -> ScriptClasses.requireScriptClass(name, Comparable.class));

            // ASSERT
            assertThat(actualFailure)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(name)
                .hasMessageContaining(Comparable.class.getName());
        }

        @Test
        void throwsNamingTheClassWhenItHasNoNoArgConstructor() {

            // ARRANGE
            var name = ArgumentTakingScript.class.getName();

            // ACT
            var actualFailure = catchThrowable(() -> ScriptClasses.requireScriptClass(name, Runnable.class));

            // ASSERT
            assertThat(actualFailure)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(name)
                .hasMessageContaining("public no-arg constructor");
        }

        @Test
        void throwsNamingTheClassWhenItsStaticInitialiserThrows() {

            // ARRANGE
            var name = InitialiserFailingScript.class.getName();

            // ACT
            var actualFailure = catchThrowable(() -> ScriptClasses.requireScriptClass(name, Runnable.class));

            // ASSERT
            assertThat(actualFailure)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(name);
        }
    }

    // Public with a public no-arg constructor: the one shape a data-named class has to take.
    public static final class BuildableScript
            implements Runnable {

        @Override
        public void run() {
        }
    }

    // Public, but only buildable with an argument no data file can supply.
    public static final class ArgumentTakingScript
            implements Runnable {

        public ArgumentTakingScript(String required) {
        }

        @Override
        public void run() {
        }
    }

    // A public constructor on a class the public lookup cannot see.
    static final class HiddenScript
            implements Runnable {

        public HiddenScript() {
        }

        @Override
        public void run() {
        }
    }

    // Stands in for a constructor reaching a type no installed mod provides, which the JVM reports on first use.
    public static final class DependencyMissingScript
            implements Runnable {

        public DependencyMissingScript() {
            throw new NoClassDefFoundError("kmlib/does/not/Exist");
        }

        @Override
        public void run() {
        }
    }

    // Fails on its first load; every later load is refused with NoClassDefFoundError, which is wrapped alike.
    public static final class InitialiserFailingScript
            implements Runnable {

        static final Object NEVER_ASSIGNED = failInitialiser();

        private static Object failInitialiser() {
            throw new UnsupportedOperationException("static initialiser failed");
        }

        @Override
        public void run() {
        }
    }

    public static final class ThrowingScript
            implements Runnable {

        static final String FAILURE_MESSAGE = "constructor failed";

        public ThrowingScript() {
            throw new UnsupportedOperationException(FAILURE_MESSAGE);
        }

        @Override
        public void run() {
        }
    }
}
