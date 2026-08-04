package kmlib.starsector.map;

import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract of {@link VisibleStars#isStarVisibleForSystem}: a system is
 * map-visible when an untagged star anchor leads into it; a hidden anchor, a
 * non-anchor jump point, an anchor leading into a different system, an anchor
 * leading nowhere, or a missing hyperspace all read as not visible. Anchors are
 * resolved to systems by the destination they lead into, never by location. The
 * cases live in a {@link Nested} group so the suite reports as a per-method tree;
 * the shared mock builders stay on the outer class.
 */
final class VisibleStarsTest {

    @Nested
    class IsStarVisibleForSystem {
        @Test
        void is_true_when_a_visible_star_anchor_leads_into_it() {
            var system = buildSystemWithId("alpha");
            var visibleStars = VisibleStars.scan(
                buildSectorWithHyperEntities(buildStarAnchorLeadingTo(system, false)));

            assertThat(visibleStars.isStarVisibleForSystem(system)).isTrue();
        }

        @Test
        void is_false_when_its_star_anchor_is_hidden_on_map() {
            var system = buildSystemWithId("alpha");
            var visibleStars = VisibleStars.scan(
                buildSectorWithHyperEntities(buildStarAnchorLeadingTo(system, true)));

            assertThat(visibleStars.isStarVisibleForSystem(system)).isFalse();
        }

        @Test
        void is_false_when_the_jump_point_is_not_a_star_anchor() {
            var visibleStars = VisibleStars.scan(buildSectorWithHyperEntities(buildNonAnchor()));

            assertThat(visibleStars.isStarVisibleForSystem(buildSystemWithId("alpha"))).isFalse();
        }

        @Test
        void is_false_when_the_only_anchor_leads_into_another_system() {
            // Resolution is by the destination system's identity, so an anchor for
            // "alpha" cannot make "beta" read as visible.
            var visibleStars = VisibleStars.scan(
                buildSectorWithHyperEntities(buildStarAnchorLeadingTo(buildSystemWithId("alpha"), false)));

            assertThat(visibleStars.isStarVisibleForSystem(buildSystemWithId("beta"))).isFalse();
        }

        @Test
        void ignores_a_star_anchor_that_leads_nowhere() {
            // A malformed anchor with no destination must drop out of the scan
            // rather than crash it or admit a phantom system.
            var visibleStars = VisibleStars.scan(
                buildSectorWithHyperEntities(buildStarAnchorLeadingTo(null, false)));

            assertThat(visibleStars.isStarVisibleForSystem(buildSystemWithId("alpha"))).isFalse();
        }

        @Test
        void is_false_when_the_sector_has_no_hyperspace() {
            // getHyperspace() defaults to null on the mock - the empty-index path.
            var visibleStars = VisibleStars.scan(mock(SectorAPI.class));

            assertThat(visibleStars.isStarVisibleForSystem(buildSystemWithId("alpha"))).isFalse();
        }

        @Test
        void is_false_for_a_null_sector() {
            var visibleStars = VisibleStars.scan(null);

            assertThat(visibleStars.isStarVisibleForSystem(buildSystemWithId("alpha"))).isFalse();
        }
    }

    private static SectorAPI buildSectorWithHyperEntities(JumpPointAPI... entities) {
        var hyperspaceMock = mock(LocationAPI.class);
        when(hyperspaceMock.getEntities(JumpPointAPI.class)).thenReturn(List.of(entities));
        var sectorMock = mock(SectorAPI.class);
        when(sectorMock.getHyperspace()).thenReturn(hyperspaceMock);
        return sectorMock;
    }

    private static JumpPointAPI buildStarAnchorLeadingTo(StarSystemAPI destination, boolean isHiddenOnMap) {
        var jumpPointMock = mock(JumpPointAPI.class);
        when(jumpPointMock.isStarAnchor()).thenReturn(true);
        when(jumpPointMock.hasTag(Tags.STAR_HIDDEN_ON_MAP)).thenReturn(isHiddenOnMap);
        when(jumpPointMock.getDestinationStarSystem()).thenReturn(destination);
        return jumpPointMock;
    }

    private static JumpPointAPI buildNonAnchor() {
        var jumpPointMock = mock(JumpPointAPI.class);
        when(jumpPointMock.isStarAnchor()).thenReturn(false);
        return jumpPointMock;
    }

    private static StarSystemAPI buildSystemWithId(String id) {
        var systemMock = mock(StarSystemAPI.class);
        when(systemMock.getId()).thenReturn(id);
        return systemMock;
    }
}
