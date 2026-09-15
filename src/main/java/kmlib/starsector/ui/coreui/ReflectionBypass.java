/*
 * Copyright Starficz
 * Copyright 2026 Klark Morrigan
 *
 * Derived from org.magiclib.ReflectionUtils in MagicLib, and modified in 2026:
 * rewritten in Java, reduced to the classloader route and the handles over it,
 * and documented to this repository's conventions.
 *
 * Credits to Lukas04 for his ReflectionUtils, Lyravega, Float, and Andylizi for
 * the original idea.
 *
 * This file is part of KMLib.
 *
 * KMLib is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License version 3 as published by the
 * Free Software Foundation.
 *
 * KMLib is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with KMLib. If not, see <https://www.gnu.org/licenses/>.
 */
package kmlib.starsector.ui.coreui;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * The route around the game's reflection ban, and the only place in the library that takes it.
 *
 * <p>The mod classloader refuses to load {@code java.lang.reflect} for mod code, alongside
 * {@code java.io}, {@code javax.script} and {@code java.util.prefs} - a sandbox over what a
 * downloaded jar can reach on a player's machine. This class asks the bootstrap loader for the
 * reflection types directly, which that refusal is never consulted about, and drives their members
 * through method handles so no reflection type is named in this library's bytecode where the
 * refusal would see it.
 *
 * <p>Restoring that capability restores all of it: whoever holds these handles could reach the rest
 * of what the sandbox covers just as easily. What keeps the library inside the sandbox regardless
 * is that nothing outside this package can ask - hence package-private, and hence a surface that is
 * one operation per member rather than anything a caller could aim elsewhere.
 *
 * <p>Nothing here decides which member to act on. It reads and calls the one it is handed, leaving
 * every question of which that should be to {@link ReflectedMembers} - so what the ban exists to
 * contain is contained to a file that holds no logic, and the logic sits in a file that holds no
 * way in.
 *
 * <p>Members arrive and are passed as bare {@link Object}s throughout. Their own types are the ones
 * the ban covers, so naming them in a signature would resolve them through the mod classloader and
 * fail where the whole point is not to ask it.
 */
final class ReflectionBypass {

    // Declared in resolution order rather than alphabetically: each handle is found on the class
    // above it, and a static field initialiser runs where it is written.
    //
    // Class.class's own loader is the bootstrap one. The refusal is made on the way down from the
    // mod classloader, so a load asked of the bottom directly never passes through it.
    private static final Class<?> FIELD_CLASS = loadBootstrapClass("java.lang.reflect.Field");

    private static final MethodHandle GET_FIELD_NAME =
        findHandle(FIELD_CLASS, "getName", String.class);
    private static final MethodHandle GET_FIELD_TYPE =
        findHandle(FIELD_CLASS, "getType", Class.class);
    private static final MethodHandle GET_FIELD_VALUE =
        findHandle(FIELD_CLASS, "get", Object.class, Object.class);
    private static final MethodHandle SET_FIELD_ACCESSIBLE =
        findHandle(FIELD_CLASS, "setAccessible", void.class, boolean.class);
    private static final MethodHandle SET_FIELD_VALUE =
        findHandle(FIELD_CLASS, "set", void.class, Object.class, Object.class);

    private static final Class<?> METHOD_CLASS = loadBootstrapClass("java.lang.reflect.Method");

    private static final MethodHandle GET_METHOD_NAME =
        findHandle(METHOD_CLASS, "getName", String.class);
    private static final MethodHandle GET_METHOD_PARAMETER_TYPES =
        findHandle(METHOD_CLASS, "getParameterTypes", Class[].class);
    private static final MethodHandle GET_METHOD_RETURN_TYPE =
        findHandle(METHOD_CLASS, "getReturnType", Class.class);
    private static final MethodHandle INVOKE_METHOD =
        findHandle(METHOD_CLASS, "invoke", Object.class, Object.class, Object[].class);
    private static final MethodHandle SET_METHOD_ACCESSIBLE =
        findHandle(METHOD_CLASS, "setAccessible", void.class, boolean.class);

    private static final Class<?> CONSTRUCTOR_CLASS =
        loadBootstrapClass("java.lang.reflect.Constructor");

    private static final MethodHandle GET_CONSTRUCTOR_PARAMETER_TYPES =
        findHandle(CONSTRUCTOR_CLASS, "getParameterTypes", Class[].class);
    private static final MethodHandle NEW_INSTANCE =
        findHandle(CONSTRUCTOR_CLASS, "newInstance", Object.class, Object[].class);
    private static final MethodHandle SET_CONSTRUCTOR_ACCESSIBLE =
        findHandle(CONSTRUCTOR_CLASS, "setAccessible", void.class, boolean.class);

    private ReflectionBypass() {
    }

    /**
     * Builds one, reaching a constructor its class keeps to itself as readily as one it publishes.
     *
     * @param constructor the constructor to call
     * @param arguments   what to pass
     * @return what it built
     */
    static Object construct(Object constructor, Object... arguments) {

        try {
            return NEW_INSTANCE.invoke(constructor, arguments);

        } catch (Throwable thrown) {
            throw throwUnchecked(thrown);
        }
    }

    /**
     * Calls a member, reaching one its shape keeps to itself as readily as one it publishes.
     *
     * @param method    the method to call
     * @param instance  the object to call on, or null for a static method
     * @param arguments what to pass
     * @return whatever it answered, or null for a void one
     */
    static Object invokeMethod(Object method, Object instance, Object... arguments) {

        try {
            return INVOKE_METHOD.invoke(method, instance, arguments);

        } catch (Throwable thrown) {
            throw throwUnchecked(thrown);
        }
    }

