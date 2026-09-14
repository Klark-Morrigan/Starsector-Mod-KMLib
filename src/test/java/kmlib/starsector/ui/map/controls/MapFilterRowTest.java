package kmlib.starsector.ui.map.controls;

import kmlib.math.geometry.Rectangle;
import kmlib.testfixtures.starsector.ui.map.controls.MapFilterButtonFake;
import kmlib.testfixtures.starsector.ui.map.controls.MapFilterRowFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how a held row is told from the one on screen, and the two measurements a control appended to
 * it is fitted by.
 *
 * <p>The identity question is the one asked every frame: the map widget rebuilds its row in its own
 * constructor, so every open of the screen produces a new one and a handle kept across that open
 * names a row nobody can see - which is the difference a control attached to it has no other way to
 * notice.
 *
 * <p>The two reads beside it answer where the row is and where the last thing on it stands, each
 * with a way of having no answer at all. Pinned here rather than only through whatever fits a
 * control to a row, because what they mean is a fact about the row: which child counts as the last
 * one, and which shapes have no box to read, are answers a reader should find under the row's own
 * name.
 */
class MapFilterRowTest {

    // A row placed away from the origin, so a box read back can be told from one that defaulted.
    private static final Rectangle ROW_BOX = new Rectangle(40f, 12f, 500f, 25f);

    @Nested
    class IsSameRowAs {

        @Test
        void isSameRowAsAnswersYesForTwoHandlesOnOneRow() {
            // The common case at rest: the screen was reopened onto the same row, and whatever is
            // attached to it is still attached to what the player is looking at.
            var rowFake = MapFilterRowFake.createMapScreenStrip("Starscape", "Fuel range");

            assertThat(new MapFilterRow(rowFake).isSameRowAs(new MapFilterRow(rowFake)))
                .isTrue();
        }

        @Test
        void isSameRowAsAnswersNoForARowThatWasRebuilt() {
            // Two rows built the same way are still two rows, which is why the question is asked by
            // identity: a rebuilt row carries the same buttons at the same sizes as the one it
            // replaced, so anything comparing their contents would call them one.
            assertThat(new MapFilterRow(MapFilterRowFake.createMapScreenStrip("Starscape"))
                .isSameRowAs(new MapFilterRow(MapFilterRowFake.createMapScreenStrip("Starscape"))))
                .isFalse();
        }

        @Test
        void isSameRowAsAnswersNoWhenThereIsNoRowToCompareAgainst() {
            // What a caller holds while no map is on screen. Not the same row as anything, so a
            // control attached to this one is not left believing it is still where it was put.
            var rowFake = MapFilterRowFake.createMapScreenStrip("Starscape");

            assertThat(new MapFilterRow(rowFake).isSameRowAs(null))
                .isFalse();
        }
    }

    @Nested
    class ReadBox {

        @Test
        void readBoxAnswersWhereTheLayoutPutTheRow() {
            // The row's own box is where a control's height comes from, and it is read live rather
            // than kept, a resized window moving the row it was measured against.
            var rowFake = MapFilterRowFake.createRowOfSize(ROW_BOX, 120f, "Starscape");

            assertThat(new MapFilterRow(rowFake).readBox())
                .isEqualTo(ROW_BOX);
        }

        @Test
        void readBoxAnswersNothingForAShapeTheLayoutNeverPlaced() {
            // A shape carrying no box at all, which is what a game build that reworked the row into
            // something the layout does not place looks like from here. The same answer as a
            // component the layout has built and not yet positioned, and for the same reason: there
            // is nothing to fit a control against.
            assertThat(new MapFilterRow(new Object()).readBox())
                .isNull();
        }
    }

    @Nested
    class ReadLastButtonBox {

        @Test
        void readLastButtonBoxAnswersTheRightmostButtonRatherThanTheFirst() {
            // The row lays its children left to right off the one before, so the last child added is
            // the one furthest right - and its far edge is where the row has been filled to. A read
            // that answered the first would report a row as emptier than it is and append over a
            // button already standing there.
            var rowFake = MapFilterRowFake.createMapScreenStrip("Starscape", "Fuel range", "Names");

            var lastButtonBox = new MapFilterRow(rowFake).readLastButtonBox();

            assertThat(lastButtonBox)
                .isNotNull();

            // Three 120-wide buttons from the row's left edge, each one 3-wide gap past the last.
            assertThat(lastButtonBox.x())
                .isEqualTo(246f);
        }

        @Test
        void readLastButtonBoxAnswersNothingForARowHoldingNoButtons() {
            // A row with nothing on it offers no width to match, so a control laid against it would
            // be sized at a guess.
            assertThat(new MapFilterRow(MapFilterRowFake.createMapScreenStrip()).readLastButtonBox())
                .isNull();
        }

        @Test
        void readLastButtonBoxAnswersNothingForALastButtonTheLayoutNeverPlaced() {
            // A button built and not yet laid out, which is the state the game's own is in between
            // the two.
            assertThat(new MapFilterRow(new UnplacedButtonRowFake()).readLastButtonBox())
                .isNull();
        }
    }

    /** A row holding one button the layout never positioned, so there is no box at the end of it. */
    private static final class UnplacedButtonRowFake {

        // Words of its own, the way a row's button has them. Nothing here reads them - what this row
        // is about is the box that is missing - but a button built without any would stand for one
        // the game does not build.
        private static final String BUTTON_LABEL = "Starscape";

        private final MapFilterButtonFake unplacedButtonFake =
            new MapFilterButtonFake(null, BUTTON_LABEL);

        public List<Object> getChildrenCopy() {
            return List.of(unplacedButtonFake);
        }
    }
}
