package kmlib.starsector.ui.map.probes;

import kmlib.math.geometry.Rectangle;
import kmlib.testfixtures.starsector.ui.coreui.CoreUiComponentFake;
import kmlib.testfixtures.starsector.ui.layout.PositionFake;
import kmlib.testfixtures.starsector.ui.map.probes.PlacedSectorMapWidgetFake;
import kmlib.testfixtures.starsector.ui.map.probes.SectorMapWidgetFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what a found map answers about where it is drawn, which is the reading a rule confining the
 * pointer to somebody else's map surface compares against.
 *
 * <p>The three ways it can answer nothing are the cases worth having: a map that is not a component
 * at all, one the layout never placed, and one faded out of sight. All three describe a map the
 * player cannot point at, and a caller that took any of them for a box would confine the cursor to a
 * surface that is not on screen.
 *
 * <p>Beside them the widget read, pinned where it parts from the box: a map drawn to nothing is
 * still a component, so a caller whose subject is the widget rather than what shows of it gets an
 * answer where the box read gives none.
 *
 * <p>And the owning widget, which is the ancestry read rather than a map read at all. Which end of
 * the chain is the outermost is the sort of thing a caller cannot check for itself and a reader
 * cannot see from a call site, so it is pinned here rather than left to each holder's indexing.
 */
class EmbeddedMapTest {

    private static final float FADED_TO_NOTHING = 0f;
    private static final float FULLY_DRAWN = 1f;
    private static final List<Object> NO_ANCESTORS = List.of();
    private static final Rectangle PLACED_BOX = new Rectangle(20f, 30f, 200f, 150f);

    @Nested
    class ResolveDrawnBox {

        @Test
        void resolveDrawnBoxAnswersThePlacedBoxOfADrawnMap() {
            // The ordinary case: a mod's minimap, placed and visible, which is the only state its
            // box is worth comparing a cursor against.
            var mapFake = new PlacedSectorMapWidgetFake(
                new PositionFake(PLACED_BOX), FULLY_DRAWN);

            assertThat(new EmbeddedMap(mapFake, NO_ANCESTORS).resolveDrawnBox())
                .isEqualTo(new Rectangle(20f, 30f, 200f, 150f));
        }

        @Test
        void resolveDrawnBoxAnswersNothingForAMapThatIsNotAComponent() {
            // A map is recognised by the map interface alone, which promises nothing about layout.
            assertThat(new EmbeddedMap(new SectorMapWidgetFake(), NO_ANCESTORS).resolveDrawnBox())
                .isNull();
        }

        @Test
        void resolveDrawnBoxAnswersNothingForAMapTheLayoutNeverPlaced() {
            // A widget built but not yet laid out occupies nothing, so there is no box to point at.
            var mapFake = new PlacedSectorMapWidgetFake(null, FULLY_DRAWN);

            assertThat(new EmbeddedMap(mapFake, NO_ANCESTORS).resolveDrawnBox())
                .isNull();
        }

        @Test
        void resolveDrawnBoxAnswersNothingForAMapFadedOutOfSight() {
            // A panel keeps its box and its place in the tree while it fades away, so the box
            // outlives what the player can see - and only what they can see is pointable.
            var mapFake = new PlacedSectorMapWidgetFake(
                new PositionFake(PLACED_BOX), FADED_TO_NOTHING);

            assertThat(new EmbeddedMap(mapFake, NO_ANCESTORS).resolveDrawnBox())
                .isNull();
        }
    }

    @Nested
    class ResolveComponent {

        @Test
        void resolveComponentAnswersTheWidgetOfAMapThatIsOne() {

            var mapFake = new PlacedSectorMapWidgetFake(
                new PositionFake(PLACED_BOX),
                FULLY_DRAWN);

            assertThat(new EmbeddedMap(mapFake, NO_ANCESTORS).resolveComponent())
                .isSameAs(mapFake);
        }

        @Test
        void resolveComponentAnswersAMapFadedOutOfSight() {
            // Where this parts from the box read: a widget drawn to nothing is still a component
            // standing somewhere, which is what a caller asking about the widget itself is after.
            var mapFake = new PlacedSectorMapWidgetFake(
                new PositionFake(PLACED_BOX),
                FADED_TO_NOTHING);

            assertThat(new EmbeddedMap(mapFake, NO_ANCESTORS).resolveComponent())
                .isSameAs(mapFake);
        }

        @Test
        void resolveComponentAnswersNothingForAMapThatIsNotAComponent() {
            assertThat(new EmbeddedMap(new SectorMapWidgetFake(), NO_ANCESTORS).resolveComponent())
                .isNull();
        }
    }

    @Nested
    class ResolveDockedWidget {

        @Test
        void resolveDockedWidgetAnswersTheWidgetAddedToTheWalksRoot() {
            // The load-bearing case, and the one an index off by one would pass anyway if the chain
            // were shorter: a mod's panel with the map nested another level down inside it. The
            // panel is what covers every way a mod might have assembled that nesting, so the walk
            // root's own child is the answer rather than whatever the map hangs directly under.
            var panelFake = new CoreUiComponentFake();

            assertThat(new EmbeddedMap(
                    new SectorMapWidgetFake(),
                    List.of(new CoreUiComponentFake(), panelFake, new CoreUiComponentFake()))
                .resolveDockedWidget())
                .isSameAs(panelFake);
        }

        @Test
        void resolveDockedWidgetAnswersTheMapItselfWhenItHangsStraightUnderTheRoot() {
            // Nothing was wrapped around it, so the map is the whole of what was docked. Answering
            // nothing here would leave a caller with no root at all for a map that is plainly on
            // screen.
            var mapFake = new SectorMapWidgetFake();

            assertThat(new EmbeddedMap(mapFake, List.of(new CoreUiComponentFake()))
                .resolveDockedWidget())
                .isSameAs(mapFake);
        }

        @Test
        void resolveDockedWidgetAnswersNothingWithoutAnAncestry() {
            // A chain that was never recorded says nothing about what the map hangs under, so there
            // is no widget to name - as against the case above, where the absence is the answer.
            assertThat(new EmbeddedMap(new SectorMapWidgetFake(), NO_ANCESTORS).resolveDockedWidget())
                .isNull();
        }
    }
}
