package kmlib.starsector.dialogs;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;

import java.util.Optional;

/**
 * Reads of the entity an interaction dialog is open on.
 *
 * <p>A rule command is handed whatever dialog the rules engine holds, which can be none, and a dialog can be open on
 * no entity. The reads here answer both as nothing rather than failing, so a command can name its target in a log line
 * before it has checked either.
 */
public final class InteractionTargets {

    private InteractionTargets() {
    }

    /**
     * @param dialog the dialog, possibly {@code null}
     * @return the planet the dialog is open on, or empty without a dialog, without a target, or on a target that is
     *         no planet
     */
    public static Optional<PlanetAPI> findTargetPlanet(InteractionDialogAPI dialog) {

        if (dialog == null || !(dialog.getInteractionTarget() instanceof PlanetAPI planet)) {
            return Optional.empty();
        }
        return Optional.of(planet);
    }

    /**
     * @param dialog the dialog, possibly {@code null}
     * @return the ID of the entity the dialog is open on, or {@code null} without a dialog or a target
     */
    public static String readTargetId(InteractionDialogAPI dialog) {

        if (dialog == null) {
            return null;
        }

        var target = dialog.getInteractionTarget();
        return target == null
            ? null
            : target.getId();
    }
}
