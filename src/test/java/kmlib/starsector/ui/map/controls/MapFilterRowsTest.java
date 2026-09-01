package kmlib.starsector.ui.map.controls;

import kmlib.starsector.ui.map.probes.EmbeddedMap;
import kmlib.testfixtures.starsector.ui.map.controls.FilteredMapWidgetFake;
import kmlib.testfixtures.starsector.ui.map.controls.MapFilterRowFake;
import kmlib.testfixtures.starsector.ui.map.probes.SectorMapWidgetFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what the reach to a map's filter row answers, and - just as much - what it answers when there
 * is no row to reach. The walk to the map itself is unpublished-API reflection and only observable
 * in a running game, but the hop off that map and the three ways it comes back empty are plain, and
 * they are what decides whether a control is appended to somebody else's widget or nothing is
 * touched at all.
 */
class MapFilterRowsTest {

    private static final List<Object> NO_ANCESTORS = List.of();

    @Nested
    class ResolveMapFilterRowOf {

        @Test
        void resolveMapFilterRowOfAnswersTheRowTheMapOffers() {
            // The ordinary case on either screen: the game's map, holding the row it built.
            //
            // Checked against a second, identical row as well as against the right one, because the
            // handle is the only way to observe which row came back: an identity test that answered
            // yes to everything would let this pass while the wrong row was resolved.
            var rowFake = new MapFilterRowFake("Starscape", "Fuel range");
            var otherRowFake = new MapFilterRowFake("Starscape", "Fuel range");

            var resolvedRow = MapFilterRows.resolveMapFilterRowOf(new FilteredMapWidgetFake(rowFake));

            assertThat(resolvedRow)
                .isNotNull();
            assertThat(resolvedRow.isSameRowAs(new MapFilterRow(rowFake)))
                .isTrue();
            assertThat(resolvedRow.isSameRowAs(new MapFilterRow(otherRowFake)))
                .isFalse();
        }

        @Test
        void resolveMapFilterRowOfAnswersNothingForAMapCarryingNoSuchAccessor() {
            // A map that is one by the published interface without being the game's own - which is
            // what a build that renamed the accessor would also look like from here. Either way
            // there is no row, and a caller has the same nothing to do about it.
            assertThat(MapFilterRows.resolveMapFilterRowOf(new SectorMapWidgetFake()))
                .isNull();
        }

        @Test
        void resolveMapFilterRowOfAnswersNothingForAMapThatOffersNoRow() {
            // The accessor is there and answers with nothing, which is a different shape from the
            // case above and the same answer: a map with no row is nothing to append to.
            assertThat(MapFilterRows.resolveMapFilterRowOf(new FilteredMapWidgetFake(null)))
                .isNull();
        }

        @Test
        void resolveMapFilterRowOfAnswersNothingWhenThereIsNoMap() {
            // Every screen showing no map, which is most of them. Not a failure, and the reason the
            // rule is asked before anything is reached for.
            assertThat(MapFilterRows.resolveMapFilterRowOf(null))
                .isNull();
        }
    }

    @Nested
    class ResolveEmbeddedMapFilterRow {

        @Test
        void resolveEmbeddedMapFilterRowAnswersTheRowOfAMapInSomebodyElsesPanel() {
            // A mod's composited map is a map like any other and builds its own row, so the same
            // hop reaches it - which is what lets one control serve a surface KMLib did not put on
            // screen.
            var rowFake = new MapFilterRowFake("Starscape");
            var otherRowFake = new MapFilterRowFake("Starscape");
            var embeddedMap = new EmbeddedMap(new FilteredMapWidgetFake(rowFake), NO_ANCESTORS);

            var resolvedRow = MapFilterRows.resolveEmbeddedMapFilterRow(embeddedMap);

            assertThat(resolvedRow)
                .isNotNull();
            assertThat(resolvedRow.isSameRowAs(new MapFilterRow(rowFake)))
                .isTrue();
            assertThat(resolvedRow.isSameRowAs(new MapFilterRow(otherRowFake)))
                .isFalse();
        }

        @Test
        void resolveEmbeddedMapFilterRowAnswersNothingWhenNoMapWasFound() {
            // The state a caller is in on every install that composites no map at all, which must
            // not reach for a row on nothing.
            assertThat(MapFilterRows.resolveEmbeddedMapFilterRow(null))
                .isNull();
        }
    }
}
