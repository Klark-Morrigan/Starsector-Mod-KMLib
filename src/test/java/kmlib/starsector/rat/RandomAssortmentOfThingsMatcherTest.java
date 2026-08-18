package kmlib.starsector.rat;

import com.fs.starfarer.api.campaign.CustomCampaignEntityPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import assortment_of_things.abyss.entities.hyper.AbyssalFracture;

import static kmlib.starsector.rat.StubbedModIds.RANDOM_ASSORTMENT_OF_THINGS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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

            ModStateScopes.runWithModEnabled(RANDOM_ASSORTMENT_OF_THINGS, false, () ->
                assertThat(RandomAssortmentOfThingsMatcher.isAbyssalFracture(entityMock))
                    .isFalse());
        }

        @Test
        void returns_false_when_the_plugin_is_not_a_fracture() {
            var entityMock = buildEntityWithPlugin(mock(CustomCampaignEntityPlugin.class));

            ModStateScopes.runWithModEnabled(RANDOM_ASSORTMENT_OF_THINGS, true, () ->
                assertThat(RandomAssortmentOfThingsMatcher.isAbyssalFracture(entityMock))
                    .isFalse());
        }

        @Test
        void returns_true_for_a_fracture_plugin_when_rat_enabled() {
            var entityMock = buildEntityWithPlugin(mock(AbyssalFracture.class));

            ModStateScopes.runWithModEnabled(RANDOM_ASSORTMENT_OF_THINGS, true, () ->
                assertThat(RandomAssortmentOfThingsMatcher.isAbyssalFracture(entityMock))
                    .isTrue());
        }
    }

    private static SectorEntityToken buildEntityWithPlugin(CustomCampaignEntityPlugin plugin) {
        var entityMock = mock(SectorEntityToken.class);
        when(entityMock.getCustomPlugin()).thenReturn(plugin);
        return entityMock;
    }
}
