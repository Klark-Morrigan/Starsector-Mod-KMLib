package kmlib.starsector.memory;

/**
 * What a stored value's key is partitioned by: whatever turns one value's base key into the key of the
 * single slot being read or written.
 *
 * <p>A value partitioned along one axis is held once per point on it rather than once per save, and the
 * axis is the consumer's to declare - one segment, or several composed in an order only it knows. A
 * holder takes the address its own state is partitioned by and never composes a key itself, so the
 * segments have one spelling, no holder can order them differently, and none can leave one off and
 * quietly share a slot with another.
 *
 * <p>Named for the address rather than for anything that answers it, because a holder has no business
 * knowing which partitioning it was handed: what it needs is the key, and what it must not do is build
 * one.
 */
public interface MemoryKeyAddress {

    /**
     * @param baseKey the preference's own sector-memory key, carrying none of this address's segments
     * @return the key that preference is stored at for this address
     */
    String resolveKeyFor(String baseKey);
}
