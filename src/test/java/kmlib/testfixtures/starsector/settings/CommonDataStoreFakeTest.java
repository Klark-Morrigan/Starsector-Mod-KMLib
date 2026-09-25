package kmlib.testfixtures.starsector.settings;

import org.json.JSONObject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the common-data folder a writer is exercised against: what is written is what is read back, each
 * write is counted, and both posed failures fail open without emptying what the folder holds.
 */
final class CommonDataStoreFakeTest {

    private static final String FILE_NAME = "kmu_preferences.json";

    private static final JSONObject CONTENT = new JSONObject();

    @Nested
    class ReadJsonFile {

        @Test
        void whatWasWrittenIsReadBack() {

            var storeFake = new CommonDataStoreFake();

            storeFake.writeJsonFile(FILE_NAME, CONTENT);

            assertThat(storeFake.readJsonFile(FILE_NAME))
                .isSameAs(CONTENT);
        }

        @Test
        void aFileNeverWrittenReadsAsNothing() {

            assertThat(new CommonDataStoreFake().readJsonFile(FILE_NAME))
                .isNull();
        }

        @Test
        void aRefusedReadAnswersNothingWhileTheFileStaysHeld() {

            var storeFake = new CommonDataStoreFake();

            storeFake.storeFile(FILE_NAME, CONTENT);
            storeFake.refuseReads();

            assertThat(storeFake.readJsonFile(FILE_NAME))
                .isNull();
            assertThat(storeFake.readStoredFile(FILE_NAME))
                .isSameAs(CONTENT);
        }
    }

    @Nested
    class WriteJsonFile {

        @Test
        void aWriteIsStoredAndCountedEachTime() {

            var storeFake = new CommonDataStoreFake();

            // The same content twice leaves the map unchanged, which is what the count is there to show.
            assertThat(storeFake.writeJsonFile(FILE_NAME, CONTENT))
                .isTrue();
            assertThat(storeFake.writeJsonFile(FILE_NAME, CONTENT))
                .isTrue();

            assertThat(storeFake.hasStoredFile(FILE_NAME))
                .isTrue();
            assertThat(storeFake.countWritesTo(FILE_NAME))
                .isEqualTo(2);
        }

        @Test
        void aRefusedWriteReportsFailureAndLeavesNoTrace() {

            var storeFake = new CommonDataStoreFake();

            storeFake.refuseWrites();

            assertThat(storeFake.writeJsonFile(FILE_NAME, CONTENT))
                .isFalse();
            assertThat(storeFake.hasStoredFile(FILE_NAME))
                .isFalse();
            assertThat(storeFake.countWritesTo(FILE_NAME))
                .isZero();
        }
    }

    @Nested
    class StoreFile {

        @Test
        void aSeededFileIsHeldAndCountsAsNoWrite() {

            var storeFake = new CommonDataStoreFake();

            storeFake.storeFile(FILE_NAME, CONTENT);

            assertThat(storeFake.readJsonFile(FILE_NAME))
                .isSameAs(CONTENT);
            assertThat(storeFake.countWritesTo(FILE_NAME))
                .isZero();
        }
    }
}
