package kmlib.starsector.ui.color;

import com.fs.starfarer.api.util.Misc;
import kmlib.starsector.testing.StarsectorSettingsFake;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class StarsectorUiColorProviderTest {
    @Test
    void getReturnsStarsectorColorWhenRawColorUsesMisc() {
        Color expected = new Color(1, 2, 3);

        // Misc.<clinit> reads from Global.getSettings(), so a no-op
        // SettingsAPI proxy must be in place before Mockito instruments
        // the class - otherwise instrumentation triggers class init and
        // explodes on an NPE deep inside Misc's static fields.
        StarsectorSettingsFake.installSettings();
        try (MockedStatic<Misc> misc = Mockito.mockStatic(Misc.class)) {
            misc.when(Misc::getHighlightColor).thenReturn(expected);

            assertThat(StarsectorUiColorProvider.get(StarsectorUiColor.GOLD))
                    .isEqualTo(expected);
        } finally {
            StarsectorSettingsFake.clearSettings();
        }
    }

    @Test
    void getReturnsCustomColorWhenRawColorHasNoStarsectorSource() {
        assertThat(StarsectorUiColorProvider.get(StarsectorUiColor.ORANGE))
                .isEqualTo(new Color(255, 100, 0, 255));
    }

    @Test
    void getRejectsMissingColor() {
        assertThatNullPointerException()
                .isThrownBy(() -> StarsectorUiColorProvider.get(null));
    }

    @Test
    void distinguishesCustomAndStarsectorColors() {
        assertThat(StarsectorUiColor.ORANGE.isCustom()).isTrue();
        assertThat(StarsectorUiColor.GRAY.isCustom()).isFalse();
    }

    @Test
    void customEntryExposesNoStarsectorSource() {
        // Guards the branch selection in get(): a custom-only entry must
        // skip resolveStarsectorColor entirely, otherwise Misc would be
        // invoked for shades that have no vanilla equivalent.
        assertThat(StarsectorUiColor.ORANGE.starsectorColor()).isEmpty();
        assertThat(StarsectorUiColor.ORANGE.customColor())
                .contains(new Color(255, 100, 0, 255));
    }

    @Test
    void starsectorEntryExposesNoCustomValue() {
        // Mirror of the above for vanilla entries - ensures the resolver
        // never silently falls back to a literal when the Misc supplier
        // is the intended source of truth.
        assertThat(StarsectorUiColor.GRAY.customColor()).isEmpty();
        assertThat(StarsectorUiColor.GRAY.starsectorColor()).isPresent();
    }

    @Test
    void getRejectsNullFromStarsectorSupplier() {
        // Misc returns null during early engine boot for some palette
        // accessors; surfacing that as an NPE with the enum name beats
        // letting a null Color propagate into UI code.
        StarsectorSettingsFake.installSettings();
        try (MockedStatic<Misc> misc = Mockito.mockStatic(Misc.class)) {
            misc.when(Misc::getHighlightColor).thenReturn(null);

            assertThatNullPointerException()
                    .isThrownBy(() -> StarsectorUiColorProvider.get(StarsectorUiColor.GOLD))
                    .withMessageContaining("GOLD");
        } finally {
            StarsectorSettingsFake.clearSettings();
        }
    }
}
