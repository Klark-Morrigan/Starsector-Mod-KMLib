package kmlib.testfixtures.starsector.listeners;

import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;

import java.util.ArrayList;
import java.util.List;

/**
 * A listener manager that records every registration made with it and holds what it was given the
 * way the game does.
 *
 * <p>What every installer suite asserts against, since an installer's whole observable behaviour is
 * the calls it makes here: which classes it cleared first, what it added, and under which flag.
 * Recording all three is what lets a suite pin the registration shape - that a class is cleared
 * before anything is added under it, and that what is added is added transient.
 *
 * <p>The recordings never shrink; what the manager holds does. Removals act on the held listeners
 * alone, so a suite can still read every call made while delivering an event only to the listeners
 * a live manager would still hold. The reads answer from that held set as the game's manager does:
 * {@link #getListeners} hands back every held listener that is an instance of the class asked for,
 * the saved ones before the transient ones, each in the order they were added, and a listener added
 * where it is already held is not held twice.
 *
 * <p>Shared rather than restated per suite so "what an install looks like from the manager's side"
 * has one answer. A suite that built its own would be free to record only the half it happened to
 * assert on, and the next case written against it would have nothing to check.
 */
public final class RecordingListenerManager
        implements ListenerManagerAPI {

    private final List<Object> addedListeners = new ArrayList<>();

    // Parallel to addedListeners: the engine's second addListener parameter means transient (true
    // keeps the listener out of the save), so the recording mirrors that vocabulary. One install can
    // add several listeners, so these are lists, not scalars.
    private final List<Boolean> addedTransientFlags = new ArrayList<>();
    private final List<Class<?>> removedListenerClasses = new ArrayList<>();

    // Held apart by the flag they were added under, because the game keeps saved and transient
    // listeners in two stores and answers a read from the saved one first.
    private final List<Object> heldSavedListeners = new ArrayList<>();
    private final List<Object> heldTransientListeners = new ArrayList<>();

    /** Every listener registered, in registration order. */
    public List<Object> getAddedListeners() {
        return addedListeners;
    }

    /** The flag each registration was made under, positionally matching the listeners above. */
    public List<Boolean> getAddedTransientFlags() {
        return addedTransientFlags;
    }

    /** Every class cleared before a registration, in the order they were cleared. */
    public List<Class<?>> getRemovedListenerClasses() {
        return removedListenerClasses;
    }

    @Override
    public void addListener(Object listener) {
        addListener(listener, false);
    }

    @Override
    public void addListener(Object listener, boolean isTransient) {

        addedListeners.add(listener);
        addedTransientFlags.add(isTransient);

        var heldListeners = isTransient ? heldTransientListeners : heldSavedListeners;

        // The game's store ignores an add of what it already holds, so one listener added twice is
        // still told once.
        if (!heldListeners.contains(listener)) {
            heldListeners.add(listener);
        }
    }

    @Override
    public void removeListener(Object listener) {

        heldSavedListeners.remove(listener);
        heldTransientListeners.remove(listener);
    }

    @Override
    public void removeListenerOfClass(Class<?> listenerClass) {

        removedListenerClasses.add(listenerClass);

        heldSavedListeners.removeIf(listenerClass::isInstance);
        heldTransientListeners.removeIf(listenerClass::isInstance);
    }

    @Override
    public boolean hasListener(Object listener) {

        return heldSavedListeners.contains(listener)
            || heldTransientListeners.contains(listener);
    }

    @Override
    public boolean hasListenerOfClass(Class<?> listenerClass) {
        return !getListeners(listenerClass).isEmpty();
    }

    @Override
    public <T> List<T> getListeners(Class<T> listenerClass) {

        var listeners = new ArrayList<T>();

        for (var heldListeners : List.of(heldSavedListeners, heldTransientListeners)) {

            heldListeners.stream()
                .filter(listenerClass::isInstance)
                .map(listenerClass::cast)
                .forEach(listeners::add);
        }

        return listeners;
    }
}
