package kmlib.testfixtures.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads the constants a holder class declares, keyed by the name each was declared under.
 *
 * <p>A holder of IDs - memory keys, string keys, drop groups - exists so one literal has one home, and
 * the case guarding it wants the whole set rather than a list written out a second time: a list is free
 * to be right while the holder is wrong, and a constant added later is covered by neither. Reflection
 * is what makes the guard scale with the holder instead of alongside it.
 *
 * <p>Keyed by name, not a bare collection of values, because the interesting failures are about one
 * constant. A case that reports {@code "$kmo_site_dat" does not start with "$kmo_"} has named the
 * symptom; one that reports which constant holds it has named the line to change.
 *
 * <p>Reads the public constants only - {@code public static final} - which is the surface a holder
 * exists to publish. A private static field is machinery the holder happens to keep, not something a
 * caller can name, so sweeping it in would report a defect against a value no call site can reach.
 */
public final class DeclaredConstants {

    private DeclaredConstants() {
    }

    /**
     * Reads every public constant of one type off a holder.
     *
     * <p>Declaration order is preserved, so a report about several constants reads in the order the
     * holder lists them rather than an order the reflection happened to produce.
     *
     * @param holder       the class declaring the constants
     * @param constantType the type of constant to read; fields of any other type are skipped
     * @param <T>          the constant type, so the caller holds values rather than {@code Object}
     * @return constant name to value, in declaration order; empty where the holder declares none
     */
    public static <T> Map<String, T> readConstantsByName(Class<?> holder, Class<T> constantType) {

        var constantsByName = new LinkedHashMap<String, T>();

        for (var field : holder.getDeclaredFields()) {
            var modifiers = field.getModifiers();

            if (!Modifier.isPublic(modifiers)
                    || !Modifier.isStatic(modifiers)
                    || !Modifier.isFinal(modifiers)
                    || field.getType() != constantType) {

                continue;
            }
            constantsByName.put(field.getName(), readConstant(field, constantType));
        }
        return constantsByName;
    }

    private static <T> T readConstant(Field field, Class<T> constantType) {

        try {
            return constantType.cast(field.get(null));

        } catch (IllegalAccessException unreachable) {

            // The field was filtered to public on a class the caller already handed over, so the read
            // cannot be refused. Failing loudly beats returning a set quietly missing one entry, which
            // is the one outcome that would let a guard pass while covering less than it claims.
            throw new IllegalStateException(
                "Cannot read " + field.getDeclaringClass().getName() + "." + field.getName(),
                unreachable);
        }
    }
}
