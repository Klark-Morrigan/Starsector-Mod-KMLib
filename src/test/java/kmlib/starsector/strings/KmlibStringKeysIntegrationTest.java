package kmlib.starsector.strings;

import kmlib.testfixtures.starsector.strings.ShippedStrings;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shipped strings file against the code that names its keys, both directions, through the
 * reads {@link ShippedStrings} holds.
 *
 * <p>The guard earns its place here more than it would over text the player reads constantly. Most
 * of what KMLib's file carries is drawn only when a binding to third-party code has already stopped
 * holding - a path that runs on almost no install, and never on the one a release was played on -
 * so a missing key survives every playtest and reaches a player at the one moment the message
 * mattered.
 */
final class KmlibStringKeysIntegrationTest {

    @Nested
    class ShippedStringIds {

        @Test
        void everyStringIdKmlibStringKeysNamesIsDeclaredInTheFile() {
            // A constant naming a key the file never declares reads as [REDACTED] wherever it is
            // drawn - visible to the player, invisible to every suite, since each hands its subject
            // a resolver rather than the shipped file.
            assertThat(ShippedStrings.readStringsByKey())
                .containsKeys(ShippedStrings.readStringIdsByConstantName(KmlibStringKeys.class)
                    .values()
                    .toArray(String[]::new));
        }

        @Test
        void everyStringIdDeclaredInTheFileIsNamedByKmlibStringKeys() {
            // The opposite drift, and the quieter one: a key nothing names is wording that ships,
            // is translated, and is never drawn - a rename that left the old row behind.
            assertThat(ShippedStrings.readStringIdsByConstantName(KmlibStringKeys.class).values())
                .containsExactlyInAnyOrderElementsOf(ShippedStrings.readStringsByKey().keySet());
        }
    }
}
