package kmlib.starsector.ui.font;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FaceOfferTest {

    // A map label's offer: the high-resolution atlas preferred, two smaller ones behind it.
    private static FaceOffer createLabelOffer() {

        return new FaceOffer(
            StarsectorFont.VANILLA_INSIGNIA_42,
            List.of(StarsectorFont.VANILLA_INSIGNIA_15, StarsectorFont.VANILLA_ORBITRON_20AA));
    }

    @Nested
    class Constructor {

        @Test
        void constructorRefusesAnAlternativeRepeatingThePreferredFace() {
            // A Radio would list the face twice under one stored label.
            assertThatThrownBy(() -> new FaceOffer(
                    StarsectorFont.VANILLA_INSIGNIA_42,
                    List.of(StarsectorFont.VANILLA_INSIGNIA_42)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("insignia42LTaa");
        }

        @Test
        void constructorRefusesAnAlternativeOfferedTwice() {

            assertThatThrownBy(() -> new FaceOffer(
                    StarsectorFont.VANILLA_INSIGNIA_42,
                    List.of(StarsectorFont.VANILLA_VICTOR_10, StarsectorFont.VANILLA_VICTOR_10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("victor10");
        }
    }

    @Nested
    class ListOfferedFonts {

        @Test
        void listOfferedFontsLeadsWithThePreferredFaceThenTheAlternativesInOrder() {

            assertThat(createLabelOffer().listOfferedFonts())
                .containsExactly(
                    StarsectorFont.VANILLA_INSIGNIA_42,
                    StarsectorFont.VANILLA_INSIGNIA_15,
                    StarsectorFont.VANILLA_ORBITRON_20AA);
        }
    }

    @Nested
    class ListChoiceLabels {

        @Test
        void listChoiceLabelsListsTheAutomaticChoiceFirstThenEachFaceByBasename() {
            // The Radio's options as a settings table spells them, and the values it stores.
            assertThat(createLabelOffer().listChoiceLabels())
                .containsExactly("Auto", "insignia42LTaa", "insignia15LTaa", "orbitron20aa");
        }
    }

    @Nested
    class IsFontOffered {

        @Test
        void isFontOfferedIsTrueForThePreferredFace() {

            assertThat(createLabelOffer().isFontOffered(StarsectorFont.VANILLA_INSIGNIA_42))
                .isTrue();
        }

        @Test
        void isFontOfferedIsTrueForAnAlternative() {

            assertThat(createLabelOffer().isFontOffered(StarsectorFont.VANILLA_ORBITRON_20AA))
                .isTrue();
        }

        @Test
        void isFontOfferedIsFalseForAFaceTheOfferLeavesOut() {

            assertThat(createLabelOffer().isFontOffered(StarsectorFont.VANILLA_VICTOR_10))
                .isFalse();
        }
    }
}
