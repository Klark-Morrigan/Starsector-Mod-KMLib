package kmlib.starsector.economy;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MonthlyReport;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;

import java.util.Optional;

/**
 * The monthly income and expenses report of a sector, read off the sector handed in.
 *
 * <p>Vanilla's own route, {@code SharedData.getData()}, reads the running sector from {@code Global}, and creates the
 * record where the sector holds none. This reads the same record under the same key, and only reads it: a sector
 * holding none answers empty.
 */
public final class MonthlyReports {

    // The key vanilla's SharedData.getData() files the record under in the sector's persistent data.
    private static final String SHARED_DATA_KEY = "core_CEFSSharedDataKey";

    private MonthlyReports() {
    }

    /**
     * The report for the month in progress, which the game totals and pays out at month end. A row written here is
     * paid with this month's report; one written after the month end lands in the next month's.
     *
     * <p>A running campaign always holds one by the time a mod's economy tick runs: vanilla's core script creates it on
     * its own tick, which the engine runs first.
     *
     * @param sector the sector whose report to read
     * @return the current report, or empty where the sector holds no shared data yet
     */
    public static Optional<MonthlyReport> findCurrentReport(SectorAPI sector) {

        if (sector.getPersistentData().get(SHARED_DATA_KEY) instanceof SharedData sharedData) {
            return Optional.of(sharedData.getCurrentReport());
        }

        return Optional.empty();
    }
}
