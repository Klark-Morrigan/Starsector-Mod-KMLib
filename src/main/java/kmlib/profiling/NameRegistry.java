package kmlib.profiling;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * One instance per name, for the values profiling identifies by name.
 *
 * <p>Identity is the point: a value resolved twice from one name is one object,
 * so a per-frame path compares references instead of hashing a string, and two
 * call sites spelling one name land on one row rather than on two rows spelled
 * alike.
 *
 * <p>Registration runs from static initialisers, and which thread runs one is
 * whichever first touched the class holding the constant - the single place in
 * profiling that cannot assume the game thread, hence a concurrent table.
 *
 * @param <T> the registered value
 */
final class NameRegistry<T> {

    private final Map<String, T> valuesByName = new ConcurrentHashMap<>();
    private final Function<String, T> createValue;

    /**
     * @param createValue makes the value a name stands for, the first time that
     *                    name is asked for
     */
    NameRegistry(Function<String, T> createValue) {
        this.createValue = createValue;
    }

    /**
     * @param name what the value is called
     * @return the one value registered under that name
     */
    T resolveByName(String name) {
        return valuesByName.computeIfAbsent(Objects.requireNonNull(name, "name"), createValue);
    }
}
