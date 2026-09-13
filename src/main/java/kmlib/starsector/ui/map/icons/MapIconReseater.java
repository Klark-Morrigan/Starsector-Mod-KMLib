package kmlib.starsector.ui.map.icons;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import kmlib.starsector.ui.map.MapIconLayering;

import org.apache.log4j.Logger;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Moves one entity's icon to the end of the map widget's draw order whenever the widget has seeded
 * it under the nebulae, so the map's own fog no longer paints over it. When that is owed is
 * {@link MapIconReseatDecision}'s, which states why the move works at all and why it is keyed on the
 * icon's placement rather than on a map having opened; this is the two engine calls that carry it
 * out and the guard that keeps a fault in either out of the campaign's frame.
 *
 * <p>Everything it decides from is a port: which entity, which maps matter, and where that entity's
 * icon currently sits. The first two only a caller can answer. The third has an answer in this
 * library - {@code MapIconLayeringProbe} reads it off the live widget - but it is wired in rather
 * than reached for, because this package writes to the map's draw order and reads nothing, which is
 * what keeps it independent of the packages that only read. Each is asked again at every step rather
 * than cached at construction, so an entity a save load replaced is the one moved rather than a
 * stale instance nothing draws.
 *
 * <p>Every move it makes is logged at DEBUG on this library's own logger, not the calling mod's, so
 * following a layering problem means turning KMLib's verbosity up rather than the mod's. A move is
 * rare - one per re-seeding of the icon map - so the lines stay readable beside a mod's own
 * icon-order trace.
 *
 * <p>Runs while paused. Opening a map holds the campaign paused for as long as it is up, which is
 * the whole window this works in - a script standing down while paused would never advance here.
 *
 * <p>The entity is out of its location for exactly one advance, which is worth knowing for anything
 * that reads the location on a timer: a save written inside that window does not hold it, so
 * whatever put the entity there is what has to put it back on the next load.
 */
public final class MapIconReseater implements EveryFrameScript {

    private static final Logger LOG = Global.getLogger(MapIconReseater.class);

    private final BooleanSupplier isMapShowing;
    private final Supplier<SectorEntityToken> findEntityToReseat;
    private final Supplier<MapIconLayering> readIconLayering;
    private final MapIconReseatDecision reseatDecision = new MapIconReseatDecision();

    // The entity taken out, with the location it came from, held for the single advance it spends
    // out. One field rather than two so the pair cannot part company: an entity without its former
    // location has nowhere to go back to, and neither half is any use alone.
    private DetachedMapIcon detachedMapIcon;

    // One-shot guard: this runs every frame, so a recurring fault would flood the log. The first
    // failure is recorded, the rest silenced.
    private boolean hasLoggedReseatError;

    // The same guard for the stand-down, which is a standing state rather than an event: without it
    // every advance for the rest of the session would report it again.
    private boolean hasLoggedStandDown;

    /**
     * @param isMapShowing whether a map whose icon order matters is on screen; it scopes the move to
     *        the maps the caller cares about rather than triggering it
     * @param findEntityToReseat the entity to move, or null when its location holds none
     * @param readIconLayering where that entity's icon currently sits; {@code MapIconLayeringProbe}
     *        answers it from the live widget, and a caller wires that in rather than this reaching
     *        for it - this package writes to the map's draw order and reads nothing, which is what
     *        keeps it independent of the probes that only read
     */
    public MapIconReseater(
            BooleanSupplier isMapShowing,
            Supplier<SectorEntityToken> findEntityToReseat,
            Supplier<MapIconLayering> readIconLayering) {

        this.isMapShowing = isMapShowing;
        this.findEntityToReseat = findEntityToReseat;
        this.readIconLayering = readIconLayering;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        // Guarded so an uncaught fault - a location that refuses the entity, a supplier that throws
        // - is recorded rather than thrown into the campaign's frame, and does not stop the script
        // from trying again on the next map open.
        try {
            applyReseatAction();
        } catch (RuntimeException exception) {
            if (!hasLoggedReseatError) {
                hasLoggedReseatError = true;
                LOG.error("Could not reseat a map icon; its layering is left as the widget seeded "
                    + "it this session.", exception);
            }
        }
    }

    private void applyReseatAction() {
        var action = reseatDecision.decideReseatAction(
            isMapShowing.getAsBoolean(),
            readIconLayering,
            () -> findEntityToReseat.get() != null);

        switch (action) {
            case REMOVE -> detachMapIcon();
            case ADD -> attachMapIcon();
            case NONE -> reportStandingDownOnce();
        }
    }

    private void detachMapIcon() {
        var entity = findEntityToReseat.get();
        // The location is read before the removal and kept, because an entity taken out of one no
        // longer names it - and the put-back has to reach the same location this took it from
        // rather than wherever the caller's supplier would point afterwards.
        var location = entity == null ? null : entity.getContainingLocation();
        if (location == null) {
            return;
        }
        detachedMapIcon = new DetachedMapIcon(entity, location);
        location.removeEntity(entity);

        // One line per move, and moves are rare - one per re-seeding of the widget's icon map. A
        // run of them says the lift is being attempted and not taking, which is the state the
        // stand-down below ends and the one worth seeing it end.
        LOG.debug("Map icon reseat: detached " + describeEntity(entity)
            + " from " + location.getName()
            + " to lift its icon past the map's nebulae");
    }

    private void attachMapIcon() {
        if (detachedMapIcon == null) {
            return;
        }
        detachedMapIcon.location().addEntity(detachedMapIcon.entity());
        LOG.debug("Map icon reseat: reattached " + describeEntity(detachedMapIcon.entity())
            + "; its icon re-enters at the tail on the next frame that draws a map");
        detachedMapIcon = null;
    }

    // Says once that the lift has been abandoned, which is the only state a player could otherwise
    // only diagnose from the picture. WARN rather than DEBUG: unlike the moves above, this one
    // reports something that will not come right on its own.
    private void reportStandingDownOnce() {
        if (hasLoggedStandDown || !reseatDecision.hasStoodDown()) {
            return;
        }
        hasLoggedStandDown = true;
        LOG.warn("Map icon reseat: gave up lifting " + describeEntity(findEntityToReseat.get())
            + " past the map's nebulae after " + MapIconReseatDecision.MAX_ATTEMPTS
            + " attempts that did not clear them. Its layering is left as the widget seeded it "
            + "for the rest of this session.");
    }

    // Names what a reader can match against the icon-order trace, which reports the plugin class
    // rather than an entity ID - the ID being absent on the decorative entities this tends to move.
    private static String describeEntity(SectorEntityToken entity) {
        return entity == null ? "no entity" : entity.getClass().getSimpleName();
    }

    // An entity out of its location, with the location owed it back.
    private record DetachedMapIcon(SectorEntityToken entity, LocationAPI location) {
    }
}
