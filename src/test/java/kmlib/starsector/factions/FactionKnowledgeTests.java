package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SpecialItemPlugin;
import com.fs.starfarer.api.campaign.impl.items.BlueprintProviderItem;
import com.fs.starfarer.api.campaign.impl.items.ModSpecItemPlugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

final class FactionKnowledgeTests {

    private static final List<String> NONE_PROVIDED = null;

    private FactionAPI factionMock;

    // A blueprint item is a special-item plugin that also provides IDs, so the mock carries both shapes.
    private static SpecialItemPlugin buildBlueprint(
            List<String> ships,
            List<String> weapons,
            List<String> fighters,
            List<String> industries) {

        var pluginMock = mock(SpecialItemPlugin.class, withSettings().extraInterfaces(BlueprintProviderItem.class));
        var providerMock = (BlueprintProviderItem) pluginMock;

        when(providerMock.getProvidedShips())
            .thenReturn(ships);
        when(providerMock.getProvidedWeapons())
            .thenReturn(weapons);
        when(providerMock.getProvidedFighters())
            .thenReturn(fighters);
        when(providerMock.getProvidedIndustries())
            .thenReturn(industries);

        return pluginMock;
    }

    private static SpecialItemPlugin buildModSpec(String modId) {

        var pluginMock = mock(ModSpecItemPlugin.class);

        when(pluginMock.getModId())
            .thenReturn(modId);

        return pluginMock;
    }

    @BeforeEach
    void setUp() {

        factionMock = mock(FactionAPI.class);
    }

    @Nested
    class IsEverythingTaughtKnown {

        @Test
        void isTrueForABlueprintWhoseEveryIdIsKnown() {

            when(factionMock.knowsShip("onslaught"))
                .thenReturn(true);
            when(factionMock.knowsWeapon("hellbore"))
                .thenReturn(true);
            when(factionMock.knowsFighter("talon_wing"))
                .thenReturn(true);
            when(factionMock.knowsIndustry("orbitalworks"))
                .thenReturn(true);

            var blueprint = buildBlueprint(
                List.of("onslaught"),
                List.of("hellbore"),
                List.of("talon_wing"),
                List.of("orbitalworks"));

            assertThat(FactionKnowledge.isEverythingTaughtKnown(factionMock, blueprint))
                .isTrue();
        }

        @Test
        void isFalseForABlueprintWithOneUnknownId() {

            when(factionMock.knowsShip("onslaught"))
                .thenReturn(true);

            var blueprint = buildBlueprint(List.of("onslaught"), List.of("hellbore"), NONE_PROVIDED, NONE_PROVIDED);

            assertThat(FactionKnowledge.isEverythingTaughtKnown(factionMock, blueprint))
                .isFalse();
        }

        @Test
        void isTrueForABlueprintProvidingNothing() {

            var blueprint = buildBlueprint(NONE_PROVIDED, NONE_PROVIDED, NONE_PROVIDED, NONE_PROVIDED);

            assertThat(FactionKnowledge.isEverythingTaughtKnown(factionMock, blueprint))
                .isTrue();
        }

        @Test
        void isTrueForAModSpecWhoseHullmodIsKnown() {

            when(factionMock.knowsHullMod("hardened_shields"))
                .thenReturn(true);

            assertThat(FactionKnowledge.isEverythingTaughtKnown(factionMock, buildModSpec("hardened_shields")))
                .isTrue();
        }

        @Test
        void isFalseForAModSpecWhoseHullmodIsUnknown() {

            assertThat(FactionKnowledge.isEverythingTaughtKnown(factionMock, buildModSpec("hardened_shields")))
                .isFalse();
        }

        @Test
        void isFalseForAnItemThatTeachesNothing() {

            assertThat(FactionKnowledge.isEverythingTaughtKnown(factionMock, mock(SpecialItemPlugin.class)))
                .isFalse();
        }

        @Test
        void isFalseForNoItem() {

            assertThat(FactionKnowledge.isEverythingTaughtKnown(factionMock, null))
                .isFalse();
        }
    }
}
