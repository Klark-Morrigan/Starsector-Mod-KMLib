package kmlib.starsector.ui.font;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FaceOfferTest {

    @Nested
    class Constructor {

        @Test
        void constructorRefusesAFallbackNamingThePreferredFace() {
            // Falling back to the face that just failed is no fallback at all.
            assertThatThrownBy(() -> new FaceOffer(
                    StarsectorFont.VANILLA_INSIGNIA_42,
                    Optional.of(StarsectorFont.VANILLA_INSIGNIA_42)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("insignia42LTaa");
        }
    }

    @Nested
    class CreateOfferWithoutFallback {

        @Test
        void createOfferWithoutFallbackOffersThePreferredFaceAlone() {

            var offer = FaceOffer.createOfferWithoutFallback(StarsectorFont.VANILLA_VICTOR_10);

            assertThat(offer.preferredFont())
                .isEqualTo(StarsectorFont.VANILLA_VICTOR_10);
            assertThat(offer.fallbackFont())
                .isEmpty();
        }
    }

    @Nested
    class CreateOfferFallingBackTo {

        @Test
        void createOfferFallingBackToOffersThePreferredFaceAndItsFallback() {

            var offer = FaceOffer.createOfferFallingBackTo(
                StarsectorFont.VANILLA_INSIGNIA_42,
                StarsectorFont.VANILLA_INSIGNIA_25);

            assertThat(offer.preferredFont())
                .isEqualTo(StarsectorFont.VANILLA_INSIGNIA_42);
            assertThat(offer.fallbackFont())
                .contains(StarsectorFont.VANILLA_INSIGNIA_25);
        }
    }
}
