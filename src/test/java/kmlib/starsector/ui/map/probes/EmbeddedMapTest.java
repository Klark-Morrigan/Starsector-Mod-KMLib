package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.ui.PositionAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.testfixtures.starsector.ui.map.probes.PlacedSectorMapWidgetFake;
import kmlib.testfixtures.starsector.ui.map.probes.SectorMapWidgetFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins what a found map answers about where it is drawn, which is the reading a rule confining the
 * pointer to somebody else's map surface compares against.
 *
 * <p>The three ways it can answer nothing are the cases worth having: a map that is not a component
 * at all, one the layout never placed, and one faded out of sight. All three describe a map the
 * player cannot point at, and a caller that took any of them for a box would confine the cursor to a
 * surface that is not on screen.
 */
class EmbeddedMapTest {

    private static final float FULLY_DRAWN = 1f;
    private static final List<Object> NO_ANCESTORS = List.of();

    @Nested
    class ResolveDrawnBox {

        @Test
        void resolveDrawnBoxAnswersThePlacedBoxOfADrawnMap() {
            // The ordinary case: a mod's minimap, placed and visible, which is the only state its
            // box is worth comparing a cursor against.
            var mapFake = new PlacedSectorMapWidgetFake(
                createPositionMock(20f, 30f, 200f, 150f), FULLY_DRAWN);

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
                createPositionMock(20f, 30f, 200f, 150f), 0f);

            assertThat(new EmbeddedMap(mapFake, NO_ANCESTORS).resolveDrawnBox())
                .isNull();
        }
    }

    // The engine's position is a wide interface of which only the four layout numbers are read, so
    // it is mocked rather than stood up.
    private static PositionAPI createPositionMock(float x, float y, float width, float height) {

        var positionMock = mock(PositionAPI.class);

        when(positionMock.getX())
            .thenReturn(x);
        when(positionMock.getY())
            .thenReturn(y);

        when(positionMock.getWidth())
            .thenReturn(width);
        when(positionMock.getHeight())
            .thenReturn(height);
            
        return positionMock;
    }
}
