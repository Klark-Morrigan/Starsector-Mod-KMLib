package kmlib.starsector.ui.widgets.lists;

import com.fs.starfarer.api.campaign.SectorAPI;

import java.lang.ref.WeakReference;
import java.util.function.Supplier;

/**
 * Memoises one picker's resolved value - typically its list of selectable items - so a per-frame
 * body build reads a cache instead of re-resolving it. A panel body is built twice a frame (the
 * render and the hit-test passes) and a picker list is typically a full pass over whatever the
 * consumer scores its items from, so without a memo that pass would run several times a frame. The
 * caller says which revision the value depends on; it is rebuilt only when that revision moves and
 * the per-frame path is otherwise an int compare.
 *
 * <p>Keyed on the sector identity as well, which is the part worth having once rather than in every
 * consumer: a save reloaded in the same session is a fresh sector whose revision may well match the
 * last, so without it the picker serves the previous save's items. The held sector reference is weak,
 * so a cached sector never outlives its unload.
 *
 * <p>What belongs in the revision is the caller's judgement, and it is the whole of the invalidation
 * contract: whatever the resolved value reads and can change without moving the revision will lag
 * until something else forces a recompute. Which item is spotlighted deliberately does not belong in
 * it - a picker list is which items are selectable, not which one is lit - so a pick or a clear moves
 * the lit row without invalidating the list.
 *
 * @param <T> the memoised value's type
 */
public final class RevisionMemo<T> {

    // The sector the memo was built against, held weakly so a cached sector never outlives its
    // unload. Starts empty so the first resolve always recomputes.
    private WeakReference<SectorAPI> cachedSector = new WeakReference<>(null);
    private String cachedScopeId;
    private int cachedRevision;
    private T cachedValue;

    /**
     * The memoised value, rebuilt through {@code resolveFreshValue} only when the sector, the scope,
     * or the revision has changed since the last call, and served from the memo otherwise.
     *
     * @param sector            the sector the value is read against; part of the key, never
     *                          dereferenced here, so a null sector is a key like any other
     * @param scopeId           the scope the value belongs to, so a switch between scopes recomputes
     *                          rather than serving the previous scope's value; opaque here, since
     *                          what partitions a consumer's pickers is the consumer's own
     * @param revision          the caller's revision of everything the value depends on; a moved
     *                          number is what forces the rebuild
     * @param resolveFreshValue resolves the value from scratch, called only on a miss
     * @return the memoised value; the same instance while nothing the key holds moves
     */
    public T resolveValue(
            SectorAPI sector,
            String scopeId,
            int revision,
            Supplier<T> resolveFreshValue) {

        if (sector != cachedSector.get()
                || !scopeId.equals(cachedScopeId)
                || revision != cachedRevision) {

            cachedSector = new WeakReference<>(sector);
            cachedScopeId = scopeId;
            cachedRevision = revision;
            cachedValue = resolveFreshValue.get();
        }
        return cachedValue;
    }
}
