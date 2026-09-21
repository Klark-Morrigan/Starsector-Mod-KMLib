package kmlib.starsector.compatibility;

import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;
import kmlib.testfixtures.starsector.strings.ShippedStrings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Composes the modal a player reads out of the wording KMLib actually ships.
 *
 * <p>The one check the unit suite cannot make: that the shipped templates and the order the record
 * fills them in still agree. A template whose slots were reordered in {@code strings.json} would
 * pass every case written against a copy of it and reach a player with the versions swapped.
 *
 * <p>Asserted as the whole modal rather than row by row, because the modal is a block: which rows
 * there are, what order they come in and how their labels line up are as much what a player reads as
 * the values in them. A row whose padding was edited out of column in the shipped file is a change
 * worth failing on.
 */
final class CompatibilityFailureIntegrationTest {

    @BeforeEach
    void installShippedStrings() {

        var stringsByKey = ShippedStrings.readStringsByKey();

        StarsectorSettingsFake.installSettings((category, key) -> stringsByKey.get(key));
    }

    @AfterEach
    void clearSettings() {

        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class DescribeForPlayer {

        @Test
        void readsAsTheModalWhereBothVersionsWereRead() {

            // Both versions arrive with the prefix their subjects self-report, and neither reaches
            // the player with it: the label already says a version is what follows, and the two
            // versions come from two places that need not agree about carrying one.
            var failure = CompatibilityFailureFixture.createFailureBetweenVersions("v0.8.8", "v0.9.1");

            assertThat(failure.describeForPlayer())
                .isEqualTo("Fast Rendering version mismatch. See starsector.log for details."
                    + "\n\n    Mod:        " + CompatibilityFailureFixture.MAP_OVERLAY_MOD_ID
                    + "\n    Built for:  0.8.8"
                    + "\n    Installed:  0.9.1"
                    + "\n    Effect:     " + CompatibilityFailureFixture.LOST_FEATURE
                    + "\n    No effect:  " + CompatibilityFailureFixture.UNAFFECTED_FEATURE);
        }

        @Test
        void readsAsTheModalWhereNeitherVersionWasRead() {

            var failure = CompatibilityFailureFixture.createFailure();

            assertThat(failure.describeForPlayer())
                .isEqualTo("Fast Rendering version mismatch. See starsector.log for details."
                    + "\n\n    Mod:        " + CompatibilityFailureFixture.MAP_OVERLAY_MOD_ID
                    + "\n    Built for:  unknown"
                    + "\n    Installed:  unknown"
                    + "\n    Effect:     " + CompatibilityFailureFixture.LOST_FEATURE
                    + "\n    No effect:  " + CompatibilityFailureFixture.UNAFFECTED_FEATURE);
        }
    }
}
