package kmlib.starsector.ui.map.presence;

/**
 * Answers "is a sector map drawn in the campaign radar's place right now?" for anything whose
 * behaviour in game space turns on there being a map on screen while no screen is open.
 *
 * <p>The surface {@link MapPresence} cannot report. That one answers for the two hosts the game
 * ships, both of them screens the player opens; a minimap replaces the radar on the campaign HUD,
 * so it is on screen precisely when neither host is - which is what makes it worth its own
 * question rather than a third disjunct in that one.
 *
 * <p>A role rather than a static read of whichever mod supplies one, because such a mod is
 * optional: a caller depends on the question, and whether anything can answer it is settled
 * behind this interface. It is also the question rather than the mod, so a second mod replacing
 * the radar the same way is an implementation to add rather than a branch threaded through every
 * caller.
 */
public interface CampaignMinimap {

    /**
     * Whether an interactive minimap stands in for the campaign radar this session.
     *
     * <p>Fails open to "no minimap": every way the answer can go missing - no mod supplying one,
     * its setting unreadable, the read taken before the game's own settings are up - reports
     * {@code false}. A caller then behaves exactly as it does on a vanilla install, which is the
     * behaviour it had before this question existed, rather than adapting to a surface that may
     * not be on screen at all.
     *
     * @return whether a minimap replaces the campaign radar, and {@code false} whenever that
     *         cannot be established
     */
    boolean isReplacingRadar();
}
