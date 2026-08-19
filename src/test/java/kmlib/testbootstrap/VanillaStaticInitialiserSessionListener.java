package kmlib.testbootstrap;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Initialises the vanilla API classes that read game settings while being initialised, once, before
 * the first test runs.
 *
 * <p>Several API classes fill static fields from {@code Global.getSettings()} at
 * class-initialisation time, and nothing supplies settings under the test JVM. Class initialisation
 * runs once and its failure is permanent, so whether such a class works at all is decided by whether
 * the first test to touch it happened to have a stub in place: the first one to reach it without
 * leaves the class unusable for the rest of the JVM, and every later test touching it fails with a
 * {@code NoClassDefFoundError} naming nothing of the cause. That is a whole-suite collapse chosen by
 * test ordering, and no test can defend against it from the inside - by the time one runs, the
 * question has already been answered by whoever went first.
 *
 * <p>So the initialisation is taken here, with a stub in place, before any test is handed the chance
 * to do it wrong. Afterwards the stub goes away and the classes stay initialised: initialisation
 * being one-shot is what makes this work rather than something to work around.
 *
 * <p>What a test author has to know is the consequence: settings-derived constants read as their
 * stubbed defaults - zero, false, null - for the whole run, and no test can arrange otherwise. That
 * was already true of any run that survived, the values having been fixed by whichever stub happened
 * to be first; it is now merely the same on every run. A test needing a real value has to reach it
 * through something it can stub, not off one of these constants.
 */
public final class VanillaStaticInitialiserSessionListener implements LauncherSessionListener {

    // The classes to initialise while the stub is up. By name rather than by class literal so that
    // nothing here links to them: this class must not become the early touch it exists to prevent.
    //
    // Only what this library actually reaches. The API carries many more of these, and listing one
    // the suite never touches would be initialising a class on the strength of a guess about who
    // might one day want it.
    private static final List<String> SETTINGS_READING_API_CLASSES =
        List.of("com.fs.starfarer.api.util.Misc");

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        // The stub is thread-local and the initialisation below runs on this thread, so it is in
        // force exactly where it is needed and nowhere else.
        try (var globalMock = mockStatic(Global.class)) {

            globalMock
                .when(Global::getSettings)
                .thenReturn(mock(SettingsAPI.class));

            SETTINGS_READING_API_CLASSES.forEach(this::initialiseApiClass);
        }
    }

    // Loads the class *and runs its initialiser*, which is what the second argument asks for and the
    // whole point of the call - a load without it would leave the hazard where it was.
    private void initialiseApiClass(String className) {
        try {
            Class.forName(
                className,
                true,
                getClass().getClassLoader());

        } catch (ClassNotFoundException | LinkageError cannotInitialiseHere) {
            // Left to the tests. A class that is absent, or that refuses to initialise even with
            // settings stubbed, will fail the tests that actually need it with their own names
            // attached - where failing the session would take down the whole suite, including
            // everything that never touches it.
        }
    }
}
