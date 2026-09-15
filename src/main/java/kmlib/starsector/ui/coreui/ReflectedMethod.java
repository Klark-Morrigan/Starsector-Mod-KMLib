/*
 * Copyright Starficz
 * Copyright 2026 Klark Morrigan
 *
 * Derived from ReflectedMethod in org.magiclib.ReflectionUtils, and modified in
 * 2026: rewritten in Java, and describing and opening the member once on the way
 * in rather than at each ask.
 *
 * Licensed under the GNU Lesser General Public License version 3. See LICENSE at
 * the repository root, and THIRD-PARTY-NOTICES.md for what this derives from.
 */
package kmlib.starsector.ui.coreui;

/**
 * One method a shape offers, callable without its own type ever being named.
 *
 * <p>Describes itself once, on the way in, and is opened on the way in - see {@link ReflectedField}
 * for why both happen there rather than at each ask.
 *
 * <p>{@link CoreUiMethod} is what leaves the package in its place. The two are kept apart because
 * this one is a member of any shape at all, where that one is what a caller outside the package is
 * handed and may go on calling - so the reach answers in its own terms and the package's published
 * type is free to change without disturbing them.
 */
final class ReflectedMethod {

    private final Object method;
    private final String name;
    private final Class<?>[] parameterTypes;
    private final Class<?> returnType;

    ReflectedMethod(Object method) {

        this.method = method;
        this.name = ReflectionBypass.readMethodName(method);
        this.parameterTypes = ReflectionBypass.readMethodParameterTypes(method);
        this.returnType = ReflectionBypass.readMethodReturnType(method);

        ReflectionBypass.makeMethodAccessible(method);
    }

    /**
     * @return its own name, which on an obfuscated shape is regenerated with each game build
     */
    String getName() {
        return name;
    }

    /**
     * @return what it takes, in declaration order, a primitive parameter as its own primitive type
     *         rather than as the box for it; the method's own array, so a caller handing it on
     *         copies it first
     */
    Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * @return what it answers with, {@code void.class} for one that answers nothing
     */
    Class<?> getReturnType() {
        return returnType;
    }

    /**
     * Calls it.
     *
     * @param instance  the object to call on, or null for a static method
     * @param arguments what to pass, matching the parameter list above
     * @return whatever it answered, or null for a void one
     * @throws RuntimeException when the call fails. It arrives undeclared and not necessarily as a
     *                          {@link RuntimeException} - the target's own throw comes back wrapped
     *                          in a checked exception, rethrown as it was thrown rather than
     *                          replaced - so a caller guarding this has to catch {@link Throwable}
     */
    Object invokeOn(Object instance, Object... arguments) {
        return ReflectionBypass.invokeMethod(method, instance, arguments);
    }
}
