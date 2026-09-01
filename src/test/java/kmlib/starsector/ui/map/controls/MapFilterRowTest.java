package kmlib.starsector.ui.map.controls;

import kmlib.testfixtures.starsector.ui.map.controls.MapFilterRowFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how a held row is told from the one on screen. The map widget rebuilds its row in its own
 * constructor, so every open of the screen produces a new one and a handle kept across that open
 * names a row nobody can see - which is the difference a control attached to it has no other way to
 * notice.
 */
class MapFilterRowTest {

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
}
