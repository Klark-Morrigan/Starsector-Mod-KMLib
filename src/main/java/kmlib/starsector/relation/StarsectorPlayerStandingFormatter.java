package kmlib.starsector.relation;

import java.util.Locale;

/**
 * Writes a {@link FactionRelation} as the prose the engine itself shows on the player's own faction
 * screens: {@code "<RepLevel name> (<rep> / 100)"}, e.g. {@code "Friendly (35 / 100)"} - what a
 * colony tooltip and an intel row print. Named for that wording rather than for the value, which
 * says nothing about whose standing it is.
 *
 * <p>Takes the standing rather than the faction, so it holds prose and nothing else: reading a
 * standing off the game is {@link StarsectorPlayerStandings}'s work, and a faction with no standing
 * to read never reaches here. That is what keeps one absent case in the package instead of two - a
 * formatter that took the faction would have to invent a second way of saying "nothing was read",
 * and a caller would have to learn which of the two it was holding.
 *
 * <p>Lives in KMLib so every mod that surfaces a faction's standing with the player words it the
 * same way. Stateless - the single entry point is a static method, no instance needed.
 */
public final class StarsectorPlayerStandingFormatter {

    private static final int MAX_STANDING_REPUTATION = 100;
    private static final String PLAYER_STANDING_DESCRIPTION_FORMAT = "%s (%d / %d)";

    private StarsectorPlayerStandingFormatter() {
    }

    /**
     * Words a standing the way the engine does.
     *
     * <p>Every level on the scale is a constant of a closed vanilla enum carrying the display name
     * it was declared with, so there is always a name to print and no fallback to write.
     *
     * @param standing the standing to word
     * @return the description, e.g. {@code "Friendly (35 / 100)"}
     */
    public static String formatPlayerStanding(FactionRelation standing) {

        return String.format(
            Locale.ROOT,
            PLAYER_STANDING_DESCRIPTION_FORMAT,
            standing.level().getDisplayName(),
            standing.reputation(),
            MAX_STANDING_REPUTATION);
    }
}
