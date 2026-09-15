/*
 * Copyright Starficz
 * Copyright 2026 Klark Morrigan
 *
 * The constructor criteria are derived from org.magiclib.ReflectionUtils in
 * MagicLib, and modified in 2026: rewritten in Java and restated as a value
 * rather than as a default-argument list.
 *
 * Licensed under the GNU Lesser General Public License version 3. See LICENSE at
 * the repository root, and THIRD-PARTY-NOTICES.md for what this derives from.
 */
package kmlib.starsector.ui.coreui;

import java.util.List;

/**
 * What a constructor has to look like to be a match, an unstated criterion meaning "any".
 *
 * <p>Carries no name, a constructor having none of its own: what it takes is the only thing telling
 * one apart from another, which is why every criterion here is about its parameters.
 *
 * <p>The parameter types are what a caller would <em>pass</em>, matched as {@link MethodQuery}'s
 * are.
 *
 * @param parameterCount how many it takes, or null for any number
 * @param parameterTypes what a caller would pass, or null to match on the count alone
 */
record ConstructorQuery(
    Integer parameterCount,
    List<Class<?>> parameterTypes) {

    /** Every constructor a shape declares, which is where any search over them starts. */
    static ConstructorQuery anyConstructor() {
        return new ConstructorQuery(null, null);
    }

    /** @return the same query, narrowed to constructors a caller could pass these to */
    ConstructorQuery taking(Class<?>... parameterTypes) {
        return takingArgumentTypes(List.of(parameterTypes));
    }

    /** @return the same query, narrowed to constructors a caller could pass these to */
    ConstructorQuery takingArgumentTypes(List<Class<?>> parameterTypes) {
        return new ConstructorQuery(parameterCount, parameterTypes);
    }

    /** @return the same query, narrowed to constructors taking this many */
    ConstructorQuery takingCount(int parameterCount) {
        return new ConstructorQuery(parameterCount, parameterTypes);
    }
}
