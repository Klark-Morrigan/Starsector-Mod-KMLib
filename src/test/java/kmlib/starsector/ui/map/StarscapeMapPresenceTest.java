package kmlib.starsector.ui.map;

import kmlib.math.geometry.Rectangle;
import kmlib.testfixtures.starsector.ui.intel.IntelScreenViewFake;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the disjunction over the two hosts that can be showing a starscape map: either alone is
 * enough, both together still read as one, and neither leaves it off. Also pins the intel side's
 * second term - a starscape filter reading as on while no visor is on screen does not count -
 * since that is the case the panel's survival across the sibling sub-tabs produces.
 */
class StarscapeMapPresenceTest {

    private static final Rectangle LIT_VISOR_RECT = new Rectangle(10f, 20f, 300f, 200f);

    private IntelScreenViewFake intelScreenViewFake;

    @BeforeEach
    void setUp() {
        intelScreenViewFake = new IntelScreenViewFake();
    }

    @Nested
    class IsStarscapeMapShowing {
        @Test
        void isFalseWhenNeitherHostIsInStarscapeMode() {
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);

            assertThat(buildPresence(false).isStarscapeMapShowing()).isFalse();
        }

        @Test
        void isTrueWhenTheSectorMapAloneIsInStarscapeMode() {
            assertThat(buildPresence(true).isStarscapeMapShowing()).isTrue();
        }

        @Test
        void isTrueWhenTheLitMapVisorAloneIsInStarscapeMode() {
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);
            intelScreenViewFake.setMapStarscapeModeOn(true);

            assertThat(buildPresence(false).isStarscapeMapShowing()).isTrue();
        }

        @Test
        void isTrueWhenBothHostsAreInStarscapeMode() {
            // Not a contradiction of "one core tab at a time": the two live reads resolve against
            // different core UIs (the sector one follows an interaction dialog's own core UI, the
            // intel one always walks the main core UI), so neither constrains the other and the
            // disjunction has to hold with both true.
            intelScreenViewFake.setMapVisorRect(LIT_VISOR_RECT);
            intelScreenViewFake.setMapStarscapeModeOn(true);

            assertThat(buildPresence(true).isStarscapeMapShowing()).isTrue();
        }

        @Test
        void isFalseWhenTheIntelStarscapeFilterIsOnWithNoLitVisor() {
            // The preview panel keeps its filter state while a sibling sub-tab (Planets, Factions)
            // is up, so the filter alone is not a starscape map being on screen.
            intelScreenViewFake.setMapStarscapeModeOn(true);

            assertThat(buildPresence(false).isStarscapeMapShowing()).isFalse();
        }
    }

    // The sector read is a plain supplier here rather than the live static, so each case names its
    // sector answer and the intel fake carries the rest.
    private StarscapeMapPresence buildPresence(boolean isSectorMapInStarscapeMode) {
        return new StarscapeMapPresence(() -> isSectorMapInStarscapeMode, intelScreenViewFake);
    }
}
