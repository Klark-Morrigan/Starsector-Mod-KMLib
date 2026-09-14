package kmlib.starsector.ui.buttons;

import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;
import kmlib.testfixtures.starsector.ui.label.ButtonLabelFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how a button the game built is reached for its words, and what is said in them.
 *
 * <p>The reaching is pinned on the shape the game actually uses - a button holding a renderer,
 * holding the piece that carries the words - because every shorter shape is one a real widget does
 * not have. The published text accessors on a button answer for a different kind of button entirely,
 * so a subject that reached for the words the obvious way would find nothing in a running game and
 * everything in a fixture that let it.
 *
 * <p>Which label is accepted is pinned hardest, because getting it wrong is silent. A widget holds
 * several - chrome, an empty one waiting to be filled - and a search returning any of them writes
 * into something nobody draws: no error, no log line, and no change on screen. The words are what
 * tell the button's own label from the rest, so a label reading anything else has to be refused
 * rather than used.
 *
 * <p>The refusals are pinned one at a time because each is a different way for somebody else's
 * widget to be shaped unlike the one this expects, and all of them have to end the same way - no
 * words, and a caller with one case to handle.
 *
 * <p>What is said is pinned as the game's own rule: a key already occurring in the words is lit
 * where it stands, and one that is not is written after them. Both halves are held, because it is
 * the pair that makes a decorated button read like the ones beside it.
 */
class VanillaButtonLabelTest {

    private static final String LABEL = "Map layers";

    // A key the label already contains, and one it does not - the two sides of the rule below.
    private static final String CONTAINED_KEY = "M";
    private static final String ABSENT_KEY = "Q";

    // What the install draws a key on a button in. Any colour would serve - what matters is that the
    // one handed to the words is the one the palette answered with.
    private static final Color SHORTCUT_COLOUR = new Color(255, 200, 100);

    @BeforeEach
    void installPalette() {

        StarsectorSettingsFake.installSettings(
            StarsectorSettingsFake.EMPTY_STRINGS, colourKey -> SHORTCUT_COLOUR);
    }

