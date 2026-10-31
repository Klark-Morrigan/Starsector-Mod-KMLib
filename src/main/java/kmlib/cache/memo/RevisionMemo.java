package kmlib.cache.memo;

import java.util.function.Supplier;

/**
 * Memoises one value resolved from live state, and serves it again while the caller hands over the
 * same scope and revision.
 *
 * <p>For a value read every frame from state that changes far less often, such as a picker's list
 * of selectable items. A panel body is built twice a frame, for the render and the hit-test passes,
 * and the list is a full pass over whatever its items are scored from, so without a memo that pass
 * would run several times a frame. The value is rebuilt only when the revision moves, and the
 * per-frame path is otherwise an int compare.
 *
 * <p>For a value derived from an object that is replaced rather than edited, where the object
 * itself is the key, {@link IdentityMemo} is the shape.
 *
 * <p>What belongs in the revision is the caller's judgement, and it is the whole of the invalidation
 * contract: whatever the resolved value reads and can change without moving the revision will lag
 * until something else forces a recompute. For a picker, which item is spotlighted deliberately does
 * not belong in it - the list is which items are selectable, not which one is lit - so a pick or a
 * clear moves the lit row without invalidating the list.
 *
 * <p>How long a memo lives is the holder's business rather than the memo's. It keys on nothing but
 * the scope and the revision, so a consumer whose value belongs to something with a lifetime of its
 * own - one game, one sector - keeps a memo per such thing and lets it go when that does;
 * {@link #discardValue} is what a holder being released calls so nothing of its value is served
 * afterwards under a revision that had no reason to move.
 *
 * @param <T> the memoised value's type
 */
public final class RevisionMemo<T> {

    // The key the held value was resolved under. Starts null, which no scope ID matches, so the
    // first resolve always recomputes.
    private String cachedScopeId;
    private int cachedRevision;
    private T cachedValue;

    /**
     * Drops the memoised value, so the next resolve walks afresh.
     *
     * <p>What a consumer calls when whatever the value belonged to is released. Nothing about the
     * key would say so on its own - a released holder's revisions simply stop moving - so serving
     * the value again is exactly what this prevents.
     */
    public void discardValue() {

        // The whole key, not just the value: a memo holding half of one reads as though it still
        // remembered what it was resolved for.
        cachedScopeId = null;
        cachedRevision = 0;
        cachedValue = null;
    }

    /**
     * The memoised value, rebuilt through {@code resolveFreshValue} only when the scope or the
     * revision has changed since the last call, and served from the memo otherwise.
     *
     * @param scopeId           the scope the value belongs to, so a switch between scopes recomputes
     *                          rather than serving the previous scope's value; never null, since
     *                          null is what an empty memo holds; opaque otherwise, since what
     *                          partitions a consumer's values is the consumer's own
     * @param revision          the caller's revision of everything the value depends on; a moved
     *                          number is what forces the rebuild
     * @param resolveFreshValue resolves the value from scratch, called only on a miss
     * @return the memoised value; the same instance while nothing the key holds moves
     */
    public T resolveValue(
            String scopeId,
            int revision,
            Supplier<? extends T> resolveFreshValue) {

        if (!scopeId.equals(cachedScopeId) || revision != cachedRevision) {

            cachedScopeId = scopeId;
            cachedRevision = revision;
            cachedValue = resolveFreshValue.get();
        }

        return cachedValue;
    }
}
