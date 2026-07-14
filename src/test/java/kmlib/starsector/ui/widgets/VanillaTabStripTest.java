package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.testfixtures.starsector.ui.font.LineWidthMeasurerFake;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link VanillaTabStrip#layoutTabs}: it keeps each tab's content beside its geometry, and
 * the width it snaps to accounts for the bracketed shortcut, so a tab that carries one is wider
 * than the same label without. Also pins {@link VanillaTabStrip#zipTabs}, the pairing both the
 * strip's own layout and a consumer holding the boxes separately build their tabs through.
 */
class VanillaTabStripTest {
    // Each character measures 10 wide, so a display string's width is a plain multiple of length.
    private final LineWidthMeasurer measurerFake = new LineWidthMeasurerFake(10d);

    @Nested
    class LayoutTabs {

        @Test
        void keepsEachContentBesideItsGeometry() {
            var factions = new VanillaTabContent("Political Map", "P");
            var tabs = VanillaTabStrip.layoutTabs(0f, 100f, 24f, 8f, 40f, 14d, List.of(factions),
                    measurerFake);
            assertThat(tabs.get(0).content()).isEqualTo(factions);
        }

        @Test
        void snapsAWiderTabForATabThatCarriesAShortcut() {
            var withShortcut = VanillaTabStrip.layoutTabs(0f, 100f, 24f, 8f, 40f, 14d,
                    List.of(new VanillaTabContent("Political Map", "P")), measurerFake);
            var withoutShortcut = VanillaTabStrip.layoutTabs(0f, 100f, 24f, 8f, 40f, 14d,
                    List.of(new VanillaTabContent("Political Map", null)), measurerFake);
            assertThat(withShortcut.get(0).bounds().width())
                    .isGreaterThan(withoutShortcut.get(0).bounds().width());
        }
    }

    @Nested
    class ZipTabs {

        @Test
        void pairsEachContentWithTheBoxAtItsIndex() {
            var political = new VanillaTabContent("Political Map", "P");
            var alliances = new VanillaTabContent("Alliances", "A");
            var politicalBox = new Rectangle(0f, 0f, 40f, 24f);
            var alliancesBox = new Rectangle(40f, 0f, 30f, 24f);
            var tabs = VanillaTabStrip.zipTabs(List.of(political, alliances),
                    List.of(politicalBox, alliancesBox));
            assertThat(tabs).containsExactly(new VanillaTab(political, politicalBox),
                    new VanillaTab(alliances, alliancesBox));
        }

        @Test
        void zipsOnlyAsFarAsTheShorterOfContentsAndBoxes() {
            var political = new VanillaTabContent("Political Map", "P");
            var alliances = new VanillaTabContent("Alliances", "A");
            var onlyBox = new Rectangle(0f, 0f, 40f, 24f);
            var tabs = VanillaTabStrip.zipTabs(List.of(political, alliances), List.of(onlyBox));
            assertThat(tabs).containsExactly(new VanillaTab(political, onlyBox));
        }

        @Test
        void pairsNothingWhenEitherSideIsEmpty() {
            var tabs = VanillaTabStrip.zipTabs(List.of(),
                    List.of(new Rectangle(0f, 0f, 40f, 24f)));
            assertThat(tabs).isEmpty();
        }
    }
}
