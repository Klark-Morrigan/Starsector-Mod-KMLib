package kmlib.starsector.rat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins that the mod and its own minimap switch are both required, and that everything unreadable
 * answers "no minimap". A caller adapting to a minimap that is not on screen would hand behaviour
 * to a surface nobody is pointing at, so the two false answers matter more than the true one.
 *
 * <p>The mod-absent case additionally pins that the switch is never read, since that read is the
 * only thing here that reaches LunaLib - which logs an error for a mod id it does not know, once
 * per read, on an install that simply does not have the mod.
 */
final class RandomAssortmentOfThingsMinimapTest {

    private static final String RAT_MOD_ID = "assortment_of_things";

    @Nested
    class IsReplacingRadar {

        @Test
        void reportsAMinimapWhileTheModIsEnabledWithItsMinimapOn() {

            var minimap = new RandomAssortmentOfThingsMinimap(() -> true, () -> true);

            assertThat(minimap.isReplacingRadar())
                .isTrue();
        }

        @Test
        void reportsNoMinimapWhileTheModsMinimapIsOff() {

            var minimap = new RandomAssortmentOfThingsMinimap(() -> true, () -> false);

            assertThat(minimap.isReplacingRadar())
                .isFalse();
        }

        @Test
        void reportsNoMinimapWithoutReadingTheSwitchWhenTheModIsAbsent() {

            var switchReadCount = new AtomicInteger();
            BooleanSupplier isMinimapSwitchedOn = () -> {
                switchReadCount.incrementAndGet();
                return true;
            };

            var minimap = new RandomAssortmentOfThingsMinimap(() -> false, isMinimapSwitchedOn);

            assertThat(minimap.isReplacingRadar())
                .isFalse();
            assertThat(switchReadCount.get())
                .isZero();
        }

        @Test
        void reportsNoMinimapThroughTheLiveReadsWhenTheModIsAbsent() {

            // The live pairing rather than a stood-in one: the presence gate is what keeps an
            // install without the mod from asking LunaLib about that mod's fields, and nothing
            // else here exercises the constructor that binds the two live reads.
            try (var globalMock = mockStatic(Global.class)) {

                stubGlobalWithModEnabled(globalMock, false);

                assertThat(new RandomAssortmentOfThingsMinimap().isReplacingRadar())
                    .isFalse();
            }
        }

        @Test
        void reportsNoMinimapThroughTheLiveReadsBeforeTheGameSettingsAreUp() {

            try (var globalMock = mockStatic(Global.class)) {

                globalMock
                    .when(Global::getSettings)
                    .thenReturn(null);

                assertThat(new RandomAssortmentOfThingsMinimap().isReplacingRadar())
                    .isFalse();
            }
        }
    }

    private static void stubGlobalWithModEnabled(
            MockedStatic<Global> globalMock,
            boolean isModEnabled) {

        var settingsMock = mock(SettingsAPI.class);
        var modManagerMock = mock(ModManagerAPI.class);

        globalMock
            .when(Global::getSettings)
            .thenReturn(settingsMock);

        when(settingsMock.getModManager())
            .thenReturn(modManagerMock);

        when(modManagerMock.isModEnabled(RAT_MOD_ID))
            .thenReturn(isModEnabled);
    }
}
