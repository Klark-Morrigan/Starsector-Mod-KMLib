package kmlib.starsector.dialogs.handoff;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;

/**
 * Handing an open interaction dialog to another plugin.
 *
 * <p>Setting the dialog's plugin does not start it: the game calls {@code init} only on the plugin a dialog opens
 * with. A plugin swapped in without its own {@code init} call leaves the dialog showing the previous plugin's text and
 * options, now answered by a plugin that never drew them. Each hand-off here sets the plugin and starts it as one
 * step.
 */
public final class DialogHandOff {

    private DialogHandOff() {
    }

    /**
     * Clears the dialog's options and hides its visual panel, then hands it to {@code plugin}. For a hand-off from the
     * rules engine, whose options and visuals belong to the plugin being replaced: the incoming plugin starts from a
     * clean dialog rather than drawing over them.
     *
     * @param dialog the open dialog
     * @param plugin the plugin to run it from now on
     */
    public static void clearAndHandDialogTo(InteractionDialogAPI dialog, InteractionDialogPlugin plugin) {

        dialog
            .getOptionPanel()
            .clearOptions();
        dialog
            .hideVisualPanel();

        handDialogTo(dialog, plugin);
    }

    /**
     * Hands the dialog to {@code plugin} as it stands, for a plugin that redraws the dialog itself, such as one
     * returning to the plugin it replaced.
     *
     * @param dialog the open dialog
     * @param plugin the plugin to run it from now on
     */
    public static void handDialogTo(InteractionDialogAPI dialog, InteractionDialogPlugin plugin) {

        dialog.setPlugin(plugin);
        plugin.init(dialog);
    }
}
