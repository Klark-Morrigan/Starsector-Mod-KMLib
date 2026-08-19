package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Siting colonies in a location, the way the game sites them.
 *
 * <p>Every colony read walks a location the same two ways - the economy is asked what it lists
 * there, and the location's own entities are walked for the markets hung on them - so every
 * suite posing one has to wire the same two stubs. Stated once here so the worlds a system
 * suite, a hyperspace suite and a sector suite each pose differ in what they contain rather
 * than in how a location is put together.
 *
 * <p>Presence and registration stay separate calls, because that split is what several cases
 * are about: a colony given only {@link #placeColonies} is the off-economy shape vanilla builds
 * Galatia Academy in, and one given only {@link #listColonies} is the economy-registered shape
 * with nothing to find by walking entities.
 */
public final class ColonyPlacementFixture {

    private ColonyPlacementFixture() {
        // fixture of static wiring, no instances.
    }

    /**
     * Sites the colonies in {@code location}, each on the entity it was built with - what the
     * entity walk finds, whether or not the economy also lists them.
     */
    public static void placeColonies(LocationAPI location, MarketAPI... colonies) {

        // Every entity is read off its colony before the stubbing opens, so calling a mock does
        // not land inside a stubbing in progress.
        var entities = new ArrayList<SectorEntityToken>();

        for (var colony : colonies) {
            entities.add(colony.getPrimaryEntity());
        }
        when(location.getAllEntities())
            .thenReturn(entities);
    }

    /** Registers the colonies with the economy as sitting in {@code location}, in listing order. */
    public static void listColonies(
            EconomyAPI economyMock,
            LocationAPI location,
            MarketAPI... colonies) {

        when(economyMock.getMarkets(location))
            .thenReturn(List.of(colonies));
    }
}
