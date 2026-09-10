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
 * <p>An entity carrying a spec is arranged through a builder per arm, so a case states the icon it
 * is about rather than the two mocks it takes to hold one. The spec-less cases stay inline: they
 * arrange a different shape - an entity with no spec at all - rather than a variation of the same
 * one.
 *
 * <p>Cases live in a {@link Nested} group named for the method under test, so the suite reports as
 * a per-method tree.
 */
final class EntityMapIconsTest {

    @Nested
    class ResolveMapIcon {

        @Test
        void resolveMapIconReadsAPlanetsOwnSpec() {

            var planetMock = buildPlanetWithIcon(
                "graphics/warroom/icon_planet.png",
                new Color(120, 200, 90));

            assertThat(EntityMapIcons.resolveMapIcon(planetMock))
                .contains(new EntityMapIcon(
                    "graphics/warroom/icon_planet.png",
                    new Color(120, 200, 90)));
        }

        @Test
        void resolveMapIconReadsACustomEntitysOwnSpec() {

            var stationMock = buildCustomEntityWithIcon(
                "graphics/icons/station0.png",
                new Color(200, 200, 255));

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
            // rather than pointing a caller at a missing sprite. The colour is authored and good,
            // which is what says the path alone decides whether there is an icon here.
            var planetMock = buildPlanetWithIcon("   ", Color.WHITE);

            assertThat(EntityMapIcons.resolveMapIcon(planetMock))
                .isEmpty();
        }

        @Test
        void resolveMapIconIsEmptyForABlankCustomEntityIconPath() {

            var stationMock = buildCustomEntityWithIcon(null, Color.WHITE);

            assertThat(EntityMapIcons.resolveMapIcon(stationMock))
                .isEmpty();
        }

        @Test
        void resolveMapIconTrimsTheAuthoredPath() {

            var stationMock = buildCustomEntityWithIcon(
                "  graphics/icons/relay.png  ",
                Color.WHITE);

            assertThat(EntityMapIcons.resolveMapIcon(stationMock))
                .contains(new EntityMapIcon("graphics/icons/relay.png", Color.WHITE));
        }

        @Test
        void resolveMapIconCarriesAnUncolouredGlyph() {
            // A spec authoring no colour still has an icon: the glyph's own pixels carry it, and
            // the draw reads a null tint as "as authored". Only the path decides presence.
            var stationMock = buildCustomEntityWithIcon("graphics/icons/buoy.png", null);

            assertThat(EntityMapIcons.resolveMapIcon(stationMock))
                .contains(new EntityMapIcon("graphics/icons/buoy.png", null));
        }
    }

    // A planet answering the icon its planet spec authored. Typed as the planet interface rather
    // than as a token, since it is that type the read branches on.
    private static PlanetAPI buildPlanetWithIcon(String iconTexture, Color iconColour) {

        var planetMock = mock(PlanetAPI.class);
        var planetSpecMock = mock(PlanetSpecAPI.class);

        when(planetMock.getSpec())
            .thenReturn(planetSpecMock);
        when(planetSpecMock.getIconTexture())
            .thenReturn(iconTexture);
        when(planetSpecMock.getIconColor())
            .thenReturn(iconColour);

        return planetMock;
    }

    // A non-planet token - a station, relay or buoy - answering the icon its custom-entity spec
    // authored, which vanilla names and colours through accessors of its own.
    private static SectorEntityToken buildCustomEntityWithIcon(String iconName, Color iconColour) {

        var entityMock = mock(SectorEntityToken.class);
        var entitySpecMock = mock(CustomEntitySpecAPI.class);

        when(entityMock.getCustomEntitySpec())
            .thenReturn(entitySpecMock);
        when(entitySpecMock.getIconName())
            .thenReturn(iconName);
        when(entitySpecMock.getIconColor())
            .thenReturn(iconColour);

        return entityMock;
    }
}
