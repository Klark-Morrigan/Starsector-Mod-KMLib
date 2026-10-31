package kmlib.memo;

import java.util.function.Function;

/**
 * Memoises the one value last derived from a key, and serves it again while the caller hands over
 * the same key instance.
 *
 * <p>For a value worked out from an object that is replaced rather than edited, such as a build's
 * draw lists or a pass's list of placements. While the same instance comes back, nothing it holds
 * has moved, so the derived value still stands; a new instance is the only signal there is. The
 * key is compared by identity rather than by equality for that reason, and because an equality
 * check over a large structure would cost more per frame than the derivation it saves.
 *
 * <p>One entry, because each holder asks after one key at a time: whatever the cursor is over, or
 * whichever build is standing. A holder asking after several keys in turn would evict its own
 * answer on every ask.
 *
 * <p>The per-frame path is one reference compare. A caller keeps it free of allocation by handing
 * over a function that captures nothing, such as a static method or a constructor reference, which
 * the JVM builds once rather than per call.
 *
 * @param <K> the key's type; compared by identity, so its own {@code equals} is never asked
 * @param <V> the derived value's type
 */
public final class IdentityMemo<K, V> {

    // Whether anything has been derived yet. Kept apart from the key so a null key is one to derive
    // for rather than one that matches the empty memo.
    private boolean isHoldingValue;

    private K cachedKey;
    private V cachedValue;

    /**
     * The value derived from this key, derived through {@code deriveValue} only when the key is not
     * the instance the held value was derived from.
     *
     * @param key         the object the value is derived from; the same instance serves the held
     *                    value, any other instance derives afresh
     * @param deriveValue derives the value from the key, called only on a miss
     * @return the derived value; the same instance while the same key comes back
     */
    public V resolveValue(
            K key,
            Function<? super K, ? extends V> deriveValue) {

        if (!isHoldingValue || key != cachedKey) {

            cachedKey = key;
            cachedValue = deriveValue.apply(key);
            isHoldingValue = true;
        }

        return cachedValue;
    }
}
