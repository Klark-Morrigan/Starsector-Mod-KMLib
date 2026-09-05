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
 * <p>What makes a value arrives with the name rather than being held here,
 * because a name is not always all a value is registered with - a section
 * declaring the phases of its loop takes those too - and a factory fixed when
 * the registry was built could not carry them.
 *
 * <p>Registration runs from static initialisers, and which thread runs one is
 * whichever first touched the class holding the constant - the single place in
 * profiling that cannot assume the game thread, hence a concurrent table.
 *
 * @param <T> the registered value
 */
final class NameRegistry<T> {

    private final Map<String, T> valuesByName = new ConcurrentHashMap<>();

    /**
     * @param name        what the value is called
     * @param createValue makes the value that name stands for, the first time
     *                    the name is asked for
     * @return the one value registered under that name
     */
    T resolveByName(String name, Function<String, T> createValue) {
        return valuesByName.computeIfAbsent(Objects.requireNonNull(name, "name"), createValue);
    }
}
