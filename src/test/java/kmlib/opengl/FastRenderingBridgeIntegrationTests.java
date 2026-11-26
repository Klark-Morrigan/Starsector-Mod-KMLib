package kmlib.opengl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Holds the installed Fast Rendering to the things KM takes from it: that its agent rewrites a mod's
 * {@code GL11} onto a class detection recognises, that the class it rewrites to answers
 * {@code glGetFloat(GL_MODELVIEW_MATRIX)} with the matrix the calls before it built, that the same class
 * refuses display lists, which would move that matrix out of the read's sight, and that its version class
 * still reads.
 *
 * <p>Drives the real {@code fr.jar} the build puts on the test runtime, by name through method handles, so
 * nothing here compiles against it. The bridge class is the one the agent's own rewrite table names, so the
 * read is taken where a rewritten mod takes it. The table is built as the agent builds it at launch: through
 * the loader that carries the ASM its analysis of the game's classes needs, over the game jars the build also
 * puts on the test runtime.
 *
 * <p>The bridge answers the read from a copy of the matrix it keeps on the calling thread, so no window or GL
 * context is needed. Setting the bridge up still loads LWJGL's native library, from the install's
 * {@code starsector-core/native/<os>} folder.
 *
 * <p>Fails rather than skipping where the install carries no Fast Rendering or no native for this platform:
 * a skipped check reads as a passed one.
 */
final class FastRenderingBridgeIntegrationTests {

    private static final String STARSECTOR_ROOT_PROPERTY = "kmlib.starsectorRoot";

    // Where LWJGL looks for its native before java.library.path, read when the native is first loaded.
    private static final String LWJGL_LIBRARY_PATH_PROPERTY = "org.lwjgl.librarypath";

    private static final String CONTEXT_MANAGER_CLASS_NAME = "com.genir.renderer.bridge.context.ContextManager";
    private static final String CONTEXT_CLASS_NAME = "com.genir.renderer.bridge.context.Context";
    private static final String EXECUTOR_CLASS_NAME = "com.genir.renderer.bridge.context.executor.Executor";

    // The loader the agent runs its transformations under, and the ASM jars inside fr.jar it carries.
    private static final String EMBEDDED_JAR_CLASS_LOADER_CLASS_NAME = "com.genir.renderer.agent.EmbeddedJarClassLoader";
    private static final String[] AGENT_EMBEDDED_JAR_NAMES = {"asm-9.1.jar", "asm-tree-9.1.jar"};

    // The agent's analysis of the game's classes, its rewrite tables built from it, and the table it applies
    // to a mod's classes.
    private static final String SYMBOLS_CLASS_NAME = "com.genir.renderer.agent.transformations.symbols.Symbols";
    private static final String TRANSFORMATIONS_CLASS_NAME =
        "com.genir.renderer.agent.transformations.symbols.Transformations";
    private static final String MOD_REWRITE_TABLE_NAME = "opengl";
    private static final String LWJGL_GL11_INTERNAL_NAME = "org/lwjgl/opengl/GL11";

    // Uneven, and different on the two axes, so a transposed or swapped read cannot land on a match.
    private static final float PAN_X = 137.01f;
    private static final float PAN_Y = 41.01f;

    private static final MethodHandles.Lookup PUBLIC_LOOKUP = MethodHandles.publicLookup();

    // The rewrite target for GL11, as the agent's table names it.
    private static String bridgeGlClassName;

    // The bridge's GL11 entry points this suite drives, each the shape LWJGL's own method has.
    private static MethodHandle matrixMode;
    private static MethodHandle loadIdentity;
    private static MethodHandle pushMatrix;
    private static MethodHandle popMatrix;
    private static MethodHandle translate;
    private static MethodHandle getFloat;
    private static MethodHandle newList;
    private static MethodHandle callList;

    // The bridge's main context, created once: the bridge holds it in a static, one per process.
    private static Object bridgeContext;

