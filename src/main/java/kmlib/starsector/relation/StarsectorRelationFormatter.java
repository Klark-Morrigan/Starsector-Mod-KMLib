package kmlib.starsector.relation;

import java.util.Locale;

/**
 * Writes a {@link FactionRelation} as the prose the engine itself shows on the player's own faction
 * screens: {@code "<RepLevel name> (<rep> / 100)"}, e.g. {@code "Friendly (35 / 100)"} - what a
 * colony tooltip and an intel row print.
 *
 * <p>Takes the relation rather than the faction, so it holds prose and nothing else: reading a
 * relation off the game is {@link StarsectorFactionRelations}' work, and a pair with none to read
 * never reaches here. That is what keeps one absent case in the package instead of two - a formatter
 * that took the faction would have to invent a second way of saying "nothing was read", and a caller
 * would have to learn which of the two it was holding.
 *
 * <p>The wording is the player's own screens', and the value is any pair's, so a surface reporting
 * one faction's relation to another words it the way the game words the player's. Lives in KMLib so
 * every mod that surfaces a relation words it the same way. Stateless - the single entry point is a
 * static method, no instance needed.
 */
public final class StarsectorRelationFormatter {

    private static final int MAX_REPUTATION = 100;
    private static final String RELATION_DESCRIPTION_FORMAT = "%s (%d / %d)";

    private StarsectorRelationFormatter() {
    }

    /**
     * Words a relation the way the engine does.
     *
     * <p>Every level on the scale is a constant of a closed vanilla enum carrying the display name
     * it was declared with, so there is always a name to print and no fallback to write.
     *
     * @param relation the relation to word
     * @return the description, e.g. {@code "Friendly (35 / 100)"}
     */
    public static String formatRelation(FactionRelation relation) {

        return String.format(
            Locale.ROOT,
            RELATION_DESCRIPTION_FORMAT,
            relation.level().getDisplayName(),
            relation.reputation(),
            MAX_REPUTATION);
    }
}
