package kmlib.testfixtures.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Posing star systems the way the sector holds them: an id, a place in hyperspace, and the two
 * entities the engine builds a system around.
 *
 * <p>Stated once here because a system is the subject of nearly every suite over the sector, and
 * each had otherwise written the same two stubs for itself - so worlds differed in how a system was
 * put together rather than in what the case was about. Shipped from KMLib so a consuming mod's
 * suites pose one the same way.
 *
 * <p>Identity and placement are separate calls, which is what keeps either from carrying the
 * other's arguments. A read over ids never looks at where a system sits, and a case about it should
 * not have to invent coordinates to say so; {@link #placeSystemAt} is there for the reads that do.
 *
 * <p>The centre and the anchor are stated only where a case is about telling two systems apart,
 * since a system id is not unique across a modded sector and those are the arms that separate a
 * colliding pair. A system built without them carries neither, which is a shape the sector holds
 * too.
 *
 * <p>Final class with a private constructor: fixture of static wiring, no instances.
 */
public final class StarSystemFixture {

    private StarSystemFixture() {
        // fixture of static wiring, no instances.
    }

    /**
     * A system answering to an id and nothing else - what a read keyed on the id alone needs, and
     * no more than that.
     */
    public static StarSystemAPI buildSystem(String systemId) {

        var systemMock = mock(StarSystemAPI.class);

        when(systemMock.getId())
            .thenReturn(systemId);

        return systemMock;
    }

    /**
     * A system answering to an id and sitting at a hyperspace position - the pair every read over
     * the sector's layout takes, and the commonest shape a case poses.
     */
    public static StarSystemAPI buildSystemAt(String systemId, float x, float y) {
        return placeSystemAt(buildSystem(systemId), x, y);
    }

    /**
     * A system carrying the three arms a key is read off: its id, and the ids of the entities it is
     * built around. Either entity id may be null, which poses a system that carries no such entity.
     *
     * <p>What a case about two systems sharing an id poses, that being a pair the sector really
     * holds and one no other fact separates.
     */
    public static StarSystemAPI buildKeyedSystem(
            String systemId,
            String centreEntityId,
            String anchorEntityId) {

        // Both entities finish their own wiring before the system's opens, since building one
        // inside a when(...) call leaves Mockito's stubbing half finished.
        var centreMock = buildEntity(centreEntityId);
        var anchorMock = buildEntity(anchorEntityId);
        var systemMock = buildSystem(systemId);

        when(systemMock.getCenter())
            .thenReturn(centreMock);
        when(systemMock.getHyperspaceAnchor())
            .thenReturn(anchorMock);

        return systemMock;
    }

    /**
     * Puts an already posed system at a hyperspace position, and answers it - so a case can state
     * where a system sits on top of whatever else it was built to carry, and can move one between
     * two polls of a read that watches for motion.
     */
    public static StarSystemAPI placeSystemAt(StarSystemAPI system, float x, float y) {

        when(system.getLocation())
            .thenReturn(new Vector2f(x, y));

        return system;
    }

    /**
     * Gives an already posed system the name a surface titles it by, and answers it. Vanilla
     * composes that name from the system's own name plus its type, so a case about what is said
     * about a system states it rather than leaving the system nameless.
     */
    public static StarSystemAPI nameSystem(StarSystemAPI system, String name) {

        when(system.getName())
            .thenReturn(name);

        return system;
    }

    /**
     * A sector listing the given systems in the order they are passed, which is the order every
     * read over the sector walks them in.
     */
    public static SectorAPI buildSectorOf(StarSystemAPI... systems) {

        var systemList = List.of(systems);
        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getStarSystems())
            .thenReturn(systemList);

        return sectorMock;
    }

    // An entity answering to an id, or nothing at all where the caller stated no id - which poses a
    // system that carries no such entity, rather than one carrying a nameless entity.
    private static SectorEntityToken buildEntity(String entityId) {

        if (entityId == null) {
            return null;
        }
        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.getId())
            .thenReturn(entityId);

        return entityMock;
    }
}