    @BeforeAll
    static void createBridgeContext()
            throws Throwable {

        System.setProperty(
            LWJGL_LIBRARY_PATH_PROPERTY,
            resolveNativesDirectory()
                .toAbsolutePath()
                .toString());

        bridgeGlClassName = readModRewriteTable()
            .get(LWJGL_GL11_INTERNAL_NAME)
            .replace('/', '.');

        var bridgeGl = loadBridgeClass(bridgeGlClassName);

        matrixMode = findVoidStatic(bridgeGl, "glMatrixMode", int.class);
        loadIdentity = findVoidStatic(bridgeGl, "glLoadIdentity");
        pushMatrix = findVoidStatic(bridgeGl, "glPushMatrix");
        popMatrix = findVoidStatic(bridgeGl, "glPopMatrix");
        translate = findVoidStatic(bridgeGl, "glTranslatef", float.class, float.class, float.class);
        getFloat = findVoidStatic(bridgeGl, "glGetFloat", int.class, FloatBuffer.class);
        newList = findVoidStatic(bridgeGl, "glNewList", int.class, int.class);
        callList = findVoidStatic(bridgeGl, "glCallList", int.class);

        var contextManagerClass = loadBridgeClass(CONTEXT_MANAGER_CLASS_NAME);
        var contextClass = loadBridgeClass(CONTEXT_CLASS_NAME);

        bridgeContext = PUBLIC_LOOKUP
            .findStatic(
                contextManagerClass,
                "createMainContext",
                MethodType.methodType(contextClass))
            .invoke();
    }

    @AfterAll
    static void removeBridgeContext()
            throws Throwable {

        if (bridgeContext == null) {
            return;
        }

        var contextManagerClass = loadBridgeClass(CONTEXT_MANAGER_CLASS_NAME);
        var contextClass = loadBridgeClass(CONTEXT_CLASS_NAME);
        var executorClass = loadBridgeClass(EXECUTOR_CLASS_NAME);

        var executor = PUBLIC_LOOKUP
            .findGetter(contextClass, "exec", executorClass)
            .invoke(bridgeContext);

        PUBLIC_LOOKUP
            .findVirtual(
                executorClass,
                "shutdown",
                MethodType.methodType(void.class))
            .invoke(executor);

        PUBLIC_LOOKUP
            .findStatic(
                contextManagerClass,
                "removeMainContext",
                MethodType.methodType(contextClass))
            .invoke();
    }

    @Nested
    class IsBridgeClassName {

        // The detection's whole job: a class the agent rewrites to must read as the bridge, or every caller is
        // routed as if on the stock renderer.
        @Test
        void recognisesTheClassTheAgentRewritesAModsGl11To() {

            // ARRANGE
            var rewriteTargetName = bridgeGlClassName;

            // ACT
            var actualIsBridge = FastRendering.isBridgeClassName(rewriteTargetName);

            // ASSERT
            assertThat(actualIsBridge)
                .isTrue();
        }
    }

    @Nested
    class GlGetFloat {

        @Test
        void answersTheModelviewTheCallsBeforeItBuiltInColumnMajorOrder()
                throws Throwable {

            // ARRANGE
            matrixMode.invoke(GL11.GL_MODELVIEW);
            loadIdentity.invoke();
            translate.invoke(PAN_X, PAN_Y, 0f);

            var expectedModelview = new float[] {
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                PAN_X, PAN_Y, 0f, 1f};

            // ACT
            var actualModelview = readModelview();

            // ASSERT
            assertThat(actualModelview)
                .containsExactly(expectedModelview);
        }

        @Test
        void answersTheModelviewAPopRestored()
                throws Throwable {

            // ARRANGE
            matrixMode.invoke(GL11.GL_MODELVIEW);
            loadIdentity.invoke();
            pushMatrix.invoke();
            translate.invoke(PAN_X, PAN_Y, 0f);
            popMatrix.invoke();

            var expectedModelview = new float[] {
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f};

            // ACT
            var actualModelview = readModelview();

            // ASSERT
            assertThat(actualModelview)
                .containsExactly(expectedModelview);
        }

