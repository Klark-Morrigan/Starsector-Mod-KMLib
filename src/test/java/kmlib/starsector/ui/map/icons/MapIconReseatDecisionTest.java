package kmlib.starsector.ui.map.icons;

import kmlib.starsector.ui.map.icons.MapIconReseatDecision.ReseatAction;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the latch the reseat rests on: it arms on the edge into a map being shown rather than on the
 * map being shown, acts over exactly two advances, and puts the entity back from every way that
 * pair of advances can be interrupted.
 */
class MapIconReseatDecisionTest {

    // The two presence answers, named so a case reads as the state it stands for rather than as a
    // bare boolean at the call.
    private static final BooleanSupplier ENTITY_PRESENT = () -> true;
    private static final BooleanSupplier ENTITY_ABSENT = () -> false;

    private static final boolean MAP_SHOWING = true;
    private static final boolean NO_MAP_SHOWING = false;

    @Nested
    class DecideReseatAction {

        @Test
        void leavesTheEntityAloneWhileNoMapIsShowing() {
            // The ordinary campaign frame. Where an icon sits in the draw order only matters once
            // something is drawing over it, so nothing is owed until a map is up.
            var reseatDecision = new MapIconReseatDecision();

            assertThat(reseatDecision.decideReseatAction(NO_MAP_SHOWING, ENTITY_PRESENT))
                .isEqualTo(ReseatAction.NONE);
        }

        @Test
        void removesTheEntityWhenAMapOpens() {
            // The edge this exists for: the widget has just seeded its icon map, putting everything
            // it appends afterwards over this entity, so the entity has to leave to get back in
            // behind them.
            var reseatDecision = new MapIconReseatDecision();

            assertThat(reseatDecision.decideReseatAction(MAP_SHOWING, ENTITY_PRESENT))
                .isEqualTo(ReseatAction.REMOVE);
        }

        @Test
        void addsTheEntityBackOnTheVeryNextAdvance() {
            // One advance out and no more: the absence exists only so a single rendered frame drops
            // the icon, and every further frame without it is a frame the entity does not draw.
            var reseatDecision = new MapIconReseatDecision();
            reseatDecision.decideReseatAction(MAP_SHOWING, ENTITY_PRESENT);

            assertThat(reseatDecision.decideReseatAction(MAP_SHOWING, ENTITY_ABSENT))
                .isEqualTo(ReseatAction.ADD);
        }

        @Test
        void leavesTheEntityAloneWhileTheMapStaysOpen() {
            // Reseating is owed once per map open, not once per frame the map is up: the icon map is
            // seeded on open, so a second move would take the entity out of a frame for nothing.
            var reseatDecision = new MapIconReseatDecision();

            reseatDecision.decideReseatAction(MAP_SHOWING, ENTITY_PRESENT);
            reseatDecision.decideReseatAction(MAP_SHOWING, ENTITY_ABSENT);

            assertThat(reseatDecision.decideReseatAction(MAP_SHOWING, ENTITY_PRESENT))
                .isEqualTo(ReseatAction.NONE);
        }

        @Test
        void leavesTheLocationAloneWhenAMapOpensOverNoEntity() {
            // Whatever seeds the entity has not run yet. Adding one from here is not even possible -
            // this is handed a way to find an entity and never a way to build one.
            var reseatDecision = new MapIconReseatDecision();

            assertThat(reseatDecision.decideReseatAction(MAP_SHOWING, ENTITY_ABSENT))
                .isEqualTo(ReseatAction.NONE);
        }

        @Test
        void addsTheEntityBackEvenAfterTheMapWasClosedMidSequence() {
            // The removal is a means, never a state to leave standing. A map closed on the detached
            // advance would otherwise strand the entity out of its location until the next load, and
            // a save written then would not hold it at all.
            var reseatDecision = new MapIconReseatDecision();
            reseatDecision.decideReseatAction(MAP_SHOWING, ENTITY_PRESENT);

            assertThat(reseatDecision.decideReseatAction(NO_MAP_SHOWING, ENTITY_ABSENT))
                .isEqualTo(ReseatAction.ADD);
        }

        @Test
        void leavesTheEntityAloneWhenSomethingElseRestoredItWhileItWasOut() {
            // Whatever seeds the entity runs on load and adds one when it finds none, so a load
            // landing on the detached advance puts one back before this does. Adding a second would
            // leave the location holding two of something meant to be unique.
            var reseatDecision = new MapIconReseatDecision();
            reseatDecision.decideReseatAction(MAP_SHOWING, ENTITY_PRESENT);

            assertThat(reseatDecision.decideReseatAction(MAP_SHOWING, ENTITY_PRESENT))
                .isEqualTo(ReseatAction.NONE);
        }

        @Test
        void removesTheEntityAgainWhenTheMapIsClosedAndReopened() {
            // Each open seeds the icon map afresh, putting the later arrivals over this entity
            // again, so the move is owed again. A latch that armed only once would leave every
            // reopened map drawing the icon where the widget first put it.
            var reseatDecision = new MapIconReseatDecision();

            reseatDecision.decideReseatAction(MAP_SHOWING, ENTITY_PRESENT);
            reseatDecision.decideReseatAction(MAP_SHOWING, ENTITY_ABSENT);
            reseatDecision.decideReseatAction(NO_MAP_SHOWING, ENTITY_PRESENT);

            assertThat(reseatDecision.decideReseatAction(MAP_SHOWING, ENTITY_PRESENT))
                .isEqualTo(ReseatAction.REMOVE);
        }

        @Test
        void doesNotAskWhetherTheEntityIsPresentOnAnOrdinaryFrame() {
            // Answering the presence question can cost a walk over everything the location holds,
            // which is why it arrives as a supplier: this runs on every campaign frame, and the
            // frames that can act are the two around a map opening.
            var presenceReadCount = new int[1];

            BooleanSupplier countingPresenceRead = () -> {
                presenceReadCount[0]++;
                return true;
            };

            var reseatDecision = new MapIconReseatDecision();
            reseatDecision.decideReseatAction(NO_MAP_SHOWING, countingPresenceRead);

            assertThat(presenceReadCount[0])
                .isEqualTo(0);
        }
    }
}
