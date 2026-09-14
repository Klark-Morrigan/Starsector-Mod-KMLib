package kmlib.starsector.ui.layout;

import com.fs.starfarer.api.ui.PositionAPI;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VanillaPositionsTest {

    @Nested
    class ToRectangle {
        @Test
        void convertsThePositionOriginAndSizeIntoARectangle() {
            var positionMock = mock(PositionAPI.class);
            when(positionMock.getX()).thenReturn(10f);
            when(positionMock.getY()).thenReturn(20f);
            when(positionMock.getWidth()).thenReturn(30f);
            when(positionMock.getHeight()).thenReturn(40f);

            var bounds = VanillaPositions.toRectangle(positionMock);

            assertThat(bounds).isEqualTo(new Rectangle(10f, 20f, 30f, 40f));
        }
    }
}
