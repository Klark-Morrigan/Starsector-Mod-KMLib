/*
 * Copyright Starficz
 * Copyright 2026 Klark Morrigan
 *
 * The method criteria are derived from org.magiclib.ReflectionUtils in MagicLib,
 * and modified in 2026: rewritten in Java, restated as a value rather than as a
 * default-argument list, and given a hierarchy walk for methods a superclass
 * declares without publishing.
 *
 * Licensed under the GNU Lesser General Public License version 3. See LICENSE at
 * the repository root, and THIRD-PARTY-NOTICES.md for what this derives from.
 */
package kmlib.starsector.ui.coreui;

import java.util.List;

/**
 * What a method has to look like to be a match, an unstated criterion meaning "any".
 *
 * <p>A method the game publishes keeps its name across builds and is asked for by it; one it does
 * not is renamed with every build, and its signature is the only durable thing left. So a caller
 * states whichever it has, and the ones it does not are left open.
 *
 * <p>The parameter types are what a caller would <em>pass</em> rather than what the method
 * declares, matched by assignment compatibility - so naming {@code Integer} finds a method taking
 * {@code Number}, and a null element matches any parameter that can hold null.
 *
 * @param name               what it is called, or null for any name
 * @param returnType         a type its answer could be handed to, or null for any
 * @param parameterCount     how many it takes, or null for any number
 * @param parameterTypes     what a caller would pass, or null to match on the count alone
 * @param searchSuperclasses whether to take in the methods a superclass declares but does not
 *                           publish, which are in neither set a shape offers on its own
 */
record MethodQuery(
    String name,
    Class<?> returnType,
    Integer parameterCount,
    List<Class<?>> parameterTypes,
    boolean searchSuperclasses) {

    /**
     * Every method a shape offers, which is where a search that narrows by signature rather than by
     * name starts.
     */
    static MethodQuery anyMethod() {
        return new MethodQuery(null, null, null, null, false);
    }

    /** The methods carrying this name, for a member the game publishes and does not rename. */
    static MethodQuery named(String name) {
        return new MethodQuery(name, null, null, null, false);
    }

    /** @return the same query, narrowed to methods whose answer fits this type */
    MethodQuery returning(Class<?> returnType) {
        return new MethodQuery(
            name, returnType, parameterCount, parameterTypes, searchSuperclasses);
    }

    /** @return the same query, run over what the shape's superclasses declare as well */
    MethodQuery searchingSuperclasses() {
        return new MethodQuery(name, returnType, parameterCount, parameterTypes, true);
    }

    /** @return the same query, narrowed to methods a caller could pass these to */
    MethodQuery taking(Class<?>... parameterTypes) {
        return takingArgumentTypes(List.of(parameterTypes));
    }

    /** @return the same query, narrowed to methods a caller could pass these to */
    MethodQuery takingArgumentTypes(List<Class<?>> parameterTypes) {
        return new MethodQuery(
            name, returnType, parameterCount, parameterTypes, searchSuperclasses);
    }

    /** @return the same query, narrowed to methods taking this many */
    MethodQuery takingCount(int parameterCount) {
        return new MethodQuery(
            name, returnType, parameterCount, parameterTypes, searchSuperclasses);
    }
}
