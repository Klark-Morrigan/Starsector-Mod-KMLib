package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.widgets.tabs.style.TabLook;
import kmlib.starsector.ui.widgets.tabs.style.TabPalette;
import kmlib.starsector.ui.widgets.tabs.style.TabWash;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how a row's looks are bound: each tab settles on the look its own selection names and travels toward
 * the one hovered shade by its own fraction. The binding is where the two per-row facts meet - which tab is
 * lit and how far each has faded - so a source that read either off the wrong tab would still paint a
 * plausible strip, which is what these assertions rule out.
 */
final class TabLookSourceTest {

    // Flat, widely-spaced shades so a blend lands on a number no two roles could both produce.
    private static final TabLook UNSELECTED_LOOK = new TabLook(
        new Color(0, 0, 0),
        new Color(10, 10, 10));
    private static final TabLook SELECTED_LOOK = new TabLook(
        new Color(100, 100, 100),
        new Color(110, 110, 110));
    private static final TabLook HOVERED_LOOK = new TabLook(
        new Color(200, 200, 200),
        new Color(210, 210, 210));

    // The look channel is what these pin, so the momentary role lifts nothing: a zero strength blends
    // nowhere, and a lift here would colour a look assertion with something the look channel never chose.
    private static final TabWash NO_LIFT = new TabWash(new Color(255, 255, 255), 0f);

    private static final TabPalette PALETTE = new TabPalette(
        new Color(5, 5, 5),
        UNSELECTED_LOOK,
        SELECTED_LOOK,
        HOVERED_LOOK,
        NO_LIFT);

    private static final int SELECTED_INDEX = 1;
    private static final int UNSELECTED_INDEX = 0;

    @Nested
    class CreateHoverFadedLookSource {

        @Test
        void createHoverFadedLookSourceLeavesAnUnhoveredTabOnTheLookItsSelectionNames() {

            var looks = TabLookSource.createHoverFadedLookSource(
                PALETTE,
                SELECTED_INDEX,
                tabIndex -> 0f);

            assertThat(looks.resolveLookAt(UNSELECTED_INDEX))
                .isEqualTo(new TabLook(new Color(0, 0, 0), new Color(10, 10, 10)));
            assertThat(looks.resolveLookAt(SELECTED_INDEX))
                .isEqualTo(new TabLook(new Color(100, 100, 100), new Color(110, 110, 110)));
        }

        @Test
        void createHoverFadedLookSourceBringsBothTabsToTheSameShadeWhenFullyHovered() {
            // The one shade the resting and the lit tab meet at, which is why hovering is a look rather
            // than a lift: no fraction of each tab's own fill could bring two starting colours together.
            var looks = TabLookSource.createHoverFadedLookSource(
                PALETTE,
                SELECTED_INDEX,
                tabIndex -> 1f);

            assertThat(looks.resolveLookAt(UNSELECTED_INDEX))
                .isEqualTo(new TabLook(new Color(200, 200, 200), new Color(210, 210, 210)));
            assertThat(looks.resolveLookAt(SELECTED_INDEX))
                .isEqualTo(new TabLook(new Color(200, 200, 200), new Color(210, 210, 210)));
        }

        @Test
        void createHoverFadedLookSourceFadesEachTabByItsOwnFraction() {
            // Two tabs part-way along at once is the ordinary case while a pointer moves across a row, so
            // each must read its own fraction rather than the row's most recent one.
            var looks = TabLookSource.createHoverFadedLookSource(
                PALETTE,
                SELECTED_INDEX,
                tabIndex -> tabIndex == UNSELECTED_INDEX ? 0.5f : 0.25f);

            assertThat(looks.resolveLookAt(UNSELECTED_INDEX).fill())
                .isEqualTo(new Color(100, 100, 100));
            assertThat(looks.resolveLookAt(SELECTED_INDEX).fill())
                .isEqualTo(new Color(125, 125, 125));
        }

        @Test
        void createHoverFadedLookSourceTreatsEveryTabAsRestingWhenNoneIsSelected() {
            // A row with no selection at all - an index outside it - must not accidentally light a tab.
            var looks = TabLookSource.createHoverFadedLookSource(
                PALETTE,
                -1,
                tabIndex -> 0f);

            assertThat(looks.resolveLookAt(UNSELECTED_INDEX))
                .isEqualTo(new TabLook(new Color(0, 0, 0), new Color(10, 10, 10)));
            assertThat(looks.resolveLookAt(SELECTED_INDEX))
                .isEqualTo(new TabLook(new Color(0, 0, 0), new Color(10, 10, 10)));
        }
    }
}
