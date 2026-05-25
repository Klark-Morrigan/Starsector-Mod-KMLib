package kmlib.starsector.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;

import kmlib.starsector.time.StarsectorClock;

/**
 * Common base for intel items that should auto-remove themselves from
 * the {@code IntelManager} after a fixed window from creation.
 * Captures the creation timestamp at construction and runs the
 * elapsed-days check on every {@link #advanceImpl} tick.
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
public abstract class BaseExpiringIntelPlugin extends BaseIntelPlugin {

    private final long createdTimestamp;

    protected BaseExpiringIntelPlugin() {
        this.createdTimestamp = Global.getSector().getClock().getTimestamp();
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
        if (Global.getSector() == null) {
            return;
        }
        float elapsed = Global.getSector().getClock().getElapsedDaysSince(createdTimestamp);
        if (elapsed >= getExpiryDays()) {
            Global.getSector().getIntelManager().removeIntel(this);
        }
    }
}
