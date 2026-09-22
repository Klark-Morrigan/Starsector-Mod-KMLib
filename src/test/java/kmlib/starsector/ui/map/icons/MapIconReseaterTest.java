package kmlib.starsector.ui.map.icons;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import kmlib.starsector.ui.map.MapIconLayering;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the two engine calls against the decision that orders them: the pair that actually moves the
 * icon, the location each half is aimed at, and the ways a supplier can answer nothing without the
 * campaign's frame paying for it.
 */
class MapIconReseaterTest {

    // Advancing an EveryFrameScript from a test says nothing about elapsed time - this script reads
    // no clock - so the value only has to be one the engine could plausibly pass.
    private static final float ONE_FRAME = 0.016f;

    private static final BooleanSupplier MAP_SHOWING = () -> true;
    private static final BooleanSupplier NO_MAP_SHOWING = () -> false;

    // The placement that owes a move. A port on the script rather than something it reads, so the
    // cases below can drive the move at all - the live read walks the widget tree and answers
    // nothing outside a running game.
    private static final Function<SectorEntityToken, MapIconLayering> ICON_BURIED =
        entity -> MapIconLayering.BURIED_UNDER_NEBULAE;

    // The placement as the live widget reports it one frame on: buried while the entity is in its
    // location, and unplaceable once it is out - the script hands the read no entity then, exactly
    // as the live probe answers for none.
    private static final Function<SectorEntityToken, MapIconLayering> ICON_BURIED_UNTIL_THE_ENTITY_IS_OUT =
        entity -> entity == null
            ? MapIconLayering.UNREADABLE
            : MapIconLayering.BURIED_UNDER_NEBULAE;

    // Faults if it is asked, so a case that must not reach the widget says so by construction rather
    // than in a comment. The read costs a walk into the live tree, and the ordinary campaign frame -
    // which is nearly every frame - must not pay for one.
    private static final Function<SectorEntityToken, MapIconLayering> ICON_PLACEMENT_NOT_TO_BE_READ =
        entity -> {
            throw new AssertionError(
                "the icon's placement must not be read while no map is showing");
        };

    @Nested
    class Advance {

        @Test
        void takesTheEntityOutAndPutsItBackOnceTheWidgetHasDroppedItsIcon() {
            // The whole point of the script: one rendered frame without the icon drops it from the
            // widget's map, and the re-add puts it back at the tail of the draw order.
            var locationMock = mock(LocationAPI.class);
            var entityMock = buildEntityIn(locationMock);

            // The supplier answers nothing once the entity is out, which is what a live walk over a
            // location's contents does - and what the put-back reads to know it is owed. A stand-in
            // that kept answering would leave the entity looking present while it was detached, and
            // this test asserting a move nothing in the game would make.
            var reseater = new MapIconReseater(
                MAP_SHOWING,
                buildEntityReadThatEmptiesOnRemovalFrom(locationMock, entityMock),
                ICON_BURIED_UNTIL_THE_ENTITY_IS_OUT);

            reseater.advance(ONE_FRAME);
            reseater.advance(ONE_FRAME);

            var moveOrder = inOrder(locationMock);

            moveOrder
                .verify(locationMock)
                .removeEntity(entityMock);
            moveOrder
                .verify(locationMock)
                .addEntity(entityMock);
        }

        @Test
        void keepsTheEntityOutWhileTheWidgetStillShowsItsIcon() {
            // The campaign advances several times per rendered frame under its speed-up. A put-back
            // on the next advance would then land in the frame the removal did, and the widget would
            // render with the icon throughout - the lift undone before it could take.
            var locationMock = mock(LocationAPI.class);
            var entityMock = buildEntityIn(locationMock);

            var reseater = new MapIconReseater(
                MAP_SHOWING,
                buildEntityReadThatEmptiesOnRemovalFrom(locationMock, entityMock),
                ICON_BURIED);

            reseater.advance(ONE_FRAME);
            reseater.advance(ONE_FRAME);

            verify(locationMock, never())
                .addEntity(entityMock);
        }

        @Test
        void putsTheEntityBackIntoTheLocationItWasTakenFrom() {
            // The location is read off the entity before the removal, because an entity out of one
            // no longer names it. A put-back that re-read the entity afterwards would have nowhere
            // to aim, and one that asked the supplier again could be aimed somewhere else entirely.
            var locationMock = mock(LocationAPI.class);
            var entityMock = buildEntityIn(locationMock);

            // Answers with the entity until it is taken out, which is what a live location read
            // would do - and what would leave a second read of it empty at exactly the wrong moment.
            Supplier<SectorEntityToken> findEntityWhileItIsIn = () ->
                entityMock.getContainingLocation() == null ? null : entityMock;

            var reseater = new MapIconReseater(
                MAP_SHOWING,
                findEntityWhileItIsIn,
                ICON_BURIED_UNTIL_THE_ENTITY_IS_OUT);
            reseater.advance(ONE_FRAME);

            when(entityMock.getContainingLocation())
                .thenReturn(null);

            reseater.advance(ONE_FRAME);

            verify(locationMock)
                .addEntity(entityMock);
        }

