package kmlib.starsector.ui.screen;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins which of the game's four screen numbers each read answers. All four differ from one another
 * here, so neither an axis swap nor a UI/pixel swap can pass by coincidence - which is the whole
 * risk this class exists to take off its callers, a scaled display being where the two spaces part
 * and a developer's monitor being where they do not.
 *
 * <p>The box is asserted whole rather than through its extents, since where its corner sits is half
 * of what a widget comparison rests on.
 */
class VanillaScreenTest {

    // A screen scaled 2x, so a pixel read standing in for a UI one is off by a factor rather than
    // equal, and both axes differ so a transposition shows.
    private static final float UI_WIDTH = 1920f;
    private static final float UI_HEIGHT = 1080f;
    private static final float PIXEL_WIDTH = 3840f;
    private static final float PIXEL_HEIGHT = 2160f;

    private MockedStatic<Global> globalMock;

    @BeforeEach
    void setUp() {

        var settingsMock = mock(SettingsAPI.class);
        when(settingsMock.getScreenWidth()).thenReturn(UI_WIDTH);
        when(settingsMock.getScreenHeight()).thenReturn(UI_HEIGHT);
        when(settingsMock.getScreenWidthPixels()).thenReturn(PIXEL_WIDTH);
        when(settingsMock.getScreenHeightPixels()).thenReturn(PIXEL_HEIGHT);

        globalMock = mockStatic(Global.class);
        globalMock
            .when(Global::getSettings)
            .thenReturn(settingsMock);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Nested
    class ResolveScreenBox {
        @Test
        void cornersTheScreenAtTheOriginWithItsUiExtent() {
            assertThat(VanillaScreen.resolveScreenBox())
                .isEqualTo(new Rectangle(0f, 0f, 1920f, 1080f));
        }
    }

    @Nested
    class ResolveUiWidth {
        @Test
        void answersTheUiAxisRatherThanThePixelOne() {
            assertThat(VanillaScreen.resolveUiWidth()).isEqualTo(1920f);
        }
    }

    @Nested
    class ResolveUiHeight {
        @Test
        void answersTheUiAxisRatherThanThePixelOne() {
            assertThat(VanillaScreen.resolveUiHeight()).isEqualTo(1080f);
        }
    }

    @Nested
    class ResolvePixelWidth {
        @Test
        void answersThePixelAxisRatherThanTheUiOne() {
            assertThat(VanillaScreen.resolvePixelWidth()).isEqualTo(3840f);
        }
    }

    @Nested
    class ResolvePixelHeight {
        @Test
        void answersThePixelAxisRatherThanTheUiOne() {
            assertThat(VanillaScreen.resolvePixelHeight()).isEqualTo(2160f);
        }
    }
}
