/*
 * Copyright Starficz
 * Copyright 2026 Klark Morrigan
 *
 * The matching rules are derived from org.magiclib.ReflectionUtils in MagicLib,
 * and modified in 2026: rewritten in Java and lifted out of the reach into a
 * facility of their own.
 *
 * Licensed under the GNU Lesser General Public License version 3. See LICENSE at
 * the repository root, and THIRD-PARTY-NOTICES.md for what this derives from.
 */
package kmlib.starsector.ui.coreui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Whether a value of one type would be accepted where another is declared, which is what selects a
 * member when a caller has arguments rather than a signature.
 *
 * <p>Assignment compatibility rather than identity, so a member resolves on the same terms a direct
 * call would be allowed on: a parameter declared as a supertype or an interface of the argument
 * takes it, a narrower primitive widens into a wider one, a primitive boxes on its way into a
 * reference parameter and a box unwraps on its way into a primitive one. Anything stricter would
 * leave the core UI's own entry points unreachable, since a caller can only ever hand over a box.
 *
 * <p>Apart from the reach itself because it needs none of it: this is arithmetic over
 * {@link Class} objects, answering the same way whether the member it is about was found by
 * reflection or handed over directly.
 */
final class ParameterCompatibility {

    // The boxed form of each primitive. void is left out: nothing is passed as one.
    private static final Map<Class<?>, Class<?>> WRAPPER_BY_PRIMITIVE = Map.of(
        boolean.class, Boolean.class,
        byte.class, Byte.class,
        char.class, Character.class,
        short.class, Short.class,
        int.class, Integer.class,
        long.class, Long.class,
        float.class, Float.class,
        double.class, Double.class);

    // Derived rather than written out a second time, so the pairing has one place to be wrong.
    private static final Map<Class<?>, Class<?>> PRIMITIVE_BY_WRAPPER = WRAPPER_BY_PRIMITIVE
        .entrySet()
        .stream()
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey));

    // Which primitives widen into which. char widens as a narrow unsigned integer does, and nothing
    // widens into boolean.
    private static final Map<Class<?>, Set<Class<?>>> WIDENS_FROM = Map.of(
        short.class, Set.of(byte.class),
        int.class, Set.of(byte.class, short.class, char.class),
        long.class, Set.of(byte.class, short.class, char.class, int.class),
        float.class, Set.of(byte.class, short.class, char.class, int.class, long.class),
        double.class, Set.of(byte.class, short.class, char.class, int.class, long.class,
            float.class));

    private ParameterCompatibility() {
    }

    /**
     * Whether a call handing over these arguments would be allowed to reach a member taking these
     * parameters.
     *
     * @param parameterTypes what the member declares, in order
     * @param argumentTypes  what the caller has, in order, null for an argument that was null
     * @return whether each argument fits the parameter in its position, count included
     */
    static boolean isCallableWith(Class<?>[] parameterTypes, List<Class<?>> argumentTypes) {

        if (parameterTypes.length != argumentTypes.size()) {
            return false;
        }

        for (var index = 0; index < parameterTypes.length; index++) {
            if (!isParameterCompatible(parameterTypes[index], argumentTypes.get(index))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a value of the argument's type would be accepted where the parameter's type is
     * declared.
     *
     * @param parameterType what is declared
     * @param argumentType  what the caller has, or null for a value that was null
     * @return whether the one fits the other
     */
    static boolean isParameterCompatible(Class<?> parameterType, Class<?> argumentType) {

        // A null argument carries no type of its own, so it fits whatever can hold null.
        if (argumentType == null) {
            return !parameterType.isPrimitive();
        }

        if (parameterType.equals(argumentType)) {
            return true;
        }

        if (parameterType.isPrimitive()) {
            // A box unwraps on the way in, after which it may still widen - so Integer reaches a
            // float parameter by the same two steps a direct call would take.
            var argumentAsPrimitive = unboxType(argumentType);

            return argumentAsPrimitive != null
                && (argumentAsPrimitive.equals(parameterType)
                    || WIDENS_FROM.getOrDefault(parameterType, Set.of()).contains(argumentAsPrimitive));
        }

        // A primitive argument reaching a reference parameter boxes on the way in.
        var argumentAsReference = argumentType.isPrimitive()
            ? WRAPPER_BY_PRIMITIVE.get(argumentType)
            : argumentType;

        return argumentAsReference != null && parameterType.isAssignableFrom(argumentAsReference);
    }

    /**
     * The types to resolve a member against, taken from the arguments themselves.
     *
     * <p>A box is unwrapped to the primitive it stands for, which is what makes the primitive entry
     * points reachable: a caller holds a {@code Float} and the member declares {@code float}, and
     * without this every such member would be missed for want of an exact type.
     *
     * @param arguments what the caller is passing
     * @return one type per argument, null where the argument was null
     */
    static List<Class<?>> readArgumentTypes(Object[] arguments) {

        // An ArrayList rather than one of the factory lists, which reject the nulls a null argument
        // contributes.
        var argumentTypes = new ArrayList<Class<?>>(arguments.length);

        for (var argument : arguments) {
            argumentTypes.add(argument == null
                ? null
                : unboxOrKeepType(argument.getClass()));
        }
        return argumentTypes;
    }

    /**
     * @param type the type to unwrap
     * @return the primitive a box stands for, or the type itself when it is not a box
     */
    static Class<?> unboxOrKeepType(Class<?> type) {
        return PRIMITIVE_BY_WRAPPER.getOrDefault(type, type);
    }

    // The primitive a type stands for: itself when already primitive, the unwrapped one when a box,
    // and nothing at all otherwise.
    private static Class<?> unboxType(Class<?> type) {

        return type.isPrimitive()
            ? type
            : PRIMITIVE_BY_WRAPPER.get(type);
    }
}