        @Test
        void asksWhereTheIconSitsForTheEntityItIsAboutToMove() {
            // The placement and the move have to be about one entity. Taking the placement as a
            // reading rather than as a read would leave that to whoever wires the two up, where
            // nothing checks it and a wiring aimed at a second entity moves the wrong one on a
            // reading of the right one.
            var locationMock = mock(LocationAPI.class);
            var entityMock = buildEntityIn(locationMock);
            var entitiesAskedAbout = new ArrayList<SectorEntityToken>();

            var reseater = new MapIconReseater(
                MAP_SHOWING,
                () -> entityMock,
                entityAskedAbout -> {
                    entitiesAskedAbout.add(entityAskedAbout);
                    return MapIconLayering.BURIED_UNDER_NEBULAE;
                });

            reseater.advance(ONE_FRAME);

            verify(locationMock)
                .removeEntity(entityMock);
            assertThat(entitiesAskedAbout)
                .containsExactly(entityMock);
        }

        @Test
        void readsTheEntityOnceOnAnAdvanceThatMovesIt() {
            // Three readings want it - the placement, the presence test and the move - and the live
            // read walks what a location holds, so asking per reading pays for the same walk three
            // times on every advance that acts.
            var locationMock = mock(LocationAPI.class);
            var entityMock = buildEntityIn(locationMock);
            var entityReadCount = new int[1];

            var reseater = new MapIconReseater(
                MAP_SHOWING,
                () -> {
                    entityReadCount[0]++;
                    return entityMock;
                },
                ICON_BURIED);

            reseater.advance(ONE_FRAME);

            assertThat(entityReadCount[0])
                .isEqualTo(1);
        }

        @Test
        void keepsNothingOwedWhenTheLocationRefusesTheRemoval() {
            // The pair is recorded only once the entity is actually out. Recorded before, a refused
            // removal would leave this holding an entity that never left, and the put-back owed for
            // it would add a second copy of it to the location it is already in.
            var locationMock = mock(LocationAPI.class);
            var entityMock = buildEntityIn(locationMock);

            doThrow(new IllegalStateException("this location will not give the entity up"))
                .when(locationMock)
                .removeEntity(entityMock);

            var reseater = new MapIconReseater(MAP_SHOWING, () -> entityMock, ICON_BURIED);

            reseater.advance(ONE_FRAME);
            reseater.advance(ONE_FRAME);

            verify(locationMock, never())
                .addEntity(entityMock);
        }

        @Test
        void leavesTheLocationAloneWhileNoMapIsShowing() {
            // The ordinary campaign frame, which is nearly all of them.
            var locationMock = mock(LocationAPI.class);
            var entityMock = buildEntityIn(locationMock);
            var reseater = new MapIconReseater(
                NO_MAP_SHOWING,
                () -> entityMock,
                ICON_PLACEMENT_NOT_TO_BE_READ);

            reseater.advance(ONE_FRAME);
            reseater.advance(ONE_FRAME);

            verify(locationMock, never())
                .removeEntity(entityMock);
            verify(locationMock, never())
                .addEntity(entityMock);
        }

        @Test
        void toleratesASupplierThatFindsNoEntity() {
            // The ordinary state before whatever seeds the entity has run. This advances every
            // frame of a campaign, so an unguarded read here would fault on every one of them.
            var reseater = new MapIconReseater(MAP_SHOWING, () -> null, ICON_BURIED);
            var advanceWithNoEntity = (Runnable) () -> reseater.advance(ONE_FRAME);

            assertThatCode(advanceWithNoEntity::run)
                .doesNotThrowAnyException();
        }

        @Test
        void toleratesAnEntityThatIsInNoLocation() {
            // An entity found but held nowhere has no location to be put back into, so there is no
            // move to make and nothing to remember for the next advance.
            var entityMock = buildEntityIn(null);
            var reseater = new MapIconReseater(MAP_SHOWING, () -> entityMock, ICON_BURIED);
            var advanceWithHomelessEntity = (Runnable) () -> reseater.advance(ONE_FRAME);

            assertThatCode(advanceWithHomelessEntity::run)
                .doesNotThrowAnyException();
        }

