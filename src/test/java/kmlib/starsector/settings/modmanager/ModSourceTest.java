package kmlib.starsector.settings.modmanager;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the one question a source answers about itself: whether it has a mod id to offer beside its
 * name.
 *
 * <p>Three answers rather than two, because absence arrives two ways. A source that is not a mod is
 * built with no id at all, while one built from a spec takes whatever that spec reports - and a
 * blank string there is the mod manager saying nothing rather than saying "". A caller that treated
 * the second as an id would print an empty bracket and pass it on as an argument no command
 * resolves.
 */
final class ModSourceTest {

    @Nested
    class HasModId {

        @Test
        void reportsAnIdForASourceTheModManagerAccountsFor() {

            assertThat(new ModSource("Tahlan Shipworks", "tahlan_shipworks").hasModId())
                .isTrue();
        }

        @Test
        void reportsNoIdForASourceThatIsNotAnInstalledMod() {
            // The base game and a folder no enabled mod is installed in both arrive this way.
            assertThat(new ModSource("vanilla", null).hasModId())
                .isFalse();
        }

        @Test
        void reportsNoIdForAModWhoseSpecNamesABlankOne() {

            assertThat(new ModSource("Some Mod", "   ").hasModId())
                .isFalse();
        }
    }

    @Nested
    class Construct {

        @Test
        void rejectsASourceWithNoName() {
            // The name is the half that is always printed, so a source without one is a line the
            // listing could not write - worth failing where it is built rather than where it shows.
            assertThatThrownBy(() -> new ModSource(null, "tahlan_shipworks"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("sourceName");
        }
    }
}
