package kmlib.settings;

import kmlib.testfixtures.logging.LogAppenderFake;
import kmlib.testfixtures.starsector.settings.LunaSettingsStoreFake;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link LunaSettingsWriter}: what each write path leaves in the store, when it reaches disk,
 * and who is told about it.
 *
 * <p>What is load-bearing is the difference between the two paths. Both leave the same value in the
 * store, so a case asserting only the stored value would pass whichever path ran - the saves are
 * counted instead, since the deferred path exists to make a burst of edits cost one disk write.
 *
 * <p>Each method's cases live in a {@link Nested} group so the suite reports as a per-method tree.
 */
final class LunaSettingsWriterTest {

    private static final String MOD_ID = "kmu";

    private LunaSettingsStoreFake storeFake;
    private LunaSettingsWriter writer;

    @BeforeEach
    void setUp() {

        storeFake = new LunaSettingsStoreFake();
        storeFake.openStoreFor(MOD_ID);
        writer = new LunaSettingsWriter(storeFake);
    }

    @Nested
    class PutString {

        @Test
        void storesTheValueAndSavesItAtOnce() {

            writer.putString(MOD_ID, "layer_style", "Banded");

            assertThat(storeFake.readStoredValue(MOD_ID, "layer_style"))
                .isEqualTo("Banded");
            assertThat(storeFake.countSavesOf(MOD_ID))
                .isEqualTo(1);
        }

        @Test
        void tellsTheListenersTheSettingsChanged() {
            // The whole point of writing through here rather than into the file: the settings
            // screen and the mod's change-driven state rebuild against what was just written.
            writer.putString(MOD_ID, "layer_style", "Banded");

            assertThat(storeFake.readAnnouncedMods())
                .containsExactly(MOD_ID);
        }

        @Test
        void staysSilentAndWritesNothingWithoutAStore() {
            // LunaLib has not loaded this mod's settings, so there is nowhere for the value to go.
            // A skipped write is said out loud, since the control the player just used will
            // otherwise appear to have done nothing.
            storeFake.loseStoreFor(MOD_ID);

            var log = LogAppenderFake.captureLogOf(
                LunaSettingsWriter.class,
                () -> writer.putString(MOD_ID, "layer_style", "Banded"));

            assertThat(storeFake.readStoredValue(MOD_ID, "layer_style"))
                .isNull();
            assertThat(storeFake.readAnnouncedMods())
                .isEmpty();
            assertThat(log.getMessages())
                .hasSize(1);
            assertThat(log.getMessages().get(0))
                .contains(MOD_ID)
                .contains("layer_style");
        }
    }

    @Nested
    class PutBoolean {

        @Test
        void storesTheValueAndSavesItAtOnce() {

            writer.putBoolean(MOD_ID, "show_borders", true);

            assertThat(storeFake.readStoredValue(MOD_ID, "show_borders"))
                .isEqualTo(true);
            assertThat(storeFake.countSavesOf(MOD_ID))
                .isEqualTo(1);
        }
    }

    @Nested
    class PutStringDeferred {

        @Test
        void storesTheValueLiveWithoutSavingIt() {
            // The readers see the new value at once - they read the same store - while the file
            // write waits, which is what makes this path affordable for a control being dragged.
            writer.putStringDeferred(MOD_ID, "layer_style", "Banded");

            assertThat(storeFake.readStoredValue(MOD_ID, "layer_style"))
                .isEqualTo("Banded");
            assertThat(storeFake.countSavesOf(MOD_ID))
                .isEqualTo(0);
            assertThat(storeFake.readAnnouncedMods())
                .containsExactly(MOD_ID);
        }
    }

    @Nested
    class PutBooleanDeferred {

        @Test
        void storesTheValueLiveWithoutSavingIt() {

            writer.putBooleanDeferred(MOD_ID, "show_borders", true);

            assertThat(storeFake.readStoredValue(MOD_ID, "show_borders"))
                .isEqualTo(true);
            assertThat(storeFake.countSavesOf(MOD_ID))
                .isEqualTo(0);
        }
    }

    @Nested
    class FlushPendingWrites {

        @Test
        void collapsesABurstOfDeferredEditsIntoOneSave() {
            // What the deferred path is for, stated as the cost it saves: three edits of a control
            // the player is dragging are one disk write rather than three.
            writer.putStringDeferred(MOD_ID, "layer_style", "Banded");
            writer.putStringDeferred(MOD_ID, "layer_style", "Solid");
            writer.putBooleanDeferred(MOD_ID, "show_borders", true);

            writer.flushPendingWrites(MOD_ID);

            assertThat(storeFake.countSavesOf(MOD_ID))
                .isEqualTo(1);
        }

