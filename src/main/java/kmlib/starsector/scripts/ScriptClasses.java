package kmlib.starsector.scripts;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Builds a class a data file names - a plugin column in a CSV, a script key in JSON - as the type the
 * caller holds it as.
 *
 * <p>The game refuses mod code {@code java.lang.reflect}: its jar class loader throws on any class in that
 * package a mod's bytecode names. The usual {@code getDeclaredConstructor().newInstance()} therefore
 * compiles, passes every suite, and throws on its first call in the game. The constructor is found
 * through a public method-handle lookup instead, which names no reflection type and needs no route around
 * the refusal: a public class with a public no-arg constructor is all it can reach, and all a data-named
 * class needs to be.
 *
 * <p>Vanilla's own route, {@code SettingsAPI.getInstanceOfScript}, is not taken. It hands back an
 * {@code Object} already built, so a class of the wrong type has run its constructor before the caller
 * can refuse it, and it has no way to check a name without building it. It is also reached through
 * {@code Global}, which only answers once the game is running. The cost is that a class compiled from a loose
 * {@code .java} source is out of reach: the game compiles those through a loader beneath the one mod
 * jars share, and this class resolves through that shared loader.
 *
 * <p>Each failure is rethrown naming the class, so a typo in the data reads as a typo in the data rather
 * than as the lookup's own message.
 */
public final class ScriptClasses {

    // What a no-arg constructor looks like to a method-handle lookup: it takes nothing and returns
    // nothing, the instance being the handle's result rather than the constructor's.
    private static final MethodType NO_ARG_CONSTRUCTOR = MethodType.methodType(void.class);

    private ScriptClasses() {
    }

    /**
     * Loads {@code className} and builds one through its public no-arg constructor.
     *
     * <p>The class is loaded through the loader that defined {@code scriptType}, so a script named by
     * one mod's data resolves where that mod's own types do. A type the JDK defines has no such loader
     * and falls back to this library's, which is the one every mod jar shares in the game.
     *
     * <p>The type is checked before anything is built, so a class of the wrong type never runs its
     * constructor.
     *
     * @param className  the fully qualified name the data file gives
     * @param scriptType the type the caller holds the instance as
     * @param <T>        that type
     * @return a new instance of {@code className}
     * @throws IllegalArgumentException when the class is missing or cannot be loaded, is not a
     *                                  {@code scriptType}, or is not a public class with a public no-arg
     *                                  constructor
     * @throws IllegalStateException    when the constructor itself throws, or a type it uses cannot be
     *                                  loaded
     */
    public static <T> T instantiateScript(String className, Class<T> scriptType) {

        var constructor = findBuildableConstructor(className, scriptType);

        return scriptType.cast(constructScript(className, constructor));
    }

    /**
     * Checks that {@code className} could be built as a {@code scriptType}, without building one. A data
     * file is read long before the first call that builds what it names, so checking while the file loads
     * reports a bad name where the data is, rather than on whichever later call first builds it.
     *
     * <p>Loading the class runs its static initialiser, as building one would.
     *
     * @param className  the fully qualified name the data file gives
     * @param scriptType the type the caller will hold an instance as
     * @throws IllegalArgumentException when the class is missing or cannot be loaded, is not a
     *                                  {@code scriptType}, or is not a public class with a public no-arg
     *                                  constructor
     */
    public static void requireScriptClass(String className, Class<?> scriptType) {

        findBuildableConstructor(className, scriptType);
    }

    private static Object constructScript(String className, MethodHandle constructor) {

        try {
            return constructor.invoke();

        } catch (LinkageError linkageError) {

            // Types resolve on first use, so a type the constructor names that no installed mod provides
            // surfaces here rather than at load. It is the class's fault, not the JVM's, so it is named.
            throw new IllegalStateException(
                describeScriptClass(className) + " could not be built: a type it uses could not be loaded.",
                linkageError);

        } catch (Error error) {
            throw error;

        } catch (Throwable exception) {

            throw new IllegalStateException(
                describeScriptClass(className) + " threw while being built.",
                exception);
        }
    }

    private static String describeScriptClass(String className) {

        return "Script class '" + className + "'";
    }

    // The type is checked before the constructor is looked up, so a class of the wrong type is refused
    // for its type rather than for its constructor.
    private static MethodHandle findBuildableConstructor(String className, Class<?> scriptType) {

        var scriptClass = loadScriptClass(className, scriptType);
        if (!scriptType.isAssignableFrom(scriptClass)) {

            throw new IllegalArgumentException(
                describeScriptClass(className) + " is not a " + scriptType.getName() + ".");
        }
        return findNoArgConstructor(scriptClass);
    }

    private static MethodHandle findNoArgConstructor(Class<?> scriptClass) {

        try {
            return MethodHandles
                .publicLookup()
                .findConstructor(scriptClass, NO_ARG_CONSTRUCTOR);

        } catch (NoSuchMethodException | IllegalAccessException exception) {

            throw new IllegalArgumentException(
                describeScriptClass(scriptClass.getName()) + " must be public and have a public no-arg constructor.",
                exception);
        }
    }

    private static Class<?> loadScriptClass(String className, Class<?> scriptType) {

        var definingLoader = scriptType.getClassLoader();
        var loader = definingLoader == null
            ? ScriptClasses.class.getClassLoader()
            : definingLoader;

        try {
            return Class.forName(className, true, loader);

        } catch (ClassNotFoundException classNotFoundException) {

            throw new IllegalArgumentException(
                describeScriptClass(className) + " was not found.",
                classNotFoundException);

        } catch (LinkageError | SecurityException exception) {

            // A class that is there but cannot be loaded is still a fault in the data or the install, so
            // it is named like a missing one: a static initialiser that throws, a type it names that no
            // installed mod provides, or one the game's loader refuses mod code.
            throw new IllegalArgumentException(
                describeScriptClass(className) + " could not be loaded.",
                exception);
        }
    }
}
