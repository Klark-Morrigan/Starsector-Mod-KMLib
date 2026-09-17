package kmlib.starsector.compatibility;

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

            var failure = new CompatibilityFailure(
                new CompatibilitySubject("Fast Rendering", "v0.8.8", "v0.9.1"),
                "Sector map overlays will not respond to the cursor this session.",
                "GLCommand is absent",
                null);

            assertThat(failure.describeForPlayer())
                .isEqualTo("Fast Rendering version mismatch"
                    + "\n\nKMLib was built against Fast Rendering v0.8.8, and this install has v0.9.1."
                    + "\n\nSector map overlays will not respond to the cursor this session."
                    + " Everything else, including your save, is unaffected."
                    + " See starsector.log for which part is mismatched.");
        }

        @Test
        void readsAsTheModalWhereNeitherVersionWasRead() {

            var failure = new CompatibilityFailure(
                new CompatibilitySubject("Fast Rendering", null, null),
                "Sector map overlays will not respond to the cursor this session.",
                "GLCommand is absent",
                null);

            assertThat(failure.describeForPlayer())
                .isEqualTo("Fast Rendering version mismatch"
                    + "\n\nKMLib was built against Fast Rendering (version unknown),"
                    + " and this install's version could not be read."
                    + " It is either newer and carries breaking changes, or too old for this build."
                    + "\n\nSector map overlays will not respond to the cursor this session."
                    + " Everything else, including your save, is unaffected."
                    + " See starsector.log for which part is mismatched.");
        }
    }
}
