package kmlib.testfixtures.starsector.listeners;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the listener manager installer suites run against: every call is recorded and the
 * recordings never shrink, while the listeners it holds answer reads the way the game's manager
 * does - by instance of the class asked for, saved before transient, in the order added, each held
 * once, and gone once removed.
 */
final class RecordingListenerManagerTests {

    private interface MarketListener {
    }

    private static final class FirstListener
            implements MarketListener {
    }

    private static final class SecondListener
            implements MarketListener {
    }

    private static final class UnrelatedListener {
    }

    @Nested
    class AddListener {

        @Test
        void recordsEachAddWithTheFlagItWasMadeUnder() {

            var listenerManager = new RecordingListenerManager();
            var first = new FirstListener();
            var second = new SecondListener();

            listenerManager.addListener(first);
            listenerManager.addListener(second, true);

            assertThat(listenerManager.getAddedListeners())
                .containsExactly(first, second);
            assertThat(listenerManager.getAddedTransientFlags())
                .containsExactly(false, true);
        }

        @Test
        void holdsAListenerAddedTwiceOnceWhileRecordingBothAdds() {
            // The game's store ignores an add of what it already holds, so a delivery reaches the
            // listener once however many times it was added.
            var listenerManager = new RecordingListenerManager();
            var listener = new FirstListener();

            listenerManager.addListener(listener, true);
            listenerManager.addListener(listener, true);

            assertThat(listenerManager.getAddedListeners())
                .containsExactly(listener, listener);
            assertThat(listenerManager.getListeners(MarketListener.class))
                .containsExactly(listener);
        }
    }

    @Nested
    class RemoveListener {

        @Test
        void stopsHoldingTheListenerWhetherSavedOrTransient() {

            var listenerManager = new RecordingListenerManager();
            var saved = new FirstListener();
            var transientListener = new SecondListener();

            listenerManager.addListener(saved, false);
            listenerManager.addListener(transientListener, true);
            listenerManager.removeListener(saved);
            listenerManager.removeListener(transientListener);

            assertThat(listenerManager.getListeners(MarketListener.class))
                .isEmpty();
        }

        @Test
        void leavesTheRecordedAddsInPlace() {

            var listenerManager = new RecordingListenerManager();
            var listener = new FirstListener();

            listenerManager.addListener(listener, true);
            listenerManager.removeListener(listener);

            assertThat(listenerManager.getAddedListeners())
                .containsExactly(listener);
        }
    }

    @Nested
    class RemoveListenerOfClass {

        @Test
        void stopsHoldingEveryInstanceOfTheClassAndRecordsIt() {

            var listenerManager = new RecordingListenerManager();
            var first = new FirstListener();
            var unrelated = new UnrelatedListener();

            listenerManager.addListener(first, false);
            listenerManager.addListener(new SecondListener(), true);
            listenerManager.addListener(unrelated, true);
            listenerManager.removeListenerOfClass(SecondListener.class);

            assertThat(listenerManager.getRemovedListenerClasses())
                .containsExactly(SecondListener.class);
            assertThat(listenerManager.getListeners(Object.class))
                .containsExactly(first, unrelated);
        }

        @Test
        void stopsHoldingImplementersWhenAskedForAnInterface() {
            // The game's store files a listener under every type it is an instance of, so clearing
            // an interface clears everything implementing it.
            var listenerManager = new RecordingListenerManager();
            var unrelated = new UnrelatedListener();

            listenerManager.addListener(new FirstListener(), true);
            listenerManager.addListener(new SecondListener(), true);
            listenerManager.addListener(unrelated, true);
            listenerManager.removeListenerOfClass(MarketListener.class);

            assertThat(listenerManager.getListeners(Object.class))
                .containsExactly(unrelated);
        }
    }

    @Nested
    class HasListener {

        @Test
        void answersTrueForAHeldListener() {

            var listenerManager = new RecordingListenerManager();
            var listener = new FirstListener();

            listenerManager.addListener(listener, true);

            assertThat(listenerManager.hasListener(listener))
                .isTrue();
        }

        @Test
        void answersFalseOnceTheListenerIsRemoved() {

            var listenerManager = new RecordingListenerManager();
            var listener = new FirstListener();

            listenerManager.addListener(listener, false);
            listenerManager.removeListener(listener);

            assertThat(listenerManager.hasListener(listener))
                .isFalse();
        }
    }

    @Nested
    class HasListenerOfClass {

        @Test
        void answersTrueWhenAnInstanceOfTheClassIsHeld() {

            var listenerManager = new RecordingListenerManager();

            listenerManager.addListener(new FirstListener(), true);

            assertThat(listenerManager.hasListenerOfClass(MarketListener.class))
                .isTrue();
        }

        @Test
        void answersFalseWhenNoInstanceOfTheClassIsHeld() {

            var listenerManager = new RecordingListenerManager();

            listenerManager.addListener(new UnrelatedListener(), true);

            assertThat(listenerManager.hasListenerOfClass(MarketListener.class))
                .isFalse();
        }
    }

    @Nested
    class GetListeners {

        @Test
        void answersSavedListenersBeforeTransientOnesEachInTheOrderAdded() {

            var listenerManager = new RecordingListenerManager();
            var firstTransient = new FirstListener();
            var firstSaved = new SecondListener();
            var secondTransient = new SecondListener();
            var secondSaved = new FirstListener();

            listenerManager.addListener(firstTransient, true);
            listenerManager.addListener(firstSaved, false);
            listenerManager.addListener(secondTransient, true);
            listenerManager.addListener(secondSaved, false);

            assertThat(listenerManager.getListeners(MarketListener.class))
                .containsExactly(firstSaved, secondSaved, firstTransient, secondTransient);
        }

        @Test
        void answersOnlyInstancesOfTheClassAskedFor() {

            var listenerManager = new RecordingListenerManager();
            var listener = new FirstListener();

            listenerManager.addListener(listener, true);
            listenerManager.addListener(new UnrelatedListener(), true);

            assertThat(listenerManager.getListeners(MarketListener.class))
                .containsExactly(listener);
        }

        @Test
        void answersNothingWhenNothingWasAdded() {

            assertThat(new RecordingListenerManager().getListeners(MarketListener.class))
                .isEmpty();
        }
    }
}
