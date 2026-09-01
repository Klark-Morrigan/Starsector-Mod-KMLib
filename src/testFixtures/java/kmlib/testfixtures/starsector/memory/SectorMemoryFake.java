package kmlib.testfixtures.starsector.memory;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * A sector whose memory really stores what is written into it, reached the way the game exposes it -
 * through {@code Global.getSector()}. What anything persisting a value into the save is posed against.
 *
 * <p>Backed by a map rather than by stubbed answers, because the two pose different things. A stubbed
 * answer states what a read would return, which suits a suite that only reads; a suite exercising a
 * writer has to give the write somewhere to land and then read it back the way the game would - so a
 * value written under one key and read under another fails here, rather than passing on two stubs that
 * happen to agree.
 *
 * <p>What was stored is asked of the memory itself rather than of the calls made against it. A write
 * that landed is the thing under test; which method carried it there is the production code's business,
 * and a suite pinning that fails whenever an equivalent write is spelled differently. The write and
 * removal counts answer the one question the stored value cannot - that memory was left alone - since a
 * value that merely still reads the same way cannot show whether it was written again.
 *
 * <p>The sector can be taken away mid-case ({@link #removeSector()}), because "before the sector exists"
 * is the branch every persisted value has to answer for: the main menu and early load reach these reads
 * with no save behind them.
 *
 * <p>Holds the static stand-in for {@code Global} open for its lifetime, so the suite that opened it
 * closes it - as a try-with-resources, or from the teardown matching the setup it was made in. Left
 * open, the stand-in outlives its case and the next one reaches this sector instead of posing its own.
 */
public final class SectorMemoryFake implements AutoCloseable {

    // What memory holds, and how often each key has been written to or removed. The counts are kept
    // beside the values because a write of the value already stored leaves no trace in the map.
    private final Map<String, Object> storedValues = new HashMap<>();
    private final Map<String, Integer> writeCounts = new HashMap<>();
    private final Map<String, Integer> removalCounts = new HashMap<>();

    private final MemoryAPI memory = buildStoredMemory();

    // The stand-in for the game's static entry point, held so the sector below is what a production read
    // finds. Closing it puts the real one back.
    private final MockedStatic<Global> globalMock;

    /** Opens a sector carrying an empty memory, reached through {@code Global.getSector()}. */
    public SectorMemoryFake() {

        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getMemoryWithoutUpdate())
            .thenReturn(memory);

        globalMock = mockStatic(Global.class);
        globalMock
            .when(Global::getSector)
            .thenReturn(sectorMock);
    }

    /** The memory the posed sector carries, for a caller that has to hand one over directly. */
    public MemoryAPI getMemory() {
        return memory;
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

    /**
     * @param key the memory key
     * @return what memory holds under {@code key}, or null where it holds nothing
     */
    public Object readStoredValue(String key) {
        return storedValues.get(key);
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
     * @return how many times {@code key} has been written, so a case can show a write was skipped rather
     *         than only that the value ended up unchanged
     */
    public int countWritesTo(String key) {
        return writeCounts.getOrDefault(key, 0);
    }

    /**
     * @param key the memory key
     * @return how many times {@code key} has been removed, for the reason the write count exists
     */
    public int countRemovalsOf(String key) {
        return removalCounts.getOrDefault(key, 0);
    }

    /**
     * Takes the sector away, so every read and write behind it meets the state before a save exists.
     * What was stored is kept rather than dropped, since the case is posing a missing sector and not a
     * wiped save.
     */
    public void removeSector() {
        globalMock
            .when(Global::getSector)
            .thenReturn(null);
    }

    @Override
    public void close() {
        globalMock.close();
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

    // Stores a value under its key and counts the write, answering the way a map does so the stubbed
    // call has something to hand back whichever return type it was declared with.
    private Object recordWrite(String key, Object value) {

        writeCounts.merge(key, 1, Integer::sum);

        return storedValues.put(key, value);
    }

    // The same for a removal, counted whether or not anything was there to remove: that a caller asked
    // for one is what the count is about.
    private Object recordRemoval(String key) {

        removalCounts.merge(key, 1, Integer::sum);

        return storedValues.remove(key);
    }

    // What is stored under the key, or the type's own empty value where nothing is. One rule for all
    // four typed reads, so they cannot come to disagree about what an absent key reads as.
    private Object readStoredOr(String key, Object emptyValue) {

        var stored = storedValues.get(key);

        return stored == null
            ? emptyValue
            : stored;
    }

    // The key an invocation was made under, every posed method taking it first. Named so the generic
    // argument read appears once rather than at each of the ten stubs above.
    private static String readKeyOf(InvocationOnMock invocation) {
        return invocation.getArgument(0);
    }
}