        // The refusal KM's guarded reader is built around, kept as the shape a guard catches.
        @Test
        void refusesTheProjectionMatrix() {

            // ARRANGE
            var matrixBuffer = BufferUtils.createFloatBuffer(GlMatrix.FLOAT_COUNT);

            // ACT
            var actualError = catchThrowable(() ->
                getFloat.invoke(GL11.GL_PROJECTION_MATRIX, matrixBuffer));

            // ASSERT
            assertThat(actualError)
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // A display list replayed on the render thread would move the modelview where the calling thread's copy
    // cannot follow, and the read above would answer a matrix the drawing no longer uses, with nothing thrown.
    // The bridge refusing lists is what keeps that copy complete.
    @Nested
    class GlNewList {

        @Test
        void refusesToCompileAList() {

            // ARRANGE
            var listName = 1;

            // ACT
            var actualError = catchThrowable(() ->
                newList.invoke(listName, GL11.GL_COMPILE));

            // ASSERT
            assertThat(actualError)
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // The other half of the same guarantee: no list can be replayed either.
    @Nested
    class GlCallList {

        @Test
        void refusesToCallAList() {

            // ARRANGE
            var listName = 1;

            // ACT
            var actualError = catchThrowable(() -> callList.invoke(listName));

            // ASSERT
            assertThat(actualError)
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class ReadInstalledVersion {

        @Test
        void readsAReleaseOffTheInstalledJar() {

            // ARRANGE
            var expectedReleasePrefix = "v";

            // ACT
            var actualVersion = FastRendering.readInstalledVersion();

            // ASSERT
            assertThat(actualVersion)
                .startsWith(expectedReleasePrefix);
        }
    }

    private static float[] readModelview() throws Throwable {

        var matrixBuffer = BufferUtils.createFloatBuffer(GlMatrix.FLOAT_COUNT);

        getFloat.invoke(GL11.GL_MODELVIEW_MATRIX, matrixBuffer);

        var matrix = new float[GlMatrix.FLOAT_COUNT];

        matrixBuffer.get(matrix);

        return matrix;
    }

    // The table the agent applies to a mod's classes: LWJGL's internal names to the bridge's. The agent holds
    // it on the instance it builds at launch, from its analysis of the game's classes, so it is built the same
    // way here.
    @SuppressWarnings("unchecked")
    private static Map<String, String> readModRewriteTable()
            throws Throwable {

        var embeddedJarClassLoaderClass = loadBridgeClass(EMBEDDED_JAR_CLASS_LOADER_CLASS_NAME);

        var agentLoader = (ClassLoader) PUBLIC_LOOKUP
            .findConstructor(
                embeddedJarClassLoaderClass,
                MethodType.methodType(
                    void.class,
                    ClassLoader.class,
                    String[].class))
            .asFixedArity()
            .invoke(
                embeddedJarClassLoaderClass.getClassLoader(),
                AGENT_EMBEDDED_JAR_NAMES);

        var symbolsClass = loadBridgeClass(
            SYMBOLS_CLASS_NAME,
            agentLoader);

        var transformationsClass = loadBridgeClass(
            TRANSFORMATIONS_CLASS_NAME,
            agentLoader);

        var symbols = PUBLIC_LOOKUP
            .findConstructor(
                symbolsClass,
                MethodType.methodType(void.class))
            .invoke();

        var transformations = PUBLIC_LOOKUP
            .findConstructor(
                transformationsClass,
                MethodType.methodType(
                    void.class,
                    symbolsClass))
            .invoke(symbols);

        return (Map<String, String>) PUBLIC_LOOKUP
            .findGetter(
                transformationsClass,
                MOD_REWRITE_TABLE_NAME,
                Map.class)
            .invoke(transformations);
    }

    private static MethodHandle findVoidStatic(
            Class<?> owner,
            String name,
            Class<?>... parameterTypes)
                throws ReflectiveOperationException {

        return PUBLIC_LOOKUP.findStatic(
            owner,
            name,
            MethodType.methodType(void.class, parameterTypes));
    }

    private static Class<?> loadBridgeClass(String className) {

        return loadBridgeClass(
            className,
            FastRenderingBridgeIntegrationTests
                .class
                .getClassLoader());
    }

    private static Class<?> loadBridgeClass(String className, ClassLoader loader) {

        try {
            return Class.forName(
                className,
                true,
                loader);

        } catch (ClassNotFoundException missing) {

            throw new IllegalStateException(
                "No " + className + " on the test runtime. This suite needs the install's starsector-core/fr.jar,"
                    + " from a Fast-Rendering-patched install.",
                missing);
        }
    }

    private static Path resolveNativesDirectory() {

        var osName = System
            .getProperty("os.name")
            .toLowerCase(Locale.ROOT);

        String platformFolder;

        if (osName.contains("win")) {
            platformFolder = "windows";

        } else if (osName.contains("linux")) {
            platformFolder = "linux";

        } else {
            throw new IllegalStateException("No LWJGL native folder is known for " + osName + ".");
        }

        var nativesDirectory = Path.of(
            System.getProperty(STARSECTOR_ROOT_PROPERTY),
            "starsector-core",
            "native",
            platformFolder);

        if (!Files.isDirectory(nativesDirectory)) {

            throw new IllegalStateException(
                "No LWJGL natives at " + nativesDirectory + "."
                    + " Setting the bridge up loads LWJGL's native library;"
                    + " see docs/dev/rendering-environment.md for where the Linux one comes from.");
        }

        return nativesDirectory;
    }
}
