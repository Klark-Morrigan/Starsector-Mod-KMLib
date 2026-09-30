package kmlib.starsector.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.starsector.time.CampaignCountdown;
import kmlib.starsector.time.StarsectorClock;

/**
 * Common base for intel items that should auto-remove themselves from
 * the {@code IntelManager} after a fixed window from creation.
 * Captures the creation timestamp at construction and runs the
 * elapsed-days check on every {@link #advanceImpl} tick.
 *
 * <p>Extends {@link BaseTaggedIntelPlugin} so an expiring intel can
 * declare its intel-tab tags via the constructor without a separate
 * {@code getIntelTags} override; the no-arg constructor preserves the
 * untagged-expiring case for callers that pin to a vanilla tab.</p>
 *
 * <p>Subclasses override {@link #getExpiryDays()} to change the
 * window. The default is one Starsector month
 * ({@link StarsectorClock#DAYS_PER_MONTH}), because that is the
 * cadence at which most month-end-event intel needs to clear before
 * the next month's notice lands.</p>
 *
 * <p>Subclasses that need per-tick work alongside the expiry check
 * override {@link #advanceImpl} and call {@code super.advanceImpl}
 * to inherit the removal logic.</p>
 */
public abstract class BaseExpiringIntelPlugin extends BaseTaggedIntelPlugin {

    private static final float NO_SLACK_DAYS = 0f;

    private final long createdTimestamp;

    protected BaseExpiringIntelPlugin() {
        this(new String[0]);
    }

    /**
     * Builds an expiring intel that also contributes
     * {@code extraIntelTags} to its tab placement, stamped off the
     * running sector's clock. See
     * {@link #BaseExpiringIntelPlugin(CampaignClockAPI, String...)}.
     */
    protected BaseExpiringIntelPlugin(String... extraIntelTags) {
        this(Global.getSector().getClock(), extraIntelTags);
    }

    /**
     * Builds an expiring intel whose window opens at {@code clock}'s
     * current time, for code handed its sector rather than reading the
     * running one. Forwards to {@link BaseTaggedIntelPlugin} for the
     * tag merge and captures the creation timestamp locally for the
     * expiry check.
     *
     * @param clock          the campaign clock the window opens on
     * @param extraIntelTags tab tags added to the intel's placement
     */
    protected BaseExpiringIntelPlugin(CampaignClockAPI clock, String... extraIntelTags) {

        super(extraIntelTags);
        this.createdTimestamp = clock.getTimestamp();
    }

    /**
     * {@code true} once the elapsed in-game days since construction
     * has reached {@link #getExpiryDays()}. The base class's
     * {@link #advanceImpl} also removes the intel from the
     * {@code IntelManager} once this flips, but synchronous lookup
     * helpers like {@link #findActive(Class)} need a direct predicate
     * that asks "is this item still within its window?" because the
     * {@code IntelManager} list may still hold a just-expired item
     * for a few frames before the next {@code advance} prunes it.
     *
     * <p>Returns {@code false} when the sector is unavailable (early
     * load / teardown) so callers can treat a missing clock as "not
     * yet expired" rather than as a hard error.</p>
     */
    public final boolean isExpired() {

        var sector = Global.getSector();
        if (sector == null) {
            return false;
        }
        return isExpired(sector.getClock());
    }

    /**
     * {@link #isExpired()} against a clock the caller already holds,
     * for code handed its sector rather than reading the running one.
     *
     * @param clock the campaign clock to measure the window against
     * @return whether the window has run out on {@code clock}
     */
    public final boolean isExpired(CampaignClockAPI clock) {

        // No slack: an intel's window is a player-facing promise with no frame-jitter completion to absorb.
        return new CampaignCountdown(createdTimestamp, getExpiryDays(), NO_SLACK_DAYS)
            .isComplete(clock);
    }

    /**
     * {@link #findActive(SectorAPI, Class)} over the running sector.
     *
     * @param intelClass the intel type to look up
     * @param <T>        the intel type
     * @return the first intel of that type still within its window, or {@code null}
     */
    public static <T extends BaseExpiringIntelPlugin> T findActive(Class<T> intelClass) {
        return findActive(Global.getSector(), intelClass);
    }

    /**
     * Returns the first non-expired intel of type {@code intelClass}
     * registered with the sector's {@code IntelManager}, or
     * {@code null} when none exists.
     *
     * <p>Most expiring-intel mechanics keep a single live item per
     * window and re-register it on the first event after expiry, so
     * "find the current window's item" is the dominant lookup
     * shape. Routing it through this helper avoids each caller
     * re-implementing the empty-list / expired-head dance and
     * guarantees they all use the same definition of "still
     * within window" as {@link #isExpired()} above.</p>
     *
     * <p>Returns {@code null} when the sector or its
     * {@code IntelManager} is unavailable so callers can use the
     * same null branch they already need for the empty-list case.</p>
     *
     * @param sector     the sector whose intel manager and clock to read, possibly {@code null}
     * @param intelClass the intel type to look up
     * @param <T>        the intel type
     * @return the first intel of that type still within its window, or {@code null}
     */
    public static <T extends BaseExpiringIntelPlugin> T findActive(SectorAPI sector, Class<T> intelClass) {

        if (sector == null) {
            return null;
        }

        var intelManager = sector.getIntelManager();
        if (intelManager == null) {
            return null;
        }

        var clock = sector.getClock();
        var items = intelManager.getIntel(intelClass);

        for (var item : items) {

            var typed = intelClass.cast(item);
            if (!typed.isExpired(clock)) {
                return typed;
            }
        }
        return null;
    }

    /**
     * Lifetime of this intel from construction, in in-game days.
     * Defaults to {@link StarsectorClock#DAYS_PER_MONTH} so the
     * next month's notice has the prior one cleared away by the
     * time it lands - the common case for month-end transient
     * intel. Override for any other cadence.
     */
    protected float getExpiryDays() {
        return StarsectorClock.DAYS_PER_MONTH;
    }

    /**
     * Timestamp captured at construction. Exposed for subclasses
     * that need to render "created N days ago" or similar; the
     * expiry check uses it internally and does not require
     * subclass involvement.
     */
    protected final long getCreatedTimestamp() {
        return createdTimestamp;
    }

    @Override
    protected void advanceImpl(float amount) {

        // An engine callback handed no sector, so it reads the running one once. Early load and teardown leave the
        // sector or its intel manager missing, the same absences findActive answers as "nothing to act on".
        var sector = Global.getSector();
        if (sector == null) {
            return;
        }

        var intelManager = sector.getIntelManager();
        if (intelManager == null) {
            return;
        }

        if (isExpired(sector.getClock())) {
            intelManager.removeIntel(this);
        }
    }
}