    @AfterEach
    void clearPalette() {

        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class ResolveLabelOf {

        @Test
        void resolveLabelOfReachesTheWordsTwoHopsBelowTheButton() {
            // The shape the game builds: the button holds a renderer, the renderer holds the piece
            // carrying the words. Neither hop is optional, and the button itself answers nothing.
            var buttonFake = new ButtonFake(new ButtonLabelFake(LABEL));

            assertThat(VanillaButtonLabel.resolveLabelOf(buttonFake, LABEL))
                .isNotNull();
        }

        @Test
        void resolveLabelOfRefusesALabelReadingSomethingElse() {
            // The one that fails silently if it is not refused: a widget holds several labels, and
            // writing into the wrong one changes nothing anyone can see and reports nothing.
            var buttonFake = new ButtonFake(new ButtonLabelFake("Starscape"));

            assertThat(VanillaButtonLabel.resolveLabelOf(buttonFake, LABEL))
                .isNull();
        }

        @Test
        void resolveLabelOfTakesTheLabelReadingTheWordsFromAmongSeveral() {
            // The same widget holding chrome and an empty label beside the one that draws its words.
            // Order is not the discriminator - what the label says is - so the wanted one is put
            // last, where a search taking the first it met would already have answered.
            var buttonFake = new ButtonFake(
                new ButtonLabelFake("Starscape"),
                new ButtonLabelFake(""),
                new ButtonLabelFake(LABEL));

            VanillaButtonLabel.resolveLabelOf(buttonFake, LABEL).announceShortcut(CONTAINED_KEY);

            assertThat(buttonFake.readLastLabel().readHighlightedRuns())
                .containsExactly(CONTAINED_KEY);
        }

        @Test
        void resolveLabelOfAnswersNothingForWordsNoLabelUnderItReads() {
            var buttonFake = new ButtonFake(new ButtonLabelFake(LABEL));

            assertThat(VanillaButtonLabel.resolveLabelOf(buttonFake, "Constellations"))
                .isNull();
        }

        @Test
        void resolveLabelOfAnswersNothingWhenTheCallerNamesNoWords() {
            // A caller with nothing to match on cannot be given an answer worth having: every label
            // under the button would be as good as every other, which is the state this exists to
            // avoid.
            var buttonFake = new ButtonFake(new ButtonLabelFake(LABEL));

            assertThat(VanillaButtonLabel.resolveLabelOf(buttonFake, " "))
                .isNull();
        }

        @Test
        void resolveLabelOfStopsBeforeWordsBuriedDeeperThanTheGamePutsThem() {
            // A widget tree is walked to the depth the game uses and no further. Left open, a search
            // over a live tree would call its way across half the screen looking for something that
            // is two hops away or nowhere at all.
            var buttonFake = new ButtonFake(new HolderFake(new ButtonLabelFake(LABEL)));

            assertThat(VanillaButtonLabel.resolveLabelOf(buttonFake, LABEL))
                .isNull();
        }

        @Test
        void resolveLabelOfTreatsAnAccessorThatThrowsAsADeadEndRatherThanAFailure() {
            // A widget asked a question it does not care for answers by throwing. That is one branch
            // of the walk ending, not the walk failing - the words may still be down another.
            var buttonFake = new ButtonFake(new ThrowingHolderFake(), new ButtonLabelFake(LABEL));

            assertThat(VanillaButtonLabel.resolveLabelOf(buttonFake, LABEL))
                .isNotNull();
        }

        @Test
        void resolveLabelOfAnswersNothingForAWidgetWithNothingUnderIt() {
            // What a reworked game build looks like from here: a button that leads nowhere. Not an
            // error - the widget is somebody else's - so the caller gets one case to handle.
            assertThat(VanillaButtonLabel.resolveLabelOf(new Object(), LABEL))
                .isNull();
        }
    }

    @Nested
    class AnnounceShortcut {

        @Test
        void announceShortcutLightsTheKeyWhereTheWordsAlreadyHoldIt() {
            // The game's rule for a key its button's words already contain: the occurrence is lit
            // rather than repeated, so the words are not lengthened to say what they already say.
            var labelFake = announceOn(LABEL, CONTAINED_KEY);

            assertThat(labelFake.getText())
                .isEqualTo("Map layers");
            assertThat(labelFake.readHighlightedRuns())
                .containsExactly("M");
        }

        @Test
        void announceShortcutLightsTheOccurrenceAsItIsWrittenRatherThanAsTheKeyIsNamed() {
            // The match ignores case and the lighting cannot: a run handed over has to be a substring
            // of what is drawn, so a key named "L" against words holding "l" lights the "l".
            assertThat(announceOn(LABEL, "L").readHighlightedRuns())
                .containsExactly("l");
        }

        @Test
        void announceShortcutWritesTheKeyAfterTheWordsWhereTheyDoNotHoldIt() {
            // The other half of the same rule, and the one the game's own filter row takes: a key the
            // words do not contain is spelled out after them, and that is what is lit.
            var labelFake = announceOn(LABEL, ABSENT_KEY);

            assertThat(labelFake.getText())
                .isEqualTo("Map layers [Q]");
            assertThat(labelFake.readHighlightedRuns())
                .containsExactly("[Q]");
        }

        @Test
        void announceShortcutLightsWhatItSaysInTheColourTheGameLightsAKeyIn() {
            // A run lit without a colour draws in whatever the last caller left behind, so the two
            // travel together or the announcement reads differently from screen to screen.
            assertThat(announceOn(LABEL, CONTAINED_KEY).readHighlightColours())
                .containsExactly(SHORTCUT_COLOUR);
        }

        @Test
        void announceShortcutSaysNothingForAKeyWithNoName() {
            // A code the key table does not cover. A bracket around nothing would read as a control
            // whose key had gone missing rather than one whose key cannot be written down.
            var labelFake = announceOn(LABEL, " ");

            assertThat(labelFake.getText())
                .isEqualTo("Map layers");
            assertThat(labelFake.readHighlightedRuns())
                .isEmpty();
        }
    }

    // Says a key on the words of a button standing in for one the game built, and hands the words
    // back for reading.
    private static ButtonLabelFake announceOn(String words, String keyName) {

        var labelFake = new ButtonLabelFake(words);

        VanillaButtonLabel.resolveLabelOf(new ButtonFake(labelFake), words).announceShortcut(keyName);

        return labelFake;
    }

    /**
     * A button the game built: it draws none of its own words and holds the renderer that does.
     *
     * <p>Both hops are named as the game names them - the renderer accessor it leaves alone, and the
     * title accessor on the piece below - so a subject reaching by any other route finds nothing
     * here, exactly as it would in a running game.
     */
    private static final class ButtonFake {

        private final RendererFake rendererFake;

        private ButtonFake(Object... held) {
            this.rendererFake = new RendererFake(held);
        }

        // The last thing it holds, for a case that put the wanted label behind the decoys.
        ButtonLabelFake readLastLabel() {
            return rendererFake.readLastLabel();
        }

        public RendererFake getRenderer() {
            return rendererFake;
        }
    }

    /** What draws a button: it holds the pieces carrying words, one accessor per piece. */
    private static final class RendererFake {

        private final Object[] held;

        private RendererFake(Object... held) {
            this.held = held;
        }

        ButtonLabelFake readLastLabel() {
            return (ButtonLabelFake) held[held.length - 1];
        }

        // Named as meaninglessly as the game names it, so nothing can reach past it by knowing one.
        public TitleFake o00000() {
            return new TitleFake(held.length > 0 ? held[0] : null);
        }

        public TitleFake o00001() {
            return new TitleFake(held.length > 1 ? held[1] : null);
        }

        public TitleFake o00002() {
            return new TitleFake(held.length > 2 ? held[2] : null);
        }
    }

    /** The piece a renderer holds, reached by the accessor the game leaves unobfuscated. */
    private static final class TitleFake {

        private final Object title;

        private TitleFake(Object title) {
            this.title = title;
        }

        public Object getTitle() {
            return title;
        }
    }

    /** One level too many: words held below the depth the game puts them at. */
    private static final class HolderFake {

        private final Object held;

        private HolderFake(Object held) {
            this.held = held;
        }

        public Object getTitle() {
            return new TitleFake(held);
        }
    }

    /** A piece that answers a question it does not care for by throwing. */
    private static final class ThrowingHolderFake {

        public Object getTitle() {
            throw new IllegalStateException("this widget no longer answers that");
        }
    }
}
