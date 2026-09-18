package kmlib.opengl;

import org.lwjgl.util.vector.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Classes carrying the six mirrored members in the shapes the bridge adapter is compiled against,
 * answering to the names the probe looks them up by - plus the variants a case swaps one of them for.
 *
 * <p>Fast Rendering's own classes are the wrong subjects: the bridge on the runtime is whatever the
 * install has, so a case against it pins the machine rather than the probe. These answer to the real
 * names through a lookup the case hands in, so what a case pins is the probe's reading of a shape
 * the case wrote.
 *
 * <p>Final class with a private constructor: a holder of shapes and lookups, no instances.
 */
final class MirroredBridgeShapes {

    static final String CONTEXT_MANAGER_NAME = "com.genir.renderer.bridge.context.ContextManager";
    static final String VERSION_NAME = "com.genir.renderer.Version";

    /** What the matching version shape reports. */
    static final String REPORTED_VERSION = "v9.9.9";

    private static final String CONTEXT_NAME = "com.genir.renderer.bridge.context.Context";
    private static final String EXECUTOR_NAME = "com.genir.renderer.bridge.context.Executor";
    private static final String GL_COMMAND_NAME = "com.genir.renderer.bridge.interfaces.GLCommand";
    private static final String TRANSFORM_MANAGER_NAME = "com.genir.renderer.bridge.context.TransformManager";

    private MirroredBridgeShapes() {
        // holder of shapes and lookups, no instances.
    }

    /**
     * @return every bridge name mapped to the shape whose members match the mirrors, as a map a
     *         case may swap one entry of
     */
    static Map<String, Class<?>> mapMatchingShapesByName() {

        var shapesByName = new HashMap<String, Class<?>>();
        shapesByName.put(CONTEXT_MANAGER_NAME, ContextManagerShape.class);
        shapesByName.put(CONTEXT_NAME, ContextShape.class);
        shapesByName.put(EXECUTOR_NAME, ExecutorShape.class);
        shapesByName.put(GL_COMMAND_NAME, GLCommandShape.class);
        shapesByName.put(TRANSFORM_MANAGER_NAME, TransformManagerShape.class);
        shapesByName.put(VERSION_NAME, VersionShape.class);
        return shapesByName;
    }

    /**
     * @param shapesByName which class answers which name
     * @return a lookup answering from the map, and throwing for any name it does not hold
     */
    static FastRenderingBridgeDiagnostic.ClassLookup lookupAmong(Map<String, Class<?>> shapesByName) {

        return className -> {
            var shape = shapesByName.get(className);
            if (shape == null) {
                throw new ClassNotFoundException(className);
            }
            return shape;
        };
    }

    /** {@code ContextManager} as mirrored: a static accessor answering the context. */
    public static final class ContextManagerShape {

        public static ContextShape getThreadContext() {
            return null;
        }
    }

    /** {@code ContextManager} with the accessor re-signatured to answer something else. */
    public static final class ContextManagerResignaturedShape {

        public static Object getThreadContext() {
            return null;
        }
    }

    /** {@code ContextManager} present but without the accessor at all. */
    public static final class ContextManagerWithoutAccessorShape {
    }

    /** {@code Context} as mirrored: the two public final fields the adapter reads. */
    public static final class ContextShape {

        public final TransformManagerShape transformManager = null;

        public final ExecutorShape exec = null;
    }

    /** {@code Executor} as mirrored: the one enqueue the adapter calls. */
    public static final class ExecutorShape {

        public void execute(GLCommandShape command) {
        }
    }

    /** {@code GLCommand} as mirrored. */
    public interface GLCommandShape {

        void run(ContextShape context, float[] args, int argsOffset);
    }

    /** {@code TransformManager} as mirrored: the modelview read. */
    public static final class TransformManagerShape {

        public Matrix4f getCPUModelView() {
            return null;
        }
    }

    /** {@code Version} as shipped from v0.8.2: a static accessor answering the release. */
    public static final class VersionShape {

        public static String getVersion() {
            return REPORTED_VERSION;
        }
    }

    /** {@code Version} whose accessor throws when called. */
    public static final class VersionThrowingShape {

        public static String getVersion() {
            throw new IllegalStateException("A version that could not be read");
        }
    }
}
