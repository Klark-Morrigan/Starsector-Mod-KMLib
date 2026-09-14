package kmlib.mods.nexerelin;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The sector an adapter in this package resolves an owner against.
 *
 * <p>Every routine deferred to here is handed factions rather than IDs - it reads that mod's own
 * configuration, tariffs and colours off them - so each adapter looks the owner up and declines an
 * ID nothing answers to. That makes "a sector that knows one faction and nothing else" the shape
 * every suite here poses, and a suite spelling it out itself is free to pose a sector that answers
 * every ID and still pass the case about the one it does not.
 *
 * <p>Package-private: what it builds is general, but the reason it exists is the lookup these
 * adapters share, and a fixture offered wider would be inviting suites with their own faction
 * arrangements to inherit this one's.
 */
final class SectorFactionFixture {

    private SectorFactionFixture() {
        // fixture of static builders, no instances.
    }

    /**
     * A sector that knows the named faction and nothing else, so an ID it was not given reads as an
     * owner that does not exist.
     *
     * @param factionId the one owner the sector can answer for
     * @return the sector to pose the adapter against
     */
    static SectorAPI buildSectorHolding(String factionId) {

        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getFaction(factionId))
            .thenReturn(mock(FactionAPI.class));

        return sectorMock;
    }
}
