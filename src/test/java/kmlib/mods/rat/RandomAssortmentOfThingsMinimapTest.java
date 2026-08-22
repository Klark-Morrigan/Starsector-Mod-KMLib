package kmlib.mods.rat;

import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static kmlib.testfixtures.starsector.settings.StubbedModIds.RANDOM_ASSORTMENT_OF_THINGS;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins that the mod and its own minimap switch are both required, and that everything unreadable
 * answers "no minimap". A caller adapting to a minimap that is not on screen would hand behaviour
 * to a surface nobody is pointing at, so the two false answers matter more than the true one.
 *
 * <p>The mod-absent case additionally pins that the switch is never read, since that read is the
 * only thing here that reaches LunaLib - which logs an error for a mod id it does not know, once
 * per read, on an install that simply does not have the mod.
 *
 * <p>The read behind that switch is a passthrough to LunaLib and is exercised in-engine rather
 * than here, so no case drives it: standing LunaLib up would pin that Mockito can stub a static
 * and nothing about this class.
 */
final class RandomAssortmentOfThingsMinimapTest {

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
            ModStateScopes.runWithModEnabled(RANDOM_ASSORTMENT_OF_THINGS, false, () ->
                assertThat(new RandomAssortmentOfThingsMinimap().isReplacingRadar())
                    .isFalse());
        }

        @Test
        void reportsNoMinimapThroughTheLiveReadsBeforeTheGameSettingsAreUp() {

            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(new RandomAssortmentOfThingsMinimap().isReplacingRadar())
                    .isFalse());
        }
    }
}
