package kmlib.starsector.ui.map.icons;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import kmlib.starsector.ui.map.MapIconLayering;

import org.apache.log4j.Logger;

import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Moves one entity's icon to the end of the map widget's draw order whenever the widget has seeded
 * it under the nebulae, so the map's own fog no longer paints over it. When that is owed is
 * {@link MapIconReseatDecision}'s, which states why the move works at all and why it is keyed on the
 * icon's placement rather than on a map having opened; this is the two engine calls that carry it
 * out and the guard that keeps a fault in either out of the campaign's frame.
 *
 * <p>Everything it decides from is a port: which entity, which maps matter, and where an entity's
 * icon currently sits. The first two only a caller can answer. The third has an answer in this
 * library - {@code MapIconLayeringProbe} reads it off the live widget - but it is wired in rather
 * than reached for, because this package writes to the map's draw order and reads nothing, which is
 * what keeps it independent of the packages that only read. Each is asked again at every advance
 * rather than cached at construction, so an entity a save load replaced is the one moved rather than
 * a stale instance nothing draws.
 *
 * <p>The placement port is asked <em>about</em> an entity rather than standing for one, so it cannot
 * be pointed at a different entity from the one moved. Stated as two independent readings it would
 * be the caller's job to aim both at the same thing, which is a rule nothing could check and one a
 * reader of the wiring would have to be told. Within an advance the entity is read once and handed
 * to both, so the pairing costs a walk rather than three.
 *
 * <p>Every move it makes is logged at DEBUG on this library's own logger, not the calling mod's, so
 * following a layering problem means turning KMLib's verbosity up rather than the mod's. A move is
 * rare - one per re-seeding of the icon map - so the lines stay readable beside a mod's own
 * icon-order trace. So are the map coming and going, each carrying the count of lifts the icon has
 * not been seen clear since, because that count running up across opens is what a layering that
 * degrades over a session looks like from inside. Two states are said at WARN, each once per session,
 * since neither comes right within the open and a line per open would be noise on a failure that
 * does not heal: a map that stays up with no icon placeable for the entity - the map read and the
 * placement read disagreeing about what is on screen - and the stand-down, with the readings that
 * led to it.
 *
 * <p>Runs while paused. Opening a map holds the campaign paused for as long as it is up, which is
 * the whole window this works in - a script standing down while paused would never advance here.
 *
 * <p>The entity is out of its location until the widget has dropped its icon - one advance on an
 * ordinary frame, a few under the campaign's speed-up, which advances several times per rendered
 * frame, a few more where a fault delays the put-back - which is worth knowing for anything that
 * reads the location on a timer: a save written inside that window does not hold it, so whatever
 * put the entity there is what has to put it back on the next load.
 */
public final class MapIconReseater implements EveryFrameScript {

    private static final Logger LOG = Global.getLogger(MapIconReseater.class);

    private final BooleanSupplier isMapShowing;
    private final Supplier<SectorEntityToken> findEntityToReseat;
    private final Function<SectorEntityToken, MapIconLayering> readIconLayeringOf;
    private final MapIconReseatDecision reseatDecision = new MapIconReseatDecision();

    // The entity taken out, with the location it came from, held for the single advance it spends
    // out. One field rather than two so the pair cannot part company: an entity without its former
    // location has nowhere to go back to, and neither half is any use alone.
    private DetachedMapIcon detachedMapIcon;

    // One-shot guard: this runs every frame, so a recurring fault would flood the log. The first
    // failure is recorded, the rest silenced.
    private boolean hasLoggedReseatError;

    // The same guard for the stand-down, which the decision reaches afresh on every map open it
    // cannot lift on: without it every such open would report it again.
    private boolean hasLoggedStandDown;

    // The same guard for the two reads disagreeing, which the decision detects once per open: a
    // failure that does not heal would otherwise be said on every open for the rest of the session.
    private boolean hasLoggedDisagreement;

