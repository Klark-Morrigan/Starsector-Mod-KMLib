package kmlib.starsector.systems.claims;

/**
 * One faction's standing in a star system's claim contest: how strongly the game weighs its
 * presence there, and whether that presence is the kind that can take a claim at all.
 *
 * <p>The score is the faction's strongest single market in the system, measured the way the
 * claim mechanic measures it, so two factions' scores are directly comparable and rank the
 * same way the mechanic ranks them.
 *
 * <p>A faction that is not territorial is scored all the same. It can never win the system,
 * but it is present and it is competing for the space, and a reader asking who holds a system
 * is better served by seeing that than by seeing it silently dropped.
 *
 * @param factionId     the id of the faction this standing belongs to
 * @param score         the faction's strongest market score in the system
 * @param isTerritorial whether the faction's punitive-expedition data marks it territorial -
 *                      the gate a faction must pass before any score can claim a system
 */
public record FactionClaimScore(
    String factionId,
    int score,
    boolean isTerritorial) {
}
