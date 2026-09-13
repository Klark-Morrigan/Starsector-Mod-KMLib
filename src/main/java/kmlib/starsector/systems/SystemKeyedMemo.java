package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.StarSystemAPI;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * One value per star system, worked out on the first ask and remembered for the rest of a pass.
 *
 * <p>Two rules every memo keyed on a system has to state, held here so that none states them for
 * itself. It is keyed on the whole {@link SystemKey} rather than the id, because an id is not
 * unique: a sector holding two systems under one id would otherwise pool them into a single entry
 * and hand the first system's value to the second. And a system stating no arm at all - no id and
 * neither entity - has the blank key, which equals every other blank one, so it is resolved afresh
 * on every ask rather than pooled: the repeat costs what a later ask would have saved, which is
 * the honest price of a system the sector states nothing about. A system carrying any one arm is
 * remembered like the rest.
 *
 * <p>Built for one pass and discarded with it: the values are a snapshot, and a memo outliving its
 * pass would keep answering off a sector that has since moved on. Not safe for concurrent use, a
 * pass being one thread's work.
 *
 * @param <V> the value remembered per system
 */
public final class SystemKeyedMemo<V> {

    private final Map<SystemKey, V> valueBySystemKey = new HashMap<>();

    /**
     * The value for {@code system}: what {@code resolver} answered the first time this system was
     * asked about, or what it answers now if this is that time.
     *
     * <p>The system is required rather than answered for. A null system has no key to remember it
     * by, and what a read answers for one is the read's own contract - an empty set, an empty
     * value - which this cannot state for it.
     *
     * @param system   the system to answer for
     * @param resolver what works the value out on a miss, handed the system it is for
     * @return the system's value, remembered from the first ask wherever the key allows it
     */
    public V readValueFor(StarSystemAPI system, Function<StarSystemAPI, V> resolver) {

        Objects.requireNonNull(system, "system");

        var key = SystemKey.readKeyOf(system);

        if (!key.hasStatedArm()) {
            // Nothing to tell this system from another, so nothing to remember it under.
            return resolver.apply(system);
        }
        return valueBySystemKey.computeIfAbsent(key, memoKey -> resolver.apply(system));
    }
}
