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
 * Pins which of the game's four screen numbers each read answers, and that each is taken afresh.
 *
 * <p>All four differ from one another here, so neither an axis swap nor a UI/pixel swap can pass by
 * coincidence - which is the risk this class exists to take off its callers, a scaled display being
 * where the two spaces part and a developer's monitor being where they do not.
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

    // The screen a resize moves to, differing from the first on every axis.
    private static final float RESIZED_UI_WIDTH = 1280f;
    private static final float RESIZED_UI_HEIGHT = 720f;

    private MockedStatic<Global> globalMock;
    private SettingsAPI settingsMock;

    @BeforeEach
    void setUp() {

        settingsMock = mock(SettingsAPI.class);

        when(settingsMock.getScreenWidth())
            .thenReturn(UI_WIDTH);
        when(settingsMock.getScreenHeight())
            .thenReturn(UI_HEIGHT);
        when(settingsMock.getScreenWidthPixels())
            .thenReturn(PIXEL_WIDTH);
        when(settingsMock.getScreenHeightPixels())
            .thenReturn(PIXEL_HEIGHT);

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

        /**
         * The load-bearing case for the suppression this feeds: a box kept from before a resize
         * would go on reporting a widget as off screen after the screen grew to include it, and
         * nothing downstream could tell that had happened.
         */
        @Test
        void takesTheScreenAfreshRatherThanHoldingTheFirstAnswer() {

            VanillaScreen.resolveScreenBox();

            when(settingsMock.getScreenWidth())
                .thenReturn(RESIZED_UI_WIDTH);
            when(settingsMock.getScreenHeight())
                .thenReturn(RESIZED_UI_HEIGHT);

            assertThat(VanillaScreen.resolveScreenBox())
                .isEqualTo(new Rectangle(0f, 0f, 1280f, 720f));
        }
    }

    @Nested
    class ResolveUiWidth {

        @Test
        void answersTheUiAxisRatherThanThePixelOne() {
            assertThat(VanillaScreen.resolveUiWidth())
                .isEqualTo(1920f);
        }
    }

    @Nested
    class ResolveUiHeight {

        @Test
        void answersTheUiAxisRatherThanThePixelOne() {
            assertThat(VanillaScreen.resolveUiHeight())
                .isEqualTo(1080f);
        }
    }

    @Nested
    class ResolveXAxis {

        @Test
        void bindsTheHorizontalUiLengthToItsOwnPixelLength() {
            assertThat(VanillaScreen.resolveXAxis())
                .isEqualTo(new ScreenAxis(1920f, 3840f));
        }
    }

    @Nested
    class ResolveYAxis {

        @Test
        void bindsTheVerticalUiLengthToItsOwnPixelLength() {
            assertThat(VanillaScreen.resolveYAxis())
                .isEqualTo(new ScreenAxis(1080f, 2160f));
        }
    }
}
