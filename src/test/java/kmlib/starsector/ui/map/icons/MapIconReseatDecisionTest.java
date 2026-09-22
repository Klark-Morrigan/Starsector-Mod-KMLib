package kmlib.starsector.ui.map.icons;

import kmlib.starsector.ui.map.MapIconLayering;
import kmlib.starsector.ui.map.icons.MapIconReseatDecision.ReseatAction;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the rule the reseat rests on: it acts on where the icon is rather than on an event that would
 * have moved it, acts over exactly two advances, puts the entity back from every way that pair of
 * advances can be interrupted, and abandons a lift that never takes.
 *
 * <p>The state-keyed shape is what most of these are about. An earlier rule armed on the edge into a
 * map being shown, which every test of it passed and which failed in play the first time an open
 * went unobserved - so the cases here drive readings rather than transitions, and the one that would
 * have caught it asks for a second lift with no intervening "no map at all".
 */
class MapIconReseatDecisionTest {

    // The two presence answers, named so a case reads as the state it stands for rather than as a
    // bare boolean at the call.
    private static final BooleanSupplier ENTITY_PRESENT = () -> true;
    private static final BooleanSupplier ENTITY_ABSENT = () -> false;

    // The three placements, likewise.
    private static final Supplier<MapIconLayering> ICON_BURIED = () -> MapIconLayering.BURIED_UNDER_NEBULAE;
    private static final Supplier<MapIconLayering> ICON_CLEAR = () -> MapIconLayering.CLEAR_OF_NEBULAE;
    private static final Supplier<MapIconLayering> ICON_UNPLACEABLE = () -> MapIconLayering.UNREADABLE;

    private static final boolean MAP_SHOWING = true;
    private static final boolean NO_MAP_SHOWING = false;

    @Nested
    class DecideReseatAction {

        @Test
        void leavesTheEntityAloneWhileNoMapIsShowing() {
            // The ordinary campaign frame. Where an icon sits in the draw order only matters once
            // something is drawing over it, so nothing is owed until a map is up.
            var reseatDecision = new MapIconReseatDecision();

            assertThat(reseatDecision.decideReseatAction(
                    NO_MAP_SHOWING,
                    ICON_BURIED,
                    ENTITY_PRESENT))
                .isEqualTo(ReseatAction.NONE);
        }

        @Test
        void removesTheEntityWhileItsIconIsBuried() {
            // The state this exists for: the widget has seeded its icon map with this entity ahead
            // of the nebulae it appends afterwards, so the entity has to leave to get back in behind
            // them.
            var reseatDecision = new MapIconReseatDecision();

            assertThat(reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_PRESENT))
                .isEqualTo(ReseatAction.REMOVE);
        }

        @Test
        void addsTheEntityBackOnTheVeryNextAdvance() {
            // One advance out and no more: the absence exists only so a single rendered frame drops
            // the icon, and every further frame without it is a frame the entity does not draw.
            var reseatDecision = new MapIconReseatDecision();
            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_PRESENT);

