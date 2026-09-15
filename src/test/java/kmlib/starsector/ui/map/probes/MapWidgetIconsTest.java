package kmlib.starsector.ui.map.probes;

import kmlib.testfixtures.starsector.ui.coreui.CoreUiComponentFake;
import kmlib.testfixtures.starsector.ui.map.probes.MapIconOrderWidgetFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins which shape under a map tab is taken for the map widget, and what is answered when none is.
 * The tab this walks from is resolved from a live screen, so the case a running game covers is the
 * resolution - what happens below the tab, which is where both icon reads get their answer, is not
 * covered anywhere a game is not running.
 *
 * <p>Recognition by name is the point: the widget is found by the accessor it answers rather than by
 * its type, the type being obfuscator output that a game build renames. A test posing widgets of no
 * particular type is therefore posing the same question the live walk asks.
 */
class MapWidgetIconsTest {

    private static final Object NO_MAP_TAB = null;

    @Nested
    class ReadIconMapUnder {

        @Test
        void readsTheIconsOfTheWidgetUnderTheTab() {
            // The shape the walk exists to find: the tab holds a scrolling panel, whose content
            // holds the map. A fixed path down to it would be a fact about one build's layout, so
            // what is asserted is that it is found at all rather than where.
            var mapWidgetFake = new MapIconOrderWidgetFake();
            mapWidgetFake.seedIconsFor("nebula", "colony");

            var mapTabFake = new CoreUiComponentFake(new CoreUiComponentFake(mapWidgetFake));

            // Read back as the order it was seeded in, which is the whole of what a caller takes off
            // this map: every rule over it is about where one icon sits relative to the rest.
            assertThat(readIconKeysUnder(mapTabFake))
                .containsExactly("nebula", "colony");
        }

        @Test
        void readsTheIconsOfTheTabItselfWhereTheTabIsTheWidget() {
            // The root answers before anything below it, so a caller that has already picked the map
            // out of its tab can hand this that map rather than the tab it came from.
            var mapWidgetFake = new MapIconOrderWidgetFake();
            mapWidgetFake.seedIconsFor("nebula");

            assertThat(readIconKeysUnder(mapWidgetFake))
                .containsExactly("nebula");
        }

        @Test
        void answersNothingWhileNoMapScreenIsUp() {
            // The ordinary state on every screen but the map ones, which is nearly all of them.
            assertThat(MapWidgetIcons.readIconMapUnder(NO_MAP_TAB))
                .isNull();
        }

        @Test
        void answersNothingWhenNoComponentUnderTheTabCarriesTheAccessor() {
            // A build whose map widget no longer answers this name. The answer is the same as no map
            // screen being up - there is nothing to read an icon order off - which is why the two
            // are told apart in the log rather than in what comes back.
            var mapTabFake = new CoreUiComponentFake(new CoreUiComponentFake());

            assertThat(MapWidgetIcons.readIconMapUnder(mapTabFake))
                .isNull();
        }
    }

    // The entities the icons are held for, in the order the map holds them. Taken as a list because
    // that is what the assertions are about; the icons themselves are never read, here or anywhere
    // else - every rule over this map is about its keys and their order.
    private static List<Object> readIconKeysUnder(Object mapTab) {
        return new ArrayList<>(MapWidgetIcons.readIconMapUnder(mapTab).keySet());
    }
}
