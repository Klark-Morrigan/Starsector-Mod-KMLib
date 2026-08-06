package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin;

import kmlib.starsector.ui.map.MapIconLayering;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the placement this library acts on: where an icon sits relative to the nebulae the widget
 * appends after its own entities. The walk down to the live widget answers nothing outside a running
 * game and goes uncovered for that reason; what is covered is the reading taken from an icon map
 * already in hand, which is where a wrong answer would send an entity in and out of its location for
 * nothing - or leave it under the fog while reporting otherwise.
 */
class MapIconLayeringProbeTest {

    @Nested
    class ReadLayeringIn {

        @Test
        void readsAnIconAfterEveryNebulaAsClear() {
            // The state a lift is trying to reach, and the one that has to be recognised or the
            // entity is taken back out on the very next frame.
            var entityMock = mock(SectorEntityToken.class);
            var icons = buildIconMapOf(buildNebulaIcon(), buildNebulaIcon(), entityMock);

            assertThat(MapIconLayeringProbe.readLayeringIn(icons, entityMock))
                .isEqualTo(MapIconLayering.CLEAR_OF_NEBULAE);
        }

        @Test
        void readsAnIconBeforeAnyNebulaAsBuried() {
            // What the widget seeds on open: the location's own entities first, its nebulae after.
            var entityMock = mock(SectorEntityToken.class);
            var icons = buildIconMapOf(entityMock, buildNebulaIcon(), buildNebulaIcon());

            assertThat(MapIconLayeringProbe.readLayeringIn(icons, entityMock))
                .isEqualTo(MapIconLayering.BURIED_UNDER_NEBULAE);
        }

        @Test
        void readsAnIconWithOneNebulaStillAfterItAsBuried() {
            // Being past most of the fog is being under it. A rule keyed on the first nebula rather
            // than the last would call this cleared and stop lifting with the icon half buried.
            var entityMock = mock(SectorEntityToken.class);
            var icons = buildIconMapOf(buildNebulaIcon(), entityMock, buildNebulaIcon());

            assertThat(MapIconLayeringProbe.readLayeringIn(icons, entityMock))
                .isEqualTo(MapIconLayering.BURIED_UNDER_NEBULAE);
        }

        @Test
        void readsAnIconInAMapHoldingNoNebulaeAsClear() {
            // A sector with no systemwide nebulae, and the intel visor's own map. There is no fog
            // over this icon and no move that could improve it, so a lift would be pure churn.
            var entityMock = mock(SectorEntityToken.class);
            var icons = buildIconMapOf(mock(SectorEntityToken.class), entityMock);

            assertThat(MapIconLayeringProbe.readLayeringIn(icons, entityMock))
                .isEqualTo(MapIconLayering.CLEAR_OF_NEBULAE);
        }

        @Test
        void readsAnEntityWithNoIconAtAllAsUnreadable() {
            // The advance between putting the entity back and the frame that seeds an icon for it.
            // Reading that as buried would start a second lift on top of the one just finished.
            var icons = buildIconMapOf(buildNebulaIcon());

            assertThat(MapIconLayeringProbe.readLayeringIn(icons, mock(SectorEntityToken.class)))
                .isEqualTo(MapIconLayering.UNREADABLE);
        }

        @Test
        void ignoresATerrainThatIsNotANebula() {
            // Slipstreams, hyperspace and any mod's own terrain share the icon map and the terrain
            // tag. Only what the map draws as fog decides this, so only the nebula plugin counts.
            var entityMock = mock(SectorEntityToken.class);
            var icons = buildIconMapOf(
                entityMock,
                buildTerrainIconWith(mock(CampaignTerrainPlugin.class)));

            assertThat(MapIconLayeringProbe.readLayeringIn(icons, entityMock))
                .isEqualTo(MapIconLayering.CLEAR_OF_NEBULAE);
        }
    }

    // Insertion-ordered, because insertion order is the whole subject: the widget's own map is one,
    // and a reading taken from an unordered copy would be about nothing.
    private static Map<Object, Object> buildIconMapOf(Object... iconKeys) {

        var icons = new LinkedHashMap<>();
        for (var iconKey : iconKeys) {
            icons.put(iconKey, new Object());
        }
        return icons;
    }

    private static CampaignTerrainAPI buildNebulaIcon() {
        return buildTerrainIconWith(mock(NebulaTerrainPlugin.class));
    }

    private static CampaignTerrainAPI buildTerrainIconWith(CampaignTerrainPlugin plugin) {

        var terrainMock = mock(CampaignTerrainAPI.class);

        when(terrainMock.getPlugin())
            .thenReturn(plugin);

        return terrainMock;
    }
}
