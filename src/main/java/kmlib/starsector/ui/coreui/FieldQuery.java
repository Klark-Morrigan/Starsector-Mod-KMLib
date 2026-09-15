/*
 * Copyright Starficz
 * Copyright 2026 Klark Morrigan
 *
 * The field criteria are derived from org.magiclib.ReflectionUtils in MagicLib,
 * and modified in 2026: rewritten in Java and restated as a value rather than as
 * a default-argument list.
 *
 * Licensed under the GNU Lesser General Public License version 3. See LICENSE at
 * the repository root, and THIRD-PARTY-NOTICES.md for what this derives from.
 */
package kmlib.starsector.ui.coreui;

/**
 * What a field has to look like to be a match, an unstated criterion meaning "any".
 *
 * <p>A field the game publishes keeps its name across builds and is asked for by it; one it does
 * not is renamed with every build, and what it is declared as is the only durable thing left. So a
 * caller states whichever it has, and the ones it does not are left open.
 *
 * <p>Read the two type criteria as opposite directions, because a caller wanting one almost never
 * wants the other. {@link #assignableTo} asks whether the field's value could be handed somewhere,
 * which is the question a read is aimed by; {@link #accepting} asks whether a value could be put in
 * the field, which is the question a write is aimed by.
 *
 * @param name               what it is called, or null for any name
 * @param exactType          the type it is declared as, matched exactly, or null for any
 * @param assignableTo       a type its value could be handed to, or null for any
 * @param accepting          a type whose values could be put in it, or null for any
 * @param searchSuperclasses whether the fields a superclass declares count too
 */
record FieldQuery(
    String name,
    Class<?> exactType,
    Class<?> assignableTo,
    Class<?> accepting,
    boolean searchSuperclasses) {

    /**
     * Every field a shape declares, which is where a search that narrows by type rather than by
     * name starts.
     */
    static FieldQuery anyField() {
        return new FieldQuery(null, null, null, null, false);
    }

    /** The fields carrying this name, for a member the game publishes and does not rename. */
    static FieldQuery named(String name) {
        return new FieldQuery(name, null, null, null, false);
    }

    /** @return the same query, narrowed to fields a value of this type could be put in */
    FieldQuery accepting(Class<?> accepting) {
        return new FieldQuery(name, exactType, assignableTo, accepting, searchSuperclasses);
    }

    /** @return the same query, narrowed to fields whose value could be handed to this type */
    FieldQuery assignableTo(Class<?> assignableTo) {
        return new FieldQuery(name, exactType, assignableTo, accepting, searchSuperclasses);
    }

    /** @return the same query, narrowed to fields declared as exactly this type */
    FieldQuery ofExactType(Class<?> exactType) {
        return new FieldQuery(name, exactType, assignableTo, accepting, searchSuperclasses);
    }

    /** @return the same query, run over what the shape's superclasses declare as well */
    FieldQuery searchingSuperclasses() {
        return new FieldQuery(name, exactType, assignableTo, accepting, true);
    }

    /** @return whether anything about the field's type is being matched on */
    boolean isTypeConstrained() {
        return exactType != null || assignableTo != null || accepting != null;
    }
}
