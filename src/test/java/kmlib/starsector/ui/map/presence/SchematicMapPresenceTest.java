package kmlib.starsector.ui.map.presence;

import kmlib.math.geometry.Rectangle;
import kmlib.testfixtures.starsector.ui.intel.IntelScreenViewFake;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the disjunction over the two hosts that can be showing a schematic map: either alone is
 * enough, both together still read as one, and neither leaves it off. Also pins the case that
 * separates this from a negation of the starscape read - a screen with no map at all is not a
 * schematic map, so the two reads are counterparts rather than opposites.
 */
class SchematicMapPresenceTest {

    private static final Rectangle LIT_VISOR_RECT = new Rectangle(10f, 20f, 300f, 200f);

    private IntelScreenViewFake intelScreenViewFake;

    @BeforeEach
    void setUp() {
        intelScreenViewFake = new IntelScreenViewFake();
    }

    @Nested
    class IsSchematicMapShowing {
        @Test
        void isFalseWhenNeitherHostIsShowingAMap() {
            // The fake opens with no visor, so this is every screen that is neither the map nor the
            // intel tab - and the case that stops this being read as "not starscape".
            assertThat(buildPresence(false).isSchematicMapShowing())
                .isFalse();
        }

        @Test
        void isTrueWhenTheSectorMapAloneIsShowingTheSchematic() {
            assertThat(buildPresence(true).isSchematicMapShowing())
                .isTrue();
        }

        @Test
        void isTrueWhenTheLitMapVisorAloneIsShowingTheSchematic() {
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);

            assertThat(buildPresence(false).isSchematicMapShowing())
                .isTrue();
        }

        @Test
        void isTrueWhenBothHostsAreShowingTheSchematic() {
            // Not a contradiction of "one core tab at a time": the two live reads resolve against
            // different core UIs (the sector one follows an interaction dialog's own core UI, the
            // intel one always walks the main core UI), so neither constrains the other and the
            // disjunction has to hold with both true.
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);

            assertThat(buildPresence(true).isSchematicMapShowing())
                .isTrue();
        }

        @Test
        void isFalseWhenTheLitMapVisorIsInStarscapeMode() {
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);
            intelScreenViewFake.setMapStarscapeModeOn(true);

            assertThat(buildPresence(false).isSchematicMapShowing())
                .isFalse();
        }

    }

    // The sector read is a plain supplier here rather than the live static, so each case names its
    // sector answer and the intel fake carries the rest.
    private SchematicMapPresence buildPresence(boolean isSectorMapWithStarscapeOff) {
        return new SchematicMapPresence(() -> isSectorMapWithStarscapeOff, intelScreenViewFake);
    }
}