    /**
     * @param isMapShowing whether a map whose icon order matters is on screen; it scopes the move to
     *        the maps the caller cares about rather than triggering it
     * @param findEntityToReseat the entity to move, or null when its location holds none
     * @param readIconLayeringOf where an entity's icon currently sits; {@code MapIconLayeringProbe}
     *        answers it from the live widget, and a caller wires that in rather than this reaching
     *        for it - this package writes to the map's draw order and reads nothing, which is what
     *        keeps it independent of the probes that only read. Asked about an entity rather than
     *        standing for one, so it cannot be pointed at a different entity from the one moved
     */
    public MapIconReseater(
            BooleanSupplier isMapShowing,
            Supplier<SectorEntityToken> findEntityToReseat,
            Function<SectorEntityToken, MapIconLayering> readIconLayeringOf) {

        this.isMapShowing = isMapShowing;
        this.findEntityToReseat = findEntityToReseat;
        this.readIconLayeringOf = readIconLayeringOf;
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
        //
        // Linkage failures are caught beside the runtime ones because the reads behind this reach
        // into the game's own widgets by name: a build whose widgets no longer carry what this was
        // compiled against fails at the call rather than at load, and arrives as an Error that a
        // guard written for exceptions lets straight through into the campaign's frame. Deliberately
        // not Throwable - an assertion or a JVM-level error says nothing about this reach and is not
        // this script's to absorb.
        try {
            applyReseatAction();

        } catch (RuntimeException | LinkageError reseatFailure) {
            reportReseatFailureOnce(reseatFailure);
        }
        returnAStrandedEntity();
    }

    private void applyReseatAction() {

        // One reading of the entity for the whole advance, handed to everything below. The decision's
        // two lazy reads and the move itself are all about the same entity, and asking the caller's
        // supplier at each would pay three times over for a walk across what a location holds.
        var entityThisAdvance = new EntityThisAdvance(findEntityToReseat);

        var action = reseatDecision.decideReseatAction(
            isMapShowing.getAsBoolean(),
            () -> readIconLayeringOf.apply(entityThisAdvance.resolveEntity()),
            () -> entityThisAdvance.resolveEntity() != null);

        switch (action) {
            case REMOVE -> detachMapIcon(entityThisAdvance.resolveEntity());
            case ADD -> attachMapIcon();
            case NONE -> {
                // Nothing owed the location this advance.
            }
        }
        reportAdvanceDiagnostics(entityThisAdvance);
    }

