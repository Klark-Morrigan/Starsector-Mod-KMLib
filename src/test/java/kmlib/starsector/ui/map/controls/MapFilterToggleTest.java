package kmlib.starsector.ui.map.controls;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.testfixtures.starsector.ui.layout.PositionFake;
import kmlib.testfixtures.starsector.ui.map.controls.MapFilterActionListenerFake;
import kmlib.testfixtures.starsector.ui.map.controls.MapFilterButtonFake;
import kmlib.testfixtures.starsector.ui.map.controls.MapFilterRowFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how a control is fitted to a row it did not build: that it comes out at the row's own
 * metrics rather than at any stated ones, that it is refused wherever there is no room or nothing to
 * measure, and that a caller holding one can read it, set it, and tell whether it is still where it
 * was put.
 *
 * <p>The measurement is pinned on both of the game's rows, at their real sizes, because serving them
 * both from one path with no branch on which screen is up is the whole reason it is measured at all.
 * A control that took its size from anywhere but the row would be right on one screen and wrong on
 * the other, and the difference between the two is a handful of units - visible in play, invisible
 * in a number.
 *
 * <p>The refusals are pinned one at a time because each is a different way for another party's
 * widget to be unfit for a control, and all of them have to end the same way. The row does not clip
 * what it holds, so the one that is not obvious from the outside is the crowded row: an append past
 * the end of it draws over whatever is beyond the strip rather than failing, which is why it has to
 * be declined before it is attempted rather than survived afterwards.
 */
class MapFilterToggleTest {

    private static final String LABEL = "Map layers";

    // What the game lays the M screen's own toggles at, which is what a control appended to that
    // row has to come out at.
    private static final float MAP_SCREEN_BUTTON_WIDTH = 120f;
    private static final float MAP_SCREEN_BUTTON_HEIGHT = 25f;

    // A row with plenty of room, for the cases that are about something other than the fit.
    private static final Rectangle ROOMY_ROW_BOX =
        new Rectangle(0f, 0f, 500f, MAP_SCREEN_BUTTON_HEIGHT);

    private static final String NOT_A_ROW =
        "A row standing in for one of the game's own answers what it is asked, not what it draws.";

    private static final Runnable DOES_NOTHING = () -> {
    };

    @Nested
    class AppendToRow {

        @Test
        void appendToRowLaysTheToggleAtTheMetricsOfTheRowItStandsOn() {
            // The whole point of measuring: the button comes out the size of the one beside it, and
            // as tall as the row itself, without either number being stated by the caller.
            var rowFake = MapFilterRowFake.createMapScreenStrip(
                "Starscape", "Fuel range", "Exploration");

            MapFilterToggle.appendToRow(new MapFilterRow(rowFake), LABEL, DOES_NOTHING);

            var appendedButton = (MapFilterButtonFake) rowFake.getChildrenCopy().get(3);

            assertThat(appendedButton.getPosition().getWidth())
                .isEqualTo(MAP_SCREEN_BUTTON_WIDTH);
            assertThat(appendedButton.getPosition().getHeight())
                .isEqualTo(MAP_SCREEN_BUTTON_HEIGHT);
        }

        @Test
        void appendToRowStandsTheToggleOneGapPastTheLastButtonOnTheRow() {
            // Where the row put it: two 120-wide buttons and the 3-wide gaps between them, so the
            // third stands at 246. Stated rather than worked out from those numbers - an expectation
            // that recomputed the layout would agree with a layout that had gone wrong.
            var rowFake = MapFilterRowFake.createMapScreenStrip("Starscape", "Fuel range");

            MapFilterToggle.appendToRow(new MapFilterRow(rowFake), LABEL, DOES_NOTHING);

            var appendedButton = (MapFilterButtonFake) rowFake.getChildrenCopy().get(2);

            assertThat(appendedButton.getPosition().getX())
                .isEqualTo(246f);
        }

        @Test
        void appendToRowStandsTheToggleRelativeToARowAwayFromTheOrigin() {
            // The row is not at the screen's left edge on either screen, so what is free has to be
            // measured from the row's own left rather than from zero. A row standing at 500 with one
            // 120-wide button on it has its next button at 623, and has room for it.
            var rowFake = MapFilterRowFake.createRowOfSize(
                new Rectangle(500f, 40f, 600f, MAP_SCREEN_BUTTON_HEIGHT),
                MAP_SCREEN_BUTTON_WIDTH,
                "Starscape");

            MapFilterToggle.appendToRow(new MapFilterRow(rowFake), LABEL, DOES_NOTHING);

            var appendedButton = (MapFilterButtonFake) rowFake.getChildrenCopy().get(1);

            assertThat(appendedButton.getPosition().getX())
                .isEqualTo(623f);
        }

