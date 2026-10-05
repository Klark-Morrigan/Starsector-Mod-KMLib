package kmlib.opengl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.FloatBuffer;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Holds the installed Fast Rendering to the three things KM takes from it: that its agent rewrites a mod's
 * {@code GL11} onto a class detection recognises, that the class it rewrites to answers
 * {@code glGetFloat(GL_MODELVIEW_MATRIX)} with the matrix the calls before it built, and that its version
 * class still reads.
 *
 * <p>Drives the real {@code fr.jar} and {@code fr.agent.jar} the build puts on the test runtime, by name
 * through method handles, so nothing here compiles against either. The bridge class is the one the agent's
 * own rewrite table names, so the read is taken where a rewritten mod takes it.
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

    // The agent's rewrite tables, and the one it applies to a mod's classes.
    private static final String TRANSFORMATIONS_CLASS_NAME = "com.genir.renderer.agent.Transformations";
    private static final String MOD_REWRITE_TABLE_NAME = "opengl";
    private static final String LWJGL_GL11_INTERNAL_NAME = "org/lwjgl/opengl/GL11";

    // Uneven, and different on the two axes, so a transposed or swapped read cannot land on a match.
    private static final float PAN_X = 137.01f;
    private static final float PAN_Y = 41.01f;
    private static final float LIST_PAN_X = 5.5f;
    private static final float LIST_PAN_Y = 7.25f;

    private static final int DISPLAY_LIST_ID = 1;

    private static final MethodHandles.Lookup PUBLIC_LOOKUP = MethodHandles.publicLookup();

    // The rewrite target for GL11, as the agent's table names it.
    private static String bridgeGlClassName;

    // The bridge's GL11 entry points this suite drives, each the shape LWJGL's own method has.
    private static MethodHandle matrixMode;
    private static MethodHandle loadIdentity;
    private static MethodHandle pushMatrix;
    private static MethodHandle popMatrix;
    private static MethodHandle translate;
    private static MethodHandle newList;
    private static MethodHandle endList;
    private static MethodHandle callList;
    private static MethodHandle getFloat;

    // The bridge's main context, created once: the bridge holds it in a static, one per process.
    private static Object bridgeContext;

    @BeforeAll
    static void createBridgeContext() throws Throwable {

        System.setProperty(LWJGL_LIBRARY_PATH_PROPERTY, resolveNativesDirectory().getAbsolutePath());

        bridgeGlClassName = readModRewriteTable()
            .get(LWJGL_GL11_INTERNAL_NAME)
            .replace('/', '.');

        var bridgeGl = loadBridgeClass(bridgeGlClassName);

        matrixMode = findVoidStatic(bridgeGl, "glMatrixMode", int.class);
        loadIdentity = findVoidStatic(bridgeGl, "glLoadIdentity");
        pushMatrix = findVoidStatic(bridgeGl, "glPushMatrix");
        popMatrix = findVoidStatic(bridgeGl, "glPopMatrix");
        translate = findVoidStatic(bridgeGl, "glTranslatef", float.class, float.class, float.class);
        newList = findVoidStatic(bridgeGl, "glNewList", int.class, int.class);
        endList = findVoidStatic(bridgeGl, "glEndList");
        callList = findVoidStatic(bridgeGl, "glCallList", int.class);
        getFloat = findVoidStatic(bridgeGl, "glGetFloat", int.class, FloatBuffer.class);

        var contextManagerClass = loadBridgeClass(CONTEXT_MANAGER_CLASS_NAME);
        var contextClass = loadBridgeClass(CONTEXT_CLASS_NAME);

        bridgeContext = PUBLIC_LOOKUP
            .findStatic(contextManagerClass, "createMainContext", MethodType.methodType(contextClass))
            .invoke();
    }

    @AfterAll
    static void removeBridgeContext() throws Throwable {

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
            .findVirtual(executorClass, "shutdown", MethodType.methodType(void.class))
            .invoke(executor);

        PUBLIC_LOOKUP
            .findStatic(contextManagerClass, "removeMainContext", MethodType.methodType(contextClass))
            .invoke();
    }

    @Nested
    class IsBridgeClassName {

        @Test
        void recognisesTheClassTheAgentRewritesAModsGl11To() {

            // The detection's whole job: a class the agent rewrites to must read as the bridge, or every
            // caller is routed as if on the stock renderer.
            assertThat(FastRendering.isBridgeClassName(bridgeGlClassName))
                .isTrue();
        }
    }

    @Nested
    class GlGetFloat {

        @Test
        void answersTheModelviewTheCallsBeforeItBuiltInColumnMajorOrder() throws Throwable {

            matrixMode.invoke(GL11.GL_MODELVIEW);
            loadIdentity.invoke();
            pushMatrix.invoke();
            translate.invoke(PAN_X, PAN_Y, 0f);

            assertThat(readModelview())
                .containsExactly(
                    1f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    PAN_X, PAN_Y, 0f, 1f);

            popMatrix.invoke();
        }

        @Test
        void answersTheModelviewAPopRestored() throws Throwable {

            matrixMode.invoke(GL11.GL_MODELVIEW);
            loadIdentity.invoke();
            pushMatrix.invoke();
            translate.invoke(PAN_X, PAN_Y, 0f);
            popMatrix.invoke();

            assertThat(readModelview())
                .containsExactly(
                    1f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f);
        }

        @Test
        void answersTheModelviewADisplayListAppliedWhenCalled() throws Throwable {

            matrixMode.invoke(GL11.GL_MODELVIEW);
            loadIdentity.invoke();
            newList.invoke(DISPLAY_LIST_ID, GL11.GL_COMPILE);
            translate.invoke(LIST_PAN_X, LIST_PAN_Y, 0f);
            endList.invoke();

            // Compiling records the translate rather than applying it.
            assertThat(readModelview())
                .containsExactly(
                    1f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f);

            callList.invoke(DISPLAY_LIST_ID);

            assertThat(readModelview())
                .containsExactly(
                    1f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    LIST_PAN_X, LIST_PAN_Y, 0f, 1f);

            loadIdentity.invoke();
        }

        @Test
        void refusesTheProjectionMatrix() {

            // The refusal KM's guarded reader is built around, kept as the shape a guard catches.
            var matrixBuffer = BufferUtils.createFloatBuffer(GlMatrix.FLOAT_COUNT);

            assertThatThrownBy(() -> getFloat.invoke(GL11.GL_PROJECTION_MATRIX, matrixBuffer))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class ReadInstalledVersion {

        @Test
        void readsAReleaseOffTheInstalledJar() {

            assertThat(FastRendering.readInstalledVersion())
                .startsWith("v");
        }
    }

    private static float[] readModelview() throws Throwable {

        var matrixBuffer = BufferUtils.createFloatBuffer(GlMatrix.FLOAT_COUNT);

        getFloat.invoke(GL11.GL_MODELVIEW_MATRIX, matrixBuffer);

        var matrix = new float[GlMatrix.FLOAT_COUNT];

        matrixBuffer.get(matrix);

        return matrix;
    }

    // The table the agent applies to a mod's classes: LWJGL's internal names to the bridge's.
    @SuppressWarnings("unchecked")
    private static Map<String, String> readModRewriteTable() throws Throwable {

        return (Map<String, String>) PUBLIC_LOOKUP
            .findStaticGetter(loadBridgeClass(TRANSFORMATIONS_CLASS_NAME), MOD_REWRITE_TABLE_NAME, Map.class)
            .invoke();
    }

    private static MethodHandle findVoidStatic(Class<?> owner, String name, Class<?>... parameterTypes)
            throws ReflectiveOperationException {

        return PUBLIC_LOOKUP.findStatic(
            owner,
            name,
            MethodType.methodType(void.class, parameterTypes));
    }

    private static Class<?> loadBridgeClass(String className) {

        try {
            return Class.forName(className);

        } catch (ClassNotFoundException missing) {

            throw new IllegalStateException(
                "No " + className + " on the test runtime. This suite needs the install's starsector-core/fr.jar"
                    + " and fr.agent.jar, from a Fast-Rendering-patched install.",
                missing);
        }
    }

    private static File resolveNativesDirectory() {

        var osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);

        String platformFolder;

        if (osName.contains("win")) {
            platformFolder = "windows";

        } else if (osName.contains("linux")) {
            platformFolder = "linux";

        } else {
            throw new IllegalStateException("No LWJGL native folder is known for " + osName + ".");
        }

        var nativesDirectory = new File(
            System.getProperty(STARSECTOR_ROOT_PROPERTY),
            "starsector-core" + File.separator + "native" + File.separator + platformFolder);

        if (!nativesDirectory.isDirectory()) {

            throw new IllegalStateException(
                "No LWJGL natives at " + nativesDirectory + ". Setting the bridge up loads LWJGL's native library;"
                    + " see docs/dev/rendering-environment.md for where the Linux one comes from.");
        }

        return nativesDirectory;
    }
}
