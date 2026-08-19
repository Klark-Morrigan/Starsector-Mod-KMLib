package kmlib.console.factions;

/**
 * What came of looking for the faction a command should act for: the faction, or the reason
 * there is none, worded for the player.
 *
 * <p>Sealed over the two, so neither can be read without the other having been dealt with. A
 * command that fell through to a null faction would hand an ownership change an id nothing in
 * the sector answers to, which the game accepts and then reads as a colony flying no flag.
 */
public sealed interface FactionTargetResolution
    permits ResolvedFactionTarget, UnresolvedFactionTarget {
}