        @Test
        void appendToRowLaysTheToggleAtTheIntelBandsSmallerMetrics() {
            // The second screen, from the same path and with no branch on which one is up. The
            // intel visor's band is both narrower and shorter than the M screen's strip, so a
            // control laid at the other screen's numbers would be plainly wrong on it.
            var rowFake = MapFilterRowFake.createIntelVisorBand("Starscape", "Show fuel range");

            MapFilterToggle.appendToRow(new MapFilterRow(rowFake), LABEL, DOES_NOTHING);

            var appendedButton = (MapFilterButtonFake) rowFake.getChildrenCopy().get(2);

            assertThat(appendedButton.getPosition().getWidth())
                .isEqualTo(125f);
            assertThat(appendedButton.getPosition().getHeight())
                .isEqualTo(19f);
        }

        @Test
        void appendToRowRefusesARowWithNoRoomLeftForAnotherButton() {
            // A row somebody else has already filled - two 120-wide buttons and the gap between
            // them, and nothing after. Refused rather than appended to, because the row does not
            // clip its children: the button would be built, laid past the end of the strip, and
            // drawn over whatever is out there.
            var rowFake = MapFilterRowFake.createRowOfSize(
                new Rectangle(0f, 0f, 243f, MAP_SCREEN_BUTTON_HEIGHT),
                MAP_SCREEN_BUTTON_WIDTH,
                "Starscape",
                "Fuel range");

            assertThat(MapFilterToggle.appendToRow(new MapFilterRow(rowFake), LABEL, DOES_NOTHING))
                .isNull();
            assertThat(rowFake.getChildrenCopy())
                .hasSize(2);
        }

        @Test
        void appendToRowAppendsToARowWithRoomForExactlyOneMoreButton() {
            // The other side of that boundary, at the width a third button and its gap exactly fill.
            // A row with just enough left is a row this may use, and refusing it would leave the
            // last of the space on both screens unusable.
            var rowFake = MapFilterRowFake.createRowOfSize(
                new Rectangle(0f, 0f, 366f, MAP_SCREEN_BUTTON_HEIGHT),
                MAP_SCREEN_BUTTON_WIDTH,
                "Starscape",
                "Fuel range");

            assertThat(MapFilterToggle.appendToRow(new MapFilterRow(rowFake), LABEL, DOES_NOTHING))
                .isNotNull();
        }

        @Test
        void appendToRowRefusesARowHoldingNothingToMeasureAgainst() {
            // A row with no buttons on it offers no width to match, and a control laid at a guess
            // would be the one thing on the row that did not look like its neighbours.
            assertThat(MapFilterToggle.appendToRow(
                    new MapFilterRow(MapFilterRowFake.createMapScreenStrip()),
                    LABEL,
                    DOES_NOTHING))
                .isNull();
        }

        @Test
        void appendToRowRefusesARowWhoseLastButtonWasNeverPlaced() {
            // A button built and not yet laid out, which is the state the game's own is in between
            // the two. Nothing to measure, so nothing is appended.
            assertThat(MapFilterToggle.appendToRow(
                    new MapFilterRow(new UnplacedButtonRowFake()),
                    LABEL,
                    DOES_NOTHING))
                .isNull();
        }

        @Test
        void appendToRowRefusesARowThatIsNotALaidOutComponent() {
            // What a game build that reworked the row into something the layout does not place
            // looks like from here. Its height is what a button's height is taken from, so there is
            // no size to build one at.
            assertThat(MapFilterToggle.appendToRow(
                    new MapFilterRow(new UnplacedRowFake()),
                    LABEL,
                    DOES_NOTHING))
                .isNull();
        }

        @Test
        void appendToRowRefusesARowMeasuringToNothing() {
            // A row placed at no size at all. It would take a button of no size, which is a control
            // the player could neither see nor click.
            var rowFake = MapFilterRowFake.createRowOfSize(
                new Rectangle(0f, 0f, 0f, 0f), 0f, "Starscape");

            assertThat(MapFilterToggle.appendToRow(new MapFilterRow(rowFake), LABEL, DOES_NOTHING))
                .isNull();
        }

        @Test
        void appendToRowRefusesARowWhoseButtonsCarryNoCheckedState() {
            // The one refusal that comes after the button is built, the button not existing before
            // then. A control whose state cannot be read is one nothing could drive or seed, so it
            // is disowned rather than handed back half-usable.
            //
            // The append is counted as well as the answer, because every other refusal here also
            // answers null: without it, a case that stopped short at the measurement or the match
            // would pass while proving nothing about the branch it was written for.
            var rowFake = new UncheckableButtonRowFake();

            assertThat(MapFilterToggle.appendToRow(new MapFilterRow(rowFake), LABEL, DOES_NOTHING))
                .isNull();
            assertThat(rowFake.countButtonsAppended())
                .isEqualTo(1);
        }
    }

    @Nested
    class IsChecked {

        @Test
        void isCheckedReportsTheStateTheButtonFlippedItselfTo() {
            // What a caller reads when its callback fires. The button flips its own state before
            // reporting the click, so the handle is what says which way it went rather than
            // anything the caller has to track alongside it.
            var rowFake = MapFilterRowFake.createMapScreenStrip("Starscape");
            var toggle = MapFilterToggle.appendToRow(new MapFilterRow(rowFake), LABEL, DOES_NOTHING);

            ((MapFilterButtonFake) rowFake.getChildrenCopy().get(1)).click();

            assertThat(toggle.isChecked())
                .isTrue();
        }
    }

