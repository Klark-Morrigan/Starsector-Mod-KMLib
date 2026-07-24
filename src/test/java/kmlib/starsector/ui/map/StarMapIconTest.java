package kmlib.starsector.ui.map;

import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the icon reconstruction that moved to KMLib off the KMU capture: it resolves the body from
 * the star anchor's destination visual (falling back to the star), classifies it into the kind that
 * sizes it, reads the anchor and the vanilla radius, and returns nothing when a system has no icon
 * to reconstruct. The per-kind vanilla sizing itself lives in {@link StarIconHitTest} and is pinned
 * there; this pins the resolution and classification around it.
 */
class StarMapIconTest {

    private static final Vector2f ANCHOR_LOCATION = new Vector2f(1200f, -800f);

    // A system whose star-anchor jump point resolves to the given body, placed at the fixed anchor.
    private static StarSystemAPI systemWithJumpPointBody(SectorEntityToken body) {
        var anchorMock = mock(JumpPointAPI.class);
        when(anchorMock.getLocation()).thenReturn(ANCHOR_LOCATION);
        when(anchorMock.getDestinationVisualEntity()).thenReturn(body);
        var systemMock = mock(StarSystemAPI.class);
        when(systemMock.getHyperspaceAnchor()).thenReturn(anchorMock);
        return systemMock;
    }

    // A planet body carrying the given spec flags and radius.
    private static PlanetAPI planet(
            boolean isStar, boolean isNebulaCentre, boolean isBlackHole, float radius) {
        var specMock = mock(PlanetSpecAPI.class);
        when(specMock.isNebulaCenter()).thenReturn(isNebulaCentre);
        when(specMock.isBlackHole()).thenReturn(isBlackHole);
        when(specMock.getScaleMultMapIcon()).thenReturn(1f);
        var bodyMock = mock(PlanetAPI.class);
        when(bodyMock.getSpec()).thenReturn(specMock);
        when(bodyMock.isStar()).thenReturn(isStar);
        when(bodyMock.getRadius()).thenReturn(radius);
        return bodyMock;
    }

    @Nested
    class ReconstructFor {

        @Test
        void reconstructForClassifiesAStar() {
            var icon = StarMapIcon.reconstructFor(
                    systemWithJumpPointBody(planet(true, false, false, 100f)));

            assertThat(icon.bodyKind()).isEqualTo(StarIconBodyKind.STAR);
        }

        @Test
        void reconstructForClassifiesANebulaCentreOverItsStarFlag() {
            // A nebula centre may also flag as a star; the nebula classification wins, since it is
            // what changes the sizing.
            var icon = StarMapIcon.reconstructFor(
                    systemWithJumpPointBody(planet(true, true, false, 0f)));

            assertThat(icon.bodyKind()).isEqualTo(StarIconBodyKind.NEBULA_CENTRE);
        }

        @Test
        void reconstructForClassifiesABlackHole() {
            var icon = StarMapIcon.reconstructFor(
                    systemWithJumpPointBody(planet(true, false, true, 100f)));

            assertThat(icon.bodyKind()).isEqualTo(StarIconBodyKind.BLACK_HOLE);
        }

        @Test
        void reconstructForClassifiesANonStarBodyAsOther() {
            var icon = StarMapIcon.reconstructFor(
                    systemWithJumpPointBody(planet(false, false, false, 100f)));

            assertThat(icon.bodyKind()).isEqualTo(StarIconBodyKind.OTHER);
        }

        @Test
        void reconstructForReadsTheAnchorAndVanillaRadius() {
            var icon = StarMapIcon.reconstructFor(
                    systemWithJumpPointBody(planet(true, false, false, 100f)));

            assertThat(icon.anchor().x).isEqualTo(ANCHOR_LOCATION.x);
            assertThat(icon.anchor().y).isEqualTo(ANCHOR_LOCATION.y);
            // A star at radius 100, scale 1: 5 * 100 * 0.75 = 375.
            assertThat(icon.worldRadius()).isEqualTo(375f, within(1e-3f));
        }

        @Test
        void reconstructForFallsBackToTheStarWhenTheAnchorIsNotAJumpPoint() {
            // A bare (non-jump-point) anchor has no destination visual, so the system's own star
            // sizes the icon.
            var star = planet(true, false, false, 100f);
            var anchorMock = mock(SectorEntityToken.class);
            when(anchorMock.getLocation()).thenReturn(ANCHOR_LOCATION);
            var systemMock = mock(StarSystemAPI.class);
            when(systemMock.getHyperspaceAnchor()).thenReturn(anchorMock);
            when(systemMock.getStar()).thenReturn(star);

            var icon = StarMapIcon.reconstructFor(systemMock);

            assertThat(icon.bodyKind()).isEqualTo(StarIconBodyKind.STAR);
            assertThat(icon.worldRadius()).isEqualTo(375f, within(1e-3f));
        }

        @Test
        void reconstructForReturnsNullWithoutAnAnchor() {
            var systemMock = mock(StarSystemAPI.class);
            when(systemMock.getHyperspaceAnchor()).thenReturn(null);

            assertThat(StarMapIcon.reconstructFor(systemMock)).isNull();
        }

        @Test
        void reconstructForReturnsNullWhenNoBodyResolves() {
            // No jump-point destination visual and no star: nothing to size an icon from.
            var anchorMock = mock(JumpPointAPI.class);
            when(anchorMock.getLocation()).thenReturn(ANCHOR_LOCATION);
            when(anchorMock.getDestinationVisualEntity()).thenReturn(null);
            var systemMock = mock(StarSystemAPI.class);
            when(systemMock.getHyperspaceAnchor()).thenReturn(anchorMock);
            when(systemMock.getStar()).thenReturn(null);

            assertThat(StarMapIcon.reconstructFor(systemMock)).isNull();
        }
    }
}
