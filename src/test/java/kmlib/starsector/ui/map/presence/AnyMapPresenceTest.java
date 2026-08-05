package kmlib.starsector.ui.map.presence;

import kmlib.math.geometry.Rectangle;
import kmlib.testfixtures.starsector.ui.intel.IntelScreenViewFake;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the disjunction over the two hosts that can be showing a map: either alone is enough, both
 * together still read as one, and neither leaves it off. Its own case, beyond what the two look-aware
 * siblings pin, is indifference to the Starscape filter - a lit visor counts with the filter set
 * either way, which is what makes this a question resolved a step earlier than theirs rather than
 * their disjunction spelled out.
 */
class AnyMapPresenceTest {

    private static final Rectangle LIT_VISOR_RECT = new Rectangle(10f, 20f, 300f, 200f);

    private IntelScreenViewFake intelScreenViewFake;

    @BeforeEach
    void setUp() {
        intelScreenViewFake = new IntelScreenViewFake();
    }

    @Nested
    class IsAnyMapShowing {

        @Test
        void isFalseWhenNeitherHostIsShowingAMap() {
            assertThat(buildPresence(false).isAnyMapShowing())
                .isFalse();
        }

        @Test
        void isTrueWhenTheSectorMapAloneIsShowing() {
            assertThat(buildPresence(true).isAnyMapShowing())
                .isTrue();
        }

        @Test
        void isTrueWhenTheLitMapVisorAloneIsShowing() {
            
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);

            assertThat(buildPresence(false).isAnyMapShowing())
                .isTrue();
        }

        @Test
        void isTrueWhenBothHostsAreShowingAMap() {
            // Not a contradiction of "one core tab at a time": the two live reads resolve against
            // different core UIs (the sector one follows an interaction dialog's own core UI, the
            // intel one always walks the main core UI), so neither constrains the other and the
            // disjunction has to hold with both true.
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);

            assertThat(buildPresence(true).isAnyMapShowing())
                .isTrue();
        }

        @Test
        void isTrueWhenTheLitMapVisorIsInStarscapeMode() {
            // The case that separates this read from the schematic sibling, which answers false for
            // exactly this screen. A map drawing the starfield is still a map on screen.
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);
            intelScreenViewFake.setMapStarscapeModeOn(true);

            assertThat(buildPresence(false).isAnyMapShowing())
                .isTrue();
        }

        @Test
        void isFalseWhenTheIntelStarscapeFilterIsOnWithNoLitVisor() {
            // The preview panel keeps its filter state while a sibling sub-tab (Planets, Factions)
            // is up, so no filter setting can stand in for a visor being on screen.
            intelScreenViewFake.setMapStarscapeModeOn(true);

            assertThat(buildPresence(false).isAnyMapShowing())
                .isFalse();
        }
    }

    // The sector read is a plain supplier here rather than the live static, so each case names its
    // sector answer and the intel fake carries the rest.
    private AnyMapPresence buildPresence(boolean isSectorMapShowing) {
        return new AnyMapPresence(() -> isSectorMapShowing, intelScreenViewFake);
    }
}
