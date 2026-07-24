package kmlib.reflection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Reads private fields and invokes methods on objects the game otherwise locks away - the sanctioned
 * way to reach a live UI widget's private state when no public API exposes it.
 *
 * <p>The game's script classloader denies mod code direct {@code java.lang.reflect} calls: a plain
 * {@code field.get(...)} or {@code method.invoke(...)} throws. The check only catches the direct
 * call, though, not one made through a {@link MethodHandle}. So the reflect classes are loaded
 * through the system classloader (outside the script classloader's reach) and their operations are
 * driven through method handles bound once here. {@code Class#getDeclaredFields} and
 * {@code Class#getMethod} are on {@link Class}, not {@code java.lang.reflect}, so they are called
 * directly; only the {@code Field}/{@code Method} operations go through handles.
 *
 * <p>Reflection into obfuscated internals is inherently brittle across game builds, so every method
 * here throws rather than swallowing - the caller decides how to degrade when a read fails, and does
 * so once at its own boundary rather than this guessing a safe default.
 */
public final class Reflection {

    private static final Object[] NO_ARGS = new Object[0];

    // Field and Method operations, bound once. The reflect classes come from the system classloader
    // so they load at all under the script classloader's restriction.
    private static final MethodHandle FIELD_GET;
    private static final MethodHandle FIELD_GET_TYPE;
    private static final MethodHandle SET_ACCESSIBLE;
    private static final MethodHandle METHOD_INVOKE;

    static {
        try {
            var lookup = MethodHandles.lookup();
            var systemClassLoader = ClassLoader.getSystemClassLoader();
            var fieldClass = Class.forName("java.lang.reflect.Field", false, systemClassLoader);
            var methodClass = Class.forName("java.lang.reflect.Method", false, systemClassLoader);
            var accessibleClass =
                    Class.forName("java.lang.reflect.AccessibleObject", false, systemClassLoader);
            FIELD_GET = lookup.findVirtual(
                    fieldClass, "get", MethodType.methodType(Object.class, Object.class));
            FIELD_GET_TYPE = lookup.findVirtual(
                    fieldClass, "getType", MethodType.methodType(Class.class));
            SET_ACCESSIBLE = lookup.findVirtual(
                    accessibleClass, "setAccessible", MethodType.methodType(void.class, boolean.class));
            METHOD_INVOKE = lookup.findVirtual(methodClass, "invoke",
                    MethodType.methodType(Object.class, Object.class, Object[].class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Reflection() {
    }

    /**
     * The value of {@code instance}'s first non-null field whose declared type is {@code fieldType}
     * or a subtype, searching up the class hierarchy. Matching by type rather than name is what keeps
     * this stable across obfuscated builds, where the field name changes but its type does not.
     *
     * @param instance  the object to read from
     * @param fieldType the field type to match
     * @return the first non-null matching field's value, or {@code null} when none matches or all
     *         match null
     * @throws Throwable when a field read fails
     */
    public static Object readFieldOfType(Object instance, Class<?> fieldType) throws Throwable {
        for (var clazz = instance.getClass(); clazz != null && clazz != Object.class;
                clazz = clazz.getSuperclass()) {
            for (var field : clazz.getDeclaredFields()) {
                var declaredType = (Class<?>) FIELD_GET_TYPE.invoke(field);
                if (!fieldType.isAssignableFrom(declaredType)) {
                    continue;
                }
                SET_ACCESSIBLE.invoke(field, true);
                var value = FIELD_GET.invoke(field, instance);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Invokes {@code instance}'s no-argument method named {@code methodName} and returns its result.
     *
     * @param instance   the object to invoke on
     * @param methodName the exact method name
     * @return the method's return value
     * @throws Throwable when no such method exists or the call fails
     */
    public static Object invokeNoArg(Object instance, String methodName) throws Throwable {
        var method = instance.getClass().getMethod(methodName);
        SET_ACCESSIBLE.invoke(method, true);
        return METHOD_INVOKE.invoke(method, instance, NO_ARGS);
    }
}
