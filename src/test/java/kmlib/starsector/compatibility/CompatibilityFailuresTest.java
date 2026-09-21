package kmlib.starsector.compatibility;

import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture.createFailure;
import static kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture.createFailureBrokenAt;
import static kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture.createFailureTakenBy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Covers the two rules the registry holds - one record per binding for the session, and a take that
 * hands over without unlatching - and that the first rule survives two threads recording at once.
 *
 * <p>A binding is a third party and the mod that took it, so the cases worth the most are the ones
 * that pull those apart: two mods over one third party each record, one mod recording the same
 * third party twice records once, and one mod filing two features under one key records both. A
 * latch on the third party alone passes every other case here, and a latch on the pair alone passes
 * all but the last.
 */
final class CompatibilityFailuresTest {

    private static final String FAST_RENDERING = CompatibilityFailureFixture.FAST_RENDERING_SUBJECT_KEY;

    private static final String NEXERELIN = CompatibilityFailureFixture.NEXERELIN_SUBJECT_KEY;

    private static final CompatibilityConsumer MAP_OVERLAY = CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER;

    private static final CompatibilityConsumer COLONY_PANEL = CompatibilityFailureFixture.COLONY_PANEL_CONSUMER;

    // What two more of that mod's features lose, each named once so a case asserts the sentence it
    // filed rather than a literal spelled again beside it.
    private static final String MAP_LEGEND_LOST_FEATURE =
        "Map legends will not name their factions this session.";

    private static final String MAP_SEARCH_LOST_FEATURE =
        "Map search will not find systems this session.";

    // The same mod under the same feature key as MAP_OVERLAY, losing something else: what one mod
    // spelling one feature key for two features looks like at the record.
    private static final CompatibilityConsumer MAP_LEGEND_UNDER_REUSED_KEY = new CompatibilityConsumer(
        CompatibilityFailureFixture.MAP_OVERLAY_MOD_ID,
        CompatibilityFailureFixture.MAP_OVERLAY_FEATURE_KEY,
        MAP_LEGEND_LOST_FEATURE);

    private static final CompatibilityConsumer MAP_SEARCH_UNDER_REUSED_KEY = new CompatibilityConsumer(
        CompatibilityFailureFixture.MAP_OVERLAY_MOD_ID,
        CompatibilityFailureFixture.MAP_OVERLAY_FEATURE_KEY,
        MAP_SEARCH_LOST_FEATURE);

    // Another mod spelling the same feature key, which is no collision: the mod ID leads the key.
    private static final CompatibilityConsumer ANOTHER_MODS_MAP_OVERLAY = new CompatibilityConsumer(
        "another-map-mod",
        CompatibilityFailureFixture.MAP_OVERLAY_FEATURE_KEY,
        "Another map will not respond to the cursor this session.");

    private final CompatibilityFailures failures = new CompatibilityFailures();

    @Nested
    class HasUnreported {

        @Test
        void isFalseWhereNothingWasRecorded() {

            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void isTrueOnceAFailureWasRecorded() {

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailure());

            assertThat(failures.hasUnreported())
                .isTrue();
        }

        @Test
        void isFalseAgainOnceTheFailureWasTaken() {

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailure());
            failures.takeNextUnreported();

