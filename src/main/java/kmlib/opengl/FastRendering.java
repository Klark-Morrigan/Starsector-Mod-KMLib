package kmlib.opengl;

import org.lwjgl.opengl.GL11;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * What KM code has to know about the Fast Rendering mod: whether it is in force, which release first
 * serves the one GL read KM needs from it, and which release is installed. It replaces the game's GL
 * calls with a batching renderer, which changes what some GL calls mean, so code that reads GL state
 * back has to account for it.
 *
 * <p>These facts live here so one file has to be checked against {@code docs/dev/rendering-environment.md},
 * which records them and their citations. None names a Fast Rendering type: this class is about the
 * renderer, not bound to it, so it stays loadable and verifiable on a stock install.
 */
public final class FastRendering {

    /**
     * The identity a Fast Rendering read that failed is recorded under.
     *
     * <p>Published rather than spelled at each recording site because a compatibility record is
     * latched per third party and consumer: two sites spelling it differently would turn one
     * renderer into two subjects and put two modals in front of a player over one mismatch. Held
     * beside the detection because whoever reads through this renderer already asks this class
     * whether it is in force.
     */
    public static final String COMPATIBILITY_SUBJECT_KEY = "fast-rendering";

    /**
     * The renderer's name as a report shows it, beside the key its records latch under.
     *
     * <p>Never qualified as a mod: Fast Rendering is an install patch with no folder under
     * {@code mods\} and no {@code mod_info.json}, so calling it one sends a player to a mod manager
     * that does not list it.
     */
    public static final String COMPATIBILITY_SUBJECT_NAME = "Fast Rendering";

    /**
     * The first release whose bridge answers {@code glGetFloat(GL_MODELVIEW_MATRIX, FloatBuffer)}.
     *
     * <p>Earlier releases refuse that read mid-render, so this is the release a player on one of them
     * is told to update to. It stands in a report where a build-time binding's version would: the
     * read is the only thing KM takes from the renderer, so the release that serves it is the one KM
     * targets.
     */
    public static final String FIRST_MODELVIEW_READ_RELEASE = "v0.9.1rc1";

    // The package every Fast Rendering bridge class sits under, whatever the release calls the rest
    // of the name. Matching the prefix rather than a whole class name is deliberate: the bridge has
    // moved within this package twice, neither time announced (v0.7.4 moved GL11 from
    // com.genir.renderer.bridge to com.genir.renderer.bridge.commands, and v0.8.9 left the
    // implementations there but pointed the rewrite at a com.genir.renderer.bridge.opengl facade).
    // A full-name comparison answers "stock" for any release whose layout it does not know. That is
    // the one wrong answer with teeth - it routes callers into GL reads the bridge cannot serve,
    // which fail mid-render.
    private static final String BRIDGE_PACKAGE_PREFIX = "com.genir.renderer.";

    // genir's own version constant, present from v0.8.2. A private class rather than a convention,
    // so it is read by name through a handle: no KM build compiles against fr.jar.
    private static final String VERSION_CLASS_NAME = BRIDGE_PACKAGE_PREFIX + "Version";
    private static final String VERSION_METHOD_NAME = "getVersion";

    // A lookup carries the access of the class that asked for it, and the version method is public,
    // so this class's own access is all reaching it needs.
    private static final MethodHandles.Lookup MEMBER_LOOKUP = MethodHandles.lookup();

    private FastRendering() {
    }

    /**
     * Reports whether this jar is running under Fast Rendering.
     *
     * <p>Asks the only question that actually matters - were this jar's GL references redirected? -
     * rather than inferring it from an install layout or a mod list. Fast Rendering rewrites
     * constant-pool class entries in every jar it loads, this one included, so the class literal
     * below reports the bridge under it and plain LWJGL otherwise. That needs no reflection and no
     * {@code Class.forName}, which matters because the game's script classloader denies mod code
     * {@code java.lang.reflect} outright.
     *
     * @return {@code true} when GL calls from this jar reach Fast Rendering's bridge
     */
    public static boolean isFastRenderingActive() {
        return isBridgeClassName(GL11.class.getName());
    }

    /**
     * Reads the release the installed {@code fr.jar} reports itself as.
     *
     * <p>For a report rather than a decision. A self-report is a lower bound on the release rather
     * than the release - {@code v0.8.5rc1} says {@code v0.8.4} - so nothing branches on it; the read
     * a caller needs is attempted and its failure is what decides.
     *
     * <p>Never throws: it is called while describing a failure, and a version read that threw there
     * would replace the report with its own trace. An unknown version is the lesser loss.
     *
     * @return the release string, such as {@code "v0.9.1rc1"}, or {@code null} where no Fast Rendering
     *         is installed, or the jar predates v0.8.2 and carries no version class
     */
    public static String readInstalledVersion() {

        return readInstalledVersion(FastRendering::loadThroughOwnLoader);
    }

    // Split from the class-literal read so the rule can be stated against names from releases this
    // machine does not have installed, which is the only way the tolerance for a relocated bridge
    // is checkable at all - the live read reports whichever renderer happens to be underneath.
    static boolean isBridgeClassName(String glClassName) {
        return glClassName.startsWith(BRIDGE_PACKAGE_PREFIX);
    }

    // Split from the live read so a suite can answer the class name with a version class of its own:
    // the live one reads whichever jar the machine has, which is no basis for an expectation.
    //
    // Over everything a handle call can throw, its invoke declaring Throwable, so a version read
    // failing in any way costs the version and nothing else. A fault in the JVM itself is the one
    // thing not worth trading a report for.
    static String readInstalledVersion(ClassLookup classes) {

        try {

            var readVersion = MEMBER_LOOKUP.findStatic(
                classes.loadClass(VERSION_CLASS_NAME),
                VERSION_METHOD_NAME,
                MethodType.methodType(String.class));

            return (String) readVersion.invoke();

        } catch (VirtualMachineError jvmFailure) {
            throw jvmFailure;

        } catch (Throwable readFailure) {
            return null;
        }
    }

    // Without initialising: only a constant is wanted, and running a third party's static initialiser
    // from a failure path is a side effect nothing here asks for.
    private static Class<?> loadThroughOwnLoader(String className) throws ClassNotFoundException {

        return Class.forName(className, false, FastRendering.class.getClassLoader());
    }

    /** What answers a Fast Rendering class name, for the version read that takes one explicitly. */
    @FunctionalInterface
    interface ClassLookup {

        /**
         * @param className the binary name of a Fast Rendering class
         * @return the class
         * @throws ClassNotFoundException where nothing answers to the name
         */
        Class<?> loadClass(String className) throws ClassNotFoundException;
    }
}
