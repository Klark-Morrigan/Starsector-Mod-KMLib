package kmlib.testfixtures.starsector.memory;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import org.mockito.invocation.InvocationOnMock;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A memory that really stores what is written into it, on its own, with nothing behind it.
 *
 * <p>Backed by a map rather than by stubbed answers, because the two pose different things. A stubbed
 * answer states what a read would return, which suits a suite that only reads; a suite exercising a
 * writer has to give the write somewhere to land and then read it back the way the game would - so a
 * value written under one key and read under another fails here, rather than passing on two stubs that
 * happen to agree.
 *
 * <p>Separate from {@link SectorMemoryFake}, which composes it, because where the memory hangs is not
 * the same question as what it does. That fake reaches memory the way the game does, through
 * {@code Global.getSector()}, and holds a static stand-in open to do it - so a suite that already holds
 * its own stand-in for {@code Global}, or that hangs memory off a planet rather than the sector, cannot
 * use it: a second stand-in for the same type throws. Such a suite poses this instead and attaches the
 * memory itself.
 *
 * <p>What was stored is asked of the memory itself rather than of the calls made against it. A write
 * that landed is the thing under test; which method carried it there is the production code's business,
 * and a suite pinning that fails whenever an equivalent write is spelled differently. The write and
 * removal counts answer the one question the stored value cannot - that memory was left alone - since a
 * value that merely still reads the same way cannot show whether it was written again.
 */
public final class StoredMemoryFake {

    // What memory holds, and how often each key has been written to or removed. The counts are kept
    // beside the values because a write of the value already stored leaves no trace in the map.
    private final Map<String, Object> storedValues = new HashMap<>();
    private final Map<String, Integer> writeCounts = new HashMap<>();
    private final Map<String, Integer> removalCounts = new HashMap<>();

    private final MemoryAPI memory = buildStoredMemory();

    /**
     * @param key the memory key
     * @return how many times {@code key} has been removed, for the reason the write count exists
     */
    public int countRemovalsOf(String key) {
        return removalCounts.getOrDefault(key, 0);
    }

    /**
     * @param key the memory key
     * @return how many times {@code key} has been written, so a case can show a write was skipped rather
     *         than only that the value ended up unchanged
     */
    public int countWritesTo(String key) {
        return writeCounts.getOrDefault(key, 0);
    }

    /** The memory itself, for a caller that has to hand one over or hang it off something. */
    public MemoryAPI getMemory() {
        return memory;
    }

    /**
     * @param key the memory key
     * @return whether memory holds anything under {@code key}
     */
    public boolean hasStoredValue(String key) {
        return storedValues.containsKey(key);
    }

    /**
     * @param key the memory key
     * @return what memory holds under {@code key}, or null where it holds nothing
     */
    public Object readStoredValue(String key) {
        return storedValues.get(key);
    }

    /**
     * Seeds a value as though an earlier session had written it, for a case posing a save that already
     * holds one. Counted as no write, standing for what was there before the case began.
     *
     * @param key   the memory key
     * @param value what the save holds under it
     */
    public void storeValue(String key, Object value) {
        storedValues.put(key, value);
    }

    // The key an invocation was made under, every posed method taking it first. Named so the generic
    // argument read appears once rather than at each of the ten stubs below.
    private static String readKeyOf(InvocationOnMock invocation) {
        return invocation.getArgument(0);
    }

    // The map-backed memory itself. Only the value accessors are posed: memory's rule bindings, expiry
    // and entity lookups are a different subject, and a suite needing one poses it rather than finding a
    // half-answer here.
    private MemoryAPI buildStoredMemory() {

        var memoryMock = mock(MemoryAPI.class);

        when(memoryMock.contains(anyString()))
            .thenAnswer(invocation -> storedValues.containsKey(readKeyOf(invocation)));
        when(memoryMock.getKeys())
            .thenAnswer(invocation -> storedValues.keySet());
        when(memoryMock.isEmpty())
            .thenAnswer(invocation -> storedValues.isEmpty());

        when(memoryMock.get(anyString()))
            .thenAnswer(invocation -> storedValues.get(readKeyOf(invocation)));
        when(memoryMock.getString(anyString()))
            .thenAnswer(invocation -> (String) storedValues.get(readKeyOf(invocation)));

        // Each typed read answers its own type's empty value for a key holding nothing, as the game's
        // memory does - an absence is told from a stored zero by contains(), not by the read.
        when(memoryMock.getBoolean(anyString()))
            .thenAnswer(invocation -> readStoredOr(readKeyOf(invocation), false));
        when(memoryMock.getInt(anyString()))
            .thenAnswer(invocation -> readStoredOr(readKeyOf(invocation), 0));
        when(memoryMock.getLong(anyString()))
            .thenAnswer(invocation -> readStoredOr(readKeyOf(invocation), 0L));
        when(memoryMock.getFloat(anyString()))
            .thenAnswer(invocation -> readStoredOr(readKeyOf(invocation), 0f));

        doAnswer(invocation -> recordWrite(readKeyOf(invocation), invocation.getArgument(1)))
            .when(memoryMock)
            .set(anyString(), any());

        doAnswer(invocation -> recordRemoval(readKeyOf(invocation)))
            .when(memoryMock)
            .unset(anyString());

        return memoryMock;
    }

    // What is stored under the key, or the type's own empty value where nothing is. One rule for all
    // four typed reads, so they cannot come to disagree about what an absent key reads as.
    private Object readStoredOr(String key, Object emptyValue) {

        var stored = storedValues.get(key);

        return stored == null
            ? emptyValue
            : stored;
    }

    // The same for a removal, counted whether or not anything was there to remove: that a caller asked
    // for one is what the count is about.
    private Object recordRemoval(String key) {

        removalCounts.merge(key, 1, Integer::sum);

        return storedValues.remove(key);
    }

    // Stores a value under its key and counts the write, answering the way a map does so the stubbed
    // call has something to hand back whichever return type it was declared with.
    private Object recordWrite(String key, Object value) {

        writeCounts.merge(key, 1, Integer::sum);

        return storedValues.put(key, value);
    }
}