            assertThat(failures.hasUnreported())
                .isFalse();
        }
    }

    @Nested
    class RecordOnce {

        @Test
        void keepsTheFirstFailureForABindingAndIgnoresTheSecond() {

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailureBrokenAt("first"));
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailureBrokenAt("second"));

            assertThat(failures.takeNextUnreported().breakage().brokenDetail())
                .isEqualTo("first");
            assertThat(failures.takeNextUnreported())
                .isNull();
        }

        @Test
        void doesNotInvokeTheDescriberOnTheIgnoredRecord() {

            var describeCount = new AtomicInteger();

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> countAndCreateFailure(describeCount));
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> countAndCreateFailure(describeCount));

            assertThat(describeCount)
                .hasValue(1);
        }

        @Test
        void handsTheDescriberTheConsumerItWasGivenUnderAKeyNobodyReused() {

            var handedConsumer = new AtomicReference<CompatibilityConsumer>();

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> {
                handedConsumer.set(recordedAs);
                return createFailureTakenBy(recordedAs);
            });

            assertThat(handedConsumer.get())
                .isSameAs(MAP_OVERLAY);
        }

        @Test
        void recordsASecondConsumerOfOneSubjectIndependently() {

            // What one broken third party costs is a different thing to each mod bound to it, said
            // in that mod's own words - so a latch keeping only the first would leave the second
            // mod's players told what another mod lost.
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailureTakenBy(recordedAs));
            failures.recordOnce(FAST_RENDERING, COLONY_PANEL, recordedAs -> createFailureTakenBy(recordedAs));

            assertThat(failures.takeNextUnreported().consumer().lostFeature())
                .isEqualTo(CompatibilityFailureFixture.LOST_FEATURE);
            assertThat(failures.takeNextUnreported().consumer().lostFeature())
                .isEqualTo(CompatibilityFailureFixture.COLONY_PANEL_LOST_FEATURE);
        }

        @Test
        void recordsASecondSubjectIndependently() {

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailureBrokenAt("first"));
            failures.recordOnce(NEXERELIN, MAP_OVERLAY, recordedAs -> createFailureBrokenAt("second"));

            assertThat(failures.takeNextUnreported().breakage().brokenDetail())
                .isEqualTo("first");
            assertThat(failures.takeNextUnreported().breakage().brokenDetail())
                .isEqualTo("second");
        }

        @Test
        void reportsBothFeaturesOneModFiledUnderOneKeyWithTheSecondKeyNumbered() {

            // One mod, one feature key, two sentences: the mod's own bug, and one that used to
            // cost the second feature's report entirely - the pair was already latched, so the
            // record dropped it exactly as it drops the same feature recording twice.
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailureTakenBy(recordedAs));
            failures.recordOnce(
                FAST_RENDERING,
                MAP_LEGEND_UNDER_REUSED_KEY,
                recordedAs -> createFailureTakenBy(recordedAs));

            var firstConsumer = failures.takeNextUnreported().consumer();
            var secondConsumer = failures.takeNextUnreported().consumer();

            assertThat(firstConsumer.consumerKey())
                .isEqualTo("map-mod:map-overlay");
            assertThat(secondConsumer.consumerKey())
                .isEqualTo("map-mod:map-overlay-2");
            assertThat(secondConsumer.lostFeature())
                .isEqualTo(MAP_LEGEND_LOST_FEATURE);
        }

        @Test
        void keepsTheBareKeyForTheSameFeatureRecordingTwice() {

            // The sentence is what tells a re-construction from a collision: the map's consumer is
            // built afresh every time its publisher is, and a record numbering every construction
            // would mint a new key per map open and report one failure once a frame forever.
            var rebuiltMapOverlay = new CompatibilityConsumer(
                CompatibilityFailureFixture.MAP_OVERLAY_MOD_ID,
                CompatibilityFailureFixture.MAP_OVERLAY_FEATURE_KEY,
                CompatibilityFailureFixture.LOST_FEATURE,
                CompatibilityFailureFixture.UNAFFECTED_FEATURE);

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailureTakenBy(recordedAs));
            failures.recordOnce(FAST_RENDERING, rebuiltMapOverlay, recordedAs -> createFailureTakenBy(recordedAs));

            assertThat(failures.takeNextUnreported().consumer().consumerKey())
                .isEqualTo("map-mod:map-overlay");
            assertThat(failures.takeNextUnreported())
                .isNull();
        }

        @Test
        void numbersAThirdSentenceUnderOneKeyNext() {

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailureTakenBy(recordedAs));
            failures.recordOnce(
                FAST_RENDERING,
                MAP_LEGEND_UNDER_REUSED_KEY,
                recordedAs -> createFailureTakenBy(recordedAs));
            failures.recordOnce(
                FAST_RENDERING,
                MAP_SEARCH_UNDER_REUSED_KEY,
                recordedAs -> createFailureTakenBy(recordedAs));

            failures.takeNextUnreported();
            failures.takeNextUnreported();

            assertThat(failures.takeNextUnreported().consumer().consumerKey())
                .isEqualTo("map-mod:map-overlay-3");
        }

        @Test
        void leavesTwoModsUnderTheirOwnKeysUnnumbered() {

            // Only the feature half can collide; the mod half is the mod's own ID, so another mod
            // spelling the same feature key is two keys and nothing to deconflict.
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailureTakenBy(recordedAs));
            failures.recordOnce(
                FAST_RENDERING,
                ANOTHER_MODS_MAP_OVERLAY,
                recordedAs -> createFailureTakenBy(recordedAs));

            assertThat(failures.takeNextUnreported().consumer().consumerKey())
                .isEqualTo("map-mod:map-overlay");
            assertThat(failures.takeNextUnreported().consumer().consumerKey())
                .isEqualTo("another-map-mod:map-overlay");
        }

        @Test
        void keepsTheBindingLatchedAfterItsFailureWasTaken() {

            // Once per session, not once per take: the frame after a report fails the same way, and
            // that is the failure already reported rather than a new one.
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailureBrokenAt("first"));
            failures.takeNextUnreported();
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailureBrokenAt("second"));

            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void invokesAThrowingDescriberOnceForTheSession() {

            var describeCount = new AtomicInteger();

            assertThatIllegalStateException()
                .isThrownBy(() -> failures.recordOnce(
                    FAST_RENDERING,
                    MAP_OVERLAY,
                    recordedAs -> countAndThrow(describeCount)));
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> countAndThrow(describeCount));

            // The latch was taken before the describer ran, so the second record is the ignored
            // path and the binding reports nothing rather than throwing on every frame.
            assertThat(describeCount)
                .hasValue(1);
            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void storesOneFailureWhereTwoThreadsRecordTheSameBindingAtOnce() throws InterruptedException {

            var describeCount = new AtomicInteger();
            var startGate = new CountDownLatch(1);
            Runnable recordFromThread = () -> {
                awaitStart(startGate);
                failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> countAndCreateFailure(describeCount));
            };
            var renderThread = new Thread(recordFromThread);
            var gameThread = new Thread(recordFromThread);
            renderThread.start();
            gameThread.start();

            // Both threads are parked on the gate, so releasing it is what makes the two records
            // race rather than run one after the other.
            startGate.countDown();
            renderThread.join();
            gameThread.join();

            assertThat(describeCount)
                .hasValue(1);
            assertThat(failures.takeNextUnreported())
                .isNotNull();
            assertThat(failures.takeNextUnreported())
                .isNull();
        }

        @Test
        void handsOutEachPositionOnceWhereTwoThreadsRecordUnderOneKeyAtOnce() throws InterruptedException {

            // The race the numbering has to survive: two of one mod's features, under the key it
            // spelled for both, recorded at once. A position read outside the latch would hand two
            // records one number, or the same key twice.
            var startGate = new CountDownLatch(1);
            var renderThread = new Thread(buildRecordReleasedBy(startGate, MAP_OVERLAY));
            var gameThread = new Thread(buildRecordReleasedBy(startGate, MAP_LEGEND_UNDER_REUSED_KEY));
            renderThread.start();
            gameThread.start();

            startGate.countDown();
            renderThread.join();
            gameThread.join();

            // Which sentence takes the bare key is whichever won the race, so what is pinned is
            // that the two positions were handed out once each.
            assertThat(List.of(
                    failures.takeNextUnreported().consumer().consumerKey(),
                    failures.takeNextUnreported().consumer().consumerKey()))
                .containsExactlyInAnyOrder("map-mod:map-overlay", "map-mod:map-overlay-2");
        }
    }

    @Nested
    class TakeNextUnreported {

        @Test
        void answersNothingWhereNothingWasRecorded() {

            assertThat(failures.takeNextUnreported())
                .isNull();
        }

        @Test
        void answersTheOldestRecordFirst() {
            // Record order, so the first binding to break is the first a player is told about.
            failures.recordOnce(NEXERELIN, MAP_OVERLAY, recordedAs -> createFailureBrokenAt("first"));
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailureBrokenAt("second"));

            assertThat(failures.takeNextUnreported().breakage().brokenDetail())
                .isEqualTo("first");
        }

        @Test
        void leavesTheRestWhereTheyAre() {

            failures.recordOnce(NEXERELIN, MAP_OVERLAY, recordedAs -> createFailureBrokenAt("first"));
            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailureBrokenAt("second"));

            failures.takeNextUnreported();

            assertThat(failures.hasUnreported())
                .isTrue();
        }

        @Test
        void answersNothingOnceTheLastWasTaken() {

            failures.recordOnce(FAST_RENDERING, MAP_OVERLAY, recordedAs -> createFailure());
            failures.takeNextUnreported();

            assertThat(failures.takeNextUnreported())
                .isNull();
        }
    }

    // One record parked on the gate, so several released together race rather than run in turn.
    private Runnable buildRecordReleasedBy(CountDownLatch startGate, CompatibilityConsumer consumer) {

        return () -> {
            awaitStart(startGate);
            failures.recordOnce(FAST_RENDERING, consumer, recordedAs -> createFailureTakenBy(recordedAs));
        };
    }

    // Parks the calling thread until the gate opens; an interruption while parked is reported as a
    // broken arrangement rather than swallowed into a record that never raced.
    private static void awaitStart(CountDownLatch startGate) {

        try {
            startGate.await();

        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted before the recording threads were released", interruption);
        }
    }

    private static CompatibilityFailure countAndCreateFailure(AtomicInteger describeCount) {

        describeCount.incrementAndGet();
        return createFailure();
    }

    private static CompatibilityFailure countAndThrow(AtomicInteger describeCount) {

        describeCount.incrementAndGet();
        throw new IllegalStateException("A describer that could not describe");
    }
}
