package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.CustomEntitySpecAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the one thing this read adds over calling for a name and a glyph apart: that both come off
 * the one token. Which glyph an entity carries is {@link EntityMapIconsTest}'s, and the branch
 * behind it is not restated here - only that whatever it answers arrives paired with the right name.
 *
 * <p>Cases live in a {@link Nested} group named for the method under test, so the suite reports as
 * a per-method tree.
 */
final class EntityNameplatesTest {

    @Nested
    class ReadNameplate {

        @Test
        void readNameplatePairsTheEntitysOwnNameWithItsOwnGlyph() {
            // Both halves off the one token is the whole of what this read buys, so the case asserts
            // them together rather than one at a time.
            var stationMock = buildEntityWithIcon(
                "Fort Ludd",
                "graphics/icons/station0.png",
                new Color(200, 200, 255));

            assertThat(EntityNameplates.readNameplate(stationMock))
                .isEqualTo(new EntityNameplate(
                    "Fort Ludd",
                    Optional.of(new EntityMapIcon(
                        "graphics/icons/station0.png",
                        new Color(200, 200, 255)))));
        }

        @Test
        void readNameplateNamesAnEntityTheMapMarksWithNothing() {
            // An entity carrying no icon spec is still identified by its name, so the read answers an
            // ordinary nameplate with an empty glyph rather than nothing at all.
            var entityMock = mock(SectorEntityToken.class);

            when(entityMock.getName())
                .thenReturn("Ancyra");
            when(entityMock.getCustomEntitySpec())
                .thenReturn(null);

            assertThat(EntityNameplates.readNameplate(entityMock))
                .isEqualTo(EntityNameplate.createUnmarkedNameplate("Ancyra"));
        }

        @Test
        void readNameplateIsBlankForANullEntity() {
            // The null-defensive shape the package holds to: a caller reading an entity that turned
            // out not to be there gets something it can draw rather than a throw at the draw.
            assertThat(EntityNameplates.readNameplate(null))
                .isEqualTo(EntityNameplate.BLANK);
        }
    }

    // A named entity answering the icon its custom-entity spec authored - where vanilla keeps a
    // station's glyph, and the arm the icon read reaches for anything that is not a planet.
    private static SectorEntityToken buildEntityWithIcon(
            String name,
            String iconName,
            Color iconColour) {

        var entityMock = mock(SectorEntityToken.class);
        var entitySpecMock = mock(CustomEntitySpecAPI.class);

        when(entityMock.getName())
            .thenReturn(name);
        when(entityMock.getCustomEntitySpec())
            .thenReturn(entitySpecMock);
        when(entitySpecMock.getIconName())
            .thenReturn(iconName);
        when(entitySpecMock.getIconColor())
            .thenReturn(iconColour);

        return entityMock;
    }
}
