package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTerrain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the two parts of the trace that can be held to account off the engine: what it makes of an
 * icon map it was handed, and the line it builds from the readings. The walk that finds the widget
 * reads the live tree by reflection and answers nothing outside a running game, but everything
 * downstream of it is arithmetic over a map, and that is where a wrong answer would send a layering
 * conclusion wrong.
 */
class MapIconOrderTraceTest {

    // One more than the trace names in a single line, so the description has to drop exactly one.
    private static final int ICONS_PAST_THE_CAP = 25;

    // Stands in for whatever the widget pairs with an entity. Never read - the trace reports the
    // key, since the value is the obfuscated icon object the map draws with.
    private static final String ANY_ICON = "icon";

    @Nested
    class ReadTerrainIcons {

        @Test
        void readTerrainIconsReportsATerrainsTypeAndPlugin() {

            var icons = buildIconMap(
                buildTerrainMock("slipstream", new CampaignTerrainPluginFake()));

            assertThat(MapIconOrderTrace.readTerrainIcons(icons))
                .containsExactly(
                    new TerrainIconReading(0, "slipstream", "CampaignTerrainPluginFake"));
        }

        @Test
        void readTerrainIconsCountsThePositionsOfIconsItDoesNotReport() {
            // The positions are the whole use of the reading - one map open's are compared against
            // another's to see whether an entity moved - so they have to index the widget's map
            // rather than this list. Counting only the terrain icons would renumber every one of
            // them the moment a fleet icon was seeded between two of ours.
            var icons = buildIconMap(
                mock(SectorEntityToken.class),
                buildTerrainMock("hyperspace", new CampaignTerrainPluginFake()),
                mock(SectorEntityToken.class),
                buildTerrainMock("nebula", new CampaignTerrainPluginFake()));

            assertThat(MapIconOrderTrace.readTerrainIcons(icons))
                .containsExactly(
                    new TerrainIconReading(1, "hyperspace", "CampaignTerrainPluginFake"),
                    new TerrainIconReading(3, "nebula", "CampaignTerrainPluginFake"));
        }

        @Test
        void readTerrainIconsCountsAKeyThatIsNotAnEntityAsHoldingItsSlot() {
            // The map is keyed by whatever the widget put in it, and a key this cannot read is
            // still a slot: skipping it would shift every position after it.
            var icons = buildIconMap(
                new Object(),
                buildTerrainMock("slipstream", new CampaignTerrainPluginFake()));

            assertThat(MapIconOrderTrace.readTerrainIcons(icons))
                .containsExactly(
                    new TerrainIconReading(1, "slipstream", "CampaignTerrainPluginFake"));
        }

        @Test
        void readTerrainIconsReportsATerrainTaggedEntityThatIsNoTerrain() {
            // The tag is what the widget sorts on, so an entity carrying it draws in the terrain
            // pass whatever else it is. Reporting it is what would show the tag and the type having
            // come apart, rather than leaving a slot in the order unaccounted for.
            var taggedEntityMock = mock(SectorEntityToken.class);

            when(taggedEntityMock.hasTag(Tags.TERRAIN))
                .thenReturn(true);

            assertThat(MapIconOrderTrace.readTerrainIcons(buildIconMap(taggedEntityMock)))
                .containsExactly(new TerrainIconReading(0, "untyped", "none"));
        }

        @Test
        void readTerrainIconsReportsATerrainHoldingNoPlugin() {

            assertThat(MapIconOrderTrace.readTerrainIcons(
                    buildIconMap(buildTerrainMock("slipstream", null))))
                .containsExactly(new TerrainIconReading(0, "slipstream", "none"));
        }

        @Test
        void readTerrainIconsReportsNothingForAMapHoldingNoIcons() {

            assertThat(MapIconOrderTrace.readTerrainIcons(new LinkedHashMap<>()))
                .isEmpty();
        }
    }

    @Nested
    class DescribeTerrainIcons {

        @Test
        void describeTerrainIconsKeepsTheOrderItWasGiven() {
            // The order is the whole subject: it is what decides which icon the map paints over
            // which, so a line that sorted or grouped them would describe a different map.
            var terrainIcons = List.of(
                new TerrainIconReading(0, "hyperspace", "HyperspaceTerrainPlugin"),
                new TerrainIconReading(1, "slipstream", "SectorMapLayerStarscapeTerrainPlugin"),
                new TerrainIconReading(4, "nebula", "NebulaTerrainPlugin"));

            assertThat(MapIconOrderTrace.describeTerrainIcons(terrainIcons))
                .isEqualTo("terrainIcons=3 order=["
                    + "[0] hyperspace HyperspaceTerrainPlugin, "
                    + "[1] slipstream SectorMapLayerStarscapeTerrainPlugin, "
                    + "[4] nebula NebulaTerrainPlugin]");
        }

        @Test
        void describeTerrainIconsReportsAnEmptyOrderForAMapHoldingNoTerrainIcons() {
            // A map with no terrain icons at all is a real answer rather than nothing to say: it
            // is what a layer that failed to install looks like from here.
            assertThat(MapIconOrderTrace.describeTerrainIcons(List.of()))
                .isEqualTo("terrainIcons=0 order=[]");
        }

        @Test
        void describeTerrainIconsNamesTheTotalWhenItNamesFewerIconsThanThat() {
            // The cap is what keeps one nebula-heavy sector from filling the log, and the total is
            // what stops the capped line from reading as the whole map.
            var description = MapIconOrderTrace.describeTerrainIcons(
                buildTerrainIcons(ICONS_PAST_THE_CAP));

            assertThat(description)
                .startsWith("terrainIcons=25 order=[[0] nebula ");
            assertThat(description)
                .contains("[23] nebula NebulaTerrainPlugin]");
            assertThat(description)
                .doesNotContain("[24]");
        }
    }

    private static List<TerrainIconReading> buildTerrainIcons(int iconCount) {

        var terrainIcons = new ArrayList<TerrainIconReading>();

        for (var position = 0; position < iconCount; position++) {
            terrainIcons.add(new TerrainIconReading(position, "nebula", "NebulaTerrainPlugin"));
        }
        return terrainIcons;
    }

    // Insertion-ordered, because insertion order is what the widget's own map has and what the
    // reading is about; any other map would make the positions arbitrary.
    private static Map<?, ?> buildIconMap(Object... iconKeys) {

        var icons = new LinkedHashMap<Object, Object>();

        for (var iconKey : iconKeys) {
            icons.put(iconKey, ANY_ICON);
        }
        return icons;
    }

    private static CampaignTerrainAPI buildTerrainMock(String type, CampaignTerrainPlugin plugin) {

        var terrainMock = mock(CampaignTerrainAPI.class);

        when(terrainMock.hasTag(Tags.TERRAIN))
            .thenReturn(true);
        when(terrainMock.getType())
            .thenReturn(type);
        when(terrainMock.getPlugin())
            .thenReturn(plugin);

        return terrainMock;
    }

    // A real class rather than a mock, because the reading reports the plugin's simple name and a
    // mock's is generated - an expectation written against one would pin the mocking library's
    // naming rather than this trace's.
    private static final class CampaignTerrainPluginFake extends BaseTerrain {
    }
}
