package kmlib.starsector.time;

import com.fs.starfarer.api.campaign.CampaignClockAPI;

/**
 * One month of the campaign calendar, as the cycle and the month within it.
 *
 * <p>What a once-a-month job keys its "already ran" record by: the job reads the month it is in, compares it with the
 * month it last ran in, and records the new one when it runs. Held here so every such record spells the month one
 * way, since the key is written into saves - two spellings of one month would each read the other's record as a
 * month never run.
 *
 * <p>The clock is handed to {@link #readCurrentMonth} rather than read from the running sector, so the month is plain
 * numbers and names no sector.
 *
 * @param cycle the campaign cycle, as the clock counts it
 * @param month the month within that cycle, as the clock counts it
 */
public record CampaignMonth(int cycle, int month) {

    private static final String KEY_SEPARATOR = "-";

    /**
     * @param clock the running sector's campaign clock
     * @return the month that clock is in
     */
    public static CampaignMonth readCurrentMonth(CampaignClockAPI clock) {
        return new CampaignMonth(clock.getCycle(), clock.getMonth());
    }

    /**
     * The month as the {@code <cycle>-<month>} string a save records it under. Stable once shipped: a record written
     * under one spelling is not found under another.
     *
     * @return the key, such as {@code "206-1"}
     */
    public String formatKey() {
        return cycle + KEY_SEPARATOR + month;
    }
}
