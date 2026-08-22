package kmlib.mods.rat;

import com.fs.starfarer.api.campaign.CustomCampaignEntityPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import assortment_of_things.abyss.entities.hyper.AbyssalFracture;

import static kmlib.testfixtures.starsector.settings.StubbedModIds.RANDOM_ASSORTMENT_OF_THINGS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract of {@link RandomAssortmentOfThingsMatcher#hasAbyssalFracture}:
 * a system holds a fracture only when RAT is enabled and some entity in it carries
 * an Abyssal Fracture as its custom plugin. The mod-enabled gate is what makes the
 * dependency optional, so the disabled case is pinned alongside the positive match.
 * The cases live in a {@link Nested} group so the suite reports as a per-method
 * tree; the shared mock builders stay on the outer class.
 */
final class RandomAssortmentOfThingsMatcherTest {

    @Nested
    class HasAbyssalFracture {
        @Test
        void returns_false_for_a_null_system() {
            // Null short-circuits before the mod-enabled check, so RAT state is
            // irrelevant and Global is never consulted.
            assertThat(RandomAssortmentOfThingsMatcher.hasAbyssalFracture(null)).isFalse();
        }

        @Test
        void returns_false_when_rat_disabled() {
            var systemMock = buildSystemWithEntities(
                buildEntityWithPlugin(mock(AbyssalFracture.class)));

            ModStateScopes.runWithModEnabled(RANDOM_ASSORTMENT_OF_THINGS, false, () ->
                assertThat(RandomAssortmentOfThingsMatcher.hasAbyssalFracture(systemMock))
                    .isFalse());
        }

        @Test
        void returns_false_when_the_system_holds_no_fracture() {
            var systemMock = buildSystemWithEntities(
                buildEntityWithPlugin(mock(CustomCampaignEntityPlugin.class)));

            ModStateScopes.runWithModEnabled(RANDOM_ASSORTMENT_OF_THINGS, true, () ->
                assertThat(RandomAssortmentOfThingsMatcher.hasAbyssalFracture(systemMock))
                    .isFalse());
        }

        @Test
        void returns_false_for_an_empty_system() {
            var systemMock = buildSystemWithEntities();

            ModStateScopes.runWithModEnabled(RANDOM_ASSORTMENT_OF_THINGS, true, () ->
                assertThat(RandomAssortmentOfThingsMatcher.hasAbyssalFracture(systemMock))
                    .isFalse());
        }

        @Test
        void returns_true_when_a_fracture_sits_behind_other_entities() {
            // The fracture is not the first entity, so this fails on a read that
            // settles on whatever the system lists first rather than scanning.
            var systemMock = buildSystemWithEntities(
                buildEntityWithPlugin(mock(CustomCampaignEntityPlugin.class)),
                buildEntityWithPlugin(mock(AbyssalFracture.class)));

            ModStateScopes.runWithModEnabled(RANDOM_ASSORTMENT_OF_THINGS, true, () ->
                assertThat(RandomAssortmentOfThingsMatcher.hasAbyssalFracture(systemMock))
                    .isTrue());
        }
    }

    private static SectorEntityToken buildEntityWithPlugin(CustomCampaignEntityPlugin plugin) {
        var entityMock = mock(SectorEntityToken.class);
        when(entityMock.getCustomPlugin()).thenReturn(plugin);
        return entityMock;
    }

    private static StarSystemAPI buildSystemWithEntities(SectorEntityToken... entities) {
        var systemMock = mock(StarSystemAPI.class);
        when(systemMock.getAllEntities()).thenReturn(List.of(entities));
        return systemMock;
    }
}
