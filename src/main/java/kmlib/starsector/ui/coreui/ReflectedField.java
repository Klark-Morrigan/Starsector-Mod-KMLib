/*
 * Copyright Starficz
 * Copyright 2026 Klark Morrigan
 *
 * Derived from ReflectedField in org.magiclib.ReflectionUtils, and modified in
 * 2026: rewritten in Java, and describing and opening the member once on the way
 * in rather than at each ask.
 *
 * Licensed under the GNU Lesser General Public License version 3. See LICENSE at
 * the repository root, and THIRD-PARTY-NOTICES.md for what this derives from.
 */
package kmlib.starsector.ui.coreui;

/**
 * One field a shape holds, readable and writable without its own type ever being named.
 *
 * <p>Describes itself once, on the way in. What a field is called and is declared as is fixed,
 * while each read of it costs a call through the bypass - and a search runs across every field a
 * shape declares.
 *
 * <p>Opened on the way in too, for the same reason it is held at all: the fields worth reaching are
 * the ones a shape keeps to itself.
 */
final class ReflectedField {

    private final Object field;
    private final String name;
    private final Class<?> type;

    ReflectedField(Object field) {

        this.field = field;
        this.name = ReflectionBypass.readFieldName(field);
        this.type = ReflectionBypass.readFieldType(field);

        ReflectionBypass.makeFieldAccessible(field);
    }

    /**
     * @return its own name, which on an obfuscated shape is regenerated with each game build
     */
    String getName() {
        return name;
    }

    /**
     * @return the type it is declared as, which is what a search matches on - not the type of
     *         whatever it happens to hold, which a declared {@code Object} hides entirely
     */
    Class<?> getType() {
        return type;
    }

    /**
     * Reads what it holds.
     *
     * @param instance the object to read it off, or null for a static field
     * @return its value
     * @throws RuntimeException when the read fails. It arrives undeclared and not necessarily as a
     *                          {@link RuntimeException}, so a caller guarding this has to catch
     *                          {@link Throwable}
     */
    Object readFrom(Object instance) {
        return ReflectionBypass.readFieldValue(field, instance);
    }

    /**
     * Writes what it holds.
     *
     * <p>Nothing checks the value against {@link #getType()} first: the write itself refuses one
     * that does not fit, and refusing earlier would only move the same failure.
     *
     * @param instance the object to write it on, or null for a static field
     * @param value    what to put in it
     * @throws RuntimeException when the write fails, on the terms {@link #readFrom} gives
     */
    void writeTo(Object instance, Object value) {
        ReflectionBypass.writeFieldValue(field, instance, value);
    }
}
