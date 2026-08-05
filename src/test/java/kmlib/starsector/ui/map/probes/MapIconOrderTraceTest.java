package kmlib.starsector.ui.map.probes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one part of the trace that can be held to account off the engine: the line it builds
 * from readings it was handed. The walk that takes them reads the live widget tree by reflection
 * and answers nothing outside a running game, but the line is what a reader draws a layering
 * conclusion from, so an order it reported wrongly - or a truncation it hid - is what would send
 * that conclusion wrong.
 */
class MapIconOrderTraceTest {

    // One more than the trace names in a single line, so the description has to drop exactly one.
    private static final int ICONS_PAST_THE_CAP = 25;

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
}
