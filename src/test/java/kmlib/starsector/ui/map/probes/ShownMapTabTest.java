package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.testfixtures.starsector.ui.coreui.CoreUiReachFailures;
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
 *
 * <p>And that the showing read fails closed on every way the walk can fail, since what stands on it
 * is a panel that has to come down with the screen.
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
        void returnsTheCurrentTabWhenItIsItselfAMap() {
            // The M screen, where the map widget is the core tab rather than something inside it.
            var mapTabMock = createMapWidgetMock();

            assertThat(ShownMapTab.resolveShownMapTab(mapTabMock, intelScreenFake))
                .isSameAs(mapTabMock);
        }

        @Test
        void prefersTheCurrentTabOverALitVisor() {
            // The two are not guaranteed to exclude each other - they read different core UIs, so
            // an interaction dialog can have both answering at once. The current tab wins, because
            // it is the map the player is looking at while the visor is one on a screen behind it.
            var mapTabMock = createMapWidgetMock();

            intelScreenFake.setMapVisorWidget(mock(UIComponentAPI.class));

            assertThat(ShownMapTab.resolveShownMapTab(mapTabMock, intelScreenFake))
                .isSameAs(mapTabMock);
        }

        @Test
        void fallsBackToTheVisorWhenTheCurrentTabIsNotAMap() {
            // The intel screen, whose core tab holds the map several levels down. The tab itself is
            // an ordinary component, so the fallback is what finds the map at all.
            var visorWidgetMock = mock(UIComponentAPI.class);

            intelScreenFake.setMapVisorWidget(visorWidgetMock);

            assertThat(ShownMapTab.resolveShownMapTab(mock(UIComponentAPI.class), intelScreenFake))
                .isSameAs(visorWidgetMock);
        }

        @Test
        void returnsNothingWhenNoScreenShowsAMap() {
            // Every other screen. Not a failure, and deliberately not distinguished from one here:
            // a caller that finds no map tab has nothing to measure either way.
            assertThat(ShownMapTab.resolveShownMapTab(mock(UIComponentAPI.class), intelScreenFake))
                .isNull();
        }

        @Test
        void returnsNothingWhenThereIsNoTabToRead() {
            // The walk answers null before any campaign UI exists, which must not be mistaken for a
            // tab that failed the map test.
            assertThat(ShownMapTab.resolveShownMapTab(null, intelScreenFake))
                .isNull();
        }

        @Test
        void ignoresAMapThatIsNotALaidOutComponent() {
            // Being a map is not enough: the rule rooted here measures boxes, and something the
            // published component interface cannot be asked about has none to measure.
            var mapWithoutLayoutMock = mock(SectorMapAPI.class);

            assertThat(ShownMapTab.resolveShownMapTab(mapWithoutLayoutMock, intelScreenFake))
                .isNull();
        }
    }

    @Nested
    class IsMapTabShowing {

        @Test
        void isTrueWhereTheReachAnswersATab() {

            assertThat(ShownMapTab.isMapTabShowing(() -> mock(UIComponentAPI.class)))
                .isTrue();
        }

        @Test
        void isFalseWhereNoScreenShowsAMap() {
            // The ordinary way out: the player left the map, so the reach answers rather than raises.
            assertThat(ShownMapTab.isMapTabShowing(() -> null))
                .isFalse();
        }

        @Test
        void failsClosedWhereTheWidgetTreeCannotBeWalked() {
            // The direction is what is pinned: a panel that cannot find the screen it stands over
            // comes down rather than standing on one it can no longer see.
            assertThat(ShownMapTab.isMapTabShowing(() -> {
                throw new IllegalStateException("The widget tree could not be walked.");
            }))
                .isFalse();
        }

        @Test
        void failsClosedWhereTheGamesOwnFailureComesBackChecked() {

            assertThat(ShownMapTab.isMapTabShowing(CoreUiReachFailures::throwWrappedGameFailure))
                .isFalse();
        }

        @Test
        void failsClosedWhereAMemberNoLongerLinks() {

            assertThat(ShownMapTab.isMapTabShowing(CoreUiReachFailures::throwUnlinkedMember))
                .isFalse();
        }
    }
}