    private void detachMapIcon(SectorEntityToken entity) {

        // The location is read before the removal and kept, because an entity taken out of one no
        // longer names it - and the put-back has to reach the same location this took it from
        // rather than wherever the caller's supplier would point afterwards.
        var location = entity == null ? null : entity.getContainingLocation();
        if (location == null) {
            return;
        }

        // Recorded only once the removal has actually happened, so a location that refuses it leaves
        // nothing owed: a pair held for an entity still in its location would have the next advance
        // add a second copy of it.
        location.removeEntity(entity);
        detachedMapIcon = new DetachedMapIcon(entity, location);

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

    // Puts the entity back when nothing else is going to. A removal is owed its put-back on the very
    // next advance and the decision is what orders it, but the two records of that removal can part
    // company: the decision's is spent the moment the next advance asks, while the pair held here
    // lasts until the move is actually made. A fault between the two therefore leaves an entity out
    // of its location that no later advance has any reason to return - the placement it would read
    // is gone along with the entity - which would cost the caller the very surface the lift exists
    // to keep drawing, for the rest of the session.
    //
    // Guarded on its own, and quietly: it runs on a frame whose fault has already been reported, and
    // an entity it cannot return now stays held, so the next advance tries again.
    private void returnAStrandedEntity() {

        if (detachedMapIcon == null || reseatDecision.isPutBackOwed()) {
            return;
        }

        try {
            attachMapIcon();

        } catch (RuntimeException | LinkageError putBackError) {

            LOG.debug(
                "Map icon reseat: "
                    + "could not return a detached entity; "
                    + "holding it for the next advance",
                putBackError);
        }
    }

    // What this advance says about the lift beyond the move itself, for a log read after the picture
    // went wrong: the map coming and going with the count carried across, the two reads behind the
    // decision disagreeing, and the stand-down. Detecting each is the decision's, being read off
    // state it keeps; wording them is this script's, the logger being its.
    private void reportAdvanceDiagnostics(EntityThisAdvance entityThisAdvance) {

        var notes = reseatDecision.readNotesOfLastAdvance();

        switch (notes.mapShowingEdge()) {
            case OPENED -> LOG.debug("Map icon reseat: a map this lifts for opened; "
                + describeLiftCount(notes));
            case CLOSED -> LOG.debug("Map icon reseat: the map closed; icon seen clear while it "
                + "was up=" + notes.wasIconSeenClearThisOpen() + ", " + describeLiftCount(notes));
            case NONE -> {
                // The map is as it was, so there is no moment to mark.
            }
        }
        if (notes.isDisagreementToReport()) {
            reportDisagreementOnce(entityThisAdvance);
        }
        reportStandingDownOnce(entityThisAdvance);
    }

    // Says once that the two reads this is handed do not describe the same screen: the map read
    // says a map this cares about is up, and for as long as it has been, the placement read has
    // found no icon for the entity - which is there to be found. Nothing can be lifted on such a
    // map, and neither read alone says why. WARN because it will not come right within the open;
    // once per session rather than per open, which is how often the decision detects it, because
    // a failure that does not heal is detected on every open and the first line already says all
    // of it.
    private void reportDisagreementOnce(EntityThisAdvance entityThisAdvance) {

        if (hasLoggedDisagreement) {
            return;
        }
        hasLoggedDisagreement = true;
        LOG.warn("Map icon reseat: a map has been showing for "
            + MapIconReseatDecision.UNPLACEABLE_ADVANCES_BEFORE_DISAGREEMENT + " advances with "
            + describeEntity(entityThisAdvance.resolveEntity())
            + " in its location and no icon placeable for it. The map read and the placement read "
            + "disagree about what is on screen, so nothing is lifted past the nebulae on this "
            + "map. Said once per session; later opens that read the same way are not reported.");
    }

    // Says once that the lift has been abandoned, which is the only state a player could otherwise
    // only diagnose from the picture. WARN rather than DEBUG: unlike the moves above, this one
    // reports something that will not come right within the open. Once per session although the
    // decision stands down per open, for the reason the disagreement is. Carries the readings that
    // led to it, since "did not clear" alone cannot say whether the lifts were never observed or
    // observed and undone.
    private void reportStandingDownOnce(EntityThisAdvance entityThisAdvance) {

        if (hasLoggedStandDown || !reseatDecision.hasStoodDown()) {
            return;
        }
        hasLoggedStandDown = true;
        LOG.warn("Map icon reseat: gave up lifting "
            + describeEntity(entityThisAdvance.resolveEntity())
            + " past the map's nebulae after " + MapIconReseatDecision.MAX_ATTEMPTS
            + " attempts that did not clear them. Its layering is left as the widget seeded it "
            + "until the map is next opened, when the lift is tried again. Said once per session. "
            + "The readings leading to it, oldest first: "
            + reseatDecision.describeRecentReadings());
    }

    // The count both edges carry, worded once so the two lines agree on what it is.
    private static String describeLiftCount(ReseatAdvanceNotes notes) {
        return "lifts since the icon was last seen clear=" + notes.attemptsSinceLastClear()
            + (notes.hasStoodDown() ? ", stood down" : "");
    }

    // One line for a fault that repeats every frame. What it costs is stated rather than the fault
    // itself, the cause being carried along for whoever reads further.
    private void reportReseatFailureOnce(Throwable reseatFailure) {

        if (hasLoggedReseatError) {
            return;
        }
        hasLoggedReseatError = true;
        LOG.error(
            "Could not reseat a map icon; "
                + "its layering is left as the widget seeded it this session.",
            reseatFailure);
    }

    // Names what a reader can match against the icon-order trace, which reports the plugin class
    // rather than an entity ID - the ID being absent on the decorative entities this tends to move.
    private static String describeEntity(SectorEntityToken entity) {
        return entity == null ? "no entity" : entity.getClass().getSimpleName();
    }

    // An entity out of its location, with the location owed it back.
    private record DetachedMapIcon(SectorEntityToken entity, LocationAPI location) {
    }

    // One advance's entity, asked for at most once and answered from what came back after that.
    //
    // Lazy because the ordinary campaign frame - which is nearly every frame - must not pay for a
    // walk over what a location holds, and asked-once because an advance that acts wants the same
    // entity three times over: to place its icon, to say whether it is there at all, and to move it.
    //
    // Built fresh per advance rather than held as a field, so an entity a save load replaced is read
    // again on the next advance instead of being answered for out of the last frame's reading.
    private static final class EntityThisAdvance {

        private final Supplier<SectorEntityToken> findEntity;

        private SectorEntityToken entity;
        private boolean hasAsked;

        private EntityThisAdvance(Supplier<SectorEntityToken> findEntity) {
            this.findEntity = findEntity;
        }

        // Null is an answer like any other here - the entity is not there - so what says the read has
        // happened is a flag rather than the value, which would otherwise re-ask on every absence.
        private SectorEntityToken resolveEntity() {

            if (!hasAsked) {
                hasAsked = true;
                entity = findEntity.get();
            }
            return entity;
        }
    }
}