    /**
     * Opens a constructor up to being called whatever its access level.
     *
     * @param constructor the constructor to open up
     */
    static void makeConstructorAccessible(Object constructor) {

        try {
            SET_CONSTRUCTOR_ACCESSIBLE.invoke(constructor, true);

        } catch (Throwable cannotOpenMember) {
            throw throwUnchecked(cannotOpenMember);
        }
    }

    /**
     * Opens a field up to being read and written whatever its access level, which is what makes the
     * fields worth reaching reachable at all.
     *
     * @param field the field to open up
     */
    static void makeFieldAccessible(Object field) {

        try {
            SET_FIELD_ACCESSIBLE.invoke(field, true);

        } catch (Throwable cannotOpenMember) {
            throw throwUnchecked(cannotOpenMember);
        }
    }

    /**
     * Opens a method up to being called whatever its access level.
     *
     * @param method the method to open up
     */
    static void makeMethodAccessible(Object method) {

        try {
            SET_METHOD_ACCESSIBLE.invoke(method, true);

        } catch (Throwable cannotOpenMember) {
            throw throwUnchecked(cannotOpenMember);
        }
    }

    /**
     * @param constructor the constructor to describe
     * @return what it takes, in declaration order; the constructor's own array, not a copy
     */
    static Class<?>[] readConstructorParameterTypes(Object constructor) {

        try {
            return (Class<?>[]) GET_CONSTRUCTOR_PARAMETER_TYPES.invoke(constructor);

        } catch (Throwable cannotReadMember) {
            throw throwUnchecked(cannotReadMember);
        }
    }

    /**
     * @param field the field to describe
     * @return its own name
     */
    static String readFieldName(Object field) {

        try {
            return (String) GET_FIELD_NAME.invoke(field);

        } catch (Throwable cannotReadMember) {
            throw throwUnchecked(cannotReadMember);
        }
    }

    /**
     * @param field the field to describe
     * @return the type it is declared as, which is not necessarily the type of what it holds
     */
    static Class<?> readFieldType(Object field) {

        try {
            return (Class<?>) GET_FIELD_TYPE.invoke(field);

        } catch (Throwable cannotReadMember) {
            throw throwUnchecked(cannotReadMember);
        }
    }

    /**
     * @param field    the field to read
     * @param instance the object to read it off, or null for a static field
     * @return what it holds
     */
    static Object readFieldValue(Object field, Object instance) {

        try {
            return GET_FIELD_VALUE.invoke(field, instance);

        } catch (Throwable thrown) {
            throw throwUnchecked(thrown);
        }
    }

    /**
     * @param method the method to describe
     * @return its own name, which on an obfuscated shape is regenerated with each game build
     */
    static String readMethodName(Object method) {

        try {
            return (String) GET_METHOD_NAME.invoke(method);

        } catch (Throwable cannotReadMember) {
            throw throwUnchecked(cannotReadMember);
        }
    }

    /**
     * @param method the method to describe
     * @return what it takes, in declaration order, a primitive parameter as its own primitive type
     *         rather than as the box for it; the method's own array, not a copy
     */
    static Class<?>[] readMethodParameterTypes(Object method) {

        try {
            return (Class<?>[]) GET_METHOD_PARAMETER_TYPES.invoke(method);

        } catch (Throwable cannotReadMember) {
            throw throwUnchecked(cannotReadMember);
        }
    }

    /**
     * @param method the method to describe
     * @return what it answers with, {@code void.class} for one that answers nothing
     */
    static Class<?> readMethodReturnType(Object method) {

        try {
            return (Class<?>) GET_METHOD_RETURN_TYPE.invoke(method);

        } catch (Throwable cannotReadMember) {
            throw throwUnchecked(cannotReadMember);
        }
    }

    /**
     * Writes a field, reaching one its shape keeps to itself as readily as one it publishes.
     *
     * @param field    the field to write
     * @param instance the object to write it on, or null for a static field
     * @param value    what to put in it
     */
    static void writeFieldValue(Object field, Object instance, Object value) {

        try {
            SET_FIELD_VALUE.invoke(field, instance, value);

        } catch (Throwable thrown) {
            throw throwUnchecked(thrown);
        }
    }

    // Rethrows what a handle threw, as it was thrown and without declaring it.
    //
    // A handle's own invoke declares Throwable, so catching is unavoidable; the question is only
    // what comes back out. Wrapping would replace the target's own failure with one of ours, and
    // declaring it would put a checked exception on every read and call this package offers. Both
    // cost more than letting it through does: a caller tells an unreachable member from a target
    // that refused by the type it catches, so that type has to arrive intact. Which is why every
    // caller of this package is told to guard over Throwable.
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException throwUnchecked(Throwable thrown) throws T {
        throw (T) thrown;
    }

    private static Class<?> loadBootstrapClass(String className) {

        try {
            return Class.forName(className, false, Class.class.getClassLoader());

        } catch (ClassNotFoundException cannotLoadClass) {
            throw new IllegalStateException(
                "The runtime does not carry " + className + ".", cannotLoadClass);
        }
    }

    // A handle onto one member of a reflection type. Handles rather than ordinary calls, because a
    // call would name the type in this class's own constant pool - which is the one thing the mod
    // classloader does get asked about.
    private static MethodHandle findHandle(
        Class<?> reflectionClass,
        String memberName,
        Class<?> returnType,
        Class<?>... parameterTypes) {

        try {
            return MethodHandles
                .lookup()
                .findVirtual(
                    reflectionClass,
                    memberName,
                    MethodType.methodType(returnType, parameterTypes));

        } catch (NoSuchMethodException | IllegalAccessException cannotReachMember) {
            throw new IllegalStateException("The runtime's " + reflectionClass.getName()
                + " does not offer " + memberName + " as this expects it.", cannotReachMember);
        }
    }
}
