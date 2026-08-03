package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.testfixtures.starsector.ui.intel.IntelScreenViewFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Pins which widget a rule about map-tab layout gets rooted at. The walk that fetches the current
 * tab is unpublished-API reflection and only observable in a running game, but the choice made over
 * what it hands back is plain, and it is that choice - not the walk - that decides whether a rule
 * measures the map or some other screen's tab.
 */
class ShownMapTabTest {

    private final IntelScreenViewFake intelScreenFake = new IntelScreenViewFake();

    // The live map widget is both a map and a laid-out component; a double has to be both for the
    // same reason the rule tests both.
    private static UIComponentAPI createMapWidgetMock() {
        return (UIComponentAPI) mock(
            SectorMapAPI.class,
            withSettings().extraInterfaces(UIComponentAPI.class));
    }

    @Nested
    class ResolveShownMapTab {

        @Test
        void resolveShownMapTabReturnsTheCurrentTabWhenItIsItselfAMap() {
            // The M screen, where the map widget is the core tab rather than something inside it.
            var mapTabMock = createMapWidgetMock();

            assertThat(ShownMapTab.resolveShownMapTab(mapTabMock, intelScreenFake))
                .isSameAs(mapTabMock);
        }

        @Test
        void resolveShownMapTabPrefersTheCurrentTabOverALitVisor() {
            // The two are not guaranteed to exclude each other - they read different core UIs, so
            // an interaction dialog can have both answering at once. The current tab wins, because
            // it is the map the player is looking at while the visor is one on a screen behind it.
            var mapTabMock = createMapWidgetMock();
            intelScreenFake.setMapVisorWidget(mock(UIComponentAPI.class));

            assertThat(ShownMapTab.resolveShownMapTab(mapTabMock, intelScreenFake))
                .isSameAs(mapTabMock);
        }

        @Test
        void resolveShownMapTabFallsBackToTheVisorWhenTheCurrentTabIsNotAMap() {
            // The intel screen, whose core tab holds the map several levels down. The tab itself is
            // an ordinary component, so the fallback is what finds the map at all.
            var visorWidgetMock = mock(UIComponentAPI.class);
            intelScreenFake.setMapVisorWidget(visorWidgetMock);

            assertThat(ShownMapTab.resolveShownMapTab(mock(UIComponentAPI.class), intelScreenFake))
                .isSameAs(visorWidgetMock);
        }

        @Test
        void resolveShownMapTabReturnsNothingWhenNoScreenShowsAMap() {
            // Every other screen. Not a failure, and deliberately not distinguished from one here:
            // a caller that finds no map tab has nothing to measure either way.
            assertThat(ShownMapTab.resolveShownMapTab(mock(UIComponentAPI.class), intelScreenFake))
                .isNull();
        }

        @Test
        void resolveShownMapTabReturnsNothingWhenThereIsNoTabToRead() {
            // The walk answers null before any campaign UI exists, which must not be mistaken for a
            // tab that failed the map test.
            assertThat(ShownMapTab.resolveShownMapTab(null, intelScreenFake))
                .isNull();
        }

        @Test
        void resolveShownMapTabIgnoresAMapThatIsNotALaidOutComponent() {
            // Being a map is not enough: the rule rooted here measures boxes, and something the
            // published component interface cannot be asked about has none to measure.
            var mapWithoutLayoutMock = mock(SectorMapAPI.class);

            assertThat(ShownMapTab.resolveShownMapTab(mapWithoutLayoutMock, intelScreenFake))
                .isNull();
        }
    }
}
