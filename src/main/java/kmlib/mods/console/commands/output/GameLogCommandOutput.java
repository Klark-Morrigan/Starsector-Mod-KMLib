package kmlib.mods.console.commands.output;

import org.apache.log4j.Logger;

/**
 * Routes command output to the game log. The second binding of
 * {@link CommandOutput}, for what a command produces to be read after the fact
 * rather than in the overlay: a diagnostic a player is asked for reaches whoever
 * asked inside the log file they already attach, while the console scrolls away
 * with the session.
 *
 * <p>At info, since a caller only writes here when a player asked them to - what
 * arrives is answered, not diagnosed. Written in one entry however many lines it
 * carries, so a table stays a table in the file.
 *
 * <p>A single {@link #INSTANCE}: the binding is a stateless forwarder over one
 * logger, so one shared value serves every command rather than a fresh object per
 * construction (the same enum-singleton shape KMLib uses for the live sources
 * backing its other ports).
 */
public enum GameLogCommandOutput implements CommandOutput {

    INSTANCE;

    private static final Logger LOG = Logger.getLogger(GameLogCommandOutput.class);

    @Override
    public void showMessage(String message) {
        LOG.info(message);
    }
}
