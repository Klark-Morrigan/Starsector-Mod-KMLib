package kmlib.starsector.ui.map.icons;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import kmlib.starsector.ui.map.probes.MapIconLayeringProbe.Layering;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doAnswer;
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

    // The placement that owes a move. Supplied rather than read, because the live read walks the
    // widget tree and answers nothing outside a running game - which would leave every case below
    // asserting that a script standing down does nothing.
    private static final Supplier<Layering> ICON_BURIED = () -> Layering.BURIED_UNDER_NEBULAE;

    @Nested
    class Advance {

        @Test
        void takesTheEntityOutAndPutsItBackOnTheNextAdvance() {
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
                ICON_BURIED);

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

            var reseater = new MapIconReseater(MAP_SHOWING, findEntityWhileItIsIn, ICON_BURIED);
            reseater.advance(ONE_FRAME);

            when(entityMock.getContainingLocation())
                .thenReturn(null);

            reseater.advance(ONE_FRAME);

            verify(locationMock)
                .addEntity(entityMock);
        }

        @Test
        void leavesTheLocationAloneWhileNoMapIsShowing() {
            // The ordinary campaign frame, which is nearly all of them.
            var locationMock = mock(LocationAPI.class);
            var entityMock = buildEntityIn(locationMock);
            var reseater = new MapIconReseater(NO_MAP_SHOWING, () -> entityMock);

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
            var reseater = new MapIconReseater(NO_MAP_SHOWING, () -> null);

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
            var reseater = new MapIconReseater(NO_MAP_SHOWING, () -> null);

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
