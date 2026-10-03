package kmlib.starsector.compatibility;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Covers the guards that stand in for a type system this value has none of.
 *
 * <p>Its components are all strings, so the compiler cannot tell a mod ID from a feature key from a
 * whole sentence. The two keys are checked for a key's shape, since a sentence there would become a
 * latch. The sentences are not checked, since no shape tells a sentence from a key in every language,
 * and refusing one costs the report it was built for.
 */
final class CompatibilityConsumerTests {

    // "The map will not respond to the cursor." (U+5730 U+56FE U+5C06 U+65E0 U+6CD5 U+54CD U+5E94
    // U+5149 U+6807 U+3002) - a whole sentence in a language written without spaces.
    private static final String CHINESE_LOST_FEATURE = "地图将无法响应光标。";

    private static final String MOD_ID = "some-mod";

    private static final String FEATURE_KEY = "map-cursor";

    private static final String LOST_FEATURE = "The map will not respond to the cursor this session.";

    private static final String UNAFFECTED_FEATURE = "On everything else, including your save.";

    @Nested
    class Constructor {

        @Test
        void refusesAConsumerWithNoModId() {

            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CompatibilityConsumer(" ", FEATURE_KEY, LOST_FEATURE));
        }

        @Test
        void refusesASentenceWhereTheModIdBelongs() {

            // The transposition that matters: a mod handing its three strings over in the wrong
            // order would otherwise latch under a sentence and put a key in front of a player.
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CompatibilityConsumer(LOST_FEATURE, FEATURE_KEY, LOST_FEATURE));
        }

        @Test
        void refusesAConsumerWithNoFeatureKey() {

            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CompatibilityConsumer(MOD_ID, " ", LOST_FEATURE));
        }

        @Test
        void refusesASentenceWhereTheFeatureKeyBelongs() {

            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CompatibilityConsumer(MOD_ID, LOST_FEATURE, LOST_FEATURE));
        }

        @Test
        void refusesAConsumerNamingNothingLost() {

            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CompatibilityConsumer(MOD_ID, FEATURE_KEY, " "));
        }

        @Test
        void takesBothSentencesWrittenWithoutSpaces() {

            // The value is built where a failure is being reported, so refusing a sentence would
            // cost the player the report.
            var consumer = new CompatibilityConsumer(MOD_ID, FEATURE_KEY, CHINESE_LOST_FEATURE, CHINESE_LOST_FEATURE);

            assertThat(consumer.lostFeature())
                .isEqualTo(CHINESE_LOST_FEATURE);
            assertThat(consumer.unaffectedFeature())
                .isEqualTo(CHINESE_LOST_FEATURE);
        }

        @Test
        void takesAConsumerThatSaysNothingAboutWhatStillWorks() {

            // The absent slot is absent rather than wrong: a mod with nothing to add is the
            // ordinary case, and the three-argument form is what it uses.
            assertThat(new CompatibilityConsumer(MOD_ID, FEATURE_KEY, LOST_FEATURE).unaffectedFeature())
                .isNull();
        }
    }

    @Nested
    class ConsumerKey {

        @Test
        void composesTheKeyFromTheModAndTheFeature() {

            // Composed rather than taken whole, which is what stops two mods spelling one key and
            // ending up behind a single latch.
            assertThat(new CompatibilityConsumer(MOD_ID, FEATURE_KEY, LOST_FEATURE).consumerKey())
                .isEqualTo("some-mod:map-cursor");
        }

        @Test
        void tellsTwoFeaturesOfOneModApart() {

            var mapCursor = new CompatibilityConsumer(MOD_ID, FEATURE_KEY, LOST_FEATURE);
            var colonyPanel = new CompatibilityConsumer(MOD_ID, "colony-panel", LOST_FEATURE);

            assertThat(mapCursor.consumerKey())
                .isNotEqualTo(colonyPanel.consumerKey());
        }

        @Test
        void tellsTwoModsTakingOneFeatureApart() {

            var ourMod = new CompatibilityConsumer(MOD_ID, FEATURE_KEY, LOST_FEATURE);
            var anotherMod = new CompatibilityConsumer("another-mod", FEATURE_KEY, LOST_FEATURE);

            assertThat(ourMod.consumerKey())
                .isNotEqualTo(anotherMod.consumerKey());
        }
    }

    @Nested
    class ResolveConsumerAtPosition {

        @Test
        void answersItselfAtTheFirstPosition() {

            // Which position keeps the bare key is this value's rule rather than the record's, so
            // the first position is answered here rather than refused for the record to handle.
            var consumer = new CompatibilityConsumer(MOD_ID, FEATURE_KEY, LOST_FEATURE);

            assertThat(consumer.resolveConsumerAtPosition(1))
                .isSameAs(consumer);
        }

        @Test
        void numbersTheFeatureHalfOfTheKeyAfterTheFirstPosition() {

            // The feature is what collided - the mod half is the mod's own ID - so the number lands
            // there, and the key still reads as that mod and one feature of it.
            assertThat(new CompatibilityConsumer(MOD_ID, FEATURE_KEY, LOST_FEATURE)
                .resolveConsumerAtPosition(2)
                .consumerKey())
                .isEqualTo("some-mod:map-cursor-2");
        }

        @Test
        void keepsTheModAndBothSentences() {

            var numbered = new CompatibilityConsumer(MOD_ID, FEATURE_KEY, LOST_FEATURE, UNAFFECTED_FEATURE)
                .resolveConsumerAtPosition(2);

            assertThat(numbered.modId())
                .isEqualTo(MOD_ID);
            assertThat(numbered.lostFeature())
                .isEqualTo(LOST_FEATURE);
            assertThat(numbered.unaffectedFeature())
                .isEqualTo(UNAFFECTED_FEATURE);
        }

        @Test
        void refusesAPositionBelowTheFirst() {

            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CompatibilityConsumer(MOD_ID, FEATURE_KEY, LOST_FEATURE)
                    .resolveConsumerAtPosition(0));
        }
    }

    @Nested
    class HasUnaffectedFeature {

        @Test
        void answersYesWhereTheModSuppliedOne() {

            assertThat(new CompatibilityConsumer(MOD_ID, FEATURE_KEY, LOST_FEATURE, UNAFFECTED_FEATURE)
                .hasUnaffectedFeature())
                .isTrue();
        }

        @Test
        void answersNoWhereTheModSuppliedNone() {

            assertThat(new CompatibilityConsumer(MOD_ID, FEATURE_KEY, LOST_FEATURE)
                .hasUnaffectedFeature())
                .isFalse();
        }
    }
}