            assertThat(reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_ABSENT))
                .isEqualTo(ReseatAction.ADD);
        }

        @Test
        void leavesTheEntityAloneOnceItsIconIsClear() {
            // The lift took. Moving again would take the entity out of a frame for nothing, and this
            // runs every frame a map is up.
            var reseatDecision = new MapIconReseatDecision();

            assertThat(reseatDecision.decideReseatAction(MAP_SHOWING, ICON_CLEAR, ENTITY_PRESENT))
                .isEqualTo(ReseatAction.NONE);
        }

        @Test
        void removesTheEntityAgainWhenItsIconIsBuriedOnceMore() {
            // The regression that put this rule on a reading. Something re-seeded the icon map -
            // a map reopened, a widget rebuilt, anything - and the icon is under the fog again with
            // no "no map showing" advance in between. An edge-armed rule sees nothing here and
            // leaves the layering wrong for the rest of the session.
            var reseatDecision = new MapIconReseatDecision();

            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_PRESENT);
            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_ABSENT);
            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_CLEAR, ENTITY_PRESENT);

            assertThat(reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_PRESENT))
                .isEqualTo(ReseatAction.REMOVE);
        }

        @Test
        void leavesTheLocationAloneWhileThereIsNoEntity() {
            // Whatever seeds the entity has not run yet. Adding one from here is not even possible -
            // this is handed a way to find an entity and never a way to build one.
            var reseatDecision = new MapIconReseatDecision();

            assertThat(reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_ABSENT))
                .isEqualTo(ReseatAction.NONE);
        }

        @Test
        void leavesTheEntityAloneWhileItsIconCannotBePlaced() {
            // No icon for it yet, or a widget tree that could not be read. Acting on that would be
            // moving an entity on the strength of an answer nobody got.
            var reseatDecision = new MapIconReseatDecision();

            assertThat(reseatDecision.decideReseatAction(
                    MAP_SHOWING,
                    ICON_UNPLACEABLE,
                    ENTITY_PRESENT))
                .isEqualTo(ReseatAction.NONE);
        }

        @Test
        void addsTheEntityBackEvenAfterTheMapWasClosedMidSequence() {
            // The removal is a means, never a state to leave standing. A map closed on the detached
            // advance would otherwise strand the entity out of its location until the next load, and
            // a save written then would not hold it at all.
            var reseatDecision = new MapIconReseatDecision();
            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_PRESENT);

            assertThat(reseatDecision.decideReseatAction(
                    NO_MAP_SHOWING,
                    ICON_UNPLACEABLE,
                    ENTITY_ABSENT))
                .isEqualTo(ReseatAction.ADD);
        }

        @Test
        void leavesTheEntityAloneWhenSomethingElseRestoredItWhileItWasOut() {
            // Whatever seeds the entity runs on load and adds one when it finds none, so a load
            // landing on the detached advance puts one back before this does. Adding a second would
            // leave the location holding two of something meant to be unique.
            var reseatDecision = new MapIconReseatDecision();
            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_PRESENT);

            assertThat(reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_PRESENT))
                .isEqualTo(ReseatAction.NONE);
        }

        @Test
        void standsDownOnceRepeatedLiftsHaveNotClearedTheNebulae() {
            // A build the lever no longer fits. Reading the state means this would otherwise try
            // forever, taking the entity out of its location every other frame for a lift that
            // cannot work - so the attempts are bounded and the layering left as the widget seeded
            // it.
            var reseatDecision = new MapIconReseatDecision();

            driveFailedLifts(reseatDecision, MapIconReseatDecision.MAX_ATTEMPTS);

            assertThat(reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_PRESENT))
                .isEqualTo(ReseatAction.NONE);
        }

        @Test
        void keepsLiftingWhileTheAttemptsHaveNotRunOut() {
            // One short of the cap, so the bound is pinned from both sides: a cap that fired early
            // would abandon the move on a game merely running slowly enough for a put-back to be
            // read before the frame that re-seeds the icon.
            var reseatDecision = new MapIconReseatDecision();

            driveFailedLifts(reseatDecision, MapIconReseatDecision.MAX_ATTEMPTS - 1);

            assertThat(reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_PRESENT))
                .isEqualTo(ReseatAction.REMOVE);
        }

        @Test
        void forgivesTheSpentAttemptsOnceTheIconIsSeenClear() {
            // The count is of lifts that achieved nothing, so a sighting of a cleared icon ends the
            // run. Without this, a long session of ordinary map opens would eventually exhaust a
            // budget meant for a lever that does not work at all.
            var reseatDecision = new MapIconReseatDecision();

            driveFailedLifts(reseatDecision, MapIconReseatDecision.MAX_ATTEMPTS);

            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_CLEAR, ENTITY_PRESENT);

            assertThat(reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_PRESENT))
                .isEqualTo(ReseatAction.REMOVE);
        }

        @Test
        void doesNotReadTheLayeringOnAnOrdinaryFrame() {
            // Answering it costs a walk into the live widget tree, which is why it arrives as a
            // supplier: this runs on every campaign frame and only the frames with a map up can act.
            var layeringReadCount = new int[1];

            Supplier<MapIconLayering> countingLayeringRead = () -> {
                layeringReadCount[0]++;
                return MapIconLayering.BURIED_UNDER_NEBULAE;
            };

            var reseatDecision = new MapIconReseatDecision();
            reseatDecision.decideReseatAction(NO_MAP_SHOWING, countingLayeringRead, ENTITY_PRESENT);

            assertThat(layeringReadCount[0])
                .isEqualTo(0);
        }

        @Test
        void doesNotAskWhetherTheEntityIsPresentOnAnOrdinaryFrame() {
            // Answering the presence question can cost a walk over everything the location holds,
            // and it is asked for the same reason the layering is: only on the frames that can act.
            var presenceReadCount = new int[1];

            BooleanSupplier countingPresenceRead = () -> {
                presenceReadCount[0]++;
                return true;
            };

            var reseatDecision = new MapIconReseatDecision();
            reseatDecision.decideReseatAction(NO_MAP_SHOWING, ICON_BURIED, countingPresenceRead);

            assertThat(presenceReadCount[0])
                .isEqualTo(0);
        }
    }

    @Nested
    class HasStoodDown {

        @Test
        void hasStoodDownIsFalseWhileLiftsAreStillBeingAttempted() {

            var reseatDecision = new MapIconReseatDecision();

            driveFailedLifts(reseatDecision, MapIconReseatDecision.MAX_ATTEMPTS - 1);

            assertThat(reseatDecision.hasStoodDown())
                .isFalse();
        }

        @Test
        void hasStoodDownIsTrueOnceTheAttemptsAreSpent() {
            // Published so the caller can say once that the layering is not coming right, which is
            // otherwise a state a player could only diagnose from the picture.
            var reseatDecision = new MapIconReseatDecision();

            driveFailedLifts(reseatDecision, MapIconReseatDecision.MAX_ATTEMPTS);

            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_PRESENT);

            assertThat(reseatDecision.hasStoodDown())
                .isTrue();
        }
    }

    @Nested
    class IsPutBackOwed {

        @Test
        void isPutBackOwedIsFalseBeforeAnythingHasBeenTakenOut() {

            assertThat(new MapIconReseatDecision().isPutBackOwed())
                .isFalse();
        }

        @Test
        void isPutBackOwedIsTrueWhileTheEntityIsOutOfItsLocation() {
            // What a caller holding the entity reads to tell the one advance it is meant to spend
            // out from an entity nothing is coming back for.
            var reseatDecision = new MapIconReseatDecision();

            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_PRESENT);

            assertThat(reseatDecision.isPutBackOwed())
                .isTrue();
        }

        @Test
        void isPutBackOwedIsFalseOnceTheAdvanceOwedThePutBackHasAsked() {
            // Spent by the asking rather than by the move: a caller whose put-back faulted after
            // this point is holding an entity no later advance will order back, which is the whole
            // state this read exists to expose.
            var reseatDecision = new MapIconReseatDecision();

            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_PRESENT);
            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_ABSENT);

            assertThat(reseatDecision.isPutBackOwed())
                .isFalse();
        }
    }

    @Nested
    class ReadNotesOfLastAdvance {

        @Test
        void reportsNoEdgeBeforeAnyAdvance() {

            assertThat(new MapIconReseatDecision().readNotesOfLastAdvance().mapShowingEdge())
                .isEqualTo(MapShowingEdge.NONE);
        }

        @Test
        void reportsAnOpenedEdgeOnTheFirstAdvanceWithAMapShowing() {
            // The moment a log wants marked: from here on the count of lifts is this open's.
            var reseatDecision = new MapIconReseatDecision();

            reseatDecision.decideReseatAction(NO_MAP_SHOWING, ICON_UNPLACEABLE, ENTITY_PRESENT);
            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_UNPLACEABLE, ENTITY_PRESENT);

            assertThat(reseatDecision.readNotesOfLastAdvance().mapShowingEdge())
                .isEqualTo(MapShowingEdge.OPENED);
        }

        @Test
        void reportsNoEdgeWhileTheMapStaysUp() {
            // The level is read every advance; only the moments it moves are worth a line.
            var reseatDecision = new MapIconReseatDecision();

            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_UNPLACEABLE, ENTITY_PRESENT);
            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_UNPLACEABLE, ENTITY_PRESENT);

            assertThat(reseatDecision.readNotesOfLastAdvance().mapShowingEdge())
                .isEqualTo(MapShowingEdge.NONE);
        }

        @Test
        void reportsAClosedEdgeOnTheFirstAdvanceWithoutAMap() {

            var reseatDecision = new MapIconReseatDecision();

            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_UNPLACEABLE, ENTITY_PRESENT);
            reseatDecision.decideReseatAction(NO_MAP_SHOWING, ICON_UNPLACEABLE, ENTITY_PRESENT);

            assertThat(reseatDecision.readNotesOfLastAdvance().mapShowingEdge())
                .isEqualTo(MapShowingEdge.CLOSED);
        }

        @Test
        void carriesTheSpentLiftsAcrossTheClosedEdge() {
            // The count is not reset by the map going down - only a clear sighting resets it - so
            // the closing line is where a count running up across opens can be watched running up.
            var reseatDecision = new MapIconReseatDecision();

            driveFailedLifts(reseatDecision, 2);
            reseatDecision.decideReseatAction(NO_MAP_SHOWING, ICON_UNPLACEABLE, ENTITY_PRESENT);

            assertThat(reseatDecision.readNotesOfLastAdvance().attemptsSinceLastClear())
                .isEqualTo(2);
        }

        @Test
        void saysTheIconWasSeenClearWhenTheMapClosesAfterAClearReading() {

            var reseatDecision = new MapIconReseatDecision();

            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_CLEAR, ENTITY_PRESENT);
            reseatDecision.decideReseatAction(NO_MAP_SHOWING, ICON_UNPLACEABLE, ENTITY_PRESENT);

            assertThat(reseatDecision.readNotesOfLastAdvance().wasIconSeenClearThisOpen())
                .isTrue();
        }

        @Test
        void saysTheIconWasNotSeenClearWhenTheMapClosesAfterLiftsAlone() {
            // An open whose lifts were never confirmed is exactly the open that spends the budget.
            var reseatDecision = new MapIconReseatDecision();

            driveFailedLifts(reseatDecision, 1);
            reseatDecision.decideReseatAction(NO_MAP_SHOWING, ICON_UNPLACEABLE, ENTITY_PRESENT);

            assertThat(reseatDecision.readNotesOfLastAdvance().wasIconSeenClearThisOpen())
                .isFalse();
        }

        @Test
        void forgetsTheClearSightingWhenTheMapOpensAgain() {

            var reseatDecision = new MapIconReseatDecision();

            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_CLEAR, ENTITY_PRESENT);
            reseatDecision.decideReseatAction(NO_MAP_SHOWING, ICON_UNPLACEABLE, ENTITY_PRESENT);
            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_UNPLACEABLE, ENTITY_PRESENT);

            assertThat(reseatDecision.readNotesOfLastAdvance().wasIconSeenClearThisOpen())
                .isFalse();
        }

        @Test
        void reportsTheStandDownOnceTheAttemptsAreSpent() {

            var reseatDecision = new MapIconReseatDecision();

            driveFailedLifts(reseatDecision, MapIconReseatDecision.MAX_ATTEMPTS);
            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_PRESENT);

            assertThat(reseatDecision.readNotesOfLastAdvance().hasStoodDown())
                .isTrue();
        }

        @Test
        void reportsTheDisagreementOnTheAdvanceTheUnplaceableRunReachesTheThreshold() {
            // A map up, the entity there, and no icon for it for this long is not the frame before
            // the widget seeds the icon - it is the two reads describing different screens.
            var reseatDecision = new MapIconReseatDecision();

            driveUnplaceableAdvances(
                reseatDecision,
                MapIconReseatDecision.UNPLACEABLE_ADVANCES_BEFORE_DISAGREEMENT);

            assertThat(reseatDecision.readNotesOfLastAdvance().isDisagreementToReport())
                .isTrue();
        }

        @Test
        void reportsNoDisagreementWhileTheUnplaceableRunIsShortOfTheThreshold() {
            // One short, so the threshold is pinned from below: an open legitimately reads
            // unplaceable for a few advances before the first render seeds the icon.
            var reseatDecision = new MapIconReseatDecision();

            driveUnplaceableAdvances(
                reseatDecision,
                MapIconReseatDecision.UNPLACEABLE_ADVANCES_BEFORE_DISAGREEMENT - 1);

            assertThat(reseatDecision.readNotesOfLastAdvance().isDisagreementToReport())
                .isFalse();
        }

        @Test
        void reportsTheDisagreementOnlyOnceWhileTheMapStaysUp() {

            var reseatDecision = new MapIconReseatDecision();

            driveUnplaceableAdvances(
                reseatDecision,
                MapIconReseatDecision.UNPLACEABLE_ADVANCES_BEFORE_DISAGREEMENT + 1);

            assertThat(reseatDecision.readNotesOfLastAdvance().isDisagreementToReport())
                .isFalse();
        }

        @Test
        void reportsTheDisagreementAgainOnTheNextOpen() {
            // The next open may be read from a different widget, so it earns its own report.
            var reseatDecision = new MapIconReseatDecision();

            driveUnplaceableAdvances(
                reseatDecision,
                MapIconReseatDecision.UNPLACEABLE_ADVANCES_BEFORE_DISAGREEMENT);
            reseatDecision.decideReseatAction(NO_MAP_SHOWING, ICON_UNPLACEABLE, ENTITY_PRESENT);
            driveUnplaceableAdvances(
                reseatDecision,
                MapIconReseatDecision.UNPLACEABLE_ADVANCES_BEFORE_DISAGREEMENT);

            assertThat(reseatDecision.readNotesOfLastAdvance().isDisagreementToReport())
                .isTrue();
        }

        @Test
        void reportsNoDisagreementWhileTheEntityIsAbsent() {
            // No icon for an entity that is in no location is the ordinary state before whatever
            // seeds the entity has run, not two reads disagreeing.
            var reseatDecision = new MapIconReseatDecision();

            for (var advance = 0;
                    advance < MapIconReseatDecision.UNPLACEABLE_ADVANCES_BEFORE_DISAGREEMENT;
                    advance++) {
                reseatDecision.decideReseatAction(MAP_SHOWING, ICON_UNPLACEABLE, ENTITY_ABSENT);
            }

            assertThat(reseatDecision.readNotesOfLastAdvance().isDisagreementToReport())
                .isFalse();
        }

        @Test
        void endsTheUnplaceableRunOnAClearReading() {
            // Consecutive advances, not a total: a run broken by any other reading starts over.
            var reseatDecision = new MapIconReseatDecision();

            driveUnplaceableAdvances(
                reseatDecision,
                MapIconReseatDecision.UNPLACEABLE_ADVANCES_BEFORE_DISAGREEMENT - 1);
            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_CLEAR, ENTITY_PRESENT);
            driveUnplaceableAdvances(
                reseatDecision,
                MapIconReseatDecision.UNPLACEABLE_ADVANCES_BEFORE_DISAGREEMENT - 1);

            assertThat(reseatDecision.readNotesOfLastAdvance().isDisagreementToReport())
                .isFalse();
        }
    }

    @Nested
    class DescribeRecentReadings {

        @Test
        void describesNothingBeforeAnyMapWasRead() {

            assertThat(new MapIconReseatDecision().describeRecentReadings())
                .isEqualTo("[]");
        }

        @Test
        void describesEachReadingWithItsAdvanceWhatWasFoundAndWhatWasOrdered() {

            var reseatDecision = new MapIconReseatDecision();

            driveFailedLifts(reseatDecision, 1);

            assertThat(reseatDecision.describeRecentReadings())
                .isEqualTo("[#1 ICON_BURIED -> REMOVE, #2 PUT_BACK_OWED -> ADD]");
        }

        @Test
        void recordsNothingOnAnOrdinaryFrameButCountsIt() {
            // The frame with no map up is nearly every frame and says nothing, but it still counts,
            // so a gap in the numbering reads as the frames between two opens.
            var reseatDecision = new MapIconReseatDecision();

            reseatDecision.decideReseatAction(NO_MAP_SHOWING, ICON_BURIED, ENTITY_PRESENT);
            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_CLEAR, ENTITY_PRESENT);

            assertThat(reseatDecision.describeRecentReadings())
                .isEqualTo("[#2 ICON_CLEAR -> NONE]");
        }

        @Test
        void endsWithTheReadingThatStoodTheLiftDown() {
            // The stand-down is recorded as the buried reading it was, with no move ordered, so a
            // report reads the whole run of unconfirmed lifts up to the one that ended it.
            var reseatDecision = new MapIconReseatDecision();

            driveFailedLifts(reseatDecision, MapIconReseatDecision.MAX_ATTEMPTS);
            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_PRESENT);

            assertThat(reseatDecision.describeRecentReadings())
                .endsWith("#8 PUT_BACK_OWED -> ADD, #9 ICON_BURIED -> NONE]");
        }

        @Test
        void keepsOnlyTheLatestReadings() {
            // Thirteen readings into a capacity of twelve: the first is dropped and the rest kept in
            // order, so the report is always the end of the story rather than its start.
            var reseatDecision = new MapIconReseatDecision();

            for (var advance = 0; advance < MapIconReseatDecision.RECENT_READINGS_CAPACITY + 1; advance++) {
                reseatDecision.decideReseatAction(MAP_SHOWING, ICON_CLEAR, ENTITY_PRESENT);
            }

            assertThat(reseatDecision.describeRecentReadings())
                .startsWith("[#2 ICON_CLEAR -> NONE, ")
                .endsWith(", #13 ICON_CLEAR -> NONE]")
                .doesNotContain("#1 ");
        }
    }

    // Runs whole lift cycles - out on one advance, back on the next - that never clear the fog, which
    // is the sequence the attempt bound is about.
    private static void driveFailedLifts(MapIconReseatDecision reseatDecision, int liftCount) {
        for (var lift = 0; lift < liftCount; lift++) {
            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_PRESENT);
            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_BURIED, ENTITY_ABSENT);
        }
    }

    // Runs advances on which a map is up and the entity is there but its icon cannot be placed,
    // which is the sequence the disagreement threshold is about.
    private static void driveUnplaceableAdvances(MapIconReseatDecision reseatDecision, int advanceCount) {
        for (var advance = 0; advance < advanceCount; advance++) {
            reseatDecision.decideReseatAction(MAP_SHOWING, ICON_UNPLACEABLE, ENTITY_PRESENT);
        }
    }
}
