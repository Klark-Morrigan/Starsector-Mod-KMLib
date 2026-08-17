package kmlib.testfixtures.starsector.ui.map.presence;

import kmlib.starsector.ui.map.presence.CampaignMinimap;

/**
 * A {@link CampaignMinimap} that is drawn or not because a test said so, standing in for the live
 * mod and settings reads wherever the question is only "and what does the caller do then?".
 *
 * <p>Ships in the main jar rather than a test source set, so a consuming mod's own tests can drive
 * their behaviour through this seam without rebuilding the fixture - which is the point of the
 * role being here at all, since what a minimap changes is decided in the mods that answer to one.
 */
public final class CampaignMinimapFake implements CampaignMinimap {

    private boolean isReplacingRadar;

    @Override
    public boolean isReplacingRadar() {
        return isReplacingRadar;
    }

    /**
     * Puts a minimap in the campaign radar's place, as if a mod were enabled with its own minimap
     * switched on.
     */
    public void replaceRadarWithMinimap() {
        isReplacingRadar = true;
    }

    /**
     * Leaves the campaign radar in place, as if no mod supplied a minimap.
     */
    public void restoreRadar() {
        isReplacingRadar = false;
    }
}
