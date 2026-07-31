package kmlib.starsector.ui.color;

import com.fs.starfarer.api.util.Misc;

import kmlib.starsector.testing.StarsectorSettingsFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class StarsectorUiColorTest {

    @Nested
    class Resolve {
        @Test
        void resolveReturnsStarsectorColorForVanillaEntry() {
            var expected = new Color(1, 2, 3);

            // Misc.<clinit> reads from Global.getSettings(), so a no-op
            // SettingsAPI proxy must be in place before Mockito instruments
            // the class - otherwise instrumentation triggers class init and
            // explodes on an NPE deep inside Misc's static fields.
            StarsectorSettingsFake.installSettings();
            try (var miscMock = Mockito.mockStatic(Misc.class)) {
                miscMock.when(Misc::getHighlightColor).thenReturn(expected);

                assertThat(StarsectorUiColor.VANILLA_HIGHLIGHT_GOLD.resolve())
                    .isEqualTo(expected);
            } finally {
                StarsectorSettingsFake.clearSettings();
            }
        }

        @Test
        void resolveReturnsLiteralForCustomEntry() {
            assertThat(StarsectorUiColor.ORANGE.resolve())
                .isEqualTo(new Color(255, 100, 0, 255));
        }

        @Test
        void resolveRejectsNullFromStarsectorSupplier() {
            // Misc returns null during early engine boot for some palette
            // accessors; surfacing that as an NPE with the enum name beats
            // letting a null Color propagate into UI code.
            StarsectorSettingsFake.installSettings();
            try (var miscMock = Mockito.mockStatic(Misc.class)) {
                miscMock.when(Misc::getHighlightColor).thenReturn(null);

                assertThatNullPointerException()
                    .isThrownBy(StarsectorUiColor.VANILLA_HIGHLIGHT_GOLD::resolve)
                    .withMessageContaining("GOLD");
            } finally {
                StarsectorSettingsFake.clearSettings();
            }
        }
    }
}
