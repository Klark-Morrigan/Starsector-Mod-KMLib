package kmlib.starsector.entities;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one rule the pair carries of its own: an unstated glyph is an unmarked entity rather
 * than a null waiting to surface where a line is being composed.
 *
 * <p>Every other record hanging an entity's name and glyph on itself used to spell that rule out
 * for itself, which is the duplication this value exists to end - so it is asserted here, once.
 */
final class EntityNameplateTest {

    @Nested
    class Construct {

        @Test
        void readsAnAbsentIconGivenAsNullAsNoIcon() {
            // A hand-built subject states its name and rarely its glyph, so an unstated icon has to
            // mean an unmarked entity rather than fail late where a line is being composed.
            var nameplate = new EntityNameplate("Chicomoztoc", null);

            assertThat(nameplate.mapIcon())
                .isEmpty();
        }

        @Test
        void carriesAStatedIconUntouched() {

            var nameplate = new EntityNameplate(
                "Chicomoztoc",
                Optional.of(new EntityMapIcon(
                    "graphics/warroom/icon_planet.png",
                    new Color(120, 200, 90))));

            assertThat(nameplate.mapIcon())
                .contains(new EntityMapIcon(
                    "graphics/warroom/icon_planet.png",
                    new Color(120, 200, 90)));
        }
    }

    @Nested
    class CreateUnmarkedNameplate {

        @Test
        void createUnmarkedNameplateNamesTheEntityAndMarksItWithNothing() {

            assertThat(EntityNameplate.createUnmarkedNameplate("Ancyra"))
                .isEqualTo(new EntityNameplate("Ancyra", Optional.empty()));
        }
    }
}
