package kmlib.starsector.intel;

import com.fs.starfarer.api.Global;

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

    private final long createdTimestamp;

    protected BaseExpiringIntelPlugin() {
        this(new String[0]);
    }

    /**
     * Builds an expiring intel that also contributes
     * {@code extraIntelTags} to its tab placement. Forwards to
     * {@link BaseTaggedIntelPlugin} for the tag merge and captures
     * the creation timestamp locally for the expiry check.
     */
    protected BaseExpiringIntelPlugin(String... extraIntelTags) {
        super(extraIntelTags);
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
        return sector.getClock().getElapsedDaysSince(createdTimestamp) >= getExpiryDays();
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
     */
    public static <T extends BaseExpiringIntelPlugin> T findActive(Class<T> intelClass) {
        var sector = Global.getSector();
        if (sector == null) {
            return null;
        }
        var intelManager = sector.getIntelManager();
        if (intelManager == null) {
            return null;
        }
        var items = intelManager.getIntel(intelClass);
        for (var item : items) {
            var typed = intelClass.cast(item);
            if (!typed.isExpired()) {
                return typed;
            }
        }
        return null;
    }

    @Override
    protected void advanceImpl(float amount) {
        if (Global.getSector() == null) {
            return;
        }
        if (isExpired()) {
            Global.getSector().getIntelManager().removeIntel(this);
        }
    }
}
