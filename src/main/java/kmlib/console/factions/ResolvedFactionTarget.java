package kmlib.console.factions;

import com.fs.starfarer.api.campaign.FactionAPI;

/**
 * The faction a command may act for - the half of {@link FactionTargetResolution} that found one.
 *
 * @param faction the faction the command was pointed at, the player's own when none was named
 */
public record ResolvedFactionTarget(
    FactionAPI faction) implements FactionTargetResolution {
}
