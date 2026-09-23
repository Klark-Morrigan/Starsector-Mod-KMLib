package kmlib.testfixtures.starsector.memory;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import kmlib.testfixtures.starsector.StubbedGlobalLogger;

import org.mockito.MockedStatic;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * A sector whose memory really stores what is written into it, reached the way the game exposes it -
 * through {@code Global.getSector()}. What anything persisting a value into the save is posed against.
 *
 * <p>The memory itself is {@link StoredMemoryFake}, and every reading below is that fake's. What this
 * one adds is where the memory hangs: on a sector, behind the game's static entry point. A suite that
 * already holds its own stand-in for {@code Global}, or that hangs memory off a planet rather than the
 * sector, poses the stored memory directly instead - a second stand-in for the same type throws.
 *
 * <p>The sector can be taken away mid-case ({@link #removeSector()}), because "before the sector exists"
 * is the branch every persisted value has to answer for: the main menu and early load reach these reads
 * with no save behind them.
 *
 * <p>Holds the static stand-in for {@code Global} open for its lifetime, so the suite that opened it
 * closes it - as a try-with-resources, or from the teardown matching the setup it was made in. Left
 * open, the stand-in outlives its case and the next one reaches this sector instead of posing its own.
 *
 * <p>That stand-in answers the logger as well as the sector, because a static logger field resolved
 * while it is open keeps its answer for the rest of the JVM - see the constructor.
 */
public final class SectorMemoryFake implements AutoCloseable {

    private final StoredMemoryFake storedMemory = new StoredMemoryFake();

    // The stand-in for the game's static entry point, held so the sector below is what a production read
    // finds. Closing it puts the real one back.
    private final MockedStatic<Global> globalMock;

    /** Opens a sector carrying an empty memory, reached through {@code Global.getSector()}. */
    public SectorMemoryFake() {

        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getMemoryWithoutUpdate())
            .thenReturn(storedMemory.getMemory());

        globalMock = mockStatic(Global.class);
        globalMock
            .when(Global::getSector)
            .thenReturn(sectorMock);

        // Not optional: a class whose static LOG field is first resolved while this stand-in is
        // open keeps whatever it was handed for the rest of the JVM. StubbedGlobalLogger says why.
        StubbedGlobalLogger.answerLoggersOn(globalMock);
    }

    @Override
    public void close() {
        globalMock.close();
    }

    /**
     * @param key the memory key
     * @return how many times {@code key} has been removed, for the reason the write count exists
     */
    public int countRemovalsOf(String key) {
        return storedMemory.countRemovalsOf(key);
    }

    /**
     * @param key the memory key
     * @return how many times {@code key} has been written, so a case can show a write was skipped rather
     *         than only that the value ended up unchanged
     */
    public int countWritesTo(String key) {
        return storedMemory.countWritesTo(key);
    }

    /** The memory the posed sector carries, for a caller that has to hand one over directly. */
    public MemoryAPI getMemory() {
        return storedMemory.getMemory();
    }

    /**
     * @param key the memory key
     * @return whether memory holds anything under {@code key}
     */
    public boolean hasStoredValue(String key) {
        return storedMemory.hasStoredValue(key);
    }

    /**
     * @param key the memory key
     * @return what memory holds under {@code key}, or null where it holds nothing
     */
    public Object readStoredValue(String key) {
        return storedMemory.readStoredValue(key);
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

    /**
     * Seeds a value as though an earlier session had written it, for a case posing a save that already
     * holds one. Counted as no write, standing for what was there before the case began.
     *
     * @param key   the memory key
     * @param value what the save holds under it
     */
    public void storeValue(String key, Object value) {
        storedMemory.storeValue(key, value);
    }
}
