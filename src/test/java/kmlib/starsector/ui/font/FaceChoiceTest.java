package kmlib.starsector.ui.font;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FaceChoiceTest {

    @Nested
    class FromLabel {

        @Test
        void fromLabelReadsTheAutomaticLabelAsTheAutomaticChoice() {

            assertThat(FaceChoice.fromLabel("Auto"))
                .isEqualTo(FaceChoice.AUTO_FACE);
        }

        @Test
        void fromLabelReadsABasenameAsThatFaceByName() {

            assertThat(FaceChoice.fromLabel("insignia42LTaa"))
                .isEqualTo(new FaceChoice.NamedFace(StarsectorFont.VANILLA_INSIGNIA_42));
        }

        @Test
        void fromLabelFallsBackToTheAutomaticChoiceForALabelNamingNoFace() {
            // A label a later build stopped offering must not pin the category to anything: the automatic
            // choice is the one that still answers what the install holds.
            assertThat(FaceChoice.fromLabel("insignia99LTaa"))
                .isEqualTo(FaceChoice.AUTO_FACE);
        }

        @Test
        void fromLabelFallsBackToTheAutomaticChoiceWhenNothingIsStored() {

            assertThat(FaceChoice.fromLabel(null))
                .isEqualTo(FaceChoice.AUTO_FACE);
        }
    }

    @Nested
    class GetLabel {

        @Test
        void getLabelStatesTheAutomaticChoiceAsAuto() {
            // The stored value: renaming it silently returns every save's pick to the default.
            assertThat(FaceChoice.AUTO_FACE.getLabel())
                .isEqualTo("Auto");
        }

        @Test
        void getLabelStatesANamedFaceByItsBasename() {

            assertThat(new FaceChoice.NamedFace(StarsectorFont.VANILLA_VICTOR_10).getLabel())
                .isEqualTo("victor10");
        }
    }

    @Nested
    class SelectByCase {

        @Test
        void selectByCaseAnswersTheAutomaticCaseForTheAutomaticChoice() {

            assertThat(FaceChoice.AUTO_FACE.selectByCase(() -> "auto", StarsectorFont::getBasename))
                .isEqualTo("auto");
        }

        @Test
        void selectByCaseHandsTheNamedCaseItsFace() {

            assertThat(new FaceChoice.NamedFace(StarsectorFont.VANILLA_ORBITRON_20AA)
                    .selectByCase(() -> "auto", StarsectorFont::getBasename))
                .isEqualTo("orbitron20aa");
        }
    }
}
