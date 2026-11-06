package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.testfixtures.starsector.ui.coreui.CoreUiReachFailures;
import kmlib.testfixtures.starsector.ui.intel.IntelScreenViewFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

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
class ShownMapTabTests {

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

        // The M screen, where the map widget is the core tab rather than something inside it.
        @Test
        void returnsTheCurrentTabWhenItIsItselfAMap() {

            // ARRANGE
            var mapTabMock = createMapWidgetMock();

            // ACT
            var actualMapTab = ShownMapTab.resolveShownMapTab(mapTabMock, intelScreenFake);

            // ASSERT
            assertThat(actualMapTab)
                .isSameAs(mapTabMock);
        }

        // The two are not guaranteed to exclude each other - they read different core UIs, so
        // an interaction dialog can have both answering at once. The current tab wins, because
        // it is the map the player is looking at while the visor is one on a screen behind it.
        @Test
        void prefersTheCurrentTabOverALitVisor() {

            // ARRANGE
            var mapTabMock = createMapWidgetMock();

            intelScreenFake.setMapVisorWidget(mock(UIComponentAPI.class));

            // ACT
            var actualMapTab = ShownMapTab.resolveShownMapTab(mapTabMock, intelScreenFake);

            // ASSERT
            assertThat(actualMapTab)
                .isSameAs(mapTabMock);
        }

        // The intel screen, whose core tab holds the map several levels down. The tab itself is
        // an ordinary component, so the fallback is what finds the map at all.
        @Test
        void fallsBackToTheVisorWhenTheCurrentTabIsNotAMap() {

            // ARRANGE
            var visorWidgetMock = mock(UIComponentAPI.class);

            intelScreenFake.setMapVisorWidget(visorWidgetMock);

            // ACT
            var actualMapTab = ShownMapTab.resolveShownMapTab(mock(UIComponentAPI.class), intelScreenFake);

            // ASSERT
            assertThat(actualMapTab)
                .isSameAs(visorWidgetMock);
        }

        // Every other screen. Not a failure, and deliberately not distinguished from one here:
        // a caller that finds no map tab has nothing to measure either way.
        @Test
        void returnsNothingWhenNoScreenShowsAMap() {

            // ARRANGE
            var otherTabMock = mock(UIComponentAPI.class);

            // ACT
            var actualMapTab = ShownMapTab.resolveShownMapTab(otherTabMock, intelScreenFake);

            // ASSERT
            assertThat(actualMapTab)
                .isNull();
        }

        // The walk answers null before any campaign UI exists, which must not be mistaken for a
        // tab that failed the map test.
        @Test
        void returnsNothingWhenThereIsNoTabToRead() {

            // ARRANGE
            Object noTab = null;

            // ACT
            var actualMapTab = ShownMapTab.resolveShownMapTab(noTab, intelScreenFake);

            // ASSERT
            assertThat(actualMapTab)
                .isNull();
        }

        // Being a map is not enough: the rule rooted here measures boxes, and something the
        // published component interface cannot be asked about has none to measure.
        @Test
        void ignoresAMapThatIsNotALaidOutComponent() {

            // ARRANGE
            var mapWithoutLayoutMock = mock(SectorMapAPI.class);

            // ACT
            var actualMapTab = ShownMapTab.resolveShownMapTab(mapWithoutLayoutMock, intelScreenFake);

            // ASSERT
            assertThat(actualMapTab)
                .isNull();
        }
    }

    @Nested
    class IsSectorMapTab {

        @Test
        void isTrueForAMapLaidOutAsAComponent() {

            // ARRANGE
            var mapTabMock = createMapWidgetMock();

            // ACT
            var actualSectorMapTab = ShownMapTab.isSectorMapTab(mapTabMock);

            // ASSERT
            assertThat(actualSectorMapTab)
                .isTrue();
        }

        @Test
        void isFalseForAMapThatIsNotALaidOutComponent() {

            // ARRANGE
            var mapWithoutLayoutMock = mock(SectorMapAPI.class);

            // ACT
            var actualSectorMapTab = ShownMapTab.isSectorMapTab(mapWithoutLayoutMock);

            // ASSERT
            assertThat(actualSectorMapTab)
                .isFalse();
        }

        @Test
        void isFalseForAnotherScreensTab() {

            // ARRANGE
            var otherTabMock = mock(UIComponentAPI.class);

            // ACT
            var actualSectorMapTab = ShownMapTab.isSectorMapTab(otherTabMock);

            // ASSERT
            assertThat(actualSectorMapTab)
                .isFalse();
        }

        @Test
        void isFalseWithNoTab() {

            // ARRANGE
            Object noTab = null;

            // ACT
            var actualSectorMapTab = ShownMapTab.isSectorMapTab(noTab);

            // ASSERT
            assertThat(actualSectorMapTab)
                .isFalse();
        }
    }

    @Nested
    class IsMapTabShowing {

        @Test
        void isTrueWhereTheReachAnswersATab() {

            // ARRANGE
            Supplier<UIComponentAPI> resolveShownMapTab = () -> mock(UIComponentAPI.class);

            // ACT
            var actualShowing = ShownMapTab.isMapTabShowing(resolveShownMapTab);

            // ASSERT
            assertThat(actualShowing)
                .isTrue();
        }

        // The ordinary way out: the player left the map, so the reach answers rather than raises.
        @Test
        void isFalseWhereNoScreenShowsAMap() {

            // ARRANGE
            Supplier<UIComponentAPI> resolveShownMapTab = () -> null;

            // ACT
            var actualShowing = ShownMapTab.isMapTabShowing(resolveShownMapTab);

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        // The direction is what is pinned: a panel that cannot find the screen it stands over
        // comes down rather than standing on one it can no longer see.
        @Test
        void failsClosedWhereTheWidgetTreeCannotBeWalked() {

            // ARRANGE
            Supplier<UIComponentAPI> resolveShownMapTab = () -> {
                throw new IllegalStateException("The widget tree could not be walked.");
            };

            // ACT
            var actualShowing = ShownMapTab.isMapTabShowing(resolveShownMapTab);

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        @Test
        void failsClosedWhereTheGamesOwnFailureComesBackChecked() {

            // ARRANGE
            Supplier<UIComponentAPI> resolveShownMapTab = CoreUiReachFailures::throwWrappedGameFailure;

            // ACT
            var actualShowing = ShownMapTab.isMapTabShowing(resolveShownMapTab);

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }

        @Test
        void failsClosedWhereAMemberNoLongerLinks() {

            // ARRANGE
            Supplier<UIComponentAPI> resolveShownMapTab = CoreUiReachFailures::throwUnlinkedMember;

            // ACT
            var actualShowing = ShownMapTab.isMapTabShowing(resolveShownMapTab);

            // ASSERT
            assertThat(actualShowing)
                .isFalse();
        }
    }
}
