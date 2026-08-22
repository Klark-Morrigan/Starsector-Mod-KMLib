package kmlib.mods.consolecommands;

import com.fs.starfarer.api.Global;

import kmlib.logging.SessionWarning;

import org.apache.log4j.Logger;

/**
 * The {@link ConsoleOverlay} answer for Console Commands: a mod-presence gate in front of the
 * mod's own overlay read, with every failure turned into "no console is open".
 *
 * <p>Console Commands is compiled against but not declared a dependency, so an install without it
 * is ordinary and must cost nothing - which is what the gate, the deferred panel read and the held
 * enablement below are each for.
 *
 * <p>Fail-open is the governing rule here rather than a footnote. The mod absent, the class
 * missing, the accessor moved by a Console Commands release, the read throwing anything at all -
 * each reports no console open and nothing else. Callers then behave as they did before this
 * question existed: drawn over the console and still holding its hotkeys, which is a survivable
 * annoyance a player can work around by closing the panel. Reporting open-on-failure, or letting
 * a throw escape, would instead take those callers away on every screen and every frame, for a
 * mod the player may not even have installed.
 */
public final class ConsoleCommandsOverlay implements ConsoleOverlay {

    private static final Logger LOG = Global.getLogger(ConsoleCommandsOverlay.class);

    // What every failure here costs, said in the log's own terms. Appended to whichever hop broke,
    // so one line names both the cause and the consequence.
    private static final String FAIL_OPEN_CONSEQUENCE =
        " Anything that steps aside for an open console will no longer do so this session.";

    /**
     * The one live console read, shared by everything on the install that stands down for a
     * console - so the settled enablement, the built presence and the spent warning are held once
     * rather than per caller. Callers name this where they compose; what they hold is the
     * {@link ConsoleOverlay} role.
     */
    // Declared below the logger and not with the other headline members: constructing it runs this
    // class's instance initialisers, and the warning among them takes LOG, which static init has
    // not reached until its own declaration.
    public static final ConsoleCommandsOverlay INSTANCE = new ConsoleCommandsOverlay();

    // Whether the console can be read at all, settled on the first ask. The mod set cannot change
    // within a run, so a successful read is held rather than repeated every frame. A failure
    // settles it unreadable because the console cannot be read any more and a retry would throw
    // again on the next frame, which is as good as the mod not being installed.
    private ConsoleReadState consoleReadState = ConsoleReadState.UNASKED;

    // Built on the first ask that gets past the gate, not at construction: building it is what
    // resolves the class that names Console Commands' overlay panel, and an install without the
    // mod must never reach that name.
    private ConsoleOverlayPresence presence;

    // Names in the log whichever hop broke, which is what turns a Console Commands release moving
    // the accessor into a line naming it rather than a player report about the console. Once, since
    // the hop that broke first is the one that stopped the read and every later frame would say the
    // same - one line against the sixty a second an ungated warning would write.
    private final SessionWarning warning = new SessionWarning(LOG);

    /**
     * Reads the console state Console Commands itself publishes. Package-private because a second
     * live reader would keep a second settled enablement and spend a second warning on the same
     * break; production reads {@link #INSTANCE}.
     */
    ConsoleCommandsOverlay() {
    }

    /**
     * @param presence the console state to read once the mod gate has passed, in place of Console
     *                 Commands' own overlay panel
     */
    ConsoleCommandsOverlay(ConsoleOverlayPresence presence) {
        this.presence = presence;
    }

    @Override
    public boolean isOpen() {
        // Short-circuit before touching the presence, so an install without Console Commands never
        // resolves the class that names its overlay panel - and so a settled failure costs one
        // boolean rather than a throw per frame.
        if (!isConsoleReadable()) {
            return false;
        }

        try {
            return resolvePresence().isOverlayUp();

        } catch (LinkageError cannotReachConsole) {

            // Installed, but its overlay panel or accessor is not where this was compiled against:
            // a Console Commands release moved, renamed or dropped it.
            return reportUnreadable(
                "Console Commands is installed but its overlay panel could not be reached.",
                cannotReachConsole);

        } catch (Throwable consoleReadFailed) {

            return reportUnreadable(
                "Reading Console Commands' overlay state failed.",
                consoleReadFailed);
        }
    }

    // Whether a console read is worth attempting at all: the mod is installed and nothing has
    // broken yet. A mod-manager read that throws answers "not readable", the fail-open answer.
    private boolean isConsoleReadable() {

        if (consoleReadState == ConsoleReadState.UNASKED) {
            try {
                consoleReadState = ConsoleCommandsPresence.isModEnabled()
                    ? ConsoleReadState.READABLE
                    : ConsoleReadState.UNREADABLE;

            } catch (Throwable cannotReadModState) {

                reportUnreadable(
                    "Could not read whether Console Commands is installed.",
                    cannotReadModState);
            }
        }
        return consoleReadState == ConsoleReadState.READABLE;
    }

    // The presence, built behind the gate on first use. Not synchronised: every ask arrives on the
    // game's own thread, and a second construction would in any case only rebuild a stateless read.
    private ConsoleOverlayPresence resolvePresence() {

        if (presence == null) {
            presence = new ConsoleCommandsPanelPresence();
        }
        return presence;
    }

    // Settles the read off for the session and says why, once. Returns the fail-open answer so a
    // failure branch reads as one line at the call site.
    private boolean reportUnreadable(String cause, Throwable failure) {

        consoleReadState = ConsoleReadState.UNREADABLE;
        warning.warnOnce(cause + FAIL_OPEN_CONSEQUENCE, failure);
        return false;
    }

    // What is known about reading the console, named rather than carried as a nullable flag: the
    // question genuinely has three answers, and "nobody has asked yet" is one a reader should not
    // have to recognise as an absent value.
    private enum ConsoleReadState {
        UNASKED,
        READABLE,
        UNREADABLE
    }
}
