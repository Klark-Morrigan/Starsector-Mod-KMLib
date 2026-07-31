package kmlib.starsector.graphics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class StarsectorSpritesTest {

    private static final String PATH = "graphics/icons/example.png";

    @Nested
    class LoadSprite {
        @Test
        void returnsTheSpriteResolvedBySettings() {
            var spriteMock = Mockito.mock(SpriteAPI.class);
            var settingsMock = Mockito.mock(SettingsAPI.class);
            Mockito.when(settingsMock.getSprite(PATH)).thenReturn(spriteMock);

            // Global.setSettings is the same real static seam the shared
            // StarsectorSettingsFake drives, so no mockStatic is needed -
            // this class only reaches Global.getSettings, never Misc.
            Global.setSettings(settingsMock);
            try {
                assertThat(StarsectorSprites.loadSprite(PATH)).isSameAs(spriteMock);
            } finally {
                Global.setSettings(null);
            }
        }

        @Test
        void returnsNullWhenTheSettingsLookupThrows() {
            // An unregistered texture path makes getSprite throw; the loader
            // exists to fold that into a null the caller can skip on.
            var settingsMock = Mockito.mock(SettingsAPI.class);
            Mockito.when(settingsMock.getSprite(PATH))
                .thenThrow(new RuntimeException("unregistered texture"));

            Global.setSettings(settingsMock);
            try {
                assertThat(StarsectorSprites.loadSprite(PATH)).isNull();
            } finally {
                Global.setSettings(null);
            }
        }
    }
}
