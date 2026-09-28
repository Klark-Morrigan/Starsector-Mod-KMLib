package kmlib.testfixtures.starsector.save;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins what the fixture reads off a save: every owned field as an element path, a collaborator's field as an empty
 * element whatever the collaborator holds, a transient field not at all; a read-back restoring owned values and
 * dropping collaborators and transient fields; and an expected-paths resource read line by line, blanks skipped.
 */
final class SaveFormatFixtureTest {

    private static final String LEDGER = "kmlib.testfixtures.starsector.save.SaveFormatFixtureTest_-Ledger";
    private static final String OWNER = "tritachyon";

    private SaveFormatFixture fixture;

    @BeforeEach
    void setUp() {

        fixture = SaveFormatFixture.createStandingInFor(Collaborator.class);
    }

    @Nested
    class ListElementPaths {

        @Test
        void listsEveryOwnedFieldAndTheCollaboratorFieldAloneSorted() {

            assertThat(fixture.listElementPaths(new Ledger(OWNER)))
                .containsExactly(
                    LEDGER,
                    LEDGER + "/collaborator",
                    LEDGER + "/entries",
                    LEDGER + "/entries/kmlib.testfixtures.starsector.save.SaveFormatFixtureTest_-Entry",
                    LEDGER + "/entries/kmlib.testfixtures.starsector.save.SaveFormatFixtureTest_-Entry/amount",
                    LEDGER + "/owner");
        }
    }

    @Nested
    class ReadExpectedPaths {

        @Test
        void readsTheNonBlankLinesInFileOrder() {

            assertThat(SaveFormatFixture.readExpectedPaths(SaveFormatFixtureTest.class, "expected-paths.txt"))
                .containsExactly("root", "root/child");
        }

        @Test
        void refusesAResourceThatIsNotThere() {

            assertThatThrownBy(() -> SaveFormatFixture.readExpectedPaths(SaveFormatFixtureTest.class, "absent.txt"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("absent.txt");
        }
    }

    @Nested
    class RestoreThroughSave {

        @Test
        void dropsTheCollaboratorAndTheTransientField() {

            var restored = fixture.restoreThroughSave(new Ledger(OWNER));

            assertThat(restored.collaborator)
                .isNull();
            assertThat(restored.cache)
                .isNull();
        }

        @Test
        void restoresTheOwnedValues() {

            var restored = fixture.restoreThroughSave(new Ledger(OWNER));

            assertThat(restored.owner)
                .isEqualTo(OWNER);
            assertThat(restored.entries)
                .extracting(entry -> entry.amount)
                .containsExactly(5);
        }
    }

    @Nested
    class WriteSave {

        @Test
        void writesNothingOfWhatTheCollaboratorHolds() {

            assertThat(fixture.writeSave(new Ledger(OWNER)))
                .contains("<collaborator")
                .doesNotContain("internals");
        }
    }

    /** A collaborator the graph points at without owning, holding state of its own. */
    static class Collaborator {

        private final String internals = "not the graph's";
    }

    /** An owned entry. */
    static final class Entry {

        private final int amount;

        Entry(int amount) {

            this.amount = amount;
        }
    }

    /** The persisted root: owned values, a collection of entries, a collaborator and a transient cache. */
    static final class Ledger {

        private transient String cache = "derived";
        private final Collaborator collaborator = new Collaborator();
        private final ArrayList<Entry> entries = new ArrayList<>(List.of(new Entry(5)));
        private final String owner;

        Ledger(String owner) {

            this.owner = owner;
        }
    }
}
