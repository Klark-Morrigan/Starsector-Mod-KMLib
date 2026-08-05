package kmlib.starsector.ui.map.icons;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import org.apache.log4j.Logger;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Moves one entity's icon to the end of the map widget's draw order each time a map opens, so what
 * the widget seeds on open no longer paints over it. When that is owed is
 * {@link MapIconReseatDecision}'s, which states why the move works at all; this is the two engine
 * calls that carry it out and the guard that keeps a fault in either out of the campaign's frame.
 *
 * <p>Which entity is a port rather than a search: the caller is the only one that can say which of a
 * location's entities is its own, and a rule stated here would be a guess about somebody's content.
 * It is asked again at each step rather than cached at construction, so an entity a save load
 * replaced is the one moved rather than a stale instance nothing draws.
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
    private final MapIconReseatDecision reseatDecision = new MapIconReseatDecision();

    // The entity taken out, with the location it came from, held for the single advance it spends
    // out. One field rather than two so the pair cannot part company: an entity without its former
    // location has nowhere to go back to, and neither half is any use alone.
    private DetachedMapIcon detachedMapIcon;

    // One-shot guard: this runs every frame, so a recurring fault would flood the log. The first
    // failure is recorded, the rest silenced.
    private boolean hasLoggedReseatError;

    /**
     * @param isMapShowing whether a map whose icon order matters is on screen; the edge into true is
     *        what arms a reseat, since the widget seeds its icon map once per open
     * @param findEntityToReseat the entity to move, or null when its location holds none
     */
    public MapIconReseater(
            BooleanSupplier isMapShowing,
            Supplier<SectorEntityToken> findEntityToReseat) {
                
        this.isMapShowing = isMapShowing;
        this.findEntityToReseat = findEntityToReseat;
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
        switch (reseatDecision.decideReseatAction(
                isMapShowing.getAsBoolean(),
                () -> findEntityToReseat.get() != null)) {
            case REMOVE -> detachMapIcon();
            case ADD -> attachMapIcon();
            case NONE -> { }
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
    }

    private void attachMapIcon() {
        if (detachedMapIcon == null) {
            return;
        }
        detachedMapIcon.location().addEntity(detachedMapIcon.entity());
        detachedMapIcon = null;
    }

    // An entity out of its location, with the location owed it back.
    private record DetachedMapIcon(SectorEntityToken entity, LocationAPI location) {
    }
}