        @Test
        void savesNothingForAModThatHasNoDeferredWrites() {
            // A settling point reached with nothing pending must not rewrite an unchanged file.
            writer.flushPendingWrites(MOD_ID);

            assertThat(storeFake.countSavesOf(MOD_ID))
                .isEqualTo(0);
        }

        @Test
        void savesNothingASecondTimeForAModAlreadyFlushed() {
            // The mark is what says a store is owed a write, so a flush that made one has to clear
            // it - left set, every later settling point would rewrite the same file.
            writer.putStringDeferred(MOD_ID, "layer_style", "Banded");
            writer.flushPendingWrites(MOD_ID);
            writer.flushPendingWrites(MOD_ID);

            assertThat(storeFake.countSavesOf(MOD_ID))
                .isEqualTo(1);
        }

        @Test
        void keepsTheMarkForALaterRetryWhenTheSaveIsRefused() {
            // The value is live in memory either way, so only persistence is at stake - a disk that
            // refused the file once may take it at the next settling point, and the mark is what
            // makes that attempt happen at all.
            writer.putStringDeferred(MOD_ID, "layer_style", "Banded");
            storeFake.refuseSaves();
            writer.flushPendingWrites(MOD_ID);

            assertThat(storeFake.countSavesOf(MOD_ID))
                .isEqualTo(0);

            storeFake.allowSaves();
            writer.flushPendingWrites(MOD_ID);

            assertThat(storeFake.countSavesOf(MOD_ID))
                .isEqualTo(1);
        }

        @Test
        void dropsTheMarkForAModWhoseStoreIsGone() {
            // Nothing to save, and nothing a later flush could do about it, so the mark goes rather
            // than the writer retrying a write that can never land.
            writer.putStringDeferred(MOD_ID, "layer_style", "Banded");
            storeFake.loseStoreFor(MOD_ID);
            writer.flushPendingWrites(MOD_ID);
            storeFake.openStoreFor(MOD_ID);
            writer.flushPendingWrites(MOD_ID);

            assertThat(storeFake.countSavesOf(MOD_ID))
                .isEqualTo(0);
        }

        @Test
        void flushesEveryModCarryingDeferredWrites() {
            // The mod-agnostic form, for a caller that knows a settling point has been reached but
            // not which mods were edited on the way to it.
            storeFake.openStoreFor("kmo");

            writer.putStringDeferred(MOD_ID, "layer_style", "Banded");
            writer.putStringDeferred("kmo", "site_style", "Compact");

            writer.flushPendingWrites();

            assertThat(storeFake.countSavesOf(MOD_ID))
                .isEqualTo(1);
            assertThat(storeFake.countSavesOf("kmo"))
                .isEqualTo(1);
        }
    }

    @Nested
    class RemoveSetting {

        @Test
        void shedsTheRetiredFieldAndSavesAtOnce() {

            storeFake.storeValue(MOD_ID, "retired_field", "stale");

            writer.removeSetting(MOD_ID, "retired_field");

            assertThat(storeFake.readStoredValue(MOD_ID, "retired_field"))
                .isNull();
            assertThat(storeFake.countSavesOf(MOD_ID))
                .isEqualTo(1);
        }

        @Test
        void tellsNobodyTheSettingsChanged() {
            // A retired field is one nothing reads, so announcing its removal would only make every
            // consumer rebuild over a value that changed for nobody.
            storeFake.storeValue(MOD_ID, "retired_field", "stale");

            writer.removeSetting(MOD_ID, "retired_field");

            assertThat(storeFake.readAnnouncedMods())
                .isEmpty();
        }

        @Test
        void savesNothingForAFieldTheStoreDoesNotHold() {
            // The common case on every load after the sweep has run once, and on every fresh
            // install - so the sweep costs no disk write at all.
            writer.removeSetting(MOD_ID, "never_stored");

            assertThat(storeFake.countSavesOf(MOD_ID))
                .isEqualTo(0);
        }

        @Test
        void settlesAPendingMarkSinceTheSaveWroteTheWholeStore() {
            // The removal's save rewrote the file, deferred edits and all, so a mark left set would
            // have the next flush rewrite an unchanged file.
            storeFake.storeValue(MOD_ID, "retired_field", "stale");

            writer.putStringDeferred(MOD_ID, "layer_style", "Banded");
            writer.removeSetting(MOD_ID, "retired_field");
            writer.flushPendingWrites(MOD_ID);

            assertThat(storeFake.countSavesOf(MOD_ID))
                .isEqualTo(1);
        }
    }
}