    @Nested
    class IsStillAttachedTo {

        @Test
        void isStillAttachedToAnswersYesForTheRowItWasAppendedTo() {
            // The common case each frame: the row on screen is the row this stands on, and there is
            // nothing to do.
            var row = new MapFilterRow(MapFilterRowFake.createMapScreenStrip("Starscape"));
            var toggle = MapFilterToggle.appendToRow(row, LABEL, DOES_NOTHING);

            assertThat(toggle.isStillAttachedTo(row))
                .isTrue();
        }

        @Test
        void isStillAttachedToAnswersNoAfterTheScreenRebuiltItsRow() {
            // Every open of the map screen builds a new row, and a toggle left on the old one draws
            // nowhere while the new row stands bare. Nothing about the handle says so, which is why
            // this is asked rather than assumed.
            var toggle = MapFilterToggle.appendToRow(
                new MapFilterRow(MapFilterRowFake.createMapScreenStrip("Starscape")),
                LABEL,
                DOES_NOTHING);

            assertThat(toggle.isStillAttachedTo(
                new MapFilterRow(MapFilterRowFake.createMapScreenStrip("Starscape"))))
                .isFalse();
        }

        @Test
        void isStillAttachedToAnswersNoWhileNoMapIsShowingARow() {
            // What a holder asks with while the player is looking at something else. Not attached
            // to anything, so nothing is reattached to a screen that is not up.
            var toggle = MapFilterToggle.appendToRow(
                new MapFilterRow(MapFilterRowFake.createMapScreenStrip("Starscape")),
                LABEL,
                DOES_NOTHING);

            assertThat(toggle.isStillAttachedTo(null))
                .isFalse();
        }
    }

    @Nested
    class SetChecked {

        @Test
        void setCheckedShowsTheStateWithoutReportingAClick() {
            // How a freshly appended toggle is seeded from what the player left behind. Seeding is
            // not a click, so it must not travel back out as one - a control that reported its own
            // seeding would rewrite the state it was seeded from on every open of the screen.
            var rowFake = MapFilterRowFake.createMapScreenStrip("Starscape");
            var toggle = MapFilterToggle.appendToRow(new MapFilterRow(rowFake), LABEL, DOES_NOTHING);

            toggle.setChecked(true);

            assertThat(toggle.isChecked())
                .isTrue();
            assertThat(rowFake.countClicksHeard())
                .isZero();
        }
    }

    /**
     * A row the layout placed, with room on it and nothing else stated - what each case below adds
     * is the one thing it is about.
     */
    private abstract static class PlacedRowFake implements UIComponentAPI {

        @Override
        public PositionAPI getPosition() {
            return new PositionFake(ROOMY_ROW_BOX);
        }

        @Override
        public void advance(float amount) {
            throw new UnsupportedOperationException(NOT_A_ROW);
        }

        @Override
        public float getOpacity() {
            throw new UnsupportedOperationException(NOT_A_ROW);
        }

        @Override
        public void processInput(List<InputEventAPI> events) {
            throw new UnsupportedOperationException(NOT_A_ROW);
        }

        @Override
        public void render(float alphaMult) {
            throw new UnsupportedOperationException(NOT_A_ROW);
        }

        @Override
        public void setOpacity(float opacity) {
            throw new UnsupportedOperationException(NOT_A_ROW);
        }
    }

    /**
     * A placed row holding a button it never placed - the state the game's own button is in between
     * being built and being added to the row.
     */
    private static final class UnplacedButtonRowFake extends PlacedRowFake {

        private final MapFilterButtonFake unplacedButtonFake = new MapFilterButtonFake(null);

        public List<Object> getChildrenCopy() {
            return List.of(unplacedButtonFake);
        }
    }

    /** A placed row whose buttons are built with no checked state to read. */
    private static final class UncheckableButtonRowFake extends PlacedRowFake {

        private final MapFilterButtonFake placedButtonFake = new MapFilterButtonFake(null);

        private int appendCount;

        UncheckableButtonRowFake() {
            placedButtonFake.layOutAt(new Rectangle(
                0f,
                0f,
                MAP_SCREEN_BUTTON_WIDTH,
                MAP_SCREEN_BUTTON_HEIGHT));
        }

        /**
         * @return how many buttons have been stood on this row, which is what says the refusal came
         *         after the build rather than before it
         */
        int countButtonsAppended() {
            return appendCount;
        }

        public List<Object> getChildrenCopy() {
            return List.of(placedButtonFake);
        }

        private UncheckableButtonFake o00000(String buttonLabel, Object shortcut) {
            return new UncheckableButtonFake();
        }

        private void o00001(UncheckableButtonFake button, float width, float height) {
            appendCount++;
        }
    }

    /** One of that row's buttons: divertible, and nothing else. */
    private static final class UncheckableButtonFake {

        public void setListener(MapFilterActionListenerFake listener) {
        }
    }

    /** A row the layout never placed, so there is no height to build a button at. */
    private static final class UnplacedRowFake {

        public List<Object> getChildrenCopy() {
            return List.of(new MapFilterButtonFake(null));
        }
    }
}
