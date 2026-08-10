package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.CustomEntitySpecAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins {@link EntityMapIcons#resolveMapIcon} - the single rule every icon-drawing surface shares.
 * The branch is the substance of it: vanilla stores a planet's glyph on its planet spec and every
 * other entity's on a custom-entity spec, so both arms are asserted head-on, along with the
 * blank-is-absent rule that decides when there is no icon to draw at all.
 *
 * <p>Cases live in a {@link Nested} group named for the method under test, so the suite reports as
 * a per-method tree.
 */
final class EntityMapIconsTest {

    @Nested
    class ResolveMapIcon {

        @Test
        void resolveMapIconReadsAPlanetsOwnSpec() {

            var planetMock = mock(PlanetAPI.class);
            var planetSpecMock = mock(PlanetSpecAPI.class);

            when(planetMock.getSpec())
                .thenReturn(planetSpecMock);
            when(planetSpecMock.getIconTexture())
                .thenReturn("graphics/warroom/icon_planet.png");
            when(planetSpecMock.getIconColor())
                .thenReturn(new Color(120, 200, 90));

            assertThat(EntityMapIcons.resolveMapIcon(planetMock))
                .contains(new EntityMapIcon(
                    "graphics/warroom/icon_planet.png",
                    new Color(120, 200, 90)));
        }

        @Test
        void resolveMapIconReadsACustomEntitysOwnSpec() {

            var stationMock = mock(SectorEntityToken.class);
            var entitySpecMock = mock(CustomEntitySpecAPI.class);

            when(stationMock.getCustomEntitySpec())
                .thenReturn(entitySpecMock);
            when(entitySpecMock.getIconName())
                .thenReturn("graphics/icons/station0.png");
            when(entitySpecMock.getIconColor())
                .thenReturn(new Color(200, 200, 255));

            assertThat(EntityMapIcons.resolveMapIcon(stationMock))
                .contains(new EntityMapIcon(
                    "graphics/icons/station0.png",
                    new Color(200, 200, 255)));
        }

        @Test
        void resolveMapIconIsEmptyForAnEntityWithNoSpec() {
            // A plain token that is neither a planet nor a custom entity carries no icon spec at
            // all, which is the "nothing authored" case rather than a malformed one.
            var entityMock = mock(SectorEntityToken.class);

            when(entityMock.getCustomEntitySpec())
                .thenReturn(null);

            assertThat(EntityMapIcons.resolveMapIcon(entityMock))
                .isEmpty();
        }

        @Test
        void resolveMapIconIsEmptyForAPlanetWithNoSpec() {

            var planetMock = mock(PlanetAPI.class);

            when(planetMock.getSpec())
                .thenReturn(null);

            assertThat(EntityMapIcons.resolveMapIcon(planetMock))
                .isEmpty();
        }

        @Test
        void resolveMapIconIsEmptyForANullEntity() {
            assertThat(EntityMapIcons.resolveMapIcon(null))
                .isEmpty();
        }

        @Test
        void resolveMapIconIsEmptyForABlankPlanetIconPath() {
            // An authored-but-empty path reads as no icon, so a whitespace path collapses to empty
            // rather than pointing a caller at a missing sprite.
            var planetMock = mock(PlanetAPI.class);
            var planetSpecMock = mock(PlanetSpecAPI.class);

            when(planetMock.getSpec())
                .thenReturn(planetSpecMock);
            when(planetSpecMock.getIconTexture())
                .thenReturn("   ");

            assertThat(EntityMapIcons.resolveMapIcon(planetMock))
                .isEmpty();
        }

        @Test
        void resolveMapIconIsEmptyForABlankCustomEntityIconPath() {

            var stationMock = mock(SectorEntityToken.class);
            var entitySpecMock = mock(CustomEntitySpecAPI.class);

            when(stationMock.getCustomEntitySpec())
                .thenReturn(entitySpecMock);
            when(entitySpecMock.getIconName())
                .thenReturn(null);

            assertThat(EntityMapIcons.resolveMapIcon(stationMock))
                .isEmpty();
        }

        @Test
        void resolveMapIconTrimsTheAuthoredPath() {

            var stationMock = mock(SectorEntityToken.class);
            var entitySpecMock = mock(CustomEntitySpecAPI.class);

            when(stationMock.getCustomEntitySpec())
                .thenReturn(entitySpecMock);
            when(entitySpecMock.getIconName())
                .thenReturn("  graphics/icons/relay.png  ");
            when(entitySpecMock.getIconColor())
                .thenReturn(Color.WHITE);

            assertThat(EntityMapIcons.resolveMapIcon(stationMock))
                .contains(new EntityMapIcon("graphics/icons/relay.png", Color.WHITE));
        }

        @Test
        void resolveMapIconCarriesAnUncolouredGlyph() {
            // A spec authoring no colour still has an icon: the glyph's own pixels carry it, and
            // the draw reads a null tint as "as authored". Only the path decides presence.
            var stationMock = mock(SectorEntityToken.class);
            var entitySpecMock = mock(CustomEntitySpecAPI.class);

            when(stationMock.getCustomEntitySpec())
                .thenReturn(entitySpecMock);
            when(entitySpecMock.getIconName())
                .thenReturn("graphics/icons/buoy.png");
            when(entitySpecMock.getIconColor())
                .thenReturn(null);

            assertThat(EntityMapIcons.resolveMapIcon(stationMock))
                .contains(new EntityMapIcon("graphics/icons/buoy.png", null));
        }
    }
}
