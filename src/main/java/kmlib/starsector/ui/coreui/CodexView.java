package kmlib.starsector.ui.coreui;

import com.fs.state.AppDriver;

/**
 * Whether the codex is standing over the screen right now - the reference work the game raises over
 * whatever the player was looking at.
 *
 * <p>The codex panel itself is a large box in the middle of the screen rather than the whole of it,
 * but what it covers is the whole of it: the game raises a screen-spanning sibling behind the panel
 * that dims the backdrop and takes the events, so nothing underneath is reachable wherever the
 * pointer rests. A caller standing aside for this therefore has nothing to measure against a box.
 *
 * <p>Beside {@link CoreUiDialogView} because it answers the same question a caller has - what stands
 * *over* a screen - but it is emphatically not the same reading, and that is the finding this class
 * exists to record. The codex is not part of the core UI at all: the campaign state holds a second
 * screen panel of its own for it, a sibling of the one every core screen is drawn into, and raises
 * the codex there. So a walk of the core UI's children - which is the whole of the modal rule next
 * door - cannot see it however deep it goes, and the codex carries none of the modal marker that
 * rule recognises either. Two independent misses, either one enough.
 *
 * <p>What that costs anything composited after the core UI is {@link CoreUiDialogView}'s to state and
 * is the same cost here, the codex panel being drawn before the render pass such an overlay is
 * reached through. What differs is how the game silences the screen underneath: not an interceptor
 * swallowing events, but a cursor parked far off screen and fed to that screen's own panel - which
 * reaches every widget in the tree and nothing that polls the mouse for itself.
 *
 * <p>Read off the app state rather than off any widget, which is what keeps it one answer: the state
 * reports the codex up both ways it can be raised - opened over a screen into that second panel, and
 * opened from the campaign itself, which the state records as a dialog of its own kind instead. A
 * widget-shaped rule would have to find two different things in two different trees.
 *
 * <p>The state itself is reached through the app driver, which is unobfuscated and stable across
 * builds; the accessor on it is taken by name, the state's own type being obfuscated. The name is
 * part of that type's contract and survives obfuscation, so no obfuscated name appears here.
 *
 * <p>Presence alone, with no fade beside it - which is where this parts company with the modal read
 * next door rather than merely reading a different thing. The codex does fade in, three tenths of a
 * second and nothing at all under the game's fast-UI setting, but the fade lives on the panel and
 * this deliberately never walks to one. At that length a caller standing down on the frame the flag
 * turns does not read as a cut, so following the fade would buy a walk and no visible difference.
 *
 * <p>Fails open for the caller - whatever cannot be established reads as no codex showing. The
 * answer only ever takes something away from a caller, so an unreadable game leaves it doing what it
 * did before this question existed rather than going quiet on a screen the player is looking at.
 */
public final class CodexView {

    // The state's own report that the codex is up. Part of its contract, so it survives obfuscation
    // the way the hops the core-UI reach takes do.
    private static final String IS_SHOWING_CODEX_METHOD = "isShowingCodex";

    private CodexView() {
    }

    /**
     * @return whether the codex stands over the screen this frame, and {@code false} whenever that
     *         cannot be established - there being no app state stood up yet, or the state in force
     *         not answering for a codex at all
     */
    public static boolean isCodexShowing() {
        return isCodexShowingOn(readCurrentAppState());
    }

    /**
     * The rule itself, over an app state its caller has already resolved. Package-private so it can
     * be driven against a stood-up state with no game running, the live resolution above being the
     * half that needs one.
     *
     * @param appState the app state in force, or null when there is none
     * @return whether it reports the codex showing
     */
    static boolean isCodexShowingOn(Object appState) {

        if (appState == null) {
            return false;
        }

        // Anything but a plain "yes, showing" is read as no codex, which puts a state carrying no
        // such accessor alongside a hop that resolved and threw. Every state that can raise a codex
        // carries it, so one that does not is a different shape rather than a broken read.
        return Boolean.TRUE.equals(
            CoreUiTree.readHopIfOffered(appState, IS_SHOWING_CODEX_METHOD));
    }

    // The state the game is currently running. Guarded because the reach is a class the game
    // provides rather than one this library ships: a build that no longer offers it fails on
    // resolution rather than on the call, which is an error and not an exception, and this is read
    // from a render pass every frame.
    private static Object readCurrentAppState() {

        try {
            return AppDriver.getInstance().getCurrentState();

        } catch (Throwable cannotReadAppState) {
            return null;
        }
    }
}
