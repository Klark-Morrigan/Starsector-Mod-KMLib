package kmlib.settings;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the label lookup: a stored label resolves to its option, and an unmatched or null label
 * (unset setting, or a label from an option that no longer exists) resolves to the fallback.
 */
class LabeledChoicesTest {

    private enum SampleChoice implements LabeledChoice {
        FIRST("First"),
        SECOND("Second");

        private final String label;

        SampleChoice(String label) {
            this.label = label;
        }

        @Override
        public String getLabel() {
            return label;
        }
    }

    @Nested
    class FromLabel {
        @Test
        void resolvesTheOptionWhoseLabelMatches() {
            var resolved = LabeledChoices.fromLabel(SampleChoice.values(), "Second",
                SampleChoice.FIRST);

            assertThat(resolved).isEqualTo(SampleChoice.SECOND);
        }

        @Test
        void fallsBackWhenNoLabelMatches() {
            var resolved = LabeledChoices.fromLabel(SampleChoice.values(), "Third",
                SampleChoice.FIRST);

            assertThat(resolved).isEqualTo(SampleChoice.FIRST);
        }

        @Test
        void fallsBackWhenLabelIsNull() {
            var resolved = LabeledChoices.fromLabel(SampleChoice.values(), null,
                SampleChoice.SECOND);

            assertThat(resolved).isEqualTo(SampleChoice.SECOND);
        }
    }
}
