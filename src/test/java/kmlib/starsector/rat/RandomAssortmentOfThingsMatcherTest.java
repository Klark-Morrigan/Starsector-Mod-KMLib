package kmlib.starsector.rat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import assortment_of_things.abyss.entities.hyper.AbyssalFracture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins the contract of {@link RandomAssortmentOfThingsMatcher#isAbyssalFracture}:
 * an entity is a fracture only when RAT is enabled and the entity's custom plugin
 * is an Abyssal Fracture. The mod-enabled gate is what makes the dependency
 * optional, so the disabled case is pinned alongside the positive match. The
 * cases live in a {@link Nested} group so the suite reports as a per-method tree;
 * the shared mock builders stay on the outer class.
 */
final class RandomAssortmentOfThingsMatcherTest {

    private static final String RAT_MOD_ID = "assortment_of_things";

    @Nested
    class IsAbyssalFracture {
        @Test
        void returns_false_for_a_null_entity() {
            // Null short-circuits before the mod-enabled check, so RAT state is
            // irrelevant and Global is never consulted.
            assertThat(RandomAssortmentOfThingsMatcher.isAbyssalFracture(null)).isFalse();
        }

        @Test
        void returns_false_when_rat_disabled() {
            var entityMock = buildEntityWithPlugin(mock(AbyssalFracture.class));
            try (MockedStatic<Global> globalMock = mockStatic(Global.class)) {
                stubModEnabled(globalMock, false);

                assertThat(RandomAssortmentOfThingsMatcher.isAbyssalFracture(entityMock))
                    .isFalse();
            }
        }

        @Test
        void returns_false_when_the_plugin_is_not_a_fracture() {
            var entityMock = buildEntityWithPlugin(mock(CustomCampaignEntityPlugin.class));
            try (MockedStatic<Global> globalMock = mockStatic(Global.class)) {
                stubModEnabled(globalMock, true);

                assertThat(RandomAssortmentOfThingsMatcher.isAbyssalFracture(entityMock))
                    .isFalse();
            }
        }

        @Test
        void returns_true_for_a_fracture_plugin_when_rat_enabled() {
            var entityMock = buildEntityWithPlugin(mock(AbyssalFracture.class));
            try (MockedStatic<Global> globalMock = mockStatic(Global.class)) {
                stubModEnabled(globalMock, true);

                assertThat(RandomAssortmentOfThingsMatcher.isAbyssalFracture(entityMock))
                    .isTrue();
            }
        }
    }

    private static SectorEntityToken buildEntityWithPlugin(CustomCampaignEntityPlugin plugin) {
        var entityMock = mock(SectorEntityToken.class);
        when(entityMock.getCustomPlugin()).thenReturn(plugin);
        return entityMock;
    }

    private static void stubModEnabled(MockedStatic<Global> globalMock, boolean isEnabled) {
        var settingsMock = mock(SettingsAPI.class);
        var modManagerMock = mock(ModManagerAPI.class);
        globalMock.when(Global::getSettings).thenReturn(settingsMock);
        when(settingsMock.getModManager()).thenReturn(modManagerMock);
        when(modManagerMock.isModEnabled(RAT_MOD_ID)).thenReturn(isEnabled);
    }
}
