/*
 * Copyright Starficz
 * Copyright 2026 Klark Morrigan
 *
 * Derived from ReflectedConstructor in org.magiclib.ReflectionUtils, and modified
 * in 2026: rewritten in Java, and describing and opening the member once on the
 * way in rather than at each ask.
 *
 * Licensed under the GNU Lesser General Public License version 3. See LICENSE at
 * the repository root, and THIRD-PARTY-NOTICES.md for what this derives from.
 */
package kmlib.starsector.ui.coreui;

/**
 * One constructor a shape offers, callable without its own type ever being named.
 *
 * <p>Describes itself once, on the way in, and is opened on the way in - see {@link ReflectedField}
 * for why both happen there rather than at each ask.
 *
 * <p>Carries no name, a constructor having none of its own: what it takes is the only thing telling
 * one apart from another, which is why a search over them matches on that alone.
 */
final class ReflectedConstructor {

    private final Object constructor;
    private final Class<?>[] parameterTypes;

    ReflectedConstructor(Object constructor) {

        this.constructor = constructor;
        this.parameterTypes = ReflectionBypass.readConstructorParameterTypes(constructor);

        ReflectionBypass.makeConstructorAccessible(constructor);
    }

    /**
     * @return what it takes, in declaration order; the constructor's own array, so a caller handing
     *         it on copies it first
     */
    Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Builds one.
     *
     * @param arguments what to pass, matching the parameter list above
     * @return what it built
     * @throws RuntimeException when the call fails, on the terms {@link ReflectedMethod#invokeOn}
     *                          gives - the constructor's own throw included
     */
    Object newInstance(Object... arguments) {
        return ReflectionBypass.construct(constructor, arguments);
    }
}
