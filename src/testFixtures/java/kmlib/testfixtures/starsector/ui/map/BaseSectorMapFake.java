package kmlib.testfixtures.starsector.ui.map;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.ui.SectorMapAPI;

/**
 * The half of a sector map that no fixture stands for: the two entity lookups the published
 * interface requires and nothing driven against a map takes. Published as a fixture variant so both KMLib's and
 * consuming mods' tests build maps that answer the same way.
 *
 * <p>They raise rather than answering null, so a subject that strayed into one fails here instead of
 * carrying on against a fixture that cannot stand for what it asked.
 *
 * <p>Sits above the fixtures for each half of a map rather than beside any one of them, for the same
 * reason the placement vocabulary sits above the map packages it is shared by: what a fixture models
 * is where they differ - being recognised, being placed on screen, being furnished with a row - and
 * this is the one part all of them answer alike.
 */
public abstract class BaseSectorMapFake implements SectorMapAPI {

    @Override
    public final SectorEntityToken getConstellationLabelEntity(Constellation constellation) {
        throw new UnsupportedOperationException("A sector map fixture holds no entities.");
    }

    @Override
    public final SectorEntityToken getIntelIconEntity(IntelInfoPlugin intel) {
        throw new UnsupportedOperationException("A sector map fixture holds no entities.");
    }
}
