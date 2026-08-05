package kmlib.starsector.ui.map.probes;

/**
 * One terrain icon as the map widget holds it: where it sits in the widget's icon map, what its
 * entity reports as its terrain type, and which plugin draws it.
 *
 * <p>The three travel together because none of them identifies an icon on its own. The position is
 * what the draw order is, and says nothing about what is being drawn; the type is what the engine
 * decides to draw with, and several icons share one; the plugin is the only half that names a
 * particular piece of code, and it is exactly what a type reported for the engine's benefit hides.
 *
 * <p>A plain carrier for one reading, not a value held across frames - the widget rebuilds its icon
 * map whenever a map is opened, so a reading kept past the frame it was taken in describes a map
 * that may no longer exist.
 *
 * @param position       the icon's index in the widget's whole icon map, non-terrain icons
 *                       included, since those occupy slots between the terrain ones
 * @param terrainType    the type the entity reports, which is what the engine gates drawing on
 * @param pluginTypeName the simple name of the plugin class that draws it
 */
record TerrainIconReading(
    int position,
    String terrainType,
    String pluginTypeName) {
}