        @Test
        void keepsAdvancingAfterAReadIntoTheGameFailsToLink() {
            // The reads behind this reach into the game's own widgets by name, so a build carrying
            // different signatures fails at the call and arrives as an Error rather than an
            // exception. It is the campaign thread's frame either way: unguarded, one mismatched
            // widget takes the game down instead of one reseat.
            var reseater = new MapIconReseater(() -> {
                throw new NoSuchMethodError("the widget no longer carries this signature");
            }, () -> null, ICON_BURIED);

            var advanceOverAnUnlinkableRead = (Runnable) () -> reseater.advance(ONE_FRAME);

            assertThatCode(advanceOverAnUnlinkableRead::run)
                .doesNotThrowAnyException();
        }

        @Test
        void putsTheEntityBackAfterAFaultConsumesThePutBackItWasOwed() {
            // The removal is recorded twice - by the decision, which orders the put-back on the next
            // advance, and here, as the pair to move back. A fault on that next advance spends the
            // decision's record without the move being made, and nothing afterwards asks again: the
            // placement a later advance reads is gone along with the entity. Left there, the caller
            // loses the surface this exists to keep drawing for the rest of the session.
            var locationMock = mock(LocationAPI.class);
            var entityMock = buildEntityIn(locationMock);
            var isEntityReadable = new boolean[] { true };

            Supplier<SectorEntityToken> findEntityUntilTheSectorFaults = () -> {
                if (!isEntityReadable[0]) {
                    throw new IllegalStateException("the sector cannot be read on this frame");
                }
                return entityMock;
            };

            var reseater = new MapIconReseater(
                MAP_SHOWING,
                findEntityUntilTheSectorFaults,
                ICON_BURIED);

            reseater.advance(ONE_FRAME);
            isEntityReadable[0] = false;
            reseater.advance(ONE_FRAME);

            verify(locationMock)
                .addEntity(entityMock);
        }

        @Test
        void leavesTheEntityOutForTheOneAdvanceTheLiftNeeds() {
            // The removal only works because a frame renders without the icon. Putting the entity
            // back on the advance that took it out would undo the lift before it could take, which
            // is what a recovery aimed at the wrong state would do on every move.
            var locationMock = mock(LocationAPI.class);
            var entityMock = buildEntityIn(locationMock);

            var reseater = new MapIconReseater(
                MAP_SHOWING,
                buildEntityReadThatEmptiesOnRemovalFrom(locationMock, entityMock),
                ICON_BURIED);

            reseater.advance(ONE_FRAME);

            verify(locationMock)
                .removeEntity(entityMock);
            verify(locationMock, never())
                .addEntity(entityMock);
        }

        @Test
        void keepsAdvancingAfterASupplierFaults() {
            // A caller's supplier reaching a live sector can throw on a frame where the game is
            // between states. The script advances on the campaign thread, so a fault escaping here
            // would take down the frame rather than one reseat.
            var reseater = new MapIconReseater(MAP_SHOWING, () -> {
                throw new IllegalStateException("the sector cannot be read on this frame");
            }, ICON_BURIED);

            var advanceOverAFaultingSupplier = (Runnable) () -> reseater.advance(ONE_FRAME);

            assertThatCode(advanceOverAFaultingSupplier::run)
                .doesNotThrowAnyException();
        }
    }

    @Nested
    class RunWhilePaused {

        @Test
        void advancesWhilePausedBecauseAnOpenMapHoldsTheCampaignPaused() {
            // The whole window this works in is a map being open, and opening one pauses the
            // campaign. Answering false would leave the script never advancing at all.
            var reseater = new MapIconReseater(NO_MAP_SHOWING, () -> null, ICON_PLACEMENT_NOT_TO_BE_READ);

            assertThat(reseater.runWhilePaused())
                .isTrue();
        }
    }

    @Nested
    class IsDone {

        @Test
        void neverFinishesSoEveryLaterMapOpenIsStillReseated() {
            // The engine drops a script that reports itself done. A reseat is owed once per map
            // open for as long as the save is loaded, so there is no point at which this is over.
            var reseater = new MapIconReseater(NO_MAP_SHOWING, () -> null, ICON_PLACEMENT_NOT_TO_BE_READ);

            assertThat(reseater.isDone())
                .isFalse();
        }
    }

    // An entity read that stops answering once the location is asked to remove it, standing in for a
    // live walk over what a location holds. Driven off the removal itself rather than off a call
    // count, so the two stay in step however many times the read is asked.
    private static Supplier<SectorEntityToken> buildEntityReadThatEmptiesOnRemovalFrom(
            LocationAPI locationMock,
            SectorEntityToken entityMock) {

        var isEntityHeld = new boolean[] { true };

        doAnswer(removal -> {
            isEntityHeld[0] = false;
            return null;
        }).when(locationMock).removeEntity(entityMock);

        return () -> isEntityHeld[0] ? entityMock : null;
    }

    private static SectorEntityToken buildEntityIn(LocationAPI location) {

        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.getContainingLocation())
            .thenReturn(location);

        return entityMock;
    }
}
