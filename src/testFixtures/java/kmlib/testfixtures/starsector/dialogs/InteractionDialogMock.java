package kmlib.testfixtures.starsector.dialogs;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A stand-in interaction dialog wired to its option, text and visual panels, each a mock of its own, so a plugin
 * drawing into the dialog is verified panel by panel.
 *
 * <p>Every dialog plugin reaches its panels through the dialog, so each suite over one would otherwise wire the same
 * three answers. The dialog is open on no entity until {@link #setTarget(SectorEntityToken)} names one.
 */
public final class InteractionDialogMock {

    private final InteractionDialogAPI dialogMock = mock(InteractionDialogAPI.class);
    private final OptionPanelAPI optionPanelMock = mock(OptionPanelAPI.class);
    private final TextPanelAPI textPanel;
    private final VisualPanelAPI visualPanelMock = mock(VisualPanelAPI.class);

    private InteractionDialogMock(TextPanelAPI textPanel) {

        this.textPanel = textPanel;

        when(dialogMock.getOptionPanel())
            .thenReturn(optionPanelMock);
        when(dialogMock.getTextPanel())
            .thenReturn(textPanel);
        when(dialogMock.getVisualPanel())
            .thenReturn(visualPanelMock);
    }

    /**
     * Opens a dialog whose panels are plain mocks.
     *
     * @return the stand-in dialog
     */
    public static InteractionDialogMock createDialogMock() {
        return new InteractionDialogMock(mock(TextPanelAPI.class));
    }

    /**
     * Opens a dialog drawing into the text panel a case supplies, such as one that records what is added to it.
     *
     * @param textPanel the text panel the dialog answers
     * @return the stand-in dialog
     */
    public static InteractionDialogMock createDialogMockWithTextPanel(TextPanelAPI textPanel) {
        return new InteractionDialogMock(textPanel);
    }

    public InteractionDialogAPI getDialog() {
        return dialogMock;
    }

    public OptionPanelAPI getOptionPanel() {
        return optionPanelMock;
    }

    public TextPanelAPI getTextPanel() {
        return textPanel;
    }

    public VisualPanelAPI getVisualPanel() {
        return visualPanelMock;
    }

    /**
     * Opens the dialog on {@code target}.
     *
     * @param target the entity the dialog answers as its interaction target
     * @return this dialog
     */
    public InteractionDialogMock setTarget(SectorEntityToken target) {

        when(dialogMock.getInteractionTarget())
            .thenReturn(target);

        return this;
    }
}
