package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract of {@link LocationStructures#readStructuresIn} - the search itself, at the
 * layer that owns it. The one method's cases live in a {@link Nested} group so the suite reports
 * as a per-method tree; the shared builders stay on the outer class.
 *
 * <p>Posed against a plain {@link LocationAPI} rather than a star system, which is the point of
 * the search sitting here: nothing about finding what is built in a place is system-specific, and
 * a caller asking about hyperspace gets the same answers.
 */
final class LocationStructuresTest {

    // The six entities vanilla ships under the objective tag: the three installations and the
    // three improvised variants, which is what the tag has to admit for the selection rule to be
    // the tag rather than a list of the first three ids.
    private static final List<String> VANILLA_STRUCTURE_TYPES = List.of(
        "comm_relay",
        "nav_buoy",
        "sensor_array",
        "comm_relay_makeshift",
        "nav_buoy_makeshift",
        "sensor_array_makeshift");

    @Nested
    class ReadStructuresIn {

        @Test
        void yieldsEveryVanillaStructureTheTagAdmits() {
            // The makeshift variants carry the tag as the full installations do, so a selection
            // keyed on the three whole-installation IDs would drop half of what is built.
            var locationMock = mock(LocationAPI.class);

            placeStructuresIn(locationMock, VANILLA_STRUCTURE_TYPES);

            assertThat(readTypesOf(LocationStructures.readStructuresIn(locationMock)))
                .containsExactly(
                    "comm_relay",
                    "nav_buoy",
                    "sensor_array",
                    "comm_relay_makeshift",
                    "nav_buoy_makeshift",
                    "sensor_array_makeshift");
        }

        @Test
        void yieldsAModdedStructureTheTagAdmits() {
            // A mod adding a fourth kind joins the set by tagging its own entity, and that is the
            // whole of what joining takes.
            var locationMock = mock(LocationAPI.class);

            placeStructuresIn(locationMock, List.of("kmu_listening_post"));

            assertThat(readTypesOf(LocationStructures.readStructuresIn(locationMock)))
                .containsExactly("kmu_listening_post");
        }

        @Test
        void yieldsAnUndiscoveredStructureLikeAnyOther() {
            // Nothing is filtered here: discovery is a fact a caller reads off the structure and
            // applies to its own purpose, and a search that withheld one would leave a caller
            // recording observations unable to see what it was meant to be recording.
            var locationMock = mock(LocationAPI.class);
            var entityMock = buildStructureEntity("comm_relay");

            when(entityMock.isDiscoverable())
                .thenReturn(true);

            when(locationMock.getEntitiesWithTag(Tags.OBJECTIVE))
                .thenReturn(List.of(entityMock));

            assertThat(LocationStructures.readStructuresIn(locationMock))
                .containsExactly(new Structure(entityMock));
        }

        @Test
        void skipsAnEntityTheLocationListsAsNothing() {
            // Nothing to stand a structure on, and the record refuses one - so the listing is
            // filtered here rather than letting one bad entry take out the whole read.
            var locationMock = mock(LocationAPI.class);
            var entityMock = buildStructureEntity("comm_relay");

            when(locationMock.getEntitiesWithTag(Tags.OBJECTIVE))
                .thenReturn(Arrays.asList(entityMock, null));

            assertThat(LocationStructures.readStructuresIn(locationMock))
                .containsExactly(new Structure(entityMock));
        }

        @Test
        void returnsEmptyForALocationHoldingNoStructures() {
            // Empty rather than null: a place with nothing built in it is an ordinary answer, and
            // every caller listing structures would otherwise guard against it separately.
            var locationMock = mock(LocationAPI.class);

            placeStructuresIn(locationMock, List.of());

            assertThat(LocationStructures.readStructuresIn(locationMock))
                .isEmpty();
        }

        @Test
        void returnsEmptyForANullLocation() {
            assertThat(LocationStructures.readStructuresIn(null))
                .isEmpty();
        }

        @Test
        void returnsEmptyWhenTheLocationReportsNoTaggedEntityList() {

            var locationMock = mock(LocationAPI.class);

            when(locationMock.getEntitiesWithTag(Tags.OBJECTIVE))
                .thenReturn(null);

            assertThat(LocationStructures.readStructuresIn(locationMock))
                .isEmpty();
        }
    }

    // What each found structure says it is, which is what an assertion about the selection reads:
    // the entity type is what identifies one kind of structure from another.
    private static List<String> readTypesOf(List<Structure> structures) {

        var types = new ArrayList<String>();

        for (var structure : structures) {
            types.add(structure.entity().getCustomEntityType());
        }
        return types;
    }

    // Tags each type's entity into the location, in the order given - the listing the tag lookup
    // hands back.
    private static void placeStructuresIn(LocationAPI locationMock, List<String> entityTypes) {

        // Every entity finishes its own stubbing before the location's opens, so the two do not
        // nest into an unfinished-stubbing error.
        var entities = new ArrayList<SectorEntityToken>();

        for (var entityType : entityTypes) {
            entities.add(buildStructureEntity(entityType));
        }
        when(locationMock.getEntitiesWithTag(Tags.OBJECTIVE))
            .thenReturn(entities);
    }

    // An entity answering to the type that names its kind. Nothing else is wired: what the search
    // decides is which entities come back, and every fact about one is the structure's own.
    private static SectorEntityToken buildStructureEntity(String entityType) {

        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.getCustomEntityType())
            .thenReturn(entityType);

        return entityMock;
    }
}
